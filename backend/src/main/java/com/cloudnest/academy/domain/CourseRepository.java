package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Course> findByStatus(Course.Status status);
    List<Course> findByInstructor_IdOrderByIdDesc(Long instructorId);
    List<Course> findAllByOrderByIdDesc();
    List<Course> findByStatusAndScheduledAtBefore(Course.Status status, Instant time);

    @Query("select b from Course b join b.bundleItems i where i.id = :courseId and b.status = :status")
    List<Course> findBundlesContaining(@Param("courseId") Long courseId, @Param("status") Course.Status status);
}
