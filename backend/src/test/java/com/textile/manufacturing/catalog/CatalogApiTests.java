package com.textile.manufacturing.catalog;

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
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogApiTests extends IntegrationTestBase {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String USERS_URL = "/api/v1/users";
    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String MATERIALS_URL = "/api/v1/materials";
    private static final String ADMIN_EMAIL = "admin@textile.test";
    private static final String ADMIN_PASSWORD = "admin-local-password";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    CatalogApiTests(@Autowired MockMvc mockMvc, @Autowired ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void plannerCanCreateAndReadProduct() throws Exception {
        String token = adminToken();
        String code = "CAT-PROD-" + uniqueCode();

        mockMvc.perform(post(PRODUCTS_URL)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody(code, "PIECE")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.outputUnit").value("PIECE"));

        mockMvc.perform(get(PRODUCTS_URL).param("status", "ACTIVE")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].code", hasItem(code)));
    }

    @Test
    void duplicateProductCodeReturnsConflict() throws Exception {
        String token = adminToken();
        String code = "CAT-DUP-" + uniqueCode();

        mockMvc.perform(post(PRODUCTS_URL)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody(code, "PIECE")))
            .andExpect(status().isCreated());

        mockMvc.perform(post(PRODUCTS_URL)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody(code, "PIECE")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Duplicate resource"));
    }

    @Test
    void updateProductChangesDetailsButNotCode() throws Exception {
        String token = adminToken();
        String code = "CAT-UPD-" + uniqueCode();
        UUID productId = createProduct(token, code);

        mockMvc.perform(patch(PRODUCTS_URL + "/" + productId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Renamed Shirt", "category": "Updated", "description": "New"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Renamed Shirt"))
            .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void archivedProductCannotBeUpdatedOrReArchived() throws Exception {
        String token = adminToken();
        String code = "CAT-ARC-" + uniqueCode();
        UUID productId = createProduct(token, code);

        mockMvc.perform(post(PRODUCTS_URL + "/" + productId + "/archival")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"))
            .andExpect(jsonPath("$.archivedAt").isNotEmpty());

        mockMvc.perform(patch(PRODUCTS_URL + "/" + productId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Should Fail", "category": null, "description": null}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Invalid state"));

        mockMvc.perform(post(PRODUCTS_URL + "/" + productId + "/archival")
                .header("Authorization", bearer(token)))
            .andExpect(status().isConflict());
    }

    @Test
    void invalidUnitValueReturnsBadRequest() throws Exception {
        String token = adminToken();

        mockMvc.perform(post(PRODUCTS_URL)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody("CAT-BAD-" + uniqueCode(), "INCHES")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void operatorRoleCannotCreateProduct() throws Exception {
        String adminToken = adminToken();

        String email = "operator-cat-" + uniqueCode() + "@textile.test";
        String password = "long-enough-password";
        String createOperatorBody = """
            {
              "email": "%s",
              "displayName": "Catalog Operator",
              "password": "%s",
              "roles": ["PRODUCTION_OPERATOR"]
            }
            """.formatted(email, password);

        mockMvc.perform(post(USERS_URL)
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createOperatorBody))
            .andExpect(status().isCreated());

        String operatorToken = loginToken(email, password);

        mockMvc.perform(post(PRODUCTS_URL)
                .header("Authorization", bearer(operatorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody("CAT-OP-" + uniqueCode(), "PIECE")))
            .andExpect(status().isForbidden());
    }

    @Test
    void duplicateMaterialCodeReturnsConflict() throws Exception {
        String token = adminToken();
        String code = "CAT-MAT-" + uniqueCode();

        mockMvc.perform(post(MATERIALS_URL)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(materialBody(code, "METER")))
            .andExpect(status().isCreated());

        mockMvc.perform(post(MATERIALS_URL)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(materialBody(code, "METER")))
            .andExpect(status().isConflict());
    }

    private String adminToken() throws Exception {
        return loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "%s", "password": "%s"}
                    """.formatted(email, password)))
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

    private String uniqueCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private UUID createProduct(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(post(PRODUCTS_URL)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productBody(code, "PIECE")))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String productId = body.path("id").asText();
        assertThat(productId).isNotBlank();
        return UUID.fromString(productId);
    }

    private String productBody(String code, String unit) {
        return """
            {
              "code": "%s",
              "name": "Test Product",
              "category": "Test",
              "description": "Created by CatalogApiTests",
              "outputUnit": "%s"
            }
            """.formatted(code, unit);
    }

    private String materialBody(String code, String unit) {
        return """
            {
              "code": "%s",
              "name": "Test Material",
              "materialType": "Fabric",
              "baseUnit": "%s"
            }
            """.formatted(code, unit);
    }
}
