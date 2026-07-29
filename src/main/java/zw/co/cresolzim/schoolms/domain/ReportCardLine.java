package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;

/** One subject row on a report card. */
@Entity @Table(name = "report_card_line")
@Getter @Setter @NoArgsConstructor
public class ReportCardLine extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_card_id")
    private ReportCard reportCard;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "continuous_assessment_percent")
    private Double continuousAssessmentPercent;

    @Column(name = "exam_percent")
    private Double examPercent;

    @Column(name = "final_percent")
    private Double finalPercent;

    @Column(length = 5)
    private String grade;

    @Column(name = "subject_position")
    private Integer subjectPosition;

    @Column(name = "class_average")
    private Double classAverage;

    @Column(name = "teacher_comment", length = 300)
    private String teacherComment;

    @Column(name = "teacher_initials", length = 10)
    private String teacherInitials;
}
