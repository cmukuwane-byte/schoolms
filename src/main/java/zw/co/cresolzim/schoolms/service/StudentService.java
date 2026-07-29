package zw.co.cresolzim.schoolms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository students;
    private final EnrolmentRepository enrolments;
    private final SchoolClassRepository classes;
    private final StudentTransferRepository transfers;
    private final DisciplinaryCaseRepository cases;
    private final AcademicYearRepository years;
    private final BookLoanRepository loans;

    /* ------------------------------------------------------------ enrolment */

    /**
     * Admits a learner and places them in a class. Rejects the admission if the
     * class is already at capacity, because an over-full register causes trouble
     * later at mark entry and boarding allocation.
     */
    @Transactional
    public Student enrol(Student student, Long classId, String actor) {
        SchoolClass sc = classes.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class", classId));

        long current = enrolments.countBySchoolClassIdAndActiveTrue(classId);
        if (current >= sc.getMaxCapacity()) {
            throw new RuleViolationException(
                    sc.getName() + " is full (" + current + " of " + sc.getMaxCapacity() + " places taken).");
        }
        if (student.getStudentNumber() == null || student.getStudentNumber().isBlank()) {
            student.setStudentNumber(nextStudentNumber(sc.getPhase()));
        }
        if (students.existsByStudentNumber(student.getStudentNumber())) {
            throw new RuleViolationException("Student number " + student.getStudentNumber() + " is already in use.");
        }

        student.setPhase(sc.getPhase());
        student.setCurrentClass(sc);
        student.setStatus(StudentStatus.ENROLLED);
        if (student.getAdmissionDate() == null) student.setAdmissionDate(LocalDate.now());
        student.setCreatedBy(actor);
        Student saved = students.save(student);

        Enrolment e = new Enrolment();
        e.setStudent(saved);
        e.setSchoolClass(sc);
        e.setAcademicYear(sc.getAcademicYear());
        e.setResidency(saved.getResidency());
        e.setEnrolledOn(saved.getAdmissionDate());
        e.setCreatedBy(actor);
        enrolments.save(e);

        return saved;
    }

    /** CA/2026/0001 style admission number, sequential within the year. */
    @Transactional(readOnly = true)
    public String nextStudentNumber(Phase phase) {
        int year = years.findByCurrentTrue().map(AcademicYear::getYear).orElse(LocalDate.now().getYear());
        String prefix = (phase == Phase.PRIMARY ? "P" : "S") + "/" + year + "/";
        long count = students.count() + 1;
        String candidate;
        do {
            candidate = prefix + String.format("%04d", count++);
        } while (students.existsByStudentNumber(candidate));
        return candidate;
    }

    /** Moves a learner between classes mid-year without creating a second enrolment row. */
    @Transactional
    public void changeClass(Long studentId, Long newClassId, String actor) {
        Student s = require(studentId);
        SchoolClass sc = classes.findById(newClassId)
                .orElseThrow(() -> new NotFoundException("Class", newClassId));

        if (sc.getPhase() != s.getPhase()) {
            throw new RuleViolationException(
                    "Cannot move a " + s.getPhase() + " learner into a " + sc.getPhase() + " class.");
        }
        s.setCurrentClass(sc);
        students.save(s);

        enrolments.findByStudentIdAndAcademicYearId(studentId, sc.getAcademicYear().getId())
                .ifPresent(e -> { e.setSchoolClass(sc); enrolments.save(e); });
    }

    /* ------------------------------------------------------------ transfers */

    /** Transfer out. Blocked until the learner has returned every library book. */
    @Transactional
    public StudentTransfer transferOut(Long studentId, StudentTransfer t, String actor) {
        Student s = require(studentId);

        long open = loans.openLoansForStudent(studentId);
        if (open > 0) {
            throw new RuleViolationException(
                    s.getFullName() + " still has " + open + " library book(s) out. Clear the library first.");
        }

        t.setStudent(s);
        t.setDirection(TransferDirection.OUT);
        t.setLibraryCleared(true);
        t.setApprovedBy(actor);
        t.setCreatedBy(actor);

        s.setStatus(StudentStatus.TRANSFERRED_OUT);
        s.setExitDate(t.getEffectiveDate());
        s.setCurrentClass(null);
        students.save(s);

        enrolments.findByStudentIdAndAcademicYearId(studentId,
                        years.findByCurrentTrue().map(AcademicYear::getId).orElse(null))
                .ifPresent(e -> {
                    e.setActive(false);
                    e.setEndedOn(t.getEffectiveDate());
                    e.setYearEndOutcome("LEFT");
                    enrolments.save(e);
                });

        return transfers.save(t);
    }

    /** Transfer in from another school — records the origin then runs a normal enrolment. */
    @Transactional
    public Student transferIn(Student student, Long classId, StudentTransfer t, String actor) {
        Student saved = enrol(student, classId, actor);
        t.setStudent(saved);
        t.setDirection(TransferDirection.IN);
        t.setCreatedBy(actor);
        transfers.save(t);
        return saved;
    }

    /* ------------------------------------------------------------ discipline */

    /**
     * Records a disciplinary decision. An expulsion closes the learner's record
     * and vacates their class place; a suspension leaves them enrolled.
     */
    @Transactional
    public DisciplinaryCase recordDiscipline(Long studentId, DisciplinaryCase c, String actor) {
        Student s = require(studentId);
        c.setStudent(s);
        c.setDecidedBy(actor);
        c.setCreatedBy(actor);

        switch (c.getOutcome()) {
            case EXPULSION -> {
                if (c.getHearingDate() == null) {
                    throw new RuleViolationException(
                            "An expulsion needs a hearing date on record before it can be saved.");
                }
                s.setStatus(StudentStatus.EXPELLED);
                s.setExitDate(c.getIncidentDate());
                s.setCurrentClass(null);
                enrolments.findByStudentIdAndAcademicYearId(studentId,
                                years.findByCurrentTrue().map(AcademicYear::getId).orElse(null))
                        .ifPresent(e -> { e.setActive(false); e.setYearEndOutcome("LEFT"); enrolments.save(e); });
            }
            case SUSPENSION -> {
                if (c.getSuspensionStart() == null || c.getSuspensionEnd() == null) {
                    throw new RuleViolationException("A suspension needs both a start and an end date.");
                }
                s.setStatus(StudentStatus.SUSPENDED);
            }
            default -> { /* warning and detention leave status untouched */ }
        }
        students.save(s);
        return cases.save(c);
    }

    /** Brings a suspended learner back onto the register. */
    @Transactional
    public void readmit(Long studentId, Long classId, String actor) {
        Student s = require(studentId);
        if (s.getStatus() == StudentStatus.EXPELLED) {
            throw new RuleViolationException(
                    "An expelled learner needs a fresh admission, not a readmission.");
        }
        s.setStatus(StudentStatus.ENROLLED);
        if (classId != null) changeClass(studentId, classId, actor);
        students.save(s);
    }

    /* ------------------------------------------------------------ promotion */

    /**
     * Year-end promotion for a whole class. Learners marked to repeat stay where
     * they are; everyone else moves to the next grade in the target year.
     */
    @Transactional
    public int promoteClass(Long fromClassId, Long targetYearId, List<Long> repeaterIds, String actor) {
        SchoolClass from = classes.findById(fromClassId)
                .orElseThrow(() -> new NotFoundException("Class", fromClassId));

        List<SchoolClass> targets = classes.findPromotionTargets(
                targetYearId, from.getPhase(), from.getGradeLevel() + 1, from.getStream());
        if (targets.isEmpty()) {
            throw new RuleViolationException(
                    "No class exists for the next grade up in the target year. Create it first.");
        }
        SchoolClass to = targets.get(0);

        int moved = 0;
        for (Student s : students.findByCurrentClassIdAndStatusOrderBySurnameAscFirstNameAsc(
                fromClassId, StudentStatus.ENROLLED)) {

            boolean repeats = repeaterIds != null && repeaterIds.contains(s.getId());

            enrolments.findByStudentIdAndAcademicYearId(s.getId(), from.getAcademicYear().getId())
                    .ifPresent(e -> {
                        e.setActive(false);
                        e.setYearEndOutcome(repeats ? "REPEATED" : "PROMOTED");
                        enrolments.save(e);
                    });

            SchoolClass destination = repeats ? from : to;
            s.setCurrentClass(destination);
            students.save(s);

            Enrolment ne = new Enrolment();
            ne.setStudent(s);
            ne.setSchoolClass(destination);
            ne.setAcademicYear(destination.getAcademicYear());
            ne.setResidency(s.getResidency());
            ne.setEnrolledOn(LocalDate.now());
            ne.setCreatedBy(actor);
            enrolments.save(ne);
            moved++;
        }
        return moved;
    }

    public Student require(Long id) {
        return students.findById(id).orElseThrow(() -> new NotFoundException("Student", id));
    }
}
