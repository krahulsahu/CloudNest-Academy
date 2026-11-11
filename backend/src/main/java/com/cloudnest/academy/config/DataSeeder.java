package com.cloudnest.academy.config;

import com.cloudnest.academy.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/** Creates demo users, categories, courses, bundles, plans and coupons on first start (empty database only). */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String SAMPLE_VIDEO = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4";

    private final UserRepository users;
    private final CategoryRepository categories;
    private final CourseRepository courses;
    private final MembershipPlanRepository plans;
    private final CouponRepository coupons;
    private final PasswordEncoder encoder;
    private final AppConfig cfg;

    public DataSeeder(UserRepository users, CategoryRepository categories, CourseRepository courses,
                      MembershipPlanRepository plans, CouponRepository coupons, PasswordEncoder encoder, AppConfig cfg) {
        this.users = users;
        this.categories = categories;
        this.courses = courses;
        this.plans = plans;
        this.coupons = coupons;
        this.encoder = encoder;
        this.cfg = cfg;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.findByEmailIgnoreCase(cfg.adminEmail).isEmpty()) {
            user("Platform Admin", cfg.adminEmail, cfg.adminPassword, User.Role.ADMIN, null, null);
            log.warn("Created admin account {} - change the password after first login!", cfg.adminEmail);
        }
        if (!cfg.seedDemoData || courses.count() > 0) return;
        log.info("Seeding demo catalog...");

        for (String c : List.of("AWS", "Azure", "SailPoint", "Identity & Access", "Java", "DevOps", "Kubernetes", "AI", "Cloud Security", "Architecture"))
            category(c);

        User amar = user("Amar Kumar", "amar@cloudnest.local", "Instructor@123", User.Role.INSTRUCTOR,
                "Senior Software Engineer & IAM Trainer",
                "Amar is a senior software engineer and identity specialist with more than a decade of experience building enterprise platforms with Java, Spring Boot, AWS and identity governance tools.\n"
                        + "He focuses on the why behind every service so students build real understanding instead of memorising facts. Every course mixes theory with practical demonstrations that mirror real projects.");
        User priya = user("Priya Nair", "priya@cloudnest.local", "Instructor@123", User.Role.INSTRUCTOR,
                "Cloud Architect",
                "Priya is a cloud architect who has designed multi-account AWS and Azure environments for large organisations.\n"
                        + "She specialises in architecture, networking and security, and turns complex diagrams into simple, memorable explanations.");
        User rahul = user("Rahul Verma", "rahul@cloudnest.local", "Instructor@123", User.Role.INSTRUCTOR,
                "DevOps & Platform Engineer",
                "Rahul is a DevOps and platform engineer working with Kubernetes, CI/CD and observability at scale.\n"
                        + "His courses are built around hands-on demos that you can repeat in your own accounts.");
        user("Demo Student", "student@cloudnest.local", "Student@123", User.Role.STUDENT, null, null);

        Course saa = course("aws-solutions-architect-associate", "AWS Solutions Architect - Associate (SAA-C03)",
                "Become a Solutions Architect and confidently pass the SAA-C03 exam.", "AWS", "Intermediate", 3299, 40, priya,
                "Solutions|Architect|ASSOCIATE|blue", null, true, false);
        curriculum(saa, saaCurriculum());
        Course sap = course("aws-solutions-architect-professional", "AWS Solutions Architect - Professional",
                "Become a Professional Solutions Architect and confidently pass the SAP-C02 exam.", "AWS", "Advanced", 6599, 80, priya,
                "Solutions|Architect|PROFESSIONAL|teal", null, true, true);
        curriculum(sap, generic("Solutions Architect Professional"));
        Course dva = course("aws-developer-associate", "AWS Developer - Associate",
                "Learn real developer skills and pass the DVA-C02 exam.", "AWS", "Intermediate", 3299, 40, amar,
                "Developer||ASSOCIATE|blue", null, true, true);
        curriculum(dva, generic("Developer Associate"));
        Course ops = course("aws-cloudops-associate", "AWS CloudOps Engineer - Associate",
                "Operate, monitor and automate workloads and pass the CloudOps exam.", "AWS", "Intermediate", 3299, 40, rahul,
                "CloudOps|Engineer|ASSOCIATE|blue", null, true, false);
        curriculum(ops, generic("CloudOps Engineer"));
        Course fun = course("tech-fundamentals", "Tech Fundamentals (Free)",
                "Networking, DNS, encryption and Linux basics every cloud engineer needs.", "AWS", "Beginner", 0, 0, amar,
                "Tech|Fundamentals|FREE|green", null, true, false);
        curriculum(fun, generic("Tech Fundamentals"));
        Course isc = course("sailpoint-isc-complete", "SailPoint ISC Complete Training",
                "Identity profiles, sources, correlation, provisioning and lifecycle states.", "SailPoint", "Beginner", 4999, 60, amar,
                "SailPoint|ISC|IDENTITY SECURITY|navy", null, true, true);
        isc.compareAtInr = 6699;
        curriculum(isc, sailpointCurriculum());
        Course iga = course("iam-iga-fundamentals", "IAM & IGA Fundamentals",
                "Understand identity, SSO, MFA, governance and access reviews from scratch.", "Identity & Access", "Beginner", 1999, 25, amar,
                "IAM|& IGA|FOUNDATION|navy", null, true, false);
        curriculum(iga, generic("IAM & IGA"));
        Course java = course("java-spring-microservices", "Java Spring Boot Microservices",
                "Build production microservices with Spring Boot 3, security and observability.", "Java", "Advanced", 3999, 50, amar,
                "Spring|Boot 3|MICROSERVICES|green", null, true, false);
        curriculum(java, generic("Spring Boot Microservices"));
        Course k8s = course("kubernetes-devops", "Kubernetes & DevOps Engineer",
                "Docker, Kubernetes, CI/CD, GitOps and monitoring with hands-on labs.", "Kubernetes", "Intermediate", 3999, 50, rahul,
                "Kubernetes||DEVOPS|blue", null, true, false);
        curriculum(k8s, generic("Kubernetes & DevOps"));
        Course az104 = course("azure-administrator-az104", "Microsoft Azure Administrator (AZ-104)",
                "Gain the skills required to become an Azure Administrator, and pass the AZ-104 exam.", "Azure", "Intermediate", 3299, 40, priya,
                "Azure|Administrator|ASSOCIATE|azure", "dark", true, false);
        curriculum(az104, generic("Azure Administrator"));
        Course az400 = course("azure-devops-az400", "Microsoft DevOps Engineer (AZ-400)",
                "Gain knowledge and skills with DevSecOps, and pass the AZ-400 exam.", "Azure", "Advanced", 6599, 80, rahul,
                "DevOps|Engineer|EXPERT|azure", "dark", true, false);
        curriculum(az400, generic("Azure DevOps Engineer"));
        Course ai = course("ai-engineering", "AI Engineering Foundations",
                "Python, LLM apps, RAG, evaluation and responsible AI for engineers.", "AI", "Beginner", 2999, 35, amar,
                "AI|Engineering|FOUNDATION|green", null, false, true);
        curriculum(ai, generic("AI Engineering"));

        bundle("aws-solutions-architect-bundle", "AWS Solutions Architect Bundle", "Solutions Architect Associate and Professional courses.",
                "AWS", 8299, 100, "Solutions|Architect|ASSOCIATE|blue;Solutions|Architect|PROFESSIONAL|teal", null, true, saa, sap, fun);
        bundle("aws-core-associate-bundle", "AWS Core Associate Bundle", "Solutions Architect, Developer and CloudOps - Associate.",
                "AWS", 8299, 100, "CloudOps||ASSOC|blue;Architect||ASSOC|blue;Developer||ASSOC|blue", null, true, saa, dva, ops, fun);
        bundle("sailpoint-isc-complete-bundle", "SailPoint ISC Complete Bundle", "IAM & IGA Fundamentals plus SailPoint ISC Complete Training.",
                "SailPoint", 5999, 75, "IAM||FOUNDATION|navy;SailPoint|ISC|COMPLETE|navy", null, true, iga, isc);
        bundle("aws-devops-bundle", "AWS DevOps Bundle", "Developer and CloudOps Associate ... plus Kubernetes & DevOps.",
                "DevOps", 10799, 130, "DevOps|Engineer|PROFESSIONAL|teal", null, true, dva, ops, k8s, fun);
        bundle("azure-devops-expert-bundle", "Microsoft DevOps Expert Bundle", "Includes AZ-104 Azure Administrator and AZ-400 DevOps Expert.",
                "Azure", 8299, 100, "Azure|Admin|ASSOC|azure;DevOps||EXPERT|azure", "dark", true, az104, az400);
        bundle("all-the-things", "All The Things Bundle", "Every current course in the academy at one price.",
                "AWS", 24999, 300, "ALL|Courses|LIFETIME|gold", null, false, saa, sap, dva, ops, fun, isc, iga, java, k8s, az104, az400, ai);

        plan("BASIC", "Basic", "Every beginner course, all categories.", 499, 9, 30, null, "Beginner");
        plan("PRO_AWS", "Professional - AWS", "Every AWS course, all levels.", 999, 15, 30, "AWS", null);
        plan("PREMIUM", "Premium (yearly)", "Every course in the academy, including new releases.", 9999, 129, 365, null, null);

        Coupon c = new Coupon();
        c.code = "LAUNCH50";
        c.percentOff = 50;
        coupons.save(c);
        Coupon f = new Coupon();
        f.code = "FREE100";
        f.percentOff = 100;
        f.maxUses = 5;
        coupons.save(f);
        log.info("Demo data ready. Admin: {} / {}  Instructor: amar@cloudnest.local / Instructor@123  Student: student@cloudnest.local / Student@123",
                cfg.adminEmail, cfg.adminPassword);
    }

    private void category(String name) {
        Category c = new Category();
        c.name = name;
        categories.save(c);
    }

    private User user(String name, String email, String password, User.Role role, String headline, String bio) {
        User u = new User();
        u.name = name;
        u.email = email;
        u.passwordHash = encoder.encode(password);
        u.role = role;
        u.emailVerified = true;
        u.headline = headline;
        u.bio = bio;
        u.country = "IN";
        return users.save(u);
    }

    private Course course(String slug, String title, String subtitle, String category, String level, int inr, int usd,
                          User instructor, String badges, String theme, boolean featured, boolean isNew) {
        Course c = new Course();
        c.slug = slug;
        c.title = title;
        c.subtitle = subtitle;
        c.category = category;
        c.level = level;
        c.priceInr = inr;
        c.priceUsd = usd;
        c.instructor = instructor;
        c.badges = badges;
        c.theme = theme;
        c.featured = featured;
        c.newBadge = isNew;
        c.status = Course.Status.PUBLISHED;
        c.publishedAt = Instant.now();
        c.description = title + " is designed to give you a deep, practical understanding of the subject - not just enough to pass an exam. "
                + "Every topic is explained visually with architecture diagrams and then reinforced with DEMO lessons in your own environment.\n\n"
                + "Getting certified can improve your confidence and get you interviews, but it's the skills gained along the way which matter long term. "
                + "You'll learn everything required for the exam and the real-world skills to perform well in interviews and on the job.";
        c.objectives = "Design secure, resilient and cost-optimised solutions\nUnderstand how each service works under the hood\nComplete hands-on DEMO lessons in your own account\nTroubleshoot realistic production scenarios\nPrepare confidently for the certification exam";
        c.prerequisites = "Basic IT knowledge (networking, operating systems)\nA laptop and a free-tier cloud account for the demos";
        c.audience = "Engineers, developers, administrators and students who want job-ready " + category + " skills.";
        c.seoDescription = subtitle;
        return courses.save(c);
    }

    private void bundle(String slug, String title, String subtitle, String category, int inr, int usd, String badges,
                        String theme, boolean featured, Course... items) {
        Course b = course(slug, title, subtitle, category, "Intermediate", inr, usd, null, badges, theme, featured, false);
        b.kind = Course.Kind.BUNDLE;
        b.bundleItems.addAll(Arrays.asList(items));
        b.description = "Get every course in this bundle with lifetime access and all future updates. "
                + "If you already own some of the included courses, their price is credited at checkout.";
    }

    private void plan(String code, String name, String desc, int inr, int usd, int days, String category, String level) {
        MembershipPlan p = new MembershipPlan();
        p.code = code;
        p.name = name;
        p.description = desc;
        p.priceInr = inr;
        p.priceUsd = usd;
        p.periodDays = days;
        p.category = category;
        p.level = level;
        plans.save(p);
    }

    /** Section spec: title, then lessons as "type|title|seconds|preview". */
    private record S(String title, String... lessons) {}

    private void curriculum(Course c, List<S> spec) {
        int sp = 0;
        for (S s : spec) {
            CourseSection sec = new CourseSection();
            sec.course = c;
            sec.title = s.title();
            sec.position = sp++;
            int lp = 0;
            for (String l : s.lessons()) {
                String[] p = l.split("\\|");
                Lesson les = new Lesson();
                les.section = sec;
                les.type = Lesson.Type.valueOf(p[0]);
                les.title = p[1];
                les.durationSeconds = Integer.parseInt(p[2]);
                les.preview = p.length > 3 && p[3].equals("preview");
                les.position = lp++;
                if (les.type == Lesson.Type.VIDEO) {
                    les.videoUrl = SAMPLE_VIDEO;
                    les.content = "In this lesson we cover " + les.title + ".\n\nLearning objectives:\n- Understand the concept\n- See it working in a demo\n- Know the exam-relevant details";
                    les.resources = "Lesson slides (PDF)|https://example.com/slides.pdf\nGitHub lab repository|https://github.com/";
                } else if (les.type == Lesson.Type.TEXT) {
                    les.content = "Step-by-step lab instructions\n\n1. Sign in to your account.\n2. Create the resources described below.\n\n```bash\naws s3 mb s3://my-first-bucket-$RANDOM\naws s3 ls\n```\n\nExpected output: your new bucket is listed.\n\nTroubleshooting: if you see AccessDenied, check that your IAM user has the AdministratorAccess policy.";
                    les.resources = "Lab files (ZIP)|https://github.com/";
                } else {
                    les.passPercent = 60;
                    les.maxAttempts = 5;
                    les.timeLimitMinutes = 10;
                    question(les, 0, QuizQuestion.Type.SINGLE, "Which option gives an application on EC2 temporary AWS credentials?",
                            "Access keys stored on the instance\nAn IAM role attached through an instance profile\nThe root user credentials\nA shared password", "1",
                            "IAM roles provide automatically rotated temporary credentials; never store long-term keys on instances.");
                    question(les, 1, QuizQuestion.Type.MULTI, "Which of these are characteristics of cloud computing? (choose two)",
                            "On-demand self-service\nLong-term hardware contracts\nRapid elasticity\nManual provisioning", "0,2",
                            "NIST defines on-demand self-service and rapid elasticity as essential characteristics.");
                    question(les, 2, QuizQuestion.Type.TRUE_FALSE, "A VPC can span multiple AWS regions.", "", "1",
                            "A VPC is a regional construct; it spans the availability zones of one region.");
                    question(les, 3, QuizQuestion.Type.FILL, "The SailPoint ISC component installed in your network to connect on-premises sources is the Virtual ______.",
                            "", "Appliance|VA", "The Virtual Appliance (VA) runs connectors close to on-premises sources.");
                }
                sec.lessons.add(les);
            }
            c.sections.add(sec);
        }
        c.recount();
    }

    private static void question(Lesson l, int pos, QuizQuestion.Type type, String prompt, String options, String correct, String explanation) {
        QuizQuestion q = new QuizQuestion();
        q.lesson = l;
        q.position = pos;
        q.type = type;
        q.prompt = prompt;
        q.options = options;
        q.correct = correct;
        q.explanation = explanation;
        l.questions.add(q);
    }

    private static List<S> saaCurriculum() {
        return List.of(
                new S("Introduction", "VIDEO|Public Introduction|152|preview", "VIDEO|Certification and Exams|1144", "VIDEO|Connect with other students and your instructor|128", "TEXT|Shared lessons explained|0"),
                new S("Course Fundamentals and Account Setup", "VIDEO|Creating your accounts|760|preview", "VIDEO|Multi-factor authentication (MFA)|555", "VIDEO|Securing the root user [DEMO]|862", "VIDEO|Budgets and billing alarms [DEMO]|605"),
                new S("Identity and Access Management", "VIDEO|IAM identity policies|970", "VIDEO|IAM users and ARNs|782", "VIDEO|IAM roles - the tech|767", "TEXT|Permissions evaluation lab|0"),
                new S("Networking (VPC)", "VIDEO|VPC sizing and structure|1116", "VIDEO|Custom VPCs [DEMO]|940", "VIDEO|NAT and NAT gateway|891"),
                new S("Practice Exam", "QUIZ|Section quiz|0"));
    }

    private static List<S> sailpointCurriculum() {
        return List.of(
                new S("Introduction", "VIDEO|Welcome and course roadmap|250|preview", "VIDEO|What is IAM vs IGA?|750|preview"),
                new S("Core Concepts with Riya's Story", "VIDEO|Identity, Account and Entitlement|860", "VIDEO|Authoritative vs non-authoritative sources|665", "VIDEO|Access profiles and roles|824"),
                new S("Sources and Connectors", "VIDEO|Virtual Appliance (VA) explained|902", "TEXT|Delimited file source lab|0", "VIDEO|Aggregation types|645"),
                new S("Identity Profiles and Lifecycle", "VIDEO|Identity profile and attribute mapping|980", "VIDEO|Correlation [DEMO]|732", "VIDEO|Lifecycle states: joiner, mover, leaver|1060", "VIDEO|Transforms vs Rules|840"),
                new S("Assessment", "QUIZ|Final assessment|0"));
    }

    private static List<S> generic(String topic) {
        return List.of(
                new S("Introduction", "VIDEO|Welcome to " + topic + "|200|preview", "VIDEO|Course structure and exam guide|670"),
                new S("Fundamentals", "VIDEO|Core concepts|930", "VIDEO|Architecture deep dive|1145", "VIDEO|Hands-on setup [DEMO]|1360"),
                new S("Advanced Topics", "VIDEO|Security and best practice|1005", "VIDEO|Troubleshooting scenarios|1100", "TEXT|Lab files and resources|0"),
                new S("Practice Exam", "QUIZ|Practice test|0"));
    }
}
