package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.StudentService;

import java.util.List;

@Controller
@RequestMapping("/academics")
@RequiredArgsConstructor
public class AcademicsController {

    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final ClassroomRepository classrooms;
    private final ClassSubjectAllocationRepository allocations;
    private final StaffRepository staff;
    private final StudentRepository students;
    private final AcademicYearRepository years;
    private final TermRepository terms;
    private final BellPeriodRepository periods;
    private final StudentService studentService;

    /* ------------------------------------------------------------ classes */

    @GetMapping("/classes")
    public String classList(@RequestParam(required = false) Phase phase, Model model) {
        AcademicYear year = years.findByCurrentTrue().orElse(null);
        List<SchoolClass> list = year == null ? List.of()
                : (phase == null
                    ? classes.findByAcademicYearIdAndActiveTrueOrderByPhaseAscGradeLevelAscNameAsc(year.getId())
                    : classes.findByAcademicYearIdAndPhaseAndActiveTrueOrderByGradeLevelAscNameAsc(year.getId(), phase));

        model.addAttribute("classes", list);
        model.addAttribute("phase", phase);
        model.addAttribute("year", year);
        return "academics/classes";
    }

    @GetMapping("/classes/new")
    public String newClass(Model model) {
        model.addAttribute("schoolClass", new SchoolClass());
        return classFormModel(model);
    }

    @GetMapping("/classes/{id}/edit")
    public String editClass(@PathVariable Long id, Model model) {
        model.addAttribute("schoolClass", classes.findById(id).orElseThrow());
        return classFormModel(model);
    }

    private String classFormModel(Model model) {
        model.addAttribute("phases", Phase.values());
        model.addAttribute("teachers", staff.findByStaffTypeAndEmploymentStatusOrderBySurnameAsc(
                StaffType.TEACHING, EmploymentStatus.ACTIVE));
        model.addAttribute("classrooms", classrooms.findByActiveTrueOrderByCodeAsc());
        model.addAttribute("years", years.findAllByOrderByYearDesc());
        return "academics/class-form";
    }

    @PostMapping("/classes")
    public String saveClass(@ModelAttribute SchoolClass schoolClass, RedirectAttributes flash) {
        if (schoolClass.getAcademicYear() == null) {
            schoolClass.setAcademicYear(years.findByCurrentTrue().orElseThrow());
        }
        SchoolClass saved = classes.save(schoolClass);
        flash.addFlashAttribute("message", saved.getName() + " saved.");
        return "redirect:/academics/classes/" + saved.getId();
    }

    @GetMapping("/classes/{id}")
    public String viewClass(@PathVariable Long id, Model model) {
        SchoolClass sc = classes.findById(id).orElseThrow();
        model.addAttribute("schoolClass", sc);
        model.addAttribute("roll", students.findByCurrentClassIdAndStatusOrderBySurnameAscFirstNameAsc(
                id, StudentStatus.ENROLLED));
        model.addAttribute("allocations", allocations.findBySchoolClassIdAndActiveTrue(id));
        model.addAttribute("subjects", subjects.findByPhaseAndActiveTrueOrderByDisplayOrderAscNameAsc(sc.getPhase()));
        model.addAttribute("teachers", staff.findByStaffTypeAndEmploymentStatusOrderBySurnameAsc(
                StaffType.TEACHING, EmploymentStatus.ACTIVE));
        return "academics/class-view";
    }

    /** Year-end promotion for one class. */
    @PostMapping("/classes/{id}/promote")
    public String promote(@PathVariable Long id, @RequestParam Long targetYearId,
                          @RequestParam(required = false) List<Long> repeaterIds,
                          RedirectAttributes flash) {
        int moved = studentService.promoteClass(id, targetYearId, repeaterIds, CurrentUser.name());
        flash.addFlashAttribute("message", moved + " learner(s) processed.");
        return "redirect:/academics/classes/" + id;
    }

    /* ----------------------------------------------------------- subjects */

    @GetMapping("/subjects")
    public String subjectList(Model model) {
        model.addAttribute("subjects", subjects.findByActiveTrueOrderByPhaseAscDisplayOrderAsc());
        model.addAttribute("subject", new Subject());
        model.addAttribute("phases", Phase.values());
        model.addAttribute("departments", List.of());
        return "academics/subjects";
    }

    @PostMapping("/subjects")
    public String saveSubject(@ModelAttribute Subject subject, RedirectAttributes flash) {
        subjects.save(subject);
        flash.addFlashAttribute("message", "Subject saved.");
        return "redirect:/academics/subjects";
    }

    /* --------------------------------------------------------- classrooms */

    @GetMapping("/classrooms")
    public String roomList(Model model) {
        model.addAttribute("classrooms", classrooms.findByActiveTrueOrderByCodeAsc());
        model.addAttribute("classroom", new Classroom());
        return "academics/classrooms";
    }

    @PostMapping("/classrooms")
    public String saveRoom(@ModelAttribute Classroom classroom, RedirectAttributes flash) {
        classrooms.save(classroom);
        flash.addFlashAttribute("message", "Room saved.");
        return "redirect:/academics/classrooms";
    }

    /* --------------------------------------------------- years and terms */

    @GetMapping("/years")
    public String yearList(Model model) {
        model.addAttribute("years", years.findAllByOrderByYearDesc());
        model.addAttribute("terms", terms.findAllByOrderByAcademicYearYearDescTermNumberDesc());
        model.addAttribute("periods", periods.findAll());
        return "academics/years";
    }

    @PostMapping("/years/{id}/current")
    public String setCurrentYear(@PathVariable Long id, RedirectAttributes flash) {
        years.findAll().forEach(y -> { y.setCurrent(y.getId().equals(id)); years.save(y); });
        flash.addFlashAttribute("message", "Current academic year changed.");
        return "redirect:/academics/years";
    }

    @PostMapping("/terms/{id}/current")
    public String setCurrentTerm(@PathVariable Long id, RedirectAttributes flash) {
        terms.findAll().forEach(t -> { t.setCurrent(t.getId().equals(id)); terms.save(t); });
        flash.addFlashAttribute("message", "Current term changed.");
        return "redirect:/academics/years";
    }

    @PostMapping("/terms/{id}/lock")
    public String lockMarks(@PathVariable Long id, @RequestParam boolean locked, RedirectAttributes flash) {
        terms.findById(id).ifPresent(t -> { t.setMarksLocked(locked); terms.save(t); });
        flash.addFlashAttribute("message", locked ? "Mark entry closed." : "Mark entry reopened.");
        return "redirect:/academics/years";
    }
}
