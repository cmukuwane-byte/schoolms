package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface AssessmentMarkRepository extends JpaRepository<AssessmentMark, Long> {

    List<AssessmentMark> findByAssessmentIdOrderByStudentSurnameAsc(Long assessmentId);

    Optional<AssessmentMark> findByAssessmentIdAndStudentId(Long assessmentId, Long studentId);

    List<AssessmentMark> findByStudentIdAndAssessmentTermId(Long studentId, Long termId);

    /** Weighted term percentage per subject for one learner. */
    @Query("""
           select m.assessment.subject.id,
                  sum((m.rawMark / m.assessment.maxMark) * 100 * m.assessment.weightPercent)
                      / sum(m.assessment.weightPercent)
           from AssessmentMark m
           where m.student.id = :studentId and m.assessment.term.id = :termId
             and m.absent = false and m.rawMark is not null and m.assessment.published = true
           group by m.assessment.subject.id
           """)
    List<Object[]> termAveragesBySubject(@Param("studentId") Long studentId, @Param("termId") Long termId);

    @Query("""
           select avg((m.rawMark / m.assessment.maxMark) * 100) from AssessmentMark m
           where m.assessment.id = :assessmentId and m.absent = false and m.rawMark is not null
           """)
    Double classAverage(@Param("assessmentId") Long assessmentId);

    void deleteByAssessmentId(Long assessmentId);
}
