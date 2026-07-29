package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface ReportCardRepository extends JpaRepository<ReportCard, Long> {
    Optional<ReportCard> findByStudentIdAndTermId(Long studentId, Long termId);
    List<ReportCard> findByTermIdAndSchoolClassIdOrderByPositionInClassAsc(Long termId, Long classId);
    List<ReportCard> findByStudentIdOrderByTermIdDesc(Long studentId);
    List<ReportCard> findByStudentIdAndReleasedTrueOrderByTermIdDesc(Long studentId);
}
