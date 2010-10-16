@echo off
REM Copyright (c) 2004 Ingres Corporation
REM
REM
REM                SCRIPT FOR RUNNING STAR PHASE1 TESTS
REM                ------------------------------------
REM
REM Description:
REM	This script is used to run the STAR SEP tests using
REM	listexec and excutor.
REM
REM	Flags are: -create (create and fill ldb/ddb databases for star)
REM
REM  History:
REM	??-Dec-1995 (belar01)
REM		Created from runstar.sh .
REM	05-Jan-1996 (somsa01)
REM		Cleaned up batch file to resemble more closely the shell
REM		script.
REM	18-Sep-2000 (vande02)
REM		Added clean up code to destroy databases when desired.
REM	22-Sep-2004 (legru01)
REM		Added code so that script works with the 
REM		MKS toolkit awk or the Cygwin gawk command.
REM ----------------------------------------------------------------------------
REM                        Setup Area
REM ----------------------------------------------------------------------------

REM Set the output directory for test results.
REM

setlocal
if not "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%TST_ROOT_OUTPUT%\star\phase1
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\star\nul PCecho "Creating Directory - %TST_ROOT_OUTPUT%\star"
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\star\nul mkdir %TST_ROOT_OUTPUT%\star

if "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%ING_TST%\output\star\phase1
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul PCecho "Creating Directory - %ING_TST%\output"
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul mkdir %ING_TST%\output
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\star\nul PCecho "Creating Directory - %ING_TST%\output\star"
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\star\nul mkdir %ING_TST%\output\star

if not exist %TST_OUTPUT%\nul PCecho "Creating Directory - %TST_OUTPUT%"
if not exist %TST_OUTPUT%\nul mkdir %TST_OUTPUT%

PCecho "Output files will be written to %TST_OUTPUT%"

set flag=%1

REM Location of star init sep tests
set TST_INIT=%ING_TST%\star\init\sep

REM Location of the star data files
set TST_DATA=%ING_TST%\star\data

REM ----------------------------------------------------------------------------
REM                       End of Setup
REM ----------------------------------------------------------------------------
REM
REM ----------------------------------------------------------------------------
REM                       Initialization Area
REM ----------------------------------------------------------------------------
REM
REM Create and Initialize STAR LDB and DDB databases........

if "%1"=="init" if not "%2"=="" goto INIT
if "%1"=="star" goto NEXT
if "%1"=="clean" goto CLEAN
	PCecho "You must input the modules to be initalized "
	PCecho "or specify "all"."
	echo. 
	PCecho " Example: runstar init all "
	echo.
	PCecho "          or "
	echo.
	PCecho "          runstar init ldbs "
	echo.
	goto END2
:NEXT
if "%1"=="star" if not "%2"=="" goto RUNTEST
	PCecho "You must input the modules to be run "
	PCecho "or specify "all"."
	echo. 
	PCecho " Example: runstar star all "
	echo.
	PCecho "          or "
	echo.
	PCecho "          runstar star ldbs "
	echo.
	goto END2

:INIT 
if "%2"=="" goto END
	if not "%2"=="all" if not "%2"=="ldbs" goto InitDdb
REM Get the SEP test names from the list, because we have to rename
REM the log files later:

REM set variable for the correct file editing command
	SET fedcmd=""
	FOR %%P in (%PATH%) do IF EXIST %%P\gawk.exe SET fedcmd=gawk
	IF %fedcmd%=="" SET fedcmd=awk
	
	  call ipset tmpdir ingprenv II_TEMPORARY
	  cat %TST_CFG%\sinitldb.lis > %tmpdir%\runstar.tmp
	  %fedcmd% 'BEGIN {FS = ":" } {print $3 }' %tmpdir%\runstar.tmp > %tmpdir%\runstar2.tmp
	  del %tmpdir%\runstar.tmp
	  %fedcmd% 'BEGIN {FS = "." } {print $1}' %tmpdir%\runstar2.tmp > %tmpdir%\runstar.tmp
	  del %tmpdir%\runstar2.tmp

REM Everything we do here is straightforward, but we do it seven
REM times:
	  call ipset count expr 0
:LOOP
	  call ipset count expr %count% + 1
	  if "%count%"=="8" goto ENDLOOP
	    call ipset datevar PCdate
	    PCecho "Creating STAR ldb%count% @ %datevar%"
	    echo.
	    call dbcreate starldbs %count%

REM Give the listexec output file and log files a unique name:
	    PCecho "Moving output and log files to unique name."
	    echo.
	    if exist %TST_OUTPUT%\ldb.out copy %TST_OUTPUT%\ldb.out %TST_OUTPUT%\ldb.%count%out
	    if exist %TST_OUTPUT%\ldb.out del %TST_OUTPUT%\ldb.out

	    if exist %TST_OUTPUT%\sldbtbl.log copy %TST_OUTPUT%\sldbtbl.log %TST_OUTPUT%\sldbtbl.%count%log
	    if exist %TST_OUTPUT%\sldbtbl.log del %TST_OUTPUT%\sldbtbl.log
	    call ipset count2 expr 0
:SUBLOOP
	    call ipset count2 expr %count2% + 1
	    call ipset tstname PCread %tmpdir%\runstar.tmp %count2%
	    if "%tstname%"=="" goto LOOP
	      if exist %TST_OUTPUT%\%tstname%.log copy %TST_OUTPUT%\%tstname%.log %TST_OUTPUT%\%tstname%.%count%log
	      if exist %TST_OUTPUT%\%tstname%.log del %TST_OUTPUT%\%tstname%.log
	      goto SUBLOOP
:ENDLOOP
	  del %tmpdir%\runstar.tmp
	  call ipset datevar PCdate
	  PCecho "Finished creating LDB databases @ %datevar%"
	  echo.
	  if "%2"=="all" goto InitDdb
	  shift
	  goto INIT

:InitDdb
	if not "%2"=="all" if not "%2"=="ddb" goto END
	  call ipset datevar PCdate
	  PCecho "Creating STAR database starddb1/star @ %datevar%"
	  call dbcreate starddb
	  call ipset datevar PCdate
	  PCecho "Finished creating DDB databases @ %datevar%"
	  if "%2"=="all" goto END
	  shift
	  goto INIT

:RUNTEST
if "%2"=="" goto END
	if not "%2"=="all" if not "%2"=="qryproc" goto DDL
	  call ipset datevar PCdate
	  PCecho "Running STAR qryproc tests @ %datevar%"
	  echo.
	  if not exist %TST_OUTPUT%\qryproc\nul mkdir %TST_OUTPUT%\qryproc
	  call dbcreate starqprc
	  call ipset datevar PCdate
	  PCecho "Finished the STAR qryproc tests @ %datevar%"
	  if "%2"=="all" goto DDL
	  shift
	  goto RUNTEST

:DDL
	if not "%2"=="all" if not "%2"=="ddl" goto TPF
	  call ipset datevar PCdate
	  PCecho "Running STAR ddl tests @ %datevar%"
	  echo.
	  if not exist %TST_OUTPUT%\ddl\nul mkdir %TST_OUTPUT%\ddl
	  call dbcreate starddl
	  call ipset datevar PCdate
	  PCecho "Finished running ddl tests @ %datevar%"
	  if "%2"=="all" goto TPF
	  shift
	  goto RUNTEST

:TPF
	if not "%2"=="all" if not "%2"=="tpf" goto AGGS
	  call ipset datevar PCdate
	  PCecho "Running STAR tpf tests %datevar%"
	  if not exist %TST_OUTPUT%\tpf\nul mkdir %TST_OUTPUT%\tpf
	  call dbcreate startpf
	  call ipset datevar PCdate
	  PCecho "Finished STAR tpf tests @ %datevar%"
	  if "%2"=="all" goto AGGS
	  shift
	  goto RUNTEST

:AGGS
	if not "%2"=="all" if not "%2"=="aggs" goto PRED
	  call ipset datevar PCdate
	  PCecho "Starting the STAR aggregate tests @ %datevar%"
	  echo.
	  if not exist %TST_OUTPUT%\aggs\nul mkdir %TST_OUTPUT%\aggs
	  call dbcreate staraggs
	  call ipset datevar PCdate
	  PCecho "Finished STAR aggregate tests @ %datevar%"
	  if "%2"=="all" goto PRED
	  shift
	  goto RUNTEST

:PRED
	if not "%2"=="all" if not "%2"=="pred" goto SUBS
	  call ipset datevar PCdate
	  PCecho "Starting the STAR predicate tests @ %datevar%"
	  echo.
	  if not exist %TST_OUTPUT%\pred\nul mkdir %TST_OUTPUT%\pred
	  call dbcreate starpred
	  call ipset datevar PCdate
	  PCecho "Finished STAR predicate tests @ %datevar%
	  if "%2"=="all" goto SUBS
	  shift
	  goto RUNTEST

:SUBS
	if not "%2"=="all" if not "%2"=="subs" goto DBPROC
	  call ipset datevar PCdate
	  PCecho "Starting the STAR subsel tests @ %datevar%"
	  echo.
	  if not exist %TST_OUTPUT%\subsel\nul mkdir %TST_OUTPUT%\subsel
	  call dbcreate starsubs
	  call ipset datevar PCdate
	  PCecho "Finished STAR subsel tests @ %datevar%"
	  if "%2"=="all" goto DBPROC
	  shift
	  goto RUNTEST

:DBPROC
	if not "%2"=="all" if not "%2"=="dbproc" goto END
	  call ipset datevar PCdate
	  PCecho "Starting the STAR dbproc tests @ %datevar%"
	  echo.
	  if not exist %TST_OUTPUT%\dbproc\nul mkdir %TST_OUTPUT%\dbproc
	  call dbcreate stardbproc
	  call ipset datevar PCdate
	  PCecho "Finished STAR dbproc tests @ %datevar%"
	  if "%2"=="all" goto END
	  shift
	  goto RUNTEST

:CLEAN
REM ----------------------------------------------------------------------------
REM                        Cleanup Area
REM ----------------------------------------------------------------------------

if not "%2" == "" goto CleanLoop
	PCecho "You must input the modules to be cleaned up "
	PCecho "or specify "all"."
	echo.
	PCecho " Example: runstar clean all "
	echo.
	PCecho "          or "
	echo.
	PCecho "          runstar clean  ldbs | ddb"
	echo.
	goto END2

:CleanLoop
    if "%2"=="" goto END

:CleanLDBS
    if not "%2"=="all" if not "%2"=="ldbs" goto CleanDDB
	call ipset datevar PCdate
	PCecho "Destroying local STAR databases @ %datevar% "
	destroydb ldb1 >>%TST_OUTPUT%\starclean.out
	destroydb ldb2 >>%TST_OUTPUT%\starclean.out
	destroydb ldb3 >>%TST_OUTPUT%\starclean.out
	destroydb ldb4 >>%TST_OUTPUT%\starclean.out
	destroydb ldb5 >>%TST_OUTPUT%\starclean.out
	destroydb ldb6 >>%TST_OUTPUT%\starclean.out
	destroydb ldb7 >>%TST_OUTPUT%\starclean.out
	if "%2"=="all" goto CleanDDB
	shift
	goto CleanLoop

:CleanDDB
    if not "%2"=="all" if not "%2"=="ddb" goto END
	call ipset datevar PCdate
	PCecho "Destroying STAR database @ %datevar% "
	destroydb starddb1 >>%TST_OUTPUT%\starclean.out
	if "%2"=="all" goto END
	shift
	goto CleanLoop

:END
call ipset datevar PCdate
if "%flag%"=="init" PCecho "All STAR databases are created @ %datevar%"
if "%flag%"=="star" PCecho "End of STAR/SEP tests @ %datevar%"
if "%flag%"=="clean" PCecho "Finished destroying databases @ %datevar% "
:END2
endlocal
