package com.curleesoft.pickem.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.repository.SeasonRepository;

class SeasonServiceTests {

    @Test
    void currentSeasonUsesTheClockAndRepositorySeasons() {
        SeasonRepository repository = mock(SeasonRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T15:00:00Z"), ZoneOffset.UTC);
        SeasonService service = new SeasonService(repository, clock);
        when(repository.findAll()).thenReturn(List.of(season("2020", false), season("2026", false)));

        assertThat(service.current().getSeason()).isEqualTo("2026");
        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.parse("2026-09-21"));
    }

    @Test
    void currentSeasonIsNotFoundWhenNothingMatches() {
        SeasonRepository repository = mock(SeasonRepository.class);
        SeasonService service = new SeasonService(repository, Clock.systemUTC());
        when(repository.findAll()).thenReturn(List.of());

        assertThatThrownBy(service::current).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(SeasonService.NO_CURRENT);
    }

    private static Season season(String year, boolean current) {
        Season season = new Season();
        season.setSeason(year);
        season.setBeginDate(year + "-01-15");
        season.setEndDate(year + "-12-15");
        season.setCurrent(current);
        return season;
    }
}
