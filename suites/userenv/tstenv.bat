@ECHO OFF
REM =========================================================================
REM Copyright (c) 2008 Ingres Corporation
REM
REM
REM
REM THIS IS A SAMPLE BATCH FILE USED TO SETUP YOUR TEST ENVIRONMENT ALONG WITH
REM YOUR INGRES INSTALLATION ON YOUR PC.  YOU WILL NEED TO MODIFY ONLY THE
REM OCCURRENCES OF '< .. >'.
REM
REM
REM A suitable value for SEP_DIFF_SLEEP on a fast machine would be 10.  The
REM default within sep of 250 milliseconds will affect how fast (or slow) SEP
REM will run.
REM
REM 27-Aug-2008 (vande02) Changed '@CA' indicators to generic indicator for
REM			  those lines which need customization.
REM 29-Sep-2008 (vande02) Added the ING_CHARSET variable to enble UTF8 testing.
REM
REM =========================================================================

set II_SYSTEM=< .. >
set II_DBMS_LOG=%II_SYSTEM%\ingres\files\dbms.log 
set II_DATE_CENTURY_BOUNDARY=10
set TERM_INGRES=ibmpc
set SEP_DIFF_SLEEP=10

REM =========================================================================
REM
REM	VARIABLE REQUIRED TO LOCATE THE TESTS
REM	YOU WILL NEED TO MODIFY ONLY THE OCCURRENCES OF '< .. >'.
REM
REM =========================================================================

set ING_TST=< .. >

REM =========================================================================
REM
REM     SET THIS VARIABLE TO UTF8 IF TESTING THAT CHARACTER SET
REM
REM =========================================================================

set ING_CHARSET=< .. >

REM =========================================================================
REM
REM	DIRECTORY FOR TEST OUTPUT AND WORKING DIRECTORIES FOR SEP
REM	YOU WILL NEED TO MODIFY ONLY THE OCCURRENCES OF '< .. >'.
REM
REM =========================================================================

set TST_OUTPUT=< .. >
set TST_ROOT_OUTPUT=%TST_OUTPUT%

REM =========================================================================
REM
REM	REQUIRED VARIABLE FOR NET\LOOPBACK TESTS
REM
REM =========================================================================

set TST_DATA=%ING_TST%\gcf\gcc\data

REM =========================================================================
REM
REM	REQUIRED VARIABLE TO LOCATE WHERE YOU INSTALLED THE TESTING TOOLS
REM	YOU WILL NEED TO MODIFY ONLY THE OCCURRENCES OF '< .. >'.
REM
REM =========================================================================

set ING_TOOLS=< .. >
set INGHOME=c:\TEMP
set TSTHOME=c:\TEMP

REM =========================================================================
REM
REM THE REST OF THIS FILE SHOULD NOT BE MODIFIED!!!!!!!!!!!!!!!!!!!!!!
REM
REM =========================================================================

set TOOLS_DIR=%ING_TOOLS%
set TST_SEP=%ING_TOOLS%\files
set PEDITOR=%ING_TOOLS%\bin\peditor.exe
set TST_TOOLS=%ING_TOOLS%\bin
set SEPPARAM_SYSTEM=%OS%
set ING_EDIT=%WINDIR%\system32\notepad.exe

set TST_DOC=%ING_TST%\suites\doc
set TST_TESTENV=%ING_TST%
set TST_TESTOOLS=%ING_TST%\testtool
set TST_CFG=%ING_TST%\suites\acceptst
set TST_LISTEXEC=%TST_CFG%
set TST_SHELL=%ING_TST%\suites\bat
set TST_INIT=%ING_TST%\basis\init

REM =========================================================================
REM
REM STRESS DIRECTORIES
REM
REM =========================================================================

set TST_STRESS=%ING_TST%\stress

REM =========================================================================
REM
REM	REQUIRED VARIABLE FOR REPLICATOR TESTS
REM
REM =========================================================================

set REP_TST=%ING_TST%

REM =========================================================================
REM
REM THESE ARE ALSO NEEDED
REM
REM =========================================================================

set II_CONFIG=%II_SYSTEM%\ingres\files
set II_ABF_RUNOPT=nondynamic
set II_TERMCAP_FILE=%II_CONFIG%\termcap

REM =========================================================================
REM
REM	PATH SETTINGS MUST INCLUDE INGRES BIN, UTILITY AND ING_TST SUITES BAT
REM
REM =========================================================================

set path=%II_SYSTEM%\ingres\bin;%II_SYSTEM%\ingres\utility;%ING_TST%\suites\bat;%TST_TOOLS%;%path%

REM =========================================================================
REM
REM SHOW WHAT WE HAVE
REM
REM =========================================================================

call ipset ii_code ingprenv II_INSTALLATION
echo Welcome to %COMPUTERNAME%/%OS% Ingres Installation
echo II_INSTALLATION= %ii_code%
echo. 
echo II_SYSTEM      = %II_SYSTEM%
echo ING_TST        = %ING_TST%
echo ING_TOOLS      = %ING_TOOLS%
echo TST_OUTPUT     = %TST_OUTPUT%
echo TST_LISTEXEC   = %TST_LISTEXEC%
echo TST_CFG        = %TST_CFG%
echo SEP_DIFF_SLEEP = %SEP_DIFF_SLEEP%
echo.
