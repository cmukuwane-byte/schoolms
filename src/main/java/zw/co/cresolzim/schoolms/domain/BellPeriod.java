package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.Phase;
import java.time.LocalTime;

/** The bell schedule. Primary and Secondary usually run different ones. */
@Entity @Table(name = "bell_period",
        uniqueConstraints = @UniqueConstraint(columnNames = {"phase", "period_number"}))
@Getter @Setter @NoArgsConstructor
public class BellPeriod extends BaseEntity {

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private Phase phase;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Column(nullable = false, length = 40)
    private String label;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Break, lunch and assembly are marked here so nothing gets timetabled into them. */
    @Column(name = "teaching_period", nullable = false)
    private boolean teachingPeriod = true;

    @Override public String toString() { return label + " (" + startTime + "-" + endTime + ")"; }
}
