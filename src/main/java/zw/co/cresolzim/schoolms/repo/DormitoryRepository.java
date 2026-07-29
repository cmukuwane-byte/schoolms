package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface DormitoryRepository extends JpaRepository<Dormitory, Long> {
    List<Dormitory> findByHostelIdAndActiveTrueOrderByNameAsc(Long hostelId);
    List<Dormitory> findByActiveTrueOrderByHostelNameAscNameAsc();

    @EntityGraph(attributePaths = "hostel")
    List<Dormitory> findAllByOrderByHostelNameAscNameAsc();
}
