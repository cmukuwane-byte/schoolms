package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.Phase;

/** A teaching group: Grade 5 Blue, Form 3 Science, ECD B and so on. */
@Entity @Table(name = "school_class",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "academic_year_id"}))
@Getter @Setter @NoArgsConstructor
public class SchoolClass extends BaseEntity {

    /** "Grade 5 Blue", "Form 3 Alpha" */
    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private Phase phase;

    /** ECD A=-1, ECD B=0, Grade 1..7 = 1..7, Form 1..6 = 1..6 within SECONDARY. */
    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Column(length = 30)
    private String stream;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_teacher_id")
    private Staff classTeacher;

    /** Home room. Practical subjects override this on the timetable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_classroom_id")
    private Classroom homeClassroom;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity = 40;

    @Column(nullable = false)
    private boolean active = true;

    @Override public String toString() { return name; }
}
