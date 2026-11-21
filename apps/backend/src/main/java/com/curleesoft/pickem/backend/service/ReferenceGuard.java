package com.curleesoft.pickem.backend.service;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.curleesoft.pickem.backend.model.Matchup;
import com.curleesoft.pickem.backend.model.Rivalry;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.model.snapshot.TeamNameSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.TeamSquadSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.curleesoft.pickem.backend.repository.FirestoreAccessException;
import com.curleesoft.pickem.backend.repository.MatchupRepository;
import com.curleesoft.pickem.backend.repository.PickRepository;
import com.curleesoft.pickem.backend.repository.RivalryRepository;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;

/**
 * Rewrites denormalized team and venue snapshots after those documents change,
 * and refuses a delete while a child or reference still points at the record.
 * Picks are never cascaded. Rivalry documents are not stored by id on any other
 * collection (matchups copy {@code rivalryName}), so a rivalry delete has
 * nothing to block on.
 */
@Service
public class ReferenceGuard {

    public static final String SEASON_WEEKS = "blocked by season weeks";

    public static final String MATCHUPS = "blocked by matchups";

    public static final String PICKS = "blocked by picks";

    public static final String TEAMS = "blocked by teams";

    public static final String RIVALRIES = "blocked by rivalries";

    private final Firestore firestore;

    private final MatchupRepository matchupRepository;

    private final TeamRepository teamRepository;

    private final RivalryRepository rivalryRepository;

    public ReferenceGuard(
            Firestore firestore,
            MatchupRepository matchupRepository,
            TeamRepository teamRepository,
            RivalryRepository rivalryRepository
    ) {
        this.firestore = firestore;
        this.matchupRepository = matchupRepository;
        this.teamRepository = teamRepository;
        this.rivalryRepository = rivalryRepository;
    }

    public void refreshTeam(Team team) {
        Objects.requireNonNull(team, "team");
        String teamId = Objects.requireNonNull(team.getId(), "teamId");
        String name = Objects.requireNonNull(team.getTeamName(), "teamName");
        String squad = Objects.requireNonNull(team.getSquadName(), "squadName");
        refreshMatchupTeams(teamId, name, squad);
        refreshRivalryTeams(teamId, name);
    }

    public void refreshVenue(Venue venue) {
        Objects.requireNonNull(venue, "venue");
        String venueId = Objects.requireNonNull(venue.getId(), "venueId");
        String name = Objects.requireNonNull(venue.getVenueName(), "venueName");
        String cityState = Objects.requireNonNull(venue.getCityState(), "cityState");
        refreshTeamVenues(venueId, name, cityState);
        refreshMatchupVenues(venueId, name, cityState);
    }

    public void deleteSeason(String seasonId) {
        String id = Objects.requireNonNull(seasonId, "seasonId");
        deleteIfUnreferenced(SeasonRepository.COLLECTION, id, (transaction, documentId) -> {

            if (referenced(transaction, SeasonWeekRepository.COLLECTION, "seasonId", documentId)) {
                throw new ConflictException(SEASON_WEEKS);

            }

            if (referenced(transaction, MatchupRepository.COLLECTION, "seasonId", documentId)) {
                throw new ConflictException(MATCHUPS);

            }

            if (referenced(transaction, PickRepository.COLLECTION, "seasonId", documentId)) {
                throw new ConflictException(PICKS);

            }

        });
    }

    public void deleteWeek(String seasonWeekId) {
        String id = Objects.requireNonNull(seasonWeekId, "seasonWeekId");
        deleteIfUnreferenced(SeasonWeekRepository.COLLECTION, id, (transaction, documentId) -> {

            if (referenced(transaction, MatchupRepository.COLLECTION, "seasonWeekId", documentId)) {
                throw new ConflictException(MATCHUPS);

            }

            if (referenced(transaction, PickRepository.COLLECTION, "seasonWeekId", documentId)) {
                throw new ConflictException(PICKS);

            }

        });
    }

    public void deleteTeam(String teamId) {
        String id = Objects.requireNonNull(teamId, "teamId");
        deleteIfUnreferenced(TeamRepository.COLLECTION, id, (transaction, documentId) -> {

            if (referenced(transaction, MatchupRepository.COLLECTION, "homeTeamId", documentId)
                    || referenced(transaction, MatchupRepository.COLLECTION, "awayTeamId", documentId)) {
                throw new ConflictException(MATCHUPS);

            }

            if (referenced(transaction, RivalryRepository.COLLECTION, "team1Id", documentId)
                    || referenced(transaction, RivalryRepository.COLLECTION, "team2Id", documentId)) {
                throw new ConflictException(RIVALRIES);

            }

            if (referenced(transaction, PickRepository.COLLECTION, "pickedTeamId", documentId)) {
                throw new ConflictException(PICKS);

            }

        });
    }

    public void deleteVenue(String venueId) {
        String id = Objects.requireNonNull(venueId, "venueId");
        deleteIfUnreferenced(VenueRepository.COLLECTION, id, (transaction, documentId) -> {

            if (referenced(transaction, TeamRepository.COLLECTION, "homeVenueId", documentId)) {
                throw new ConflictException(TEAMS);

            }

            if (referenced(transaction, MatchupRepository.COLLECTION, "venueId", documentId)) {
                throw new ConflictException(MATCHUPS);

            }

        });
    }

    public void deleteMatchup(String matchupId) {
        String id = Objects.requireNonNull(matchupId, "matchupId");
        deleteIfUnreferenced(MatchupRepository.COLLECTION, id, (transaction, documentId) -> {

            if (referenced(transaction, PickRepository.COLLECTION, "matchupId", documentId)) {
                throw new ConflictException(PICKS);

            }

        });
    }

    /**
     * Refuses the delete before claims or refresh tokens change. The delete
     * transaction checks again in case a pick arrives after this read.
     */
    public void requireUserDeletable(String pickUserId) {
        String picker = Objects.requireNonNull(pickUserId, "pickUserId");

        if (picksReference(picker)) {
            throw new ConflictException(PICKS);

        }

    }

    public void deleteUser(String userId, String pickUserId) {
        String id = Objects.requireNonNull(userId, "userId");
        String picker = Objects.requireNonNull(pickUserId, "pickUserId");
        deleteIfUnreferenced(UserRepository.COLLECTION, id, (transaction, documentId) -> {
            Objects.requireNonNull(documentId, "userId");

            if (referenced(transaction, PickRepository.COLLECTION, "userId", picker)) {
                throw new ConflictException(PICKS);

            }

        });
    }

    private void refreshMatchupTeams(String teamId, String name, String squad) {
        Set<String> ids = new LinkedHashSet<>();
        collectIds(
                ids,
                matchupRepository.query(collection -> collection.whereEqualTo("homeTeamId", teamId))
        );
        collectIds(
                ids,
                matchupRepository.query(collection -> collection.whereEqualTo("awayTeamId", teamId))
        );

        for (String matchupId : ids) {
            matchupRepository.patch(matchupId, matchup -> applyTeam(matchup, teamId, name, squad));

        }

    }

    private void refreshRivalryTeams(String teamId, String name) {
        Set<String> ids = new LinkedHashSet<>();
        collectRivalryIds(
                ids,
                rivalryRepository.query(collection -> collection.whereEqualTo("team1Id", teamId))
        );
        collectRivalryIds(
                ids,
                rivalryRepository.query(collection -> collection.whereEqualTo("team2Id", teamId))
        );

        for (String rivalryId : ids) {
            rivalryRepository.patch(rivalryId, rivalry -> applyRivalryTeam(rivalry, teamId, name));

        }

    }

    private void refreshTeamVenues(String venueId, String name, String cityState) {

        for (Team team : teamRepository.query(collection -> collection.whereEqualTo("homeVenueId", venueId))) {
            String teamId = Objects.requireNonNull(team.getId(), "teamId");
            teamRepository.patch(teamId, stored -> applyHomeVenue(stored, venueId, name, cityState));

        }

    }

    private void refreshMatchupVenues(String venueId, String name, String cityState) {

        for (Matchup matchup : matchupRepository.query(collection -> collection.whereEqualTo("venueId", venueId))) {
            String matchupId = Objects.requireNonNull(matchup.getId(), "matchupId");
            matchupRepository.patch(matchupId, stored -> applyVenue(stored, venueId, name, cityState));

        }

    }

    private static void collectIds(Set<String> ids, Iterable<Matchup> matchups) {

        for (Matchup matchup : matchups) {
            ids.add(Objects.requireNonNull(matchup.getId(), "matchupId"));

        }

    }

    private static void collectRivalryIds(Set<String> ids, Iterable<Rivalry> rivalries) {

        for (Rivalry rivalry : rivalries) {
            ids.add(Objects.requireNonNull(rivalry.getId(), "rivalryId"));

        }

    }

    private static boolean applyTeam(Matchup matchup, String teamId, String name, String squad) {
        boolean changed = false;

        if (teamId.equals(matchup.getHomeTeamId()) && !sameSquad(matchup.getHomeTeam(), teamId, name, squad)) {
            matchup.setHomeTeam(new TeamSquadSnapshot(teamId, name, squad));
            changed = true;

        }

        if (teamId.equals(matchup.getAwayTeamId()) && !sameSquad(matchup.getAwayTeam(), teamId, name, squad)) {
            matchup.setAwayTeam(new TeamSquadSnapshot(teamId, name, squad));
            changed = true;

        }

        return changed;
    }

    private static boolean applyRivalryTeam(Rivalry rivalry, String teamId, String name) {
        boolean changed = false;

        if (teamId.equals(rivalry.getTeam1Id()) && !sameName(rivalry.getTeam1(), teamId, name)) {
            rivalry.setTeam1(new TeamNameSnapshot(teamId, name));
            changed = true;

        }

        if (teamId.equals(rivalry.getTeam2Id()) && !sameName(rivalry.getTeam2(), teamId, name)) {
            rivalry.setTeam2(new TeamNameSnapshot(teamId, name));
            changed = true;

        }

        return changed;
    }

    private static boolean applyHomeVenue(Team team, String venueId, String name, String cityState) {

        if (!venueId.equals(team.getHomeVenueId()) || sameVenue(team.getHomeVenue(), venueId, name, cityState)) {
            return false;

        }

        team.setHomeVenue(new VenueSnapshot(venueId, name, cityState));
        return true;
    }

    private static boolean applyVenue(Matchup matchup, String venueId, String name, String cityState) {

        if (!venueId.equals(matchup.getVenueId()) || sameVenue(matchup.getVenue(), venueId, name, cityState)) {
            return false;

        }

        matchup.setVenue(new VenueSnapshot(venueId, name, cityState));
        return true;
    }

    private void deleteIfUnreferenced(String collectionName, String id, ReferenceCheck check) {
        String name = Objects.requireNonNull(collectionName, "collection");
        String documentId = Objects.requireNonNull(id, "id");

        try {
            firestore.runTransaction(transaction -> {
                check.check(transaction, documentId);
                CollectionReference collection = Objects.requireNonNull(firestore.collection(name), "collection");
                DocumentReference reference = Objects.requireNonNull(collection.document(documentId), "document");

                if (TransactionReads.documentExists(transaction, reference)) {
                    transaction.delete(reference);

                }

                return null;

            }).get();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FirestoreAccessException("Interrupted deleting " + name + "/" + documentId, ex);

        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

            if (cause instanceof RuntimeException runtime) {
                throw runtime;

            }

            throw new FirestoreAccessException("Failed to delete " + name + "/" + documentId, cause);

        }

    }

    private boolean picksReference(String picker) {
        String name = Objects.requireNonNull(PickRepository.COLLECTION, "collection");
        CollectionReference collection = Objects.requireNonNull(firestore.collection(name), "collection");
        Query query = Objects.requireNonNull(collection.whereEqualTo("userId", picker), "query");
        Query limited = Objects.requireNonNull(query.limit(1), "query");

        try {
            QuerySnapshot snapshot = Objects.requireNonNull(limited.get().get(), "picks");
            return !snapshot.isEmpty();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FirestoreAccessException("Interrupted reading picks for " + picker, ex);

        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

            if (cause instanceof RuntimeException runtime) {
                throw runtime;

            }

            throw new FirestoreAccessException("Failed to read picks for " + picker, cause);

        }

    }

    private boolean referenced(Transaction transaction, String collectionName, String field, String value) {
        String name = Objects.requireNonNull(collectionName, "collection");
        String column = Objects.requireNonNull(field, "field");
        String id = Objects.requireNonNull(value, "value");
        CollectionReference collection = Objects.requireNonNull(firestore.collection(name), "collection");
        Query query = Objects.requireNonNull(collection.whereEqualTo(column, id), "query");
        Query limited = Objects.requireNonNull(query.limit(1), "query");
        return !TransactionReads.get(transaction, limited).isEmpty();
    }

    private static boolean sameSquad(TeamSquadSnapshot snapshot, String id, String name, String squad) {
        return snapshot != null && id.equals(snapshot.getId()) && name.equals(snapshot.getName())
                && squad.equals(snapshot.getSquad());
    }

    private static boolean sameName(TeamNameSnapshot snapshot, String id, String name) {
        return snapshot != null && id.equals(snapshot.getId()) && name.equals(snapshot.getName());
    }

    private static boolean sameVenue(VenueSnapshot snapshot, String id, String name, String cityState) {
        return snapshot != null && id.equals(snapshot.getId()) && name.equals(snapshot.getName())
                && cityState.equals(snapshot.getCityState());
    }

    @FunctionalInterface
    private interface ReferenceCheck {

        void check(Transaction transaction, String id);
    }

}
