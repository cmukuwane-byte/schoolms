package zw.co.cresolzim.schoolms.domain;

import jakarta.persistence.*;
import lombok.*;
import zw.co.cresolzim.schoolms.domain.Enums.DisciplinaryOutcome;
import java.time.LocalDate;

/** Warnings through to expulsions. An expulsion here closes the learner's record. */
@Entity @Table(name = "disciplinary_case")
@Getter @Setter @NoArgsConstructor
public class DisciplinaryCase extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "reported_by", length = 80)
    private String reportedBy;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private DisciplinaryOutcome outcome;

    /** For suspensions. */
    @Column(name = "suspension_start")
    private LocalDate suspensionStart;

    @Column(name = "suspension_end")
    private LocalDate suspensionEnd;

    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    @Column(name = "guardian_notified", nullable = false)
    private boolean guardianNotified = false;

    @Column(name = "decided_by", length = 80)
    private String decidedBy;

    @Column(name = "appeal_notes", length = 1000)
    private String appealNotes;
}
