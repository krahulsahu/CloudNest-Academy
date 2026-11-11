package com.cloudnest.academy.security;

import com.cloudnest.academy.config.AppConfig;
import com.cloudnest.academy.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal, dependency-free HS256 JWT implementation for short-lived access tokens. */
@Service
public class JwtService {
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private static final String HEADER = ENC.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final AppConfig cfg;
    private final ObjectMapper mapper;

    public JwtService(AppConfig cfg, ObjectMapper mapper) {
        this.cfg = cfg;
        this.mapper = mapper;
        if (cfg.jwtSecret == null || cfg.jwtSecret.length() < 32) {
            throw new IllegalStateException("app.jwt-secret (JWT_SECRET) must be at least 32 characters");
        }
    }

    public String issue(User u) {
        try {
            long now = Instant.now().getEpochSecond();
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("sub", String.valueOf(u.id));
            claims.put("email", u.email);
            claims.put("role", u.role.name());
            claims.put("iat", now);
            claims.put("exp", now + cfg.accessTokenMinutes * 60);
            String payload = ENC.encodeToString(mapper.writeValueAsBytes(claims));
            String unsigned = HEADER + "." + payload;
            return unsigned + "." + Crypto.hmacUrl(cfg.jwtSecret, unsigned);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create token", e);
        }
    }

    /** Returns the principal or null when the token is invalid or expired. */
    public AuthUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !parts[0].equals(HEADER)) return null;
            String expected = Crypto.hmacUrl(cfg.jwtSecret, parts[0] + "." + parts[1]);
            if (!Crypto.safeEquals(expected, parts[2])) return null;
            Map<?, ?> claims = mapper.readValue(DEC.decode(parts[1]), Map.class);
            long exp = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) return null;
            return new AuthUser(Long.valueOf((String) claims.get("sub")), (String) claims.get("email"), (String) claims.get("role"));
        } catch (Exception e) {
            return null;
        }
    }
}
