package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findTop300ByOrderByIdDesc();
    List<SupportTicket> findByUserIdOrderByIdDesc(Long userId);
}
