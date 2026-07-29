package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.DayOfWeekSlot;

/**
 * One lesson on the grid. Three clash rules are enforced in TimetableService:
 * a class, a teacher and a room can each only be in one place per slot.
 */
@Entity @Table(name = "timetable_slot", indexes = {
        @Index(name = "ix_slot_class", columnList = "school_class_id,day_of_week,bell_period_id"),
        @Index(name = "ix_slot_teacher", columnList = "teacher_id,day_of_week,bell_period_id"),
        @Index(name = "ix_slot_room", columnList = "classroom_id,day_of_week,bell_period_id")
})
@Getter @Setter @NoArgsConstructor
public class TimetableSlot extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    @Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false, length = 12)
    private DayOfWeekSlot dayOfWeek;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bell_period_id")
    private BellPeriod bellPeriod;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Staff teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    @Column(length = 200)
    private String note;
}
