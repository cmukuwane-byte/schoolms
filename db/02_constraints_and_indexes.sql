/* ===========================================================================
   Run AFTER the application has started once and Hibernate has built the
   schema. These are the guards and indexes that belong in the database
   rather than in Java, so that a stray script cannot corrupt the data.
   =========================================================================== */

USE SchoolMS;
GO

/* ---- attendance: one row per learner, per day, per session -------------- */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_student_day_session')
    CREATE UNIQUE INDEX uq_student_day_session
        ON student_attendance (student_id, attendance_date, session_name);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_staff_day')
    CREATE UNIQUE INDEX uq_staff_day
        ON staff_attendance (staff_id, attendance_date);
GO

/* ---- timetable clash guards --------------------------------------------
   The service layer checks these before saving, but the database enforces
   them absolutely: a class, a teacher and a room cannot be in two places
   in the same period. Rooms are nullable, so that index is filtered.        */

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_slot_class')
    CREATE UNIQUE INDEX uq_slot_class
        ON timetable_slot (term_id, day_of_week, bell_period_id, school_class_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_slot_teacher')
    CREATE UNIQUE INDEX uq_slot_teacher
        ON timetable_slot (term_id, day_of_week, bell_period_id, teacher_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_slot_room')
    CREATE UNIQUE INDEX uq_slot_room
        ON timetable_slot (term_id, day_of_week, bell_period_id, classroom_id)
        WHERE classroom_id IS NOT NULL;
GO

/* ---- one open loan per physical copy ------------------------------------ */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_open_loan_per_copy')
    CREATE UNIQUE INDEX uq_open_loan_per_copy
        ON book_loan (book_copy_id)
        WHERE return_date IS NULL;
GO

/* ---- one active bed per boarder ----------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_active_bed')
    CREATE UNIQUE INDEX uq_active_bed
        ON boarding_allocation (student_id)
        WHERE active = 1;
GO

/* ---- marks must sit inside the paper total ------------------------------ */
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_mark_not_negative')
    ALTER TABLE assessment_mark
        ADD CONSTRAINT ck_mark_not_negative CHECK (raw_mark IS NULL OR raw_mark >= 0);
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_term_dates')
    ALTER TABLE term
        ADD CONSTRAINT ck_term_dates CHECK (end_date > start_date);
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_loan_dates')
    ALTER TABLE book_loan
        ADD CONSTRAINT ck_loan_dates CHECK (due_date >= issue_date);
GO

/* ---- reporting indexes -------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_mark_student_term')
    CREATE INDEX ix_mark_student_term
        ON assessment_mark (student_id) INCLUDE (assessment_id, raw_mark, absent);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_attendance_term_status')
    CREATE INDEX ix_attendance_term_status
        ON student_attendance (term_id, status) INCLUDE (student_id);
GO
