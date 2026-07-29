package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.AttendanceService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;
    private final SchoolClassRepository classes;
    private final StaffRepository staff;
    private final AcademicYearRepository years;
    private final StudentAttendanceRepository studentAttendance;
    private final TermRepository terms;

    /* --------------------------------------------------- learner register */

    @GetMapping
    public String index(Model model) {
        model.addAttribute("classes", currentClasses());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("summary", service.dailySummary(LocalDate.now()));
        return "attendance/index";
    }

    @GetMapping("/register")
    public String register(@RequestParam Long classId,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(defaultValue = "MORNING") String session,
                           Model model) {

        LocalDate day = date == null ? LocalDate.now() : date;

        model.addAttribute("rows", service.openRegister(classId, day, session, CurrentUser.staffId()));
        model.addAttribute("schoolClass", classes.findById(classId).orElseThrow());
        model.addAttribute("date", day);
        model.addAttribute("session", session);
        model.addAttribute("statuses", AttendanceStatus.values());
        return "attendance/register";
    }

    @PostMapping("/register")
    public String saveRegister(@RequestParam Long classId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam String session,
                               @RequestParam(name = "studentId", required = false) List<Long> studentIds,
                               @RequestParam(name = "status", required = false) List<AttendanceStatus> statuses,
                               @RequestParam(name = "arrival", required = false) List<String> arrivals,
                               @RequestParam(name = "remark", required = false) List<String> remarks,
                               RedirectAttributes flash) {
        int saved = service.saveRegister(classId, date, session, studentIds, statuses,
                arrivals, remarks, CurrentUser.staffId(), CurrentUser.name());
        flash.addFlashAttribute("message", saved + " entries saved for " + date + ".");
        return "redirect:/attendance/register?classId=" + classId + "&date=" + date + "&session=" + session;
    }

    @GetMapping("/student/{studentId}")
    public String studentHistory(@PathVariable Long studentId,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 Model model) {
        LocalDate start = from == null ? LocalDate.now().minusMonths(3) : from;
        model.addAttribute("records", studentAttendance
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
                        studentId, start, LocalDate.now()));
        model.addAttribute("from", start);
        return "attendance/student-history";
    }

    /** Learners missing school repeatedly this term. */
    @GetMapping("/follow-up")
    public String followUp(@RequestParam(defaultValue = "5") long threshold, Model model) {
        Long termId = terms.findByCurrentTrue().map(Term::getId).orElse(null);
        model.addAttribute("rows", termId == null ? List.of()
                : studentAttendance.chronicAbsentees(termId, threshold));
        model.addAttribute("threshold", threshold);
        return "attendance/follow-up";
    }

    /* ------------------------------------------------------ staff sign-in */

    @GetMapping("/staff")
    public String staffBook(@RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;
        model.addAttribute("date", day);
        model.addAttribute("records", service.staffDay(day));
        model.addAttribute("stillOn", service.stillOnPremises(day));
        model.addAttribute("staffList", staff.findByEmploymentStatusOrderBySurnameAsc(EmploymentStatus.ACTIVE));
        model.addAttribute("statuses", AttendanceStatus.values());
        return "attendance/staff-book";
    }

    @PostMapping("/staff/sign-in")
    public String signIn(@RequestParam Long staffId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam(required = false)
                         @DateTimeFormat(pattern = "HH:mm") LocalTime arrival,
                         RedirectAttributes flash) {
        StaffAttendance a = service.signIn(staffId, date,
                arrival == null ? LocalTime.now().withSecond(0).withNano(0) : arrival, CurrentUser.name());
        flash.addFlashAttribute("message", a.getStaff().getFullName() + " signed in at " + a.getArrivalTime()
                + (a.getMinutesLate() > 0 ? " (" + a.getMinutesLate() + " min late)" : "") + ".");
        return "redirect:/attendance/staff?date=" + date;
    }

    @PostMapping("/staff/sign-out")
    public String signOut(@RequestParam Long staffId,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @RequestParam(required = false)
                          @DateTimeFormat(pattern = "HH:mm") LocalTime departure,
                          RedirectAttributes flash) {
        StaffAttendance a = service.signOut(staffId, date,
                departure == null ? LocalTime.now().withSecond(0).withNano(0) : departure);
        flash.addFlashAttribute("message", a.getStaff().getFullName() + " signed out at " + a.getDepartureTime() + ".");
        return "redirect:/attendance/staff?date=" + date;
    }

    @PostMapping("/staff/absent")
    public String markAbsent(@RequestParam Long staffId,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam AttendanceStatus status,
                             @RequestParam(required = false) String remarks,
                             RedirectAttributes flash) {
        service.markStaffAbsent(staffId, date, status, remarks);
        flash.addFlashAttribute("message", "Recorded.");
        return "redirect:/attendance/staff?date=" + date;
    }

    private List<SchoolClass> currentClasses() {
        return years.findByCurrentTrue()
                .map(y -> classes.findByAcademicYearIdAndActiveTrueOrderByPhaseAscGradeLevelAscNameAsc(y.getId()))
                .orElse(List.of());
    }
}
