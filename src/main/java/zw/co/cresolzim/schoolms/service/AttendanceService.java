package zw.co.cresolzim.schoolms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final StudentAttendanceRepository studentAttendance;
    private final StaffAttendanceRepository staffAttendance;
    private final StudentRepository students;
    private final StaffRepository staff;
    private final SchoolClassRepository classes;
    private final TermRepository terms;

    /** Expected reporting and knock-off times. Move these to a settings table if they vary. */
    @Value("${app.staff.report-by:07:15}")  private String reportBy;
    @Value("${app.staff.knock-off:16:30}")  private String knockOff;

    /* -------------------------------------------------- learner register */

    /**
     * Opens the register for a class on a date. Any learner without a row yet is
     * pre-marked present, which is what teachers expect: they only tick exceptions.
     */
    @Transactional
    public List<StudentAttendance> openRegister(Long classId, LocalDate date, String session, Long recordedById) {
        SchoolClass sc = classes.findById(classId).orElseThrow(() -> new NotFoundException("Class", classId));
        Term term = terms.findByCurrentTrue()
                .orElseThrow(() -> new RuleViolationException("No term is marked current. Set one under Academics."));

        Staff recorder = recordedById == null ? null : staff.findById(recordedById).orElse(null);

        Map<Long, StudentAttendance> existing = new HashMap<>();
        for (StudentAttendance a : studentAttendance
                .findBySchoolClassIdAndAttendanceDateAndSessionName(classId, date, session)) {
            existing.put(a.getStudent().getId(), a);
        }

        List<StudentAttendance> register = new ArrayList<>();
        for (Student s : students.findByCurrentClassIdAndStatusOrderBySurnameAscFirstNameAsc(
                classId, StudentStatus.ENROLLED)) {

            StudentAttendance a = existing.get(s.getId());
            if (a == null) {
                a = new StudentAttendance();
                a.setStudent(s);
                a.setSchoolClass(sc);
                a.setTerm(term);
                a.setAttendanceDate(date);
                a.setSessionName(session);
                a.setStatus(AttendanceStatus.PRESENT);
                a.setRecordedBy(recorder);
            }
            register.add(a);
        }
        return register;
    }

    /**
     * Saves a submitted register. The form posts parallel arrays rather than a
     * bound object graph, so an added or withdrawn learner between opening and
     * saving the page cannot shift rows onto the wrong names.
     */
    @Transactional
    public int saveRegister(Long classId, LocalDate date, String session,
                            List<Long> studentIds, List<AttendanceStatus> statuses,
                            List<String> arrivals, List<String> remarks,
                            Long recordedById, String actor) {

        if (studentIds == null || studentIds.isEmpty()) return 0;
        if (statuses == null || statuses.size() != studentIds.size()) {
            throw new RuleViolationException("The register did not submit cleanly. Reload and try again.");
        }

        SchoolClass sc = classes.findById(classId).orElseThrow(() -> new NotFoundException("Class", classId));
        Term term = terms.findByCurrentTrue()
                .orElseThrow(() -> new RuleViolationException("No term is marked current."));
        Staff recorder = recordedById == null ? null : staff.findById(recordedById).orElse(null);

        List<StudentAttendance> toSave = new ArrayList<>();

        for (int i = 0; i < studentIds.size(); i++) {
            Long studentId = studentIds.get(i);
            AttendanceStatus status = statuses.get(i);

            StudentAttendance a = studentAttendance
                    .findByStudentIdAndAttendanceDateAndSessionName(studentId, date, session)
                    .orElseGet(StudentAttendance::new);

            if (a.getId() == null) {
                a.setStudent(students.findById(studentId)
                        .orElseThrow(() -> new NotFoundException("Student", studentId)));
                a.setSchoolClass(sc);
                a.setTerm(term);
                a.setAttendanceDate(date);
                a.setSessionName(session);
                a.setRecordedBy(recorder);
                a.setCreatedBy(actor);
            }

            a.setStatus(status);
            a.setRemarks(remarks == null || i >= remarks.size() ? null : blankToNull(remarks.get(i)));

            String arrival = arrivals == null || i >= arrivals.size() ? null : blankToNull(arrivals.get(i));
            if (status == AttendanceStatus.ABSENT) {
                a.setArrivalTime(null);
            } else if (arrival != null) {
                a.setArrivalTime(LocalTime.parse(arrival));
            } else if (status == AttendanceStatus.LATE && a.getArrivalTime() == null) {
                a.setArrivalTime(LocalTime.now().withSecond(0).withNano(0));
            }

            toSave.add(a);
        }

        studentAttendance.saveAll(toSave);
        return toSave.size();
    }

    private String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }


    /** Keyed by status name so the dashboard can read summary['ABSENT'] directly. */
    @Transactional(readOnly = true)
    public Map<String, Long> dailySummary(LocalDate date) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (AttendanceStatus s : AttendanceStatus.values()) out.put(s.name(), 0L);
        for (Object[] row : studentAttendance.dailySummary(date)) {
            out.put(((AttendanceStatus) row[0]).name(), ((Number) row[1]).longValue());
        }
        return out;
    }

    /* -------------------------------------------------- staff sign-in book */

    /**
     * Signs a member of staff in. Lateness is measured against the expected
     * reporting time and stored so the head does not have to work it out later.
     */
    @Transactional
    public StaffAttendance signIn(Long staffId, LocalDate date, LocalTime arrival, String actor) {
        Staff st = staff.findById(staffId).orElseThrow(() -> new NotFoundException("Staff", staffId));

        StaffAttendance a = staffAttendance.findByStaffIdAndAttendanceDate(staffId, date)
                .orElseGet(() -> {
                    StaffAttendance n = new StaffAttendance();
                    n.setStaff(st);
                    n.setAttendanceDate(date);
                    return n;
                });

        if (a.getArrivalTime() != null) {
            throw new RuleViolationException(
                    st.getFullName() + " already signed in at " + a.getArrivalTime() + ".");
        }

        LocalTime expected = LocalTime.parse(reportBy);
        a.setArrivalTime(arrival);
        a.setStatus(arrival.isAfter(expected) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        a.setMinutesLate(arrival.isAfter(expected)
                ? (int) Duration.between(expected, arrival).toMinutes() : 0);
        a.setSignedInBy(actor);
        return staffAttendance.save(a);
    }

    /** Signs a member of staff out and records any early departure. */
    @Transactional
    public StaffAttendance signOut(Long staffId, LocalDate date, LocalTime departure) {
        StaffAttendance a = staffAttendance.findByStaffIdAndAttendanceDate(staffId, date)
                .orElseThrow(() -> new RuleViolationException(
                        "No sign-in on record for this date, so there is nothing to sign out."));

        if (a.getArrivalTime() != null && departure.isBefore(a.getArrivalTime())) {
            throw new RuleViolationException("Departure time cannot be before the arrival time.");
        }

        LocalTime expected = LocalTime.parse(knockOff);
        a.setDepartureTime(departure);
        a.setMinutesEarlyDeparture(departure.isBefore(expected)
                ? (int) Duration.between(departure, expected).toMinutes() : 0);
        return staffAttendance.save(a);
    }

    /** Marks a member of staff absent for the day with a reason. */
    @Transactional
    public StaffAttendance markStaffAbsent(Long staffId, LocalDate date, AttendanceStatus status, String remarks) {
        Staff st = staff.findById(staffId).orElseThrow(() -> new NotFoundException("Staff", staffId));
        StaffAttendance a = staffAttendance.findByStaffIdAndAttendanceDate(staffId, date)
                .orElseGet(StaffAttendance::new);
        a.setStaff(st);
        a.setAttendanceDate(date);
        a.setStatus(status);
        a.setArrivalTime(null);
        a.setDepartureTime(null);
        a.setMinutesLate(0);
        a.setRemarks(remarks);
        return staffAttendance.save(a);
    }

    /** Everyone who has not signed out yet — the security desk's end-of-day list. */
    @Transactional(readOnly = true)
    public List<StaffAttendance> stillOnPremises(LocalDate date) {
        return staffAttendance.stillOnPremises(date);
    }

    @Transactional(readOnly = true)
    public List<StaffAttendance> staffDay(LocalDate date) {
        return staffAttendance.findByAttendanceDateOrderByArrivalTimeAsc(date);
    }
}
