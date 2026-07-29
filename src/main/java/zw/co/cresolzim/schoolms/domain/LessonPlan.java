package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.LessonPlanStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A teacher's plan for one week, for one subject, with one class.
 * Written by the teacher, submitted, then approved or returned by the head.
 * Once approved it is locked: the record of what was planned must not shift
 * after the fact.
 */
@Entity
@Table(name = "lesson_plan",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_lesson_plan_week",
           columnNames = {"school_class_id", "subject_id", "term_id", "week_number"}))
@Getter @Setter @NoArgsConstructor
public class LessonPlan extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Staff teacher;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "week_starting", nullable = false)
    private LocalDate weekStarting;

    /** Optional link to the scheme of work, so coverage can be audited. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_topic_id")
    private CurriculumTopic curriculumTopic;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "periods_planned")
    private Integer periodsPlanned;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String objectives;

    @Column(name = "teacher_activities", columnDefinition = "NVARCHAR(MAX)")
    private String teacherActivities;

    @Column(name = "learner_activities", columnDefinition = "NVARCHAR(MAX)")
    private String learnerActivities;

    @Column(name = "media_and_resources", columnDefinition = "NVARCHAR(MAX)")
    private String mediaAndResources;

    @Column(name = "assessment_method", columnDefinition = "NVARCHAR(MAX)")
    private String assessmentMethod;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String homework;

    /** Written after the week, on how it actually went. */
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String reflection;

    /** Optional uploaded plan or worksheet, stored under app.upload.dir. */
    @Column(name = "attachment_path", length = 300)
    private String attachmentPath;

    @Column(name = "attachment_name", length = 200)
    private String attachmentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private LessonPlanStatus status = LessonPlanStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private Staff reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", columnDefinition = "NVARCHAR(MAX)")
    private String reviewComment;

    @Transient
    public boolean isEditableByTeacher() {
        return status == LessonPlanStatus.DRAFT || status == LessonPlanStatus.RETURNED;
    }

    @Transient
    public boolean isAwaitingReview() {
        return status == LessonPlanStatus.SUBMITTED;
    }
}
