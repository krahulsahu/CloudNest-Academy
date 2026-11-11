package com.cloudnest.academy.service;

import com.cloudnest.academy.config.ApiException;
import com.cloudnest.academy.domain.*;
import com.cloudnest.academy.web.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class InstructorService {
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");
    private final CourseRepository courses;
    private final CourseSectionRepository sections;
    private final LessonRepository lessons;
    private final UserRepository users;
    private final EnrollmentRepository enrollments;
    private final PurchaseOrderRepository orders;
    private final LessonProgressRepository progress;
    private final QuizAttemptRepository attempts;
    private final CertificateRepository certificates;
    private final AccessService access;
    private final NotificationService notifications;

    public InstructorService(CourseRepository courses, CourseSectionRepository sections, LessonRepository lessons,
                             UserRepository users, EnrollmentRepository enrollments, PurchaseOrderRepository orders,
                             LessonProgressRepository progress, QuizAttemptRepository attempts,
                             CertificateRepository certificates, AccessService access, NotificationService notifications) {
        this.courses = courses;
        this.sections = sections;
        this.lessons = lessons;
        this.users = users;
        this.enrollments = enrollments;
        this.orders = orders;
        this.progress = progress;
        this.attempts = attempts;
        this.certificates = certificates;
        this.access = access;
        this.notifications = notifications;
    }

    private User me(Long id) { return users.findById(id).orElseThrow(() -> ApiException.unauthorized("Please log in")); }

    private Course owned(Long courseId, User u) {
        Course c = courses.findById(courseId).orElseThrow(() -> ApiException.notFound("Course not found"));
        if (!access.canManage(u, c)) throw ApiException.forbidden("You can only manage your own courses");
        return c;
    }

    public static String slugify(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return n.isEmpty() ? "course" : n;
    }

    private String uniqueSlug(String base, Long selfId) {
        String slug = base;
        int i = 2;
        while (true) {
            Optional<Course> other = courses.findBySlug(slug);
            if (other.isEmpty() || other.get().id.equals(selfId)) return slug;
            slug = base + "-" + i++;
        }
    }

    @Transactional(readOnly = true)
    public List<InstructorCourse> myCourses(Long userId) {
        User u = me(userId);
        List<Course> list = u.role == User.Role.ADMIN ? courses.findAllByOrderByIdDesc() : courses.findByInstructor_IdOrderByIdDesc(u.id);
        return list.stream().map(c -> {
            int inr = 0, usd = 0;
            for (PurchaseOrder o : orders.findByCourse_IdAndStatus(c.id, PurchaseOrder.Status.PAID)) {
                if ("USD".equals(o.currency)) usd += o.total; else inr += o.total;
            }
            long certs = certificates.countByCourse_Id(c.id);
            return new InstructorCourse(CatalogService.card(c), enrollments.countByCourse_IdAndActiveTrue(c.id), inr, usd, certs);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<CourseOption> options() {
        return courses.findAllByOrderByIdDesc().stream().map(c -> new CourseOption(c.id, c.title, c.kind.name())).toList();
    }

    public CourseEdit create(Long userId, CreateCourseReq req) {
        User u = me(userId);
        Course c = new Course();
        c.title = req.title().trim();
        c.slug = uniqueSlug(slugify(c.title), null);
        c.kind = "BUNDLE".equalsIgnoreCase(req.kind()) ? Course.Kind.BUNDLE : Course.Kind.COURSE;
        c.instructor = u;
        c.category = "AWS";
        c.subtitle = "";
        c.badges = "New|Course|COURSE|blue";
        courses.save(c);
        if (c.kind == Course.Kind.COURSE) {
            CourseSection s = new CourseSection();
            s.course = c;
            s.title = "Introduction";
            c.sections.add(s);
            sections.save(s);
        }
        audit.info("course.created id={} by={}", c.id, u.id);
        return edit(c);
    }

    @Transactional(readOnly = true)
    public CourseEdit get(Long userId, Long courseId) {
        return edit(owned(courseId, me(userId)));
    }

    public static CourseEdit edit(Course c) {
        List<SectionEdit> secs = c.sections.stream().map(s -> new SectionEdit(s.id, s.title, s.lessons.stream()
                .map(l -> new LessonEdit(l.id, l.title, l.type.name(), l.durationSeconds, l.videoUrl, l.captionsUrl, l.content,
                        l.resources, l.preview, l.passPercent, l.maxAttempts, l.timeLimitMinutes, l.randomize,
                        l.questions.stream().map(q -> new QuestionEdit(q.id, q.prompt, q.type.name(), q.options, q.correct, q.explanation)).toList()))
                .toList())).toList();
        return new CourseEdit(c.id, c.slug, c.title, c.subtitle, c.description, c.category, c.level, c.kind.name(),
                c.priceInr, c.priceUsd, c.compareAtInr, c.badges, c.theme, c.thumbnailUrl, c.previewVideoUrl, c.objectives,
                c.prerequisites, c.audience, c.seoTitle, c.seoDescription, c.status.name(), c.scheduledAt, c.featured,
                c.newBadge, c.bundleItems.stream().map(i -> i.id).toList(), secs, c.instructor == null ? null : c.instructor.name);
    }

    /** Saves the whole course tree (details + sections + lessons + quiz questions) in one call. */
    public CourseEdit save(Long userId, Long courseId, CourseEdit e) {
        User u = me(userId);
        Course c = owned(courseId, u);
        if (e.title() == null || e.title().isBlank()) throw ApiException.badRequest("Title is required");
        if (e.priceInr() < 0 || e.priceUsd() < 0) throw ApiException.badRequest("Prices cannot be negative");
        c.title = e.title().trim();
        if (e.slug() != null && !e.slug().isBlank()) c.slug = uniqueSlug(slugify(e.slug()), c.id);
        c.subtitle = e.subtitle();
        c.description = e.description();
        c.category = e.category();
        c.level = e.level();
        c.priceInr = e.priceInr();
        c.priceUsd = e.priceUsd();
        c.compareAtInr = e.compareAtInr();
        c.badges = e.badges();
        c.theme = e.theme();
        c.thumbnailUrl = e.thumbnailUrl();
        c.previewVideoUrl = e.previewVideoUrl();
        c.objectives = e.objectives();
        c.prerequisites = e.prerequisites();
        c.audience = e.audience();
        c.seoTitle = e.seoTitle();
        c.seoDescription = e.seoDescription();
        c.scheduledAt = e.scheduledAt();
        c.newBadge = e.isNew();
        if (u.role == User.Role.ADMIN) c.featured = e.featured();
        // Instructors may only move between DRAFT and UNPUBLISHED; publishing needs admin approval (submit()).
        if ("DRAFT".equals(e.status()) || "UNPUBLISHED".equals(e.status())) c.status = Course.Status.valueOf(e.status());
        if (u.role == User.Role.ADMIN && e.status() != null) c.status = Course.Status.valueOf(e.status());

        if (c.kind == Course.Kind.BUNDLE) {
            c.bundleItems.clear();
            if (e.bundleItemIds() != null) for (Long id : e.bundleItemIds())
                courses.findById(id).filter(x -> x.kind == Course.Kind.COURSE).ifPresent(c.bundleItems::add);
        } else {
            syncCurriculum(c, e.sections() == null ? List.of() : e.sections());
        }
        c.recount();
        audit.info("course.saved id={} by={}", c.id, u.id);
        return edit(c);
    }

    private void syncCurriculum(Course c, List<SectionEdit> edits) {
        Map<Long, CourseSection> oldSections = new HashMap<>();
        Map<Long, Lesson> oldLessons = new HashMap<>();
        for (CourseSection s : c.sections) {
            oldSections.put(s.id, s);
            for (Lesson l : s.lessons) oldLessons.put(l.id, l);
        }
        Set<Long> keepSections = edits.stream().map(SectionEdit::id).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> keepLessons = edits.stream().flatMap(s -> s.lessons() == null ? java.util.stream.Stream.<LessonEdit>empty() : s.lessons().stream())
                .map(LessonEdit::id).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Lesson> removedLessons = oldLessons.values().stream().filter(l -> !keepLessons.contains(l.id)).toList();
        List<CourseSection> removedSections = oldSections.values().stream().filter(s -> !keepSections.contains(s.id)).toList();

        for (CourseSection s : c.sections) s.lessons.clear();
        List<CourseSection> newList = new ArrayList<>();
        int sp = 0;
        for (SectionEdit se : edits) {
            CourseSection s = se.id() != null ? oldSections.get(se.id()) : null;
            if (s == null) {
                s = new CourseSection();
                s.course = c;
            }
            s.title = se.title() == null || se.title().isBlank() ? "Untitled section" : se.title().trim();
            s.position = sp++;
            int lp = 0;
            for (LessonEdit le : se.lessons() == null ? List.<LessonEdit>of() : se.lessons()) {
                Lesson l = le.id() != null ? oldLessons.get(le.id()) : null;
                if (l == null) l = new Lesson();
                l.section = s;
                l.position = lp++;
                l.title = le.title() == null || le.title().isBlank() ? "Untitled lesson" : le.title().trim();
                l.type = Lesson.Type.valueOf(le.type() == null ? "VIDEO" : le.type());
                l.durationSeconds = Math.max(0, le.durationSeconds());
                l.videoUrl = le.videoUrl();
                l.captionsUrl = le.captionsUrl();
                l.content = le.content();
                l.resources = le.resources();
                l.preview = le.preview();
                l.passPercent = le.passPercent() == null ? 70 : le.passPercent();
                l.maxAttempts = le.maxAttempts();
                l.timeLimitMinutes = le.timeLimitMinutes();
                l.randomize = le.randomize();
                l.questions.clear();
                int qp = 0;
                if (le.questions() != null) for (QuestionEdit qe : le.questions()) {
                    if (qe.prompt() == null || qe.prompt().isBlank()) continue;
                    QuizQuestion q = new QuizQuestion();
                    q.lesson = l;
                    q.prompt = qe.prompt().trim();
                    q.type = QuizQuestion.Type.valueOf(qe.type() == null ? "SINGLE" : qe.type());
                    q.options = qe.options();
                    q.correct = qe.correct();
                    q.explanation = qe.explanation();
                    q.position = qp++;
                    l.questions.add(q);
                }
                s.lessons.add(l);
            }
            newList.add(s);
        }
        c.sections.clear();
        c.sections.addAll(newList);
        for (CourseSection s : newList) sections.save(s);
        if (!removedLessons.isEmpty()) {
            progress.deleteByLessonIn(removedLessons);
            attempts.deleteByLessonIn(removedLessons);
            lessons.deleteAll(removedLessons);
        }
        sections.deleteAll(removedSections);
    }

    /** Instructor submits for review; admins publish (or schedule) directly. */
    public CourseEdit submit(Long userId, Long courseId) {
        User u = me(userId);
        Course c = owned(courseId, u);
        if (c.kind == Course.Kind.COURSE && c.lessonCount == 0) throw ApiException.badRequest("Add at least one lesson before publishing");
        if (c.kind == Course.Kind.BUNDLE && c.bundleItems.isEmpty()) throw ApiException.badRequest("Add courses to the bundle first");
        if (u.role == User.Role.ADMIN) publish(c);
        else c.status = Course.Status.PENDING_REVIEW;
        audit.info("course.submitted id={} by={} status={}", c.id, u.id, c.status);
        return edit(c);
    }

    public static void publish(Course c) {
        if (c.scheduledAt != null && c.scheduledAt.isAfter(Instant.now())) {
            c.status = Course.Status.SCHEDULED;
        } else {
            c.status = Course.Status.PUBLISHED;
            if (c.publishedAt == null) c.publishedAt = Instant.now();
        }
    }

    public void delete(Long userId, Long courseId) {
        Course c = owned(courseId, me(userId));
        if (enrollments.countByCourse_IdAndActiveTrue(c.id) > 0 || !orders.findByCourse_IdAndStatus(c.id, PurchaseOrder.Status.PAID).isEmpty())
            throw ApiException.badRequest("This course has students or orders. Unpublish it instead of deleting.");
        List<Lesson> all = c.allLessons();
        if (!all.isEmpty()) {
            progress.deleteByLessonIn(all);
            attempts.deleteByLessonIn(all);
        }
        courses.findAll().stream().filter(b -> b.kind == Course.Kind.BUNDLE).forEach(b -> b.bundleItems.remove(c));
        courses.delete(c);
    }

    @Transactional(readOnly = true)
    public List<StudentDto> students(Long userId, Long courseId) {
        Course c = owned(courseId, me(userId));
        int total = c.lessonCount;
        List<Lesson> quizzes = c.allLessons().stream().filter(l -> l.type == Lesson.Type.QUIZ).toList();
        return enrollments.findByCourse_IdAndActiveTrue(c.id).stream().map(e -> {
            int done = (int) progress.findByUser_IdAndCourseId(e.user.id, c.id).stream().filter(p -> p.completed).count();
            Integer last = null;
            for (Lesson q : quizzes) {
                var a = attempts.findByUser_IdAndLesson_IdOrderByIdDesc(e.user.id, q.id);
                if (!a.isEmpty()) last = a.get(0).total == 0 ? 0 : a.get(0).score * 100 / a.get(0).total;
            }
            return new StudentDto(e.user.id, e.user.name, e.user.email, e.createdAt, e.source.name(), done, total, last);
        }).toList();
    }

    public int announce(Long userId, Long courseId, AnnounceReq req) {
        Course c = owned(courseId, me(userId));
        List<Enrollment> list = enrollments.findByCourse_IdAndActiveTrue(c.id);
        for (Enrollment e : list) notifications.notify(e.user, "[" + c.title + "] " + req.title(), req.body(), "/learn/" + c.slug, true);
        return list.size();
    }
}
