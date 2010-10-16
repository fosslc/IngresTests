@echo off
REM               SCRIPT FOR RUNNING LOCAL NET TESTS
REM
REM
REM Description:
REM	This script is used to run the local NET SEP tests using
REM	listexec and excutor.
REM
REM  History:
REM	??-Dec-1995 (belar01)
REM		Created from runlbnet.sh .
REM	04-Jan-1996 (somsa01)
REM		Cleaned up batch file to resemble more closely the shell script.
REM	22-feb-1999 (somsa01)
REM	    Added clean option.
REM
REM ----------------------------------------------------------------------------
REM                       Setup Area
REM ----------------------------------------------------------------------------

REM            Set the output directory for test results.
REM 
setlocal
if not "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%TST_ROOT_OUTPUT%\net\lback
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\net\nul PCecho "Creating Directory - %TST_ROOT_OUTPUT%\net"
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\net\nul mkdir %TST_ROOT_OUTPUT%\net

if "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%ING_TST%\output\net\lback
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul PCecho "Creating Directory - %ING_TST%\output"
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul mkdir %ING_TST%\output
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\net\nul PCecho "Creating Directory - %ING_TST%\output\net"
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\net\nul mkdir %ING_TST%\output\net

if not exist %TST_OUTPUT%\nul PCecho "Creating Directory - %TST_OUTPUT%"
if not exist %TST_OUTPUT%\nul mkdir %TST_OUTPUT%

echo Output files will be written to %TST_OUTPUT%

set flag=%1

REM ----------------------------------------------------------------------------
REM                        End of Setup
REM ----------------------------------------------------------------------------
REM
REM ----------------------------------------------------------------------------
REM                        Initialization Area
REM ----------------------------------------------------------------------------
REM 

if "%1"== "init" if not "%2"=="" goto INIT
if "%1"=="lbnet" goto NEXT
if "%1"=="clean" goto CLEAN
	PCecho "You must input the modules to be initalized "
	PCecho "or specify "all"."
	echo.
	PCecho " Example: runlbnet init all "
	echo.
	PCecho "          or "
	echo.
	echo "            runlbnet init gaa gca "
	echo.
	goto END2
:NEXT
if "%1"== "lbnet" if not "%2"=="" goto RUNTEST
	PCecho "You must input the modules to be run "
	PCecho "or specify "all"."
	echo.
	PCecho " Example: runlbnet lbnet all "
	echo.
	PCecho "          or "
	echo.
	echo "            runlbnet lbnet gaa gca "
	echo.
	goto END2

:INIT
if "%2"=="" goto END
	if not "%2"=="all" if not "%2"=="gaa" goto InitGba
	  call ipset datevar PCdate
	  PCecho "Creating gaa database @ %datevar%"
	  echo.
	  call dbcreate init gaa
	  call ipset datevar PCdate
	  PCecho "Finished creating gaa database @ %datevar%"
	  echo.
	  if "%2"=="all" goto InitGba
	  shift 
	  goto INIT

:InitGba
	if not "%2"=="all" if not "%2"=="gba" goto InitGca
	  call ipset datevar PCdate
	  PCecho "Creating gba database @ %datevar%"
	  echo.
	  call dbcreate init gba
	  call ipset datevar PCdate
	  PCecho "Finished creating gba database @ %datevar%"
	  echo.
	  if "%2"=="all" goto InitGca
	  shift
	  goto INIT

:InitGca
	if not "%2"=="all" if not "%2"=="gca" goto END
	  call ipset datevar PCdate
	  PCecho "Creating gca database @ %datevar%"
	  echo.
	  call dbcreate init gca
	  call ipset datevar PCdate
	  PCecho "Finished creating gca database @ %datevar%"
	  echo.
	  if "%2"=="all" goto END
	  shift
	  goto INIT

:RUNTEST
if "%2"=="" goto END
	if not "%2"=="all" if not "%2"=="gaa" goto GBA
	  call ipset datevar PCdate
	  PCecho "Starting GAA sep tests @ %datevar%"
	  echo.
	  call dbcreate lbnet gaa
	  call ipset datevar PCdate
	  PCecho "Finshed with GAA sep tests @ %datevar%"
	  echo.
	  if "%2"=="all" goto GBA
	  shift
	  goto RUNTEST

:GBA
	if not "%2"=="all" if not "%2"=="gba" goto GCA
	  call ipset datevar PCdate
	  PCecho "Starting GBA sep tests @ %datevar%"
	  echo.
	  call dbcreate lbnet gba
	  call ipset datevar PCdate
	  PCecho "Finshed with GBA sep tests @ %datevar%"
	  echo.
	  if "%2"=="all" goto GCA
	  shift
	  goto RUNTEST

:GCA
	if not "%2"=="all" if not "%2"=="gca" goto END
	  call ipset datevar PCdate
	  PCecho "Starting GCA sep tests @ %datevar%"
	  echo.
	  call dbcreate lbnet gca
	  call ipset datevar PCdate
	  PCecho "Finshed with GCA sep tests @ %datevar%"
	  echo.
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
	PCecho " Example: runlbnet clean all "
	echo.
	PCecho "          or "
	echo.
	PCecho "          runlbnet clean gaa gca "
	echo.
	goto END2

:CleanLoop
    if "%2"=="" goto END

:CleanGaa
    if not "%2"=="all" if not "%2"=="gaa" goto CleanGba
	call ipset datevar PCdate
	PCecho "Destroying gaa database @ %datevar% "
	echo.
	destroydb gaadb >>%TST_OUTPUT%\netclean.out
	call ipset datevar PCdate
	PCecho "Finished Destroying gaa database @ %datevar% "
	echo.
	if "%2"=="all" goto CleanGba
	shift
	goto CleanLoop

:CleanGba
    if not "%2"=="all" if not "%2"=="gba" goto CleanGca
	call ipset datevar PCdate
	PCecho "Destroying gba database @ %datevar% "
	echo.
	destroydb gbadb >>%TST_OUTPUT%\netclean.out
	call ipset datevar PCdate
	PCecho "Finished Destroying gba database @ %datevar% "
	echo.
	if "%2"=="all" goto CleanGca
	shift
	goto CleanLoop

:CleanGca
    if not "%2"=="all" if not "%2"=="gca" goto END
	call ipset datevar PCdate
	PCecho "Destroying gca database @ %datevar% "
	echo.
	destroydb gcadb >>%TST_OUTPUT%\netclean.out
	call ipset datevar PCdate
	PCecho "Finished Destroying gca database @ %datevar% "
	echo.
	if "%2"=="all" goto END
	shift
	goto CleanLoop

:END
if "%flag%"=="init" PCecho "Databases for local net testing have been created."
if "%flag%"=="lbnet" call ipset datevar PCdate
if "%flag%"=="lbnet" PCecho "End of NET/SEP tests @ %datevar%"
if "%flag%"=="clean" PCecho "Databases for local net testing have been destroyed. "
:END2
endlocal
