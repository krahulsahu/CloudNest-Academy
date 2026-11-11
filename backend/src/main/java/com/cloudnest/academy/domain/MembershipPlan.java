package com.cloudnest.academy.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true) public String code;
    @Column(nullable = false) public String name;
    @Column(length = 2000) public String description;
    public int priceInr;
    public int priceUsd;
    public int periodDays = 30;
    /** null = every category */
    public String category;
    /** null = every level */
    public String level;
    public boolean active = true;
}
