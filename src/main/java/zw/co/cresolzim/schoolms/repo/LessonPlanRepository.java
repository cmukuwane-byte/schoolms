package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.Enums.LessonPlanStatus;
import zw.co.cresolzim.schoolms.domain.LessonPlan;

import java.util.*;

public interface LessonPlanRepository extends JpaRepository<LessonPlan, Long> {

    @EntityGraph(attributePaths = {"teacher", "schoolClass", "subject", "term"})
    List<LessonPlan> findByTeacherIdAndTermIdOrderByWeekNumberDescSubjectNameAsc(Long teacherId, Long termId);

    @EntityGraph(attributePaths = {"teacher", "schoolClass", "subject", "term"})
    List<LessonPlan> findByStatusOrderBySubmittedAtAsc(LessonPlanStatus status);

    @EntityGraph(attributePaths = {"teacher", "schoolClass", "subject", "term"})
    List<LessonPlan> findBySchoolClassIdAndTermIdOrderByWeekNumberAscSubjectNameAsc(Long classId, Long termId);

    @EntityGraph(attributePaths = {"teacher", "schoolClass", "subject", "term", "curriculumTopic"})
    Optional<LessonPlan> findWithDetailById(Long id);

    Optional<LessonPlan> findBySchoolClassIdAndSubjectIdAndTermIdAndWeekNumber(
            Long classId, Long subjectId, Long termId, Integer weekNumber);

    long countByTeacherIdAndTermIdAndStatus(Long teacherId, Long termId, LessonPlanStatus status);

    long countByStatus(LessonPlanStatus status);

    /**
     * Submission board for the head: how each teacher is doing this term.
     * Returns {teacher, drafted, submitted, approved, returned}.
     */
    @Query("""
           select p.teacher, p.status, count(p)
           from LessonPlan p
           where p.term.id = :termId
           group by p.teacher, p.status
           """)
    List<Object[]> submissionCounts(@Param("termId") Long termId);
}
