package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "assessment_mark",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assessment_id", "student_id"}))
@Getter @Setter @NoArgsConstructor
public class AssessmentMark extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    /** Null means the learner was absent for the sitting. */
    @Column(name = "raw_mark")
    private Double rawMark;

    @Column(nullable = false)
    private boolean absent = false;

    @Column(length = 250)
    private String comment;

    @Transient
    public Double getPercentage() {
        if (rawMark == null || assessment == null || assessment.getMaxMark() == null
                || assessment.getMaxMark() == 0) return null;
        return Math.round(rawMark / assessment.getMaxMark() * 10000.0) / 100.0;
    }
}
