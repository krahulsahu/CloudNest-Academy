package com.cloudnest.academy.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "otp_codes")
public class OtpCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public String email;
    @Column(nullable = false) public String codeHash;
    /** LOGIN or RESET */
    public String purpose;
    public Instant expiresAt;
    public int attempts;
    public boolean used;
}
