package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class PurchaseOrder {
    public enum Status { CREATED, PAID, FAILED, REFUNDED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public User user;
    @ManyToOne public Course course;
    @ManyToOne public MembershipPlan plan;
    public String currency;
    public int listPrice;
    public int credit;
    public int discount;
    public int total;
    public int taxAmount;
    public String couponCode;
    public String provider;
    @Column(unique = true) public String providerOrderId;
    public String providerPaymentId;
    @Enumerated(EnumType.STRING) public Status status = Status.CREATED;
    public String invoiceNumber;
    public String billingCountry;
    public Instant createdAt = Instant.now();
    public Instant paidAt;
    public Instant refundedAt;

    public String itemName() {
        return course != null ? course.title : plan != null ? plan.name + " membership" : "Order";
    }
}
