package com.curleesoft.pickem.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.curleesoft.pickem.backend.repository.StaleDocumentVersionException;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Transaction;

/**
 * Manager CRUD for teams. The home-venue snapshot is copied from the venue
 * document on each save of this team. {@link ReferenceGuard} then rewrites
 * matchup and rivalry snapshots that embed this team.
 * {@code GET ?conferenceMember=true} replaces
 * {@code TeamBean.getConferenceTeams} and {@code getNumberOfConferenceTeams}
 * (the list length is the count).
 */
@Service
public class TeamService {

    public static final String NOT_FOUND = "Team not found";

    public static final String NAME_NOT_UNIQUE = "team name must be unique";

    public static final String CFBD_NOT_UNIQUE = "cfbdTeamId must be unique";

    private final TeamRepository teamRepository;

    private final VenueRepository venueRepository;

    private final ReferenceGuard referenceGuard;

    public TeamService(
            TeamRepository teamRepository,
            VenueRepository venueRepository,
            ReferenceGuard referenceGuard
    ) {
        this.teamRepository = teamRepository;
        this.venueRepository = venueRepository;
        this.referenceGuard = referenceGuard;
    }

    public List<Team> search(
            String teamName,
            String squadName,
            Boolean conferenceMember,
            String homeVenueId,
            Long cfbdTeamId
    ) {
        String name = SearchText.normalizeContains(teamName);
        String squad = SearchText.normalizeContains(squadName);
        String venueId = SearchText.blankToNull(homeVenueId);

        return teamRepository.findAll().stream()
                .filter(item -> name == null || SearchText.contains(item.getTeamName(), name))
                .filter(item -> squad == null || SearchText.contains(item.getSquadName(), squad))
                .filter(item -> conferenceMember == null || conferenceMember.equals(item.getConferenceMember()))
                .filter(item -> venueId == null || venueId.equals(item.getHomeVenueId()))
                .filter(item -> cfbdTeamId == null || cfbdTeamId.equals(item.getCfbdTeamId()))
                .sorted(
                        Comparator.comparing(
                                (Team item) -> item.getTeamName(),
                                Comparator.nullsLast((String left, String right) -> left.compareTo(right))
                        )
                )
                .toList();
    }

    /**
     * Conference members ordered by team name. Replaces
     * {@code TeamBean.getConferenceTeams}. The player schedule dropdown uses this
     * list; {@link #search} remains the manager filter.
     */
    public List<Team> conferenceTeams() {
        return search(null, null, Boolean.TRUE, null, null);
    }

    public Team get(String id) {
        return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public Team create(Team request) {
        Team team = new Team();
        apply(team, request);
        return teamRepository.save(
                team,
                (transaction, collection, documentId) -> rejectDuplicate(transaction, collection, documentId, team)
        );
    }

    public Team update(String id, Team request) {
        Team existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        Team saved = saveOrResume(existing);
        referenceGuard.refreshTeam(saved);
        return saved;
    }

    public void delete(String id) {
        get(id);
        referenceGuard.deleteTeam(Objects.requireNonNull(id, "id"));
    }

    /**
     * A previous attempt may have committed this edit and then failed while
     * rewriting snapshots. A later venue snapshot patch can move the version by
     * more than one, so any newer stored row with the same fields is refreshed
     * again. A submitted version newer than the row still conflicts.
     */
    private Team saveOrResume(Team desired) {

        try {
            return teamRepository.save(
                    desired,
                    (transaction, collection, documentId) -> rejectDuplicate(
                            transaction,
                            collection,
                            documentId,
                            desired
                    )
            );

        } catch (StaleDocumentVersionException ex) {
            Team stored = get(Objects.requireNonNull(desired.getId(), "id"));

            if (storedIsNewer(stored.getVersion(), desired.getVersion()) && sameCommitted(stored, desired)) {
                return stored;

            }

            throw ex;

        }

    }

    private static boolean storedIsNewer(Long storedVersion, Long submittedVersion) {

        if (submittedVersion == null) {
            return false;

        }

        long stored = storedVersion == null ? 0L : storedVersion;
        return stored > submittedVersion;
    }

    private static boolean sameCommitted(Team stored, Team desired) {
        return Objects.equals(stored.getTeamName(), desired.getTeamName())
                && Objects.equals(stored.getSquadName(), desired.getSquadName())
                && Objects.equals(stored.getConferenceMember(), desired.getConferenceMember())
                && Objects.equals(stored.getHomeVenueId(), desired.getHomeVenueId())
                && Objects.equals(stored.getCfbdTeamId(), desired.getCfbdTeamId());
    }

    private void apply(Team target, Team request) {
        String venueId = SearchText.trim(request.getHomeVenueId());
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new InvalidRequestException(VenueService.NOT_FOUND));

        target.setTeamName(SearchText.trim(request.getTeamName()));
        target.setSquadName(SearchText.trim(request.getSquadName()));
        target.setConferenceMember(request.getConferenceMember());
        target.setHomeVenueId(venue.getId());
        target.setHomeVenue(new VenueSnapshot(venue.getId(), venue.getVenueName(), venue.getCityState()));
        target.setCfbdTeamId(request.getCfbdTeamId());
    }

    private static void rejectDuplicate(
            Transaction transaction,
            CollectionReference collection,
            String documentId,
            Team team
    ) {

        if (TransactionReads.anotherDocumentMatches(
                transaction,
                collection.whereEqualTo("teamName", team.getTeamName()),
                documentId
        )) {
            throw new ConflictException(NAME_NOT_UNIQUE);

        }

        Long externalId = team.getCfbdTeamId();

        if (externalId == null) {
            return;

        }

        if (TransactionReads.anotherDocumentMatches(
                transaction,
                collection.whereEqualTo("cfbdTeamId", externalId),
                documentId
        )) {
            throw new ConflictException(CFBD_NOT_UNIQUE);

        }

    }

}
