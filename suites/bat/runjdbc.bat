@echo off
REM Copyright (c) 2004 Ingres Corporation
REM
REM
REM                SCRIPT FOR RUNNING LOCAL JDBC TESTS
REM
REM
REM     Description:
REM	This script is used to run the local JDBC SEP tests using
REM 	listexec and executor. 
REM	
REM  History:
REM	28-Nov-01 (sinra04) Created.
REM     14-Jun-04 (legru01) Converted into .bat format for
REM                         jdbc testing
REM	24-Sep-04 (legru01) Modified to display correct 
REM			    database name before db creation.
REM	08-Sep-04 (legru01) Needed more cosmetic changes because
REM			    start date and time failed to display.	
REM	
REM
REM ----------------------------------------------------------------------------
REM		Setup Area
REM ----------------------------------------------------------------------------
REM setting local using TST_ROOT_OUTPUT
REM
setlocal

if not "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%TST_ROOT_OUTPUT%\net\jdbc
if "%TST_ROOT_OUTPUT%"== "" set TST_OUTPUT=%ING_TST%\output\net\jdbc

REM
REM  Creating output directory
REM
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\net\nul PCecho "Creating Directory - %TST_ROOT_OUTPUT%\net"
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\net\nul mkdir %TST_ROOT_OUTPUT%\net
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\net\jdbc\nul PCecho "Creating Directory - %TST_ROOT_OUTPUT%\net\jdbc"
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\net\jdbc\nul mkdir %TST_ROOT_OUTPUT%\net\jdbc
REM
if "%TST_ROOT_OUTPUT%" == "" if not exist %ING_TST%\output\nul PCecho "Creating Directory - %ING_TST%\output "
if "%TST_ROOT_OUTPUT%" == "" if not exist %ING_TST%\output\nul mkdir %ING_TST%\output
if "%TST_ROOT_OUTPUT%" == "" if not exist %ING_TST%\output\net\nul PCecho "Creating Directory - %ING_TST%\output\net" 
if "%TST_ROOT_OUTPUT%" == "" if not exist %ING_TST%\output\net\nul mkdir %ING_TST%\output\net
if "%TST_ROOT_OUTPUT%" == "" if not exist %ING_TST%\output\net\jdbc\nul PCecho "Creating Directory - %ING_TST%\output\net\jdbc" 
if "%TST_ROOT_OUTPUT%" == "" if not exist %ING_TST%\output\net\jdbc\nul mkdir %ING_TST%\output\net\jdbc


echo ----------------------------------------------	 
echo "Output files will be written to %TST_OUTPUT%
echo ----------------------------------------------	
set flag=%1
REM --------------------------------------------------------------------
REM --------------------------------------------------------------------
REM --------------------------------------------------------------------
REM			Initialization Area
REM --------------------------------------------------------------------
REM
REM Start the initialization of JDBC Databases
REM
if "%1"=="init" goto INIT
if "%1"=="jdbc" goto RUNTEST

	echo You must enter init or jdbc 
	echo.
	echo Example: "runjdbc init" or "runjdbc jdbc"
	echo.
	echo 
	goto END2
	

REM --------------------------------------------------------------------
REM create jdbc test Database 
REM --------------------------------------------------------------------

:INIT
	call ipset datevar PCdate
	PCecho "Creating database %SEPPARAMDB% @ %datevar% "    
	destroydb %SEPPARAMDB% > %TST_OUTPUT%\jdbc.out
	createdb %SEPPARAMDB% >> %TST_OUTPUT%\jdbc.out
	goto END

REM --------------------------------------------------------------------
REM			End of Initialization
REM --------------------------------------------------------------------
REM --------------------------------------------------------------------
REM			Main Work Area
REM --------------------------------------------------------------------
REM
REM Run the JDBC/Sep Test Modules.
REM
:RUNTEST
	call ipset datevar PCdate
	PCecho "Running JDBC/SEP tests @ %datevar% " 
	executor %TST_CFG%\jdbc.cfg > %TST_OUTPUT%\jdbc.out
        goto END

REM
:END
if "%flag%"=="init" echo %SEPPARAMDB% database created and ready for testing.
if "%flag%"=="jdbc" call ipset datevar PCdate
if "%flag%"=="jdbc" echo JDBC/SEP tests are complete @ %datevar%
:END2
endlocal
