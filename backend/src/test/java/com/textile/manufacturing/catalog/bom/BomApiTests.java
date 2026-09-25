package com.textile.manufacturing.catalog.bom;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BomApiTests extends IntegrationTestBase {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String ADMIN_EMAIL = "admin@textile.test";
    private static final String ADMIN_PASSWORD = "admin-local-password";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    BomApiTests(@Autowired MockMvc mockMvc, @Autowired ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void getBomReturnsNotFoundWhenNoBomExists() throws Exception {
        UUID productId = createProduct();

        mockMvc.perform(get(bomUrl(productId)).header("Authorization", bearer(adminToken())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Not found"));
    }

    @Test
    void plannerCanDefineBomWithJoinedMaterialDetails() throws Exception {
        UUID productId = createProduct();
        UUID materialId = createMaterial();

        mockMvc.perform(put(bomUrl(productId)).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bomBody(materialId, "1.5")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(productId.toString()))
            .andExpect(jsonPath("$.items[0].materialCode").isNotEmpty())
            .andExpect(jsonPath("$.items[0].unit").value("METER"))
            .andExpect(jsonPath("$.items[0].quantityPerProductUnit").value("1.500000"));
    }

    @Test
    void replacingBomSwapsTheLines() throws Exception {
        UUID productId = createProduct();
        UUID firstMaterial = createMaterial();
        UUID secondMaterial = createMaterial();

        mockMvc.perform(put(bomUrl(productId)).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bomBody(firstMaterial, "1")))
            .andExpect(status().isOk());

        mockMvc.perform(put(bomUrl(productId)).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bomBody(secondMaterial, "2.25")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
            .andExpect(jsonPath("$.items[0].quantityPerProductUnit").value("2.250000"));
    }

    @Test
    void replacingTheSameMaterialUpdatesQuantityInPlace() throws Exception {
        UUID productId = createProduct();
        UUID materialId = createMaterial();
        String token = adminToken();

        mockMvc.perform(put(bomUrl(productId)).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bomBody(materialId, "1.5")))
            .andExpect(status().isOk());

        mockMvc.perform(put(bomUrl(productId)).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bomBody(materialId, "5")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
            .andExpect(jsonPath("$.items[0].quantityPerProductUnit").value("5.000000"));
    }

    @Test
    void duplicateMaterialInBomReturnsBadRequest() throws Exception {
        UUID productId = createProduct();
        UUID materialId = createMaterial();

        mockMvc.perform(put(bomUrl(productId)).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items": [
                      {"materialId": "%s", "quantityPerProductUnit": 1},
                      {"materialId": "%s", "quantityPerProductUnit": 2}
                    ]}
                    """.formatted(materialId, materialId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void emptyBomReturnsBadRequest() throws Exception {
        UUID productId = createProduct();

        mockMvc.perform(put(bomUrl(productId)).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\": []}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void archivedMaterialCannotJoinBom() throws Exception {
        UUID productId = createProduct();
        UUID materialId = createMaterial();
        String token = adminToken();

        mockMvc.perform(post("/api/v1/materials/" + materialId + "/archival")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk());

        mockMvc.perform(put(bomUrl(productId)).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bomBody(materialId, "1")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Invalid reference"));
    }

    @Test
    void calculationReturnsRequiredQuantities() throws Exception {
        UUID productId = createProduct();
        UUID materialId = createMaterial();

        mockMvc.perform(put(bomUrl(productId)).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bomBody(materialId, "1.5")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/bom-calculations").header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"productId": "%s", "quantity": 100}
                    """.formatted(productId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].requiredQuantity").value("150.000000"));
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

    private String bomUrl(UUID productId) {
        return "/api/v1/products/" + productId + "/bom";
    }

    private String bomBody(UUID materialId, String quantity) {
        return """
            {
              "items": [
                {"materialId": "%s", "quantityPerProductUnit": %s}
              ]
            }
            """.formatted(materialId, quantity);
    }

    private UUID createProduct() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/products")
                .header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "BOM-PROD-%s",
                      "name": "BOM Test Product",
                      "category": "Test",
                      "description": null,
                      "outputUnit": "PIECE"
                    }
                    """.formatted(UUID.randomUUID().toString().substring(0, 8))))
            .andExpect(status().isCreated())
            .andReturn();

        return UUID.fromString(
            objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
    }

    private UUID createMaterial() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/materials")
                .header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "BOM-MAT-%s",
                      "name": "BOM Test Material",
                      "materialType": "Fabric",
                      "baseUnit": "METER"
                    }
                    """.formatted(UUID.randomUUID().toString().substring(0, 8))))
            .andExpect(status().isCreated())
            .andReturn();

        return UUID.fromString(
            objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
    }
}
