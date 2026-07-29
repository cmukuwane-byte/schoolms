package zw.co.cresolzim.schoolms.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import zw.co.cresolzim.schoolms.repo.*;
import zw.co.cresolzim.schoolms.service.LibraryService;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService service;
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final BookLoanRepository loans;
    private final StudentRepository students;
    private final StaffRepository staff;
    private final SubjectRepository subjects;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("titleCount", books.count());
        model.addAttribute("copyCount", copies.count());
        model.addAttribute("onLoan", copies.countByStatus(CopyStatus.ON_LOAN));
        model.addAttribute("available", copies.countByStatus(CopyStatus.AVAILABLE));
        model.addAttribute("overdue", service.overdueList());
        return "library/index";
    }

    /* --------------------------------------------------------- catalogue */

    @GetMapping("/catalogue")
    public String catalogue(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String category, Model model) {
        List<Book> results;
        if (q != null && !q.isBlank())          results = books.search(q.trim());
        else if (category != null)              results = books.findByCategoryOrderByTitleAsc(category);
        else                                    results = books.findAll();

        model.addAttribute("books", results);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("categories", books.allCategories());
        return "library/catalogue";
    }

    @GetMapping("/catalogue/new")
    public String newBook(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("subjects", subjects.findByActiveTrueOrderByPhaseAscDisplayOrderAsc());
        return "library/book-form";
    }

    @PostMapping("/catalogue")
    public String saveBook(@ModelAttribute Book book, RedirectAttributes flash) {
        Book saved = books.save(book);
        flash.addFlashAttribute("message", "Title saved. Add copies to make it borrowable.");
        return "redirect:/library/catalogue/" + saved.getId();
    }

    @GetMapping("/catalogue/{id}")
    public String viewBook(@PathVariable Long id, Model model) {
        model.addAttribute("book", books.findById(id).orElseThrow());
        model.addAttribute("copies", copies.findByBookId(id));
        return "library/book-view";
    }

    @PostMapping("/catalogue/{id}/copies")
    public String addCopies(@PathVariable Long id, @RequestParam int howMany,
                            @RequestParam String prefix,
                            @RequestParam(required = false) String shelf,
                            @RequestParam(required = false) BigDecimal cost,
                            RedirectAttributes flash) {
        int added = service.addCopies(id, howMany, prefix, shelf, cost);
        flash.addFlashAttribute("message", added + " copies accessioned.");
        return "redirect:/library/catalogue/" + id;
    }

    /* ---------------------------------------------------- issue / return */

    @GetMapping("/circulation")
    public String circulation(Model model) {
        model.addAttribute("onLoan", loans.findByStatusOrderByDueDateAsc(LoanStatus.ON_LOAN));
        model.addAttribute("overdue", service.overdueList());
        return "library/circulation";
    }

    @PostMapping("/issue")
    public String issue(@RequestParam String accession, @RequestParam BorrowerType borrowerType,
                        @RequestParam Long borrowerId, RedirectAttributes flash) {
        BookLoan loan = service.issue(accession, borrowerType, borrowerId, CurrentUser.name());
        flash.addFlashAttribute("message",
                loan.getBookCopy().getBook().getTitle() + " issued to " + loan.getBorrowerName()
                        + ", due " + loan.getDueDate() + ".");
        return "redirect:/library/circulation";
    }

    @PostMapping("/return")
    public String receive(@RequestParam String accession,
                          @RequestParam(required = false) String condition,
                          RedirectAttributes flash) {
        BookLoan loan = service.receive(accession, condition, CurrentUser.name());
        String note = loan.getFineAmount() != null && loan.getFineAmount().signum() > 0
                ? " Fine due: " + loan.getFineAmount() + "." : "";
        flash.addFlashAttribute("message", "Returned by " + loan.getBorrowerName() + "." + note);
        return "redirect:/library/circulation";
    }

    @PostMapping("/loans/{id}/renew")
    public String renew(@PathVariable Long id, RedirectAttributes flash) {
        BookLoan loan = service.renew(id);
        flash.addFlashAttribute("message", "Renewed to " + loan.getDueDate() + ".");
        return "redirect:/library/circulation";
    }

    @PostMapping("/loans/{id}/lost")
    public String lost(@PathVariable Long id, RedirectAttributes flash) {
        BookLoan loan = service.reportLost(id, CurrentUser.name());
        flash.addFlashAttribute("message", "Copy written off. Charge raised: " + loan.getFineAmount() + ".");
        return "redirect:/library/circulation";
    }

    /** Borrower lookup used by the issue desk. */
    @GetMapping("/borrowers")
    @ResponseBody
    public List<?> borrowerSearch(@RequestParam BorrowerType type, @RequestParam String q) {
        if (type == BorrowerType.STUDENT) {
            return students.search(q, StudentStatus.ENROLLED).stream()
                    .map(s -> new Object[]{s.getId(), s.getStudentNumber() + " - " + s.getFullName()}).toList();
        }
        return staff.search(q).stream()
                .map(s -> new Object[]{s.getId(), s.getStaffNumber() + " - " + s.getFullName()}).toList();
    }
}
