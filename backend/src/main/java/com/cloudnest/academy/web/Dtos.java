package com.cloudnest.academy.web;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** All request/response payloads of the REST API. Entities are never serialised directly. */
public final class Dtos {
    private Dtos() {}

    // ---------- Auth ----------
    public record UserDto(Long id, String name, String email, String role, boolean emailVerified,
                          String country, String headline, String bio, boolean hasPassword) {}
    public record AuthResponse(String accessToken, String refreshToken, UserDto user) {}
    public record MessageResponse(String message, String devCode) {}
    public record RegisterReq(@NotBlank @Size(max = 120) String name, @NotBlank @Email String email,
                              @Size(max = 100) String password, boolean marketing, String country) {}
    public record EmailReq(@NotBlank @Email String email) {}
    public record OtpVerifyReq(@NotBlank @Email String email, @NotBlank String code) {}
    public record LoginReq(@NotBlank @Email String email, @NotBlank String password) {}
    public record RefreshReq(@NotBlank String refreshToken) {}
    public record ResetReq(@NotBlank @Email String email, @NotBlank String code, @NotBlank @Size(min = 8, max = 100) String newPassword) {}
    public record ProfileReq(@NotBlank @Size(max = 120) String name, String country, String headline, String bio) {}
    public record PasswordReq(String currentPassword, @NotBlank @Size(min = 8, max = 100) String newPassword) {}

    // ---------- Catalog ----------
    public record CourseCard(Long id, String slug, String title, String subtitle, String kind, String category,
                             String level, int priceInr, int priceUsd, Integer compareAtInr, boolean featured,
                             boolean isNew, String badges, String theme, String thumbnailUrl, String instructorName,
                             int itemCount, int lessonCount, int totalSeconds, double rating, int reviewCount,
                             String status) {}
    public record PageDto<T>(List<T> items, long total, int page, int size) {}
    public record LessonSummary(Long id, String title, String type, int durationSeconds, boolean preview, boolean completed) {}
    public record SectionDto(Long id, String title, List<LessonSummary> lessons) {}
    public record InstructorDto(Long id, String name, String headline, String bio) {}
    public record ReviewDto(Long id, String userName, int rating, String comment, Instant createdAt, String courseTitle, boolean visible) {}
    public record CourseDetail(CourseCard card, String description, List<String> objectives, List<String> prerequisites,
                               String audience, String previewVideoUrl, InstructorDto instructor, List<SectionDto> sections,
                               List<CourseCard> bundleItems, List<CourseCard> inBundles, List<CourseCard> related,
                               boolean hasAccess, List<ReviewDto> reviews, boolean canReview,
                               String seoTitle, String seoDescription) {}
    public record CategoryDto(Long id, String name) {}
    public record PlanDto(Long id, String code, String name, String description, int priceInr, int priceUsd,
                          int periodDays, String category, String level, boolean active) {}
    public record PublicConfig(String razorpayKeyId, boolean razorpay, boolean stripe, boolean mockPayments, int refundDays) {}
    public record ReviewReq(@Min(1) @Max(5) int rating, @Size(max = 3000) String comment) {}
    public record ContactReq(String type, @NotBlank @Size(max = 120) String name, @NotBlank @Email String email,
                             @Size(max = 200) String subject, @NotBlank @Size(max = 5000) String message) {}

    // ---------- Learning ----------
    public record ResourceDto(String label, String url) {}
    public record QuestionDto(Long id, String prompt, String type, List<String> options) {}
    public record LessonDto(Long id, String title, String type, int durationSeconds, String sectionTitle,
                            String videoUrl, String captionsUrl, String content, List<ResourceDto> resources,
                            List<QuestionDto> questions, Integer passPercent, Integer maxAttempts, Integer timeLimitMinutes,
                            int attemptsUsed, Integer bestPercent, boolean completed, int positionSeconds,
                            boolean bookmarked, String note, boolean hasAccess, Long prevId, Long nextId) {}
    public record LearnCourse(CourseCard card, List<SectionDto> sections, int completed, int total,
                              Long resumeLessonId, String certificateCode, boolean hasAccess) {}
    public record ProgressReq(Integer positionSeconds, Boolean completed, Boolean bookmarked) {}
    public record NoteReq(@Size(max = 10000) String text) {}
    public record QuizSubmit(Map<Long, String> answers) {}
    public record QuestionResult(Long id, boolean correct, String correctAnswer, String explanation) {}
    public record QuizResult(int score, int total, int percent, boolean passed, List<QuestionResult> results) {}
    public record MyCourse(CourseCard card, int completed, int total, Long resumeLessonId, String certificateCode,
                           String access, Instant expiresAt) {}
    public record BookmarkDto(Long lessonId, String lessonTitle, String courseSlug, String courseTitle) {}
    public record QuizHistoryDto(String courseTitle, String lessonTitle, int score, int total, boolean passed, Instant at) {}
    public record CertificateDto(String code, String studentName, String courseTitle, String courseSlug, Instant issuedAt) {}
    public record NotificationDto(Long id, String title, String body, String link, boolean seen, Instant createdAt) {}

    // ---------- Commerce ----------
    public record QuoteReq(String courseSlug, String planCode, String coupon, String currency) {}
    public record Quote(String currency, int listPrice, int credit, int discount, int total, String itemName, String couponMessage) {}
    public record CheckoutResponse(Long orderId, String provider, int total, String currency, boolean completed,
                                   String razorpayOrderId, String razorpayKeyId, String stripeUrl,
                                   String userName, String userEmail, String description, String redirect) {}
    public record RazorpayVerifyReq(@NotNull Long orderId, @NotBlank String razorpayOrderId,
                                    @NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) {}
    public record OrderDto(Long id, String invoiceNumber, String item, String currency, int listPrice, int credit,
                           int discount, int tax, int total, String status, String provider, Instant createdAt,
                           Instant paidAt, String userEmail, String couponCode) {}
    public record SubscriptionDto(Long id, String planName, String planCode, Instant startsAt, Instant expiresAt,
                                  boolean autoRenew, String status) {}

    // ---------- Instructor ----------
    public record CreateCourseReq(@NotBlank @Size(max = 200) String title, String kind) {}
    public record QuestionEdit(Long id, String prompt, String type, String options, String correct, String explanation) {}
    public record LessonEdit(Long id, String title, String type, int durationSeconds, String videoUrl, String captionsUrl,
                             String content, String resources, boolean preview, Integer passPercent, Integer maxAttempts,
                             Integer timeLimitMinutes, boolean randomize, List<QuestionEdit> questions) {}
    public record SectionEdit(Long id, String title, List<LessonEdit> lessons) {}
    public record CourseEdit(Long id, String slug, String title, String subtitle, String description, String category,
                             String level, String kind, int priceInr, int priceUsd, Integer compareAtInr, String badges,
                             String theme, String thumbnailUrl, String previewVideoUrl, String objectives,
                             String prerequisites, String audience, String seoTitle, String seoDescription,
                             String status, Instant scheduledAt, boolean featured, boolean isNew,
                             List<Long> bundleItemIds, List<SectionEdit> sections, String instructorName) {}
    public record InstructorCourse(CourseCard card, long students, int revenueInr, int revenueUsd, long certificates) {}
    public record StudentDto(Long userId, String name, String email, Instant enrolledAt, String source,
                             int completed, int total, Integer lastQuizPercent) {}
    public record AnnounceReq(@NotBlank String title, @NotBlank @Size(max = 3000) String body) {}
    public record UploadResponse(String ref, String fileName, long size) {}
    public record CourseOption(Long id, String title, String kind) {}

    // ---------- Admin ----------
    public record MonthPoint(String month, int revenueInr, int revenueUsd, int orders) {}
    public record TopCourse(Long id, String title, long value) {}
    public record AdminStats(long students, long instructors, long courses, long publishedCourses, long pendingCourses,
                             long enrollments, long activeLearners30d, long certificates, long activeSubscriptions,
                             int revenueInr, int revenueUsd, int subscriptionRevenueInr, int subscriptionRevenueUsd,
                             long paidOrders, long refundedOrders, List<MonthPoint> monthly,
                             List<TopCourse> popular, List<TopCourse> profitableInr, double retentionPercent) {}
    public record AdminUser(Long id, String name, String email, String role, boolean enabled, boolean emailVerified, Instant createdAt) {}
    public record RoleReq(@NotBlank String role) {}
    public record EnabledReq(boolean enabled) {}
    public record CouponDto(Long id, @NotBlank String code, @Min(1) @Max(100) int percentOff, boolean active,
                            Instant expiresAt, Integer maxUses, int uses, String courseSlug) {}
    public record TicketDto(Long id, String type, String name, String email, String subject, String message,
                            String reply, String status, Instant createdAt) {}
    public record TicketUpdate(String status, String reply) {}
    public record ManualEnrollReq(@NotBlank @Email String email, @NotBlank String courseSlug) {}
    public record SupportReq(@NotBlank @Size(max = 200) String subject, @NotBlank @Size(max = 5000) String message) {}
}
