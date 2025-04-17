package com.thehoodjunction.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class RequestLoggingConfig {

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/actuator",
            "/swagger-ui",
            "/api-docs"
    );

    @Bean
    public OncePerRequestFilter requestLoggingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                
                if (isExcludedPath(request.getRequestURI())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // Generate a unique request ID
                String requestId = UUID.randomUUID().toString().substring(0, 8);
                
                // Wrap request and response to cache content
                ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
                
                long startTime = System.currentTimeMillis();
                
                // Log request details
                logRequest(requestWrapper, requestId);
                
                try {
                    // Process the request
                    filterChain.doFilter(requestWrapper, responseWrapper);
                } finally {
                    // Log response details
                    long duration = System.currentTimeMillis() - startTime;
                    logResponse(responseWrapper, requestId, duration);
                    
                    // Copy content back to the original response
                    responseWrapper.copyBodyToResponse();
                }
            }
            
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return false;
            }
        };
    }
    
    private boolean isExcludedPath(String requestURI) {
        return EXCLUDED_PATHS.stream().anyMatch(requestURI::startsWith);
    }
    
    private void logRequest(ContentCachingRequestWrapper request, String requestId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = uri + (queryString != null ? "?" + queryString : "");
        
        // Get headers
        String headers = Collections.list(request.getHeaderNames())
                .stream()
                .map(headerName -> headerName + ": " + request.getHeader(headerName))
                .collect(Collectors.joining(", "));
        
        // Build beautiful log message
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n┌─────────────────────────────────────────────────────────────────────────────────┐\n");
        logMessage.append(String.format("│ 🔔 REQUEST [%s] %s %s                                                 \n", requestId, method, fullUrl));
        logMessage.append("├─────────────────────────────────────────────────────────────────────────────────┤\n");
        logMessage.append(String.format("│ 📋 Headers: %s                                                       \n", headers));
        
        // Log request body for POST, PUT, PATCH
        if (Arrays.asList("POST", "PUT", "PATCH").contains(method)) {
            String requestBody = getRequestBody(request);
            if (requestBody != null && !requestBody.isEmpty()) {
                logMessage.append("├─────────────────────────────────────────────────────────────────────────────────┤\n");
                logMessage.append(String.format("│ 📦 Body: %s                                                          \n", requestBody));
            }
        }
        
        logMessage.append("└─────────────────────────────────────────────────────────────────────────────────┘");
        log.info(logMessage.toString());
    }
    
    private void logResponse(ContentCachingResponseWrapper response, String requestId, long duration) {
        int status = response.getStatus();
        String statusText = getStatusText(status);
        
        // Build beautiful log message
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n┌─────────────────────────────────────────────────────────────────────────────────┐\n");
        logMessage.append(String.format("│ 🔔 RESPONSE [%s] %d %s (%d ms)                                           \n", 
                requestId, status, statusText, duration));
        logMessage.append("├─────────────────────────────────────────────────────────────────────────────────┤\n");
        
        // Get response body
        String responseBody = getResponseBody(response);
        if (responseBody != null && !responseBody.isEmpty()) {
            logMessage.append(String.format("│ 📦 Body: %s                                                          \n", responseBody));
        }
        
        logMessage.append("└─────────────────────────────────────────────────────────────────────────────────┘");
        
        // Use appropriate log level based on status code
        if (status >= 400 && status < 500) {
            log.warn(logMessage.toString());
        } else if (status >= 500) {
            log.error(logMessage.toString());
        } else {
            log.info(logMessage.toString());
        }
    }
    
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        
        try {
            String body = new String(content, request.getCharacterEncoding());
            
            // Truncate if too long
            if (body.length() > 1000) {
                body = body.substring(0, 1000) + "... (truncated)";
            }
            
            return body;
        } catch (UnsupportedEncodingException e) {
            log.warn("Could not read request body", e);
            return "[Could not read request body]";
        }
    }
    
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        
        try {
            String body = new String(content, response.getCharacterEncoding());
            
            // Truncate if too long
            if (body.length() > 1000) {
                body = body.substring(0, 1000) + "... (truncated)";
            }
            
            return body;
        } catch (UnsupportedEncodingException e) {
            log.warn("Could not read response body", e);
            return "[Could not read response body]";
        }
    }
    
    private String getStatusText(int status) {
        switch (status) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            default: return "Unknown";
        }
    }
}
