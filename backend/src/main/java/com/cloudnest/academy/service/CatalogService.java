package com.cloudnest.academy.service;

import com.cloudnest.academy.config.ApiException;
import com.cloudnest.academy.domain.*;
import com.cloudnest.academy.web.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final CourseRepository courses;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final LessonProgressRepository progress;
    private final AccessService access;

    public CatalogService(CourseRepository courses, ReviewRepository reviews, UserRepository users,
                          LessonProgressRepository progress, AccessService access) {
        this.courses = courses;
        this.reviews = reviews;
        this.users = users;
        this.progress = progress;
        this.access = access;
    }

    public static CourseCard card(Course c) {
        double rating = c.ratingCount == 0 ? 0 : Math.round(10.0 * c.ratingSum / c.ratingCount) / 10.0;
        int lessons = c.lessonCount, secs = c.totalSeconds;
        if (c.kind == Course.Kind.BUNDLE) {
            lessons = 0;
            secs = 0;
            for (Course i : c.bundleItems) { lessons += i.lessonCount; secs += i.totalSeconds; }
        }
        return new CourseCard(c.id, c.slug, c.title, c.subtitle, c.kind.name(), c.category, c.level, c.priceInr,
                c.priceUsd, c.compareAtInr, c.featured, c.newBadge, c.badges, c.theme, c.thumbnailUrl,
                c.instructor != null ? c.instructor.name : null, c.bundleItems.size(), lessons, secs, rating,
                c.ratingCount, c.status.name());
    }

    public static List<String> lines(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split("\\r?\\n")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    public static ReviewDto review(Review r) {
        return new ReviewDto(r.id, r.user.name, r.rating, r.comment, r.createdAt, r.course.title, r.visible);
    }

    public PageDto<CourseCard> search(String q, String category, String kind, String level, String price,
                                      String sort, boolean featuredOnly, int page, int size) {
        String term = q == null ? "" : q.trim().toLowerCase();
        List<Course> list = courses.findByStatus(Course.Status.PUBLISHED).stream()
                .filter(c -> !featuredOnly || c.featured)
                .filter(c -> category == null || category.isBlank() || category.equalsIgnoreCase(c.category))
                .filter(c -> kind == null || kind.isBlank() || kind.equalsIgnoreCase(c.kind.name()))
                .filter(c -> level == null || level.isBlank() || level.equalsIgnoreCase(c.level))
                .filter(c -> price == null || price.isBlank() || ("free".equals(price) ? c.priceInr == 0 : c.priceInr > 0))
                .filter(c -> term.isEmpty() || (c.title + " " + Objects.toString(c.subtitle, "") + " " + c.category + " "
                        + Objects.toString(c.description, "")).toLowerCase().contains(term))
                .collect(Collectors.toCollection(ArrayList::new));
        Comparator<Course> cmp = switch (sort == null ? "" : sort) {
            case "newest" -> Comparator.comparing((Course c) -> c.publishedAt == null ? c.createdAt : c.publishedAt).reversed();
            case "price_asc" -> Comparator.comparingInt(c -> c.priceInr);
            case "price_desc" -> Comparator.comparingInt((Course c) -> c.priceInr).reversed();
            case "rating" -> Comparator.comparingDouble((Course c) -> c.ratingCount == 0 ? 0 : (double) c.ratingSum / c.ratingCount).reversed();
            case "popular" -> Comparator.comparingInt((Course c) -> c.ratingCount).reversed();
            default -> Comparator.comparing((Course c) -> !c.featured).thenComparing(c -> c.id);
        };
        list.sort(cmp);
        int s = Math.max(1, Math.min(size, 60));
        int from = Math.min(Math.max(page, 0) * s, list.size());
        List<CourseCard> items = list.subList(from, Math.min(from + s, list.size())).stream().map(CatalogService::card).toList();
        return new PageDto<>(items, list.size(), page, s);
    }

    public CourseDetail detail(String slug, Long userId) {
        Course c = courses.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Course not found"));
        User u = userId == null ? null : users.findById(userId).orElse(null);
        boolean manage = access.canManage(u, c);
        if (c.status != Course.Status.PUBLISHED && !manage) throw ApiException.notFound("Course not found");
        boolean hasAccess = access.hasAccess(u, c);
        Set<Long> done = u == null ? Set.of() : progress.findByUser_IdAndCourseId(u.id, c.id).stream()
                .filter(p -> p.completed).map(p -> p.lesson.id).collect(Collectors.toSet());
        List<SectionDto> sections = c.sections.stream().map(s -> new SectionDto(s.id, s.title, s.lessons.stream()
                .map(l -> new LessonSummary(l.id, l.title, l.type.name(), l.durationSeconds, l.preview, done.contains(l.id)))
                .toList())).toList();
        List<CourseCard> items = c.bundleItems.stream().filter(i -> i.status == Course.Status.PUBLISHED).map(CatalogService::card).toList();
        List<CourseCard> inBundles = courses.findBundlesContaining(c.id, Course.Status.PUBLISHED).stream().map(CatalogService::card).toList();
        List<CourseCard> related = courses.findByStatus(Course.Status.PUBLISHED).stream()
                .filter(x -> !x.id.equals(c.id) && Objects.equals(x.category, c.category) && x.kind == Course.Kind.COURSE)
                .limit(3).map(CatalogService::card).toList();
        List<ReviewDto> revs = reviews.findTop50ByCourse_IdAndVisibleTrueOrderByIdDesc(c.id).stream().map(CatalogService::review).toList();
        boolean canReview = u != null && hasAccess && c.kind == Course.Kind.COURSE && !manage;
        InstructorDto ins = c.instructor == null ? null : new InstructorDto(c.instructor.id, c.instructor.name, c.instructor.headline, c.instructor.bio);
        return new CourseDetail(card(c), c.description, lines(c.objectives), lines(c.prerequisites), c.audience,
                c.previewVideoUrl, ins, sections, items, inBundles, related, hasAccess, revs, canReview,
                c.seoTitle != null ? c.seoTitle : c.title, c.seoDescription != null ? c.seoDescription : c.subtitle);
    }
}
