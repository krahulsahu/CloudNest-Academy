package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "coupons")
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true) public String code;
    public int percentOff;
    public boolean active = true;
    public Instant expiresAt;
    public Integer maxUses;
    public int uses;
    /** null = valid for every course, bundle and plan */
    public String courseSlug;
}
