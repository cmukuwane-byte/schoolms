package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/** A catalogue title. Physical copies hang off it as BookCopy rows. */
@Entity @Table(name = "book", indexes = {
        @Index(name = "ix_book_title", columnList = "title"),
        @Index(name = "ix_book_isbn", columnList = "isbn")
})
@Getter @Setter @NoArgsConstructor
public class Book extends BaseEntity {

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 200)
    private String author;

    @Column(length = 20)
    private String isbn;

    @Column(length = 150)
    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(length = 60)
    private String edition;

    /** Dewey or the school's own shelf code. */
    @Column(name = "classification", length = 40)
    private String classification;

    @Column(length = 80)
    private String category;

    @Column(length = 60)
    private String language = "English";

    /** Set when the title is a set book for a subject. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "cover_path", length = 250)
    private String coverPath;

    @Column(length = 1000)
    private String synopsis;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookCopy> copies = new ArrayList<>();

    @Transient
    public long getAvailableCount() {
        return copies.stream().filter(c -> c.getStatus() == Enums.CopyStatus.AVAILABLE).count();
    }
}
