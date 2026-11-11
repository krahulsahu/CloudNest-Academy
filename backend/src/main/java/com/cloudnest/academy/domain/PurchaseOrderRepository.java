package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByUser_IdOrderByIdDesc(Long userId);
    Optional<PurchaseOrder> findByProviderOrderId(String providerOrderId);
    List<PurchaseOrder> findByStatusAndPaidAtBetween(PurchaseOrder.Status status, Instant from, Instant to);
    List<PurchaseOrder> findByCourse_IdAndStatus(Long courseId, PurchaseOrder.Status status);
    List<PurchaseOrder> findTop300ByOrderByIdDesc();
}
