@echo off
REM Copyright (c) 2006 Ingres Corporation
REM 
REM  
REM                SCRIPT FOR RUNNING THE FRONTEND TESTS
REM
REM
REM Description:
REM	This script is used to setup and run the embedded 3gl language tests
REM     using listexec and excutor. 
REM
REM  History:
REM	13-Feb-1996 (somsa01)
REM		Created from runfe3gl.sh .
REM	20-Jun-2000 (sarjo01)
REM		Added MT (Multithreaded) Suite	
REM	12-Apr-2001 (rogch01)
REM		Added FORTRAN Suite
REM	13-Oct-2004 (legru01)
REM		Modified script so it pertains to the Open Source Acceptance
REM		Package test suites.
REM	12-Sep-2006  (sarjo01)
REM             Add -n flag tp createdb personnel to allow unicode types
REM
REM ---------------------------------------------------------------------------
REM			Setup Area
REM --------------------------------------------------------------------------

REM Set the output directory for test results.
REM
setlocal
if not "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%TST_ROOT_OUTPUT%\embed

if "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%ING_TST%\output\embed
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul PCecho "Creating Directory - %ING_TST%\output"
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul md "%ING_TST%\output"

if not exist %TST_OUTPUT%\nul PCecho "Creating Directory - %TST_OUTPUT%"
if not exist %TST_OUTPUT%\nul md %TST_OUTPUT%

echo -------------------------------------------
echo Output files will fe written to %TST_OUTPUT%
echo -------------------------------------------

set flag=%1

if "%1"=="init" if not "%2"=="" goto INIT
if "%1"=="3gl" goto NEXT
	echo You must input the modules to be initalized 
	echo or specify "all".
	echo.
	echo  Example: runfe3gl init all 
	echo           or 
	echo.
	echo           runfe3gl init 3gl1
	echo.
	goto END2
:NEXT
if "%1"=="3gl" if not "%2"=="" goto RUNTEST
	echo You must input the 3gl modules to be tested
	echo or specify all
	echo.
	echo  Example: runfe3gl 3gl all
	echo           or
	echo.
	echo           runfe3gl 3gl c
	echo.
	goto END2
rem ----------------------------------------------------------------------------
rem                        Initialization Area
rem ----------------------------------------------------------------------------
rem 
rem  Start the initialization of Frontend Databases
rem 
:INIT
if "%2"=="" goto END
	if not "%2"=="all" if not "%2"=="3gl1" goto Init3gl2
	  call ipset datevar PCdate
	  PCecho "Creating the personnel database @ %datevar%"
	  echo.
	  destroydb personnel >>%TST_OUTPUT%\feinit3gl.out
	  createdb -n personnel >>%TST_OUTPUT%\feinit3gl.out
	  call ipset datevar PCdate
	  PCecho "Finished creating personnel database @ %datevar%"
	  echo.
	  if "%2"=="all" goto Init3gl2
	  shift
	  goto INIT

:Init3gl2
	if not "%2"=="all" if not "%2"=="3gl2" goto END
	  call ipset datevar PCdate
	  PCecho "Creating the gourmet database @ %datevar%"
	  echo.
	  destroydb gourmet >>%TST_OUTPUT%\feinit3gl.out
	  createdb gourmet >>%TST_OUTPUT%\feinit3gl.out
	  call ipset datevar PCdate
	  PCecho "Finished creating gourmet database @ %datevar%"
	  echo.
	  if "%2"=="all" goto END
	  shift
	  goto INIT

:RUNTEST
if "%2"=="" goto END
	if not "%2"=="all" if not "%2"=="c" goto END
	  if not exist %TST_OUTPUT%\c\nul PCecho "Creating Directory - %TST_OUTPUT%\c"
	  if not exist %TST_OUTPUT%\c\nul md "%TST_OUTPUT%\c"
	  call ipset datevar PCdate
	  PCecho "Running the EMBEDDED C tests @ %datevar%"
	  echo.
	  call dbcreate 3gl c
	  call ipset datevar PCdate
	  PCecho "Finished with the Embedded C tests @ %datevar%"
	  echo.
	  if "%2"=="all" goto END
	  shift
	  goto RUNTEST

:END
call ipset datevar PCdate
if "%flag%"=="init" PCecho "3gl databases have been created @ %datevar%"
if "%flag%"=="fe" PCecho "End of EMBEDDED 3GL tests @ %datevar%"
:END2
endlocal
