package com.navya.gcp.rest.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that logs each HTTP request with method, URI, status, and
 * elapsed time in milliseconds.
 *
 * <p>Output example:
 * <pre>
 *   [REQUEST]  POST /api/v1/appointments
 *   [RESPONSE] POST /api/v1/appointments -> 201 in 47ms
 * </pre>
 *
 * <p>Skips logging for actuator health and metrics endpoints to reduce noise.
 */
@Slf4j
@Component
@Order(2)
public class RequestLoggingFilter implements Filter {

    private static final String[] SKIP_PATHS = {"/actuator/health", "/actuator/prometheus"};

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        if (shouldSkip(uri)) {
            chain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        log.info("[REQUEST]  {} {}", req.getMethod(), uri);

        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("[RESPONSE] {} {} -> {} in {}ms",
                    req.getMethod(), uri, resp.getStatus(), elapsed);
        }
    }

    private boolean shouldSkip(String uri) {
        for (String path : SKIP_PATHS) {
            if (uri.startsWith(path)) return true;
        }
        return false;
    }
}
