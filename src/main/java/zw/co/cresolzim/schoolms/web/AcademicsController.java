package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.RuleViolationException;
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
    private final BellPeriodRepository bells;
    private final DepartmentRepository departments;
    private final TimetableSlotRepository slots;
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

    /** Same page, but with an existing subject loaded into the form. */
    @GetMapping("/subjects/{id}/edit")
    public String editSubject(@PathVariable Long id, Model model) {
        model.addAttribute("subjects", subjects.findByActiveTrueOrderByPhaseAscDisplayOrderAsc());
        model.addAttribute("subject", subjects.findById(id).orElseThrow());
        model.addAttribute("phases", Phase.values());
        model.addAttribute("departments", departments.findAllByOrderByNameAsc());
        return "academics/subjects";
    }

    @PostMapping("/subjects")
    public String saveSubject(@ModelAttribute Subject subject, RedirectAttributes flash) {
        Subject saved = subjects.save(subject);
        flash.addFlashAttribute("message", saved.getName() + " saved.");
        return "redirect:/academics/subjects";
    }

    /** Subjects are retired rather than deleted, so historic marks keep their subject. */
    @PostMapping("/subjects/{id}/retire")
    public String retireSubject(@PathVariable Long id, RedirectAttributes flash) {
        subjects.findById(id).ifPresent(s -> { s.setActive(false); subjects.save(s); });
        flash.addFlashAttribute("message", "Subject retired. Past marks are unaffected.");
        return "redirect:/academics/subjects";
    }

    /* --------------------------------------------------------- classrooms */

    @GetMapping("/classrooms")
    public String roomList(Model model) {
        model.addAttribute("classrooms", classrooms.findByActiveTrueOrderByCodeAsc());
        model.addAttribute("classroom", new Classroom());
        return "academics/classrooms";
    }

    @GetMapping("/classrooms/{id}/edit")
    public String editRoom(@PathVariable Long id, Model model) {
        model.addAttribute("classrooms", classrooms.findByActiveTrueOrderByCodeAsc());
        model.addAttribute("classroom", classrooms.findById(id).orElseThrow());
        return "academics/classrooms";
    }

    @PostMapping("/classrooms")
    public String saveRoom(@ModelAttribute Classroom classroom, RedirectAttributes flash) {
        Classroom saved = classrooms.save(classroom);
        flash.addFlashAttribute("message", saved.getCode() + " saved.");
        return "redirect:/academics/classrooms";
    }

    @PostMapping("/classrooms/{id}/retire")
    public String retireRoom(@PathVariable Long id, RedirectAttributes flash) {
        classrooms.findById(id).ifPresent(r -> { r.setActive(false); classrooms.save(r); });
        flash.addFlashAttribute("message", "Room retired.");
        return "redirect:/academics/classrooms";
    }

    /* -------------------------------------------------------- departments */

    @GetMapping("/departments")
    public String departmentList(Model model) {
        model.addAttribute("departments", departments.findAllByOrderByNameAsc());
        model.addAttribute("department", new Department());
        model.addAttribute("staffList",
                staff.findByEmploymentStatusOrderBySurnameAsc(EmploymentStatus.ACTIVE));
        return "academics/departments";
    }

    @GetMapping("/departments/{id}/edit")
    public String editDepartment(@PathVariable Long id, Model model) {
        model.addAttribute("departments", departments.findAllByOrderByNameAsc());
        model.addAttribute("department", departments.findById(id).orElseThrow());
        model.addAttribute("staffList",
                staff.findByEmploymentStatusOrderBySurnameAsc(EmploymentStatus.ACTIVE));
        return "academics/departments";
    }

    @PostMapping("/departments")
    public String saveDepartment(@ModelAttribute Department department, RedirectAttributes flash) {
        Department saved = departments.save(department);
        flash.addFlashAttribute("message", saved.getName() + " saved.");
        return "redirect:/academics/departments";
    }

    /* ------------------------------------------------------- bell periods */

    /** The daily bell schedule, per phase. The timetable grid is built from this. */
    @GetMapping("/bells")
    public String bellList(@RequestParam(required = false) Phase phase, Model model) {
        Phase p = phase == null ? Phase.PRIMARY : phase;
        model.addAttribute("phase", p);
        model.addAttribute("phases", Phase.values());
        model.addAttribute("periods", bells.findByPhaseOrderByPeriodNumberAsc(p));
        BellPeriod blank = new BellPeriod();
        blank.setPhase(p);
        model.addAttribute("period", blank);
        return "academics/bells";
    }

    @GetMapping("/bells/{id}/edit")
    public String editBell(@PathVariable Long id, Model model) {
        BellPeriod bp = bells.findById(id).orElseThrow();
        model.addAttribute("phase", bp.getPhase());
        model.addAttribute("phases", Phase.values());
        model.addAttribute("periods", bells.findByPhaseOrderByPeriodNumberAsc(bp.getPhase()));
        model.addAttribute("period", bp);
        return "academics/bells";
    }

    @PostMapping("/bells")
    public String saveBell(@ModelAttribute("period") BellPeriod period, RedirectAttributes flash) {
        if (period.getEndTime() != null && period.getStartTime() != null
                && !period.getEndTime().isAfter(period.getStartTime())) {
            throw new RuleViolationException("A period must end after it starts.");
        }
        BellPeriod saved = bells.save(period);
        flash.addFlashAttribute("message", saved.getLabel() + " saved.");
        return "redirect:/academics/bells?phase=" + saved.getPhase();
    }

    @PostMapping("/bells/{id}/remove")
    public String removeBell(@PathVariable Long id, RedirectAttributes flash) {
        BellPeriod bp = bells.findById(id).orElseThrow();
        Phase p = bp.getPhase();
        if (slots.countByBellPeriodId(id) > 0) {
            throw new RuleViolationException(
                    "Lessons are timetabled in " + bp.getLabel()
                    + ". Clear them from the timetable before removing the period.");
        }
        bells.delete(bp);
        flash.addFlashAttribute("message", "Period removed.");
        return "redirect:/academics/bells?phase=" + p;
    }

    /* --------------------------------------------------- years and terms */

    @GetMapping("/years")
    public String yearList(Model model) {
        model.addAttribute("years", years.findAllByOrderByYearDesc());
        model.addAttribute("terms", terms.findAllByOrderByAcademicYearYearDescTermNumberDesc());
        model.addAttribute("periods", bells.findAll());
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
