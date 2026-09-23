package com.textile.manufacturing.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.textile.manufacturing.support.IntegrationTestBase;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiTests extends IntegrationTestBase {

    private static final String USERS_URL = "/api/v1/users";

    private static final String VALID_CREATE_BODY = """
        {
          "email": "planner@textile.test",
          "displayName": "Test Planner",
          "password": "long-enough-password",
          "roles": ["PLANNER"]
        }
        """;

    private static String bodyWith(String email) {
        return """
            {
              "email": "%s",
              "displayName": "Test Planner",
              "password": "long-enough-password",
              "roles": ["PLANNER"]
            }
            """.formatted(email);
    }

    private final MockMvc mockMvc;

    UserApiTests(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateUser() throws Exception {
        mockMvc.perform(post(USERS_URL)
                .contentType(APPLICATION_JSON)
                .content(VALID_CREATE_BODY))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(USERS_URL + "/")))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.email").value("planner@textile.test"))
            .andExpect(jsonPath("$.displayName").value("Test Planner"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.roles[0]").value("PLANNER"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListUsers() throws Exception {
        mockMvc.perform(post(USERS_URL)
                .contentType(APPLICATION_JSON)
                .content(bodyWith("list-user@textile.test")))
            .andExpect(status().isCreated());

        mockMvc.perform(get(USERS_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateEmailReturnsConflict() throws Exception {
        String email = "duplicate@textile.test";

        mockMvc.perform(post(USERS_URL)
                .contentType(APPLICATION_JSON)
                .content(bodyWith(email)))
            .andExpect(status().isCreated());

        mockMvc.perform(post(USERS_URL)
                .contentType(APPLICATION_JSON)
                .content(bodyWith(email)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Duplicate resource"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingUserReturnsNotFound() throws Exception {
        mockMvc.perform(get(USERS_URL + "/" + UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidRequestReturnsBadRequestWithFieldErrors() throws Exception {
        String invalidBody = """
            {
              "email": "not-an-email",
              "displayName": "",
              "password": "short",
              "roles": []
            }
            """;

        mockMvc.perform(post(USERS_URL)
                .contentType(APPLICATION_JSON)
                .content(invalidBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    void nonAdminReceivesForbidden() throws Exception {
        mockMvc.perform(get(USERS_URL))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestReceivesUnauthorized() throws Exception {
        mockMvc.perform(get(USERS_URL))
            .andExpect(status().isUnauthorized());
    }
}
