package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByStudentNumber(String studentNumber);

    boolean existsByStudentNumber(String studentNumber);

    List<Student> findByCurrentClassIdAndStatusOrderBySurnameAscFirstNameAsc(Long classId, StudentStatus status);

    List<Student> findByStatusOrderBySurnameAsc(StudentStatus status);

    long countByStatus(StudentStatus status);

    long countByPhaseAndStatus(Phase phase, StudentStatus status);

    long countByResidencyAndStatus(Residency residency, StudentStatus status);

    @Query("""
           select s from Student s
           where s.status = :status
             and (lower(s.firstName) like lower(concat('%', :q, '%'))
               or lower(s.surname)   like lower(concat('%', :q, '%'))
               or lower(s.studentNumber) like lower(concat('%', :q, '%')))
           order by s.surname, s.firstName
           """)
    List<Student> search(@Param("q") String q, @Param("status") StudentStatus status);

    @Query("""
           select distinct sg.student from StudentGuardian sg
           where sg.guardian.id = :guardianId
           order by sg.student.surname
           """)
    List<Student> findWardsOfGuardian(@Param("guardianId") Long guardianId);
}
