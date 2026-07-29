package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface StudentTransferRepository extends JpaRepository<StudentTransfer, Long> {
    List<StudentTransfer> findByStudentIdOrderByEffectiveDateDesc(Long studentId);
    List<StudentTransfer> findByDirectionOrderByEffectiveDateDesc(TransferDirection direction);
}
