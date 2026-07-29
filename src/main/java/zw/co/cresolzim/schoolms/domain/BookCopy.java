package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.CopyStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One physical book on the shelf, identified by its accession number. */
@Entity @Table(name = "book_copy")
@Getter @Setter @NoArgsConstructor
public class BookCopy extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "accession_number", nullable = false, unique = true, length = 40)
    private String accessionNumber;

    @Column(name = "barcode", unique = true, length = 60)
    private String barcode;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private CopyStatus status = CopyStatus.AVAILABLE;

    /** NEW, GOOD, FAIR, POOR */
    @Column(name = "copy_condition", length = 20)
    private String condition = "GOOD";

    @Column(name = "shelf_location", length = 40)
    private String shelfLocation;

    @Column(name = "date_acquired")
    private LocalDate dateAcquired;

    @Column(name = "replacement_cost", precision = 12, scale = 2)
    private BigDecimal replacementCost;
}
