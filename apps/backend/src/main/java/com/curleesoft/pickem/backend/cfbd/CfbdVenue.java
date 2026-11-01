package com.curleesoft.pickem.backend.cfbd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One row from CFBD {@code GET /venues}. City and state may be null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CfbdVenue(long id, String name, String city, String state) {
}
