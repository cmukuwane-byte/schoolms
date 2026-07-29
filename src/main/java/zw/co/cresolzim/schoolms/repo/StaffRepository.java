package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByStaffNumber(String staffNumber);

    boolean existsByStaffNumber(String staffNumber);

    List<Staff> findByEmploymentStatusOrderBySurnameAsc(EmploymentStatus status);

    List<Staff> findByStaffTypeAndEmploymentStatusOrderBySurnameAsc(StaffType type, EmploymentStatus status);

    long countByEmploymentStatus(EmploymentStatus status);

    @Query("""
           select s from Staff s
           where s.employmentStatus = zw.co.cresolzim.schoolms.domain.Enums.EmploymentStatus.ACTIVE
             and (lower(s.firstName) like lower(concat('%', :q, '%'))
               or lower(s.surname)   like lower(concat('%', :q, '%'))
               or lower(s.staffNumber) like lower(concat('%', :q, '%')))
           order by s.surname
           """)
    List<Staff> search(@Param("q") String q);

    @Query("select distinct a.teacher from ClassSubjectAllocation a where a.subject.id = :subjectId")
    List<Staff> findTeachersOfSubject(@Param("subjectId") Long subjectId);
}
