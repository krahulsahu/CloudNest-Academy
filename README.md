# CloudNest Academy — Online Technical Learning Platform

A commercial learning platform with a Cantrill-style public site, built with
**React + TypeScript (Vite)**, **Java 17+/Spring Boot 3.3**, **Spring Security (JWT)** and **PostgreSQL** (H2 for zero-setup local development).

```
cloudnest-academy/
├── backend/    Spring Boot REST API (port 8080)
├── frontend/   React + TypeScript web app (port 5173)
└── docker-compose.yml   optional: PostgreSQL + API + web
```

---

## 1. Run locally (no Docker needed)

**Requirements:** Java 17 or 21, Maven 3.9+, Node.js 18+.

### Terminal 1 – backend
```bash
cd backend
mvn spring-boot:run
```
The first start creates an embedded database in `backend/data/` and demo data.
Check: http://localhost:8080/api/courses

### Terminal 2 – frontend
```bash
cd frontend
npm install
npm run dev
```
Open **http://localhost:5173**

### Demo accounts
| Role | Email | Password |
|---|---|---|
| Admin | admin@cloudnest.local | Admin@12345 |
| Instructor | amar@cloudnest.local | Instructor@123 |
| Student | student@cloudnest.local | Student@123 |

Use **Login → "log in with a password"**, or log in with an email code.
In development the login code is shown on screen (and in the backend log) because no mail server is configured.

### Test the payment flow locally
Without Razorpay/Stripe keys the app runs in **development payment mode**: checkout shows a
"Simulate successful payment" button. The server still verifies the order and creates the enrollment.
Coupon codes: `LAUNCH50` (50% off), `FREE100` (100% off, 5 uses).

### Using PostgreSQL locally
```bash
createdb academy        # user/password academy/academy, or set DB_USER / DB_PASSWORD
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

### Corporate network / certificate errors
If `mvn` or `npm install` fails with `PKIX` / `x509 certificate` errors, your company proxy re-signs HTTPS traffic:
```bash
npm config set cafile /path/to/company-root-ca.pem
# Maven: import the CA into the JDK trust store
keytool -importcert -alias company-ca -file company-root-ca.pem -cacerts -storepass changeit
```

---

## 2. Features

**Public site** – Cantrill-style purple header, cloud hero, 3-column course cards with certification badges,
NEW UPDATE / -25% ribbons, course & bundle pages, Example Curriculum with PREVIEW/START, instructor section, FAQ,
reviews, related courses, Subscribe, Roadmap, Team Training, Contact, FAQ, Terms, Privacy, newsletter,
certificate verification, INR/USD switcher, WhatsApp chatbot.

**Catalog** – search, category chips (admin-managed), type, level, free/paid, sort (featured/popular/rating/newest/price), pagination.

**Accounts** – sign up, email-code login, password login, forgot/reset password, email verification,
JWT access tokens (15 min) + rotating refresh tokens with reuse detection, logout everywhere, rate limiting.

**Learning** – curriculum sidebar, HTML5 video (or YouTube/Vimeo) with speed control, captions, resume
from last position, Complete & Continue, progress bar, bookmarks, private auto-saved notes, text/lab lessons with
code blocks and copy buttons, downloadable resources, free previews, mobile curriculum drawer.

**Assessments** – multiple choice, multiple select, true/false, fill-in-the-blank; pass mark, attempt limits,
time limits, randomised order, explanations; certificates issued only when every lesson is complete and every quiz is passed;
PDF certificate + public verification page.

**Commerce** – courses, bundles, memberships (monthly/yearly, category/level scoped), free courses,
**bundle upgrade credit** for courses already owned, coupons (percent, expiry, max uses, per-course),
Razorpay (INR) and Stripe (USD) with server-side signature verification, signed webhooks with duplicate protection,
GST-inclusive PDF invoices, refunds (provider refund + access revoked), refund requests, renewal reminders,
expiry with grace period.

**Student dashboard** – my courses, continue learning, certificates, bookmarks, quiz history, orders & invoices,
subscriptions (cancel/renew), notification centre, support tickets, profile & password.

**Instructor portal** – create courses/bundles, details, **drag-and-drop curriculum** (sections & lessons, across sections),
video/caption/file uploads, quiz builder, pricing (INR/USD/compare-at), scheduling, SEO fields, submit for review,
students list with progress & quiz scores (CSV export), announcements (in-app + email), revenue and ratings.

**Admin** – KPIs with date range (students, active learners, retention, enrollments, completions, INR/USD and
subscription revenue, refunds), monthly revenue chart, most popular / most profitable courses, approve / reject /
unpublish / feature courses, users (search, role, disable, manual enrollment), orders (filter, refund, CSV),
coupons, membership plans, categories, support tickets with email replies, review moderation.

**Security** – BCrypt passwords, stateless JWT, role checks on every admin/instructor endpoint, course access
checked on the server for every lesson, HMAC-signed expiring media URLs with Range support, upload type whitelist,
constant-time signature checks, rate limiting, audit log (`AUDIT` logger), secrets from environment variables.

---

## 3. Configuration (environment variables)

See `.env.production.example`. The most important ones:

| Variable | Purpose |
|---|---|
| `JWT_SECRET`, `MEDIA_SECRET` | Long random secrets (required in production) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` + profile `postgres` | PostgreSQL |
| `MAIL_HOST`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP (e.g. Amazon SES) – needed to email login codes |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | Indian payments |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | International payments |
| `FRONTEND_URL` | Your domain, used for CORS, emails and Stripe redirects |
| Profile `prod` | Disables dev login codes and simulated payments |

**Webhooks to register**
- Razorpay → `https://your-domain/api/webhooks/razorpay` (events: `payment.captured`, `order.paid`, `payment.failed`)
- Stripe → `https://your-domain/api/webhooks/stripe` (events: `checkout.session.completed`, `checkout.session.expired`)

WhatsApp number and chatbot answers: `frontend/src/config.ts` and `frontend/src/components/WhatsAppWidget.tsx`.

---

## 4. Production on AWS (recommended)

```
Route 53 → CloudFront (+ AWS WAF) ─┬─ S3 bucket: frontend/dist  (static site)
                                   └─ /api/* → ALB → ECS Fargate (backend container)
                                                       ├─ RDS PostgreSQL (Multi-AZ, automated backups)
                                                       ├─ EFS or S3 for uploads
                                                       ├─ SES for email
                                                       └─ Secrets Manager for all secrets
CloudWatch logs/alarms · ACM certificate for HTTPS · GitHub Actions or CodePipeline for CI/CD
```
1. `cd frontend && npm run build` → upload `dist/` to S3, serve through CloudFront with SPA fallback to `index.html`.
2. Build `backend/Dockerfile`, push to ECR, run on ECS Fargate with `SPRING_PROFILES_ACTIVE=postgres,prod`.
3. Add a CloudFront behaviour for `/api/*` pointing to the ALB (disable caching, forward `Authorization`).
4. Health check path: `/actuator/health`.

`docker compose up --build` runs the whole stack locally with PostgreSQL at http://localhost:3000.

---

## 5. Before go-live checklist
- [ ] Set every secret; enable profile `prod` (turns off dev login codes and simulated payments)
- [ ] Configure SES (or another SMTP) – otherwise users cannot receive login codes
- [ ] Add Razorpay/Stripe live keys and register both webhooks; make a real ₹1 test purchase and refund it
- [ ] Replace `ddl-auto: update` with Flyway migrations
- [ ] Move video storage to S3 + CloudFront signed URLs (or Mux / Vimeo OTT) for large-scale streaming
- [ ] Replace the Terms and Privacy templates with legally reviewed text; confirm GST registration
- [ ] Change the admin password and the WhatsApp number
