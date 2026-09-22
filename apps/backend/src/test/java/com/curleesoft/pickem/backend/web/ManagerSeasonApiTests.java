package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.service.SeasonCalendar;
import com.curleesoft.pickem.backend.service.SeasonService;
import com.curleesoft.pickem.backend.service.SeasonWeekService;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ManagerSeasonApiTests extends FirestoreEmulatorSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private SeasonWeekRepository seasonWeekRepository;

    @Autowired
    private JsonMapper jsonMapper;

    private final List<String> seasonIds = new ArrayList<>();

    private final List<String> weekIds = new ArrayList<>();

    private String managerUid;

    private Cookie manager;

    @BeforeEach
    void signInManager() {
        firebase.clear();
        managerUid = "manager-" + UUID.randomUUID();
        manager = new Cookie(SessionCookies.NAME, firebase.issueCookie(identity(managerUid, List.of("manager"))));
    }

    @AfterEach
    void deleteFixtures() {
        for (String id : weekIds) {
            seasonWeekRepository.delete(id);
        }
        for (String id : seasonIds) {
            seasonRepository.delete(id);
        }
        weekIds.clear();
        seasonIds.clear();
    }

    @Test
    void seasonsAndWeeksRequireTheManagerRole() throws Exception {
        Cookie player = new Cookie(SessionCookies.NAME,
                firebase.issueCookie(identity("player-" + UUID.randomUUID(), List.of("player"))));
        String body = seasonBody("4101", "4101-08-05", "4102-01-07", false);

        mockMvc.perform(get("/api/manager/seasons")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/manager/seasons").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/manager/seasons/missing").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/manager/seasons/missing")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/manager/season-weeks")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/manager/season-weeks").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/manager/seasons").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/manager/seasons").cookie(player).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/season-weeks").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/manager/season-weeks/missing").cookie(player)).andExpect(status().isForbidden());
    }

    @Test
    void createSeasonStampsAuditAndRejectsInvalidOrDuplicateYears() throws Exception {
        String year = uniqueYear();
        LocalDate begin = LocalDate.of(Integer.parseInt(year), 8, 28);
        LocalDate end = LocalDate.of(Integer.parseInt(year) + 1, 1, 12);

        MvcResult created = mockMvc
                .perform(post("/api/manager/seasons").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(seasonBody(year, begin.toString(), end.toString(), false)))
                .andExpect(status().isCreated()).andExpect(header().string("Location", containsString("/api/manager/seasons/")))
                .andExpect(jsonPath("$.season").value(year)).andExpect(jsonPath("$.isCurrent").value(false))
                .andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.createUser").value(managerUid))
                .andExpect(jsonPath("$.lastUpdateUser").value(managerUid))
                .andExpect(jsonPath("$.createDate").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z"))).andReturn();

        String id = idOf(created);
        seasonIds.add(id);

        mockMvc.perform(get("/api/manager/seasons/" + id).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.beginDate").value(begin.toString()))
                .andExpect(jsonPath("$.endDate").value(end.toString()));

        mockMvc.perform(post("/api/manager/seasons").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(seasonBody(year, begin.toString(), end.toString(), false))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(SeasonService.NOT_UNIQUE));

        mockMvc.perform(post("/api/manager/seasons").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(seasonBody(year, end.toString(), end.plusYears(1).toString(), false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(SeasonCalendar.BEGIN_DATE_INVALID));

        String nextYear = String.valueOf(Integer.parseInt(year) + 1);
        mockMvc.perform(post("/api/manager/seasons").cookie(manager).contentType(MediaType.APPLICATION_JSON).content(
                seasonBody(year, begin.toString(), LocalDate.of(Integer.parseInt(nextYear) + 1, 1, 1).toString(), false)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(SeasonCalendar.END_DATE_INVALID));

        mockMvc.perform(get("/api/manager/seasons/missing-" + year).cookie(manager)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(SeasonService.NOT_FOUND));
    }

    @Test
    void updateRejectsAStaleVersionAndSearchFiltersExampleFields() throws Exception {
        String olderYear = uniqueYear();
        String newerYear = String.valueOf(Integer.parseInt(olderYear) + 1);
        LocalDate olderBegin = LocalDate.of(Integer.parseInt(olderYear), 8, 15);
        LocalDate newerBegin = LocalDate.of(Integer.parseInt(newerYear), 8, 16);
        String olderId = createSeason(olderYear, olderBegin, LocalDate.of(Integer.parseInt(olderYear), 12, 20), false);
        String newerId = createSeason(newerYear, newerBegin, LocalDate.of(Integer.parseInt(newerYear) + 1, 1, 10), true);

        MvcResult updated = mockMvc
                .perform(put("/api/manager/seasons/" + newerId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(versionedSeason(newerYear, newerBegin, LocalDate.of(Integer.parseInt(newerYear) + 1, 1, 18),
                                true, 0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.createUser").value(managerUid))
                .andExpect(jsonPath("$.endDate").value(LocalDate.of(Integer.parseInt(newerYear) + 1, 1, 18).toString()))
                .andReturn();

        mockMvc.perform(put("/api/manager/seasons/" + newerId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedSeason(newerYear, newerBegin, LocalDate.of(Integer.parseInt(newerYear) + 1, 1, 18), true,
                        0)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(containsString("Stale version")));

        mockMvc.perform(get("/api/manager/seasons").cookie(manager).param("season", olderYear))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(olderId))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/manager/seasons").cookie(manager).param("beginDate", newerBegin.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(newerId));

        mockMvc.perform(get("/api/manager/seasons").cookie(manager).param("isCurrent", "true").param("season", newerYear))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(newerId));

        String listed = mockMvc.perform(get("/api/manager/seasons").cookie(manager)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode seasons = jsonMapper.readTree(listed);
        assertThat(indexOf(seasons, olderId)).isNotNegative();
        assertThat(indexOf(seasons, newerId)).isGreaterThan(indexOf(seasons, olderId));
        assertThat(jsonMapper.readTree(updated.getResponse().getContentAsString()).path("version").asInt()).isEqualTo(1);
    }

    @Test
    void deleteRemovesTheSeasonAndCurrentUsesTheFlag() throws Exception {
        String year = uniqueYear();
        String id = createSeason(year, LocalDate.of(Integer.parseInt(year), 9, 3),
                LocalDate.of(Integer.parseInt(year), 12, 17), false);

        mockMvc.perform(delete("/api/manager/seasons/" + id).cookie(manager)).andExpect(status().isNoContent());
        seasonIds.remove(id);
        mockMvc.perform(get("/api/manager/seasons/" + id).cookie(manager)).andExpect(status().isNotFound());

        seasonRepository.findAll().stream().filter(season -> "9999".equals(season.getSeason()))
                .forEach(season -> seasonRepository.delete(season.getId()));
        createSeason("9999", LocalDate.parse("9999-01-15"), LocalDate.parse("9999-12-15"), true);

        mockMvc.perform(get("/api/manager/seasons/current").cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isCurrent").value(true));
    }

    @Test
    void manualWeeksFollowThursdayBoundsAndAreQueryableBySeason() throws Exception {
        String year = uniqueYear();
        int seasonYear = Integer.parseInt(year);
        LocalDate seasonBegin = firstThursdayOnOrAfter(LocalDate.of(seasonYear, 8, 1));
        String seasonId = createSeason(year, seasonBegin, LocalDate.of(seasonYear + 1, 1, 15), false);
        LocalDate weekOneBegin = seasonBegin;
        LocalDate weekOneEnd = seasonBegin.plusDays(6);
        LocalDate weekTwoBegin = seasonBegin.plusDays(7);
        LocalDate weekTwoEnd = seasonBegin.plusDays(13);

        String weekTwo = createWeek(seasonId, 2, weekTwoBegin, weekTwoEnd);
        String weekOne = createWeek(seasonId, 1, weekOneBegin, weekOneEnd);

        mockMvc.perform(get("/api/manager/season-weeks").cookie(manager).param("seasonId", seasonId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(weekOne))
                .andExpect(jsonPath("$[0].weekNumber").value(1)).andExpect(jsonPath("$[1].id").value(weekTwo))
                .andExpect(jsonPath("$[1].weekNumber").value(2));

        mockMvc.perform(get("/api/manager/season-weeks/" + weekOne).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.beginDate").value(weekOneBegin.toString()))
                .andExpect(jsonPath("$.endDate").value(weekOneEnd.toString()))
                .andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.createUser").value(managerUid));

        mockMvc.perform(get("/api/manager/season-weeks").cookie(manager).param("seasonId", seasonId).param("weekNumber", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(weekTwo))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/manager/season-weeks").cookie(manager).param("seasonId", seasonId).param("beginDate",
                weekOneBegin.toString())).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(weekOne));

        mockMvc.perform(post("/api/manager/season-weeks").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(weekBody(seasonId, 1, weekOneBegin, weekOneEnd))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(SeasonWeekService.NOT_UNIQUE));

        mockMvc.perform(post("/api/manager/season-weeks").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(weekBody(seasonId, 1, weekOneBegin.plusDays(1), weekOneBegin.plusDays(7))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(SeasonCalendar.WEEK_BEGIN_DATE_INVALID));

        mockMvc.perform(post("/api/manager/season-weeks").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(weekBody(seasonId, 1, weekOneBegin, weekOneBegin.plusDays(5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(SeasonCalendar.WEEK_END_DATE_INVALID));

        mockMvc.perform(post("/api/manager/season-weeks").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(weekBody(seasonId, 2, weekOneBegin, weekOneEnd))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(SeasonCalendar.WEEK_BEGIN_ALIGNMENT_INVALID));

        mockMvc.perform(post("/api/manager/season-weeks").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(weekBody("missing-season", 1, weekOneBegin, weekOneEnd))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(SeasonWeekService.SEASON_NOT_FOUND));

        mockMvc.perform(put("/api/manager/season-weeks/" + weekTwo).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedWeek(seasonId, 2, weekTwoBegin, weekTwoEnd, 0))).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        LocalDate importedBegin = weekOneBegin.plusDays(4);
        assertThat(importedBegin.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        SeasonWeek imported = new SeasonWeek();
        imported.setSeasonId(seasonId);
        imported.setWeekNumber(15);
        imported.setBeginDate(importedBegin.toString());
        imported.setEndDate(importedBegin.plusDays(6).toString());
        imported = seasonWeekRepository.save(imported, "cfbd-import");
        weekIds.add(imported.getId());

        mockMvc.perform(post("/api/manager/season-weeks").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(weekBody(seasonId, 15, importedBegin, importedBegin.plusDays(6))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(SeasonCalendar.WEEK_BEGIN_DATE_INVALID));

        mockMvc.perform(get("/api/manager/season-weeks/" + imported.getId()).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.beginDate").value(importedBegin.toString()))
                .andExpect(jsonPath("$.createUser").value("cfbd-import"));

        String spec = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertThat(spec).contains("/api/manager/seasons");
        assertThat(spec).contains("/api/manager/season-weeks");
    }

    private String createSeason(String year, LocalDate begin, LocalDate end, boolean current) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/seasons").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(seasonBody(year, begin.toString(), end.toString(), current)))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        seasonIds.add(id);
        return id;
    }

    private String createWeek(String seasonId, int weekNumber, LocalDate begin, LocalDate end) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/season-weeks").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(weekBody(seasonId, weekNumber, begin, end)))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        weekIds.add(id);
        return id;
    }

    private String idOf(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString()).path("id").asString();
    }

    private static int indexOf(JsonNode seasons, String id) {
        for (int i = 0; i < seasons.size(); i++) {
            if (id.equals(seasons.get(i).path("id").asString())) {
                return i;
            }
        }
        return -1;
    }

    private static String seasonBody(String year, String begin, String end, boolean current) {
        return """
                {"season":"%s","beginDate":"%s","endDate":"%s","isCurrent":%s,"createUser":"hacker","version":9}
                """.formatted(year, begin, end, current);
    }

    private static String versionedSeason(String year, LocalDate begin, LocalDate end, boolean current, long version) {
        return """
                {"season":"%s","beginDate":"%s","endDate":"%s","isCurrent":%s,"version":%d}
                """.formatted(year, begin, end, current, version);
    }

    private static String weekBody(String seasonId, int weekNumber, LocalDate begin, LocalDate end) {
        return """
                {"seasonId":"%s","weekNumber":%d,"beginDate":"%s","endDate":"%s","createUser":"hacker","version":9}
                """.formatted(seasonId, weekNumber, begin, end);
    }

    private static String versionedWeek(String seasonId, int weekNumber, LocalDate begin, LocalDate end, long version) {
        return """
                {"seasonId":"%s","weekNumber":%d,"beginDate":"%s","endDate":"%s","version":%d}
                """.formatted(seasonId, weekNumber, begin, end, version);
    }

    private static LocalDate firstThursdayOnOrAfter(LocalDate date) {
        LocalDate cursor = date;
        while (cursor.getDayOfWeek() != DayOfWeek.THURSDAY) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }

    private static String uniqueYear() {
        return String.valueOf(4200 + ThreadLocalRandom.current().nextInt(500));
    }

    private static VerifiedIdentity identity(String uid, List<String> roles) {
        return new VerifiedIdentity(uid, uid + "@example.com", "Test User", "Test", "User", "google.com",
                RoleClaims.forRoles(roles));
    }
}
