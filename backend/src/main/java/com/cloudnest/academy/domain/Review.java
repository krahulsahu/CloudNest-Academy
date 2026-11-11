package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public User user;
    @ManyToOne(optional = false) public Course course;
    public int rating;
    @Column(length = 3000) public String comment;
    public boolean visible = true;
    public Instant createdAt = Instant.now();
}
