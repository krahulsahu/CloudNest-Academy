package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByCode(String code);
    long countByCourse_Id(Long courseId);
    Optional<Certificate> findByUser_IdAndCourse_Id(Long userId, Long courseId);
    List<Certificate> findByUser_IdOrderByIdDesc(Long userId);
}
