package com.cloudnest.academy.web;

import com.cloudnest.academy.config.ApiException;
import com.cloudnest.academy.config.AppConfig;
import com.cloudnest.academy.domain.*;
import com.cloudnest.academy.security.CurrentUser;
import com.cloudnest.academy.service.*;
import com.cloudnest.academy.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PublicController {
    private final CatalogService catalog;
    private final CategoryRepository categories;
    private final MembershipPlanRepository plans;
    private final CertificateRepository certificates;
    private final CourseRepository courses;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final SupportTicketRepository tickets;
    private final NewsletterSubscriberRepository newsletter;
    private final AccessService access;
    private final DocumentService documents;
    private final AppConfig cfg;

    public PublicController(CatalogService catalog, CategoryRepository categories, MembershipPlanRepository plans,
                            CertificateRepository certificates, CourseRepository courses, ReviewRepository reviews,
                            UserRepository users, SupportTicketRepository tickets, NewsletterSubscriberRepository newsletter,
                            AccessService access, DocumentService documents, AppConfig cfg) {
        this.catalog = catalog;
        this.categories = categories;
        this.plans = plans;
        this.certificates = certificates;
        this.courses = courses;
        this.reviews = reviews;
        this.users = users;
        this.tickets = tickets;
        this.newsletter = newsletter;
        this.access = access;
        this.documents = documents;
        this.cfg = cfg;
    }

    @GetMapping("/config")
    public PublicConfig config() {
        return new PublicConfig(cfg.razorpayEnabled() ? cfg.razorpayKeyId : null, cfg.razorpayEnabled(), cfg.stripeEnabled(),
                cfg.mockPayments, cfg.refundDays);
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return categories.findAllByOrderByNameAsc().stream().map(c -> new CategoryDto(c.id, c.name)).toList();
    }

    @GetMapping("/courses")
    public PageDto<CourseCard> courses(@RequestParam(required = false) String q, @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String kind, @RequestParam(required = false) String level,
                                       @RequestParam(required = false) String price, @RequestParam(required = false) String sort,
                                       @RequestParam(defaultValue = "false") boolean featured,
                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "24") int size) {
        return catalog.search(q, category, kind, level, price, sort, featured, page, size);
    }

    @GetMapping("/courses/{slug}")
    public CourseDetail course(@PathVariable String slug) { return catalog.detail(slug, CurrentUser.idOrNull()); }

    @PostMapping("/courses/{slug}/reviews")
    @Transactional
    public ReviewDto review(@PathVariable String slug, @Valid @RequestBody ReviewReq req) {
        User u = users.findById(CurrentUser.id()).orElseThrow();
        Course c = courses.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Course not found"));
        if (!access.hasAccess(u, c)) throw ApiException.forbidden("Only enrolled students can review this course");
        Review r = reviews.findByUser_IdAndCourse_Id(u.id, c.id).orElse(null);
        if (r == null) {
            r = new Review();
            r.user = u;
            r.course = c;
            c.ratingCount++;
        } else if (r.visible) {
            c.ratingSum -= r.rating;
        } else {
            c.ratingCount++;
        }
        r.rating = req.rating();
        r.comment = req.comment();
        r.visible = true;
        c.ratingSum += r.rating;
        reviews.save(r);
        return CatalogService.review(r);
    }

    @GetMapping("/plans")
    public List<PlanDto> plans() { return plans.findByActiveTrueOrderByPriceInrAsc().stream().map(AdminService::plan).toList(); }

    @GetMapping("/certificates/verify/{code}")
    @Transactional(readOnly = true)
    public CertificateDto verify(@PathVariable String code) {
        return certificates.findByCode(code.trim().toUpperCase()).map(LearningService::cert)
                .orElseThrow(() -> ApiException.notFound("No certificate found with this ID"));
    }

    @GetMapping("/certificates/{code}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> certificatePdf(@PathVariable String code) {
        Certificate c = certificates.findByCode(code.trim().toUpperCase()).orElseThrow(() -> ApiException.notFound("Certificate not found"));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=certificate-" + c.code + ".pdf")
                .body(documents.certificatePdf(c));
    }

    @PostMapping("/contact")
    public MessageResponse contact(@Valid @RequestBody ContactReq req) {
        SupportTicket t = new SupportTicket();
        t.type = req.type() == null ? "CONTACT" : req.type().toUpperCase();
        t.name = req.name();
        t.email = req.email();
        t.subject = req.subject();
        t.message = req.message();
        t.userId = CurrentUser.idOrNull();
        tickets.save(t);
        return new MessageResponse("Thanks! We'll get back to you within one working day.", null);
    }

    @PostMapping("/newsletter")
    public MessageResponse subscribe(@Valid @RequestBody EmailReq req) {
        if (!newsletter.existsByEmailIgnoreCase(req.email())) {
            NewsletterSubscriber s = new NewsletterSubscriber();
            s.email = req.email().trim().toLowerCase();
            newsletter.save(s);
        }
        return new MessageResponse("You're subscribed. Watch your inbox for new courses and offers!", null);
    }
}
