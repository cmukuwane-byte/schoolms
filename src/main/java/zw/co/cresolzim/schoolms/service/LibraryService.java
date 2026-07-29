package zw.co.cresolzim.schoolms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final BookRepository books;
    private final BookCopyRepository copies;
    private final BookLoanRepository loans;
    private final StudentRepository students;
    private final StaffRepository staff;

    @Value("${app.library.loan-days:14}")          private int loanDays;
    @Value("${app.library.max-loans-student:3}")   private int maxStudent;
    @Value("${app.library.max-loans-staff:6}")     private int maxStaff;
    @Value("${app.library.fine-per-day:0.50}")     private BigDecimal finePerDay;

    /* -------------------------------------------------- accessioning */

    /** Adds physical copies to a title, numbering them from the last accession used. */
    @Transactional
    public int addCopies(Long bookId, int howMany, String prefix, String shelf, BigDecimal cost) {
        Book b = books.findById(bookId).orElseThrow(() -> new NotFoundException("Book", bookId));
        int added = 0;
        int next = copies.findByBookId(bookId).size() + 1;
        while (added < howMany) {
            String accession = prefix + "-" + String.format("%05d", next++);
            if (copies.findByAccessionNumber(accession).isPresent()) continue;
            BookCopy c = new BookCopy();
            c.setBook(b);
            c.setAccessionNumber(accession);
            c.setShelfLocation(shelf);
            c.setReplacementCost(cost);
            c.setDateAcquired(LocalDate.now());
            c.setStatus(CopyStatus.AVAILABLE);
            copies.save(c);
            added++;
        }
        return added;
    }

    /* -------------------------------------------------- issue and return */

    /**
     * Issues a copy. Refuses if the copy is not on the shelf, if the borrower is
     * at their limit, or if they have something overdue.
     */
    @Transactional
    public BookLoan issue(String accessionOrBarcode, BorrowerType type, Long borrowerId, String actor) {
        BookCopy copy = copies.findByAccessionNumber(accessionOrBarcode)
                .or(() -> copies.findByBarcode(accessionOrBarcode))
                .orElseThrow(() -> new RuleViolationException(
                        "No copy found with accession or barcode " + accessionOrBarcode + "."));

        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new RuleViolationException(
                    "Copy " + copy.getAccessionNumber() + " is marked " + copy.getStatus() + ", not available.");
        }

        BookLoan loan = new BookLoan();
        loan.setBookCopy(copy);
        loan.setBorrowerType(type);
        loan.setIssueDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(loanDays));
        loan.setIssuedBy(actor);

        if (type == BorrowerType.STUDENT) {
            Student s = students.findById(borrowerId)
                    .orElseThrow(() -> new NotFoundException("Student", borrowerId));
            if (!s.isActive()) {
                throw new RuleViolationException(s.getFullName() + " is not currently on the register.");
            }
            checkLimit(loans.countByStudentIdAndStatus(borrowerId, LoanStatus.ON_LOAN), maxStudent, s.getFullName());
            checkNoOverdue(loans.findByStudentIdOrderByIssueDateDesc(borrowerId), s.getFullName());
            loan.setStudent(s);
        } else {
            Staff st = staff.findById(borrowerId).orElseThrow(() -> new NotFoundException("Staff", borrowerId));
            checkLimit(loans.countByStaffIdAndStatus(borrowerId, LoanStatus.ON_LOAN), maxStaff, st.getFullName());
            checkNoOverdue(loans.findByStaffIdOrderByIssueDateDesc(borrowerId), st.getFullName());
            loan.setStaff(st);
        }

        copy.setStatus(CopyStatus.ON_LOAN);
        copies.save(copy);
        return loans.save(loan);
    }

    private void checkLimit(long open, int max, String who) {
        if (open >= max) {
            throw new RuleViolationException(who + " already has " + open + " books out, the limit is " + max + ".");
        }
    }

    private void checkNoOverdue(List<BookLoan> history, String who) {
        boolean overdue = history.stream().anyMatch(BookLoan::isOverdue);
        if (overdue) {
            throw new RuleViolationException(who + " has an overdue book. Take that back first.");
        }
    }

    /** Takes a copy back, works out any fine, and puts the copy back on the shelf. */
    @Transactional
    public BookLoan receive(String accessionOrBarcode, String condition, String actor) {
        BookCopy copy = copies.findByAccessionNumber(accessionOrBarcode)
                .or(() -> copies.findByBarcode(accessionOrBarcode))
                .orElseThrow(() -> new RuleViolationException(
                        "No copy found with accession or barcode " + accessionOrBarcode + "."));

        BookLoan loan = loans.findByBookCopyIdAndReturnDateIsNull(copy.getId())
                .orElseThrow(() -> new RuleViolationException(
                        "Copy " + copy.getAccessionNumber() + " is not recorded as being out on loan."));

        loan.setReturnDate(LocalDate.now());
        loan.setReceivedBy(actor);
        loan.setStatus(LoanStatus.RETURNED);

        if (loan.getReturnDate().isAfter(loan.getDueDate())) {
            long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), loan.getReturnDate());
            loan.setFineAmount(finePerDay.multiply(BigDecimal.valueOf(daysLate))
                    .setScale(2, RoundingMode.HALF_UP));
        }

        if (condition != null && !condition.isBlank()) copy.setCondition(condition);
        copy.setStatus("POOR".equalsIgnoreCase(condition) ? CopyStatus.DAMAGED : CopyStatus.AVAILABLE);
        copies.save(copy);

        return loans.save(loan);
    }

    /** Extends a loan once, provided it is not already overdue. */
    @Transactional
    public BookLoan renew(Long loanId) {
        BookLoan loan = loans.findById(loanId).orElseThrow(() -> new NotFoundException("Loan", loanId));
        if (loan.getReturnDate() != null) throw new RuleViolationException("That loan is already closed.");
        if (loan.isOverdue()) throw new RuleViolationException("An overdue loan cannot be renewed.");
        if (loan.getRenewalCount() >= 1) throw new RuleViolationException("This loan has already been renewed once.");
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        loan.setDueDate(loan.getDueDate().plusDays(loanDays));
        return loans.save(loan);
    }

    /** Writes a copy off as lost and charges the replacement cost to the borrower. */
    @Transactional
    public BookLoan reportLost(Long loanId, String actor) {
        BookLoan loan = loans.findById(loanId).orElseThrow(() -> new NotFoundException("Loan", loanId));
        BookCopy copy = loan.getBookCopy();
        loan.setStatus(LoanStatus.LOST);
        loan.setReturnDate(LocalDate.now());
        loan.setReceivedBy(actor);
        if (copy.getReplacementCost() != null) loan.setFineAmount(copy.getReplacementCost());
        copy.setStatus(CopyStatus.LOST);
        copies.save(copy);
        return loans.save(loan);
    }

    /** Runs at 06:00 daily to flag anything that has gone past its due date. */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void flagOverdue() {
        for (BookLoan l : loans.findOverdue(LocalDate.now())) {
            if (l.getStatus() == LoanStatus.ON_LOAN) {
                l.setStatus(LoanStatus.OVERDUE);
                loans.save(l);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<BookLoan> overdueList() { return loans.findOverdue(LocalDate.now()); }
}
