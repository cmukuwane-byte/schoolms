package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface BoardingAllocationRepository extends JpaRepository<BoardingAllocation, Long> {

    List<BoardingAllocation> findByDormitoryIdAndActiveTrue(Long dormitoryId);

    Optional<BoardingAllocation> findByStudentIdAndActiveTrue(Long studentId);

    long countByDormitoryIdAndActiveTrue(Long dormitoryId);

    /**
     * Returns {dormitory, hostel, occupiedBeds}. The hostel is selected explicitly
     * rather than reached through d.hostel because SQL Server will not order by a
     * column that is absent from the GROUP BY, and selecting it also puts the
     * hostel in the persistence context so the view can read its name.
     */
    @Query("""
           select d, h, count(a) from Dormitory d
           join d.hostel h
           left join BoardingAllocation a on a.dormitory = d and a.active = true
           where d.active = true
           group by d, h
           order by h.name, d.name
           """)
    List<Object[]> occupancyByDormitory();
}
