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
import zw.co.cresolzim.schoolms.service.TimetableService;

import java.util.List;

@Controller
@RequestMapping("/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService service;
    private final SchoolClassRepository classes;
    private final BellPeriodRepository periods;
    private final ClassroomRepository classrooms;
    private final ClassSubjectAllocationRepository allocations;
    private final StaffRepository staff;
    private final TermRepository terms;
    private final AcademicYearRepository years;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("classes", currentClasses());
        model.addAttribute("teachers", staff.findByStaffTypeAndEmploymentStatusOrderBySurnameAsc(
                StaffType.TEACHING, EmploymentStatus.ACTIVE));
        model.addAttribute("term", terms.findByCurrentTrue().orElse(null));
        return "academics/timetable-index";
    }

    /** The class grid, editable in place by an administrator. */
    @GetMapping("/class/{classId}")
    public String classGrid(@PathVariable Long classId, Model model) {
        SchoolClass sc = classes.findById(classId).orElseThrow();
        Term term = terms.findByCurrentTrue().orElseThrow();

        model.addAttribute("schoolClass", sc);
        model.addAttribute("term", term);
        model.addAttribute("days", DayOfWeekSlot.values());
        model.addAttribute("periods", periods.findByPhaseOrderByPeriodNumberAsc(sc.getPhase()));
        model.addAttribute("grid", service.classGrid(term.getId(), classId));
        model.addAttribute("allocations", allocations.findBySchoolClassIdAndActiveTrue(classId));
        model.addAttribute("classrooms", classrooms.findByActiveTrueOrderByCodeAsc());
        model.addAttribute("audit", service.auditClass(term.getId(), classId));
        return "academics/timetable-class";
    }

    /** A teacher's own grid, read-only. */
    @GetMapping("/teacher/{teacherId}")
    public String teacherGrid(@PathVariable Long teacherId, Model model) {
        Staff t = staff.findById(teacherId).orElseThrow();
        Term term = terms.findByCurrentTrue().orElseThrow();

        model.addAttribute("teacher", t);
        model.addAttribute("term", term);
        model.addAttribute("days", DayOfWeekSlot.values());
        model.addAttribute("periods", periods.findByPhaseOrderByPeriodNumberAsc(
                t.getPhase() == null ? Phase.SECONDARY : t.getPhase()));
        model.addAttribute("grid", service.teacherGrid(term.getId(), teacherId));
        return "academics/timetable-teacher";
    }

    @PostMapping("/slot")
    public String placeSlot(@RequestParam Long classId, @RequestParam Long subjectId,
                            @RequestParam Long teacherId,
                            @RequestParam(required = false) Long roomId,
                            @RequestParam DayOfWeekSlot day, @RequestParam Long periodId,
                            @RequestParam(required = false) Long slotId,
                            RedirectAttributes flash) {
        Term term = terms.findByCurrentTrue().orElseThrow();
        service.place(term.getId(), classId, subjectId, teacherId, roomId, day, periodId, slotId);
        flash.addFlashAttribute("message", "Lesson placed.");
        return "redirect:/timetable/class/" + classId;
    }

    @PostMapping("/slot/{id}/remove")
    public String removeSlot(@PathVariable Long id, @RequestParam Long classId, RedirectAttributes flash) {
        service.remove(id);
        flash.addFlashAttribute("message", "Lesson removed.");
        return "redirect:/timetable/class/" + classId;
    }

    private List<SchoolClass> currentClasses() {
        return years.findByCurrentTrue()
                .map(y -> classes.findByAcademicYearIdAndActiveTrueOrderByPhaseAscGradeLevelAscNameAsc(y.getId()))
                .orElse(List.of());
    }
}
