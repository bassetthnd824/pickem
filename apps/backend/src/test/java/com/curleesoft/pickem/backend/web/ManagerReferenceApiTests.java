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

import com.curleesoft.pickem.backend.repository.RivalryRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.curleesoft.pickem.backend.security.FakeFirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.SessionCookies;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.service.RivalryService;
import com.curleesoft.pickem.backend.service.TeamService;
import com.curleesoft.pickem.backend.service.VenueService;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ManagerReferenceApiTests extends FirestoreEmulatorSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeFirebaseIdentityClient firebase;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private RivalryRepository rivalryRepository;

    @Autowired
    private JsonMapper jsonMapper;

    private final List<String> venueIds = new ArrayList<>();

    private final List<String> teamIds = new ArrayList<>();

    private final List<String> rivalryIds = new ArrayList<>();

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
        for (String id : rivalryIds) {
            rivalryRepository.delete(id);
        }
        for (String id : teamIds) {
            teamRepository.delete(id);
        }
        for (String id : venueIds) {
            venueRepository.delete(id);
        }
        rivalryIds.clear();
        teamIds.clear();
        venueIds.clear();
    }

    @Test
    void referenceDataRequiresTheManagerRole() throws Exception {
        Cookie player = new Cookie(SessionCookies.NAME,
                firebase.issueCookie(identity("player-" + UUID.randomUUID(), List.of("player"))));

        mockMvc.perform(get("/api/manager/venues")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/manager/venues").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/manager/venues/missing").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/manager/venues/missing")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/manager/teams")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/manager/teams").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/manager/teams/missing")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/manager/rivalries")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/manager/rivalries").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/manager/rivalries/missing").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/manager/venues").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/teams/missing").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/manager/teams").cookie(player).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/manager/rivalries/missing").cookie(player)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/rivalries").cookie(player)).andExpect(status().isForbidden());
    }

    @Test
    void venuesRequireNameAndCityAndKeepCfbdIdsUnique() throws Exception {
        String token = token();
        long externalId = externalId();
        String knoxville = "Knoxville " + token;
        String athens = "Athens " + token;

        MvcResult created = mockMvc
                .perform(post("/api/manager/venues").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(venueBody("Neyland " + token, knoxville, externalId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/manager/venues/")))
                .andExpect(jsonPath("$.venueName").value("Neyland " + token))
                .andExpect(jsonPath("$.cfbdVenueId").value(externalId)).andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createUser").value(managerUid))
                .andExpect(jsonPath("$.lastUpdateUser").value(managerUid))
                .andExpect(jsonPath("$.createDate").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z"))).andReturn();

        String neylandId = idOf(created);
        venueIds.add(neylandId);
        String sanfordId = createVenue("Sanford " + token, athens, null);

        mockMvc.perform(get("/api/manager/venues/" + neylandId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cityState").value(knoxville));

        mockMvc.perform(post("/api/manager/venues").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(venueBody("Other " + token, "Other " + token, externalId))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(VenueService.CFBD_NOT_UNIQUE));

        createVenue("Untitled " + token, "Nowhere " + token, null);

        mockMvc.perform(post("/api/manager/venues").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(venueBody(" ", knoxville, null))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("venueName")));

        mockMvc.perform(post("/api/manager/venues").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(venueBody("x".repeat(61), knoxville, null))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("venueName")));

        mockMvc.perform(post("/api/manager/venues").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(venueBody("Short " + token, " ", null))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("cityState")));

        mockMvc.perform(get("/api/manager/venues").cookie(manager).param("cfbdVenueId", "nope"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Request is invalid"));

        mockMvc.perform(get("/api/manager/venues").cookie(manager).param("venueName", "neyland " + token.toLowerCase()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(neylandId))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/manager/venues").cookie(manager).param("cityState", token)
                .param("cfbdVenueId", Long.toString(externalId))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(neylandId)).andExpect(jsonPath("$[1]").doesNotExist());

        String listed = mockMvc.perform(get("/api/manager/venues").cookie(manager).param("cityState", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode venues = jsonMapper.readTree(listed);
        assertThat(indexOf(venues, neylandId)).isLessThan(indexOf(venues, sanfordId));

        mockMvc.perform(put("/api/manager/venues/" + neylandId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedVenue("Neyland " + token, knoxville, externalId, 4))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Stale version")));

        mockMvc.perform(put("/api/manager/venues/" + neylandId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedVenue("Neyland Stadium " + token, knoxville, externalId, 0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.createUser").value(managerUid))
                .andExpect(jsonPath("$.venueName").value("Neyland Stadium " + token));

        mockMvc.perform(get("/api/manager/venues/missing-" + token).cookie(manager)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(VenueService.NOT_FOUND));

        mockMvc.perform(delete("/api/manager/venues/" + sanfordId).cookie(manager)).andExpect(status().isNoContent());
        venueIds.remove(sanfordId);
        mockMvc.perform(get("/api/manager/venues/" + sanfordId).cookie(manager)).andExpect(status().isNotFound());
    }

    @Test
    void teamsEmbedTheHomeVenueAndRejectDuplicateNamesAndCfbdIds() throws Exception {
        String token = token();
        long externalId = externalId();
        String venueId = createVenue("Bryant-Denny " + token, "Tuscaloosa " + token, externalId());
        String laterVenueId = createVenue("Sanford " + token, "Athens " + token, null);

        MvcResult created = mockMvc
                .perform(post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(teamBody("Alabama " + token, "Crimson Tide", true, venueId, externalId, true)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/manager/teams/")))
                .andExpect(jsonPath("$.teamName").value("Alabama " + token))
                .andExpect(jsonPath("$.squadName").value("Crimson Tide")).andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createUser").value(managerUid))
                .andExpect(jsonPath("$.homeVenueId").value(venueId)).andExpect(jsonPath("$.homeVenue.id").value(venueId))
                .andExpect(jsonPath("$.homeVenue.name").value("Bryant-Denny " + token))
                .andExpect(jsonPath("$.homeVenue.cityState").value("Tuscaloosa " + token))
                .andExpect(jsonPath("$.cfbdTeamId").value(externalId)).andReturn();

        String alabamaId = idOf(created);
        teamIds.add(alabamaId);

        mockMvc.perform(get("/api/manager/teams/" + alabamaId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.homeVenue.name").value("Bryant-Denny " + token))
                .andExpect(jsonPath("$.conferenceMember").value(true));

        mockMvc.perform(post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(teamBody("  Alabama " + token + "  ", "Tide", false, venueId, null, false)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(TeamService.NAME_NOT_UNIQUE));

        mockMvc.perform(post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(teamBody("Georgia " + token, "Bulldogs", true, venueId, externalId, false)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(TeamService.CFBD_NOT_UNIQUE));

        String georgiaId = createTeam("Georgia " + token, "Bulldogs", true, laterVenueId, null);
        String vanderbiltId = createTeam("Vanderbilt " + token, "Commodores", false, venueId, null);

        mockMvc.perform(post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(teamBody("Missing " + token, "Squad", true, "missing-" + token, null, false)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(VenueService.NOT_FOUND));

        mockMvc.perform(post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(teamBody(" ", "Squad", true, venueId, null, false))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("teamName")));

        mockMvc.perform(post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(teamBody("Name " + token, "s".repeat(41), true, venueId, null, false)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(containsString("squadName")));

        mockMvc.perform(post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON).content("""
                {"teamName":"No Flag %s","squadName":"Squad","homeVenueId":"%s"}
                """.formatted(token, venueId))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("conferenceMember")));

        mockMvc.perform(put("/api/manager/venues/" + venueId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedVenue("Bryant-Denny Stadium " + token, "Tuscaloosa, AL " + token, null, 0)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manager/teams/" + alabamaId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.homeVenue.name").value("Bryant-Denny " + token));

        mockMvc.perform(put("/api/manager/teams/" + alabamaId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedTeam("Alabama " + token, "Crimson Tide", true, venueId, externalId, 0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.homeVenue.name").value("Bryant-Denny Stadium " + token))
                .andExpect(jsonPath("$.homeVenue.cityState").value("Tuscaloosa, AL " + token))
                .andExpect(jsonPath("$.createUser").value(managerUid));

        mockMvc.perform(get("/api/manager/teams/" + georgiaId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.homeVenue.name").value("Sanford " + token));

        mockMvc.perform(put("/api/manager/teams/" + alabamaId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedTeam("Alabama " + token, "Crimson Tide", true, laterVenueId, externalId, 0)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(containsString("Stale version")));

        mockMvc.perform(put("/api/manager/teams/" + alabamaId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedTeam("Alabama " + token, "Crimson Tide", true, laterVenueId, null, 1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.homeVenueId").value(laterVenueId))
                .andExpect(jsonPath("$.homeVenue.name").value("Sanford " + token));

        String reused = createTeam("Auburn " + token, "Tigers", true, venueId, externalId);
        assertThat(reused).isNotBlank();

        String cleared = mockMvc.perform(get("/api/manager/teams/" + alabamaId).cookie(manager)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(jsonMapper.readTree(cleared).path("cfbdTeamId").isNull()).isTrue();

        mockMvc.perform(delete("/api/manager/venues/" + venueId).cookie(manager)).andExpect(status().isNoContent());
        venueIds.remove(venueId);
        mockMvc.perform(get("/api/manager/teams/" + vanderbiltId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.homeVenue.name").value("Bryant-Denny " + token));

        mockMvc.perform(get("/api/manager/teams/missing-" + token).cookie(manager)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(TeamService.NOT_FOUND));

        mockMvc.perform(delete("/api/manager/teams/" + alabamaId).cookie(manager)).andExpect(status().isNoContent());
        teamIds.remove(alabamaId);
        mockMvc.perform(get("/api/manager/teams/" + alabamaId).cookie(manager)).andExpect(status().isNotFound());
    }

    @Test
    void conferenceMemberQueryReplacesTheConferenceTeamList() throws Exception {
        String token = token();
        String venueId = createVenue("Conference Venue " + token, "City " + token, null);
        String alphaId = createTeam("Alpha " + token, "Squad A " + token, true, venueId, externalId());
        String midId = createTeam("Mid " + token, "Squad M " + token, true, venueId, null);
        String otherId = createTeam("Other " + token, "Squad Z " + token, false, venueId, null);

        mockMvc.perform(get("/api/manager/teams").cookie(manager).param("conferenceMember", "true").param("teamName", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(alphaId)).andExpect(jsonPath("$[1].id").value(midId));

        mockMvc.perform(get("/api/manager/teams").cookie(manager).param("conferenceMember", "false").param("squadName",
                "squad z " + token.toLowerCase())).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(otherId));

        mockMvc.perform(get("/api/manager/teams").cookie(manager).param("homeVenueId", venueId).param("teamName", "Alpha"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(alphaId))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void rivalriesRequireTwoDifferentTeamsAndEmbedSnapshots() throws Exception {
        String token = token();
        String venueId = createVenue("Rivalry Venue " + token, "City " + token, null);
        String alabamaId = createTeam("Alabama " + token, "Crimson Tide", true, venueId, null);
        String tennesseeId = createTeam("Tennessee " + token, "Volunteers", true, venueId, null);
        String georgiaId = createTeam("Georgia " + token, "Bulldogs", true, venueId, null);

        MvcResult created = mockMvc
                .perform(post("/api/manager/rivalries").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(rivalryBody("Third Saturday " + token, alabamaId, tennesseeId, true)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/manager/rivalries/")))
                .andExpect(jsonPath("$.rivalryName").value("Third Saturday " + token))
                .andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.createUser").value(managerUid))
                .andExpect(jsonPath("$.team1Id").value(alabamaId)).andExpect(jsonPath("$.team2Id").value(tennesseeId))
                .andExpect(jsonPath("$.team1.id").value(alabamaId))
                .andExpect(jsonPath("$.team1.name").value("Alabama " + token))
                .andExpect(jsonPath("$.team2.name").value("Tennessee " + token)).andReturn();

        String rivalryId = idOf(created);
        rivalryIds.add(rivalryId);
        String deepSouthId = createRivalry("Deep South " + token, alabamaId, georgiaId);

        mockMvc.perform(post("/api/manager/rivalries").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(rivalryBody("Same " + token, alabamaId, alabamaId, false))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(RivalryService.TEAMS_EQUAL));

        mockMvc.perform(post("/api/manager/rivalries").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(rivalryBody("Missing " + token, alabamaId, "missing-" + token, false)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(TeamService.NOT_FOUND));

        mockMvc.perform(post("/api/manager/rivalries").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(rivalryBody(" ", alabamaId, tennesseeId, false))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("rivalryName")));

        mockMvc.perform(post("/api/manager/rivalries").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(rivalryBody("n".repeat(61), alabamaId, tennesseeId, false))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("rivalryName")));

        mockMvc.perform(get("/api/manager/rivalries").cookie(manager).param("rivalryName", "third saturday " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(rivalryId))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/manager/rivalries").cookie(manager).param("team1Id", tennesseeId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(rivalryId))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/manager/rivalries").cookie(manager).param("team2Id", alabamaId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/manager/rivalries").cookie(manager).param("team2Id", georgiaId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(deepSouthId))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(put("/api/manager/teams/" + alabamaId).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(versionedTeam("Alabama Crimson " + token, "Crimson Tide", true, venueId, null, 0)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manager/rivalries/" + rivalryId).cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.team1.name").value("Alabama " + token));

        mockMvc.perform(put("/api/manager/rivalries/" + rivalryId).cookie(manager)
                .contentType(MediaType.APPLICATION_JSON)
                .content(versionedRivalry("Third Saturday " + token, tennesseeId, georgiaId, 0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.team1.name").value("Tennessee " + token))
                .andExpect(jsonPath("$.team2.name").value("Georgia " + token))
                .andExpect(jsonPath("$.createUser").value(managerUid));

        mockMvc.perform(put("/api/manager/rivalries/" + rivalryId).cookie(manager)
                .contentType(MediaType.APPLICATION_JSON)
                .content(versionedRivalry("Third Saturday " + token, tennesseeId, georgiaId, 0)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(containsString("Stale version")));

        mockMvc.perform(get("/api/manager/rivalries/missing-" + token).cookie(manager)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(RivalryService.NOT_FOUND));

        mockMvc.perform(delete("/api/manager/rivalries/" + rivalryId).cookie(manager)).andExpect(status().isNoContent());
        rivalryIds.remove(rivalryId);
        mockMvc.perform(get("/api/manager/rivalries/" + rivalryId).cookie(manager)).andExpect(status().isNotFound());

        String spec = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertThat(spec).contains("/api/manager/venues");
        assertThat(spec).contains("/api/manager/teams");
        assertThat(spec).contains("/api/manager/rivalries");
    }

    private String createVenue(String name, String cityState, Long cfbdVenueId) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/venues").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(venueBody(name, cityState, cfbdVenueId)))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        venueIds.add(id);
        return id;
    }

    private String createTeam(String name, String squad, boolean conferenceMember, String venueId, Long cfbdTeamId)
            throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/teams").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(teamBody(name, squad, conferenceMember, venueId, cfbdTeamId, false)))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        teamIds.add(id);
        return id;
    }

    private String createRivalry(String name, String team1Id, String team2Id) throws Exception {
        MvcResult result = mockMvc
                .perform(post("/api/manager/rivalries").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(rivalryBody(name, team1Id, team2Id, false)))
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(result);
        rivalryIds.add(id);
        return id;
    }

    private String idOf(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString()).path("id").asString();
    }

    private static int indexOf(JsonNode documents, String id) {
        for (int i = 0; i < documents.size(); i++) {
            if (id.equals(documents.get(i).path("id").asString())) {
                return i;
            }
        }
        return -1;
    }

    private static String venueBody(String name, String cityState, Long cfbdVenueId) {
        return """
                {"venueName":"%s","cityState":"%s","cfbdVenueId":%s,"createUser":"hacker","version":9}
                """.formatted(escape(name), escape(cityState), jsonNumber(cfbdVenueId));
    }

    private static String versionedVenue(String name, String cityState, Long cfbdVenueId, long version) {
        return """
                {"venueName":"%s","cityState":"%s","cfbdVenueId":%s,"version":%d}
                """.formatted(escape(name), escape(cityState), jsonNumber(cfbdVenueId), version);
    }

    private static String teamBody(String name, String squad, boolean conferenceMember, String venueId, Long cfbdTeamId,
            boolean wrongSnapshot) {
        String snapshot = wrongSnapshot ? ",\"homeVenue\":{\"id\":\"wrong\",\"name\":\"Wrong\",\"cityState\":\"Nowhere\"}"
                : "";
        return """
                {"teamName":"%s","squadName":"%s","conferenceMember":%s,"homeVenueId":"%s","cfbdTeamId":%s%s,"createUser":"hacker","version":9}
                """.formatted(escape(name), escape(squad), conferenceMember, escape(venueId), jsonNumber(cfbdTeamId),
                snapshot);
    }

    private static String versionedTeam(String name, String squad, boolean conferenceMember, String venueId,
            Long cfbdTeamId, long version) {
        return """
                {"teamName":"%s","squadName":"%s","conferenceMember":%s,"homeVenueId":"%s","cfbdTeamId":%s,"version":%d}
                """.formatted(escape(name), escape(squad), conferenceMember, escape(venueId), jsonNumber(cfbdTeamId),
                version);
    }

    private static String rivalryBody(String name, String team1Id, String team2Id, boolean wrongSnapshot) {
        String snapshots = wrongSnapshot
                ? ",\"team1\":{\"id\":\"wrong\",\"name\":\"Wrong\"},\"team2\":{\"id\":\"wrong\",\"name\":\"Wrong\"}"
                : "";
        return """
                {"rivalryName":"%s","team1Id":"%s","team2Id":"%s"%s,"createUser":"hacker","version":9}
                """.formatted(escape(name), escape(team1Id), escape(team2Id), snapshots);
    }

    private static String versionedRivalry(String name, String team1Id, String team2Id, long version) {
        return """
                {"rivalryName":"%s","team1Id":"%s","team2Id":"%s","version":%d}
                """.formatted(escape(name), escape(team1Id), escape(team2Id), version);
    }

    private static String jsonNumber(Long value) {
        return value == null ? "null" : value.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String token() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static long externalId() {
        return ThreadLocalRandom.current().nextLong(3_000_000_000L, 9_000_000_000_000L);
    }

    private static VerifiedIdentity identity(String uid, List<String> roles) {
        return new VerifiedIdentity(uid, uid + "@example.com", "Test User", "Test", "User", "google.com",
                RoleClaims.forRoles(roles));
    }
}
