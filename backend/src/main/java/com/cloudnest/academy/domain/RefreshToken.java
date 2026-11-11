package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public User user;
    @Column(nullable = false, unique = true) public String tokenHash;
    public Instant expiresAt;
    public boolean revoked;
}
