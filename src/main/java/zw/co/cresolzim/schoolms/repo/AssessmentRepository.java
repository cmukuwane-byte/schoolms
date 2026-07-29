package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByTermIdAndSchoolClassIdOrderByAssessmentDateDesc(Long termId, Long classId);

    List<Assessment> findByTermIdAndSchoolClassIdAndSubjectId(Long termId, Long classId, Long subjectId);

    List<Assessment> findByTeacherIdAndTermIdOrderByAssessmentDateDesc(Long teacherId, Long termId);

    List<Assessment> findByTermIdAndPublishedFalse(Long termId);
}
