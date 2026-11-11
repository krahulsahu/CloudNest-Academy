package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    public enum Role { STUDENT, INSTRUCTOR, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public String name;
    @Column(nullable = false, unique = true) public String email;
    public String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Role role = Role.STUDENT;
    public boolean emailVerified;
    public boolean enabled = true;
    public boolean marketingOptIn;
    public String country;
    public String headline;
    @Column(length = 4000) public String bio;
    public Instant createdAt = Instant.now();
}
