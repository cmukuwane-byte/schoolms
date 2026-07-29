package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.AssessmentType;
import java.time.LocalDate;

/** A test or exam sitting for one class and one subject. */
@Entity @Table(name = "assessment")
@Getter @Setter @NoArgsConstructor
public class Assessment extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AssessmentType type;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Staff teacher;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "max_mark", nullable = false)
    private Double maxMark = 100.0;

    /** Share of the term mark this sitting carries, as a percentage. */
    @Column(name = "weight_percent", nullable = false)
    private Double weightPercent = 100.0;

    /** True once the teacher has submitted; blocks further edits. */
    @Column(nullable = false)
    private boolean published = false;
}
