package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.curleesoft.pickem.backend.cfbd.CfbdCalendarWeek;
import com.curleesoft.pickem.backend.cfbd.CfbdClient;
import com.curleesoft.pickem.backend.cfbd.CfbdClientException;
import com.curleesoft.pickem.backend.model.ImportJob;
import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.repository.ImportJobRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.InternalJobFilter;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.service.CfbdImportService;
import com.curleesoft.pickem.backend.service.CfbdPause;
import com.curleesoft.pickem.backend.service.SeasonService;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ImportApiTests extends FirestoreEmulatorSupport {

    private static final String SECRET = "test-secret";

    private static final String OUTSIDE = "203.0.113.10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CfbdImportService cfbdImportService;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private SeasonWeekRepository seasonWeekRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    @MockitoBean
    private CfbdClient cfbdClient;

    @MockitoBean
    private CfbdPause cfbdPause;

    private final List<String> seasonIds = new ArrayList<>();

    private final List<String> jobIds = new ArrayList<>();

    private Cookie manager;

    private Cookie player;

    @DynamicPropertySource
    static void jobSecret(DynamicPropertyRegistry registry) {
        registry.add("pickem.internal.job-secret", () -> SECRET);
    }

    @BeforeEach
    void signIn() {
        firebase.clear();
        manager = new Cookie(SessionCookies.NAME, firebase.issueCookie(identity("manager-" + UUID.randomUUID(),
                List.of("manager"))));
        player = new Cookie(SessionCookies.NAME,
                firebase.issueCookie(identity("player-" + UUID.randomUUID(), List.of("player"))));
    }

    @AfterEach
    void deleteFixtures() {
        for (String jobId : jobIds) {
            importJobRepository.delete(jobId);
        }
        for (String seasonId : seasonIds) {
            String parentId = Objects.requireNonNull(seasonId, "seasonId");
            for (SeasonWeek week : seasonWeekRepository.findAll()) {
                if (parentId.equals(week.getSeasonId()) && week.getId() != null) {
                    seasonWeekRepository.delete(week.getId());
                }
            }
            seasonRepository.delete(parentId);
        }
        jobIds.clear();
        seasonIds.clear();
    }

    @Test
    void managerEnqueueReturnsBeforeTheCfbdWalkFinishes() throws Exception {
        int year = uniqueYear();
        String seasonId = createSeason(year);
        LocalDate monday = LocalDate.of(year, 8, 1);
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }
        LocalDate sunday = monday.plusDays(6);
        CfbdCalendarWeek week = new CfbdCalendarWeek(year, 1, "regular", monday + "T00:00:00.000Z",
                sunday + "T23:59:59.000Z");
        CountDownLatch inCfbd = new CountDownLatch(1);
        CountDownLatch requestFinished = new CountDownLatch(1);
        when(cfbdClient.calendar(year)).thenAnswer(invocation -> {
            inCfbd.countDown();
            assertThat(requestFinished.await(10, TimeUnit.SECONDS)).isTrue();
            return List.of(week);
        });
        when(cfbdClient.games(eq(year), isNull(), eq("regular"))).thenReturn(List.of());

        ExecutorService callers = Executors.newSingleThreadExecutor();
        Future<MvcResult> future = callers.submit(() -> mockMvc.perform(post("/api/manager/imports/matchups")
                .cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content("{\"seasonId\":\"" + seasonId + "\"}")).andReturn());
        try {
            assertThat(inCfbd.await(10, TimeUnit.SECONDS)).isTrue();
            MvcResult result = future.get(2, TimeUnit.SECONDS);
            assertThat(result.getResponse().getStatus()).isEqualTo(202);
            String jobId = jsonMapper.readTree(result.getResponse().getContentAsString()).path("jobId").asString();
            assertThat(jobId).isNotBlank();
            jobIds.add(jobId);
            ImportJob running = importJobRepository.findById(jobId).orElseThrow();
            assertThat(running.getStatus()).isEqualTo(ImportJob.RUNNING);
        } finally {
            requestFinished.countDown();
            future.get(10, TimeUnit.SECONDS);
            callers.shutdownNow();
        }

        String jobId = jobIds.get(jobIds.size() - 1);
        ImportJob done = awaitJob(jobId);
        assertThat(done.getStatus()).isEqualTo(ImportJob.SUCCEEDED);
        SeasonWeek stored = null;
        for (SeasonWeek found : seasonWeekRepository.findAll()) {
            if (seasonId.equals(found.getSeasonId())) {
                stored = found;
            }
        }
        SeasonWeek storedWeek = Objects.requireNonNull(stored, "week");
        assertThat(storedWeek.getBeginDate()).isEqualTo(monday.minusDays(1).toString());
        assertThat(LocalDate.parse(storedWeek.getBeginDate()).getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    void statusRequiresManagerAndTheWorkerStaysInternal() throws Exception {
        int year = uniqueYear();
        String seasonId = createSeason(year);
        when(cfbdClient.calendar(anyInt())).thenThrow(new CfbdClientException(429, CfbdClientException.RATE_LIMITED));

        mockMvc.perform(post("/api/manager/imports/matchups").contentType(MediaType.APPLICATION_JSON)
                .content("{\"seasonId\":\"" + seasonId + "\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/manager/imports/missing")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/manager/imports/matchups").cookie(player).contentType(MediaType.APPLICATION_JSON)
                .content("{\"seasonId\":\"" + seasonId + "\"}")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/imports/missing").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/manager/imports/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content("{\"seasonId\":\"missing-season\"}")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(SeasonService.NOT_FOUND));
        mockMvc.perform(post("/api/manager/imports/matchups").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content("{\"seasonId\":\"" + seasonId + "\",\"seasonType\":\"bowls\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(CfbdImportService.SEASON_TYPE_INVALID));

        ImportJob queued = cfbdImportService.enqueue(seasonId, null);
        String jobId = Objects.requireNonNull(queued.getId(), "jobId");
        jobIds.add(jobId);

        mockMvc.perform(post("/api/internal/jobs/cfbd-matchups").contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobId\":\"" + jobId + "\"}").cookie(manager).with(from(OUTSIDE)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cfbdClient);

        mockMvc.perform(post("/api/internal/jobs/cfbd-matchups").contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobId\":\"" + jobId + "\"}").header(InternalJobFilter.SECRET_HEADER, SECRET).with(from(OUTSIDE)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(CfbdClientException.RATE_LIMITED));
        verify(cfbdClient, times(3)).calendar(year);
        verify(cfbdPause, times(2)).pause(anyInt());

        mockMvc.perform(get("/api/manager/imports/" + jobId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ImportJob.FAILED))
                .andExpect(jsonPath("$.error").value(CfbdClientException.RATE_LIMITED));

        clearInvocations(cfbdClient);
        mockMvc.perform(post("/api/internal/jobs/cfbd-matchups").header(InternalJobFilter.SECRET_HEADER, SECRET)
                .with(from(OUTSIDE))).andExpect(status().isAccepted()).andExpect(jsonPath("$.job").value("cfbd-matchups"));
        verify(cfbdClient, never()).calendar(anyInt());

        String spec = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertThat(spec).contains("/api/manager/imports/matchups");
        assertThat(spec).contains("/api/manager/imports/{jobId}");
        assertThat(spec).contains("/api/internal/jobs/cfbd-matchups");
    }

    private ImportJob awaitJob(String jobId) throws InterruptedException {
        String id = Objects.requireNonNull(jobId, "jobId");
        for (int attempt = 0; attempt < 50; attempt++) {
            ImportJob job = importJobRepository.findById(id).orElseThrow();
            if (ImportJob.SUCCEEDED.equals(job.getStatus()) || ImportJob.FAILED.equals(job.getStatus())) {
                return job;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("import did not finish");
    }

    private String createSeason(int year) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/seasons").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                        {"season":"%s","beginDate":"%s-08-01","endDate":"%s-12-31","isCurrent":false}
                        """.formatted(year, year, year))).andExpect(status().isCreated()).andReturn();
        String id = jsonMapper.readTree(result.getResponse().getContentAsString()).path("id").asString();
        seasonIds.add(id);
        return id;
    }

    private static VerifiedIdentity identity(String uid, List<String> roles) {
        return new VerifiedIdentity(uid, uid + "@example.com", "Test User", "Test", "User", "google.com",
                RoleClaims.forRoles(roles));
    }

    private static RequestPostProcessor from(String remoteAddress) {
        String address = Objects.requireNonNull(remoteAddress, "remoteAddress");
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private static int uniqueYear() {
        return 7000 + ThreadLocalRandom.current().nextInt(1500);
    }
}
