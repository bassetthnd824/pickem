package com.curleesoft.pickem.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.curleesoft.pickem.backend.model.snapshot.TeamSquadSnapshot;
import com.curleesoft.pickem.backend.model.snapshot.VenueSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainDtoJsonTests {

  private final ObjectMapper objectMapper = JsonMapper.builder()
    .addModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .build();

  @Test
  void serializesCalendarDatesAndTimestampsAsIso8601() throws Exception {
    Season season = new Season();
    season.setSeason("2024");
    season.setBeginDate("2024-08-29");
    season.setEndDate("2025-01-13");
    season.setCurrent(true);
    season.setCreateDate(Instant.parse("2024-08-01T15:30:00Z"));
    season.setCreateUser("seed");
    season.setLastUpdateDate(Instant.parse("2024-08-01T15:30:00Z"));
    season.setLastUpdateUser("seed");
    season.setVersion(0L);

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(season));

    assertThat(json.path("beginDate").asText()).isEqualTo("2024-08-29");
    assertThat(json.path("endDate").asText()).isEqualTo("2025-01-13");
    assertThat(json.path("isCurrent").asBoolean()).isTrue();
    assertThat(json.path("createDate").asText()).isEqualTo("2024-08-01T15:30:00Z");
  }

  @Test
  void serializesMatchupSnapshots() throws Exception {
    Matchup matchup = new Matchup();
    matchup.setSeasonId("s1");
    matchup.setSeasonWeekId("w1");
    matchup.setWeekNumber(3);
    matchup.setMatchupDate("2024-09-14");
    matchup.setHomeTeamId("home");
    matchup.setAwayTeamId("away");
    matchup.setHomeTeam(new TeamSquadSnapshot("home", "Alabama", "Crimson Tide"));
    matchup.setAwayTeam(new TeamSquadSnapshot("away", "Georgia", "Bulldogs"));
    matchup.setVenueId("v1");
    matchup.setVenue(new VenueSnapshot("v1", "Bryant-Denny Stadium", "Tuscaloosa, AL"));

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(matchup));

    assertThat(json.path("matchupDate").asText()).isEqualTo("2024-09-14");
    assertThat(json.path("homeTeam").path("name").asText()).isEqualTo("Alabama");
    assertThat(json.path("homeTeam").path("squad").asText()).isEqualTo(
      "Crimson Tide"
    );
    assertThat(json.path("venue").path("cityState").asText()).isEqualTo(
      "Tuscaloosa, AL"
    );
    assertThat(json.path("homeTeamScore").isNull()).isTrue();
  }

  @Test
  void userJsonHasNoPasswordField() throws Exception {
    User user = new User();
    user.setUid("abc");
    user.setId("abc");
    user.setEmailAddr("a@b.com");
    user.setFirstName("A");
    user.setLastName("B");
    user.setNickName("AB");
    user.setThemeId("light");
    user.setRoles(List.of("player"));

    String json = objectMapper.writeValueAsString(user);

    assertThat(json).doesNotContain("password");
    assertThat(json).doesNotContain("userPass");
    assertThat(objectMapper.readTree(json).path("roles").path(0).asText()).isEqualTo(
      "player"
    );
  }
}
