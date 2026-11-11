package com.cloudnest.academy.web;

import com.cloudnest.academy.security.CurrentUser;
import com.cloudnest.academy.service.AdminService;
import com.cloudnest.academy.service.CheckoutService;
import com.cloudnest.academy.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    private final CheckoutService checkout;

    public AdminController(AdminService admin, CheckoutService checkout) {
        this.admin = admin;
        this.checkout = checkout;
    }

    @GetMapping("/stats")
    public AdminStats stats(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return admin.stats(from, to);
    }

    @GetMapping("/users")
    public List<AdminUser> users(@RequestParam(required = false) String q) { return admin.users(q); }

    @PutMapping("/users/{id}/role")
    public void role(@PathVariable Long id, @Valid @RequestBody RoleReq req) { admin.setRole(CurrentUser.id(), id, req.role()); }

    @PutMapping("/users/{id}/enabled")
    public void enabled(@PathVariable Long id, @RequestBody EnabledReq req) { admin.setEnabled(CurrentUser.id(), id, req.enabled()); }

    @PostMapping("/enrollments")
    public void enroll(@Valid @RequestBody ManualEnrollReq req) { admin.manualEnroll(CurrentUser.id(), req); }

    @GetMapping("/courses")
    public List<CourseCard> courses() { return admin.allCourses(); }

    @PostMapping("/courses/{id}/{action}")
    public CourseCard courseAction(@PathVariable Long id, @PathVariable String action) { return admin.setStatus(CurrentUser.id(), id, action); }

    @GetMapping("/orders")
    public List<OrderDto> orders() { return admin.orders(); }

    @PostMapping("/orders/{id}/refund")
    public OrderDto refund(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean force) { return checkout.refund(id, force); }

    @GetMapping("/coupons")
    public List<CouponDto> coupons() { return admin.coupons(); }

    @PostMapping("/coupons")
    public void saveCoupon(@Valid @RequestBody CouponDto d) { admin.saveCoupon(d); }

    @DeleteMapping("/coupons/{id}")
    public void deleteCoupon(@PathVariable Long id) { admin.deleteCoupon(id); }

    @PostMapping("/categories")
    public void addCategory(@RequestBody Map<String, String> body) { admin.addCategory(body.get("name")); }

    @DeleteMapping("/categories/{id}")
    public void deleteCategory(@PathVariable Long id) { admin.deleteCategory(id); }

    @GetMapping("/plans")
    public List<PlanDto> plans() { return admin.plans(); }

    @PostMapping("/plans")
    public void savePlan(@RequestBody PlanDto d) { admin.savePlan(d); }

    @GetMapping("/tickets")
    public List<TicketDto> tickets() { return admin.tickets(); }

    @PutMapping("/tickets/{id}")
    public void updateTicket(@PathVariable Long id, @RequestBody TicketUpdate u) { admin.updateTicket(id, u); }

    @GetMapping("/reviews")
    public List<ReviewDto> reviews() { return admin.reviews(); }

    @PutMapping("/reviews/{id}/visible")
    public void reviewVisible(@PathVariable Long id, @RequestBody EnabledReq req) { admin.setReviewVisible(id, req.enabled()); }
}
