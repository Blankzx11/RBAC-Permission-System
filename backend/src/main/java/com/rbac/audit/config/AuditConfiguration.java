package com.rbac.audit.config;

import com.rbac.audit.infrastructure.SnowflakeAuditIdGenerator;
import com.rbac.audit.port.AuditIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class AuditConfiguration {
    @Bean @ConditionalOnMissingBean(Clock.class)
    Clock auditClock() { return Clock.systemUTC(); }

    @Bean @ConditionalOnMissingBean(AuditIdGenerator.class)
    AuditIdGenerator auditIds(Clock clock, @Value("${audit.node-id:0}") int nodeId) {
        return new SnowflakeAuditIdGenerator(clock, nodeId);
    }
}
