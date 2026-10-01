package com.curleesoft.pickem.backend.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.Theme;
import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.PickRepository;
import com.curleesoft.pickem.backend.repository.RivalryRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.ThemeRepository;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;

class FirestoreSeederTests extends FirestoreEmulatorSupport {

    private static final String PROJECT_ID = "pickem-seed-" + UUID.randomUUID().toString().substring(0, 8);

    private static final Instant FIRST = Instant.parse("2026-09-30T15:00:00Z");

    private static final Instant SECOND = Instant.parse("2026-10-01T15:00:00Z");

    private static final List<String> COLLECTIONS = List.of(VenueRepository.COLLECTION, TeamRepository.COLLECTION,
            RivalryRepository.COLLECTION, SeasonRepository.COLLECTION, SeasonWeekRepository.COLLECTION,
            MatchupRepository.COLLECTION, ThemeRepository.COLLECTION, UserRepository.COLLECTION,
            PickRepository.COLLECTION);

    private static final List<String> PASSWORD_KEYS = List.of("password", "oldpass", "userpass", "confirmpass",
            "passwd", "passhash", "passwordhash");

    private Firestore firestore;

    @BeforeEach
    void openClient() throws Exception {
        String projectId = Objects.requireNonNull(PROJECT_ID, "projectId");
        String emulatorHost = Objects.requireNonNull(emulatorEndpoint(), "emulatorHost");
        firestore = FirestoreOptions.newBuilder().setProjectId(projectId).setEmulatorHost(emulatorHost)
                .setCredentials(new FirestoreOptions.EmulatorCredentials()).build().getService();
        clear();
    }

    @AfterEach
    void closeClient() throws Exception {
        if (firestore != null) {
            firestore.close();
        }
    }

    @Test
    void seedsReferenceDataTheAppCanRead() throws Exception {
        RecordingIdentity identity = new RecordingIdentity();
        identity.accounts.put("boss@example.com", new BootstrapAccount("uid-1", "boss@example.com", "Kenney", "Curlee"));
        Optional<String> uid = seeder(FIRST, identity).seed("boss@example.com");

        assertThat(uid).contains("uid-1");
        assertThat(identity.grants).isEqualTo(1);
        assertThat(count(VenueRepository.COLLECTION)).isEqualTo(SeedCatalog.venues().size());
        assertThat(count(TeamRepository.COLLECTION)).isEqualTo(16);
        assertThat(count(RivalryRepository.COLLECTION)).isEqualTo(SeedCatalog.rivalries().size());
        assertThat(count(SeasonRepository.COLLECTION)).isEqualTo(1);
        assertThat(count(SeasonWeekRepository.COLLECTION)).isEqualTo(SeedCatalog.WEEK_COUNT);
        assertThat(count(MatchupRepository.COLLECTION)).isEqualTo(SeedCatalog.matchups().size());
        assertThat(count(ThemeRepository.COLLECTION)).isEqualTo(18);
        assertThat(count(PickRepository.COLLECTION)).isZero();
        assertThat(count(UserRepository.COLLECTION)).isEqualTo(1);

        DocumentSnapshot seasonDoc = require(SeasonRepository.COLLECTION, "2026");
        assertThat(seasonDoc.getBoolean("isCurrent")).isTrue();
        assertThat(seasonDoc.contains("current")).isFalse();
        Season season = Objects.requireNonNull(seasonDoc.toObject(Season.class), "season");
        assertThat(season.isCurrent()).isTrue();
        assertThat(season.getBeginDate()).isEqualTo("2026-08-27");
        assertThat(season.getEndDate()).isEqualTo("2026-12-09");
        assertThat(season.getCreateUser()).isEqualTo(SeedCatalog.ACTOR);
        assertThat(season.getVersion()).isZero();

        Venue unknown = Objects.requireNonNull(require(VenueRepository.COLLECTION, "unknown").toObject(Venue.class),
                "unknown");
        assertThat(unknown.getCfbdVenueId()).isNull();

        for (SeedCatalog.TeamSeed expected : SeedCatalog.teams()) {
            Team team = Objects.requireNonNull(require(TeamRepository.COLLECTION, expected.id()).toObject(Team.class),
                    "team");
            assertThat(team.getTeamName()).isEqualTo(expected.name());
            assertThat(team.getCfbdTeamId()).isEqualTo(expected.cfbdTeamId());
            assertThat(team.getConferenceMember()).isTrue();
            assertThat(team.getHomeVenue()).isNotNull();
            Venue home = Objects.requireNonNull(
                    require(VenueRepository.COLLECTION, team.getHomeVenueId()).toObject(Venue.class), "home");
            assertThat(home.getCfbdVenueId()).isNotNull();
            assertThat(team.getHomeVenue().getName()).isEqualTo(home.getVenueName());
        }

        for (int weekNumber = 1; weekNumber <= SeedCatalog.WEEK_COUNT; weekNumber++) {
            SeasonWeek week = Objects.requireNonNull(
                    require(SeasonWeekRepository.COLLECTION, SeedCatalog.weekId(weekNumber)).toObject(SeasonWeek.class),
                    "week");
            LocalDate begin = LocalDate.parse(week.getBeginDate());
            LocalDate end = LocalDate.parse(week.getEndDate());
            assertThat(begin.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
            assertThat(end).isEqualTo(begin.plusDays(6));
            assertThat(begin).isEqualTo(SeedCatalog.weekBegin(weekNumber));
        }

        Matchup ironBowl = Objects.requireNonNull(
                require(MatchupRepository.COLLECTION, "2026-w6-alabama-at-auburn").toObject(Matchup.class), "ironBowl");
        assertThat(ironBowl.getRivalryName()).isEqualTo("Iron Bowl");
        assertThat(ironBowl.getHomeTeamScore()).isNull();
        assertThat(ironBowl.getWinningTeamId()).isNull();
        assertThat(ironBowl.getVenue().getName()).isEqualTo("Jordan-Hare Stadium");

        Matchup cocktail = Objects.requireNonNull(
                require(MatchupRepository.COLLECTION, "2026-w6-florida-at-georgia").toObject(Matchup.class), "cocktail");
        assertThat(cocktail.getRivalryName()).isEqualTo("World's Largest Outdoor Cocktail Party");

        Matchup thirdSaturday = Objects.requireNonNull(
                require(MatchupRepository.COLLECTION, "2026-w5-tennessee-at-alabama").toObject(Matchup.class),
                "thirdSaturday");
        assertThat(thirdSaturday.getRivalryName()).isEqualTo("Third Saturday in October");
        assertThat(thirdSaturday.getHomeTeam().getName()).isEqualTo("Alabama");
        assertThat(thirdSaturday.getHomeTeam().getSquad()).isEqualTo("Crimson Tide");
        assertThat(thirdSaturday.getWinningTeamId()).isEqualTo("alabama");
        assertThat(thirdSaturday.getHomeTeamScore()).isEqualTo(24);
        assertThat(thirdSaturday.getAwayTeamScore()).isEqualTo(17);

        Matchup unscored = Objects.requireNonNull(
                require(MatchupRepository.COLLECTION, "2026-w5-missouri-at-south-carolina").toObject(Matchup.class),
                "unscored");
        assertThat(unscored.getRivalryName()).isNull();
        assertThat(unscored.getWinningTeamId()).isNull();

        for (SeedCatalog.ThemeSeed expected : SeedCatalog.themes()) {
            Theme theme = Objects.requireNonNull(
                    require(ThemeRepository.COLLECTION, expected.key()).toObject(Theme.class), "theme");
            assertThat(theme.getThemePath()).isEqualTo(expected.key());
            assertThat(theme.getThemeName()).isEqualTo(expected.name());
            assertThat(theme.getPrimary()).isEqualTo(expected.primary());
            assertThat(theme.getSecondary()).isEqualTo(expected.secondary());
            assertThat(theme.getActive()).isTrue();
        }

        User manager = Objects.requireNonNull(require(UserRepository.COLLECTION, "uid-1").toObject(User.class),
                "manager");
        assertThat(manager.getEmailAddr()).isEqualTo("boss@example.com");
        assertThat(manager.getFirstName()).isEqualTo("Kenney");
        assertThat(manager.getLastName()).isEqualTo("Curlee");
        assertThat(manager.getNickName()).isEqualTo("Kenney");
        assertThat(manager.getThemeId()).isEqualTo("light");
        assertThat(manager.getRoles()).containsExactly(RoleClaims.PLAYER, RoleClaims.MANAGER);
        assertNoPasswordFields();
    }

    @Test
    void rerunKeepsAuditPicksAndAnEditedProfile() throws Exception {
        RecordingIdentity identity = new RecordingIdentity();
        identity.accounts.put("boss@example.com", new BootstrapAccount("uid-1", "boss@example.com", "Kenney", "Curlee"));
        seeder(FIRST, identity).seed("boss@example.com");

        String pickId = "uid-1__2026-w1-vanderbilt-at-alabama";
        Map<String, Object> pick = Objects.requireNonNull(
                Map.of("userId", "uid-1", "matchupId", "2026-w1-vanderbilt-at-alabama", "note", "keep"), "pick");
        document(PickRepository.COLLECTION, pickId).set(pick).get();
        Map<String, Object> extra = Objects.requireNonNull(Map.of("venueName", "Extra", "cityState", "Nowhere"),
                "extra");
        document(VenueRepository.COLLECTION, "extra").set(extra).get();
        firestore.collection(UserRepository.COLLECTION).document("uid-1")
                .update("nickName", "KC", "themeId", "dark", "firstName", "Ken").get();
        identity.accounts.put("boss@example.com",
                new BootstrapAccount("uid-1", "boss2@example.com", "New", "Name"));

        seeder(SECOND, identity).seed("boss@example.com");

        assertThat(count(VenueRepository.COLLECTION)).isEqualTo(SeedCatalog.venues().size() + 1);
        assertThat(count(TeamRepository.COLLECTION)).isEqualTo(16);
        assertThat(count(PickRepository.COLLECTION)).isEqualTo(1);
        assertThat(require(VenueRepository.COLLECTION, "extra").getString("venueName")).isEqualTo("Extra");
        assertThat(require(PickRepository.COLLECTION, pickId).getString("note")).isEqualTo("keep");

        Venue venue = Objects.requireNonNull(require(VenueRepository.COLLECTION, "bryant-denny").toObject(Venue.class),
                "venue");
        assertThat(venue.getCreateDate()).isEqualTo(FIRST);
        assertThat(venue.getCreateUser()).isEqualTo(SeedCatalog.ACTOR);
        assertThat(venue.getLastUpdateDate()).isEqualTo(SECOND);
        assertThat(venue.getLastUpdateUser()).isEqualTo(SeedCatalog.ACTOR);
        assertThat(venue.getVersion()).isEqualTo(1L);

        User manager = Objects.requireNonNull(require(UserRepository.COLLECTION, "uid-1").toObject(User.class),
                "manager");
        assertThat(manager.getEmailAddr()).isEqualTo("boss2@example.com");
        assertThat(manager.getFirstName()).isEqualTo("Ken");
        assertThat(manager.getLastName()).isEqualTo("Curlee");
        assertThat(manager.getNickName()).isEqualTo("KC");
        assertThat(manager.getThemeId()).isEqualTo("dark");
        assertThat(manager.getRoles()).containsExactly(RoleClaims.PLAYER, RoleClaims.MANAGER);
        assertThat(manager.getCreateDate()).isEqualTo(FIRST);
        assertThat(manager.getVersion()).isEqualTo(1L);
        assertThat(identity.grants).isEqualTo(2);
        Map<String, Object> claims = identity.claims.get("uid-1");
        assertThat(claims).hasSize(3);
        assertThat(claims).containsEntry(RoleClaims.ROLES, List.of(RoleClaims.PLAYER, RoleClaims.MANAGER));
        assertThat(claims).containsEntry(RoleClaims.PLAYER, true);
        assertThat(claims).containsEntry(RoleClaims.MANAGER, true);
        assertNoPasswordFields();
    }

    @Test
    void blankProfileFieldsAreFilledWithoutReplacingAudit() throws Exception {
        User existing = new User();
        existing.setId("uid-1");
        existing.setUid("uid-1");
        existing.setEmailAddr("old@example.com");
        existing.setFirstName(" ");
        existing.setLastName("");
        existing.setNickName("  ");
        existing.setThemeId("");
        existing.setRoles(List.of(RoleClaims.PLAYER));
        existing.setCreateDate(Instant.parse("2020-01-01T00:00:00Z"));
        existing.setCreateUser("google");
        existing.setLastUpdateDate(existing.getCreateDate());
        existing.setLastUpdateUser("google");
        existing.setVersion(3L);
        firestore.collection(UserRepository.COLLECTION).document("uid-1").set(existing).get();

        RecordingIdentity identity = new RecordingIdentity();
        identity.accounts.put("boss@example.com", new BootstrapAccount("uid-1", "boss@example.com", "Kenney", "Curlee"));
        seeder(FIRST, identity).seed("boss@example.com");

        User manager = Objects.requireNonNull(require(UserRepository.COLLECTION, "uid-1").toObject(User.class),
                "manager");
        assertThat(manager.getFirstName()).isEqualTo("Kenney");
        assertThat(manager.getLastName()).isEqualTo("Curlee");
        assertThat(manager.getNickName()).isEqualTo("Kenney");
        assertThat(manager.getThemeId()).isEqualTo(SeedCatalog.DEFAULT_THEME);
        assertThat(manager.getCreateUser()).isEqualTo("google");
        assertThat(manager.getCreateDate()).isEqualTo(Instant.parse("2020-01-01T00:00:00Z"));
        assertThat(manager.getVersion()).isEqualTo(4L);
        assertThat(manager.getRoles()).containsExactly(RoleClaims.PLAYER, RoleClaims.MANAGER);
    }

    @Test
    void missingBootstrapUserFailsAfterReferenceDataIsWritten() throws Exception {
        RecordingIdentity identity = new RecordingIdentity();
        identity.miss = true;
        FirestoreSeeder seeder = seeder(FIRST, identity);

        assertThatThrownBy(() -> seeder.seed("missing@example.com")).isInstanceOf(SeedFailedException.class)
                .hasMessageContaining("missing@example.com");
        assertThat(count(VenueRepository.COLLECTION)).isEqualTo(SeedCatalog.venues().size());
        assertThat(count(UserRepository.COLLECTION)).isZero();
        assertThat(count(PickRepository.COLLECTION)).isZero();
        assertThat(identity.grants).isZero();
        assertThat(identity.lookups).isEqualTo(1);
    }

    @Test
    void omittedEmailSkipsTheManager() throws Exception {
        RecordingIdentity identity = new RecordingIdentity();
        FirestoreSeeder seeder = seeder(FIRST, identity);

        assertThat(seeder.seed(null)).isEmpty();
        assertThat(seeder.seed("  ")).isEmpty();
        assertThat(count(TeamRepository.COLLECTION)).isEqualTo(16);
        assertThat(count(UserRepository.COLLECTION)).isZero();
        assertThat(identity.lookups).isZero();
        assertThat(identity.grants).isZero();
    }

    private FirestoreSeeder seeder(Instant instant, BootstrapIdentity identity) {
        return new FirestoreSeeder(firestore, Clock.fixed(instant, ZoneOffset.UTC), identity);
    }

    private DocumentSnapshot require(String collection, String id) throws Exception {
        DocumentSnapshot snapshot = document(collection, id).get().get();
        assertThat(snapshot.exists()).as(collection + "/" + id).isTrue();
        return snapshot;
    }

    private int count(String collection) throws Exception {
        return collection(collection).get().get().size();
    }

    private void clear() throws Exception {
        for (String collection : COLLECTIONS) {
            for (QueryDocumentSnapshot document : collection(collection).get().get().getDocuments()) {
                document.getReference().delete().get();
            }
        }
    }

    private void assertNoPasswordFields() throws Exception {
        for (String collection : COLLECTIONS) {
            for (QueryDocumentSnapshot document : collection(collection).get().get().getDocuments()) {
                assertNoPasswordFields(document.getData());
            }
        }
    }

    private CollectionReference collection(String name) {
        String collectionName = Objects.requireNonNull(name, "collection");
        return Objects.requireNonNull(firestore.collection(collectionName), "collection");
    }

    private DocumentReference document(String name, String id) {
        String documentId = Objects.requireNonNull(id, "id");
        return Objects.requireNonNull(collection(name).document(documentId), "document");
    }

    private static void assertNoPasswordFields(Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String normalized = entry.getKey().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            assertThat(PASSWORD_KEYS).doesNotContain(normalized);
            if (entry.getValue() instanceof Map<?, ?> nested) {
                Map<String, Object> child = new LinkedHashMap<>();
                for (Map.Entry<?, ?> item : nested.entrySet()) {
                    child.put(String.valueOf(item.getKey()), item.getValue());
                }
                assertNoPasswordFields(child);
            }
        }
    }

    private static final class RecordingIdentity implements BootstrapIdentity {

        private final Map<String, BootstrapAccount> accounts = new LinkedHashMap<>();

        private final Map<String, Map<String, Object>> claims = new LinkedHashMap<>();

        private boolean miss;

        private int lookups;

        private int grants;

        @Override
        public Optional<BootstrapAccount> findByEmail(String email) {
            lookups++;

            if (miss) {
                return Optional.empty();
            }

            return Optional.ofNullable(accounts.get(email));
        }

        @Override
        public void grantManagerAndPlayer(String uid) {
            grants++;
            claims.put(uid, RoleClaims.forRoles(List.of(RoleClaims.PLAYER, RoleClaims.MANAGER)));
        }
    }
}
