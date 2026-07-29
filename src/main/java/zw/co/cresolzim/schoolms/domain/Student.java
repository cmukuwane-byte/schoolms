package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "student", indexes = {
        @Index(name = "ix_student_number", columnList = "student_number", unique = true),
        @Index(name = "ix_student_surname", columnList = "surname")
})
@Getter @Setter @NoArgsConstructor
public class Student extends BaseEntity {

    /** School-issued admission number, e.g. CA/2026/0341 */
    @Column(name = "student_number", nullable = false, unique = true, length = 30)
    private String studentNumber;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "middle_name", length = 60)
    private String middleName;

    @Column(nullable = false, length = 60)
    private String surname;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    /** National registration / birth entry number. */
    @Column(name = "national_id", length = 30)
    private String nationalId;

    @Column(length = 60)
    private String nationality = "Zimbabwean";

    @Column(length = 40)
    private String religion;

    @Column(name = "home_address", length = 250)
    private String homeAddress;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private Phase phase;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private Residency residency = Residency.DAY;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ENROLLED;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    /** Current class. Null between promotions or after exit. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_class_id")
    private SchoolClass currentClass;

    @Column(name = "photo_path", length = 250)
    private String photoPath;

    @Column(name = "medical_notes", length = 1000)
    private String medicalNotes;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentGuardian> guardians = new ArrayList<>();

    @OneToMany(mappedBy = "student")
    private List<Enrolment> enrolments = new ArrayList<>();

    public String getFullName() { return firstName + " " + surname; }

    public String getFullNameFormal() {
        return surname.toUpperCase() + ", " + firstName + (middleName == null ? "" : " " + middleName);
    }

    public int getAge() {
        return dateOfBirth == null ? 0 : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public boolean isActive() { return status == StudentStatus.ENROLLED || status == StudentStatus.SUSPENDED; }
}
