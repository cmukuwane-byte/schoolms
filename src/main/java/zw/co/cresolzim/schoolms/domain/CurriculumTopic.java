package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.Phase;

/**
 * One row of the scheme of work: what a given grade or form covers in a given
 * subject, in a given term and week. The head maintains this; teachers draw
 * their weekly lesson plans from it, which is what keeps parallel streams
 * teaching the same thing at the same time.
 */
@Entity
@Table(name = "curriculum_topic",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_curriculum_slot",
           columnNames = {"subject_id", "grade_level", "term_number", "week_number"}))
@Getter @Setter @NoArgsConstructor
public class CurriculumTopic extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Phase phase;

    /** 0 for ECD, 1–7 primary, 1–6 secondary. Read together with phase. */
    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Column(name = "term_number", nullable = false)
    private Integer termNumber;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "sub_topic", length = 300)
    private String subTopic;

    /** What the learner should be able to do by the end of the week. */
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String objectives;

    /** Ministry syllabus competencies, where the syllabus names them. */
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String competencies;

    @Column(name = "suggested_activities", columnDefinition = "NVARCHAR(MAX)")
    private String suggestedActivities;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String resources;

    /** Periods the syllabus expects this to take. Helps spot an overloaded week. */
    @Column(name = "suggested_periods")
    private Integer suggestedPeriods;

    @Column(name = "syllabus_reference", length = 80)
    private String syllabusReference;

    @Column(nullable = false)
    private boolean active = true;

    @Override public String toString() {
        return "Week " + weekNumber + ": " + topic;
    }
}
