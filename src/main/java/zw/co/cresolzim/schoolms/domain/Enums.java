package zw.co.cresolzim.schoolms.domain;

public final class Enums {
    private Enums() {}

    /** A school running Primary and Secondary under one roof. */
    public enum Phase { PRIMARY, SECONDARY }

    public enum Gender { MALE, FEMALE }

    public enum Residency { DAY, BOARDER }

    /** Lifecycle of a learner in this school. */
    public enum StudentStatus {
        ENROLLED, SUSPENDED, TRANSFERRED_OUT, EXPELLED, WITHDRAWN, GRADUATED
    }

    public enum TransferDirection { IN, OUT }

    public enum DisciplinaryOutcome { WARNING, DETENTION, SUSPENSION, EXPULSION }

    public enum StaffType { TEACHING, ADMIN, SUPPORT, BOARDING }

    public enum EmploymentStatus { ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED }

    public enum AttendanceStatus { PRESENT, ABSENT, LATE, EXCUSED, SICK_BAY }

    public enum DayOfWeekSlot { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY }

    public enum AssessmentType { CLASS_TEST, ASSIGNMENT, MID_TERM, END_OF_TERM, MOCK, PUBLIC_EXAM }

    public enum LoanStatus { ON_LOAN, RETURNED, OVERDUE, LOST }

    public enum CopyStatus { AVAILABLE, ON_LOAN, RESERVED, DAMAGED, LOST, WITHDRAWN }

    public enum BorrowerType { STUDENT, STAFF }

    /** Lesson plan workflow. RETURNED means the head sent it back for revision. */
    public enum LessonPlanStatus { DRAFT, SUBMITTED, APPROVED, RETURNED }
}
