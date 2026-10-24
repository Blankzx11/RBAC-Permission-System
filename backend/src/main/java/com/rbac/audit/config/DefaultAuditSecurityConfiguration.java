package com.rbac.audit.config;

import com.rbac.audit.web.AuditHttpErrors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!demo")
public class DefaultAuditSecurityConfiguration {
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain auditClosedByDefault(HttpSecurity http, AuditHttpErrors errors) throws Exception {
        // 集成前不接受任意Bearer字符串或客户端身份头，A/E接入后由宿主提供认证链。
        return http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().denyAll())
            .exceptionHandling(e -> e.authenticationEntryPoint(errors::unauthenticated).accessDeniedHandler(errors::forbidden))
            .build();
    }
}
