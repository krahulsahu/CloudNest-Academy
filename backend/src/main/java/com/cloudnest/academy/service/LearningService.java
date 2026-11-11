package com.cloudnest.academy.service;

import com.cloudnest.academy.config.ApiException;
import com.cloudnest.academy.domain.*;
import com.cloudnest.academy.web.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LearningService {
    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final UserRepository users;
    private final LessonProgressRepository progress;
    private final QuizAttemptRepository attempts;
    private final CertificateRepository certificates;
    private final EnrollmentRepository enrollments;
    private final AccessService access;
    private final MediaService media;
    private final NotificationService notifications;

    public LearningService(CourseRepository courses, LessonRepository lessons, UserRepository users,
                           LessonProgressRepository progress, QuizAttemptRepository attempts,
                           CertificateRepository certificates, EnrollmentRepository enrollments,
                           AccessService access, MediaService media, NotificationService notifications) {
        this.courses = courses;
        this.lessons = lessons;
        this.users = users;
        this.progress = progress;
        this.attempts = attempts;
        this.certificates = certificates;
        this.enrollments = enrollments;
        this.access = access;
        this.media = media;
        this.notifications = notifications;
    }

    private Course course(String slug, User u) {
        Course c = courses.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Course not found"));
        if (c.status != Course.Status.PUBLISHED && !access.canManage(u, c)) throw ApiException.notFound("Course not found");
        if (c.kind == Course.Kind.BUNDLE) throw ApiException.badRequest("Open the individual courses of this bundle from My Courses");
        return c;
    }

    private User userOrNull(Long id) { return id == null ? null : users.findById(id).orElse(null); }

    private Map<Long, LessonProgress> progressMap(User u, Course c) {
        if (u == null) return Map.of();
        return progress.findByUser_IdAndCourseId(u.id, c.id).stream().collect(Collectors.toMap(p -> p.lesson.id, p -> p, (a, b) -> a));
    }

    @Transactional(readOnly = true)
    public LearnCourse learnCourse(String slug, Long userId) {
        User u = userOrNull(userId);
        Course c = course(slug, u);
        boolean hasAccess = access.hasAccess(u, c);
        Map<Long, LessonProgress> pm = progressMap(u, c);
        List<SectionDto> sections = c.sections.stream().map(s -> new SectionDto(s.id, s.title, s.lessons.stream()
                .map(l -> new LessonSummary(l.id, l.title, l.type.name(), l.durationSeconds, l.preview,
                        pm.containsKey(l.id) && pm.get(l.id).completed)).toList())).toList();
        List<Lesson> all = c.allLessons();
        int done = (int) all.stream().filter(l -> pm.containsKey(l.id) && pm.get(l.id).completed).count();
        Long resume = pm.values().stream().max(Comparator.comparing(p -> p.updatedAt)).map(p -> p.lesson.id)
                .orElse(all.isEmpty() ? null : all.get(0).id);
        if (!hasAccess) resume = all.stream().filter(l -> l.preview).map(l -> l.id).findFirst().orElse(null);
        String cert = u == null ? null : certificates.findByUser_IdAndCourse_Id(u.id, c.id).map(x -> x.code).orElse(null);
        return new LearnCourse(CatalogService.card(c), sections, done, all.size(), resume, cert, hasAccess);
    }

    @Transactional(readOnly = true)
    public LessonDto lesson(String slug, Long lessonId, Long userId) {
        User u = userOrNull(userId);
        Course c = course(slug, u);
        List<Lesson> all = c.allLessons();
        Lesson l = all.stream().filter(x -> x.id.equals(lessonId)).findFirst().orElseThrow(() -> ApiException.notFound("Lesson not found"));
        boolean hasAccess = access.hasAccess(u, c);
        if (!hasAccess && !l.preview) {
            if (u == null) throw ApiException.unauthorized("Please log in to view this lesson");
            throw ApiException.forbidden("Enroll in this course to unlock this lesson");
        }
        LessonProgress p = u == null ? null : progress.findByUser_IdAndLesson_Id(u.id, l.id).orElse(null);
        List<ResourceDto> res = CatalogService.lines(l.resources).stream().map(line -> {
            String[] parts = line.split("\\|", 2);
            return parts.length == 2 ? new ResourceDto(parts[0].trim(), media.publicUrl(parts[1].trim()))
                    : new ResourceDto(line, media.publicUrl(line));
        }).toList();
        List<QuestionDto> qs = l.questions.stream()
                .map(q -> new QuestionDto(q.id, q.prompt, q.type.name(), q.type == QuizQuestion.Type.TRUE_FALSE
                        ? List.of("True", "False") : CatalogService.lines(q.options)))
                .collect(Collectors.toCollection(ArrayList::new));
        if (l.randomize) Collections.shuffle(qs);
        List<QuizAttempt> tries = u == null ? List.of() : attempts.findByUser_IdAndLesson_IdOrderByIdDesc(u.id, l.id);
        Integer best = tries.stream().map(a -> a.total == 0 ? 0 : a.score * 100 / a.total).max(Integer::compare).orElse(null);
        int idx = all.indexOf(l);
        Long prev = idx > 0 ? all.get(idx - 1).id : null;
        Long next = idx < all.size() - 1 ? all.get(idx + 1).id : null;
        return new LessonDto(l.id, l.title, l.type.name(), l.durationSeconds, l.section.title,
                media.publicUrl(l.videoUrl), media.publicUrl(l.captionsUrl), l.content, res, qs, l.passPercent,
                l.maxAttempts, l.timeLimitMinutes, tries.size(), best, p != null && p.completed,
                p == null ? 0 : p.positionSeconds, p != null && p.bookmarked, p == null ? null : p.note, hasAccess, prev, next);
    }

    private record Ctx(User user, Lesson lesson, Course course) {}

    private Ctx requireAccess(Long lessonId, Long userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Please log in"));
        Lesson l = lessons.findById(lessonId).orElseThrow(() -> ApiException.notFound("Lesson not found"));
        Course c = l.section.course;
        if (!access.hasAccess(u, c)) throw ApiException.forbidden("Enroll in this course to save progress");
        return new Ctx(u, l, c);
    }

    private LessonProgress progressFor(Ctx ctx) {
        return progress.findByUser_IdAndLesson_Id(ctx.user.id, ctx.lesson.id).orElseGet(() -> {
            LessonProgress p = new LessonProgress();
            p.user = ctx.user;
            p.lesson = ctx.lesson;
            p.courseId = ctx.course.id;
            return progress.save(p);
        });
    }

    public void updateProgress(Long lessonId, Long userId, ProgressReq req) {
        Ctx ctx = requireAccess(lessonId, userId);
        LessonProgress p = progressFor(ctx);
        if (req.positionSeconds() != null) p.positionSeconds = Math.max(0, req.positionSeconds());
        if (req.bookmarked() != null) p.bookmarked = req.bookmarked();
        if (req.completed() != null) {
            if (req.completed() && ctx.lesson.type == Lesson.Type.QUIZ && !ctx.lesson.questions.isEmpty()
                    && !attempts.existsByUser_IdAndLesson_IdAndPassedTrue(userId, lessonId))
                throw ApiException.badRequest("Pass the quiz to complete this lesson");
            p.completed = req.completed();
        }
        p.updatedAt = Instant.now();
    }

    public void saveNote(Long lessonId, Long userId, String text) {
        LessonProgress p = progressFor(requireAccess(lessonId, userId));
        p.note = text;
        p.updatedAt = Instant.now();
    }

    public QuizResult submitQuiz(Long lessonId, Long userId, QuizSubmit req) {
        Ctx ctx = requireAccess(lessonId, userId);
        Lesson l = ctx.lesson;
        if (l.questions.isEmpty()) throw ApiException.badRequest("This lesson has no quiz");
        if (l.maxAttempts != null && l.maxAttempts > 0
                && attempts.findByUser_IdAndLesson_IdOrderByIdDesc(userId, lessonId).size() >= l.maxAttempts)
            throw ApiException.badRequest("You have used all " + l.maxAttempts + " attempts for this quiz");
        Map<Long, String> answers = req.answers() == null ? Map.of() : req.answers();
        int score = 0;
        List<QuestionResult> results = new ArrayList<>();
        for (QuizQuestion q : l.questions) {
            boolean ok = isCorrect(q, answers.get(q.id));
            if (ok) score++;
            results.add(new QuestionResult(q.id, ok, correctText(q), q.explanation));
        }
        int total = l.questions.size();
        int percent = score * 100 / total;
        boolean passed = percent >= (l.passPercent == null ? 70 : l.passPercent);
        QuizAttempt a = new QuizAttempt();
        a.user = ctx.user;
        a.lesson = l;
        a.score = score;
        a.total = total;
        a.passed = passed;
        attempts.save(a);
        if (passed) {
            LessonProgress p = progressFor(ctx);
            p.completed = true;
            p.updatedAt = Instant.now();
        }
        return new QuizResult(score, total, percent, passed, results);
    }

    static boolean isCorrect(QuizQuestion q, String answer) {
        if (answer == null || q.correct == null) return false;
        String a = answer.trim();
        return switch (q.type) {
            case SINGLE, TRUE_FALSE -> a.equals(q.correct.trim());
            case MULTI -> indexSet(a).equals(indexSet(q.correct));
            case FILL -> Arrays.stream(q.correct.split("\\|")).map(String::trim).anyMatch(x -> x.equalsIgnoreCase(a));
        };
    }

    private static Set<String> indexSet(String s) {
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toCollection(TreeSet::new));
    }

    private static String correctText(QuizQuestion q) {
        if (q.correct == null) return "";
        if (q.type == QuizQuestion.Type.FILL) return q.correct.split("\\|")[0].trim();
        List<String> opts = q.type == QuizQuestion.Type.TRUE_FALSE ? List.of("True", "False") : CatalogService.lines(q.options);
        return indexSet(q.correct).stream().map(i -> {
            try { return opts.get(Integer.parseInt(i)); } catch (Exception e) { return i; }
        }).collect(Collectors.joining(", "));
    }

    /** Issues a certificate only when every lesson is complete and every quiz is passed. */
    public CertificateDto claimCertificate(String slug, Long userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Please log in"));
        Course c = course(slug, u);
        if (!access.hasAccess(u, c)) throw ApiException.forbidden("Enroll in this course first");
        Optional<Certificate> existing = certificates.findByUser_IdAndCourse_Id(u.id, c.id);
        if (existing.isPresent()) return cert(existing.get());
        Map<Long, LessonProgress> pm = progressMap(u, c);
        for (Lesson l : c.allLessons()) {
            if (!pm.containsKey(l.id) || !pm.get(l.id).completed)
                throw ApiException.badRequest("Complete every lesson to earn your certificate (missing: " + l.title + ")");
            if (l.type == Lesson.Type.QUIZ && !l.questions.isEmpty() && !attempts.existsByUser_IdAndLesson_IdAndPassedTrue(u.id, l.id))
                throw ApiException.badRequest("Pass the quiz \"" + l.title + "\" to earn your certificate");
        }
        Certificate cert = new Certificate();
        cert.user = u;
        cert.course = c;
        cert.code = "CN-" + com.cloudnest.academy.security.Crypto.randomCode(10);
        certificates.save(cert);
        notifications.notify(u, "🎓 Certificate earned: " + c.title,
                "Congratulations! Your certificate ID is " + cert.code + ". Anyone can verify it on our website.",
                "/verify/" + cert.code, true);
        return cert(cert);
    }

    public static CertificateDto cert(Certificate c) {
        return new CertificateDto(c.code, c.user.name, c.course.title, c.course.slug, c.issuedAt);
    }

    @Transactional(readOnly = true)
    public List<MyCourse> myCourses(Long userId) {
        User u = users.findById(userId).orElseThrow();
        Map<Long, MyCourse> out = new LinkedHashMap<>();
        for (Enrollment e : enrollments.findByUser_IdAndActiveTrue(userId)) {
            if (e.expiresAt != null && e.expiresAt.isBefore(Instant.now())) continue;
            if (e.course.kind == Course.Kind.COURSE) out.put(e.course.id, myCourse(u, e.course, e.source.name(), e.expiresAt));
        }
        // Courses unlocked by an active membership
        for (Course c : courses.findByStatus(Course.Status.PUBLISHED)) {
            if (out.containsKey(c.id) || c.kind != Course.Kind.COURSE) continue;
            Subscription s = access.coveringSubscription(u, c);
            if (s != null) out.put(c.id, myCourse(u, c, "MEMBERSHIP", s.expiresAt));
        }
        return new ArrayList<>(out.values());
    }

    private MyCourse myCourse(User u, Course c, String accessType, Instant expires) {
        Map<Long, LessonProgress> pm = progressMap(u, c);
        List<Lesson> all = c.allLessons();
        int done = (int) all.stream().filter(l -> pm.containsKey(l.id) && pm.get(l.id).completed).count();
        Long resume = pm.values().stream().max(Comparator.comparing(p -> p.updatedAt)).map(p -> p.lesson.id)
                .orElse(all.isEmpty() ? null : all.get(0).id);
        String cert = certificates.findByUser_IdAndCourse_Id(u.id, c.id).map(x -> x.code).orElse(null);
        return new MyCourse(CatalogService.card(c), done, all.size(), resume, cert, accessType, expires);
    }

    @Transactional(readOnly = true)
    public List<BookmarkDto> bookmarks(Long userId) {
        return progress.findByUser_IdAndBookmarkedTrue(userId).stream()
                .map(p -> new BookmarkDto(p.lesson.id, p.lesson.title, p.lesson.section.course.slug, p.lesson.section.course.title))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizHistoryDto> quizHistory(Long userId) {
        return attempts.findByUser_IdOrderByIdDesc(userId).stream().limit(100)
                .map(a -> new QuizHistoryDto(a.lesson.section.course.title, a.lesson.title, a.score, a.total, a.passed, a.createdAt))
                .toList();
    }
}
