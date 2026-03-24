package com.minidb.minidb.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class QueryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void supportsLegacyTextResponse() throws Exception {
        mockMvc.perform(post("/query")
                .contentType(MediaType.TEXT_PLAIN)
                .content("SHOW TABLES"))
            .andExpect(status().isOk())
            .andExpect(content().string(anyOf(containsString("Tables"), containsString("No tables found."))));
    }

    @Test
    void supportsJsonResponseViaQueryParam() throws Exception {
        mockMvc.perform(post("/query?format=json")
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .content("SHOW TABLES"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.executionTimeMs").exists())
            .andExpect(jsonPath("$.raw").exists());
    }
}
