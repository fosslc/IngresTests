@echo off
REM Copyright (c) 2004 Ingres Corporation
REM
REM                SCRIPT FOR RUNNING THE FRONTEND TESTS
REM
REM
REM Description:
REM	This script is used to setup and run the frontend SEP tests using
REM listexec and excutor. 
REM
REM  History:
REM	??-Sep-1995 (belar01)
REM		Created from runfe.sh .
REM	04-Jan-1996 (somsa01)
REM		Cleaned up batch file to resemble more closely the shell
REM		script.
REM	16-Jul-1996 (somsa01)
REM		Added InitFormutil.
REM	22-feb-1999 (somsa01)
REM	    Added cleanup segment.
REM	23-feb-1999 (somsa01)
REM	    Fixed typo in cleanup segment.
REM     25-Mar-1997 (chiel02) 
REM             Added COPY and corrected IMAGE.
REM	23-Sep-2004 (legru01)
REM		Modified script so it pertains to the Open Source Acceptance
REM		Package test suites.
REM
REM ---------------------------------------------------------------------------
REM			Setup Area
REM --------------------------------------------------------------------------

REM Set the output directory for test results.
REM
setlocal
if not "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%TST_ROOT_OUTPUT%\fe\local
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\fe\nul PCecho "Creating Directory - %TST_ROOT_OUTPUT%\fe"
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\fe\nul mkdir %TST_ROOT_OUTPUT%\fe

if "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%ING_TST%\output\fe\local
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul PCecho "Creating Directory - %ING_TST%\output"
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul mkdir %ING_TST%\output
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\fe\nul PCecho "Creating Directory - %ING_TST%\output\fe
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\fe\nul mkdir %ING_TST%\output\fe

if not exist %TST_OUTPUT%\nul PCecho "Creating Directory - %TST_OUTPUT%"
if not exist %TST_OUTPUT%\nul mkdir %TST_OUTPUT%

REM Variable to determine which sep tests to run if running frontend tests
REM against STAR database. If false, run all parts of the sep test. If true,
REM skip the db proc part of the sep test.

set SEPPARAM_STAR=false

echo -------------------------------------------
echo Output files will fe written to %TST_OUTPUT%
echo -------------------------------------------

set flag=%1

if "%1"=="init" if not "%2"=="" goto INIT
if "%1"=="fe" goto NEXT
if "%1"=="clean" goto CLEAN
	echo You must input the modules to fe initalized 
	echo or specify "all".
	echo.
	echo  Example: runfe init all 
	echo           or 
	echo.
	echo           runfe init abf copy 
	echo.
	goto END2
:NEXT
if "%1"=="fe" if not "%2"=="" goto RUNTEST
	echo You must input the modules to fe run
	echo or specify all
	echo.
	echo  Example: runfe fe all
	echo           or
	echo.
	echo           runfe fe abf copy
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
	if not "%2"=="all" if not "%2"=="4gl" goto InitAbf
	  echo Creating Directory - %TST_OUTPUT%\4gl
	  mkdir %TST_OUTPUT%\4gl
	  call ipset datevar PCdate
	  PCecho " Initializing 4GL database @ %datevar%"
	  call dbcreate init 4gl
	  if "%2"=="all" goto InitAbf
	  shift
	  goto INIT

:InitAbf
	if not "%2"=="all" if not "%2"=="abf" goto InitImage
	  echo Creating Directory - %TST_OUTPUT%\abf
	  mkdir %TST_OUTPUT%\abf
	  call ipset datevar PCdate
	  PCecho " Initializing ABF databases @ %datevar%"
	  call dbcreate init abf
	  if "%2"=="all" goto InitImage
	  shift
	  goto INIT

:InitImage
	if not "%2"=="all" if not "%2"=="image" goto InitCopy
	  echo Creating Directory - %TST_OUTPUT%\image
	  mkdir %TST_OUTPUT%\image
	  call ipset datevar PCdate
	  PCecho " Initializing ImageApp database @ %datevar%"
	  call dbcreate init image
	  if "%2"=="all" goto InitCopy
	  shift
	  goto INIT

:InitCopy
	if not "%2"=="all" if not "%2"=="copy" goto InitFrs
	  echo Creating Directory - %TST_OUTPUT%\copy
	  mkdir %TST_OUTPUT%\copy
	  call ipset datevar PCdate
	  PCecho " Initializing COPY database @ %datevar%"
	  call dbcreate init copy
	  if "%2"=="all" goto InitFrs
	  shift
	  goto INIT

:InitFrs
	if not "%2"=="all" if not "%2"=="frs" goto InitFstm
	  echo Creating Directory - %TST_OUTPUT%\frs
	  mkdir %TST_OUTPUT%\frs
	  call ipset datevar PCdate
	  PCecho " Initializing FRS database @ %datevar%"
	  call dbcreate init frs
	  if "%2"=="all" goto InitFstm
	  shift
	  goto INIT

:InitFstm
	if not "%2"=="all" if not "%2"=="fstm" goto InitGenutil
	  echo Creating Directory - %TST_OUTPUT%\fstm
	  mkdir %TST_OUTPUT%\fstm
	  call ipset datevar PCdate
	  PCecho " Initializing FSTM database @ %datevar%"
	  call dbcreate init fstm
	  if "%2"=="all" goto InitGenutil
	  shift
	  goto INIT

:InitGenutil
	if not "%2"=="all" if not "%2"=="genutil" goto InitQbf
	  echo Creating Directory - %TST_OUTPUT%\genutil
	  mkdir %TST_OUTPUT%\genutil
	  call ipset datevar PCdate
	  PCecho " Initializing GENUTIL database @ %datevar%"
	  call dbcreate init genutil
	  if "%2"=="all" goto InitQbf
	  shift
	  goto INIT

:InitQbf
	if not "%2"=="all" if not "%2"=="qbf" goto InitRbf
	  echo Creating Directory - %TST_OUTPUT%\qbf
	  mkdir %TST_OUTPUT%\qbf
	  call ipset datevar PCdate
	  PCecho " Initializing QBF database @ %datevar%"
	  call dbcreate init qbf
	  if "%2"=="all" goto InitRbf
	  shift
	  goto INIT

:InitRbf
	if not "%2"=="all" if not "%2"=="rbf" goto InitRw
	  echo Creating Directory - %TST_OUTPUT%\rbf
	  mkdir %TST_OUTPUT%\rbf
	  call ipset datevar PCdate
	  PCecho " Initializing RBF database @ %datevar%"
	  call dbcreate init rbf
	  if "%2"=="all" goto InitRw
	  shift
	  goto INIT

:InitRw
	if not "%2"=="all" if not "%2"=="rw" goto InitVifred
	  echo Creating Directory - %TST_OUTPUT%\rw
	  mkdir %TST_OUTPUT%\rw
	  call ipset datevar PCdate
	  PCecho " Initializing REPORT WRITER database @ %datevar%"
	  call dbcreate init rw
	  if "%2"=="all" goto InitVifred
	  shift
	  goto INIT

:InitVifred
	if not "%2"=="all" if not "%2"=="vifred" goto InitVision
	  echo Creating Directory - %TST_OUTPUT%\vifred
	  mkdir %TST_OUTPUT%\vifred
	  call ipset datevar PCdate
	  PCecho " Initializing Vifred database @ %datevar%"
	  call dbcreate init vifred
	  if "%2"=="all" goto InitVision
	  shift
	  goto INIT

:InitVision
	if not "%2"=="all" if not "%2"=="vision" goto END
	  echo Creating Directory - %TST_OUTPUT%\vision
	  mkdir %TST_OUTPUT%\vision
	  call ipset datevar PCdate
	  PCecho " Initializing Vision database @ %datevar%"
	  call dbcreate init vision
	  if "%2"=="all" goto END
	  shift
	  goto INIT

:RUNTEST
if "%2"=="" goto END
	if not "%2"=="all" if not "%2"=="4gl" goto ABF
	  call ipset datevar PCdate
	  PCecho " Running the 4gl tests @ %datevar%     . . ."
	  call dbcreate fe 4gl
	  if "%2"=="all" goto ABF
	  shift
	  goto RUNTEST

:ABF
	if not "%2"=="all" if not "%2"=="abf" goto COPY
	  call ipset datevar PCdate
	  PCecho " Running the abf tests @ %datevar%     . . ."
	  call dbcreate fe abf
	  if "%2"=="all" goto COPY
	  shift
	  goto RUNTEST

:COPY
	if not "%2"=="all" if not "%2"=="copy" goto IMAGE
	  call ipset datevar PCdate
	  PCecho " Running the copy tests @ %datevar%     . . ."
	  call dbcreate fe copy
	  if "%2"=="all" goto IMAGE
	  shift
	  goto RUNTEST

:IMAGE
	if not "%2"=="all" if not "%2"=="image" goto FRS
	  call ipset datevar PCdate
	  PCecho " Running the image tests @ %datevar%     . . ."
	  call dbcreate fe image
	  if "%2"=="all" goto FRS
	  shift
	  goto RUNTEST

:FRS
	if not "%2"=="all" if not "%2"=="frs" goto FSTM
	  call ipset datevar PCdate
	  PCecho " Running the frs tests @ %datevar%     . . ."
	  call dbcreate fe frs
	  if "%2"=="all" goto FSTM
	  shift
	  goto RUNTEST

:FSTM
	if not "%2"=="all" if not "%2"=="fstm" goto QBF
	  call ipset datevar PCdate
	  PCecho " Running the fstm tests @ %datevar%     . . ."
	  call dbcreate fe fstm
	  if "%2"=="all" goto QBF
	  shift
	  goto RUNTEST

:QBF
	if not "%2"=="all" if not "%2"=="qbf" goto RBF
	  call ipset datevar PCdate
	  PCecho " Running the qbf tests @ %datevar%     . . ."
	  call dbcreate fe qbf
	  if "%2"=="all" goto RBF
	  shift
	  goto RUNTEST

:RBF
	if not "%2"=="all" if not "%2"=="rbf" goto RW
	  call ipset datevar PCdate
	  PCecho " Running the rbf tests @ %datevar%     . . ."
	  call dbcreate fe rbf
	  if "%2"=="all" goto RW
	  shift
	  goto RUNTEST

:RW
	if not "%2"=="all" if not "%2"=="rw" goto VISION
	  call ipset datevar PCdate
	  PCecho " Running the rw tests @ %datevar%     . . ."
	  call dbcreate fe rw
	  if "%2"=="all" goto VISION
	  shift
	  goto RUNTEST

:VISION
	if not "%2"=="all" if not "%2"=="vision" goto VIFRED
	  call ipset datevar PCdate
	  PCecho " Running the vision tests @ %datevar%     . . ."
	  call dbcreate fe vision
	  if "%2"=="all" goto VIFRED
	  shift
	  goto RUNTEST

:VIFRED
	if not "%2"=="all" if not "%2"=="vifred" goto GENUTIL
	  call ipset datevar PCdate
	  PCecho " Running the vifred tests @ %datevar%     . . ."
	  call dbcreate fe vifred
	  if "%2"=="all" goto GENUTIL
	  shift
	  goto RUNTEST

:GENUTIL
        if not "%2"=="all" if not "%2"=="genutil" goto END
	  call ipset datevar PCdate
	  PCecho " Running the genutil tests @ %datevar%     . . ."
	  call dbcreate fe genutil
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
	PCecho " Example: runfe clean all "
	echo.
	PCecho "          or "
	echo.
	PCecho "          runfe clean 4gl abf image ... "
	echo.
	goto END2

:CleanLoop
    if "%2"=="" goto END

:Clean4gl
    if not "%2"=="all" if not "%2"=="4gl" goto CleanAbf
	call ipset datevar PCdate
	PCecho "Destroying 4GL database @ %datevar% "
	destroydb abf4gldb >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanAbf
	shift
	goto CleanLoop

:CleanAbf
    if not "%2"=="all" if not "%2"=="abf" goto CleanImage
	call ipset datevar PCdate
	PCecho "Destroying ABF databases @ %datevar% "
	destroydb abfdb1 >>%TST_OUTPUT%\feclean.out
	destroydb abfdb2 >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanImage
	shift
	goto CleanLoop

:CleanImage
    if not "%2"=="all" if not "%2"=="image" goto CleanCopy
	call ipset datevar PCdate
	PCecho "Destroying ImageApp databases @ %datevar% "
	destroydb imagedb >>%TST_OUTPUT%\feclean.out
	destroydb imagedb2 >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanCopy
	shift
	goto CleanLoop

:CleanCopy
    if not "%2"=="all" if not "%2"=="copy" goto CleanFrs
	call ipset datevar PCdate
	PCecho "Destroying COPY database @ %datevar% "
	destroydb copydb1 >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanFrs
	shift
	goto CleanLoop

:CleanFrs
    if not "%2"=="all" if not "%2"=="frs" goto CleanFstm
	call ipset datevar PCdate
	PCecho "Destroying FRS database @ %datevar% "
	destroydb frsdb >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanFstm
	shift
	goto CleanLoop

:CleanFstm
    if not "%2"=="all" if not "%2"=="fstm" goto CleanQbf
	call ipset datevar PCdate
	PCecho "Destroying FSTM database @ %datevar% "
	destroydb basisdb >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanQbf
	shift
	goto CleanLoop

:CleanQbf
    if not "%2"=="all" if not "%2"=="qbf" goto CleanRbf
	call ipset datevar PCdate
	PCecho "Destroying QBF database @ %datevar% "
	destroydb qbf1 >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanRbf
	shift
	goto CleanLoop

:CleanRbf
    if not "%2"=="all" if not "%2"=="rbf" goto CleanRw
	call ipset datevar PCdate
	PCecho "Destroying RBF database @ %datevar% "
	destroydb rbfdb1 >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanRw
	shift
	goto CleanLoop

:CleanRw
    if not "%2"=="all" if not "%2"=="rw" goto CleanVifred
	call ipset datevar PCdate
	PCecho "Destroying REPORT WRITER database @ %datevar% "
	destroydb rwdb >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanVifred
	shift
	goto CleanLoop

:CleanVifred
    if not "%2"=="all" if not "%2"=="vifred" goto CleanVision
	call ipset datevar PCdate
	PCecho "Destroying Vifred database @ %datevar% "
	destroydb vifred1 >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanVision
	shift
	goto CleanLoop

:CleanVision
    if not "%2"=="all" if not "%2"=="vision" goto CleanGenutil
	call ipset datevar PCdate
	PCecho "Destroying Vision database @ %datevar% "
	destroydb gourmet >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto CleanGenutil
	shift
	goto CleanLoop

:CleanGenutil
    if not "%2"=="all" if not "%2"=="genutil" goto END
	call ipset datevar PCdate
	PCecho "Destroying Genutil database @ %datevar% "
	destroydb genutildb >>%TST_OUTPUT%\feclean.out
	if "%2"=="all" goto END
	shift
	goto CleanLoop

:END
call ipset datevar PCdate
if "%flag%"=="init" PCecho "Finished initializing databases @ %datevar%"
if "%flag%"=="fe" PCecho "End of FE/SEP tests @ %datevar% . . . "
if "%flag%"=="clean" PCecho "Finished destroying databases @ %datevar% "
:END2
endlocal
