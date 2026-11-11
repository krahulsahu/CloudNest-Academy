package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByUser_IdAndLesson_Id(Long userId, Long lessonId);
    List<LessonProgress> findByUser_IdAndCourseId(Long userId, Long courseId);
    List<LessonProgress> findByUser_IdAndBookmarkedTrue(Long userId);
    void deleteByLessonIn(Collection<Lesson> lessons);

    @Query("select count(distinct p.user.id) from LessonProgress p where p.updatedAt > :since")
    long countActiveLearners(@Param("since") Instant since);
}
