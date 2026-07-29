package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface BellPeriodRepository extends JpaRepository<BellPeriod, Long> {
    List<BellPeriod> findByPhaseOrderByPeriodNumberAsc(Phase phase);
    List<BellPeriod> findByPhaseAndTeachingPeriodTrueOrderByPeriodNumberAsc(Phase phase);
}
