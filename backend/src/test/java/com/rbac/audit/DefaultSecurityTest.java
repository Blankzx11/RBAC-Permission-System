package com.rbac.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DefaultSecurityTest {
    @Autowired MockMvc mvc;

    @Test void nonDemoDoesNotAcceptUnverifiedBearerOrIdentityHeaders() throws Exception {
        mvc.perform(get("/api/v1/platform/audit-logs").header("Authorization", "Bearer invented-token").header("X-Platform-Admin", "true")).andExpect(status().isUnauthorized());
    }
}
