package com.rbac.audit.demo;

import com.rbac.audit.web.AuditHttpErrors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Set;

@Configuration
@Profile("demo")
public class DemoSecurityConfiguration {
    @Bean
    PasswordEncoder demoPasswordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService demoUsers(@Value("${audit.demo-password:DemoOnly12345}") String password, PasswordEncoder encoder) {
        String encoded = encoder.encode(password);
        Set<String> audit = Set.of("sys:audit:read");
        var users = List.of(
            new DemoUser("platform_admin", encoded, 1, "平台管理员", null, true, Set.of()),
            new DemoUser("platform_a", encoded, 1, "平台管理员（系统A）", 1L, true, Set.of()),
            new DemoUser("auditor_a", encoded, 2, "系统A审计员", 1L, false, audit),
            new DemoUser("auditor_b", encoded, 3, "系统B审计员", 2L, false, audit),
            new DemoUser("ordinary_a", encoded, 4, "普通成员", 1L, false, Set.of())
        );
        // 每次返回新principal，避免认证完成后的eraseCredentials清空缓存口令。
        return username -> users.stream().filter(user -> user.getUsername().equals(username)).findFirst()
            .map(user -> new DemoUser(username, encoded, user.userId(), user.displayName(), user.sysId(), user.platformAdmin(), user.permissionCodes()))
            .orElseThrow(() -> new UsernameNotFoundException("Unknown demo user"));
    }

    @Bean
    SecurityFilterChain demoSecurity(HttpSecurity http, AuditHttpErrors errors) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().authenticated())
            .httpBasic(basic -> basic.authenticationEntryPoint(errors::demoLoginFailed))
            .exceptionHandling(e -> e.authenticationEntryPoint(errors::demoLoginFailed).accessDeniedHandler(errors::forbidden))
            .build();
    }
}
