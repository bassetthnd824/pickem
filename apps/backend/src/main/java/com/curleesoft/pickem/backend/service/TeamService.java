package com.curleesoft.pickem.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.curleesoft.pickem.backend.repository.TeamRepository;
import com.curleesoft.pickem.backend.repository.VenueRepository;

/**
 * Manager CRUD for teams. The home-venue snapshot is copied from the venue
 * document on each save of this team. Refreshing other documents is US-35.
 * {@code GET ?conferenceMember=true} replaces {@code TeamBean.getConferenceTeams}
 * and {@code getNumberOfConferenceTeams} (the list length is the count).
 */
@Service
public class TeamService {

    public static final String NOT_FOUND = "Team not found";

    public static final String NAME_INVALID = "team name is invalid";

    public static final String SQUAD_INVALID = "squad name is invalid";

    public static final String CONFERENCE_REQUIRED = "conferenceMember is required";

    public static final String NAME_NOT_UNIQUE = "team name must be unique";

    public static final String CFBD_NOT_UNIQUE = "cfbdTeamId must be unique";

    private static final int NAME_MAX = 40;

    private final TeamRepository teamRepository;

    private final VenueRepository venueRepository;

    public TeamService(TeamRepository teamRepository, VenueRepository venueRepository) {
        this.teamRepository = teamRepository;
        this.venueRepository = venueRepository;
    }

    public List<Team> search(String teamName, String squadName, Boolean conferenceMember, String homeVenueId,
            Long cfbdTeamId) {
        String name = normalizeContains(teamName);
        String squad = normalizeContains(squadName);
        String venueId = blankToNull(homeVenueId);

        return teamRepository.findAll().stream()
                .filter(item -> name == null || contains(item.getTeamName(), name))
                .filter(item -> squad == null || contains(item.getSquadName(), squad))
                .filter(item -> conferenceMember == null || conferenceMember.equals(item.getConferenceMember()))
                .filter(item -> venueId == null || venueId.equals(item.getHomeVenueId()))
                .filter(item -> cfbdTeamId == null || cfbdTeamId.equals(item.getCfbdTeamId()))
                .sorted(Comparator.comparing((Team item) -> item.getTeamName(),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right))))
                .toList();
    }

    public Team get(String id) {
        return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public Team create(Team request) {
        Team team = new Team();
        apply(team, request);
        ensureUnique(team);
        return teamRepository.save(team);
    }

    public Team update(String id, Team request) {
        Team existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        ensureUnique(existing);
        return teamRepository.save(existing);
    }

    public void delete(String id) {
        get(id);
        teamRepository.delete(id);
    }

    private void apply(Team target, Team request) {
        String name = trim(request.getTeamName());
        String squad = trim(request.getSquadName());

        if (!StringUtils.hasText(name) || name.length() > NAME_MAX) {
            throw new InvalidRequestException(NAME_INVALID);
        }

        if (!StringUtils.hasText(squad) || squad.length() > NAME_MAX) {
            throw new InvalidRequestException(SQUAD_INVALID);
        }

        if (request.getConferenceMember() == null) {
            throw new InvalidRequestException(CONFERENCE_REQUIRED);
        }

        String venueId = trim(request.getHomeVenueId());
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new InvalidRequestException(VenueService.NOT_FOUND));

        target.setTeamName(name);
        target.setSquadName(squad);
        target.setConferenceMember(request.getConferenceMember());
        target.setHomeVenueId(venue.getId());
        target.setHomeVenue(new VenueSnapshot(venue.getId(), venue.getVenueName(), venue.getCityState()));
        target.setCfbdTeamId(request.getCfbdTeamId());
    }

    private void ensureUnique(Team team) {
        boolean duplicateName = teamRepository
                .query(collection -> collection.whereEqualTo("teamName", team.getTeamName())).stream()
                .anyMatch(found -> !Objects.equals(found.getId(), team.getId()));

        if (duplicateName) {
            throw new ConflictException(NAME_NOT_UNIQUE);
        }

        Long externalId = team.getCfbdTeamId();

        if (externalId == null) {
            return;
        }

        boolean duplicateExternalId = teamRepository
                .query(collection -> collection.whereEqualTo("cfbdTeamId", externalId)).stream()
                .anyMatch(found -> !Objects.equals(found.getId(), team.getId()));

        if (duplicateExternalId) {
            throw new ConflictException(CFBD_NOT_UNIQUE);
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private static String normalizeContains(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
