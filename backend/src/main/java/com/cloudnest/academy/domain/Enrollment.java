package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
public class Enrollment {
    public enum Source { PURCHASE, BUNDLE, FREE, ADMIN, ORGANIZATION }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public User user;
    @ManyToOne(optional = false) public Course course;
    @Enumerated(EnumType.STRING) public Source source;
    public Long orderId;
    public boolean active = true;
    public Instant expiresAt;
    public Instant createdAt = Instant.now();
}
