/* ===========================================================================
   Views for the office and for anyone pulling figures into Excel or Power BI.
   =========================================================================== */

USE SchoolMS;
GO

CREATE OR ALTER VIEW vw_enrolment_summary AS
SELECT
    c.phase,
    c.name              AS class_name,
    c.grade_level,
    COUNT(s.id)         AS learners,
    SUM(CASE WHEN s.gender = 'MALE'    THEN 1 ELSE 0 END) AS boys,
    SUM(CASE WHEN s.gender = 'FEMALE'  THEN 1 ELSE 0 END) AS girls,
    SUM(CASE WHEN s.residency = 'BOARDER' THEN 1 ELSE 0 END) AS boarders,
    c.max_capacity,
    c.max_capacity - COUNT(s.id) AS places_left
FROM school_class c
LEFT JOIN student s ON s.current_class_id = c.id AND s.status = 'ENROLLED'
WHERE c.active = 1
GROUP BY c.phase, c.name, c.grade_level, c.max_capacity;
GO

CREATE OR ALTER VIEW vw_daily_attendance AS
SELECT
    a.attendance_date,
    c.phase,
    c.name AS class_name,
    SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present,
    SUM(CASE WHEN a.status = 'ABSENT'  THEN 1 ELSE 0 END) AS absent,
    SUM(CASE WHEN a.status = 'LATE'    THEN 1 ELSE 0 END) AS late,
    COUNT(*) AS on_roll,
    CAST(100.0 * SUM(CASE WHEN a.status IN ('PRESENT','LATE') THEN 1 ELSE 0 END)
         / NULLIF(COUNT(*), 0) AS DECIMAL(5,2)) AS attendance_rate
FROM student_attendance a
JOIN school_class c ON c.id = a.school_class_id
WHERE a.session_name = 'MORNING'
GROUP BY a.attendance_date, c.phase, c.name;
GO

CREATE OR ALTER VIEW vw_staff_punctuality AS
SELECT
    st.staff_number,
    st.first_name + ' ' + st.surname AS staff_name,
    COUNT(*)                              AS days_recorded,
    SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) AS days_late,
    SUM(a.minutes_late)                   AS total_minutes_late,
    AVG(CAST(a.minutes_late AS FLOAT))    AS avg_minutes_late,
    SUM(CASE WHEN a.departure_time IS NULL THEN 1 ELSE 0 END) AS days_not_signed_out
FROM staff_attendance a
JOIN staff st ON st.id = a.staff_id
GROUP BY st.staff_number, st.first_name, st.surname;
GO

CREATE OR ALTER VIEW vw_library_circulation AS
SELECT
    b.title,
    b.author,
    COUNT(l.id)          AS times_issued,
    SUM(CASE WHEN l.return_date IS NULL THEN 1 ELSE 0 END) AS currently_out,
    SUM(CASE WHEN l.status = 'LOST' THEN 1 ELSE 0 END)     AS copies_lost,
    SUM(l.fine_amount)   AS fines_raised
FROM book b
JOIN book_copy bc ON bc.book_id = b.id
LEFT JOIN book_loan l ON l.book_copy_id = bc.id
GROUP BY b.title, b.author;
GO

CREATE OR ALTER VIEW vw_subject_performance AS
SELECT
    t.name  AS term_name,
    c.name  AS class_name,
    sub.name AS subject_name,
    COUNT(m.id) AS marks_entered,
    CAST(AVG(m.raw_mark / NULLIF(a.max_mark, 0) * 100) AS DECIMAL(5,2)) AS average_percent,
    SUM(CASE WHEN m.raw_mark / NULLIF(a.max_mark, 0) * 100 >= 50 THEN 1 ELSE 0 END) AS passes
FROM assessment_mark m
JOIN assessment a   ON a.id = m.assessment_id
JOIN term t         ON t.id = a.term_id
JOIN school_class c ON c.id = a.school_class_id
JOIN subject sub    ON sub.id = a.subject_id
WHERE m.absent = 0 AND m.raw_mark IS NOT NULL AND a.published = 1
GROUP BY t.name, c.name, sub.name;
GO
