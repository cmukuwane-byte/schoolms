package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface EnrolmentRepository extends JpaRepository<Enrolment, Long> {
    List<Enrolment> findByStudentIdOrderByAcademicYearYearDesc(Long studentId);
    Optional<Enrolment> findByStudentIdAndAcademicYearId(Long studentId, Long yearId);
    List<Enrolment> findBySchoolClassIdAndActiveTrue(Long classId);
    long countBySchoolClassIdAndActiveTrue(Long classId);
}
