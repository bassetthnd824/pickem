package com.curleesoft.pickem.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests extends FirestoreEmulatorSupport {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void doesNotSendCorsAllowOriginHeaders() throws Exception {
    mockMvc
      .perform(
        get("/")
          .header("Origin", "http://localhost:3000")
      )
      .andExpect(status().isOk())
      .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }
}
