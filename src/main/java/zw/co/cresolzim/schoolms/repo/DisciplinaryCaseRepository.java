package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface DisciplinaryCaseRepository extends JpaRepository<DisciplinaryCase, Long> {
    List<DisciplinaryCase> findByStudentIdOrderByIncidentDateDesc(Long studentId);
    List<DisciplinaryCase> findByOutcomeOrderByIncidentDateDesc(DisciplinaryOutcome outcome);
    List<DisciplinaryCase> findByIncidentDateBetweenOrderByIncidentDateDesc(LocalDate from, LocalDate to);
}
