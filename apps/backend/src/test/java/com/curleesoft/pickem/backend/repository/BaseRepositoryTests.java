package com.curleesoft.pickem.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.model.snapshot.TeamSquadSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

@SpringBootTest
class BaseRepositoryTests extends FirestoreEmulatorSupport {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchupRepository matchupRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveStampsAuditFieldsAndStartsVersionAtZero() {
        Venue venue = newVenue("Bryant-Denny Stadium", "Tuscaloosa, AL");

        Venue saved = venueRepository.save(venue, "tester");

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getCreateUser()).isEqualTo("tester");
        assertThat(saved.getLastUpdateUser()).isEqualTo("tester");
        assertThat(saved.getCreateDate()).isNotNull();
        assertThat(saved.getLastUpdateDate()).isEqualTo(saved.getCreateDate());

        Venue loaded = venueRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getVenueName()).isEqualTo("Bryant-Denny Stadium");
        assertThat(loaded.getVersion()).isZero();
        assertThat(loaded.getCreateUser()).isEqualTo("tester");
    }

    @Test
    void saveIncrementsVersionAndPreservesCreateAudit() {
        Venue saved = venueRepository.save(newVenue("Sanford Stadium", "Athens, GA"), "creator");
        saved = venueRepository.findById(saved.getId()).orElseThrow();
        Instant createDate = saved.getCreateDate();
        saved.setCityState("Athens, Georgia");

        Venue updated = venueRepository.save(saved, "updater");

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getCreateUser()).isEqualTo("creator");
        assertThat(updated.getCreateDate()).isEqualTo(createDate);
        assertThat(updated.getLastUpdateUser()).isEqualTo("updater");
        assertThat(updated.getLastUpdateDate()).isAfterOrEqualTo(createDate);
        assertThat(updated.getCityState()).isEqualTo("Athens, Georgia");
    }

    @Test
    void saveRejectsStaleVersionInATransaction() {
        Venue saved = venueRepository.save(newVenue("Tiger Stadium", "Baton Rouge, LA"), "tester");
        saved.setVenueName("Death Valley");
        venueRepository.save(saved, "tester");

        saved.setVersion(0L);
        saved.setVenueName("stale write");

        assertThatThrownBy(() -> venueRepository.save(saved, "tester"))
                .isInstanceOf(StaleDocumentVersionException.class).hasMessageContaining(saved.getId());

        Venue current = venueRepository.findById(saved.getId()).orElseThrow();
        assertThat(current.getVenueName()).isEqualTo("Death Valley");
        assertThat(current.getVersion()).isEqualTo(1L);
    }

    @Test
    void findAllAndQueryAndDelete() {
        String unique = UUID.randomUUID().toString();
        Venue one = newVenue("Venue-" + unique + "-a", "City A");
        Venue two = newVenue("Venue-" + unique + "-b", "City B");
        venueRepository.save(one, "tester");
        venueRepository.save(two, "tester");

        List<Venue> all = venueRepository.findAll();
        assertThat(all).extracting(Venue::getVenueName).contains(one.getVenueName(), two.getVenueName());

        List<Venue> queried = venueRepository
                .query(collection -> collection.whereEqualTo("venueName", one.getVenueName()));
        assertThat(queried).hasSize(1);
        assertThat(queried.get(0).getCityState()).isEqualTo("City A");

        venueRepository.delete(one.getId());
        assertThat(venueRepository.findById(one.getId())).isEmpty();
    }

    @Test
    void savesSeasonCalendarDatesAsIso8601Strings() {
        Season season = new Season();
        season.setSeason("2024");
        season.setBeginDate("2024-08-29");
        season.setEndDate("2025-01-13");
        season.setCurrent(true);

        Season saved = seasonRepository.save(season, "tester");
        Season loaded = seasonRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getBeginDate()).isEqualTo("2024-08-29");
        assertThat(loaded.getEndDate()).isEqualTo("2025-01-13");
        assertThat(loaded.isCurrent()).isTrue();
    }

    @Test
    void savesTeamAndMatchupWithDenormalizedSnapshots() {
        Venue venue = venueRepository.save(newVenue("Neyland Stadium", "Knoxville, TN"), "tester");
        VenueSnapshot venueSnapshot = new VenueSnapshot(venue.getId(), venue.getVenueName(), venue.getCityState());

        Team home = new Team();
        home.setTeamName("Tennessee-" + UUID.randomUUID());
        home.setSquadName("Volunteers");
        home.setConferenceMember(true);
        home.setHomeVenueId(venue.getId());
        home.setHomeVenue(venueSnapshot);
        home.setCfbdTeamId(2633L);
        home = teamRepository.save(home, "tester");

        Team away = new Team();
        away.setTeamName("Alabama-" + UUID.randomUUID());
        away.setSquadName("Crimson Tide");
        away.setConferenceMember(true);
        away.setHomeVenueId(venue.getId());
        away.setHomeVenue(venueSnapshot);
        away.setCfbdTeamId(333L);
        away = teamRepository.save(away, "tester");

        Matchup matchup = new Matchup();
        matchup.setSeasonId("season-1");
        matchup.setSeasonWeekId("week-1");
        matchup.setWeekNumber(1);
        matchup.setMatchupDate("2024-10-19");
        matchup.setHomeTeamId(home.getId());
        matchup.setAwayTeamId(away.getId());
        matchup.setHomeTeam(new TeamSquadSnapshot(home.getId(), home.getTeamName(), home.getSquadName()));
        matchup.setAwayTeam(new TeamSquadSnapshot(away.getId(), away.getTeamName(), away.getSquadName()));
        matchup.setVenueId(venue.getId());
        matchup.setVenue(venueSnapshot);
        matchup.setRivalryName("Third Saturday in October");
        matchup.setCfbdGameId(401532447L);

        Matchup saved = matchupRepository.save(matchup, "tester");
        Matchup loaded = matchupRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getHomeTeam().getName()).isEqualTo(home.getTeamName());
        assertThat(loaded.getHomeTeam().getSquad()).isEqualTo("Volunteers");
        assertThat(loaded.getAwayTeam().getId()).isEqualTo(away.getId());
        assertThat(loaded.getVenue().getName()).isEqualTo("Neyland Stadium");
        assertThat(loaded.getRivalryName()).isEqualTo("Third Saturday in October");
        assertThat(loaded.getCfbdGameId()).isEqualTo(401532447L);
        assertThat(loaded.getHomeTeamScore()).isNull();
        assertThat(teamRepository.findById(home.getId()).orElseThrow().getHomeVenue().getCityState())
                .isEqualTo("Knoxville, TN");
    }

    @Test
    void savesUserWithClientSuppliedUidAsDocumentId() {
        String uid = "google-uid-" + UUID.randomUUID();
        User user = new User();
        user.setId(uid);
        user.setUid(uid);
        user.setEmailAddr("player@example.com");
        user.setFirstName("Kenney");
        user.setLastName("Curlee");
        user.setNickName("KC");
        user.setThemeId("light");
        user.setRoles(List.of("player"));

        User saved = userRepository.save(user, uid);
        assertThat(saved.getId()).isEqualTo(uid);
        assertThat(saved.getVersion()).isZero();

        User loaded = userRepository.findById(uid).orElseThrow();
        assertThat(loaded.getEmailAddr()).isEqualTo("player@example.com");
        assertThat(loaded.getRoles()).containsExactly("player");
        assertThat(loaded.getUid()).isEqualTo(uid);
    }

    private static Venue newVenue(String name, String cityState) {
        Venue venue = new Venue();
        venue.setVenueName(name);
        venue.setCityState(cityState);
        return venue;
    }
}
