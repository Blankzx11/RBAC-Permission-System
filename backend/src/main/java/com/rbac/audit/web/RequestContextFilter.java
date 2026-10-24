package com.rbac.audit.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component("auditRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID = RequestContextFilter.class.getName() + ".requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID, requestId);
        response.setHeader("X-Request-Id", requestId);
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }

    public static String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ID);
        return value instanceof String text ? text : UUID.randomUUID().toString();
    }
}
