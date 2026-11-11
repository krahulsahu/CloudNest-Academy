package com.cloudnest.academy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Typed access to the "app.*" settings in application.yml. */
@Component
public class AppConfig {
    @Value("${app.name}") public String name;
    @Value("${app.frontend-url}") public String frontendUrl;
    @Value("${app.mail-from}") public String mailFrom;
    @Value("${app.jwt-secret}") public String jwtSecret;
    @Value("${app.access-token-minutes}") public long accessTokenMinutes;
    @Value("${app.refresh-token-days}") public long refreshTokenDays;
    @Value("${app.dev-show-otp}") public boolean devShowOtp;
    @Value("${app.seed-demo-data:true}") public boolean seedDemoData;
    @Value("${app.upload-dir}") public String uploadDir;
    @Value("${app.media-secret}") public String mediaSecret;
    @Value("${app.media-url-minutes}") public long mediaUrlMinutes;
    @Value("${app.refund-days}") public int refundDays;
    @Value("${app.subscription-grace-days}") public int subscriptionGraceDays;
    @Value("${app.gst-percent}") public int gstPercent;
    @Value("${app.admin-email}") public String adminEmail;
    @Value("${app.admin-password}") public String adminPassword;
    @Value("${app.payments.mock-enabled}") public boolean mockPayments;
    @Value("${app.payments.razorpay-key-id}") public String razorpayKeyId;
    @Value("${app.payments.razorpay-key-secret}") public String razorpayKeySecret;
    @Value("${app.payments.razorpay-webhook-secret}") public String razorpayWebhookSecret;
    @Value("${app.payments.stripe-secret-key}") public String stripeSecretKey;
    @Value("${app.payments.stripe-webhook-secret}") public String stripeWebhookSecret;

    public boolean razorpayEnabled() { return notBlank(razorpayKeyId) && notBlank(razorpayKeySecret); }
    public boolean stripeEnabled() { return notBlank(stripeSecretKey); }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
