package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "guardian")
@Getter @Setter @NoArgsConstructor
public class Guardian extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(nullable = false, length = 60)
    private String surname;

    @Column(name = "national_id", length = 30)
    private String nationalId;

    @Column(name = "primary_phone", nullable = false, length = 25)
    private String primaryPhone;

    @Column(name = "alternate_phone", length = 25)
    private String alternatePhone;

    @Column(length = 120)
    private String email;

    @Column(length = 100)
    private String occupation;

    @Column(length = 120)
    private String employer;

    @Column(name = "residential_address", length = 250)
    private String residentialAddress;

    @OneToMany(mappedBy = "guardian")
    private List<StudentGuardian> wards = new ArrayList<>();

    public String getFullName() { return firstName + " " + surname; }
}
