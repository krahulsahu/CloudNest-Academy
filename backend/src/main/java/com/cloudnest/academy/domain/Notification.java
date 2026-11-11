package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public User user;
    public String title;
    @Column(length = 3000) public String body;
    public String link;
    public boolean seen;
    public Instant createdAt = Instant.now();
}
