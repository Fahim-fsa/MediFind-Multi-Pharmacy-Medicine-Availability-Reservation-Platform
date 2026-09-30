# MediFind

**Multi-Pharmacy Medicine Availability & Reservation Platform**
A university Software Engineering course project.

MediFind lets a patient search for a medicine, compare price/stock/distance across nearby pharmacies, and reserve it — with a confirmation code to collect it in person. Pharmacies manage their own inventory and incoming reservations; admins verify new pharmacies, moderate the platform, and run reports.

---

## 1. Tech stack

| Layer          | Technology                                                        |
|-----------------|--------------------------------------------------------------------|
| Language        | Java 17                                                             |
| Framework       | Spring Boot **3.5.15** (Spring MVC, Spring Data JPA, Spring Security) |
| Templating      | Thymeleaf (server-rendered HTML — no separate frontend build step) |
| Database        | MySQL 8.x, accessed through Hibernate/JPA                          |
| Maps            | Leaflet.js + OpenStreetMap (no API key required)                   |
| PDF export      | OpenPDF 3.0.3                                                       |
| Build tool      | Maven                                                               |
| Testing         | JUnit 5, Mockito, AssertJ (bundled in `spring-boot-starter-test`), H2 for a dependency-free test run |

**A note on the Spring Boot version:** Spring Boot 4.x is the current major line, but it shipped very close to (and its later `4.1` line after) this project being put together, in the same window as a framework-wide jump (Spring Framework 7, Jakarta EE 11). Rather than guess at API details for a brand-new major version across ~90 files with no way to compile-check them here, this project deliberately targets the last Spring Boot 3.x line (3.5.15) — extremely well-documented, still what most Spring courses and tutorials use, and a straightforward upgrade path later if your team wants to move to Boot 4.

Architecture is a classic **3-tier layered monolith** — Controller → Service → Repository — with no microservices, exactly as the project brief asks for:

```
com.medifind
 ├── controller   → handles HTTP requests, returns a Thymeleaf view name (or JSON, for ApiController)
 ├── service      → business logic and transactions
 ├── repository   → Spring Data JPA interfaces (no hand-written SQL except two native queries)
 ├── entity       → JPA-mapped database tables
 ├── dto          → request/response shapes distinct from the database entities
 ├── security     → Spring Security integration (CustomUserDetails, CustomUserDetailsService)
 ├── config       → SecurityConfig, scheduling, demo-data seeding
 ├── exception    → custom exceptions + a single centralized error handler
 ├── enums        → Role, ReservationStatus, PaymentStatus, etc.
 └── util         → small stateless helpers (distance calculation, code generation, PDF building)
```

---

## 2. Prerequisites

- **Java 17 or newer** (Java 21 LTS also works) — check with `java -version`
- **Maven 3.9+** — check with `mvn -version` (or use the included `./mvnw` wrapper if you add one — this project assumes a system Maven install)
- **MySQL 8.x** server running locally (or reachable over the network)
- An internet connection the first time you build, so Maven can download dependencies

---

## 3. Getting started

### 3.1 Create the database

You only need to create an **empty** database — Hibernate creates every table for you the first time the app starts (see §5, "Database setup" below for how this works).

```sql
CREATE DATABASE medifind_db CHARACTER SET utf8mb4;
```

### 3.2 Configure the connection

Open `src/main/resources/application-dev.properties` and adjust these three lines if your MySQL username/password/host differ from the defaults (`root` / `root` / `localhost:3306`):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/medifind_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
```

### 3.3 Run it

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**. The first startup automatically creates every table and seeds demo data (see §4) — this can take a few extra seconds the very first time.

To build a standalone jar instead:

```bash
mvn clean package
java -jar target/medifind.jar
```

### 3.4 Run the tests

```bash
mvn test
```

Tests run against an in-memory H2 database (see `src/test/resources/application-test.properties`), so this works immediately — you don't need MySQL running just to run `mvn test`.

---

## 4. Demo accounts

The app seeds itself with demo data on first run (see `DataSeeder.java`) so you can explore every role immediately:

| Role                              | Email                        | Password      | Notes |
|------------------------------------|-------------------------------|----------------|-------|
| Admin                               | `admin@medifind.com`         | `Admin@12345`  | Sign in at **/auth/admin-login** (not the regular login page) |
| Pharmacist — Lazz Pharma (Dhanmondi) | `pharmacist1@medifind.com`   | `Pharma@12345` | Already verified/approved |
| Pharmacist — Well Care (Gulshan)    | `pharmacist2@medifind.com`   | `Pharma@12345` | Already verified/approved |
| Pharmacist — Popular Pharmacy (Mirpur) | `pharmacist3@medifind.com` | `Pharma@12345` | Deliberately left **pending**, so you have something to approve/reject in the Admin Portal |
| Patient                             | *(register your own)*        | —              | Go to **/auth/register/patient** |

The admin login is a **two-step** sign-in: after your password, MediFind "emails" a 6-digit one-time code. Since no real mail server is configured (see §6), that code is printed to the **application console/log** — look for a block that says `[SIMULATED EMAIL]` right after you submit the admin password form.

---

## 5. Database setup — two ways

**Option A — the default, easiest way.** Leave `spring.jpa.hibernate.ddl-auto=update` as-is in `application-dev.properties`. Hibernate reads every `@Entity` class and creates/updates the matching tables automatically. Nothing to run by hand.

**Option B — manual/reviewable schema.** `database/schema.sql` contains the same schema as plain, documented SQL (handy for cross-checking against an ER diagram, or if you'd rather set the tables up yourself):

```bash
mysql -u root -p medifind_db < database/schema.sql
```

If you use Option B, you'll probably also want to switch `ddl-auto` to `validate` (Hibernate checks the schema matches instead of altering it) — this is exactly what `application-prod.properties` already does.

Demo data (the admin account, sample pharmacies, medicines, and stock) is **only** seeded through `DataSeeder.java`, not through a SQL script — passwords need to be BCrypt-hashed, which isn't something a plain `.sql` file can do safely. `DataSeeder` checks whether each table is empty before inserting, so it's safe to leave running: nothing is re-seeded on your second, third, or hundredth restart.

---

## 6. Where "email" and "SMS" go — read this before you go looking for a mail server

The brief scopes notifications as **"email notification simulation"** for this MVP — MediFind does not hold real SMTP or SMS provider credentials, and none is required to run the app. Every place the app would send an email (reservation confirmation, password reset link, admin MFA code, low-stock alert, ...) instead:

1. Saves a row in the `notifications` table (visible in-app under each role's **Notifications** page), and
2. Logs a clearly-marked `[SIMULATED EMAIL]` block to the application console.

`EmailSimulationService.java` is the one place this happens — swapping in a real provider later (SMTP via `JavaMailSender`, or an SDK like SendGrid/Twilio) only means changing what happens inside that one method.

---

## 7. Feature tour (what's implemented, and where)

- **Patients** — register/sign in, search medicine by name/generic/brand, filter by distance (browser geolocation, optional), see a Leaflet map of every verified pharmacy, reserve a medicine (cash or "online"), see a confirmation code, view/filter reservation history, cancel a reservation before pickup, edit their profile.
- **Pharmacies** — register a new branch (goes to an admin verification queue) or join an existing branch as a second staff login, manage their own inventory (add/edit stock, price, availability, and an optional photo when first adding a medicine to the shared catalogue), see incoming reservations in a live-refreshing queue, mark a reservation collected or reject it, edit their pharmacy profile.
- **Admins** — a two-step (password + emailed code) sign-in, approve/reject pharmacies, suspend/reactivate patient accounts, full CRUD over pharmacy listings (add one directly, edit its details, or delete it — blocked if it has reservation history to protect), browse every pharmacy's inventory and remove an inappropriate listing, view/cancel a disputed reservation, generate four reports (daily reservations, most-searched medicines, active pharmacies, active patients) each exportable as CSV or PDF, a full audit log of every sensitive admin action, and a small runtime-configurable settings page (pickup window length, low-stock threshold, notification wording).

A full, story-by-story map of the Jira backlog to the exact files that implement each one lives in **[`docs/SLP_TRACEABILITY.md`](docs/SLP_TRACEABILITY.md)**.

### A couple of deliberate design calls worth knowing about

- **Stock is held the moment a reservation is placed, not when it's collected.** The brief's Pharmacy Dashboard section says stock should "decrease automatically" on collection — this project decrements it immediately on reservation instead (and restores it on cancel/expiry). Decrementing only at collection would let the same last unit be reserved by more people than the pharmacy actually has, since nothing would reflect a pending reservation in the meantime. See the Javadoc on `ReservationService` for the full reasoning.
- **"SLP" tags in code comments and in the traceability doc refer to the Jira Epic/Story from the uploaded `MediFind_Jira__Stories.csv`** (12 epics, 78 stories) — that CSV didn't have a column literally called "SLP", so each comment cites the closest matching Epic + Story text from that sheet. If "SLP" meant something more specific in your course materials, the mapping is easy to re-label since every tag already names the exact story it refers to.
- **Admin sign-in doesn't use Spring Security's built-in login form** — it's a custom two-step flow (see `AuthService` and `SecurityConfig`'s class comments) since a one-shot login can't express a "pause in the middle for a mailed code" step.
- A few small, low-cost additions beyond the minimum brief: optimistic locking on stock rows (prevents two simultaneous edits from silently overwriting each other), login rate-limiting/lockout for every role, a runtime-editable settings table instead of hard-coded numbers, and Spring Boot Actuator's `/actuator/health` endpoint for basic monitoring.

---

## 8. Project layout

```
medifind/
├── pom.xml
├── README.md                      ← you are here
├── database/
│   └── schema.sql                 ← reference SQL
├── scripts/
│   ├── backup.sh / restore.sh     ← mysqldump wrappers
├── docs/
│   └── SLP_TRACEABILITY.md        ← Jira story → code file map
├── src/main/java/com/medifind/    ← application code 
├── src/main/resources/
│   ├── application*.properties    ← base / dev / prod config
│   ├── static/{css,js}            ← stylesheet + vanilla JS (theme toggle, map, search, polling)
│   └── templates/                 ← Thymeleaf pages, organised by role (auth/, patient/, pharmacist/, admin/)
└── src/test/java/com/medifind/    ← unit + smoke tests
```

---

## 9. Troubleshooting

- **`Communications link failure` / can't connect to MySQL** — make sure MySQL is running and the credentials in `application-dev.properties` are correct.
- **`Unknown database 'medifind_db'`** — the connection URL includes `createDatabaseIfNotExist=true`, so this shouldn't happen with a fresh MySQL user that has `CREATE` privileges; if it does, create the database manually.
- **Port 8080 already in use** — change `server.port` in `application-dev.properties`.
- **Can't find the admin MFA code** — check the console/terminal where you ran `mvn spring-boot:run`, not your email.
- **Maps not loading** — the map page loads Leaflet.js from a CDN; it needs outbound internet access from your browser.

---

## 10. Academic note

This project was developed to closely follow the team's own Software Requirements Specification and Jira backlog. Comments throughout the code reference the specific backlog Epic/Story each part implements — use `docs/SLP_TRACEABILITY.md` as the starting point when presenting or extending this project.
