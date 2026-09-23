package com.textile.manufacturing.identity;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.textile.manufacturing.support.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTests extends IntegrationTestBase {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String USERS_URL = "/api/v1/users";
    private static final String ADMIN_EMAIL = "admin@textile.test";
    private static final String ADMIN_PASSWORD = "admin-local-password";

    private final MockMvc mockMvc;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    AuthApiTests(@Autowired MockMvc mockMvc, @Autowired com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void loginReturnsTokenAndCurrentUser() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(ADMIN_EMAIL, ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresAt").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value(ADMIN_EMAIL))
            .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"))
            .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void loginWithWrongPasswordReturnsGenericUnauthorized() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(ADMIN_EMAIL, "wrong-password-123")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void loginWithUnknownEmailReturnsSameGenericUnauthorized() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("nobody@textile.test", "whatever-password-1")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void realTokenAccessesProtectedEndpoint() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(ADMIN_EMAIL));

        mockMvc.perform(get(USERS_URL).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = adminToken();
        String tampered = token.substring(0, token.length() - 3) + "xxx";

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(tampered)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedUserCannotUseExistingTokenOrLogin() throws Exception {
        String adminToken = adminToken();

        String email = "deactivated@textile.test";
        String createBody = """
            {
              "email": "%s",
              "displayName": "To Be Deactivated",
              "password": "long-enough-password",
              "roles": ["PLANNER"]
            }
            """.formatted(email);

        MvcResult createResult = mockMvc.perform(post(USERS_URL)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode createdUser = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String userId = createdUser.path("id").asText();
        assertThat(userId).isNotBlank();

        String plannerToken = loginToken(email, "long-enough-password");

        mockMvc.perform(post(USERS_URL + "/" + userId + "/deactivation")
                .header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get(USERS_URL).header("Authorization", bearer(plannerToken)))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, "long-enough-password")))
            .andExpect(status().isUnauthorized());
    }

    private String adminToken() throws Exception {
        return loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String loginBody(String email, String password) {
        return """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);
    }
}
