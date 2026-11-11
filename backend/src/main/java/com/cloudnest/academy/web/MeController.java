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
@RequestMapping("/api/me")
public class MeController {
    private final UserRepository users;
    private final AuthService auth;
    private final LearningService learning;
    private final CheckoutService checkout;
    private final SubscriptionRepository subscriptions;
    private final CertificateRepository certificates;
    private final NotificationRepository notifications;
    private final SupportTicketRepository tickets;
    private final PurchaseOrderRepository orders;
    private final DocumentService documents;
    private final AppConfig cfg;

    public MeController(UserRepository users, AuthService auth, LearningService learning, CheckoutService checkout,
                        SubscriptionRepository subscriptions, CertificateRepository certificates,
                        NotificationRepository notifications, SupportTicketRepository tickets,
                        PurchaseOrderRepository orders, DocumentService documents, AppConfig cfg) {
        this.users = users;
        this.auth = auth;
        this.learning = learning;
        this.checkout = checkout;
        this.subscriptions = subscriptions;
        this.certificates = certificates;
        this.notifications = notifications;
        this.tickets = tickets;
        this.orders = orders;
        this.documents = documents;
        this.cfg = cfg;
    }

    @GetMapping
    public UserDto me() { return AuthService.toDto(users.findById(CurrentUser.id()).orElseThrow()); }

    @PutMapping
    public UserDto update(@Valid @RequestBody ProfileReq req) { return auth.updateProfile(CurrentUser.id(), req); }

    @PutMapping("/password")
    public void password(@Valid @RequestBody PasswordReq req) { auth.changePassword(CurrentUser.id(), req); }

    @PostMapping("/logout-all")
    public void logoutAll() { auth.logoutEverywhere(CurrentUser.id()); }

    @GetMapping("/courses")
    public List<MyCourse> courses() { return learning.myCourses(CurrentUser.id()); }

    @GetMapping("/bookmarks")
    public List<BookmarkDto> bookmarks() { return learning.bookmarks(CurrentUser.id()); }

    @GetMapping("/quiz-history")
    public List<QuizHistoryDto> quizHistory() { return learning.quizHistory(CurrentUser.id()); }

    @GetMapping("/certificates")
    @Transactional(readOnly = true)
    public List<CertificateDto> certificates() {
        return certificates.findByUser_IdOrderByIdDesc(CurrentUser.id()).stream().map(LearningService::cert).toList();
    }

    @GetMapping("/orders")
    public List<OrderDto> orders() { return checkout.myOrders(CurrentUser.id()); }

    @GetMapping("/orders/{id}")
    public OrderDto order(@PathVariable Long id) { return checkout.myOrder(CurrentUser.id(), id); }

    @GetMapping("/orders/{id}/invoice")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> invoice(@PathVariable Long id) {
        PurchaseOrder o = orders.findById(id).filter(x -> x.user.id.equals(CurrentUser.id()) && x.invoiceNumber != null)
                .orElseThrow(() -> ApiException.notFound("Invoice not found"));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + o.invoiceNumber + ".pdf")
                .body(documents.invoicePdf(o));
    }

    @PostMapping("/orders/{id}/refund-request")
    @Transactional
    public MessageResponse refundRequest(@PathVariable Long id, @RequestBody(required = false) SupportReq req) {
        OrderDto o = checkout.myOrder(CurrentUser.id(), id);
        User u = users.findById(CurrentUser.id()).orElseThrow();
        SupportTicket t = new SupportTicket();
        t.type = "REFUND";
        t.userId = u.id;
        t.name = u.name;
        t.email = u.email;
        t.subject = "Refund request for order #" + o.id() + " (" + o.item() + ")";
        t.message = req == null ? "Refund requested" : req.message();
        tickets.save(t);
        return new MessageResponse("Refund request received. Our team will review it within 2 working days.", null);
    }

    @GetMapping("/subscriptions")
    @Transactional(readOnly = true)
    public List<SubscriptionDto> subscriptions() {
        return subscriptions.findByUser_IdOrderByIdDesc(CurrentUser.id()).stream().map(MeController::sub).toList();
    }

    @PostMapping("/subscriptions/{id}/cancel")
    @Transactional
    public SubscriptionDto cancel(@PathVariable Long id) {
        Subscription s = subscriptions.findById(id).filter(x -> x.user.id.equals(CurrentUser.id()))
                .orElseThrow(() -> ApiException.notFound("Subscription not found"));
        if ("ACTIVE".equals(s.status)) {
            s.status = "CANCELLED";
            s.autoRenew = false;
        }
        return sub(s);
    }

    static SubscriptionDto sub(Subscription s) {
        return new SubscriptionDto(s.id, s.plan.name, s.plan.code, s.startsAt, s.expiresAt, s.autoRenew, s.status);
    }

    @GetMapping("/notifications")
    @Transactional(readOnly = true)
    public List<NotificationDto> notifications() {
        return notifications.findTop50ByUser_IdOrderByIdDesc(CurrentUser.id()).stream()
                .map(n -> new NotificationDto(n.id, n.title, n.body, n.link, n.seen, n.createdAt)).toList();
    }

    @GetMapping("/notifications/unread")
    public long unread() { return notifications.countByUser_IdAndSeenFalse(CurrentUser.id()); }

    @PostMapping("/notifications/read-all")
    @Transactional
    public void readAll() { notifications.findByUser_IdAndSeenFalse(CurrentUser.id()).forEach(n -> n.seen = true); }

    @GetMapping("/tickets")
    public List<TicketDto> tickets() {
        return tickets.findByUserIdOrderByIdDesc(CurrentUser.id()).stream().map(AdminService::ticket).toList();
    }

    @PostMapping("/tickets")
    public TicketDto openTicket(@Valid @RequestBody SupportReq req) {
        User u = users.findById(CurrentUser.id()).orElseThrow();
        SupportTicket t = new SupportTicket();
        t.type = "SUPPORT";
        t.userId = u.id;
        t.name = u.name;
        t.email = u.email;
        t.subject = req.subject();
        t.message = req.message();
        return AdminService.ticket(tickets.save(t));
    }
}
