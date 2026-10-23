package com.example.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class RbacBackendIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void effectivePermissionsMenusAndUnifiedAuthorizationFormClosedLoop() throws Exception {
        mockMvc.perform(get("/api/v1/me/permissions"))
                .andExpect(status().isUnauthorized());

        String adminToken = login("platform_admin", "1");
        mockMvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sysId").value("1"))
                .andExpect(jsonPath("$.data.permissionCodes").value(org.hamcrest.Matchers.hasItem("sys:role:grant")))
                .andExpect(jsonPath("$.data.permissionCodes").value(org.hamcrest.Matchers.hasItem("sys:audit:read")));
        mockMvc.perform(get("/api/v1/me/menus").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));

        String readerToken = login("chenyu", "1");
        mockMvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(readerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCodes").value(org.hamcrest.Matchers.hasItem("sys:member:read")))
                .andExpect(jsonPath("$.data.permissionCodes").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("sys:role:grant"))));
        mockMvc.perform(get("/api/v1/systems/1/roles").header("Authorization", bearer(readerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/systems/2/roles").header("Authorization", bearer(readerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        String auditorToken = login("auditor", "1");
        mockMvc.perform(get("/api/v1/me/menus").header("Authorization", bearer(auditorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("审计日志"))
                .andExpect(jsonPath("$.data[0].navigable").value(true));

        MvcResult rolesResult = mockMvc.perform(get("/api/v1/systems/1/members/4/roles")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleIds[0]").value("103"))
                .andExpect(header().exists("ETag"))
                .andReturn();
        String etag = rolesResult.getResponse().getHeader("ETag");
        assertThat(etag).isEqualTo("\"1\"");

        mockMvc.perform(put("/api/v1/systems/1/members/4/roles")
                        .header("Authorization", bearer(adminToken))
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[\"104\"]}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.data.roleIds[0]").value("104"))
                .andExpect(jsonPath("$.data.version").value("2"));

        String changedReaderToken = login("chenyu", "1");
        mockMvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(changedReaderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCodes").value(org.hamcrest.Matchers.hasItem("sys:audit:read")))
                .andExpect(jsonPath("$.data.permissionCodes").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("sys:member:read"))));

        String systemBToken = login("chenyu", "2");
        mockMvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(systemBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sysId").value("2"))
                .andExpect(jsonPath("$.data.permissionCodes").value(org.hamcrest.Matchers.hasItem("sys:member:read")))
                .andExpect(jsonPath("$.data.permissionCodes").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("sys:audit:read"))));
    }

    private String login(String username, String sysId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"DemoOnly12345\",\"sysId\":\""
                                + sysId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.at("/data/accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
