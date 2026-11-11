package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lesson_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
public class LessonProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public User user;
    @ManyToOne(optional = false) public Lesson lesson;
    public Long courseId;
    public boolean completed;
    public int positionSeconds;
    public boolean bookmarked;
    @Column(length = 10000) public String note;
    public Instant updatedAt = Instant.now();
}
