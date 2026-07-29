/* ===========================================================================
   Optional demonstration data. Run against an empty SchoolMS only.
   The application seeds roles, the administrator, grading scales, the bell
   schedule and the calendar on first start — this adds subjects, rooms,
   classes and hostels on top so there is something to click through.
   =========================================================================== */

USE SchoolMS;
GO

DECLARE @year_id BIGINT = (SELECT TOP 1 id FROM academic_year WHERE is_current = 1);

/* ---- departments -------------------------------------------------------- */
INSERT INTO department (name, created_at, updated_at) VALUES
 ('Languages', SYSDATETIME(), SYSDATETIME()),
 ('Mathematics and Science', SYSDATETIME(), SYSDATETIME()),
 ('Humanities', SYSDATETIME(), SYSDATETIME()),
 ('Practical Subjects', SYSDATETIME(), SYSDATETIME());

/* ---- subjects ----------------------------------------------------------- */
INSERT INTO subject (code, name, phase, core, display_order, active, created_at, updated_at) VALUES
 ('ENG-P',  'English Language',        'PRIMARY',   1, 10, 1, SYSDATETIME(), SYSDATETIME()),
 ('SHO-P',  'Shona',                   'PRIMARY',   1, 20, 1, SYSDATETIME(), SYSDATETIME()),
 ('MAT-P',  'Mathematics',             'PRIMARY',   1, 30, 1, SYSDATETIME(), SYSDATETIME()),
 ('SCI-P',  'Science and Technology',  'PRIMARY',   1, 40, 1, SYSDATETIME(), SYSDATETIME()),
 ('SOC-P',  'Social Studies',          'PRIMARY',   1, 50, 1, SYSDATETIME(), SYSDATETIME()),
 ('AGR-P',  'Agriculture',             'PRIMARY',   0, 60, 1, SYSDATETIME(), SYSDATETIME()),
 ('PE-P',   'Physical Education',      'PRIMARY',   0, 70, 1, SYSDATETIME(), SYSDATETIME()),

 ('ENG-S',  'English Language',        'SECONDARY', 1, 10, 1, SYSDATETIME(), SYSDATETIME()),
 ('LIT-S',  'English Literature',      'SECONDARY', 0, 15, 1, SYSDATETIME(), SYSDATETIME()),
 ('SHO-S',  'Shona',                   'SECONDARY', 1, 20, 1, SYSDATETIME(), SYSDATETIME()),
 ('MAT-S',  'Mathematics',             'SECONDARY', 1, 30, 1, SYSDATETIME(), SYSDATETIME()),
 ('COM-S',  'Combined Science',        'SECONDARY', 1, 40, 1, SYSDATETIME(), SYSDATETIME()),
 ('BIO-S',  'Biology',                 'SECONDARY', 0, 41, 1, SYSDATETIME(), SYSDATETIME()),
 ('CHE-S',  'Chemistry',               'SECONDARY', 0, 42, 1, SYSDATETIME(), SYSDATETIME()),
 ('PHY-S',  'Physics',                 'SECONDARY', 0, 43, 1, SYSDATETIME(), SYSDATETIME()),
 ('GEO-S',  'Geography',               'SECONDARY', 0, 50, 1, SYSDATETIME(), SYSDATETIME()),
 ('HIS-S',  'History',                 'SECONDARY', 0, 51, 1, SYSDATETIME(), SYSDATETIME()),
 ('ACC-S',  'Principles of Accounts',  'SECONDARY', 0, 60, 1, SYSDATETIME(), SYSDATETIME()),
 ('CS-S',   'Computer Science',        'SECONDARY', 0, 65, 1, SYSDATETIME(), SYSDATETIME()),
 ('HER-S',  'Heritage Studies',        'SECONDARY', 1, 70, 1, SYSDATETIME(), SYSDATETIME());

/* ---- rooms -------------------------------------------------------------- */
INSERT INTO classroom (code, name, block, capacity, room_type, active, created_at, updated_at) VALUES
 ('P01', 'Primary Room 1', 'Primary Block', 40, 'CLASSROOM',    1, SYSDATETIME(), SYSDATETIME()),
 ('P02', 'Primary Room 2', 'Primary Block', 40, 'CLASSROOM',    1, SYSDATETIME(), SYSDATETIME()),
 ('P03', 'Primary Room 3', 'Primary Block', 40, 'CLASSROOM',    1, SYSDATETIME(), SYSDATETIME()),
 ('S01', 'Form Room 1',    'Senior Block',  40, 'CLASSROOM',    1, SYSDATETIME(), SYSDATETIME()),
 ('S02', 'Form Room 2',    'Senior Block',  40, 'CLASSROOM',    1, SYSDATETIME(), SYSDATETIME()),
 ('LAB1','Science Laboratory 1', 'Science Block', 32, 'LABORATORY', 1, SYSDATETIME(), SYSDATETIME()),
 ('ICT', 'Computer Laboratory',  'Science Block', 30, 'COMPUTER_LAB', 1, SYSDATETIME(), SYSDATETIME()),
 ('HALL','Assembly Hall',  'Main',         400, 'HALL',         1, SYSDATETIME(), SYSDATETIME()),
 ('LIB', 'Library',        'Main',          60, 'LIBRARY',      1, SYSDATETIME(), SYSDATETIME());

/* ---- classes ------------------------------------------------------------ */
INSERT INTO school_class (name, phase, grade_level, stream, academic_year_id,
                          max_capacity, active, created_at, updated_at) VALUES
 ('ECD B',        'PRIMARY',    0, NULL,      @year_id, 30, 1, SYSDATETIME(), SYSDATETIME()),
 ('Grade 1 Blue', 'PRIMARY',    1, 'Blue',    @year_id, 40, 1, SYSDATETIME(), SYSDATETIME()),
 ('Grade 4 Blue', 'PRIMARY',    4, 'Blue',    @year_id, 40, 1, SYSDATETIME(), SYSDATETIME()),
 ('Grade 5 Blue', 'PRIMARY',    5, 'Blue',    @year_id, 40, 1, SYSDATETIME(), SYSDATETIME()),
 ('Grade 6 Blue', 'PRIMARY',    6, 'Blue',    @year_id, 40, 1, SYSDATETIME(), SYSDATETIME()),
 ('Grade 7 Blue', 'PRIMARY',    7, 'Blue',    @year_id, 40, 1, SYSDATETIME(), SYSDATETIME()),
 ('Form 1 Alpha', 'SECONDARY',  1, 'Alpha',   @year_id, 40, 1, SYSDATETIME(), SYSDATETIME()),
 ('Form 2 Alpha', 'SECONDARY',  2, 'Alpha',   @year_id, 40, 1, SYSDATETIME(), SYSDATETIME()),
 ('Form 3 Alpha', 'SECONDARY',  3, 'Alpha',   @year_id, 40, 1, SYSDATETIME(), SYSDATETIME()),
 ('Form 4 Alpha', 'SECONDARY',  4, 'Alpha',   @year_id, 40, 1, SYSDATETIME(), SYSDATETIME());

/* ---- hostels and dormitories -------------------------------------------- */
INSERT INTO hostel (name, gender, phase, active, created_at, updated_at) VALUES
 ('Nyanga House',  'MALE',   'SECONDARY', 1, SYSDATETIME(), SYSDATETIME()),
 ('Chimanimani House', 'FEMALE', 'SECONDARY', 1, SYSDATETIME(), SYSDATETIME());

DECLARE @boys BIGINT  = (SELECT id FROM hostel WHERE name = 'Nyanga House');
DECLARE @girls BIGINT = (SELECT id FROM hostel WHERE name = 'Chimanimani House');

INSERT INTO dormitory (hostel_id, name, bed_capacity, active, created_at, updated_at) VALUES
 (@boys,  'Dorm A', 24, 1, SYSDATETIME(), SYSDATETIME()),
 (@boys,  'Dorm B', 24, 1, SYSDATETIME(), SYSDATETIME()),
 (@girls, 'Dorm C', 24, 1, SYSDATETIME(), SYSDATETIME()),
 (@girls, 'Dorm D', 24, 1, SYSDATETIME(), SYSDATETIME());
GO
