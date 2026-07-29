package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.AttendanceStatus;
import java.time.LocalDate;
import java.time.LocalTime;

/** Daily register. One row per learner per day per session. */
@Entity @Table(name = "student_attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_student_day_session",
                columnNames = {"student_id", "attendance_date", "session_name"}),
        indexes = @Index(name = "ix_att_date_class", columnList = "attendance_date,school_class_id"))
@Getter @Setter @NoArgsConstructor
public class StudentAttendance extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /** MORNING or AFTERNOON. Boarders also get an EVENING roll call. */
    @Column(name = "session_name", nullable = false, length = 12)
    private String sessionName = "MORNING";

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(length = 250)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id")
    private Staff recordedBy;
}
