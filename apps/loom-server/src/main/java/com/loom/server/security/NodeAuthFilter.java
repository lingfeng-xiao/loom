package com.loom.server.security;

import com.loom.server.config.LoomProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class NodeAuthFilter extends OncePerRequestFilter {

    private final LoomProperties properties;

    public NodeAuthFilter(LoomProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.equals("/api/nodes/register")
                || path.matches("^/api/nodes/[^/]+/heartbeat$")
                || path.matches("^/api/nodes/[^/]+/snapshot$"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String expectedToken = properties.getNodes().getServerToken();
        String actualHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String expectedHeader = "Bearer " + expectedToken;
        if (expectedToken == null || expectedToken.isBlank() || expectedHeader.equals(actualHeader)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"unauthorized node request\"}");
    }
}
