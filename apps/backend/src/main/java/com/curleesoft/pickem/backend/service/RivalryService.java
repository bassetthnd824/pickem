package com.curleesoft.pickem.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Rivalry;
import com.curleesoft.pickem.backend.model.Team;
import com.curleesoft.pickem.backend.model.snapshot.TeamNameSnapshot;
import com.curleesoft.pickem.backend.repository.RivalryRepository;
import com.curleesoft.pickem.backend.repository.TeamRepository;

/**
 * Manager CRUD for rivalries. Team snapshots are copied from the team
 * documents on each save of this rivalry. A search {@code team1Id} matches
 * either side, which is how the legacy rivalry search form looks up one team.
 */
@Service
public class RivalryService {

    public static final String NOT_FOUND = "Rivalry not found";

    public static final String NAME_INVALID = "rivalry name is invalid";

    public static final String TEAMS_REQUIRED = "rivalry teams are required";

    public static final String TEAMS_EQUAL = "teams cannot be equal";

    private static final int NAME_MAX = 60;

    private final RivalryRepository rivalryRepository;

    private final TeamRepository teamRepository;

    public RivalryService(RivalryRepository rivalryRepository, TeamRepository teamRepository) {
        this.rivalryRepository = rivalryRepository;
        this.teamRepository = teamRepository;
    }

    public List<Rivalry> search(String rivalryName, String team1Id, String team2Id) {
        String name = normalizeContains(rivalryName);
        String first = blankToNull(team1Id);
        String second = blankToNull(team2Id);

        return rivalryRepository.findAll().stream()
                .filter(item -> name == null || contains(item.getRivalryName(), name))
                .filter(item -> first == null || first.equals(item.getTeam1Id()) || first.equals(item.getTeam2Id()))
                .filter(item -> second == null || second.equals(item.getTeam2Id()))
                .sorted(Comparator.comparing((Rivalry item) -> item.getRivalryName(),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right))))
                .toList();
    }

    public Rivalry get(String id) {
        return rivalryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public Rivalry create(Rivalry request) {
        Rivalry rivalry = new Rivalry();
        apply(rivalry, request);
        return rivalryRepository.save(rivalry);
    }

    public Rivalry update(String id, Rivalry request) {
        Rivalry existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        return rivalryRepository.save(existing);
    }

    public void delete(String id) {
        get(id);
        rivalryRepository.delete(id);
    }

    private void apply(Rivalry target, Rivalry request) {
        String name = trim(request.getRivalryName());

        if (!StringUtils.hasText(name) || name.length() > NAME_MAX) {
            throw new InvalidRequestException(NAME_INVALID);
        }

        String firstId = trim(request.getTeam1Id());
        String secondId = trim(request.getTeam2Id());

        if (!StringUtils.hasText(firstId) || !StringUtils.hasText(secondId)) {
            throw new InvalidRequestException(TEAMS_REQUIRED);
        }

        if (firstId.equals(secondId)) {
            throw new InvalidRequestException(TEAMS_EQUAL);
        }

        Team first = teamRepository.findById(firstId)
                .orElseThrow(() -> new InvalidRequestException(TeamService.NOT_FOUND));
        Team second = teamRepository.findById(secondId)
                .orElseThrow(() -> new InvalidRequestException(TeamService.NOT_FOUND));

        target.setRivalryName(name);
        target.setTeam1Id(first.getId());
        target.setTeam2Id(second.getId());
        target.setTeam1(new TeamNameSnapshot(first.getId(), first.getTeamName()));
        target.setTeam2(new TeamNameSnapshot(second.getId(), second.getTeamName()));
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
