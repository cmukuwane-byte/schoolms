package zw.co.cresolzim.schoolms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.DayOfWeekSlot;
import zw.co.cresolzim.schoolms.repo.*;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableSlotRepository slots;
    private final ClassSubjectAllocationRepository allocations;
    private final BellPeriodRepository periods;
    private final SchoolClassRepository classes;
    private final ClassroomRepository rooms;
    private final SubjectRepository subjects;
    private final StaffRepository staff;
    private final TermRepository terms;

    /**
     * Places one lesson on the grid after checking the three ways a slot can clash:
     * the class is already busy, the teacher is already busy, or the room is taken.
     */
    @Transactional
    public TimetableSlot place(Long termId, Long classId, Long subjectId, Long teacherId,
                               Long roomId, DayOfWeekSlot day, Long periodId, Long existingSlotId) {

        BellPeriod period = periods.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Period", periodId));
        if (!period.isTeachingPeriod()) {
            throw new RuleViolationException(period.getLabel() + " is not a teaching period.");
        }

        List<TimetableSlot> clashes = slots.findClashes(
                termId, day, periodId, classId, teacherId, roomId, existingSlotId);

        if (!clashes.isEmpty()) {
            throw new RuleViolationException(describeClashes(clashes, classId, teacherId, roomId));
        }

        TimetableSlot slot = existingSlotId == null
                ? new TimetableSlot()
                : slots.findById(existingSlotId).orElseThrow(() -> new NotFoundException("Slot", existingSlotId));

        slot.setTerm(terms.findById(termId).orElseThrow(() -> new NotFoundException("Term", termId)));
        slot.setSchoolClass(classes.findById(classId).orElseThrow(() -> new NotFoundException("Class", classId)));
        slot.setSubject(subjects.findById(subjectId).orElseThrow(() -> new NotFoundException("Subject", subjectId)));
        slot.setTeacher(staff.findById(teacherId).orElseThrow(() -> new NotFoundException("Teacher", teacherId)));
        slot.setClassroom(roomId == null ? null : rooms.findById(roomId).orElse(null));
        slot.setDayOfWeek(day);
        slot.setBellPeriod(period);

        return slots.save(slot);
    }

    private String describeClashes(List<TimetableSlot> clashes, Long classId, Long teacherId, Long roomId) {
        StringBuilder sb = new StringBuilder("This slot is already taken. ");
        for (TimetableSlot c : clashes) {
            if (c.getSchoolClass().getId().equals(classId)) {
                sb.append(c.getSchoolClass().getName())
                  .append(" has ").append(c.getSubject().getName()).append(" then. ");
            }
            if (c.getTeacher().getId().equals(teacherId)) {
                sb.append(c.getTeacher().getShortName())
                  .append(" is teaching ").append(c.getSchoolClass().getName()).append(" then. ");
            }
            if (roomId != null && c.getClassroom() != null && c.getClassroom().getId().equals(roomId)) {
                sb.append(c.getClassroom().getCode())
                  .append(" is booked for ").append(c.getSchoolClass().getName()).append(" then. ");
            }
        }
        return sb.toString().trim();
    }

    @Transactional
    public void remove(Long slotId) { slots.deleteById(slotId); }

    /** grid[day][periodId] -> slot, ready for the Thymeleaf table. */
    @Transactional(readOnly = true)
    public Map<DayOfWeekSlot, Map<Long, TimetableSlot>> classGrid(Long termId, Long classId) {
        Map<DayOfWeekSlot, Map<Long, TimetableSlot>> grid = new LinkedHashMap<>();
        for (DayOfWeekSlot d : DayOfWeekSlot.values()) grid.put(d, new HashMap<>());
        for (TimetableSlot s : slots.findByTermIdAndSchoolClassId(termId, classId)) {
            grid.get(s.getDayOfWeek()).put(s.getBellPeriod().getId(), s);
        }
        return grid;
    }

    @Transactional(readOnly = true)
    public Map<DayOfWeekSlot, Map<Long, TimetableSlot>> teacherGrid(Long termId, Long teacherId) {
        Map<DayOfWeekSlot, Map<Long, TimetableSlot>> grid = new LinkedHashMap<>();
        for (DayOfWeekSlot d : DayOfWeekSlot.values()) grid.put(d, new HashMap<>());
        for (TimetableSlot s : slots.findByTermIdAndTeacherId(termId, teacherId)) {
            grid.get(s.getDayOfWeek()).put(s.getBellPeriod().getId(), s);
        }
        return grid;
    }

    /**
     * Compares what the timetable actually contains against the periods-per-week
     * agreed in the subject allocations, so gaps show up before term starts.
     */
    @Transactional(readOnly = true)
    public List<String> auditClass(Long termId, Long classId) {
        List<String> findings = new ArrayList<>();
        Map<Long, Integer> placed = new HashMap<>();
        for (TimetableSlot s : slots.findByTermIdAndSchoolClassId(termId, classId)) {
            placed.merge(s.getSubject().getId(), 1, Integer::sum);
        }
        for (ClassSubjectAllocation a : allocations.findBySchoolClassIdAndActiveTrue(classId)) {
            int have = placed.getOrDefault(a.getSubject().getId(), 0);
            int want = a.getPeriodsPerWeek();
            if (have < want) {
                findings.add(a.getSubject().getName() + ": " + have + " of " + want + " lessons placed");
            } else if (have > want) {
                findings.add(a.getSubject().getName() + ": " + have + " lessons placed, only " + want + " allocated");
            }
        }
        return findings;
    }
}
