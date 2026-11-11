package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public User user;
    @ManyToOne(optional = false) public Lesson lesson;
    public int score;
    public int total;
    public boolean passed;
    public Instant createdAt = Instant.now();
}
