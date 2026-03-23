package com.navya.gcp.rest.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that injects a correlation ID into the MDC for every request.
 *
 * <p>The correlation ID is:
 * <ol>
 *   <li>Read from the {@code X-Correlation-Id} request header if present</li>
 *   <li>Generated as a fresh UUID if the header is absent</li>
 * </ol>
 *
 * <p>The ID is:
 * <ul>
 *   <li>Added to MDC as {@code correlationId} — appears in every log line</li>
 *   <li>Written back to the response as {@code X-Correlation-Id}</li>
 *   <li>Cleared from MDC after the request completes</li>
 * </ul>
 *
 * <p>Configure logback pattern to include {@code %X{correlationId}}:
 * <pre>
 *   %d{ISO8601} [%thread] [%X{correlationId}] %-5level %logger - %msg%n
 * </pre>
 */
@Slf4j
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY            = "correlationId";
    public static final String REQUEST_ID_KEY     = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpReq  = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String correlationId = httpReq.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String requestId = UUID.randomUUID().toString();

        try {
            MDC.put(MDC_KEY,        correlationId);
            MDC.put(REQUEST_ID_KEY, requestId);
            MDC.put("method",       httpReq.getMethod());
            MDC.put("uri",          httpReq.getRequestURI());

            httpResp.setHeader(CORRELATION_HEADER, correlationId);
            httpResp.setHeader("X-Request-Id",     requestId);

            log.debug("Inbound {} {}", httpReq.getMethod(), httpReq.getRequestURI());

            chain.doFilter(request, response);

        } finally {
            MDC.remove(MDC_KEY);
            MDC.remove(REQUEST_ID_KEY);
            MDC.remove("method");
            MDC.remove("uri");
        }
    }
}
