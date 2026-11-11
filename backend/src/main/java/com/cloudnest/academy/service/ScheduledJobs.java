package com.cloudnest.academy.service;

import com.cloudnest.academy.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Publishes scheduled courses, expires memberships and sends renewal reminders. */
@Component
public class ScheduledJobs {
    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);
    private final CourseRepository courses;
    private final SubscriptionRepository subscriptions;
    private final EnrollmentRepository enrollments;
    private final NotificationService notifications;
    private final com.cloudnest.academy.config.AppConfig cfg;

    public ScheduledJobs(CourseRepository courses, SubscriptionRepository subscriptions, EnrollmentRepository enrollments,
                         NotificationService notifications, com.cloudnest.academy.config.AppConfig cfg) {
        this.courses = courses;
        this.subscriptions = subscriptions;
        this.enrollments = enrollments;
        this.notifications = notifications;
        this.cfg = cfg;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 20_000)
    @Transactional
    public void publishScheduled() {
        for (Course c : courses.findByStatusAndScheduledAtBefore(Course.Status.SCHEDULED, Instant.now())) {
            c.status = Course.Status.PUBLISHED;
            c.publishedAt = Instant.now();
            log.info("Published scheduled course {}", c.slug);
        }
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 30_000)
    @Transactional
    public void subscriptions() {
        Instant now = Instant.now();
        // Expire after the grace period
        for (Subscription s : subscriptions.findByStatusInAndExpiresAtBefore(List.of("ACTIVE", "CANCELLED"), now.minus(Duration.ofDays(cfg.subscriptionGraceDays)))) {
            s.status = "EXPIRED";
            notifications.notify(s.user, "Your " + s.plan.name + " membership has ended",
                    "Renew any time to continue learning where you left off. Your progress is saved.", "/subscribe", true);
        }
        // Remind 3 days before expiry
        for (Subscription s : subscriptions.findByStatusAndReminded("ACTIVE", false)) {
            if (s.expiresAt.isBefore(now.plus(Duration.ofDays(3)))) {
                s.reminded = true;
                notifications.notify(s.user, "Your " + s.plan.name + " membership renews soon",
                        "Your membership expires on " + s.expiresAt.toString().substring(0, 10) + ". Renew from the Subscribe page to keep access.", "/subscribe", true);
            }
        }
        // Time-limited enrollments
        for (Enrollment e : enrollments.findAll()) {
            if (e.active && e.expiresAt != null && e.expiresAt.isBefore(now)) e.active = false;
        }
    }
}
