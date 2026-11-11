package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByUser_IdAndCourse_Id(Long userId, Long courseId);
    List<Enrollment> findByUser_IdAndActiveTrue(Long userId);
    List<Enrollment> findByCourse_IdAndActiveTrue(Long courseId);
    long countByCourse_IdAndActiveTrue(Long courseId);
    List<Enrollment> findByOrderId(Long orderId);

    @Query("select e.course.id, e.course.title, count(e) from Enrollment e where e.active = true group by e.course.id, e.course.title order by count(e) desc")
    List<Object[]> topCourses();
}
