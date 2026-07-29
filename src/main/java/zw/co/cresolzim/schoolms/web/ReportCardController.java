package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.ReportCardService;

import java.util.List;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService service;
    private final ReportCardRepository reportCards;
    private final SchoolClassRepository classes;
    private final TermRepository terms;
    private final AcademicYearRepository years;

    @GetMapping
    public String index(@RequestParam(required = false) Long classId, Model model) {
        Term term = terms.findByCurrentTrue().orElse(null);
        model.addAttribute("term", term);
        model.addAttribute("classes", currentClasses());
        model.addAttribute("classId", classId);
        model.addAttribute("cards", (term == null || classId == null) ? List.of()
                : reportCards.findByTermIdAndSchoolClassIdOrderByPositionInClassAsc(term.getId(), classId));
        return "assessment/reports";
    }

    @PostMapping("/generate")
    public String generate(@RequestParam Long classId, RedirectAttributes flash) {
        Term term = terms.findByCurrentTrue().orElseThrow();
        int n = service.generateForClass(classId, term.getId(), CurrentUser.name());
        flash.addFlashAttribute("message", n + " report card(s) generated and ranked.");
        return "redirect:/reports?classId=" + classId;
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("card", reportCards.findById(id).orElseThrow());
        return "assessment/report-card";
    }

    @PostMapping("/{id}/comments")
    public String saveComments(@PathVariable Long id,
                               @RequestParam(required = false) String classTeacherComment,
                               @RequestParam(required = false) String headComment,
                               @RequestParam(required = false) String conduct,
                               RedirectAttributes flash) {
        ReportCard rc = reportCards.findById(id).orElseThrow();
        rc.setClassTeacherComment(classTeacherComment);
        rc.setHeadComment(headComment);
        rc.setConduct(conduct);
        reportCards.save(rc);
        flash.addFlashAttribute("message", "Comments saved.");
        return "redirect:/reports/" + id;
    }

    @PostMapping("/{id}/release")
    public String release(@PathVariable Long id, RedirectAttributes flash) {
        service.release(id);
        flash.addFlashAttribute("message", "Report released to the guardian portal.");
        return "redirect:/reports/" + id;
    }

    @PostMapping("/release-class")
    public String releaseClass(@RequestParam Long classId, RedirectAttributes flash) {
        Term term = terms.findByCurrentTrue().orElseThrow();
        int n = service.releaseClass(classId, term.getId());
        flash.addFlashAttribute("message", n + " report(s) released.");
        return "redirect:/reports?classId=" + classId;
    }

    private List<SchoolClass> currentClasses() {
        return years.findByCurrentTrue()
                .map(y -> classes.findByAcademicYearIdAndActiveTrueOrderByPhaseAscGradeLevelAscNameAsc(y.getId()))
                .orElse(List.of());
    }
}
