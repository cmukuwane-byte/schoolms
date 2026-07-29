package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long> {

    Optional<StaffAttendance> findByStaffIdAndAttendanceDate(Long staffId, LocalDate date);

    List<StaffAttendance> findByAttendanceDateOrderByArrivalTimeAsc(LocalDate date);

    List<StaffAttendance> findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            Long staffId, LocalDate from, LocalDate to);

    @Query("""
           select a from StaffAttendance a
           where a.attendanceDate = :date and a.departureTime is null
             and a.status <> zw.co.cresolzim.schoolms.domain.Enums.AttendanceStatus.ABSENT
           order by a.staff.surname
           """)
    List<StaffAttendance> stillOnPremises(@Param("date") LocalDate date);

    @Query("""
           select a.staff, count(a), coalesce(sum(a.minutesLate),0) from StaffAttendance a
           where a.attendanceDate between :from and :to and a.minutesLate > 0
           group by a.staff
           order by sum(a.minutesLate) desc
           """)
    List<Object[]> latenessLeague(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByAttendanceDateAndStatus(LocalDate date, AttendanceStatus status);
}
