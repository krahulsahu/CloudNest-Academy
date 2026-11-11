package com.cloudnest.academy.domain;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    long countByRole(User.Role role);
    List<User> findTop200ByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByIdDesc(String name, String email);
    List<User> findByRole(User.Role role);
}
