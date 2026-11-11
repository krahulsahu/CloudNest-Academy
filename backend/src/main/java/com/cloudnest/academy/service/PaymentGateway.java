package com.cloudnest.academy.service;

import com.cloudnest.academy.config.AppConfig;
import com.cloudnest.academy.domain.PurchaseOrder;
import com.cloudnest.academy.security.Crypto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/** Talks to Razorpay and Stripe over their REST APIs and verifies their signatures. */
@Service
public class PaymentGateway {
    private final AppConfig cfg;
    private final RestClient http = RestClient.create();

    public PaymentGateway(AppConfig cfg) { this.cfg = cfg; }

    // ---------------- Razorpay ----------------
    /** Creates a Razorpay order; amount is in rupees, Razorpay expects paise. */
    public String razorpayCreateOrder(PurchaseOrder o) {
        Map<?, ?> res = http.post().uri("https://api.razorpay.com/v1/orders")
                .headers(h -> h.setBasicAuth(cfg.razorpayKeyId, cfg.razorpayKeySecret))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", o.total * 100L, "currency", o.currency, "receipt", "order_" + o.id,
                        "notes", Map.of("orderId", String.valueOf(o.id))))
                .retrieve().body(Map.class);
        return (String) res.get("id");
    }

    /** Checkout signature = HMAC_SHA256(order_id + "|" + payment_id, key_secret). */
    public boolean razorpayVerifyPayment(String orderId, String paymentId, String signature) {
        return Crypto.safeEquals(Crypto.hmacHex(cfg.razorpayKeySecret, orderId + "|" + paymentId), signature);
    }

    public boolean razorpayVerifyWebhook(String body, String signature) {
        if (cfg.razorpayWebhookSecret == null || cfg.razorpayWebhookSecret.isBlank()) return false;
        return Crypto.safeEquals(Crypto.hmacHex(cfg.razorpayWebhookSecret, body), signature);
    }

    public void razorpayRefund(String paymentId, int amountRupees) {
        http.post().uri("https://api.razorpay.com/v1/payments/{id}/refund", paymentId)
                .headers(h -> h.setBasicAuth(cfg.razorpayKeyId, cfg.razorpayKeySecret))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountRupees * 100L))
                .retrieve().toBodilessEntity();
    }

    // ---------------- Stripe ----------------
    public record StripeSession(String id, String url) {}

    public StripeSession stripeCreateSession(PurchaseOrder o, String successUrl, String cancelUrl) {
        MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        f.add("mode", "payment");
        f.add("success_url", successUrl);
        f.add("cancel_url", cancelUrl);
        f.add("client_reference_id", String.valueOf(o.id));
        f.add("customer_email", o.user.email);
        f.add("metadata[orderId]", String.valueOf(o.id));
        f.add("line_items[0][quantity]", "1");
        f.add("line_items[0][price_data][currency]", o.currency.toLowerCase());
        f.add("line_items[0][price_data][unit_amount]", String.valueOf(o.total * 100L));
        f.add("line_items[0][price_data][product_data][name]", o.itemName());
        Map<?, ?> res = http.post().uri("https://api.stripe.com/v1/checkout/sessions")
                .headers(h -> h.setBearerAuth(cfg.stripeSecretKey))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(f)
                .retrieve().body(Map.class);
        return new StripeSession((String) res.get("id"), (String) res.get("url"));
    }

    /** Stripe-Signature: "t=timestamp,v1=hex"; signed payload = t + "." + body. Rejects events older than 5 minutes. */
    public boolean stripeVerifyWebhook(String body, String header) {
        if (header == null || cfg.stripeWebhookSecret == null || cfg.stripeWebhookSecret.isBlank()) return false;
        String t = null;
        java.util.List<String> v1 = new java.util.ArrayList<>();
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("t")) t = kv[1];
            if (kv[0].equals("v1")) v1.add(kv[1]);
        }
        if (t == null) return false;
        try {
            if (Math.abs(Instant.now().getEpochSecond() - Long.parseLong(t)) > 300) return false;
        } catch (NumberFormatException e) {
            return false;
        }
        String expected = Crypto.hmacHex(cfg.stripeWebhookSecret, t + "." + body);
        return v1.stream().anyMatch(s -> Crypto.safeEquals(expected, s));
    }

    public void stripeRefund(String paymentIntentId) {
        MultiValueMap<String, String> f = new LinkedMultiValueMap<>();
        f.add("payment_intent", paymentIntentId);
        http.post().uri("https://api.stripe.com/v1/refunds")
                .headers(h -> h.setBearerAuth(cfg.stripeSecretKey))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(f)
                .retrieve().toBodilessEntity();
    }
}
