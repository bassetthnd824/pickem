package com.curleesoft.pickem.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.curleesoft.pickem.backend.cfbd.CfbdClientException;

class CfbdClientExceptionHandlerTests {

    @Test
    void cfbdFailuresAreServiceUnavailableWithoutAStack() throws Exception {
        List<String> messages = List.of(CfbdClientException.MISSING_KEY, CfbdClientException.REJECTED_KEY,
                CfbdClientException.RATE_LIMITED, CfbdClientException.UNAVAILABLE, CfbdClientException.REJECTED,
                CfbdClientException.UNREACHABLE, CfbdClientException.UNEXPECTED);

        for (String message : messages) {
            ProblemDetail problem = new ApiExceptionHandler().cfbd(new CfbdClientException(401, message));
            assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
            assertThat(problem.getDetail()).isEqualTo(message);
            assertThat(problem.getProperties()).isNull();

            String body = MockMvcBuilders.standaloneSetup(new CfbdFailureProbe(message))
                    .setControllerAdvice(new ApiExceptionHandler())
                    .setMessageConverters(new JacksonJsonHttpMessageConverter()).build().perform(get("/probe/cfbd"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value(message)).andExpect(jsonPath("$.status").value(503))
                    .andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain("cfbd-secret-do-not-log");
            assertThat(body).doesNotContain("CfbdClientException");
            assertThat(body).doesNotContain("\tat ");
            assertThat(body).doesNotContain("\"trace\"");
        }
    }

    @RestController
    static class CfbdFailureProbe {

        private final String message;

        CfbdFailureProbe(String message) {
            this.message = message;
        }

        @GetMapping("/probe/cfbd")
        public void fail() {
            throw new CfbdClientException(401, message);
        }
    }
}
