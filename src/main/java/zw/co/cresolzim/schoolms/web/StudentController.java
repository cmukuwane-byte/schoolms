package zw.co.cresolzim.schoolms.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.NotFoundException;
import zw.co.cresolzim.schoolms.service.RuleViolationException;
import zw.co.cresolzim.schoolms.service.StudentService;

import java.util.List;

@Controller
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;
    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final StudentGuardianRepository links;
    private final SchoolClassRepository classes;
    private final EnrolmentRepository enrolments;
    private final StudentTransferRepository transfers;
    private final DisciplinaryCaseRepository cases;
    private final AcademicYearRepository years;
    private final BookLoanRepository loans;
    private final ReportCardRepository reportCards;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) StudentStatus status,
                       @RequestParam(required = false) Long classId,
                       Model model) {

        StudentStatus effective = status == null ? StudentStatus.ENROLLED : status;
        List<Student> results;
        if (q != null && !q.isBlank())        results = students.search(q.trim(), effective);
        else if (classId != null)             results = students
                .findByCurrentClassIdAndStatusOrderBySurnameAscFirstNameAsc(classId, effective);
        else                                  results = students.findByStatusOrderBySurnameAsc(effective);

        model.addAttribute("students", results);
        model.addAttribute("q", q);
        model.addAttribute("status", effective);
        model.addAttribute("classId", classId);
        model.addAttribute("statuses", StudentStatus.values());
        model.addAttribute("classes", currentClasses());
        return "students/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("student", new Student());
        model.addAttribute("classes", currentClasses());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("residencies", Residency.values());
        return "students/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("student") Student student, BindingResult binding,
                         @RequestParam Long classId, RedirectAttributes flash, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("classes", currentClasses());
            model.addAttribute("genders", Gender.values());
            model.addAttribute("residencies", Residency.values());
            return "students/form";
        }
        Student saved = service.enrol(student, classId, CurrentUser.name());
        flash.addFlashAttribute("message",
                saved.getFullName() + " enrolled as " + saved.getStudentNumber() + ".");
        return "redirect:/students/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Student s = service.require(id);
        model.addAttribute("student", s);
        model.addAttribute("guardians", links.findByStudentId(id));
        model.addAttribute("allGuardians", guardians.findAll(
                org.springframework.data.domain.Sort.by("surname", "firstName")));
        model.addAttribute("enrolments", enrolments.findByStudentIdOrderByAcademicYearYearDesc(id));
        model.addAttribute("transfers", transfers.findByStudentIdOrderByEffectiveDateDesc(id));
        model.addAttribute("cases", cases.findByStudentIdOrderByIncidentDateDesc(id));
        model.addAttribute("loans", loans.findByStudentIdOrderByIssueDateDesc(id));
        model.addAttribute("reports", reportCards.findByStudentIdOrderByTermIdDesc(id));
        model.addAttribute("classes", currentClasses());
        return "students/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("student", service.require(id));
        model.addAttribute("classes", currentClasses());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("residencies", Residency.values());
        return "students/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute Student student,
                         RedirectAttributes flash) {
        student.setId(id);
        students.save(student);
        flash.addFlashAttribute("message", "Record updated.");
        return "redirect:/students/" + id;
    }

    @PostMapping("/{id}/class")
    public String changeClass(@PathVariable Long id, @RequestParam Long classId, RedirectAttributes flash) {
        service.changeClass(id, classId, CurrentUser.name());
        flash.addFlashAttribute("message", "Class changed.");
        return "redirect:/students/" + id;
    }

    /* ---------------------------------------------------------- transfers */

    @GetMapping("/{id}/transfer")
    public String transferForm(@PathVariable Long id, Model model) {
        model.addAttribute("student", service.require(id));
        model.addAttribute("transfer", new StudentTransfer());
        return "students/transfer";
    }

    @PostMapping("/{id}/transfer")
    public String transferOut(@PathVariable Long id, @ModelAttribute StudentTransfer transfer,
                              RedirectAttributes flash) {
        service.transferOut(id, transfer, CurrentUser.name());
        flash.addFlashAttribute("message",
                "Transfer recorded. Print the transfer letter from the learner's record.");
        return "redirect:/students/" + id;
    }

    /* --------------------------------------------------------- discipline */

    @GetMapping("/{id}/discipline/new")
    public String disciplineForm(@PathVariable Long id, Model model) {
        model.addAttribute("student", service.require(id));
        model.addAttribute("case", new DisciplinaryCase());
        model.addAttribute("outcomes", DisciplinaryOutcome.values());
        return "students/discipline";
    }

    @PostMapping("/{id}/discipline")
    public String recordDiscipline(@PathVariable Long id, @ModelAttribute("case") DisciplinaryCase c,
                                   RedirectAttributes flash) {
        service.recordDiscipline(id, c, CurrentUser.name());
        flash.addFlashAttribute("message", "Disciplinary outcome recorded: " + c.getOutcome() + ".");
        return "redirect:/students/" + id;
    }

    @PostMapping("/{id}/readmit")
    public String readmit(@PathVariable Long id, @RequestParam(required = false) Long classId,
                          RedirectAttributes flash) {
        service.readmit(id, classId, CurrentUser.name());
        flash.addFlashAttribute("message", "Learner readmitted.");
        return "redirect:/students/" + id;
    }

    /* ---------------------------------------------------------- guardians */

    /**
     * Adds a guardian to a learner in one step: either linking an existing
     * guardian record, or creating a new one from the name and contact details
     * typed in. Matching on national ID or phone number first stops the same
     * parent being entered once per child.
     */
    @PostMapping("/{id}/guardians")
    public String addGuardian(@PathVariable Long id,
                              @RequestParam(required = false) Long guardianId,
                              @RequestParam(required = false) String firstName,
                              @RequestParam(required = false) String surname,
                              @RequestParam(required = false) String primaryPhone,
                              @RequestParam(required = false) String alternatePhone,
                              @RequestParam(required = false) String email,
                              @RequestParam(required = false) String nationalId,
                              @RequestParam(required = false) String occupation,
                              @RequestParam(required = false) String residentialAddress,
                              @RequestParam String relationship,
                              @RequestParam(defaultValue = "false") boolean primaryContact,
                              @RequestParam(defaultValue = "false") boolean feePayer,
                              @RequestParam(defaultValue = "true") boolean mayCollect,
                              RedirectAttributes flash) {

        Student student = service.require(id);
        Guardian guardian;

        if (guardianId != null) {
            guardian = guardians.findById(guardianId)
                    .orElseThrow(() -> new NotFoundException("Guardian", guardianId));
        } else {
            if (isBlank(firstName) || isBlank(surname) || isBlank(primaryPhone)) {
                throw new RuleViolationException(
                        "A new guardian needs at least a first name, surname and phone number.");
            }

            // Reuse an existing record where we can recognise the same person.
            guardian = null;
            if (!isBlank(nationalId)) {
                guardian = guardians.findByNationalId(nationalId.trim()).orElse(null);
            }
            if (guardian == null) {
                guardian = guardians.findByPrimaryPhone(primaryPhone.trim()).orElse(null);
            }
            if (guardian == null) {
                guardian = new Guardian();
                guardian.setCreatedBy(CurrentUser.name());
            }

            guardian.setFirstName(firstName.trim());
            guardian.setSurname(surname.trim());
            guardian.setPrimaryPhone(primaryPhone.trim());
            guardian.setAlternatePhone(blankToNull(alternatePhone));
            guardian.setEmail(blankToNull(email));
            guardian.setNationalId(blankToNull(nationalId));
            guardian.setOccupation(blankToNull(occupation));
            guardian.setResidentialAddress(blankToNull(residentialAddress));
            guardian = guardians.save(guardian);
        }

        final Long gid = guardian.getId();
        if (links.findByStudentId(id).stream()
                .anyMatch(l -> l.getGuardian().getId().equals(gid))) {
            throw new RuleViolationException(
                    guardian.getFullName() + " is already linked to " + student.getFullName() + ".");
        }

        // Only one primary contact per learner, or nobody knows who to ring.
        if (primaryContact) {
            links.findByStudentId(id).stream()
                 .filter(StudentGuardian::isPrimaryContact)
                 .forEach(l -> { l.setPrimaryContact(false); links.save(l); });
        }

        StudentGuardian sg = new StudentGuardian();
        sg.setStudent(student);
        sg.setGuardian(guardian);
        sg.setRelationship(relationship);
        sg.setPrimaryContact(primaryContact);
        sg.setFeePayer(feePayer);
        sg.setMayCollect(mayCollect);
        sg.setCreatedBy(CurrentUser.name());
        links.save(sg);

        flash.addFlashAttribute("message",
                guardian.getFullName() + " linked as " + relationship + ".");
        return "redirect:/students/" + id;
    }

    @PostMapping("/{id}/guardians/{linkId}/remove")
    public String unlinkGuardian(@PathVariable Long id, @PathVariable Long linkId,
                                 RedirectAttributes flash) {
        links.findById(linkId).ifPresent(links::delete);
        flash.addFlashAttribute("message", "Guardian unlinked from this learner.");
        return "redirect:/students/" + id;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String blankToNull(String s) { return isBlank(s) ? null : s.trim(); }

    private List<SchoolClass> currentClasses() {
        return years.findByCurrentTrue()
                .map(y -> classes.findByAcademicYearIdAndActiveTrueOrderByPhaseAscGradeLevelAscNameAsc(y.getId()))
                .orElse(List.of());
    }
}
