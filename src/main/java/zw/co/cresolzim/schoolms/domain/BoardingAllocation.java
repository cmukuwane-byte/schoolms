package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/** A boarder's bed for a term. Ended when the learner leaves or moves dorm. */
@Entity @Table(name = "boarding_allocation")
@Getter @Setter @NoArgsConstructor
public class BoardingAllocation extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "dormitory_id")
    private Dormitory dormitory;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    @Column(name = "bed_number", length = 20)
    private String bedNumber;

    @Column(name = "allocated_on", nullable = false)
    private LocalDate allocatedOn;

    @Column(name = "vacated_on")
    private LocalDate vacatedOn;

    @Column(nullable = false)
    private boolean active = true;
}
