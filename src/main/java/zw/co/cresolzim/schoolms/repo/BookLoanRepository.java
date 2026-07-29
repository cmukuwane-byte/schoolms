package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface BookLoanRepository extends JpaRepository<BookLoan, Long> {

    List<BookLoan> findByStatusOrderByDueDateAsc(LoanStatus status);

    List<BookLoan> findByStudentIdOrderByIssueDateDesc(Long studentId);

    List<BookLoan> findByStaffIdOrderByIssueDateDesc(Long staffId);

    long countByStudentIdAndStatus(Long studentId, LoanStatus status);

    long countByStaffIdAndStatus(Long staffId, LoanStatus status);

    Optional<BookLoan> findByBookCopyIdAndReturnDateIsNull(Long copyId);

    @Query("""
           select l from BookLoan l
           where l.returnDate is null and l.dueDate < :today
           order by l.dueDate
           """)
    List<BookLoan> findOverdue(@Param("today") LocalDate today);

    @Query("""
           select count(l) from BookLoan l
           where l.returnDate is null and l.student.id = :studentId
           """)
    long openLoansForStudent(@Param("studentId") Long studentId);
}
