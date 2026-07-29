package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity @Table(name = "staff", indexes = {
        @Index(name = "ix_staff_number", columnList = "staff_number", unique = true)
})
@Getter @Setter @NoArgsConstructor
public class Staff extends BaseEntity {

    @Column(name = "staff_number", nullable = false, unique = true, length = 30)
    private String staffNumber;

    @Column(length = 20)
    private String title;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(nullable = false, length = 60)
    private String surname;

    @Enumerated(EnumType.STRING) @Column(length = 10)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "national_id", length = 30)
    private String nationalId;

    @Column(name = "mobile_phone", nullable = false, length = 25)
    private String mobilePhone;

    @Column(length = 120)
    private String email;

    @Column(length = 250)
    private String address;

    @Enumerated(EnumType.STRING) @Column(name = "staff_type", nullable = false, length = 12)
    private StaffType staffType = StaffType.TEACHING;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    /** Which side of the school this teacher mainly serves. Null means both. */
    @Enumerated(EnumType.STRING) @Column(length = 12)
    private Phase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "job_title", length = 80)
    private String jobTitle;

    @Column(name = "qualifications", length = 500)
    private String qualifications;

    /** Ministry of Primary and Secondary Education registration number. */
    @Column(name = "teacher_registration_no", length = 40)
    private String teacherRegistrationNo;

    @Column(name = "date_joined", nullable = false)
    private LocalDate dateJoined;

    @Column(name = "date_left")
    private LocalDate dateLeft;

    @Column(name = "photo_path", length = 250)
    private String photoPath;

    /** Subjects this teacher is qualified to teach — drives the allocation picker. */
    @ManyToMany
    @JoinTable(name = "staff_subject",
            joinColumns = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id"))
    private Set<Subject> teachableSubjects = new HashSet<>();

    public String getFullName() {
        return (title == null ? "" : title + " ") + firstName + " " + surname;
    }

    public String getShortName() { return firstName.charAt(0) + ". " + surname; }

    @Override public String toString() { return getFullName(); }
}
