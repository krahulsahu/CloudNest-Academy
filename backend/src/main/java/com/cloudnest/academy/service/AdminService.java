package com.cloudnest.academy.service;

import com.cloudnest.academy.config.ApiException;
import com.cloudnest.academy.domain.*;
import com.cloudnest.academy.web.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class AdminService {
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final PurchaseOrderRepository orders;
    private final CertificateRepository certificates;
    private final SubscriptionRepository subscriptions;
    private final LessonProgressRepository progress;
    private final CouponRepository coupons;
    private final CategoryRepository categories;
    private final MembershipPlanRepository plans;
    private final SupportTicketRepository tickets;
    private final ReviewRepository reviews;
    private final NotificationService notifications;
    private final MailService mail;

    public AdminService(UserRepository users, CourseRepository courses, EnrollmentRepository enrollments,
                        PurchaseOrderRepository orders, CertificateRepository certificates,
                        SubscriptionRepository subscriptions, LessonProgressRepository progress, CouponRepository coupons,
                        CategoryRepository categories, MembershipPlanRepository plans, SupportTicketRepository tickets,
                        ReviewRepository reviews, NotificationService notifications, MailService mail) {
        this.users = users;
        this.courses = courses;
        this.enrollments = enrollments;
        this.orders = orders;
        this.certificates = certificates;
        this.subscriptions = subscriptions;
        this.progress = progress;
        this.coupons = coupons;
        this.categories = categories;
        this.plans = plans;
        this.tickets = tickets;
        this.reviews = reviews;
        this.notifications = notifications;
        this.mail = mail;
    }

    @Transactional(readOnly = true)
    public AdminStats stats(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(ZONE) : to;
        LocalDate start = from == null ? end.minusMonths(11).withDayOfMonth(1) : from;
        Instant a = start.atStartOfDay(ZONE).toInstant(), b = end.plusDays(1).atStartOfDay(ZONE).toInstant();
        List<PurchaseOrder> paid = orders.findByStatusAndPaidAtBetween(PurchaseOrder.Status.PAID, a, b);
        List<PurchaseOrder> refunded = orders.findByStatusAndPaidAtBetween(PurchaseOrder.Status.REFUNDED, a, b);

        Map<String, int[]> months = new LinkedHashMap<>();
        for (YearMonth m = YearMonth.from(start); !m.isAfter(YearMonth.from(end)); m = m.plusMonths(1)) months.put(m.format(MONTH), new int[3]);
        int inr = 0, usd = 0, subInr = 0, subUsd = 0;
        Map<Long, long[]> profit = new HashMap<>();
        Map<Long, String> titles = new HashMap<>();
        for (PurchaseOrder o : paid) {
            boolean isUsd = "USD".equals(o.currency);
            if (isUsd) usd += o.total; else inr += o.total;
            if (o.plan != null) { if (isUsd) subUsd += o.total; else subInr += o.total; }
            int[] m = months.get(YearMonth.from(o.paidAt.atZone(ZONE)).format(MONTH));
            if (m != null) { if (isUsd) m[1] += o.total; else m[0] += o.total; m[2]++; }
            if (o.course != null && !isUsd) {
                profit.computeIfAbsent(o.course.id, k -> new long[1])[0] += o.total;
                titles.put(o.course.id, o.course.title);
            }
        }
        List<MonthPoint> monthly = months.entrySet().stream().map(e -> new MonthPoint(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2])).toList();
        List<TopCourse> popular = enrollments.topCourses().stream().limit(5)
                .map(r -> new TopCourse((Long) r[0], (String) r[1], ((Number) r[2]).longValue())).toList();
        List<TopCourse> profitable = profit.entrySet().stream().sorted((x, y) -> Long.compare(y.getValue()[0], x.getValue()[0])).limit(5)
                .map(e -> new TopCourse(e.getKey(), titles.get(e.getKey()), e.getValue()[0])).toList();
        long students = users.countByRole(User.Role.STUDENT);
        long active = progress.countActiveLearners(Instant.now().minus(Duration.ofDays(30)));
        double retention = students == 0 ? 0 : Math.round(1000.0 * active / students) / 10.0;
        long published = courses.findByStatus(Course.Status.PUBLISHED).size();
        long pending = courses.findByStatus(Course.Status.PENDING_REVIEW).size();
        long activeSubs = subscriptions.countByStatusInAndExpiresAtAfter(List.of("ACTIVE", "CANCELLED"), Instant.now());
        return new AdminStats(students, users.countByRole(User.Role.INSTRUCTOR), courses.count(), published, pending,
                enrollments.count(), active, certificates.count(), activeSubs, inr, usd, subInr, subUsd, paid.size(),
                refunded.size(), monthly, popular, profitable, retention);
    }

    // ---------- Users ----------
    @Transactional(readOnly = true)
    public List<AdminUser> users(String q) {
        String t = q == null ? "" : q.trim();
        return users.findTop200ByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByIdDesc(t, t).stream()
                .map(u -> new AdminUser(u.id, u.name, u.email, u.role.name(), u.enabled, u.emailVerified, u.createdAt)).toList();
    }

    public void setRole(Long adminId, Long userId, String role) {
        if (adminId.equals(userId)) throw ApiException.badRequest("You cannot change your own role");
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        u.role = User.Role.valueOf(role);
        audit.info("admin.role admin={} user={} role={}", adminId, userId, role);
    }

    public void setEnabled(Long adminId, Long userId, boolean enabled) {
        if (adminId.equals(userId)) throw ApiException.badRequest("You cannot disable yourself");
        users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found")).enabled = enabled;
        audit.info("admin.enabled admin={} user={} enabled={}", adminId, userId, enabled);
    }

    public void manualEnroll(Long adminId, ManualEnrollReq req) {
        User u = users.findByEmailIgnoreCase(req.email().trim()).orElseThrow(() -> ApiException.notFound("No user with that email"));
        Course c = courses.findBySlug(req.courseSlug()).orElseThrow(() -> ApiException.notFound("Course not found"));
        List<Course> grant = new ArrayList<>(List.of(c));
        if (c.kind == Course.Kind.BUNDLE) grant.addAll(c.bundleItems);
        for (Course x : grant) {
            Enrollment e = enrollments.findByUser_IdAndCourse_Id(u.id, x.id).orElseGet(Enrollment::new);
            e.user = u;
            e.course = x;
            e.source = Enrollment.Source.ADMIN;
            e.active = true;
            enrollments.save(e);
        }
        notifications.notify(u, "You've been enrolled in " + c.title, "An administrator gave you access. Happy learning!", "/dashboard", true);
        audit.info("admin.enroll admin={} user={} course={}", adminId, u.id, c.id);
    }

    // ---------- Courses ----------
    @Transactional(readOnly = true)
    public List<CourseCard> allCourses() {
        return courses.findAllByOrderByIdDesc().stream().map(CatalogService::card).toList();
    }

    public CourseCard setStatus(Long adminId, Long courseId, String action) {
        Course c = courses.findById(courseId).orElseThrow(() -> ApiException.notFound("Course not found"));
        switch (action) {
            case "approve" -> InstructorService.publish(c);
            case "reject" -> c.status = Course.Status.DRAFT;
            case "unpublish" -> c.status = Course.Status.UNPUBLISHED;
            case "feature" -> c.featured = true;
            case "unfeature" -> c.featured = false;
            default -> throw ApiException.badRequest("Unknown action");
        }
        if (c.instructor != null && ("approve".equals(action) || "reject".equals(action)))
            notifications.notify(c.instructor, "Course " + ("approve".equals(action) ? "approved" : "returned for changes") + ": " + c.title,
                    "approve".equals(action) ? "Your course is now live." : "An administrator returned your course to draft. Please review and resubmit.",
                    "/instructor/courses/" + c.id, true);
        audit.info("admin.course admin={} course={} action={}", adminId, courseId, action);
        return CatalogService.card(c);
    }

    // ---------- Orders ----------
    @Transactional(readOnly = true)
    public List<OrderDto> orders() {
        return orders.findTop300ByOrderByIdDesc().stream().map(CheckoutService::order).toList();
    }

    // ---------- Coupons ----------
    @Transactional(readOnly = true)
    public List<CouponDto> coupons() {
        return coupons.findAll().stream().map(c -> new CouponDto(c.id, c.code, c.percentOff, c.active, c.expiresAt, c.maxUses, c.uses, c.courseSlug)).toList();
    }

    public void saveCoupon(CouponDto d) {
        Coupon c = d.id() == null ? coupons.findByCodeIgnoreCase(d.code()).orElseGet(Coupon::new) : coupons.findById(d.id()).orElseThrow();
        c.code = d.code().trim().toUpperCase();
        c.percentOff = d.percentOff();
        c.active = d.active();
        c.expiresAt = d.expiresAt();
        c.maxUses = d.maxUses();
        c.courseSlug = d.courseSlug() == null || d.courseSlug().isBlank() ? null : d.courseSlug().trim();
        coupons.save(c);
    }

    public void deleteCoupon(Long id) { coupons.deleteById(id); }

    // ---------- Categories ----------
    public void addCategory(String name) {
        if (name == null || name.isBlank()) throw ApiException.badRequest("Name is required");
        if (categories.existsByNameIgnoreCase(name.trim())) throw ApiException.conflict("Category already exists");
        Category c = new Category();
        c.name = name.trim();
        categories.save(c);
    }

    public void deleteCategory(Long id) { categories.deleteById(id); }

    // ---------- Plans ----------
    @Transactional(readOnly = true)
    public List<PlanDto> plans() {
        return plans.findAll().stream().map(AdminService::plan).toList();
    }

    public static PlanDto plan(MembershipPlan p) {
        return new PlanDto(p.id, p.code, p.name, p.description, p.priceInr, p.priceUsd, p.periodDays, p.category, p.level, p.active);
    }

    public void savePlan(PlanDto d) {
        MembershipPlan p = d.id() == null ? new MembershipPlan() : plans.findById(d.id()).orElseThrow();
        p.code = d.code().trim().toUpperCase();
        p.name = d.name();
        p.description = d.description();
        p.priceInr = d.priceInr();
        p.priceUsd = d.priceUsd();
        p.periodDays = d.periodDays() <= 0 ? 30 : d.periodDays();
        p.category = d.category() == null || d.category().isBlank() ? null : d.category();
        p.level = d.level() == null || d.level().isBlank() ? null : d.level();
        p.active = d.active();
        plans.save(p);
    }

    // ---------- Tickets ----------
    @Transactional(readOnly = true)
    public List<TicketDto> tickets() {
        return tickets.findTop300ByOrderByIdDesc().stream().map(AdminService::ticket).toList();
    }

    public static TicketDto ticket(SupportTicket t) {
        return new TicketDto(t.id, t.type, t.name, t.email, t.subject, t.message, t.reply, t.status, t.createdAt);
    }

    public void updateTicket(Long id, TicketUpdate u) {
        SupportTicket t = tickets.findById(id).orElseThrow(() -> ApiException.notFound("Ticket not found"));
        if (u.reply() != null && !u.reply().isBlank() && !u.reply().equals(t.reply)) {
            t.reply = u.reply();
            mail.send(t.email, "Re: " + (t.subject == null ? "Your message" : t.subject), u.reply());
            if (t.userId != null) users.findById(t.userId).ifPresent(usr ->
                    notifications.notify(usr, "Support replied: " + t.subject, u.reply(), "/dashboard?tab=support", false));
        }
        if (u.status() != null) t.status = u.status();
    }

    // ---------- Reviews ----------
    @Transactional(readOnly = true)
    public List<ReviewDto> reviews() {
        return reviews.findTop300ByOrderByIdDesc().stream().map(CatalogService::review).toList();
    }

    public void setReviewVisible(Long id, boolean visible) {
        Review r = reviews.findById(id).orElseThrow(() -> ApiException.notFound("Review not found"));
        if (r.visible != visible) {
            r.visible = visible;
            r.course.ratingSum += visible ? r.rating : -r.rating;
            r.course.ratingCount += visible ? 1 : -1;
        }
    }
}
