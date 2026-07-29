package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "department")
@Getter @Setter @NoArgsConstructor
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_of_department_id")
    private Staff headOfDepartment;

    @Override public String toString() { return name; }
}
