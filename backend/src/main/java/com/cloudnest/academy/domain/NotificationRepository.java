package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUser_IdOrderByIdDesc(Long userId);
    long countByUser_IdAndSeenFalse(Long userId);
    List<Notification> findByUser_IdAndSeenFalse(Long userId);
}
