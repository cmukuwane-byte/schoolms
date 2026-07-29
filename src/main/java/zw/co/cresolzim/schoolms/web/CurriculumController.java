package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.Phase;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.CurriculumService;

import java.util.List;

/**
 * The scheme of work. Everyone may read it; only the head and administrator
 * may change it.
 */
@Controller
@RequestMapping("/curriculum")
@RequiredArgsConstructor
public class CurriculumController {

    private final CurriculumService service;
    private final SubjectRepository subjects;
    private final SchoolClassRepository classes;
    private final CurriculumTopicRepository topics;

    @GetMapping
    public String index(@RequestParam(required = false) Long subjectId,
                        @RequestParam(required = false) Integer gradeLevel,
                        Model model) {

        model.addAttribute("subjects", subjects.findByActiveTrueOrderByPhaseAscDisplayOrderAsc());
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("gradeLevel", gradeLevel);

        if (subjectId != null && gradeLevel != null) {
            Subject s = subjects.findById(subjectId).orElseThrow();
            model.addAttribute("subject", s);
            model.addAttribute("byTerm", service.byTerm(subjectId, gradeLevel));
        }
        return "curriculum/index";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyAuthority(T(zw.co.cresolzim.schoolms.domain.Role).ADMIN, " +
                  "T(zw.co.cresolzim.schoolms.domain.Role).HEAD)")
    public String blank(@RequestParam(required = false) Long subjectId,
                        @RequestParam(required = false) Integer gradeLevel,
                        @RequestParam(required = false) Integer termNumber,
                        Model model) {
        CurriculumTopic topic = new CurriculumTopic();
        if (subjectId != null) topic.setSubject(subjects.findById(subjectId).orElse(null));
        topic.setGradeLevel(gradeLevel);
        topic.setTermNumber(termNumber == null ? 1 : termNumber);
        model.addAttribute("topic", topic);
        return form(model);
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyAuthority(T(zw.co.cresolzim.schoolms.domain.Role).ADMIN, " +
                  "T(zw.co.cresolzim.schoolms.domain.Role).HEAD)")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("topic", topics.findById(id).orElseThrow());
        return form(model);
    }

    private String form(Model model) {
        model.addAttribute("subjects", subjects.findByActiveTrueOrderByPhaseAscDisplayOrderAsc());
        model.addAttribute("phases", Phase.values());
        return "curriculum/form";
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority(T(zw.co.cresolzim.schoolms.domain.Role).ADMIN, " +
                  "T(zw.co.cresolzim.schoolms.domain.Role).HEAD)")
    public String save(@ModelAttribute("topic") CurriculumTopic topic, RedirectAttributes flash) {
        CurriculumTopic saved = service.save(topic, CurrentUser.name());
        flash.addFlashAttribute("message",
                "Week " + saved.getWeekNumber() + " of term " + saved.getTermNumber() + " saved.");
        return "redirect:/curriculum?subjectId=" + saved.getSubject().getId()
             + "&gradeLevel=" + saved.getGradeLevel();
    }

    @PostMapping("/{id}/retire")
    @PreAuthorize("hasAnyAuthority(T(zw.co.cresolzim.schoolms.domain.Role).ADMIN, " +
                  "T(zw.co.cresolzim.schoolms.domain.Role).HEAD)")
    public String retire(@PathVariable Long id, @RequestParam Long subjectId,
                         @RequestParam Integer gradeLevel, RedirectAttributes flash) {
        service.retire(id);
        flash.addFlashAttribute("message", "Topic removed from the scheme.");
        return "redirect:/curriculum?subjectId=" + subjectId + "&gradeLevel=" + gradeLevel;
    }

    @PostMapping("/copy")
    @PreAuthorize("hasAnyAuthority(T(zw.co.cresolzim.schoolms.domain.Role).ADMIN, " +
                  "T(zw.co.cresolzim.schoolms.domain.Role).HEAD)")
    public String copy(@RequestParam Long subjectId, @RequestParam Integer fromGrade,
                       @RequestParam Integer toGrade, RedirectAttributes flash) {
        int n = service.copyToGrade(subjectId, fromGrade, toGrade, CurrentUser.name());
        flash.addFlashAttribute("message", n + " topic(s) copied. Edit them to suit the grade.");
        return "redirect:/curriculum?subjectId=" + subjectId + "&gradeLevel=" + toGrade;
    }

    /** Coverage: scheme weeks with no lesson plan against them, for one class. */
    @GetMapping("/coverage")
    public String coverage(@RequestParam Long classId, @RequestParam Long subjectId,
                           @RequestParam Integer termNumber, Model model) {
        SchoolClass sc = classes.findById(classId).orElseThrow();
        model.addAttribute("schoolClass", sc);
        model.addAttribute("subject", subjects.findById(subjectId).orElseThrow());
        model.addAttribute("termNumber", termNumber);
        model.addAttribute("gaps", service.gaps(subjectId, sc.getGradeLevel(), termNumber, classId));
        model.addAttribute("classes", classes.findAll());
        return "curriculum/coverage";
    }
}
