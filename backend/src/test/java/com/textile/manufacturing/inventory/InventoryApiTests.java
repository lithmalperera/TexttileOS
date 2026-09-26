package com.textile.manufacturing.inventory;

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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryApiTests extends IntegrationTestBase {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String MATERIALS_URL = "/api/v1/materials";
    private static final String RECEIPTS_URL = "/api/v1/inventory/receipts";
    private static final String ADJUSTMENTS_URL = "/api/v1/inventory/adjustments";
    private static final String ADMIN_EMAIL = "admin@textile.test";
    private static final String ADMIN_PASSWORD = "admin-local-password";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    InventoryApiTests(@Autowired MockMvc mockMvc, @Autowired ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void receiptCreatesBalanceAndLedgerEntry() throws Exception {
        UUID materialId = createMaterial();

        mockMvc.perform(post(RECEIPTS_URL).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptBody(materialId, "100", "PO #42 delivery")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onHandQuantity").value("100.000000"))
            .andExpect(jsonPath("$.availableQuantity").value("100.000000"))
            .andExpect(jsonPath("$.inventoryItemId").isNotEmpty());

        mockMvc.perform(get("/api/v1/inventory/movements").param("materialId", materialId.toString())
                .header("Authorization", bearer(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].transactionType").value("RECEIPT"))
            .andExpect(jsonPath("$[0].onHandDelta").value("100.000000"));
    }

    @Test
    void multipleReceiptsAccumulate() throws Exception {
        UUID materialId = createMaterial();

        mockMvc.perform(post(RECEIPTS_URL).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptBody(materialId, "100", "first")))
            .andExpect(status().isOk());

        mockMvc.perform(post(RECEIPTS_URL).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptBody(materialId, "50", "second")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onHandQuantity").value("150.000000"));

        mockMvc.perform(get("/api/v1/inventory/movements").param("materialId", materialId.toString())
                .header("Authorization", bearer(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void adjustmentUpdatesBalanceAndCreatesLedgerEntry() throws Exception {
        UUID materialId = createMaterial();

        mockMvc.perform(post(RECEIPTS_URL).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptBody(materialId, "100", "initial")))
            .andExpect(status().isOk());

        mockMvc.perform(post(ADJUSTMENTS_URL).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"materialId": "%s", "onHandDelta": -10, "reason": "Damaged roll removed"}
                    """.formatted(materialId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onHandQuantity").value("90.000000"));

        mockMvc.perform(get("/api/v1/inventory/movements").param("materialId", materialId.toString())
                .header("Authorization", bearer(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].transactionType").value("ADJUSTMENT"))
            .andExpect(jsonPath("$[0].onHandDelta").value("-10.000000"));
    }

    @Test
    void adjustmentBelowZeroIsRejected() throws Exception {
        UUID materialId = createMaterial();

        mockMvc.perform(post(RECEIPTS_URL).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptBody(materialId, "100", "initial")))
            .andExpect(status().isOk());

        mockMvc.perform(post(ADJUSTMENTS_URL).header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"materialId": "%s", "onHandDelta": -1000, "reason": "should fail"}
                    """.formatted(materialId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void receiptForArchivedMaterialIsRejected() throws Exception {
        UUID materialId = createMaterial();
        String token = adminToken();

        mockMvc.perform(post(MATERIALS_URL + "/" + materialId + "/archival")
                .header("Authorization", bearer(token)))
            .andExpect(status().isOk());

        mockMvc.perform(post(RECEIPTS_URL).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptBody(materialId, "50", "late delivery")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Invalid reference"));
    }

    @Test
    void inventoryManagerRoleCanReceiveButPlannerCannot() throws Exception {
        String adminToken = adminToken();
        UUID materialId = createMaterial();

        String managerEmail = "inv-manager-" + uniqueCode() + "@textile.test";
        String managerPassword = "long-enough-password";
        createUserWithRole(adminToken, managerEmail, managerPassword, "INVENTORY_MANAGER");
        String managerToken = loginToken(managerEmail, managerPassword);

        String plannerEmail = "inv-planner-" + uniqueCode() + "@textile.test";
        createUserWithRole(adminToken, plannerEmail, managerPassword, "PLANNER");
        String plannerToken = loginToken(plannerEmail, managerPassword);

        mockMvc.perform(post(RECEIPTS_URL).header("Authorization", bearer(managerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptBody(materialId, "40", "manager receipt")))
            .andExpect(status().isOk());

        mockMvc.perform(post(RECEIPTS_URL).header("Authorization", bearer(plannerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(receiptBody(materialId, "40", "planner must not receive")))
            .andExpect(status().isForbidden());
    }

    @Test
    void movementsForNeverReceivedMaterialAreEmpty() throws Exception {
        UUID materialId = createMaterial();

        mockMvc.perform(get("/api/v1/inventory/movements").param("materialId", materialId.toString())
                .header("Authorization", bearer(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
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

    private void createUserWithRole(String adminToken, String email, String password, String role) throws Exception {
        mockMvc.perform(post("/api/v1/users").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "displayName": "Test User",
                      "password": "%s",
                      "roles": ["%s"]
                    }
                    """.formatted(email, password, role)))
            .andExpect(status().isCreated());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private UUID createMaterial() throws Exception {
        MvcResult result = mockMvc.perform(post(MATERIALS_URL)
                .header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "INV-MAT-%s",
                      "name": "Inventory Test Material",
                      "materialType": "Fabric",
                      "baseUnit": "METER"
                    }
                    """.formatted(uniqueCode())))
            .andExpect(status().isCreated())
            .andReturn();

        return UUID.fromString(
            objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
    }

    private String receiptBody(UUID materialId, String quantity, String reason) {
        return """
            {
              "materialId": "%s",
              "quantity": %s,
              "reason": "%s"
            }
            """.formatted(materialId, quantity, reason);
    }
}
