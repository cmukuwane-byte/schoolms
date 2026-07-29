package zw.co.cresolzim.schoolms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.LessonPlanStatus;
import zw.co.cresolzim.schoolms.repo.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Weekly lesson plans and the review cycle around them.
 *
 * The rules here are the point of the module, not the storage: a teacher may
 * only plan for a class and subject they are actually allocated, may not alter
 * a plan once it is with the head, and an approved plan is frozen so the record
 * of what was planned cannot be rewritten after the lesson was taught.
 */
@Service
@RequiredArgsConstructor
public class LessonPlanService {

    private final LessonPlanRepository plans;
    private final ClassSubjectAllocationRepository allocations;
    private final CurriculumTopicRepository curriculum;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final StaffRepository staff;
    private final TermRepository terms;

    /* ------------------------------------------------------------- reading */

    @Transactional(readOnly = true)
    public List<LessonPlan> mine(Long teacherId, Long termId) {
        return plans.findByTeacherIdAndTermIdOrderByWeekNumberDescSubjectNameAsc(teacherId, termId);
    }

    @Transactional(readOnly = true)
    public List<LessonPlan> awaitingReview() {
        return plans.findByStatusOrderBySubmittedAtAsc(LessonPlanStatus.SUBMITTED);
    }

    @Transactional(readOnly = true)
    public LessonPlan get(Long id) {
        return plans.findWithDetailById(id).orElseThrow(() -> new NotFoundException("Lesson plan", id));
    }

    /** {teacher, drafted, submitted, approved, returned} for the head's board. */
    @Transactional(readOnly = true)
    public List<Object[]> submissionBoard(Long termId) {
        Map<Staff, long[]> tally = new LinkedHashMap<>();
        for (Object[] row : plans.submissionCounts(termId)) {
            Staff teacher = (Staff) row[0];
            LessonPlanStatus status = (LessonPlanStatus) row[1];
            long count = ((Number) row[2]).longValue();
            long[] cells = tally.computeIfAbsent(teacher, k -> new long[4]);
            switch (status) {
                case DRAFT     -> cells[0] += count;
                case SUBMITTED -> cells[1] += count;
                case APPROVED  -> cells[2] += count;
                case RETURNED  -> cells[3] += count;
            }
        }
        List<Object[]> out = new ArrayList<>();
        tally.forEach((teacher, c) -> out.add(new Object[]{teacher, c[0], c[1], c[2], c[3]}));
        out.sort(Comparator.comparing(r -> ((Staff) r[0]).getSurname(), String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /** The subject-and-class pairs this teacher is allowed to plan for. */
    @Transactional(readOnly = true)
    public List<ClassSubjectAllocation> allocationsOf(Long teacherId) {
        return allocations.findByTeacherIdAndActiveTrue(teacherId);
    }

    /* ------------------------------------------------------------- writing */

    @Transactional
    public LessonPlan saveDraft(LessonPlan form, Long teacherId, String actor) {
        Term term = terms.findByCurrentTrue()
                .orElseThrow(() -> new RuleViolationException("No term is marked current."));

        SchoolClass sc = classes.findById(form.getSchoolClass().getId())
                .orElseThrow(() -> new NotFoundException("Class", form.getSchoolClass().getId()));
        Subject subject = subjects.findById(form.getSubject().getId())
                .orElseThrow(() -> new NotFoundException("Subject", form.getSubject().getId()));
        Staff teacher = staff.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("Staff", teacherId));

        requireAllocation(teacherId, sc.getId(), subject.getId());
        requireSaneWeek(form.getWeekNumber());

        LessonPlan plan;
        if (form.getId() != null) {
            plan = plans.findById(form.getId())
                    .orElseThrow(() -> new NotFoundException("Lesson plan", form.getId()));

            if (!plan.getTeacher().getId().equals(teacherId)) {
                throw new RuleViolationException("This plan belongs to another teacher.");
            }
            if (!plan.isEditableByTeacher()) {
                throw new RuleViolationException(
                        plan.getStatus() == LessonPlanStatus.APPROVED
                            ? "This plan has been approved and can no longer be edited."
                            : "This plan is with the head for review. Ask for it to be returned first.");
            }
        } else {
            // One plan per class, subject, term and week.
            plans.findBySchoolClassIdAndSubjectIdAndTermIdAndWeekNumber(
                            sc.getId(), subject.getId(), term.getId(), form.getWeekNumber())
                 .ifPresent(existing -> {
                     throw new RuleViolationException(
                             "A plan already exists for week " + form.getWeekNumber() + ", "
                             + subject.getName() + " with " + sc.getName() + ".");
                 });
            plan = new LessonPlan();
            plan.setCreatedBy(actor);
            plan.setStatus(LessonPlanStatus.DRAFT);
        }

        plan.setTeacher(teacher);
        plan.setSchoolClass(sc);
        plan.setSubject(subject);
        plan.setTerm(term);
        plan.setWeekNumber(form.getWeekNumber());
        plan.setWeekStarting(form.getWeekStarting() == null
                ? mondayOfWeek(term, form.getWeekNumber()) : form.getWeekStarting());
        plan.setTopic(form.getTopic());
        plan.setPeriodsPlanned(form.getPeriodsPlanned());
        plan.setObjectives(form.getObjectives());
        plan.setTeacherActivities(form.getTeacherActivities());
        plan.setLearnerActivities(form.getLearnerActivities());
        plan.setMediaAndResources(form.getMediaAndResources());
        plan.setAssessmentMethod(form.getAssessmentMethod());
        plan.setHomework(form.getHomework());
        plan.setReflection(form.getReflection());

        // Attach the matching scheme-of-work row if one exists, so coverage reporting works.
        curriculum.findBySubjectIdAndGradeLevelAndTermNumberAndWeekNumber(
                        subject.getId(), sc.getGradeLevel(), term.getTermNumber(), form.getWeekNumber())
                  .ifPresent(plan::setCurriculumTopic);

        return plans.save(plan);
    }

    @Transactional
    public void submit(Long planId, Long teacherId) {
        LessonPlan plan = get(planId);

        if (!plan.getTeacher().getId().equals(teacherId)) {
            throw new RuleViolationException("This plan belongs to another teacher.");
        }
        if (!plan.isEditableByTeacher()) {
            throw new RuleViolationException("This plan has already been submitted.");
        }
        if (isBlank(plan.getObjectives())) {
            throw new RuleViolationException("Write the lesson objectives before submitting.");
        }
        if (isBlank(plan.getTeacherActivities()) && isBlank(plan.getLearnerActivities())) {
            throw new RuleViolationException("Describe what you and the learners will do.");
        }

        plan.setStatus(LessonPlanStatus.SUBMITTED);
        plan.setSubmittedAt(LocalDateTime.now());
        plan.setReviewComment(null);
        plans.save(plan);
    }

    @Transactional
    public void approve(Long planId, Long reviewerStaffId, String comment) {
        LessonPlan plan = get(planId);
        if (plan.getStatus() != LessonPlanStatus.SUBMITTED) {
            throw new RuleViolationException("Only a submitted plan can be approved.");
        }
        plan.setStatus(LessonPlanStatus.APPROVED);
        stampReview(plan, reviewerStaffId, comment);
    }

    @Transactional
    public void returnForRevision(Long planId, Long reviewerStaffId, String comment) {
        LessonPlan plan = get(planId);
        if (plan.getStatus() != LessonPlanStatus.SUBMITTED) {
            throw new RuleViolationException("Only a submitted plan can be returned.");
        }
        if (isBlank(comment)) {
            throw new RuleViolationException(
                    "Say what needs changing. Returning a plan without a reason helps nobody.");
        }
        plan.setStatus(LessonPlanStatus.RETURNED);
        stampReview(plan, reviewerStaffId, comment);
    }

    /** Lets a head unlock an approved plan when something genuinely needs correcting. */
    @Transactional
    public void reopen(Long planId, Long reviewerStaffId, String reason) {
        LessonPlan plan = get(planId);
        if (plan.getStatus() != LessonPlanStatus.APPROVED) {
            throw new RuleViolationException("Only an approved plan needs reopening.");
        }
        if (isBlank(reason)) {
            throw new RuleViolationException("Record why the plan is being reopened.");
        }
        plan.setStatus(LessonPlanStatus.RETURNED);
        stampReview(plan, reviewerStaffId, "Reopened: " + reason);
    }

    /* ------------------------------------------------------------- helpers */

    private void stampReview(LessonPlan plan, Long reviewerStaffId, String comment) {
        if (reviewerStaffId != null) {
            staff.findById(reviewerStaffId).ifPresent(plan::setReviewedBy);
        }
        plan.setReviewedAt(LocalDateTime.now());
        plan.setReviewComment(isBlank(comment) ? null : comment.trim());
        plans.save(plan);
    }

    private void requireAllocation(Long teacherId, Long classId, Long subjectId) {
        boolean allowed = allocations.findByTeacherIdAndActiveTrue(teacherId).stream()
                .anyMatch(a -> a.getSchoolClass().getId().equals(classId)
                            && a.getSubject().getId().equals(subjectId));
        if (!allowed) {
            throw new RuleViolationException(
                    "You are not allocated to teach that subject to that class. "
                    + "Ask the head to update the allocation first.");
        }
    }

    private void requireSaneWeek(Integer week) {
        if (week == null || week < 1 || week > 15) {
            throw new RuleViolationException("Week number must be between 1 and 15.");
        }
    }

    /** Best guess at the Monday of a given teaching week, from the term start. */
    private LocalDate mondayOfWeek(Term term, int weekNumber) {
        LocalDate start = term.getStartDate();
        LocalDate monday = start.minusDays(start.getDayOfWeek().getValue() - 1L);
        return monday.plusWeeks(weekNumber - 1L);
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
