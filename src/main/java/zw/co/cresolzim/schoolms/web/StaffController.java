package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffRepository staff;
    private final SubjectRepository subjects;
    private final DepartmentRepository departments;
    private final ClassSubjectAllocationRepository allocations;
    private final SchoolClassRepository classes;
    private final StaffAttendanceRepository attendance;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) StaffType type, Model model) {
        List<Staff> results;
        if (q != null && !q.isBlank())  results = staff.search(q.trim());
        else if (type != null)          results = staff
                .findByStaffTypeAndEmploymentStatusOrderBySurnameAsc(type, EmploymentStatus.ACTIVE);
        else                            results = staff
                .findByEmploymentStatusOrderBySurnameAsc(EmploymentStatus.ACTIVE);

        model.addAttribute("staffList", results);
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("types", StaffType.values());
        return "staff/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("member", new Staff());
        return formModel(model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("member", staff.findById(id).orElseThrow());
        return formModel(model);
    }

    private String formModel(Model model) {
        model.addAttribute("departments", departments.findAll());
        model.addAttribute("subjects", subjects.findByActiveTrueOrderByPhaseAscDisplayOrderAsc());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("types", StaffType.values());
        model.addAttribute("phases", Phase.values());
        model.addAttribute("statuses", EmploymentStatus.values());
        return "staff/form";
    }

    @PostMapping
    public String save(@ModelAttribute("member") Staff member,
                       @RequestParam(required = false) List<Long> subjectIds,
                       RedirectAttributes flash) {
        if (member.getDateJoined() == null) member.setDateJoined(LocalDate.now());
        if (member.getStaffNumber() == null || member.getStaffNumber().isBlank()) {
            member.setStaffNumber("EMP" + String.format("%04d", staff.count() + 1));
        }
        member.setTeachableSubjects(subjectIds == null ? new HashSet<>()
                : new HashSet<>(subjects.findAllById(subjectIds)));
        Staff saved = staff.save(member);
        flash.addFlashAttribute("message", saved.getFullName() + " saved.");
        return "redirect:/staff/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Staff member = staff.findById(id).orElseThrow();
        model.addAttribute("member", member);
        model.addAttribute("allocations", allocations.findByTeacherIdAndActiveTrue(id));
        model.addAttribute("weeklyLoad", allocations.weeklyLoadOfTeacher(id));
        model.addAttribute("formClasses", classes.findByClassTeacherIdAndActiveTrue(id));
        model.addAttribute("recentAttendance", attendance
                .findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
                        id, LocalDate.now().minusDays(30), LocalDate.now()));
        return "staff/view";
    }

    /** Assigns a teacher to a class and subject, with the weekly period count. */
    @PostMapping("/allocations")
    public String allocate(@RequestParam Long classId, @RequestParam Long subjectId,
                           @RequestParam Long teacherId, @RequestParam Integer periodsPerWeek,
                           RedirectAttributes flash) {

        ClassSubjectAllocation a = allocations.findBySchoolClassIdAndSubjectId(classId, subjectId)
                .orElseGet(ClassSubjectAllocation::new);
        a.setSchoolClass(classes.findById(classId).orElseThrow());
        a.setSubject(subjects.findById(subjectId).orElseThrow());
        a.setTeacher(staff.findById(teacherId).orElseThrow());
        a.setPeriodsPerWeek(periodsPerWeek);
        a.setActive(true);
        allocations.save(a);

        flash.addFlashAttribute("message", "Subject allocated.");
        return "redirect:/academics/classes/" + classId;
    }

    @PostMapping("/allocations/{id}/remove")
    public String removeAllocation(@PathVariable Long id, @RequestParam Long classId) {
        allocations.deleteById(id);
        return "redirect:/academics/classes/" + classId;
    }
}
