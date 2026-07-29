package zw.co.cresolzim.schoolms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.Phase;
import zw.co.cresolzim.schoolms.repo.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Puts the minimum in place on an empty database: roles, one administrator,
 * the grading scales, and a bell schedule for each phase. It does nothing
 * once those rows exist, so it is safe to leave switched on in production.
 */
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roles;
    private final AppUserRepository users;
    private final GradeBandRepository bands;
    private final BellPeriodRepository periods;
    private final AcademicYearRepository years;
    private final TermRepository terms;
    private final PasswordEncoder encoder;

    @Value("${app.bootstrap.admin-username}") private String adminUsername;
    @Value("${app.bootstrap.admin-password}") private String adminPassword;

    @Bean
    public ApplicationRunner seed() {
        return args -> {
            seedRoles();
            seedAdministrator();
            seedGradeBands();
            seedBellSchedule();
            seedCalendar();
        };
    }

    private void seedRoles() {
        record R(String name, String description) {}
        List<R> wanted = List.of(
                new R(Role.ADMIN,     "Full access to every module"),
                new R(Role.HEAD,      "Head and deputy: records, discipline, reports"),
                new R(Role.TEACHER,   "Registers, mark entry, own timetable"),
                new R(Role.GUARDIAN,  "Parent portal for their own children"),
                new R(Role.LIBRARIAN, "Catalogue and circulation"),
                new R(Role.MATRON,    "Hostels, dormitories and exeats"));

        for (R r : wanted) {
            if (roles.findByName(r.name()).isEmpty()) {
                roles.save(new Role(r.name(), r.description()));
            }
        }
    }

    private void seedAdministrator() {
        if (users.count() > 0) return;

        AppUser admin = new AppUser();
        admin.setUsername(adminUsername);
        admin.setPassword(encoder.encode(adminPassword));
        admin.setEnabled(true);
        admin.setMustChangePassword(true);
        admin.setRoles(Set.of(roles.findByName(Role.ADMIN).orElseThrow()));
        users.save(admin);
    }

    /** Primary: a simple A–E scale. Secondary: the ZIMSEC-style A–U bands. */
    private void seedGradeBands() {
        if (bands.count() > 0) return;

        band(Phase.PRIMARY, 80, 100, "A", "Excellent", 5.0);
        band(Phase.PRIMARY, 70, 79.99, "B", "Very good", 4.0);
        band(Phase.PRIMARY, 60, 69.99, "C", "Good", 3.0);
        band(Phase.PRIMARY, 50, 59.99, "D", "Satisfactory", 2.0);
        band(Phase.PRIMARY, 40, 49.99, "E", "Needs support", 1.0);
        band(Phase.PRIMARY, 0, 39.99, "U", "Well below standard", 0.0);

        band(Phase.SECONDARY, 75, 100, "A", "Distinction", 5.0);
        band(Phase.SECONDARY, 65, 74.99, "B", "Merit", 4.0);
        band(Phase.SECONDARY, 55, 64.99, "C", "Credit", 3.0);
        band(Phase.SECONDARY, 45, 54.99, "D", "Pass", 2.0);
        band(Phase.SECONDARY, 35, 44.99, "E", "Marginal pass", 1.0);
        band(Phase.SECONDARY, 0, 34.99, "U", "Ungraded", 0.0);
    }

    private void band(Phase phase, double min, double max, String symbol, String descriptor, double points) {
        GradeBand b = new GradeBand();
        b.setPhase(phase);
        b.setMinPercent(min);
        b.setMaxPercent(max);
        b.setSymbol(symbol);
        b.setDescriptor(descriptor);
        b.setGradePoints(points);
        bands.save(b);
    }

    private void seedBellSchedule() {
        if (periods.count() > 0) return;

        // Primary — shorter periods, earlier finish
        bell(Phase.PRIMARY, 1, "Assembly", "07:30", "07:45", false);
        bell(Phase.PRIMARY, 2, "Period 1", "07:45", "08:20", true);
        bell(Phase.PRIMARY, 3, "Period 2", "08:20", "08:55", true);
        bell(Phase.PRIMARY, 4, "Period 3", "08:55", "09:30", true);
        bell(Phase.PRIMARY, 5, "Break",    "09:30", "09:50", false);
        bell(Phase.PRIMARY, 6, "Period 4", "09:50", "10:25", true);
        bell(Phase.PRIMARY, 7, "Period 5", "10:25", "11:00", true);
        bell(Phase.PRIMARY, 8, "Period 6", "11:00", "11:35", true);
        bell(Phase.PRIMARY, 9, "Lunch",    "11:35", "12:15", false);
        bell(Phase.PRIMARY, 10, "Period 7", "12:15", "12:50", true);
        bell(Phase.PRIMARY, 11, "Period 8", "12:50", "13:25", true);

        // Secondary
        bell(Phase.SECONDARY, 1, "Assembly", "07:20", "07:40", false);
        bell(Phase.SECONDARY, 2, "Period 1", "07:40", "08:20", true);
        bell(Phase.SECONDARY, 3, "Period 2", "08:20", "09:00", true);
        bell(Phase.SECONDARY, 4, "Period 3", "09:00", "09:40", true);
        bell(Phase.SECONDARY, 5, "Break",    "09:40", "10:00", false);
        bell(Phase.SECONDARY, 6, "Period 4", "10:00", "10:40", true);
        bell(Phase.SECONDARY, 7, "Period 5", "10:40", "11:20", true);
        bell(Phase.SECONDARY, 8, "Period 6", "11:20", "12:00", true);
        bell(Phase.SECONDARY, 9, "Lunch",    "12:00", "12:45", false);
        bell(Phase.SECONDARY, 10, "Period 7", "12:45", "13:25", true);
        bell(Phase.SECONDARY, 11, "Period 8", "13:25", "14:05", true);
        bell(Phase.SECONDARY, 12, "Period 9", "14:05", "14:45", true);
    }

    private void bell(Phase phase, int number, String label, String from, String to, boolean teaching) {
        BellPeriod p = new BellPeriod();
        p.setPhase(phase);
        p.setPeriodNumber(number);
        p.setLabel(label);
        p.setStartTime(LocalTime.parse(from));
        p.setEndTime(LocalTime.parse(to));
        p.setTeachingPeriod(teaching);
        periods.save(p);
    }

    /** A first academic year with three terms, so the app is usable on first run. */
    private void seedCalendar() {
        if (years.count() > 0) return;

        int y = LocalDate.now().getYear();
        AcademicYear year = new AcademicYear();
        year.setYear(y);
        year.setStartDate(LocalDate.of(y, 1, 10));
        year.setEndDate(LocalDate.of(y, 12, 5));
        year.setCurrent(true);
        years.save(year);

        term(year, 1, "Term 1", LocalDate.of(y, 1, 10), LocalDate.of(y, 4, 5));
        term(year, 2, "Term 2", LocalDate.of(y, 5, 6), LocalDate.of(y, 8, 8));
        term(year, 3, "Term 3", LocalDate.of(y, 9, 9), LocalDate.of(y, 12, 5));

        // Mark whichever term contains today as current, otherwise the first.
        List<Term> all = terms.findByAcademicYearIdOrderByTermNumberAsc(year.getId());
        LocalDate today = LocalDate.now();
        Term current = all.stream()
                .filter(t -> !today.isBefore(t.getStartDate()) && !today.isAfter(t.getEndDate()))
                .findFirst().orElse(all.get(0));
        current.setCurrent(true);
        terms.save(current);
    }

    private void term(AcademicYear year, int number, String name, LocalDate from, LocalDate to) {
        Term t = new Term();
        t.setAcademicYear(year);
        t.setTermNumber(number);
        t.setName(name);
        t.setStartDate(from);
        t.setEndDate(to);
        terms.save(t);
    }
}
