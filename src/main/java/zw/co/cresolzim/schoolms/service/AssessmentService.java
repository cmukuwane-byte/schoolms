package zw.co.cresolzim.schoolms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.StudentStatus;
import zw.co.cresolzim.schoolms.repo.*;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessments;
    private final AssessmentMarkRepository marks;
    private final StudentRepository students;

    /**
     * Builds the mark sheet for a sitting: every enrolled learner in the class,
     * with any mark already captured attached.
     */
    @Transactional
    public List<AssessmentMark> markSheet(Long assessmentId) {
        Assessment a = assessments.findById(assessmentId)
                .orElseThrow(() -> new NotFoundException("Assessment", assessmentId));

        Map<Long, AssessmentMark> existing = new HashMap<>();
        for (AssessmentMark m : marks.findByAssessmentIdOrderByStudentSurnameAsc(assessmentId)) {
            existing.put(m.getStudent().getId(), m);
        }

        List<AssessmentMark> sheet = new ArrayList<>();
        for (Student s : students.findByCurrentClassIdAndStatusOrderBySurnameAscFirstNameAsc(
                a.getSchoolClass().getId(), StudentStatus.ENROLLED)) {
            AssessmentMark m = existing.get(s.getId());
            if (m == null) {
                m = new AssessmentMark();
                m.setAssessment(a);
                m.setStudent(s);
            }
            sheet.add(m);
        }
        return sheet;
    }

    /**
     * Saves entered marks. The sheet posts parallel arrays keyed by learner id,
     * so rows cannot drift onto the wrong names if the roll changed while the
     * page was open. Anything above the paper total or below zero is rejected —
     * a transposed digit here quietly corrupts the whole report card.
     */
    @Transactional
    public int saveMarks(Long assessmentId, List<Long> studentIds, List<Double> rawMarks,
                         List<String> presence, List<String> comments, String actor) {

        Assessment a = assessments.findById(assessmentId)
                .orElseThrow(() -> new NotFoundException("Assessment", assessmentId));

        if (a.isPublished()) {
            throw new RuleViolationException(
                    "This sitting has been published. Ask an administrator to reopen it before editing marks.");
        }
        if (a.getTerm().isMarksLocked()) {
            throw new RuleViolationException("Mark entry is closed for " + a.getTerm().getName() + ".");
        }
        if (studentIds == null || studentIds.isEmpty()) return 0;

        List<String> problems = new ArrayList<>();
        List<AssessmentMark> toSave = new ArrayList<>();

        for (int i = 0; i < studentIds.size(); i++) {
            Long studentId = studentIds.get(i);
            Student s = students.findById(studentId)
                    .orElseThrow(() -> new NotFoundException("Student", studentId));

            AssessmentMark m = marks.findByAssessmentIdAndStudentId(assessmentId, studentId)
                    .orElseGet(AssessmentMark::new);
            if (m.getId() == null) {
                m.setAssessment(a);
                m.setStudent(s);
                m.setCreatedBy(actor);
            }

            boolean absent = presence != null && i < presence.size() && "ABSENT".equals(presence.get(i));
            Double mark = rawMarks == null || i >= rawMarks.size() ? null : rawMarks.get(i);

            m.setAbsent(absent);
            m.setComment(comments == null || i >= comments.size() ? null : comments.get(i));

            if (absent) {
                m.setRawMark(null);
            } else if (mark == null) {
                m.setRawMark(null);
            } else if (mark < 0) {
                problems.add(s.getFullName() + ": negative mark");
            } else if (mark > a.getMaxMark()) {
                problems.add(s.getFullName() + ": " + mark + " is above the paper total of " + a.getMaxMark());
            } else {
                m.setRawMark(mark);
            }
            toSave.add(m);
        }

        if (!problems.isEmpty()) {
            throw new RuleViolationException("Fix these entries first — " + String.join("; ", problems));
        }
        marks.saveAll(toSave);
        return toSave.size();
    }

    /** Publishing freezes the marks and makes them count towards the report card. */
    @Transactional
    public void publish(Long assessmentId) {
        Assessment a = assessments.findById(assessmentId)
                .orElseThrow(() -> new NotFoundException("Assessment", assessmentId));

        long entered = marks.findByAssessmentIdOrderByStudentSurnameAsc(assessmentId).stream()
                .filter(m -> m.getRawMark() != null || m.isAbsent()).count();
        if (entered == 0) {
            throw new RuleViolationException("There are no marks to publish yet.");
        }
        a.setPublished(true);
        assessments.save(a);
    }

    @Transactional
    public void unpublish(Long assessmentId) {
        Assessment a = assessments.findById(assessmentId)
                .orElseThrow(() -> new NotFoundException("Assessment", assessmentId));
        a.setPublished(false);
        assessments.save(a);
    }

    /** Simple distribution for the teacher's own check: how did the class do? */
    @Transactional(readOnly = true)
    public Map<String, Object> statistics(Long assessmentId) {
        List<AssessmentMark> all = marks.findByAssessmentIdOrderByStudentSurnameAsc(assessmentId);
        List<Double> scored = all.stream()
                .filter(m -> !m.isAbsent() && m.getPercentage() != null)
                .map(AssessmentMark::getPercentage).sorted().toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("entered", scored.size());
        stats.put("absent", all.stream().filter(AssessmentMark::isAbsent).count());
        if (scored.isEmpty()) return stats;

        stats.put("average", round(scored.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
        stats.put("highest", scored.get(scored.size() - 1));
        stats.put("lowest", scored.get(0));
        stats.put("median", scored.get(scored.size() / 2));
        stats.put("passRate", round(scored.stream().filter(p -> p >= 50).count() * 100.0 / scored.size()));
        return stats;
    }

    private double round(double d) { return Math.round(d * 100.0) / 100.0; }
}
