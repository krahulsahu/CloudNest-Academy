package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public User user;
    @ManyToOne(optional = false) public MembershipPlan plan;
    public Instant startsAt;
    public Instant expiresAt;
    public boolean autoRenew = true;
    /** ACTIVE, CANCELLED (valid until expiry), EXPIRED, REFUNDED */
    public String status = "ACTIVE";
    public boolean reminded;
    public Long orderId;
}
