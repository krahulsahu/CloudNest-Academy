package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUser_IdAndLesson_IdOrderByIdDesc(Long userId, Long lessonId);
    List<QuizAttempt> findByUser_IdOrderByIdDesc(Long userId);
    boolean existsByUser_IdAndLesson_IdAndPassedTrue(Long userId, Long lessonId);
    void deleteByLessonIn(Collection<Lesson> lessons);
}
