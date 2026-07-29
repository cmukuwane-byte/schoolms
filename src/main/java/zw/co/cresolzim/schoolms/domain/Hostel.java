package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.Gender;
import zw.co.cresolzim.schoolms.domain.Enums.Phase;

@Entity @Table(name = "hostel")
@Getter @Setter @NoArgsConstructor
public class Hostel extends BaseEntity {

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING) @Column(length = 12)
    private Phase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "housemaster_id")
    private Staff housemaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matron_id")
    private Staff matron;

    @Column(nullable = false)
    private boolean active = true;

    @Override public String toString() { return name; }
}
