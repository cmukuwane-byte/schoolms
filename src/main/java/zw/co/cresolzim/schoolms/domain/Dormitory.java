package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "dormitory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"hostel_id", "name"}))
@Getter @Setter @NoArgsConstructor
public class Dormitory extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id")
    private Hostel hostel;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "bed_capacity", nullable = false)
    private Integer bedCapacity = 20;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prefect_id")
    private Student prefect;

    @Column(nullable = false)
    private boolean active = true;

    @Override public String toString() { return hostel.getName() + " / " + name; }
}
