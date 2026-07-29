package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.AttendanceService;
import zw.co.cresolzim.schoolms.service.LibraryService;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final StudentRepository students;
    private final StaffRepository staff;
    private final SchoolClassRepository classes;
    private final BookCopyRepository copies;
    private final AcademicYearRepository years;
    private final TermRepository terms;
    private final AttendanceService attendance;
    private final LibraryService library;
    private final ClassSubjectAllocationRepository allocations;
    private final AssessmentRepository assessments;

    @GetMapping("/")
    public String root() { return "redirect:/dashboard"; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/denied")
    public String denied() { return "denied"; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();

        model.addAttribute("primaryCount",
                students.countByPhaseAndStatus(Phase.PRIMARY, StudentStatus.ENROLLED));
        model.addAttribute("secondaryCount",
                students.countByPhaseAndStatus(Phase.SECONDARY, StudentStatus.ENROLLED));
        model.addAttribute("boarderCount",
                students.countByResidencyAndStatus(Residency.BOARDER, StudentStatus.ENROLLED));
        model.addAttribute("staffCount", staff.countByEmploymentStatus(EmploymentStatus.ACTIVE));

        model.addAttribute("attendanceToday", attendance.dailySummary(today));
        model.addAttribute("staffOnPremises", attendance.stillOnPremises(today).size());
        model.addAttribute("overdueBooks", library.overdueList().size());
        model.addAttribute("booksOnLoan", copies.countByStatus(CopyStatus.ON_LOAN));

        model.addAttribute("currentYear", years.findByCurrentTrue().orElse(null));
        model.addAttribute("currentTerm", terms.findByCurrentTrue().orElse(null));
        model.addAttribute("today", today);
        return "dashboard/index";
    }

    /** A teacher's own day: classes taught, registers due, marks outstanding. */
    @GetMapping("/dashboard/teacher")
    public String teacherDashboard(Model model) {
        Long staffId = CurrentUser.staffId();
        if (staffId == null) return "redirect:/dashboard";

        Long termId = terms.findByCurrentTrue().map(t -> t.getId()).orElse(null);

        model.addAttribute("me", staff.findById(staffId).orElse(null));
        model.addAttribute("allocations", allocations.findByTeacherIdAndActiveTrue(staffId));
        model.addAttribute("weeklyLoad", allocations.weeklyLoadOfTeacher(staffId));
        model.addAttribute("formClasses", classes.findByClassTeacherIdAndActiveTrue(staffId));
        model.addAttribute("unpublished", termId == null
                ? java.util.List.of()
                : assessments.findByTeacherIdAndTermIdOrderByAssessmentDateDesc(staffId, termId)
                        .stream().filter(a -> !a.isPublished()).toList());
        model.addAttribute("today", LocalDate.now());
        return "dashboard/teacher";
    }
}
