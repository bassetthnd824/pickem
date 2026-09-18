package com.curleesoft.pickem.backend.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.curleesoft.pickem.backend.support.FirestoreEmulatorSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTests extends FirestoreEmulatorSupport {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void springdocServesOpenApiSpec() throws Exception {
    mockMvc
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.openapi").exists())
      .andExpect(jsonPath("$.info.title").value("Kenney's Pickem API"))
      .andExpect(content().string(containsString("openapi")));
  }
}
