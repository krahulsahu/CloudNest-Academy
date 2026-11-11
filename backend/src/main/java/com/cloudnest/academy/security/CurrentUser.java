package com.cloudnest.academy.security;

import com.cloudnest.academy.config.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    /** Returns the logged-in user id or null for anonymous requests. */
    public static Long idOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AuthUser u) return u.id();
        return null;
    }

    public static Long id() {
        Long id = idOrNull();
        if (id == null) throw ApiException.unauthorized("Please log in");
        return id;
    }
}
