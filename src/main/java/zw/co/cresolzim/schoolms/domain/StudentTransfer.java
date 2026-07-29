package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.TransferDirection;
import java.time.LocalDate;

/** Records a learner arriving from, or leaving for, another school. */
@Entity @Table(name = "student_transfer")
@Getter @Setter @NoArgsConstructor
public class StudentTransfer extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 5)
    private TransferDirection direction;

    @Column(name = "other_school", nullable = false, length = 150)
    private String otherSchool;

    @Column(name = "other_school_address", length = 250)
    private String otherSchoolAddress;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(length = 500)
    private String reason;

    /** Transfer letter reference number issued by the head. */
    @Column(name = "letter_reference", length = 60)
    private String letterReference;

    @Column(name = "clearance_given", nullable = false)
    private boolean clearanceGiven = false;

    @Column(name = "fees_cleared", nullable = false)
    private boolean feesCleared = false;

    @Column(name = "library_cleared", nullable = false)
    private boolean libraryCleared = false;

    @Column(name = "approved_by", length = 80)
    private String approvedBy;
}
