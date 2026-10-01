package com.curleesoft.pickem.backend.seed;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.curleesoft.pickem.backend.model.AuditableDocument;
import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Rivalry;
import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.Theme;
import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.model.snapshot.TeamNameSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.TeamSquadSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.RivalryRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.ThemeRepository;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

/**
 * Upserts seed documents by stable id. Does not delete extra documents, picks,
 * or any password field. A re-run keeps {@code createDate} and {@code createUser}
 * and increments {@code version}.
 */
public final class FirestoreSeeder {

    private final Firestore firestore;

    private final Clock clock;

    private final BootstrapIdentity identity;

    public FirestoreSeeder(Firestore firestore, Clock clock, BootstrapIdentity identity) {
        this.firestore = Objects.requireNonNull(firestore, "firestore");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.identity = Objects.requireNonNull(identity, "identity");
    }

    /**
     * Writes reference data, then grants the bootstrap manager when {@code email}
     * is set. Returns that user's uid, or empty when the email was omitted.
     */
    public Optional<String> seed(String email) {
        writeReferenceData();

        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(writeManager(email.trim()));
    }

    private void writeReferenceData() {
        Map<String, SeedCatalog.VenueSeed> venues = new HashMap<>();

        for (SeedCatalog.VenueSeed venue : SeedCatalog.venues()) {
            venues.put(venue.id(), venue);
            save(VenueRepository.COLLECTION, venueDocument(venue));
        }

        Map<String, SeedCatalog.TeamSeed> teams = new HashMap<>();

        for (SeedCatalog.TeamSeed team : SeedCatalog.teams()) {
            teams.put(team.id(), team);
            save(TeamRepository.COLLECTION, teamDocument(team, venues));
        }

        for (SeedCatalog.RivalrySeed rivalry : SeedCatalog.rivalries()) {
            save(RivalryRepository.COLLECTION, rivalryDocument(rivalry, teams));
        }

        save(SeasonRepository.COLLECTION, seasonDocument());

        for (int weekNumber = 1; weekNumber <= SeedCatalog.WEEK_COUNT; weekNumber++) {
            save(SeasonWeekRepository.COLLECTION, weekDocument(weekNumber));
        }

        for (SeedCatalog.MatchupSeed matchup : SeedCatalog.matchups()) {
            save(MatchupRepository.COLLECTION, matchupDocument(matchup, teams, venues));
        }

        for (SeedCatalog.ThemeSeed theme : SeedCatalog.themes()) {
            save(ThemeRepository.COLLECTION, themeDocument(theme));
        }
    }

    private String writeManager(String email) {
        BootstrapAccount account = identity.findByEmail(email)
                .orElseThrow(() -> new SeedFailedException("No Firebase user exists for " + email
                        + ". Sign in with that Google account, then run the seed again."));

        if (account.uid() == null || account.uid().isBlank()) {
            throw new SeedFailedException("Firebase user " + email + " has no uid");
        }

        String uid = account.uid();
        identity.grantManagerAndPlayer(uid);
        saveUser(account);
        return uid;
    }

    private void saveUser(BootstrapAccount account) {
        String uid = account.uid();
        DocumentReference reference = document(UserRepository.COLLECTION, uid);
        DocumentSnapshot snapshot = get(reference);
        User user = new User();
        user.setId(uid);
        user.setUid(uid);
        user.setEmailAddr(account.email());
        user.setRoles(List.of(RoleClaims.PLAYER, RoleClaims.MANAGER));

        if (snapshot.exists()) {
            User stored = snapshot.toObject(User.class);

            if (stored == null) {
                throw new SeedFailedException("Failed to map users/" + uid);
            }

            user.setFirstName(keep(stored.getFirstName(), account.firstName()));
            user.setLastName(keep(stored.getLastName(), account.lastName()));
            user.setNickName(keep(stored.getNickName(), account.firstName()));
            user.setThemeId(keep(stored.getThemeId(), SeedCatalog.DEFAULT_THEME));
            applyExistingAudit(user, stored);

        } else {
            user.setFirstName(account.firstName());
            user.setLastName(account.lastName());
            user.setNickName(account.firstName());
            user.setThemeId(SeedCatalog.DEFAULT_THEME);
            applyNewAudit(user);
        }

        set(reference, user);
    }

    private static Venue venueDocument(SeedCatalog.VenueSeed seed) {
        Venue venue = new Venue();
        venue.setId(seed.id());
        venue.setVenueName(seed.name());
        venue.setCityState(seed.cityState());
        venue.setCfbdVenueId(seed.cfbdVenueId());
        return venue;
    }

    private static Team teamDocument(SeedCatalog.TeamSeed seed, Map<String, SeedCatalog.VenueSeed> venues) {
        SeedCatalog.VenueSeed venue = requireVenue(venues, seed.venueId());
        Team team = new Team();
        team.setId(seed.id());
        team.setTeamName(seed.name());
        team.setSquadName(seed.squad());
        team.setConferenceMember(true);
        team.setHomeVenueId(venue.id());
        team.setHomeVenue(new VenueSnapshot(venue.id(), venue.name(), venue.cityState()));
        team.setCfbdTeamId(seed.cfbdTeamId());
        return team;
    }

    private static Rivalry rivalryDocument(SeedCatalog.RivalrySeed seed, Map<String, SeedCatalog.TeamSeed> teams) {
        SeedCatalog.TeamSeed team1 = requireTeam(teams, seed.team1Id());
        SeedCatalog.TeamSeed team2 = requireTeam(teams, seed.team2Id());

        if (team1.id().equals(team2.id())) {
            throw new SeedFailedException("rivalry teams must differ: " + seed.id());
        }

        Rivalry rivalry = new Rivalry();
        rivalry.setId(seed.id());
        rivalry.setRivalryName(seed.name());
        rivalry.setTeam1Id(team1.id());
        rivalry.setTeam2Id(team2.id());
        rivalry.setTeam1(new TeamNameSnapshot(team1.id(), team1.name()));
        rivalry.setTeam2(new TeamNameSnapshot(team2.id(), team2.name()));
        return rivalry;
    }

    private static Season seasonDocument() {
        Season season = new Season();
        season.setId(SeedCatalog.seasonId());
        season.setSeason(SeedCatalog.SEASON_YEAR);
        season.setBeginDate(SeedCatalog.SEASON_BEGIN.toString());
        season.setEndDate(SeedCatalog.seasonEnd().toString());
        season.setCurrent(true);
        return season;
    }

    private static SeasonWeek weekDocument(int weekNumber) {
        LocalDate begin = SeedCatalog.weekBegin(weekNumber);
        SeasonWeek week = new SeasonWeek();
        week.setId(SeedCatalog.weekId(weekNumber));
        week.setSeasonId(SeedCatalog.seasonId());
        week.setWeekNumber(weekNumber);
        week.setBeginDate(begin.toString());
        week.setEndDate(begin.plusDays(6).toString());
        return week;
    }

    private static Matchup matchupDocument(SeedCatalog.MatchupSeed seed, Map<String, SeedCatalog.TeamSeed> teams,
            Map<String, SeedCatalog.VenueSeed> venues) {
        if (seed.weekNumber() < 1 || seed.weekNumber() > SeedCatalog.WEEK_COUNT) {
            throw new SeedFailedException("matchup week is invalid: " + seed.id());
        }

        LocalDate begin = SeedCatalog.weekBegin(seed.weekNumber());
        LocalDate end = begin.plusDays(6);
        LocalDate date = LocalDate.parse(seed.date());

        if (date.isBefore(begin) || date.isAfter(end)) {
            throw new SeedFailedException("matchup date is outside its week: " + seed.id());
        }

        SeedCatalog.TeamSeed home = requireTeam(teams, seed.homeId());
        SeedCatalog.TeamSeed away = requireTeam(teams, seed.awayId());

        if (home.id().equals(away.id())) {
            throw new SeedFailedException("matchup teams must differ: " + seed.id());
        }

        SeedCatalog.VenueSeed venue = requireVenue(venues, seed.venueId());
        Matchup matchup = new Matchup();
        matchup.setId(seed.id());
        matchup.setSeasonId(SeedCatalog.seasonId());
        matchup.setSeasonWeekId(SeedCatalog.weekId(seed.weekNumber()));
        matchup.setWeekNumber(seed.weekNumber());
        matchup.setMatchupDate(date.toString());
        matchup.setHomeTeamId(home.id());
        matchup.setAwayTeamId(away.id());
        matchup.setHomeTeam(new TeamSquadSnapshot(home.id(), home.name(), home.squad()));
        matchup.setAwayTeam(new TeamSquadSnapshot(away.id(), away.name(), away.squad()));
        matchup.setHomeTeamScore(seed.homeScore());
        matchup.setAwayTeamScore(seed.awayScore());
        matchup.setWinningTeamId(winner(seed.homeScore(), seed.awayScore(), home.id(), away.id(), seed.id()));
        matchup.setVenueId(venue.id());
        matchup.setVenue(new VenueSnapshot(venue.id(), venue.name(), venue.cityState()));
        matchup.setRivalryName(rivalryName(home.id(), away.id()));
        return matchup;
    }

    private static Theme themeDocument(SeedCatalog.ThemeSeed seed) {
        Theme theme = new Theme();
        theme.setId(seed.key());
        theme.setThemeName(seed.name());
        theme.setThemePath(seed.key());
        theme.setActive(true);
        theme.setPrimary(seed.primary());
        theme.setSecondary(seed.secondary());
        return theme;
    }

    private static String winner(Integer homeScore, Integer awayScore, String homeId, String awayId, String matchupId) {
        if (homeScore == null && awayScore == null) {
            return null;
        }

        if (homeScore == null || awayScore == null || homeScore.equals(awayScore)) {
            throw new SeedFailedException("sample matchup scores must both be set and must not tie: " + matchupId);
        }

        return homeScore > awayScore ? homeId : awayId;
    }

    private static String rivalryName(String homeId, String awayId) {
        String chosen = null;

        for (SeedCatalog.RivalrySeed rivalry : SeedCatalog.rivalries()) {
            if (!pairs(rivalry, homeId, awayId)) {
                continue;
            }

            if (chosen == null || rivalry.name().compareTo(chosen) < 0) {
                chosen = rivalry.name();
            }
        }

        return chosen;
    }

    private static boolean pairs(SeedCatalog.RivalrySeed rivalry, String homeId, String awayId) {
        return (homeId.equals(rivalry.team1Id()) && awayId.equals(rivalry.team2Id()))
                || (homeId.equals(rivalry.team2Id()) && awayId.equals(rivalry.team1Id()));
    }

    private static SeedCatalog.TeamSeed requireTeam(Map<String, SeedCatalog.TeamSeed> teams, String id) {
        SeedCatalog.TeamSeed team = teams.get(id);

        if (team == null) {
            throw new SeedFailedException("unknown team " + id);
        }

        return team;
    }

    private static SeedCatalog.VenueSeed requireVenue(Map<String, SeedCatalog.VenueSeed> venues, String id) {
        SeedCatalog.VenueSeed venue = venues.get(id);

        if (venue == null) {
            throw new SeedFailedException("unknown venue " + id);
        }

        return venue;
    }

    private static String keep(String stored, String fallback) {
        if (stored != null && !stored.isBlank()) {
            return stored;
        }

        return fallback;
    }

    private void save(String collection, AuditableDocument document) {
        String id = Objects.requireNonNull(document.getId(), "id");
        DocumentReference reference = document(collection, id);
        DocumentSnapshot snapshot = get(reference);

        if (!snapshot.exists()) {
            applyNewAudit(document);

        } else {
            applyExistingAudit(document, readStored(snapshot, document));
        }

        set(reference, document);
    }

    private void applyNewAudit(AuditableDocument document) {
        Instant now = clock.instant();
        document.setCreateDate(now);
        document.setCreateUser(SeedCatalog.ACTOR);
        document.setLastUpdateDate(now);
        document.setLastUpdateUser(SeedCatalog.ACTOR);
        document.setVersion(0L);
    }

    private void applyExistingAudit(AuditableDocument document, AuditableDocument stored) {
        long version = stored.getVersion() == null ? 0L : stored.getVersion();
        document.setCreateDate(stored.getCreateDate());
        document.setCreateUser(stored.getCreateUser());
        document.setLastUpdateDate(clock.instant());
        document.setLastUpdateUser(SeedCatalog.ACTOR);
        document.setVersion(version + 1);
    }

    @SuppressWarnings("unchecked")
    private static AuditableDocument readStored(DocumentSnapshot snapshot, AuditableDocument document) {
        Class<AuditableDocument> type = (Class<AuditableDocument>) document.getClass();
        AuditableDocument stored = snapshot.toObject(type);

        if (stored == null) {
            throw new SeedFailedException("Failed to map " + snapshot.getReference().getPath());
        }

        return stored;
    }

    private DocumentReference document(String collection, String id) {
        String collectionName = Objects.requireNonNull(collection, "collection");
        CollectionReference documents = Objects.requireNonNull(firestore.collection(collectionName), "collection");
        String documentId = Objects.requireNonNull(id, "id");
        return Objects.requireNonNull(documents.document(documentId), "document");
    }

    private static DocumentSnapshot get(DocumentReference reference) {
        try {
            return reference.get().get();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SeedFailedException("Interrupted reading " + reference.getPath(), ex);

        } catch (ExecutionException ex) {
            throw new SeedFailedException("Failed to read " + reference.getPath(), ex);
        }
    }

    private static void set(DocumentReference reference, Object document) {
        Object body = Objects.requireNonNull(document, "document");

        try {
            reference.set(body).get();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SeedFailedException("Interrupted writing " + reference.getPath(), ex);

        } catch (ExecutionException ex) {
            throw new SeedFailedException("Failed to write " + reference.getPath(), ex);
        }
    }
}
