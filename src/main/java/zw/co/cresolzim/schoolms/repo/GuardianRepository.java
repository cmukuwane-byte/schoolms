package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    Optional<Guardian> findByNationalId(String nationalId);

    @Query("""
           select g from Guardian g
           where lower(g.firstName) like lower(concat('%', :q, '%'))
              or lower(g.surname)   like lower(concat('%', :q, '%'))
              or g.primaryPhone     like concat('%', :q, '%')
           order by g.surname
           """)
    List<Guardian> search(@Param("q") String q);

    Optional<Guardian> findByPrimaryPhone(String primaryPhone);
}
