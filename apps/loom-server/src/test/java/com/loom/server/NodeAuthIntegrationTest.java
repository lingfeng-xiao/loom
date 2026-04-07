package com.loom.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NodeAuthIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsUnauthorizedNodeRegistration() throws Exception {
        mockMvc.perform(post("/api/nodes/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "unauthorized-node",
                                  "type": "server",
                                  "host": "127.0.0.1",
                                  "tags": [],
                                  "capabilities": []
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsAuthorizedNodeRegistration() throws Exception {
        mockMvc.perform(post("/api/nodes/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "authorized-node",
                                  "type": "server",
                                  "host": "127.0.0.1",
                                  "tags": [],
                                  "capabilities": []
                                }
                                """))
                .andExpect(status().isOk());
    }
}
