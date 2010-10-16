@echo off
REM Copyright (c) 2008 Ingres Corporation
REM
REM                SCRIPT FOR RUNNING THE FRONTEND TESTS
REM
REM
REM Description:
REM	This script is used to setup and run the Replicator SEP tests using
REM listexec and excutor. 
REM
REM  History:
REM     30-Jul-1997 (hsuji01) Created
REM	22-feb-1999 (somsa01) Added cleanup segment.
REM	11-may-1999 (somsa01) Corrected running of Replicator datatypes tests.
REM	23-Sep-2004 (legru01) Modified script so it pertains to the Open Source
REM                           Acceptance Package test suites.
REM	13-Feb-2008 (vande02) Added cleanup/startup 10 Replicator services and
REM                           repinst remove in the clean block.
REM     07-Nov-2008 (vande02) Added SEPPARAM_CHARSET to enable tests to run
REM                           with UTF8.
REM
REM ---------------------------------------------------------------------------
REM			Setup Area
REM --------------------------------------------------------------------------

REM Set the output directory for test results.
REM
setlocal

REM Set a variable that can be used to check the character set
REM
call ipset ii_code ingprenv II_INSTALLATION
call ipset SEPPARAM_CHARSET ingprenv II_CHARSET%ii_code%

if not "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%TST_ROOT_OUTPUT%\rep
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\rep\nul PCecho "Creating Directory - %TST_ROOT_OUTPUT%\rep"
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\rep\nul md "%TST_ROOT_OUTPUT%\rep"

if "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%ING_TST%\output\rep
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul PCecho "Creating Directory - %ING_TST%\output"
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul md "%ING_TST%\output"
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\rep\nul PCecho "Creating Directory - %ING_TST%\output\rep
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\rep\nul md "%ING_TST%\output\rep"

if not exist %TST_OUTPUT%\nul PCecho "Creating Directory - %TST_OUTPUT%"
if not exist %TST_OUTPUT%\nul md "%TST_OUTPUT%"


echo -------------------------------------------
echo Output files will rep written to %TST_OUTPUT%
echo -------------------------------------------

set flag=%1

if "%1"=="init" if not "%2"=="" goto INIT
if "%1"=="rep" goto NEXT
if "%1"=="clean" goto CLEAN
	echo You must input the modules to rep initalized 
	echo or specify "all".
	echo.
	echo  Example: runrep init all 
	echo           or 
	echo.
	echo           runrep init backup|benign|branch|repmgr
	echo.
	goto END2
:NEXT
if "%1"=="rep" if not "%2"=="" goto RUNTEST
	echo You must input the modules to rep run
	echo or specify all
	echo.
	echo  Example: runrep rep all
	echo           or
	echo.
	echo           runrep rep backup|benign|branch|repmgr
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
      call ipset datevar PCdate
      PCecho "Cleanup/Startup Replicator Services using 'testenv' password."
        repinst remove
        repinst 10
        set TESTENV_PWD=ca-testenv
        call ipset ii ingprenv II_INSTALLATION
             sc.exe config Ingres_Replicator_%ii%_1  obj= ".\testenv" password= "%testenv_pwd%"
             sc.exe config Ingres_Replicator_%ii%_2  obj= ".\testenv" password= "%testenv_pwd%"
             sc.exe config Ingres_Replicator_%ii%_3  obj= ".\testenv" password= "%testenv_pwd%"
             sc.exe config Ingres_Replicator_%ii%_4  obj= ".\testenv" password= "%testenv_pwd%"
             sc.exe config Ingres_Replicator_%ii%_5  obj= ".\testenv" password= "%testenv_pwd%"
             sc.exe config Ingres_Replicator_%ii%_6  obj= ".\testenv" password= "%testenv_pwd%"
             sc.exe config Ingres_Replicator_%ii%_7  obj= ".\testenv" password= "%testenv_pwd%"
             sc.exe config Ingres_Replicator_%ii%_8  obj= ".\testenv" password= "%testenv_pwd%"
             sc.exe config Ingres_Replicator_%ii%_9  obj= ".\testenv" password= "%testenv_pwd%"
             sc.exe config Ingres_Replicator_%ii%_10 obj= ".\testenv" password= "%testenv_pwd%"
	  PCecho " Initializing Replicator databases @ %datevar%"
	  call dbcreate init replicator
	  if not "%2"=="all" goto InitBackup
	  if "%2"=="all" goto InitBackup
	  shift
	  goto INIT

:InitBackup
	if not "%2"=="all" if not "%2"=="backup" goto InitBenign
	  echo Creating Directory - %TST_OUTPUT%\backup
	  md "%TST_OUTPUT%\backup"
	  if "%2"=="all" goto InitBenign
	  shift
	  goto INIT

:InitBenign
	if not "%2"=="all" if not "%2"=="benign" goto InitBranch
	  echo Creating Directory - %TST_OUTPUT%\benign
	  md "%TST_OUTPUT%\benign"
	  if "%2"=="all" goto InitBranch
	  shift
	  goto INIT

:InitBranch
	if not "%2"=="all" if not "%2"=="branch" goto InitRepmgr
	  echo Creating Directory - %TST_OUTPUT%\branch
	  md "%TST_OUTPUT%\branch"
	  if "%2"=="all" goto InitRepmgr
	  shift
	  goto INIT

:InitRepmgr
	if not "%2"=="all" if not "%2"=="repmgr" goto END
	  echo Creating Directory - %TST_OUTPUT%\repmgr
	  md "%TST_OUTPUT%\repmgr"
	  if "%2"=="all" goto END
	  shift
	  goto INIT

:RUNTEST
if "%2"=="" goto END
	if not "%2"=="all" if not "%2"=="backup" goto BENIGN
	  call ipset datevar PCdate
	  PCecho "Running the Central-to-Backup tests @ %datevar% "
	  echo.
	  call dbcreate rep backup
	  if "%2"=="all" goto BENIGN
	  shift
	  goto RUNTEST

:BENIGN
	if not "%2"=="all" if not "%2"=="benign" goto BRANCH
	  call ipset datevar PCdate
	  PCecho "Running the BENIGN tests @ %datevar% "
	  echo.
	  call dbcreate rep benign
	  if "%2"=="all" goto BRANCH
	  shift
	  goto RUNTEST

:BRANCH
	if not "%2"=="all" if not "%2"=="branch" goto REPMGR
	  call ipset datevar PCdate
	  PCecho "Running the Central-to-Branch tests @ %datevar% "
	  echo.
	  call dbcreate rep branch
	  if "%2"=="all" goto REPMGR
	  shift
	  goto RUNTEST

:REPMGR
	if not "%2"=="all" if not "%2"=="repmgr" goto END
	  call ipset datevar PCdate
	  PCecho "Running the REPMGR tests @ %datevar% "
	  echo.
	  call dbcreate rep repmgr
	  if "%2"=="all" goto END
	  shift
	  goto RUNTEST


:CLEAN
REM ----------------------------------------------------------------------------
REM                        Cleanup Area
REM ----------------------------------------------------------------------------

    call ipset datevar PCdate
    PCecho "Destroying REP/Tests databases @ %datevar% "
    echo raise dbevent dd_stop_server\g| sql repdb1 >>%TST_OUTPUT%\repinit.out
    echo raise dbevent dd_stop_server\g| sql repdb2 >>%TST_OUTPUT%\repinit.out
    echo raise dbevent dd_stop_server\g| sql repdb3 >>%TST_OUTPUT%\repinit.out
    echo raise dbevent dd_stop_server\g| sql repdb4 >>%TST_OUTPUT%\repinit.out
    echo raise dbevent dd_stop_server\g| sql repdb5 >>%TST_OUTPUT%\repinit.out
    sleep 10
    destroydb repdb1 >>%TST_OUTPUT%\repclean.out
    destroydb repdb2 >>%TST_OUTPUT%\repclean.out
    destroydb repdb3 >>%TST_OUTPUT%\repclean.out
    destroydb repdb4 >>%TST_OUTPUT%\repclean.out
    destroydb repdb5 >>%TST_OUTPUT%\repclean.out
    PCecho "Replicator databases are destroyed for clean up"
    PCecho "Cleanup Replicator Services"
    repinst remove

:END
call ipset datevar PCdate
if "%flag%"=="init" PCecho "Finished initializing databases @ %datevar%"
if "%flag%"=="rep" PCecho "End of REP/SEP tests @ %datevar% . . . "
:END2
endlocal
