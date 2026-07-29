package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long> {

    List<StudentAttendance> findBySchoolClassIdAndAttendanceDateAndSessionName(
            Long classId, LocalDate date, String session);

    Optional<StudentAttendance> findByStudentIdAndAttendanceDateAndSessionName(
            Long studentId, LocalDate date, String session);

    List<StudentAttendance> findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            Long studentId, LocalDate from, LocalDate to);

    long countByStudentIdAndTermIdAndStatus(Long studentId, Long termId, AttendanceStatus status);

    @Query("""
           select a.status, count(a) from StudentAttendance a
           where a.attendanceDate = :date and a.sessionName = 'MORNING'
           group by a.status
           """)
    List<Object[]> dailySummary(@Param("date") LocalDate date);

    /** Learners with more absences than the threshold this term — the follow-up list. */
    @Query("""
           select a.student, count(a) as misses from StudentAttendance a
           where a.term.id = :termId
             and a.status = zw.co.cresolzim.schoolms.domain.Enums.AttendanceStatus.ABSENT
           group by a.student
           having count(a) >= :threshold
           order by count(a) desc
           """)
    List<Object[]> chronicAbsentees(@Param("termId") Long termId, @Param("threshold") long threshold);

    boolean existsBySchoolClassIdAndAttendanceDateAndSessionName(Long classId, LocalDate date, String session);
}
