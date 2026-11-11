package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "certificates")
public class Certificate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true) public String code;
    @ManyToOne(optional = false) public User user;
    @ManyToOne(optional = false) public Course course;
    public Instant issuedAt = Instant.now();
}
