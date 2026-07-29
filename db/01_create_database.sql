/* ===========================================================================
   School Management System — database provisioning for MS SQL Server
   Run this once as sysadmin before starting the application.
   Hibernate creates the tables (ddl-auto=update); this script creates the
   database, the login the application uses, and the indexes and constraints
   that Hibernate will not add for you.
   =========================================================================== */

IF DB_ID('SchoolMS') IS NULL
BEGIN
    CREATE DATABASE SchoolMS
        COLLATE SQL_Latin1_General_CP1_CI_AS;
END
GO

ALTER DATABASE SchoolMS SET RECOVERY FULL;
GO

/* ---- application login -------------------------------------------------- */

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'schoolms_app')
BEGIN
    CREATE LOGIN schoolms_app
        WITH PASSWORD = 'Ch4nge#This#2026',
             DEFAULT_DATABASE = SchoolMS,
             CHECK_POLICY = ON;
END
GO

USE SchoolMS;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'schoolms_app')
BEGIN
    CREATE USER schoolms_app FOR LOGIN schoolms_app;
    ALTER ROLE db_datareader ADD MEMBER schoolms_app;
    ALTER ROLE db_datawriter ADD MEMBER schoolms_app;
    ALTER ROLE db_ddladmin  ADD MEMBER schoolms_app;   -- needed while ddl-auto=update
END
GO
