package com.cloudnest.academy.security;

/** The authenticated principal stored in the security context. */
public record AuthUser(Long id, String email, String role) {}
