package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface TermRepository extends JpaRepository<Term, Long> {
    Optional<Term> findByCurrentTrue();
    List<Term> findByAcademicYearIdOrderByTermNumberAsc(Long yearId);
    List<Term> findAllByOrderByAcademicYearYearDescTermNumberDesc();
}
