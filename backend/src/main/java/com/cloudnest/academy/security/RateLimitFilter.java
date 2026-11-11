package com.cloudnest.academy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple fixed-window rate limiter per client IP for sensitive endpoints.
 * For multiple server instances, move this to Redis / AWS WAF rate-based rules.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int AUTH_LIMIT_PER_MINUTE = 20;
    private static final int API_LIMIT_PER_MINUTE = 600;
    private final Map<String, long[]> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        if (!path.startsWith("/api/") || path.startsWith("/api/webhooks/") || path.startsWith("/api/media/")) {
            chain.doFilter(req, res);
            return;
        }
        boolean auth = path.startsWith("/api/auth/") || path.startsWith("/api/contact") || path.startsWith("/api/newsletter");
        String key = (auth ? "a:" : "g:") + req.getRemoteAddr();
        long minute = System.currentTimeMillis() / 60000;
        long[] w = windows.compute(key, (k, v) -> (v == null || v[0] != minute) ? new long[]{minute, 1} : new long[]{minute, v[1] + 1});
        if (w[1] > (auth ? AUTH_LIMIT_PER_MINUTE : API_LIMIT_PER_MINUTE)) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Too many requests. Please wait a minute and try again.\"}");
            return;
        }
        if (windows.size() > 50_000) windows.clear();
        chain.doFilter(req, res);
    }
}
