package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    List<Department> findAllByOrderByNameAsc();
}
