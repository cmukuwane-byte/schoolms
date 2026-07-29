package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "term")
@Getter @Setter @NoArgsConstructor
public class Term extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    /** 1, 2 or 3 */
    @Column(name = "term_number", nullable = false)
    private Integer termNumber;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    private boolean current = false;

    /** Locks mark entry once report cards have been issued. */
    @Column(name = "marks_locked", nullable = false)
    private boolean marksLocked = false;

    @Override public String toString() { return name + " " + academicYear.getYear(); }
}
