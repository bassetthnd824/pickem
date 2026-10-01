package com.curleesoft.pickem.backend.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SeedCatalogTests {

    @Test
    void teamsMatchCfbdSchools() {
        assertThat(SeedCatalog.teams()).hasSize(16);
        assertThat(team("alabama")).isEqualTo(new SeedCatalog.TeamSeed("alabama", "Alabama", "Crimson Tide", 333L,
                "bryant-denny"));
        assertThat(team("arkansas")).isEqualTo(new SeedCatalog.TeamSeed("arkansas", "Arkansas", "Razorbacks", 8L,
                "razorback"));
        assertThat(team("auburn")).isEqualTo(new SeedCatalog.TeamSeed("auburn", "Auburn", "Tigers", 2L, "jordan-hare"));
        assertThat(team("florida")).isEqualTo(new SeedCatalog.TeamSeed("florida", "Florida", "Gators", 57L,
                "ben-hill-griffin"));
        assertThat(team("georgia")).isEqualTo(new SeedCatalog.TeamSeed("georgia", "Georgia", "Bulldogs", 61L,
                "sanford"));
        assertThat(team("kentucky")).isEqualTo(new SeedCatalog.TeamSeed("kentucky", "Kentucky", "Wildcats", 96L,
                "kroger-field"));
        assertThat(team("lsu")).isEqualTo(new SeedCatalog.TeamSeed("lsu", "LSU", "Tigers", 99L, "tiger-stadium"));
        assertThat(team("mississippi-state")).isEqualTo(new SeedCatalog.TeamSeed("mississippi-state",
                "Mississippi State", "Bulldogs", 344L, "davis-wade"));
        assertThat(team("missouri")).isEqualTo(new SeedCatalog.TeamSeed("missouri", "Missouri", "Tigers", 142L,
                "faurot-field"));
        assertThat(team("oklahoma")).isEqualTo(new SeedCatalog.TeamSeed("oklahoma", "Oklahoma", "Sooners", 201L,
                "gaylord"));
        assertThat(team("ole-miss")).isEqualTo(new SeedCatalog.TeamSeed("ole-miss", "Ole Miss", "Rebels", 145L,
                "vaught-hemingway"));
        assertThat(team("south-carolina")).isEqualTo(new SeedCatalog.TeamSeed("south-carolina", "South Carolina",
                "Gamecocks", 2579L, "williams-brice"));
        assertThat(team("tennessee")).isEqualTo(new SeedCatalog.TeamSeed("tennessee", "Tennessee", "Volunteers", 2633L,
                "neyland"));
        assertThat(team("texas")).isEqualTo(new SeedCatalog.TeamSeed("texas", "Texas", "Longhorns", 251L, "dkr-texas"));
        assertThat(team("texas-am")).isEqualTo(new SeedCatalog.TeamSeed("texas-am", "Texas A&M", "Aggies", 245L,
                "kyle-field"));
        assertThat(team("vanderbilt")).isEqualTo(new SeedCatalog.TeamSeed("vanderbilt", "Vanderbilt", "Commodores",
                238L, "firstbank"));

        Set<Long> cfbdIds = new HashSet<>();
        Set<String> names = new HashSet<>();

        for (SeedCatalog.TeamSeed team : SeedCatalog.teams()) {
            assertThat(team.name()).hasSizeLessThanOrEqualTo(40);
            assertThat(team.squad()).isNotBlank().hasSizeLessThanOrEqualTo(40);
            assertThat(cfbdIds.add(team.cfbdTeamId())).isTrue();
            assertThat(names.add(team.name())).isTrue();
            assertThat(venue(team.venueId()).cfbdVenueId()).isNotNull();
        }
    }

    @Test
    void venuesIncludeHomeStadiumsAndUnknown() {
        assertThat(SeedCatalog.venues()).hasSize(17);
        assertThat(venue("unknown")).isEqualTo(new SeedCatalog.VenueSeed("unknown", "Unknown", "Unknown", null));
        assertThat(venue("williams-brice").cfbdVenueId()).isEqualTo(3994L);
        assertThat(venue("bryant-denny").cfbdVenueId()).isEqualTo(3657L);

        Set<Long> cfbdIds = new HashSet<>();
        Set<String> ids = new HashSet<>();

        for (SeedCatalog.VenueSeed venue : SeedCatalog.venues()) {
            assertThat(ids.add(venue.id())).isTrue();
            assertThat(venue.name()).isNotBlank().hasSizeLessThanOrEqualTo(60);
            assertThat(venue.cityState()).isNotBlank().hasSizeLessThanOrEqualTo(60);

            if (venue.cfbdVenueId() != null) {
                assertThat(cfbdIds.add(venue.cfbdVenueId())).isTrue();
            }
        }

        assertThat(cfbdIds).hasSize(16);
    }

    @Test
    void rivalriesStayInsideTheConference() {
        Set<String> teamIds = teamIds();

        for (SeedCatalog.RivalrySeed rivalry : SeedCatalog.rivalries()) {
            assertThat(rivalry.name()).isNotBlank().hasSizeLessThanOrEqualTo(60);
            assertThat(teamIds).contains(rivalry.team1Id(), rivalry.team2Id());
            assertThat(rivalry.team1Id()).isNotEqualTo(rivalry.team2Id());
        }

        List<String> names = SeedCatalog.rivalries().stream().map(rivalry -> rivalry.name()).toList();
        assertThat(names).contains("Iron Bowl", "Southwest Classic", "Red River Rivalry", "Lone Star Showdown",
                "Deep South's Oldest Rivalry", "World's Largest Outdoor Cocktail Party", "Saban Bowl",
                "Mississippi State - Kentucky Rivalry");
        assertThat(names).doesNotContain("Sunshine Showdown", "Clean, Old-Fashioned Hate", "Governor's Cup",
                "Battle of the Palmetto State", "Tennessee - Memphis Rivalry",
                "Missippissi State - Kentucky Rivalry");
    }

    @Test
    void themesCoverLightDarkAndSixteenSchools() {
        assertThat(SeedCatalog.themes()).hasSize(18);
        Set<String> keys = new HashSet<>();

        for (SeedCatalog.ThemeSeed theme : SeedCatalog.themes()) {
            assertThat(keys.add(theme.key())).isTrue();
            assertThat(theme.key()).doesNotStartWith("/");
            assertThat(theme.name()).isNotBlank().hasSizeLessThanOrEqualTo(40);
            assertThat(theme.primary()).matches("#[0-9A-F]{6}");
            assertThat(theme.secondary()).matches("#[0-9A-F]{6}");
        }

        assertThat(keys).contains("light", "dark", "alabama", "arkansas", "auburn", "florida", "georgia", "kentucky",
                "lsu", "mississippi-state", "missouri", "oklahoma", "ole-miss", "south-carolina", "tennessee", "texas",
                "texas-am", "vanderbilt");
    }

    @Test
    void sampleSeasonUsesThursdayWeeks() {
        assertThat(SeedCatalog.SEASON_BEGIN.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
        assertThat(SeedCatalog.SEASON_BEGIN).isEqualTo(LocalDate.of(2026, 8, 27));
        assertThat(SeedCatalog.seasonEnd()).isEqualTo(LocalDate.of(2026, 12, 9));
        assertThat(SeedCatalog.seasonEnd().getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(SeedCatalog.weekBegin(1)).isEqualTo(SeedCatalog.SEASON_BEGIN);
        assertThat(SeedCatalog.weekBegin(5)).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(SeedCatalog.weekId(1)).isEqualTo("2026-w01");
        assertThat(SeedCatalog.weekId(15)).isEqualTo("2026-w15");

        Set<String> teamIds = teamIds();
        Set<String> venueIds = new HashSet<>();

        for (SeedCatalog.VenueSeed venue : SeedCatalog.venues()) {
            venueIds.add(venue.id());
        }

        for (SeedCatalog.MatchupSeed matchup : SeedCatalog.matchups()) {
            assertThat(teamIds).contains(matchup.homeId(), matchup.awayId());
            assertThat(matchup.homeId()).isNotEqualTo(matchup.awayId());
            assertThat(venueIds).contains(matchup.venueId());
            LocalDate begin = SeedCatalog.weekBegin(matchup.weekNumber());
            LocalDate date = LocalDate.parse(matchup.date());
            assertThat(date).isBetween(begin, begin.plusDays(6));

            if (matchup.homeScore() != null || matchup.awayScore() != null) {
                assertThat(matchup.homeScore()).isNotNull();
                assertThat(matchup.awayScore()).isNotNull();
                assertThat(matchup.homeScore()).isNotEqualTo(matchup.awayScore());
            }
        }
    }

    private static SeedCatalog.TeamSeed team(String id) {
        for (SeedCatalog.TeamSeed team : SeedCatalog.teams()) {
            if (id.equals(team.id())) {
                return team;
            }
        }

        throw new AssertionError("missing team " + id);
    }

    private static SeedCatalog.VenueSeed venue(String id) {
        for (SeedCatalog.VenueSeed venue : SeedCatalog.venues()) {
            if (id.equals(venue.id())) {
                return venue;
            }
        }

        throw new AssertionError("missing venue " + id);
    }

    private static Set<String> teamIds() {
        Set<String> ids = new HashSet<>();

        for (SeedCatalog.TeamSeed team : SeedCatalog.teams()) {
            ids.add(team.id());
        }

        return ids;
    }
}
