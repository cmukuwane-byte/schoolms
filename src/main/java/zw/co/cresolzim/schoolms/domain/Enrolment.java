package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.Residency;
import java.time.LocalDate;

/** One row per learner per class per academic year. This is the promotion history. */
@Entity @Table(name = "enrolment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "academic_year_id"}))
@Getter @Setter @NoArgsConstructor
public class Enrolment extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private Residency residency = Residency.DAY;

    @Column(name = "enrolled_on", nullable = false)
    private LocalDate enrolledOn;

    @Column(name = "ended_on")
    private LocalDate endedOn;

    @Column(nullable = false)
    private boolean active = true;

    /** PROMOTED, REPEATED, LEFT — filled at year end. */
    @Column(name = "year_end_outcome", length = 20)
    private String yearEndOutcome;
}
