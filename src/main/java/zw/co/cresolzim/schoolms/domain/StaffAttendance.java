package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.AttendanceStatus;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/** Staff sign-in book: arrival and departure times, with lateness worked out on save. */
@Entity @Table(name = "staff_attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_staff_day", columnNames = {"staff_id", "attendance_date"}))
@Getter @Setter @NoArgsConstructor
public class StaffAttendance extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    /** Minutes past the expected reporting time. Zero when on time or early. */
    @Column(name = "minutes_late", nullable = false)
    private Integer minutesLate = 0;

    /** Minutes short of the expected knock-off time. */
    @Column(name = "minutes_early_departure", nullable = false)
    private Integer minutesEarlyDeparture = 0;

    @Column(name = "signed_in_by", length = 80)
    private String signedInBy;

    @Column(length = 250)
    private String remarks;

    /** Worked minutes on the premises, or null if still signed in. */
    @Transient
    public Long getMinutesOnDuty() {
        if (arrivalTime == null || departureTime == null) return null;
        return Duration.between(arrivalTime, departureTime).toMinutes();
    }
}
