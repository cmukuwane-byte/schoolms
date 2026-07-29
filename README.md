# School Management System

A web application for a Zimbabwean school running **Primary and Secondary sections plus a
boarding establishment** from one database. Built on the same stack you already deploy:
Spring Boot 3.3, Spring Data JPA, Thymeleaf, Spring Security, MS SQL Server, Maven, JDK 17.

---

## What is in the box

| Module | What it does |
|---|---|
| **Learners** | Enrolment with automatic admission numbers, class placement with capacity checks, mid-year class moves, transfers in and out with clearance gates, disciplinary cases through to expulsion, year-end promotion with repeaters |
| **Staff** | Profiles, qualifications, teacher registration numbers, subject competencies, class and subject allocation with weekly period counts, workload totals |
| **Classes and rooms** | Classes by phase and grade level, streams, class teachers, home rooms, specialist rooms, subject allocation |
| **Timetable** | Per-phase bell schedules, a class grid and a teacher grid, three-way clash detection (class, teacher, room), and an audit that compares placed lessons against allocated periods |
| **Attendance** | Daily learner registers by class and session, pre-marked present so teachers only tick exceptions; a staff sign-in book recording arrival and departure times with lateness and early-departure minutes calculated on save; absence follow-up lists |
| **Tests and exams** | Sittings per class and subject with paper totals and weightings, mark sheets with range validation, publish/reopen, live class statistics |
| **Report cards** | Weighted term averages per subject, grading against per-phase bands, dense-skip class ranking, attendance counts, teacher and head comments, release to guardians, print layout |
| **Library** | Catalogue of titles with accessioned physical copies, issue and return with borrower limits and overdue holds, renewals, fines, lost-copy write-offs, nightly overdue sweep |
| **Boarding** | Hostels, dormitories and beds with sex and capacity checks, exeat sign-out and return logging |
| **Access control** | Six roles — Admin, Head, Teacher, Guardian, Librarian, Matron — with role-based routing on sign-in and a guardian portal scoped to a parent's own children |

---

## Getting it running

### 1. Database

```bat
sqlcmd -S localhost -E -i db\01_create_database.sql
```

This creates `SchoolMS` and the `schoolms_app` login. **Change the password in that script
before you run it.**

### 2. Configure

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=SchoolMS;encrypt=true;trustServerCertificate=true
spring.datasource.username=schoolms_app
spring.datasource.password=<the password you set>

app.bootstrap.admin-username=admin
app.bootstrap.admin-password=<a strong first-run password>
```

Also worth setting, since lateness is measured against them:

```properties
app.staff.report-by=07:15
app.staff.knock-off=16:30
```

### 3. Build and run

```bat
mvn clean package
java -jar target\schoolms.jar
```

Then open `http://localhost:8080` and sign in with the bootstrap administrator. You are forced
to change the password on first sign-in.

### 4. Finish the schema

Hibernate creates the tables on first start (`ddl-auto=update`). Once that has happened, run the
scripts that add the guards Hibernate will not:

```bat
sqlcmd -S localhost -E -d SchoolMS -i db\02_constraints_and_indexes.sql
sqlcmd -S localhost -E -d SchoolMS -i db\03_reporting_views.sql
```

`02` adds the filtered unique indexes that make the timetable clash rules and the
one-open-loan-per-copy rule absolute at the database level, not just in Java.
`03` adds views for enrolment, daily attendance, staff punctuality, circulation and subject
performance — handy for Excel and Power BI.

Optionally, for something to click through:

```bat
sqlcmd -S localhost -E -d SchoolMS -i db\04_sample_data.sql
```

### 5. Once live, tighten the schema

When the tables have settled, switch to:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

and drop `db_ddladmin` from `schoolms_app`. From then on, schema changes go through scripts.

---

## Order of setup on a fresh install

The application seeds roles, the administrator, both grading scales, both bell schedules and a
calendar with three terms. After that, work in this order — later screens depend on earlier ones:

1. **Setup → Years and terms** — confirm the current year and term
2. **Classes → Rooms** — add classrooms and laboratories
3. **Classes → Subjects** — add subjects, per phase, with report-card display order
4. **Classes** — create each class with its grade level and class teacher
5. **Staff** — add teachers, ticking the subjects each can take
6. **Class → Allocate a subject** — assign teacher and periods per week
7. **Timetable** — place lessons; the audit banner shows what is still short
8. **Boarding** — hostels and dormitories, then allocate beds
9. **Library** — catalogue titles, then accession copies

---

## How some things work, so you can change them

**Admission numbers** — `StudentService.nextStudentNumber` produces `P/2026/0001` for primary
and `S/2026/0001` for secondary. If your school uses a different convention, that one method is
the only place to change.

**Grading** — `grade_band` rows, seeded per phase in `DataInitializer`. Primary gets a plain
A–E scale, secondary the ZIMSEC-style bands. Edit the table; no code change needed.

**Term mark weighting** — each `Assessment` carries a `weightPercent`. The term percentage per
subject is the weighted mean of published sittings, computed in
`AssessmentMarkRepository.termAveragesBySubject`. Three tests at 20 and an end-of-term paper at
40 comes to 100.

**Ranking** — dense-skip: two learners tied at 2nd are both 2nd, and the next is 4th.
See `ReportCardService.rank`.

**Library rules** — loan period, per-borrower limits and the daily fine all live in
`application.properties` under `app.library.*`.

**Report release** — a report cannot be released until the class teacher's comment is written.
Released reports are never overwritten by a regeneration, so late marks cannot silently change a
report a parent has already seen.

---

## Things worth knowing before you go live

- **Lombok** is used for getters and setters. In NetBeans 22 this needs annotation processing
  enabled (it is, by default, via the Maven plugin). If you prefer plain accessors, the
  `@Getter @Setter` annotations are the only thing to remove.
- **`ddl-auto=update` never drops anything.** If you rename a field, the old column stays behind.
  Plan to move to Flyway once the schema is stable.
- **Fees are not in here.** The transfer-out clearance has a `feesCleared` flag, but there is no
  billing module. That is the obvious next piece.
- **Report card PDFs** — OpenPDF is on the classpath and the report card screen has a print
  stylesheet that strips the navigation. If you want a server-generated PDF instead of browser
  print, that is a new service against `ReportCard`.
- **No SMS.** Guardian notifications go by email through `spring.mail.*`. In Zimbabwe you will
  probably want a bulk SMS gateway for absence alerts — the hook point is
  `AttendanceService.saveRegister`.
- **Backups.** The database is set to FULL recovery. Point AutoBackup Pro at `SchoolMS`.

---

## Project layout

```
schoolms/
├── pom.xml
├── db/
│   ├── 01_create_database.sql        database, login, permissions
│   ├── 02_constraints_and_indexes.sql clash guards, check constraints
│   ├── 03_reporting_views.sql        views for Excel and Power BI
│   └── 04_sample_data.sql            optional demonstration data
└── src/main/
    ├── java/zw/co/cresolzim/schoolms/
    │   ├── config/     security, user details, bootstrap seeding, converters
    │   ├── domain/     30 JPA entities and the shared enums
    │   ├── repo/       Spring Data repositories with the reporting queries
    │   ├── service/    the rules: enrolment, timetabling, attendance,
    │   │               marks, report cards, library, boarding
    │   └── web/        controllers, the guardian portal, error handling
    └── resources/
        ├── application.properties
        ├── static/css/app.css
        └── templates/  Thymeleaf views
```

The rules live in `service/`. Controllers do routing and binding only, and the repositories hold
the queries. If you are changing behaviour, `service/` is almost always the right file.
