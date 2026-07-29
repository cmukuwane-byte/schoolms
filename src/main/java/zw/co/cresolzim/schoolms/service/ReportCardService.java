package zw.co.cresolzim.schoolms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.AttendanceStatus;
import zw.co.cresolzim.schoolms.domain.Enums.StudentStatus;
import zw.co.cresolzim.schoolms.repo.*;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportCardService {

    private final ReportCardRepository reportCards;
    private final AssessmentMarkRepository marks;
    private final SubjectRepository subjects;
    private final StudentRepository students;
    private final SchoolClassRepository classes;
    private final TermRepository terms;
    private final GradeBandRepository bands;
    private final StudentAttendanceRepository attendance;
    private final ClassSubjectAllocationRepository allocations;

    /**
     * Generates or refreshes report cards for a whole class, then ranks them.
     * Ties share a position, and the next position skips accordingly — the way
     * a class list is normally read out.
     */
    @Transactional
    public int generateForClass(Long classId, Long termId, String actor) {
        SchoolClass sc = classes.findById(classId).orElseThrow(() -> new NotFoundException("Class", classId));
        Term term = terms.findById(termId).orElseThrow(() -> new NotFoundException("Term", termId));

        List<Student> roll = students.findByCurrentClassIdAndStatusOrderBySurnameAscFirstNameAsc(
                classId, StudentStatus.ENROLLED);
        if (roll.isEmpty()) throw new RuleViolationException("There are no enrolled learners in " + sc.getName() + ".");

        List<GradeBand> scale = bands.findByPhaseOrderByMinPercentDesc(sc.getPhase());
        Map<Long, Subject> subjectsById = new HashMap<>();
        for (ClassSubjectAllocation a : allocations.findBySchoolClassIdAndActiveTrue(classId)) {
            subjectsById.put(a.getSubject().getId(), a.getSubject());
        }

        List<ReportCard> generated = new ArrayList<>();

        for (Student s : roll) {
            ReportCard rc = reportCards.findByStudentIdAndTermId(s.getId(), termId).orElseGet(ReportCard::new);
            if (rc.isReleased()) { generated.add(rc); continue; }   // never overwrite a released report

            rc.setStudent(s);
            rc.setTerm(term);
            rc.setSchoolClass(sc);
            rc.setCreatedBy(actor);
            rc.getLines().clear();

            double total = 0; int counted = 0;
            for (Object[] row : marks.termAveragesBySubject(s.getId(), termId)) {
                Long subjectId = (Long) row[0];
                Double percent = row[1] == null ? null : round(((Number) row[1]).doubleValue());
                if (percent == null) continue;

                ReportCardLine line = new ReportCardLine();
                line.setReportCard(rc);
                line.setSubject(subjectsById.getOrDefault(subjectId,
                        subjects.findById(subjectId).orElse(null)));
                line.setFinalPercent(percent);
                line.setGrade(gradeFor(scale, percent));
                rc.getLines().add(line);

                total += percent; counted++;
            }

            rc.setTotalMarks(round(total));
            rc.setAveragePercent(counted == 0 ? null : round(total / counted));
            rc.setClassSize(roll.size());
            rc.setDaysPresent((int) attendance.countByStudentIdAndTermIdAndStatus(
                    s.getId(), termId, AttendanceStatus.PRESENT));
            rc.setDaysAbsent((int) attendance.countByStudentIdAndTermIdAndStatus(
                    s.getId(), termId, AttendanceStatus.ABSENT));
            rc.setNextTermBegins(nextTermStart(term));

            generated.add(reportCards.save(rc));
        }

        rank(generated);
        reportCards.saveAll(generated);
        return generated.size();
    }

    /** Dense-skip ranking on average percentage; learners with no marks are left unranked. */
    private void rank(List<ReportCard> cards) {
        List<ReportCard> ranked = cards.stream()
                .filter(c -> c.getAveragePercent() != null)
                .sorted(Comparator.comparingDouble(ReportCard::getAveragePercent).reversed())
                .toList();

        int position = 0; int index = 0; Double previous = null;
        for (ReportCard c : ranked) {
            index++;
            if (previous == null || !previous.equals(c.getAveragePercent())) position = index;
            c.setPositionInClass(position);
            previous = c.getAveragePercent();
        }
    }

    private String gradeFor(List<GradeBand> scale, double percent) {
        for (GradeBand b : scale) {
            if (percent >= b.getMinPercent() && percent <= b.getMaxPercent()) return b.getSymbol();
        }
        return "-";
    }

    private LocalDate nextTermStart(Term term) {
        return terms.findByAcademicYearIdOrderByTermNumberAsc(term.getAcademicYear().getId()).stream()
                .filter(t -> t.getTermNumber() > term.getTermNumber())
                .map(Term::getStartDate).findFirst().orElse(null);
    }

    /** Releasing makes the report visible in the guardian portal and printable. */
    @Transactional
    public void release(Long reportCardId) {
        ReportCard rc = reportCards.findById(reportCardId)
                .orElseThrow(() -> new NotFoundException("Report card", reportCardId));
        if (rc.getClassTeacherComment() == null || rc.getClassTeacherComment().isBlank()) {
            throw new RuleViolationException("Add the class teacher's comment before releasing this report.");
        }
        rc.setReleased(true);
        rc.setReleasedOn(LocalDate.now());
        reportCards.save(rc);
    }

    @Transactional
    public int releaseClass(Long classId, Long termId) {
        int released = 0;
        for (ReportCard rc : reportCards.findByTermIdAndSchoolClassIdOrderByPositionInClassAsc(termId, classId)) {
            if (!rc.isReleased() && rc.getClassTeacherComment() != null && !rc.getClassTeacherComment().isBlank()) {
                rc.setReleased(true);
                rc.setReleasedOn(LocalDate.now());
                reportCards.save(rc);
                released++;
            }
        }
        return released;
    }

    private double round(double d) { return Math.round(d * 100.0) / 100.0; }
}
