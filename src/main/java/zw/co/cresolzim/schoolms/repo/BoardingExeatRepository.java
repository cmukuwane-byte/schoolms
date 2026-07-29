package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface BoardingExeatRepository extends JpaRepository<BoardingExeat, Long> {
    List<BoardingExeat> findByStudentIdOrderByDepartureTimeDesc(Long studentId);
    List<BoardingExeat> findByActualReturnIsNullOrderByExpectedReturnAsc();
}
