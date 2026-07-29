package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.AssessmentService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService service;
    private final AssessmentRepository assessments;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final StaffRepository staff;
    private final TermRepository terms;
    private final AcademicYearRepository years;
    private final ClassSubjectAllocationRepository allocations;

    @GetMapping
    public String list(@RequestParam(required = false) Long classId, Model model) {
        Term term = terms.findByCurrentTrue().orElse(null);
        model.addAttribute("term", term);
        model.addAttribute("classes", currentClasses());
        model.addAttribute("classId", classId);
        model.addAttribute("assessments", (term == null || classId == null) ? List.of()
                : assessments.findByTermIdAndSchoolClassIdOrderByAssessmentDateDesc(term.getId(), classId));
        return "assessment/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long classId, Model model) {
        Assessment a = new Assessment();
        if (classId != null) a.setSchoolClass(classes.findById(classId).orElse(null));
        model.addAttribute("assessment", a);
        model.addAttribute("classes", currentClasses());
        model.addAttribute("types", AssessmentType.values());
        model.addAttribute("allocations", classId == null ? List.of()
                : allocations.findBySchoolClassIdAndActiveTrue(classId));
        return "assessment/form";
    }

    @PostMapping
    public String create(@ModelAttribute Assessment assessment, RedirectAttributes flash) {
        assessment.setTerm(terms.findByCurrentTrue().orElseThrow());
        if (assessment.getTeacher() == null && CurrentUser.staffId() != null) {
            assessment.setTeacher(staff.findById(CurrentUser.staffId()).orElse(null));
        }
        Assessment saved = assessments.save(assessment);
        flash.addFlashAttribute("message", "Sitting created. Enter marks when ready.");
        return "redirect:/assessments/" + saved.getId() + "/marks";
    }

    /** The mark sheet. One row per learner, tab straight down the column. */
    @GetMapping("/{id}/marks")
    public String markSheet(@PathVariable Long id, Model model) {
        Assessment a = assessments.findById(id).orElseThrow();

        model.addAttribute("assessment", a);
        model.addAttribute("rows", service.markSheet(id));
        model.addAttribute("stats", service.statistics(id));
        return "assessment/marks";
    }

    @PostMapping("/{id}/marks")
    public String saveMarks(@PathVariable Long id,
                            @RequestParam(name = "studentId", required = false) List<Long> studentIds,
                            @RequestParam(name = "rawMark", required = false) List<Double> rawMarks,
                            @RequestParam(name = "presence", required = false) List<String> presence,
                            @RequestParam(name = "comment", required = false) List<String> comments,
                            RedirectAttributes flash) {
        int saved = service.saveMarks(id, studentIds, rawMarks, presence, comments, CurrentUser.name());
        flash.addFlashAttribute("message", saved + " marks saved.");
        return "redirect:/assessments/" + id + "/marks";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes flash) {
        service.publish(id);
        flash.addFlashAttribute("message", "Sitting published. Marks now count towards the report card.");
        return "redirect:/assessments/" + id + "/marks";
    }

    @PostMapping("/{id}/unpublish")
    public String unpublish(@PathVariable Long id, RedirectAttributes flash) {
        service.unpublish(id);
        flash.addFlashAttribute("message", "Sitting reopened for editing.");
        return "redirect:/assessments/" + id + "/marks";
    }

    private List<SchoolClass> currentClasses() {
        return years.findByCurrentTrue()
                .map(y -> classes.findByAcademicYearIdAndActiveTrueOrderByPhaseAscGradeLevelAscNameAsc(y.getId()))
                .orElse(List.of());
    }
}
