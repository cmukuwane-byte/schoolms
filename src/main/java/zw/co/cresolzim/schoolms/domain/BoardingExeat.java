package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Leave of absence from the hostel — weekend exeat, medical, bereavement. */
@Entity @Table(name = "boarding_exeat")
@Getter @Setter @NoArgsConstructor
public class BoardingExeat extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "expected_return", nullable = false)
    private LocalDateTime expectedReturn;

    @Column(name = "actual_return")
    private LocalDateTime actualReturn;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(name = "collected_by", length = 120)
    private String collectedBy;

    @Column(name = "authorised_by", length = 80)
    private String authorisedBy;
}
