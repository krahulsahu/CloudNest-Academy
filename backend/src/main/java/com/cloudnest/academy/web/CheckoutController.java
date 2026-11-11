package com.cloudnest.academy.web;

import com.cloudnest.academy.security.CurrentUser;
import com.cloudnest.academy.service.CheckoutService;
import com.cloudnest.academy.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkout;

    public CheckoutController(CheckoutService checkout) { this.checkout = checkout; }

    @PostMapping("/quote")
    public Quote quote(@RequestBody QuoteReq req) { return checkout.quote(CurrentUser.id(), req); }

    @PostMapping
    public CheckoutResponse create(@RequestBody QuoteReq req) { return checkout.checkout(CurrentUser.id(), req); }

    @PostMapping("/razorpay/verify")
    public OrderDto verify(@Valid @RequestBody RazorpayVerifyReq req) { return checkout.verifyRazorpay(CurrentUser.id(), req); }

    /** Development only (app.payments.mock-enabled=true): simulates a successful payment. */
    @PostMapping("/mock/{orderId}/confirm")
    public OrderDto mock(@PathVariable Long orderId) { return checkout.confirmMock(CurrentUser.id(), orderId); }
}
