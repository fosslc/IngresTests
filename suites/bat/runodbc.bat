@echo off
REM               SCRIPT FOR RUNNING ODBC TESTS
REM
REM
REM Description:
REM	This script is used to run the odbc tests using listexec and excutor.
REM
REM  History:
REM	24-May-2001 (rogch01)
REM		Created from runnet.bat.
REM ----------------------------------------------------------------------------
REM                       Setup Area
REM ----------------------------------------------------------------------------

REM            Set the output directory for test results.
REM 
setlocal
if not "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%TST_ROOT_OUTPUT%\net\odbc
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\net\nul PCecho "Creating Directory - %TST_ROOT_OUTPUT%\net"
if not "%TST_ROOT_OUTPUT%"=="" if not exist %TST_ROOT_OUTPUT%\net\nul mkdir %TST_ROOT_OUTPUT%\net

if "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%ING_TST%\output\net\odbc
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul PCecho "Creating Directory - %ING_TST%\output"
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul mkdir %ING_TST%\output
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\net\nul PCecho "Creating Directory - %ING_TST%\output\net"
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\net\nul mkdir %ING_TST%\output\net

if not exist %TST_OUTPUT%\nul PCecho "Creating Directory - %TST_OUTPUT%"
if not exist %TST_OUTPUT%\nul mkdir %TST_OUTPUT%

echo Output files will be written to %TST_OUTPUT%

REM ----------------------------------------------------------------------------
REM                        End of Setup
REM ----------------------------------------------------------------------------
REM
REM ----------------------------------------------------------------------------
REM                        Initialization Area
REM ----------------------------------------------------------------------------
REM 

if "%1"== "init" goto INIT
if "%1"=="odbc" goto RUNTEST
	PCecho "You must specify init or odbc "
	echo.
	PCecho " Example: runodbc init "
	echo.
	PCecho "          or "
	echo.
	PCecho "          runodbc odbc "
	echo.
	goto END2

:INIT
	call ipset datevar PCdate
	if "%SEPPARAM_NODE%" == "" set SEPPARAM_NODE=odbcdb
	PCecho "Creating %SEPPARAM_NODE% database @ %datevar%"
	echo.
	call dbnet init odbc
	call ipset datevar PCdate
	PCecho "Finished creating %SEPPARAM_NODE% database @ %datevar%"
	echo.
	goto END

:RUNTEST
	call ipset datevar PCdate
	if "%SEPPARAM_NODE%" == "" set SEPPARAM_NODE=odbcdb
	PCecho "Starting ODBC sep tests @ %datevar%"
	echo.
	call dbnet odbc odbc
	call ipset datevar PCdate
	PCecho "Finshed with ODBC sep tests @ %datevar%"
	echo.

:END
if "%1"=="init" PCecho "Databases for ODBC testing have been created."
if "%1"=="odbc" call ipset datevar PCdate
if "%1"=="odbc" PCecho "End of ODBC tests @ %datevar%"
:END2
endlocal
