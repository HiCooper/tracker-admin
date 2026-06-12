package com.gateflow.tracker;

import com.gateflow.tracker.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldReturn403WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/spm"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WithValidToken() throws Exception {
        String token = jwtUtil.generateToken("test_user", "ADMIN");
        mockMvc.perform(get("/api/v1/spm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/spm")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldPermitHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void loginShouldReturnToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }
}
