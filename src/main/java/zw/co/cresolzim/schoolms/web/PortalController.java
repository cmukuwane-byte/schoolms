package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import zw.co.cresolzim.schoolms.domain.Student;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.RuleViolationException;

import java.time.LocalDate;
import java.util.List;

/** What a parent sees. Everything here is scoped to their own children. */
@Controller
@RequestMapping("/portal")
@RequiredArgsConstructor
public class PortalController {

    private final StudentRepository students;
    private final ReportCardRepository reportCards;
    private final StudentAttendanceRepository attendance;
    private final BookLoanRepository loans;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("wards", wards());
        return "portal/index";
    }

    @GetMapping("/child/{studentId}")
    public String child(@PathVariable Long studentId, Model model) {
        Student s = requireOwnChild(studentId);
        model.addAttribute("student", s);
        model.addAttribute("reports", reportCards.findByStudentIdAndReleasedTrueOrderByTermIdDesc(studentId));
        model.addAttribute("attendance", attendance
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
                        studentId, LocalDate.now().minusMonths(1), LocalDate.now()));
        model.addAttribute("loans", loans.findByStudentIdOrderByIssueDateDesc(studentId));
        return "portal/child";
    }

    @GetMapping("/report/{reportId}")
    public String report(@PathVariable Long reportId, Model model) {
        var rc = reportCards.findById(reportId).orElseThrow();
        requireOwnChild(rc.getStudent().getId());
        if (!rc.isReleased()) {
            throw new RuleViolationException("That report has not been released yet.");
        }
        model.addAttribute("card", rc);
        model.addAttribute("readOnly", true);
        return "assessment/report-card";
    }

    private List<Student> wards() {
        Long gid = CurrentUser.guardianId();
        return gid == null ? List.of() : students.findWardsOfGuardian(gid);
    }

    /** Stops a parent reading another family's records by changing the URL. */
    private Student requireOwnChild(Long studentId) {
        return wards().stream()
                .filter(w -> w.getId().equals(studentId)).findFirst()
                .orElseThrow(() -> new RuleViolationException("That learner is not on your account."));
    }
}
