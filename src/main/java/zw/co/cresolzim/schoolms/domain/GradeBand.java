package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.Phase;

/** Grading scale. Primary uses A-E on percentage; secondary follows the ZIMSEC bands. */
@Entity @Table(name = "grade_band")
@Getter @Setter @NoArgsConstructor
public class GradeBand extends BaseEntity {

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private Phase phase;

    @Column(name = "min_percent", nullable = false)
    private Double minPercent;

    @Column(name = "max_percent", nullable = false)
    private Double maxPercent;

    @Column(nullable = false, length = 5)
    private String symbol;

    @Column(length = 40)
    private String descriptor;

    @Column(name = "grade_points")
    private Double gradePoints;
}
