package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "courses")
public class Course {
    public enum Kind { COURSE, BUNDLE }
    public enum Status { DRAFT, PENDING_REVIEW, SCHEDULED, PUBLISHED, UNPUBLISHED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true) public String slug;
    @Column(nullable = false) public String title;
    @Column(length = 500) public String subtitle;
    @Column(length = 20000) public String description;
    public String category;
    public String level = "Beginner";
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Kind kind = Kind.COURSE;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Status status = Status.DRAFT;
    public int priceInr;
    public int priceUsd;
    public Integer compareAtInr;
    public boolean featured;
    public boolean newBadge;
    /** Thumbnail badges, format: "line1|line2|LEVEL|color;..." */
    @Column(length = 1000) public String badges;
    public String theme;
    public String thumbnailUrl;
    public String previewVideoUrl;
    @Column(length = 4000) public String objectives;
    @Column(length = 4000) public String prerequisites;
    @Column(length = 2000) public String audience;
    public String seoTitle;
    @Column(length = 500) public String seoDescription;
    public int lessonCount;
    public int totalSeconds;
    public int ratingSum;
    public int ratingCount;
    @ManyToOne public User instructor;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @OrderBy("position")
    public List<CourseSection> sections = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "bundle_items",
            joinColumns = @JoinColumn(name = "bundle_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    public Set<Course> bundleItems = new LinkedHashSet<>();

    public Instant scheduledAt;
    public Instant publishedAt;
    public Instant createdAt = Instant.now();

    public int price(String currency) { return "USD".equals(currency) ? priceUsd : priceInr; }

    public void recount() {
        int n = 0, secs = 0;
        for (CourseSection s : sections) for (Lesson l : s.lessons) { n++; secs += l.durationSeconds; }
        lessonCount = n;
        totalSeconds = secs;
    }

    public List<Lesson> allLessons() {
        List<Lesson> out = new ArrayList<>();
        for (CourseSection s : sections) out.addAll(s.lessons);
        return out;
    }
}
