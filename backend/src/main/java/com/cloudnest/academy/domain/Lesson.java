package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lessons")
public class Lesson {
    public enum Type { VIDEO, TEXT, QUIZ }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public CourseSection section;
    @Column(nullable = false) public String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Type type = Type.VIDEO;
    public int durationSeconds;
    /** External URL (mp4 / YouTube / Vimeo) or "upload:file.mp4" for a private uploaded file. */
    @Column(length = 1000) public String videoUrl;
    @Column(length = 1000) public String captionsUrl;
    /** Lesson text: description, step-by-step instructions, code (``` blocks), troubleshooting. */
    @Column(length = 50000) public String content;
    /** One resource per line: "Label|url" (url may be "upload:file.zip"). */
    @Column(length = 4000) public String resources;
    public boolean preview;
    public int position;
    public Integer passPercent = 70;
    public Integer maxAttempts;
    public Integer timeLimitMinutes;
    public boolean randomize;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    public List<QuizQuestion> questions = new ArrayList<>();
}
