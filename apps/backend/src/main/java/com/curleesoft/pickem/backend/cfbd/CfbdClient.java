package com.curleesoft.pickem.backend.cfbd;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * Server-side client for {@code https://api.collegefootballdata.com}. Sends
 * {@code Authorization: Bearer} and does not log the API key. A missing key
 * fails on the first call so the rest of the API can still start.
 */
public final class CfbdClient {

    static final String SEC = "SEC";

    static final String FBS = "fbs";

    private static final Logger log = LoggerFactory.getLogger(CfbdClient.class);

    private static final ParameterizedTypeReference<List<CfbdGame>> GAMES = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<List<CfbdCalendarWeek>> WEEKS = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<List<CfbdScoreboardGame>> SCOREBOARD = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    private final String apiKey;

    public CfbdClient(RestClient restClient, String apiKey) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    /**
     * {@code GET /games?year=&conference=SEC}. {@code week} and
     * {@code seasonType} are omitted when empty.
     */
    public List<CfbdGame> games(int year, Integer week, String seasonType) {
        return get("games", builder -> {
            UriBuilder uri = Objects.requireNonNull(builder, "uriBuilder").path("/games").queryParam("year", year)
                    .queryParam("conference", SEC);
            if (week != null) {
                Integer weekNumber = Objects.requireNonNull(week, "week");
                uri.queryParam("week", weekNumber);
            }
            if (StringUtils.hasText(seasonType)) {
                String type = Objects.requireNonNull(seasonType, "seasonType");
                uri.queryParam("seasonType", type);
            }
            return Objects.requireNonNull(uri.build(), "uri");
        }, GAMES);
    }

    /**
     * {@code GET /calendar?year=}.
     */
    public List<CfbdCalendarWeek> calendar(int year) {
        return get("calendar", builder -> {
            URI uri = Objects.requireNonNull(builder, "uriBuilder").path("/calendar").queryParam("year", year).build();
            return Objects.requireNonNull(uri, "uri");
        }, WEEKS);
    }

    /**
     * {@code GET /scoreboard?classification=fbs&conference=SEC}.
     */
    public List<CfbdScoreboardGame> scoreboard() {
        return get("scoreboard", builder -> {
            URI uri = Objects.requireNonNull(builder, "uriBuilder").path("/scoreboard")
                    .queryParam("classification", FBS).queryParam("conference", SEC).build();
            return Objects.requireNonNull(uri, "uri");
        }, SCOREBOARD);
    }

    private <T> List<T> get(String operation, Function<UriBuilder, URI> uri, ParameterizedTypeReference<List<T>> type) {
        if (!StringUtils.hasText(apiKey)) {
            throw new CfbdClientException(CfbdClientException.MISSING_KEY);
        }

        String credential = Objects.requireNonNull(apiKey, "apiKey");
        String path = Objects.requireNonNull(operation, "operation");
        Function<UriBuilder, URI> uriFunction = Objects.requireNonNull(uri, "uri");
        ParameterizedTypeReference<List<T>> bodyType = Objects.requireNonNull(type, "bodyType");

        try {
            List<T> body = Objects.requireNonNull(restClient.get(), "request").uri(uriFunction)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + credential).retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> fail(path, request, response))
                    .body(bodyType);
            if (body == null) {
                log.warn("CFBD {} returned no body", path);
                throw new CfbdClientException(CfbdClientException.EMPTY);
            }
            return body;
        } catch (CfbdClientException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            CfbdClientException upstream = findCfbd(ex);
            if (upstream != null) {
                throw upstream;
            }
            logFailure(path, credential, ex);
            if (ex instanceof ResourceAccessException) {
                throw new CfbdClientException(CfbdClientException.UNREACHABLE);
            }
            throw new CfbdClientException(CfbdClientException.UNEXPECTED);
        }
    }

    private static void fail(String path, HttpRequest request, ClientHttpResponse response) throws IOException {
        Objects.requireNonNull(request, "request");
        ClientHttpResponse error = Objects.requireNonNull(response, "response");
        int status = error.getStatusCode().value();
        log.warn("CFBD {} failed with status {}", path, status);
        throw new CfbdClientException(status, CfbdClientException.messageFor(status));
    }

    private static void logFailure(String path, String credential, RuntimeException ex) {
        String message = ex.getMessage();
        if (message != null && StringUtils.hasText(credential)) {
            message = message.replace(credential, "[redacted]");
        }
        log.warn("CFBD {} call failed ({}): {}", path, ex.getClass().getSimpleName(), message);
    }

    private static CfbdClientException findCfbd(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof CfbdClientException cfbd) {
                return cfbd;
            }
            current = current.getCause();
        }
        return null;
    }
}
