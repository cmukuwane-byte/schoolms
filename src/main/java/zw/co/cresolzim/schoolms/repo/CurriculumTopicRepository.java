package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.CurriculumTopic;
import zw.co.cresolzim.schoolms.domain.Enums.Phase;

import java.util.*;

public interface CurriculumTopicRepository extends JpaRepository<CurriculumTopic, Long> {

    @EntityGraph(attributePaths = "subject")
    List<CurriculumTopic> findBySubjectIdAndGradeLevelAndActiveTrueOrderByTermNumberAscWeekNumberAsc(
            Long subjectId, Integer gradeLevel);

    @EntityGraph(attributePaths = "subject")
    List<CurriculumTopic> findByPhaseAndGradeLevelAndTermNumberAndActiveTrueOrderBySubjectDisplayOrderAscWeekNumberAsc(
            Phase phase, Integer gradeLevel, Integer termNumber);

    Optional<CurriculumTopic> findBySubjectIdAndGradeLevelAndTermNumberAndWeekNumber(
            Long subjectId, Integer gradeLevel, Integer termNumber, Integer weekNumber);

    /** Coverage check: which weeks of the scheme have no lesson plan against them. */
    @Query("""
           select c from CurriculumTopic c
           where c.subject.id = :subjectId and c.gradeLevel = :gradeLevel
             and c.termNumber = :termNumber and c.active = true
             and not exists (
                 select 1 from LessonPlan p
                 where p.curriculumTopic = c and p.schoolClass.id = :classId
             )
           order by c.weekNumber
           """)
    List<CurriculumTopic> uncoveredWeeks(@Param("subjectId") Long subjectId,
                                         @Param("gradeLevel") Integer gradeLevel,
                                         @Param("termNumber") Integer termNumber,
                                         @Param("classId") Long classId);

    long countBySubjectIdAndGradeLevelAndActiveTrue(Long subjectId, Integer gradeLevel);
}
