package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.Phase;

@Entity @Table(name = "subject")
@Getter @Setter @NoArgsConstructor
public class Subject extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private Phase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false)
    private boolean core = true;

    /** Appears on the report card in this order. */
    @Column(name = "display_order")
    private Integer displayOrder = 100;

    @Column(nullable = false)
    private boolean active = true;

    @Override public String toString() { return name; }
}
