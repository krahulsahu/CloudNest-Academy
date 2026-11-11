package com.cloudnest.academy.web;

import com.cloudnest.academy.service.CheckoutService;
import com.cloudnest.academy.service.PaymentGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Payment provider webhooks. The raw body is verified with the provider's signing secret before anything is trusted.
 * Razorpay: subscribe to payment.captured, order.paid, payment.failed.  Stripe: checkout.session.completed.
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final PaymentGateway gateway;
    private final CheckoutService checkout;
    private final ObjectMapper mapper;

    public WebhookController(PaymentGateway gateway, CheckoutService checkout, ObjectMapper mapper) {
        this.gateway = gateway;
        this.checkout = checkout;
        this.mapper = mapper;
    }

    @PostMapping("/razorpay")
    @Transactional
    public ResponseEntity<String> razorpay(@RequestBody String body,
                                           @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                           @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) throws Exception {
        if (signature == null || !gateway.razorpayVerifyWebhook(body, signature)) {
            log.warn("Rejected Razorpay webhook with invalid signature");
            return ResponseEntity.status(400).body("invalid signature");
        }
        if (checkout.alreadyProcessed("RAZORPAY", eventId)) return ResponseEntity.ok("duplicate");
        JsonNode root = mapper.readTree(body);
        String event = root.path("event").asText();
        JsonNode payment = root.path("payload").path("payment").path("entity");
        String orderId = payment.path("order_id").asText(null);
        switch (event) {
            case "payment.captured", "order.paid" -> {
                if (orderId != null) checkout.fulfilByProviderOrder(orderId, payment.path("id").asText(null));
            }
            case "payment.failed" -> { if (orderId != null) checkout.failByProviderOrder(orderId); }
            default -> log.info("Ignoring Razorpay event {}", event);
        }
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/stripe")
    @Transactional
    public ResponseEntity<String> stripe(@RequestBody String body,
                                         @RequestHeader(value = "Stripe-Signature", required = false) String signature) throws Exception {
        if (!gateway.stripeVerifyWebhook(body, signature)) {
            log.warn("Rejected Stripe webhook with invalid signature");
            return ResponseEntity.status(400).body("invalid signature");
        }
        JsonNode root = mapper.readTree(body);
        if (checkout.alreadyProcessed("STRIPE", root.path("id").asText(null))) return ResponseEntity.ok("duplicate");
        String type = root.path("type").asText();
        JsonNode obj = root.path("data").path("object");
        if ("checkout.session.completed".equals(type) && "paid".equals(obj.path("payment_status").asText())) {
            checkout.fulfilByProviderOrder(obj.path("id").asText(), obj.path("payment_intent").asText(null));
        } else if ("checkout.session.expired".equals(type)) {
            checkout.failByProviderOrder(obj.path("id").asText());
        }
        return ResponseEntity.ok("ok");
    }
}
