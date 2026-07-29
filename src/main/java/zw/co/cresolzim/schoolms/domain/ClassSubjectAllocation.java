package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;

/** Who teaches what to whom. One row per class + subject for a given year. */
@Entity @Table(name = "class_subject_allocation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"school_class_id", "subject_id"}))
@Getter @Setter @NoArgsConstructor
public class ClassSubjectAllocation extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Staff teacher;

    /** How many lessons a week this subject should get. Timetable builder uses it. */
    @Column(name = "periods_per_week", nullable = false)
    private Integer periodsPerWeek = 4;

    /** Weight of this subject in the class average, normally 1. */
    @Column(name = "weighting", nullable = false)
    private Double weighting = 1.0;

    @Column(nullable = false)
    private boolean active = true;
}
