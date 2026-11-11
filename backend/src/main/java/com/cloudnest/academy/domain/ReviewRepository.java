package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findTop50ByCourse_IdAndVisibleTrueOrderByIdDesc(Long courseId);
    Optional<Review> findByUser_IdAndCourse_Id(Long userId, Long courseId);
    List<Review> findTop300ByOrderByIdDesc();
}
