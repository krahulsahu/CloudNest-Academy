package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUser_IdOrderByIdDesc(Long userId);
    List<Subscription> findByUser_IdAndExpiresAtAfter(Long userId, Instant time);
    List<Subscription> findByStatusInAndExpiresAtBefore(Collection<String> statuses, Instant time);
    List<Subscription> findByStatusAndReminded(String status, boolean reminded);
    List<Subscription> findByOrderId(Long orderId);
    long countByStatusInAndExpiresAtAfter(Collection<String> statuses, Instant time);
}
