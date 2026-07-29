package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.BorrowerType;
import zw.co.cresolzim.schoolms.domain.Enums.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Issue and return of one copy. Borrower is either a learner or a member of staff. */
@Entity @Table(name = "book_loan", indexes = {
        @Index(name = "ix_loan_status", columnList = "status"),
        @Index(name = "ix_loan_due", columnList = "due_date")
})
@Getter @Setter @NoArgsConstructor
public class BookLoan extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_copy_id")
    private BookCopy bookCopy;

    @Enumerated(EnumType.STRING) @Column(name = "borrower_type", nullable = false, length = 10)
    private BorrowerType borrowerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "renewal_count", nullable = false)
    private Integer renewalCount = 0;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private LoanStatus status = LoanStatus.ON_LOAN;

    @Column(name = "fine_amount", precision = 12, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(name = "fine_paid", nullable = false)
    private boolean finePaid = false;

    @Column(name = "issued_by", length = 80)
    private String issuedBy;

    @Column(name = "received_by", length = 80)
    private String receivedBy;

    @Column(length = 250)
    private String remarks;

    @Transient
    public String getBorrowerName() {
        if (borrowerType == BorrowerType.STUDENT && student != null) return student.getFullName();
        if (staff != null) return staff.getFullName();
        return "-";
    }

    @Transient
    public boolean isOverdue() {
        return returnDate == null && dueDate != null && dueDate.isBefore(LocalDate.now());
    }
}
