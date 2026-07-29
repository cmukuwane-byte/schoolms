package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface ClassSubjectAllocationRepository extends JpaRepository<ClassSubjectAllocation, Long> {

    List<ClassSubjectAllocation> findBySchoolClassIdAndActiveTrue(Long classId);

    List<ClassSubjectAllocation> findByTeacherIdAndActiveTrue(Long teacherId);

    Optional<ClassSubjectAllocation> findBySchoolClassIdAndSubjectId(Long classId, Long subjectId);

    @Query("""
           select coalesce(sum(a.periodsPerWeek), 0) from ClassSubjectAllocation a
           where a.teacher.id = :teacherId and a.active = true
           """)
    Integer weeklyLoadOfTeacher(@Param("teacherId") Long teacherId);
}
