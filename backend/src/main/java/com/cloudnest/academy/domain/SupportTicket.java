package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    /** CONTACT, TEAM, SUPPORT, REFUND */
    public String type = "CONTACT";
    public Long userId;
    public String name;
    public String email;
    public String subject;
    @Column(length = 5000) public String message;
    @Column(length = 5000) public String reply;
    /** OPEN or CLOSED */
    public String status = "OPEN";
    public Instant createdAt = Instant.now();
}
