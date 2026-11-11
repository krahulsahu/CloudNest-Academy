package com.cloudnest.academy.service;

import com.cloudnest.academy.config.AppConfig;
import com.cloudnest.academy.domain.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Single place that decides whether a user may open a course. Enforced on the backend for every lesson. */
@Service
public class AccessService {
    private final EnrollmentRepository enrollments;
    private final SubscriptionRepository subscriptions;
    private final AppConfig cfg;

    public AccessService(EnrollmentRepository enrollments, SubscriptionRepository subscriptions, AppConfig cfg) {
        this.enrollments = enrollments;
        this.subscriptions = subscriptions;
        this.cfg = cfg;
    }

    public boolean canManage(User u, Course c) {
        return u != null && (u.role == User.Role.ADMIN || (c.instructor != null && Objects.equals(c.instructor.id, u.id)));
    }

    public boolean hasAccess(User u, Course c) {
        if (u == null) return false;
        if (canManage(u, c)) return true;
        if (c.kind == Course.Kind.BUNDLE) {
            if (c.bundleItems.isEmpty()) return false;
            for (Course item : c.bundleItems) if (!hasAccess(u, item)) return false;
            return true;
        }
        if (hasEnrollment(u, c)) return true;
        return membershipCovers(u, c);
    }

    public boolean hasEnrollment(User u, Course c) {
        return enrollments.findByUser_IdAndCourse_Id(u.id, c.id)
                .filter(e -> e.active && (e.expiresAt == null || e.expiresAt.isAfter(Instant.now())))
                .isPresent();
    }

    /** Active membership whose category/level covers this course (cancelled plans stay valid until expiry + grace). */
    public Subscription coveringSubscription(User u, Course c) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(cfg.subscriptionGraceDays));
        for (Subscription s : subscriptions.findByUser_IdAndExpiresAtAfter(u.id, cutoff)) {
            if (!"ACTIVE".equals(s.status) && !"CANCELLED".equals(s.status)) continue;
            MembershipPlan p = s.plan;
            boolean cat = p.category == null || p.category.isBlank() || p.category.equalsIgnoreCase(c.category);
            boolean lvl = p.level == null || p.level.isBlank() || p.level.equalsIgnoreCase(c.level);
            if (cat && lvl && c.kind == Course.Kind.COURSE) return s;
        }
        return null;
    }

    public boolean membershipCovers(User u, Course c) { return coveringSubscription(u, c) != null; }
}
