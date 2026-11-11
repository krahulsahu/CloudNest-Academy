package com.cloudnest.academy.service;

import com.cloudnest.academy.config.ApiException;
import com.cloudnest.academy.config.AppConfig;
import com.cloudnest.academy.domain.*;
import com.cloudnest.academy.web.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Objects;

/**
 * Orders, pricing (bundle upgrade credit + coupons + tax), payment verification and fulfilment.
 * Enrollment is created ONLY in fulfil(), which is only reached after server-side verification.
 */
@Service
@Transactional
public class CheckoutService {
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    private final CourseRepository courses;
    private final MembershipPlanRepository plans;
    private final CouponRepository coupons;
    private final PurchaseOrderRepository orders;
    private final EnrollmentRepository enrollments;
    private final SubscriptionRepository subscriptions;
    private final UserRepository users;
    private final WebhookEventRepository webhookEvents;
    private final AccessService access;
    private final PaymentGateway gateway;
    private final NotificationService notifications;
    private final AppConfig cfg;

    public CheckoutService(CourseRepository courses, MembershipPlanRepository plans, CouponRepository coupons,
                           PurchaseOrderRepository orders, EnrollmentRepository enrollments,
                           SubscriptionRepository subscriptions, UserRepository users,
                           WebhookEventRepository webhookEvents, AccessService access, PaymentGateway gateway,
                           NotificationService notifications, AppConfig cfg) {
        this.courses = courses;
        this.plans = plans;
        this.coupons = coupons;
        this.orders = orders;
        this.enrollments = enrollments;
        this.subscriptions = subscriptions;
        this.users = users;
        this.webhookEvents = webhookEvents;
        this.access = access;
        this.gateway = gateway;
        this.notifications = notifications;
        this.cfg = cfg;
    }

    private record Priced(Course course, MembershipPlan plan, Quote quote, Coupon coupon) {}

    private Priced price(User u, QuoteReq req) {
        String currency = "USD".equalsIgnoreCase(req.currency()) ? "USD" : "INR";
        Course course = null;
        MembershipPlan plan = null;
        int list, credit = 0;
        String itemName;
        if (req.courseSlug() != null && !req.courseSlug().isBlank()) {
            course = courses.findBySlug(req.courseSlug()).filter(c -> c.status == Course.Status.PUBLISHED)
                    .orElseThrow(() -> ApiException.notFound("Course not found"));
            if (access.hasAccess(u, course)) throw ApiException.conflict("You already have access to " + course.title);
            list = course.price(currency);
            itemName = course.title;
            if (course.kind == Course.Kind.BUNDLE) {
                // Upgrade credit: courses of this bundle the student already bought individually.
                for (Course item : course.bundleItems) {
                    var e = enrollments.findByUser_IdAndCourse_Id(u.id, item.id);
                    if (e.isPresent() && e.get().active && e.get().source == Enrollment.Source.PURCHASE) credit += item.price(currency);
                }
                credit = Math.min(credit, list);
            }
        } else if (req.planCode() != null && !req.planCode().isBlank()) {
            plan = plans.findByCode(req.planCode()).filter(p -> p.active).orElseThrow(() -> ApiException.notFound("Plan not found"));
            list = "USD".equals(currency) ? plan.priceUsd : plan.priceInr;
            itemName = plan.name + " membership";
        } else {
            throw ApiException.badRequest("Choose a course or a plan");
        }
        int discount = 0;
        String couponMsg = null;
        Coupon coupon = null;
        if (req.coupon() != null && !req.coupon().isBlank()) {
            coupon = coupons.findByCodeIgnoreCase(req.coupon().trim()).orElse(null);
            String why = couponProblem(coupon, course);
            if (why != null) {
                couponMsg = why;
                coupon = null;
            } else {
                discount = (list - credit) * coupon.percentOff / 100;
                couponMsg = coupon.code + " applied: " + coupon.percentOff + "% off";
            }
        }
        int total = Math.max(0, list - credit - discount);
        return new Priced(course, plan, new Quote(currency, list, credit, discount, total, itemName, couponMsg), coupon);
    }

    private static String couponProblem(Coupon c, Course course) {
        if (c == null || !c.active) return "This coupon code is not valid";
        if (c.expiresAt != null && c.expiresAt.isBefore(Instant.now())) return "This coupon has expired";
        if (c.maxUses != null && c.uses >= c.maxUses) return "This coupon has reached its usage limit";
        if (c.courseSlug != null && !c.courseSlug.isBlank() && (course == null || !c.courseSlug.equals(course.slug)))
            return "This coupon is not valid for this item";
        return null;
    }

    public Quote quote(Long userId, QuoteReq req) {
        return price(users.findById(userId).orElseThrow(), req).quote();
    }

    public CheckoutResponse checkout(Long userId, QuoteReq req) {
        User u = users.findById(userId).orElseThrow();
        Priced p = price(u, req);
        Quote q = p.quote();
        PurchaseOrder o = new PurchaseOrder();
        o.user = u;
        o.course = p.course();
        o.plan = p.plan();
        o.currency = q.currency();
        o.listPrice = q.listPrice();
        o.credit = q.credit();
        o.discount = q.discount();
        o.total = q.total();
        o.couponCode = p.coupon() == null ? null : p.coupon().code;
        o.billingCountry = u.country;
        o.taxAmount = "INR".equals(o.currency) ? Math.round(o.total * cfg.gstPercent / (100f + cfg.gstPercent)) : 0;
        orders.save(o);
        String redirect = redirectFor(o);

        if (o.total == 0) {
            o.provider = "FREE";
            fulfil(o, null);
            return new CheckoutResponse(o.id, "FREE", 0, o.currency, true, null, null, null, u.name, u.email, q.itemName(), redirect);
        }
        if ("INR".equals(o.currency) && cfg.razorpayEnabled()) {
            o.provider = "RAZORPAY";
            o.providerOrderId = gateway.razorpayCreateOrder(o);
            return new CheckoutResponse(o.id, "RAZORPAY", o.total, o.currency, false, o.providerOrderId, cfg.razorpayKeyId,
                    null, u.name, u.email, q.itemName(), redirect);
        }
        if ("USD".equals(o.currency) && cfg.stripeEnabled()) {
            o.provider = "STRIPE";
            var s = gateway.stripeCreateSession(o, cfg.frontendUrl + "/checkout/success?order=" + o.id,
                    cfg.frontendUrl + (o.course != null ? "/p/" + o.course.slug : "/subscribe"));
            o.providerOrderId = s.id();
            return new CheckoutResponse(o.id, "STRIPE", o.total, o.currency, false, null, null, s.url(), u.name, u.email, q.itemName(), redirect);
        }
        if (cfg.mockPayments) {
            o.provider = "MOCK";
            return new CheckoutResponse(o.id, "MOCK", o.total, o.currency, false, null, null, null, u.name, u.email, q.itemName(), redirect);
        }
        throw ApiException.badRequest("Online payment is not available for " + o.currency + " yet. Please contact support.");
    }

    private static String redirectFor(PurchaseOrder o) {
        if (o.course != null && o.course.kind == Course.Kind.COURSE) return "/learn/" + o.course.slug;
        return "/dashboard";
    }

    public OrderDto verifyRazorpay(Long userId, RazorpayVerifyReq req) {
        PurchaseOrder o = orders.findById(req.orderId()).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!o.user.id.equals(userId) || !"RAZORPAY".equals(o.provider) || !Objects.equals(o.providerOrderId, req.razorpayOrderId()))
            throw ApiException.badRequest("Order mismatch");
        if (!gateway.razorpayVerifyPayment(req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature())) {
            audit.warn("payment.signature.invalid order={}", o.id);
            throw ApiException.badRequest("Payment verification failed");
        }
        fulfil(o, req.razorpayPaymentId());
        return order(o);
    }

    public OrderDto confirmMock(Long userId, Long orderId) {
        if (!cfg.mockPayments) throw ApiException.forbidden("Mock payments are disabled");
        PurchaseOrder o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!o.user.id.equals(userId) || !"MOCK".equals(o.provider)) throw ApiException.badRequest("Order mismatch");
        fulfil(o, "mock_" + o.id);
        return order(o);
    }

    /** Returns true if this event was already processed (duplicate webhook protection). */
    public boolean alreadyProcessed(String provider, String eventId) {
        if (eventId == null || eventId.isBlank()) return false;
        if (webhookEvents.existsByEventId(eventId)) return true;
        WebhookEvent e = new WebhookEvent();
        e.eventId = eventId;
        e.provider = provider;
        webhookEvents.save(e);
        return false;
    }

    public void fulfilByProviderOrder(String providerOrderId, String paymentId) {
        orders.findByProviderOrderId(providerOrderId).ifPresentOrElse(
                o -> fulfil(o, paymentId),
                () -> audit.warn("webhook.order.unknown providerOrderId={}", providerOrderId));
    }

    public void failByProviderOrder(String providerOrderId) {
        orders.findByProviderOrderId(providerOrderId).ifPresent(o -> {
            if (o.status == PurchaseOrder.Status.CREATED) o.status = PurchaseOrder.Status.FAILED;
        });
    }

    /** Idempotent: grants access exactly once per order. */
    void fulfil(PurchaseOrder o, String paymentId) {
        if (o.status == PurchaseOrder.Status.PAID || o.status == PurchaseOrder.Status.REFUNDED) return;
        o.status = PurchaseOrder.Status.PAID;
        o.paidAt = Instant.now();
        o.providerPaymentId = paymentId;
        o.invoiceNumber = "INV-" + Year.now().getValue() + "-" + String.format("%06d", o.id);
        if (o.couponCode != null) coupons.findByCodeIgnoreCase(o.couponCode).ifPresent(c -> c.uses++);
        User u = o.user;
        String link;
        if (o.course != null) {
            Enrollment.Source src = o.total == 0 && o.listPrice == 0 ? Enrollment.Source.FREE : Enrollment.Source.PURCHASE;
            grant(u, o.course, src, o.id);
            if (o.course.kind == Course.Kind.BUNDLE) for (Course item : o.course.bundleItems) grant(u, item, Enrollment.Source.BUNDLE, o.id);
            link = o.course.kind == Course.Kind.BUNDLE ? "/dashboard" : "/learn/" + o.course.slug;
        } else {
            Subscription s = subscriptions.findByUser_IdAndExpiresAtAfter(u.id, Instant.now()).stream()
                    .filter(x -> x.plan.id.equals(o.plan.id) && !"REFUNDED".equals(x.status)).findFirst().orElse(null);
            if (s == null) {
                s = new Subscription();
                s.user = u;
                s.plan = o.plan;
                s.startsAt = Instant.now();
                s.expiresAt = Instant.now();
            }
            s.expiresAt = s.expiresAt.plus(Duration.ofDays(o.plan.periodDays)); // renewal extends the period
            s.status = "ACTIVE";
            s.autoRenew = true;
            s.reminded = false;
            s.orderId = o.id;
            subscriptions.save(s);
            link = "/dashboard";
        }
        audit.info("order.paid id={} user={} total={} {} provider={}", o.id, u.id, o.total, o.currency, o.provider);
        notifications.notify(u, "You're enrolled: " + o.itemName(),
                "Thank you for your purchase! Invoice " + o.invoiceNumber + " for " + o.currency + " " + o.total
                        + ". You can start learning right away from My Courses.", link, true);
    }

    private void grant(User u, Course c, Enrollment.Source src, Long orderId) {
        Enrollment e = enrollments.findByUser_IdAndCourse_Id(u.id, c.id).orElseGet(Enrollment::new);
        e.user = u;
        e.course = c;
        if (e.id == null || !e.active || e.source == null) e.source = src;
        e.active = true;
        e.expiresAt = null;
        e.orderId = orderId;
        enrollments.save(e);
    }

    /** Admin refund: refunds through the provider (when configured), revokes access, records the refund. */
    public OrderDto refund(Long orderId, boolean ignorePolicy) {
        PurchaseOrder o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (o.status != PurchaseOrder.Status.PAID) throw ApiException.badRequest("Only paid orders can be refunded");
        if (!ignorePolicy && o.paidAt.isBefore(Instant.now().minus(Duration.ofDays(cfg.refundDays))))
            throw ApiException.badRequest("Outside the " + cfg.refundDays + "-day refund window");
        if (o.total > 0 && o.providerPaymentId != null) {
            if ("RAZORPAY".equals(o.provider)) gateway.razorpayRefund(o.providerPaymentId, o.total);
            if ("STRIPE".equals(o.provider)) gateway.stripeRefund(o.providerPaymentId);
        }
        o.status = PurchaseOrder.Status.REFUNDED;
        o.refundedAt = Instant.now();
        for (Enrollment e : enrollments.findByOrderId(o.id)) e.active = false;
        for (Subscription s : subscriptions.findByOrderId(o.id)) s.status = "REFUNDED";
        audit.info("order.refunded id={}", o.id);
        notifications.notify(o.user, "Refund processed: " + o.itemName(),
                "Your refund of " + o.currency + " " + o.total + " has been processed. It may take 5-7 working days to appear.", "/dashboard?tab=orders", true);
        return order(o);
    }

    public static OrderDto order(PurchaseOrder o) {
        return new OrderDto(o.id, o.invoiceNumber, o.itemName(), o.currency, o.listPrice, o.credit, o.discount,
                o.taxAmount, o.total, o.status.name(), o.provider, o.createdAt, o.paidAt, o.user.email, o.couponCode);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> myOrders(Long userId) {
        return orders.findByUser_IdOrderByIdDesc(userId).stream().map(CheckoutService::order).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto myOrder(Long userId, Long orderId) {
        PurchaseOrder o = orders.findById(orderId).filter(x -> x.user.id.equals(userId))
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        return order(o);
    }
}
