package com.curleesoft.pickem.backend.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.curleesoft.pickem.backend.cfbd.CfbdClient;
import com.curleesoft.pickem.backend.security.InternalJobFilter;
import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;

@SpringBootTest
@AutoConfigureMockMvc
class InternalJobLoopbackDisabledTests extends FirestoreEmulatorSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CfbdClient cfbdClient;

    @DynamicPropertySource
    static void requireSecret(DynamicPropertyRegistry registry) {
        registry.add("pickem.internal.job-secret", () -> "test-secret");
        registry.add("pickem.internal.allow-loopback", () -> "false");
    }

    @BeforeEach
    void stubCfbd() {
        lenient().when(cfbdClient.games(anyInt(), isNull(), any())).thenReturn(List.of());
    }

    @Test
    void loopbackWithoutTheSecretIsRejected() throws Exception {
        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").with(from("127.0.0.1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loopbackWithTheSecretRunsTheScoreJob() throws Exception {
        mockMvc.perform(post("/api/internal/jobs/cfbd-scores").header(InternalJobFilter.SECRET_HEADER, "test-secret")
                .with(from("127.0.0.1"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("cfbd-scores"));
    }

    private static RequestPostProcessor from(String remoteAddress) {
        String address = Objects.requireNonNull(remoteAddress, "remoteAddress");
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
