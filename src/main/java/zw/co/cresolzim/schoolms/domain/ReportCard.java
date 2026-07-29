package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The term report. Generated from published assessments, then commented on and released. */
@Entity @Table(name = "report_card",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "term_id"}))
@Getter @Setter @NoArgsConstructor
public class ReportCard extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @Column(name = "average_percent")
    private Double averagePercent;

    @Column(name = "total_marks")
    private Double totalMarks;

    @Column(name = "position_in_class")
    private Integer positionInClass;

    @Column(name = "class_size")
    private Integer classSize;

    @Column(name = "days_present")
    private Integer daysPresent;

    @Column(name = "days_absent")
    private Integer daysAbsent;

    @Column(name = "class_teacher_comment", length = 800)
    private String classTeacherComment;

    @Column(name = "head_comment", length = 800)
    private String headComment;

    @Column(name = "conduct", length = 40)
    private String conduct;

    @Column(name = "next_term_begins")
    private LocalDate nextTermBegins;

    /** Released reports are visible to guardians and can be printed. */
    @Column(nullable = false)
    private boolean released = false;

    @Column(name = "released_on")
    private LocalDate releasedOn;

    @OneToMany(mappedBy = "reportCard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportCardLine> lines = new ArrayList<>();
}
