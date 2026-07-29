package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;

/** Links a learner to a parent or guardian, with the relationship and who to call first. */
@Entity @Table(name = "student_guardian",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "guardian_id"}))
@Getter @Setter @NoArgsConstructor
public class StudentGuardian extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private Guardian guardian;

    /** Mother, Father, Aunt, Legal Guardian ... */
    @Column(nullable = false, length = 40)
    private String relationship;

    @Column(name = "primary_contact", nullable = false)
    private boolean primaryContact = false;

    @Column(name = "fee_payer", nullable = false)
    private boolean feePayer = false;

    /** May this person collect the child from school? */
    @Column(name = "may_collect", nullable = false)
    private boolean mayCollect = true;
}
