package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    List<SchoolClass> findByAcademicYearIdAndActiveTrueOrderByPhaseAscGradeLevelAscNameAsc(Long yearId);

    List<SchoolClass> findByAcademicYearIdAndPhaseAndActiveTrueOrderByGradeLevelAscNameAsc(Long yearId, Phase phase);

    List<SchoolClass> findByClassTeacherIdAndActiveTrue(Long teacherId);

    Optional<SchoolClass> findByNameAndAcademicYearId(String name, Long yearId);

    /** Next grade up, same stream where possible — used by the promotion routine. */
    @Query("""
           select c from SchoolClass c
           where c.academicYear.id = :yearId and c.phase = :phase
             and c.gradeLevel = :gradeLevel and c.active = true
           order by case when c.stream = :stream then 0 else 1 end, c.name
           """)
    List<SchoolClass> findPromotionTargets(@Param("yearId") Long yearId,
                                           @Param("phase") Phase phase,
                                           @Param("gradeLevel") Integer gradeLevel,
                                           @Param("stream") String stream);
}
