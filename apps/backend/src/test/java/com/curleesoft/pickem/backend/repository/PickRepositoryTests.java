package com.curleesoft.pickem.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.curleesoft.pickem.backend.model.Pick;
import com.curleesoft.pickem.backend.service.InvalidRequestException;
import com.curleesoft.pickem.backend.service.PickService;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

@SpringBootTest
class PickRepositoryTests extends FirestoreEmulatorSupport {

    @Autowired
    private PickRepository pickRepository;

    private final List<String> userIds = new ArrayList<>();

    @AfterEach
    void deleteFixtures() {
        for (String userId : userIds) {
            for (Pick pick : pickRepository.findByUserId(userId)) {
                pickRepository.delete(pick.getId());
            }
        }
        userIds.clear();
    }

    @Test
    void commitWeekChecksVersionsOnUpdateAndDelete() {
        String userId = userId();
        String weekId = "week-" + UUID.randomUUID();
        Pick created = pickRepository
                .commitWeek(userId, weekId, List.of(pick(userId, weekId, "matchup-a", "team-home", 1)), List.of(), null)
                .get(0);

        assertThat(created.getVersion()).isZero();
        assertThat(created.getId()).isEqualTo(PickRepository.documentId(userId, "matchup-a"));
        assertThat(pickRepository.findById(created.getId()).orElseThrow().getRank()).isEqualTo(1);

        Pick loaded = pickRepository.findById(created.getId()).orElseThrow();
        loaded.setRank(2);
        Pick updated = pickRepository.commitWeek(userId, weekId, List.of(loaded), List.of(), null).get(0);

        assertThat(loaded.getVersion()).isZero();
        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getRank()).isEqualTo(2);

        Pick stale = pickRepository.findById(created.getId()).orElseThrow();
        stale.setVersion(0L);
        stale.setRank(3);

        assertThatThrownBy(() -> pickRepository.commitWeek(userId, weekId, List.of(stale), List.of(), null))
                .isInstanceOf(StaleDocumentVersionException.class).hasMessageContaining(created.getId());

        Pick current = pickRepository.findById(created.getId()).orElseThrow();
        assertThat(current.getRank()).isEqualTo(2);
        assertThat(current.getVersion()).isEqualTo(1L);

        Pick staleDelete = pickRepository.findById(created.getId()).orElseThrow();
        staleDelete.setVersion(0L);

        assertThatThrownBy(() -> pickRepository.commitWeek(userId, weekId, List.of(), List.of(staleDelete), null))
                .isInstanceOf(StaleDocumentVersionException.class);
        assertThat(pickRepository.findById(created.getId())).isPresent();

        Pick removal = pickRepository.findById(created.getId()).orElseThrow();
        assertThat(pickRepository.commitWeek(userId, weekId, List.of(), List.of(removal), null)).isEmpty();
        assertThat(pickRepository.findById(created.getId())).isEmpty();

        Pick missing = pick(userId, weekId, "matchup-gone", "team-home", 1);
        missing.setId("missing-" + UUID.randomUUID());
        missing.setVersion(0L);
        assertThat(pickRepository.commitWeek(userId, weekId, List.of(), List.of(missing), null)).isEmpty();
    }

    @Test
    void commitWeekRereadsTheWeekBeforeAcceptingARank() {
        String userId = userId();
        String weekId = "week-" + UUID.randomUUID();
        pickRepository.commitWeek(userId, weekId, List.of(pick(userId, weekId, "matchup-a", "team-home", 1)), List.of(),
                null);

        Pick second = pick(userId, weekId, "matchup-b", "team-away", 1);

        assertThatThrownBy(() -> pickRepository.commitWeek(userId, weekId, List.of(second), List.of(), stored -> {
            Set<Integer> ranks = new HashSet<>();

            for (Pick existing : stored.values()) {
                if (second.getMatchupId().equals(existing.getMatchupId()) || existing.getRank() == null) {
                    continue;
                }

                if (!ranks.add(existing.getRank())) {
                    throw new InvalidRequestException(PickService.RANK_NOT_UNIQUE);
                }
            }

            if (second.getRank() != null && !ranks.add(second.getRank())) {
                throw new InvalidRequestException(PickService.RANK_NOT_UNIQUE);
            }
        })).isInstanceOf(InvalidRequestException.class).hasMessage(PickService.RANK_NOT_UNIQUE);

        assertThat(pickRepository.findByUserId(userId)).hasSize(1);
        assertThat(pickRepository.findById(PickRepository.documentId(userId, "matchup-b"))).isEmpty();
        assertThat(pickRepository.findById(PickRepository.documentId(userId, "matchup-a")).orElseThrow().getRank())
                .isEqualTo(1);
    }

    @Test
    void anEmptyCommitDoesNotRunTheGuard() {
        assertThat(pickRepository.commitWeek(userId(), "week", List.of(), List.of(), stored -> {
            throw new IllegalStateException("guard");
        })).isEmpty();
    }

    private String userId() {
        String userId = "user-" + UUID.randomUUID();
        userIds.add(userId);
        return userId;
    }

    private static Pick pick(String userId, String weekId, String matchupId, String teamId, int rank) {
        Pick pick = new Pick();
        pick.setId(PickRepository.documentId(userId, matchupId));
        pick.setUserId(userId);
        pick.setMatchupId(matchupId);
        pick.setSeasonId("season-" + weekId);
        pick.setSeasonWeekId(weekId);
        pick.setPickedTeamId(teamId);
        pick.setRank(rank);
        return pick;
    }
}
