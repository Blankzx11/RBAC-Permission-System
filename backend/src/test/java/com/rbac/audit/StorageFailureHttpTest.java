package com.rbac.audit;

import com.rbac.audit.port.AuditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"demo", "test"})
class StorageFailureHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuditRepository repository;

    @Test void databaseFailureReturnsSanitized503() throws Exception {
        when(repository.query(any())).thenThrow(new DataAccessResourceFailureException("private-sql-and-password"));
        mvc.perform(get("/api/v1/systems/1/audit-logs").with(httpBasic("auditor_a", "DemoOnly12345")))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-sql"))));
    }
}
