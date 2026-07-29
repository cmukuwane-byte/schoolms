package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {

    List<TimetableSlot> findByTermIdAndSchoolClassId(Long termId, Long classId);

    List<TimetableSlot> findByTermIdAndTeacherId(Long termId, Long teacherId);

    List<TimetableSlot> findByTermIdAndClassroomId(Long termId, Long roomId);

    /** Clash detection: anything already sitting in this day + period. */
    @Query("""
           select t from TimetableSlot t
           where t.term.id = :termId and t.dayOfWeek = :day and t.bellPeriod.id = :periodId
             and (t.schoolClass.id = :classId or t.teacher.id = :teacherId
                  or (:roomId is not null and t.classroom.id = :roomId))
             and (:excludeId is null or t.id <> :excludeId)
           """)
    List<TimetableSlot> findClashes(@Param("termId") Long termId,
                                    @Param("day") DayOfWeekSlot day,
                                    @Param("periodId") Long periodId,
                                    @Param("classId") Long classId,
                                    @Param("teacherId") Long teacherId,
                                    @Param("roomId") Long roomId,
                                    @Param("excludeId") Long excludeId);

    /** Who is meant to be teaching this class right now — used by the attendance screen. */
    @Query("""
           select t from TimetableSlot t
           where t.term.id = :termId and t.dayOfWeek = :day
           order by t.bellPeriod.periodNumber
           """)
    List<TimetableSlot> findDayGrid(@Param("termId") Long termId, @Param("day") DayOfWeekSlot day);

    void deleteByTermIdAndSchoolClassId(Long termId, Long classId);
}
