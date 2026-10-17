package com.curleesoft.pickem.backend.cfbd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.curleesoft.pickem.backend.cfbd.CfbdScoreboardGame.Team;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class CfbdClientTests {

    private static final String API_KEY = "cfbd-secret-do-not-log";

    private static final String BASE = "https://api.collegefootballdata.com";

    private static final String GAMES_JSON = """
            [
              {
                "id": 401628319,
                "season": 2024,
                "week": 7,
                "seasonType": "regular",
                "startDate": "2024-10-12T16:00:00.000Z",
                "completed": true,
                "venueId": 3854,
                "venue": "Neyland Stadium",
                "homeId": 2633,
                "homeTeam": "Tennessee",
                "homePoints": 24,
                "awayId": 333,
                "awayTeam": "Alabama",
                "awayPoints": 17,
                "homePregameElo": 1800,
                "notes": "cfbd-secret-do-not-log"
              },
              {
                "id": 401628320,
                "season": 2024,
                "week": 8,
                "seasonType": "regular",
                "startDate": "2024-10-19T16:00:00.000Z",
                "completed": false,
                "venueId": null,
                "venue": null,
                "homeId": 61,
                "homeTeam": "Georgia",
                "homePoints": null,
                "awayId": 2579,
                "awayTeam": "South Carolina",
                "awayPoints": null
              }
            ]
            """;

    private MockRestServiceServer server;

    private CfbdClient client;

    private Logger logger;

    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        client = clientWith(API_KEY);
        logger = (Logger) org.slf4j.LoggerFactory.getLogger(CfbdClient.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
    }

    @Test
    void gamesReadsSecScheduleAndMapsExternalIds() {
        expect(BASE + "/games?year=2024&conference=SEC", GAMES_JSON);

        List<CfbdGame> games = client.games(2024, null, null);

        CfbdGame finished = games.get(0);
        assertThat(finished.id()).isEqualTo(401628319L);
        assertThat(finished.season()).isEqualTo(2024);
        assertThat(finished.week()).isEqualTo(7);
        assertThat(finished.seasonType()).isEqualTo("regular");
        assertThat(finished.startDate()).isEqualTo("2024-10-12T16:00:00.000Z");
        assertThat(finished.completed()).isTrue();
        assertThat(finished.venue()).isEqualTo("Neyland Stadium");
        assertThat(finished.cfbdVenueId()).isEqualTo(3854L);
        assertThat(finished.homeCfbdTeamId()).isEqualTo(2633L);
        assertThat(finished.homeTeam()).isEqualTo("Tennessee");
        assertThat(finished.homePoints()).isEqualTo(24);
        assertThat(finished.awayCfbdTeamId()).isEqualTo(333L);
        assertThat(finished.awayTeam()).isEqualTo("Alabama");
        assertThat(finished.awayPoints()).isEqualTo(17);

        CfbdGame upcoming = games.get(1);
        assertThat(upcoming.completed()).isFalse();
        assertThat(upcoming.cfbdVenueId()).isNull();
        assertThat(upcoming.homePoints()).isNull();
        assertThat(upcoming.awayPoints()).isNull();
        server.verify();
        assertKeyWasNotLogged();
    }

    @Test
    void gamesSendsOptionalWeekAndSeasonType() {
        expect(BASE + "/games?year=2024&conference=SEC&week=7&seasonType=postseason", "[]");

        assertThat(client.games(2024, 7, "postseason")).isEmpty();
        server.verify();
        assertKeyWasNotLogged();
    }

    @Test
    void calendarReadsWeeks() {
        expect(BASE + "/calendar?year=2024", """
                [
                  {
                    "season": 2024,
                    "week": 1,
                    "seasonType": "regular",
                    "startDate": "2024-08-24T00:00:00.000Z",
                    "endDate": "2024-09-02T23:59:59.000Z",
                    "firstGameStart": "2024-08-24T16:00:00.000Z",
                    "lastGameStart": "2024-09-02T00:00:00.000Z"
                  }
                ]
                """);

        List<CfbdCalendarWeek> weeks = client.calendar(2024);

        assertThat(weeks).hasSize(1);
        CfbdCalendarWeek week = weeks.get(0);
        assertThat(week.season()).isEqualTo(2024);
        assertThat(week.week()).isEqualTo(1);
        assertThat(week.seasonType()).isEqualTo("regular");
        assertThat(week.startDate()).isEqualTo("2024-08-24T00:00:00.000Z");
        assertThat(week.endDate()).isEqualTo("2024-09-02T23:59:59.000Z");
        server.verify();
        assertKeyWasNotLogged();
    }

    @Test
    void scoreboardReadsCurrentSecStatus() {
        expect(BASE + "/scoreboard?classification=fbs&conference=SEC", """
                [
                  {
                    "id": 401628319,
                    "startDate": "2024-10-12T16:00:00.000Z",
                    "status": "in_progress",
                    "weather": { "temperature": 72, "description": "Sunny" },
                    "betting": { "spread": -7.5 },
                    "venue": { "name": "Neyland Stadium", "city": "Knoxville", "state": "TN" },
                    "homeTeam": { "id": 2633, "name": "Tennessee", "points": 17 },
                    "awayTeam": { "id": 333, "name": "Alabama", "points": 14 }
                  }
                ]
                """);

        List<CfbdScoreboardGame> games = client.scoreboard();

        CfbdScoreboardGame game = games.get(0);
        assertThat(game.id()).isEqualTo(401628319L);
        assertThat(game.startDate()).isEqualTo("2024-10-12T16:00:00.000Z");
        assertThat(game.status()).isEqualTo("in_progress");
        assertThat(game.venue().name()).isEqualTo("Neyland Stadium");
        assertThat(game.venue().city()).isEqualTo("Knoxville");
        assertThat(game.venue().state()).isEqualTo("TN");
        Team home = game.homeTeam();
        assertThat(home.cfbdTeamId()).isEqualTo(2633L);
        assertThat(home.name()).isEqualTo("Tennessee");
        assertThat(home.points()).isEqualTo(17);
        assertThat(game.awayTeam().cfbdTeamId()).isEqualTo(333L);
        assertThat(game.awayTeam().points()).isEqualTo(14);
        server.verify();
        assertKeyWasNotLogged();
    }

    @Test
    void missingKeyFailsBeforeCallingCfbd() {
        CfbdClient unconfigured = clientWith("  ");

        assertThatThrownBy(() -> unconfigured.games(2024, null, null)).isInstanceOf(CfbdClientException.class)
                .hasMessage(CfbdClientException.MISSING_KEY);
        assertThatThrownBy(() -> clientWith(null).calendar(2024)).isInstanceOf(CfbdClientException.class)
                .hasMessage(CfbdClientException.MISSING_KEY);
        server.verify();
    }

    @Test
    void upstreamErrorsStayControlledAndDoNotEchoTheKey() {
        expectStatus(BASE + "/games?year=2024&conference=SEC", withUnauthorizedRequest()
                .body("{\"message\":\"bad " + API_KEY + "\"}").contentType(MediaType.APPLICATION_JSON));
        assertFailure(() -> client.games(2024, null, null), 401, CfbdClientException.REJECTED_KEY);
        nextExchange();

        expectStatus(BASE + "/calendar?year=2024", withTooManyRequests());
        assertFailure(() -> client.calendar(2024), 429, CfbdClientException.RATE_LIMITED);
        nextExchange();

        expectStatus(BASE + "/scoreboard?classification=fbs&conference=SEC", withServerError());
        assertFailure(client::scoreboard, 500, CfbdClientException.UNAVAILABLE);
        nextExchange();

        expectStatus(BASE + "/games?year=2024&conference=SEC", withBadRequest());
        assertFailure(() -> client.games(2024, null, null), 400, CfbdClientException.REJECTED);
        nextExchange();

        expectStatus(BASE + "/calendar?year=2024", withStatus(HttpStatus.BAD_GATEWAY));
        assertFailure(() -> client.calendar(2024), 502, CfbdClientException.UNAVAILABLE);
        server.verify();
        assertKeyWasNotLogged();
    }

    @Test
    void transportAndParseFailuresStayControlled() {
        server.expect(requestTo(BASE + "/games?year=2024&conference=SEC"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andRespond(withException(new IOException("connection reset " + API_KEY)));

        assertThatThrownBy(() -> client.games(2024, null, null)).isInstanceOf(CfbdClientException.class)
                .hasMessage(CfbdClientException.UNREACHABLE).hasNoCause();
        nextExchange();

        server.expect(requestTo(BASE + "/calendar?year=2024"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andRespond(withSuccess("not-json " + API_KEY, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.calendar(2024)).isInstanceOf(CfbdClientException.class)
                .hasMessage(CfbdClientException.UNEXPECTED).hasNoCause();
        nextExchange();

        server.expect(requestTo(BASE + "/scoreboard?classification=fbs&conference=SEC"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThatThrownBy(client::scoreboard).isInstanceOf(CfbdClientException.class)
                .hasMessage(CfbdClientException.EMPTY);
        server.verify();
        assertKeyWasNotLogged();
    }

    private void assertFailure(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, int status, String message) {
        assertThatThrownBy(call).isInstanceOf(CfbdClientException.class).hasMessage(message).hasNoCause()
                .satisfies(error -> assertThat(((CfbdClientException) error).upstreamStatus()).isEqualTo(status));
        assertThat(message).doesNotContain(API_KEY);
    }

    private void expect(String url, String json) {
        server.expect(requestTo(url)).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(request -> assertThat(request.getURI()).isEqualTo(URI.create(url)))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    private void expectStatus(String url,
            org.springframework.test.web.client.response.DefaultResponseCreator response) {
        server.expect(requestTo(url)).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andRespond(response);
    }

    private void nextExchange() {
        server.verify();
        server.reset();
    }

    private void assertKeyWasNotLogged() {
        assertThat(logs.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(API_KEY);
            if (event.getThrowableProxy() != null) {
                assertThat(event.getThrowableProxy().getMessage()).doesNotContain(API_KEY);
            }
        });
    }

    private CfbdClient clientWith(String apiKey) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE).configureMessageConverters(converters -> {
            HttpMessageConverters.ClientBuilder http = Objects.requireNonNull(converters, "converters");
            http.disableDefaults().withJsonConverter(new JacksonJsonHttpMessageConverter());
        });
        server = MockRestServiceServer.bindTo(builder).build();
        return new CfbdClient(builder.build(), apiKey);
    }
}
