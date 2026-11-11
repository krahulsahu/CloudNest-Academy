package com.cloudnest.academy.web;

import com.cloudnest.academy.security.CurrentUser;
import com.cloudnest.academy.service.LearningService;
import com.cloudnest.academy.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learn")
public class LearningController {
    private final LearningService learning;

    public LearningController(LearningService learning) { this.learning = learning; }

    @GetMapping("/{slug}")
    public LearnCourse course(@PathVariable String slug) { return learning.learnCourse(slug, CurrentUser.idOrNull()); }

    @GetMapping("/{slug}/lessons/{lessonId}")
    public LessonDto lesson(@PathVariable String slug, @PathVariable Long lessonId) {
        return learning.lesson(slug, lessonId, CurrentUser.idOrNull());
    }

    @PutMapping("/lessons/{lessonId}/progress")
    public void progress(@PathVariable Long lessonId, @RequestBody ProgressReq req) {
        learning.updateProgress(lessonId, CurrentUser.id(), req);
    }

    @PutMapping("/lessons/{lessonId}/note")
    public void note(@PathVariable Long lessonId, @Valid @RequestBody NoteReq req) {
        learning.saveNote(lessonId, CurrentUser.id(), req.text());
    }

    @PostMapping("/lessons/{lessonId}/quiz")
    public QuizResult quiz(@PathVariable Long lessonId, @RequestBody QuizSubmit req) {
        return learning.submitQuiz(lessonId, CurrentUser.id(), req);
    }

    @PostMapping("/{slug}/certificate")
    public CertificateDto certificate(@PathVariable String slug) { return learning.claimCertificate(slug, CurrentUser.id()); }
}
