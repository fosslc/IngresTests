@ECHO OFF
REM Copyright (c) 2007 Ingres Corporation
REM
REM Usage:
REM     Setup Test Environment for Windows
REM
REM History:
REM 	13-Dec-2001 (xu$we01)
REM		Created batch file
REM	06-May-2001 (shafa01)
REM	        06-May-2001 (shafa01)
REM		Added sql statement which will input file rmcmd.sql, 
REM		in order to give testenv rmcmd privileges. Turned
REM		cache_sharing to ON and rmcmd start up count to 1.
REM	10-Oct-2002 (vande02)
REM		Removed the iilink step so most testing will be done with a
REM		generic DBMS Server and put iilink setup in runbe.sh/.bat
REM		exclusively for UDT/SOL testing suites which require udt's.
REM		Most customers use a generic DBMS Server and not udt's.
REM
REM		Removed shared cache ON setting as most testing is done with one
REM		DBMS.  Shared cache is required only when doing testing such as
REM		CTS with multiple DBMS servers and on machines with enough
REM		shared memory configured.
REM
REM		Removed rmcmd count setting to 1 because this is the
REM		installation default.
REM
REM	14-SEP-2004 (sinra04)
REM		Added command to increase opf_memory on DBMS to 320k.
REM	17-Sep-2004 (legru01)
REM		Replaced MKS toolkit "mv" and "cp" commands with the OS
REM		commands "move" and "copy." This this script is longer 
REM		 dependent on third party "mv" and "cp" command.
REM 	19-Sep-2004 (legru01)
REM		Added a "if exist" before the copying ibmpc.map to 
REM             ibmpc_org.map this prevents overwriting it with QA 
REM		version of the ibmpc.map.
REM	03-Mar-2005 (wu$ca01)
REM		Add command to change max_tuple_length to 32767 on STAR server
REM		in order for test sza12.sep to run.
REM	10-Aug-2006	(rogch01)
REM		Force result structure to remain as cheap instead of the new
REM		default of heap to prevent a plethora of diffs.  See change
REM		480264.  Set DBMS stack size to 400,000 to allow the 3gl/c
REM		tests to run clean.
REM     24-Oct-2006 (vande02)
REM             Set date_type_alias to ingresdate to ensure compatibility with
REM             majority of our tests.  The ansidate will be explicitly used
REM             when testing that data type and not be the system default.
REM      4-Jun-2007 (vande02)
REM             Added defpagesz.sep to verify user table default_page_size is
REM             now 8K as of 2006 Release 3 and then reset this parameter to 2K
REM		to be compatible with our existing regression suites.
REM	25-Jul-2007 (vande02)
REM		Removed the date_type_alias setting to ingresdate because this
REM		is now the default at install time.
REM     24-Sep-2007 (vande02)
REM             Increasing stack_size to 400,000 on STAR Server so 1024 column
REM             tests star/ddl/sep/sta07.sep will run successfully.
REM     30-Nov-2007 (vande02)
REM             Removed the prompt/pause commands, minor header corrections.

ECHO Before you run this batch file, you must have done the following:
ECHO ----------------------------------------------------------------
ECHO 1. Login as Ingres user.                                       
ECHO 2. Make sure Ingres Intelligent Database password set correctly 
ECHO 3. Install MKS tools or Cygwin.                                
ECHO 4. Install Ingres, test tools, and test suite.                             
ECHO 5. Customized and "sourced" the environment file tstenv.bat.               
ECHO    The templates can be found in %ING_TST\suites\userenv.      
ECHO ----------------------------------------------------------------

REM -------------------------------------------
REM  Checking environment variables setting 
REM -------------------------------------------

IF (%II_SYSTEM%) == () GOTO E1_SETTING
IF NOT EXIST %II_SYSTEM%\nul GOTO E1_SETTING
IF (%ING_TOOLS%) == () GOTO E1_SETTING
IF NOT EXIST %ING_TOOLS%\bin\qasetuser.exe GOTO E2_SETTING
IF (%ING_TST%) == () GOTO E1_SETTING
IF NOT EXIST %ING_TST%\suites\userenv\ibmpc.map GOTO E3_SETTING
IF NOT EXIST %ING_TST%\suites\userenv\users.sql GOTO E3_SETTING
IF (%TST_SHELL%) == () GOTO E1_SETTING
IF NOT EXIST %TST_SHELL%\Netsetup.bat GOTO E4_SETTING

REM -------------------------------------------
REM  Checking login user
REM -------------------------------------------

CALL ipset SERVER_HOST=iigetres ii.*.config.server_host
CALL ipset USERID id -u -n
IF NOT %USERID%==%SERVER_HOST%\ingres GOTO CHECK_1
GOTO CHECK_2

:CHECK_1
IF NOT %userid%==ingres GOTO MESSAGE_1

:CHECK_2
IF (%TST_OUTPUT%) == () GOTO E1_SETTING
IF EXIST %TST_OUTPUT%\nul GOTO DIR_PROCESS1
IF NOT EXIST %TST_OUTPUT%\nul  GOTO DIR_PROCESS2

REM ------------------------------------
REM Error messages display
REM ------------------------------------

:E1_SETTING
IF (%II_SYSTEM%) == () ECHO II_SYSTEM does not set
IF NOT EXIST %II_SYSTEM%\nul ECHO %II_SYSTEM% does not exist
IF (%ING_TOOLS%) == () ECHO ING_TOOS does not set
IF (%ING_TST%) == () ECHO ING_TST does not set
IF (%TST_SHELL%) == () ECHO TST_SHELL does not set
IF (%TST_OUTPUT%) == () ECHO TST_OUTPUT does not set
GOTO END

:E2_SETTING
ECHO %ING_TOOS%\bin\qasetuser.exe does not exist
GOTO END

:E3_SETTING
IF NOT EXIST %ING_TST%\suites\userenv\ibmpc.map ECHO %ING_TST%\suites\userenv\ibmpc.map does not exist
IF NOT EXIST %ING_TST%\suites\userenv\users.sql ECHO %ING_TST%\suites\userenv\users.sql does not exist
GOTO END

:E4_SETTING
ECHO %TST_SHELL%\Netsetup.bat does not exist
GOTO END

:DIR_PROCESS1
chmod 777 %TST_OUTPUT%
GOTO CONTINUE

:DIR_PROCESS2
MKDIR %TST_OUTPUT%
chmod 777 %TST_OUTPUT%
GOTO CONTINUE

:MESSAGE_1
ECHO You must login as ingres to run this batch file.
GOTO END

:MESSAGE_2
ECHO Ingres failed to start. Please check the errorlog.log, solve the problem, and run it again.
GOTO END

:MESSAGE_3
ECHO Ingres failed to stop. Please check the errorlog.log, solve the problem, and run it again.
GOTO END

REM ------------------------------------
REM Start setup
REM ------------------------------------

:CONTINUE
ECHO Set correct permissions for qasetuser
chmod 4755 %ING_TOOLS%\bin\qasetuser.exe

ECHO Copy file ibmpc.map
IF EXIST %II_SYSTEM%\ingres\files\ibmpc_org.map GOTO SKIPTHIS
move %II_SYSTEM%\ingres\files\ibmpc.map %II_SYSTEM%\ingres\files\ibmpc_org.map
copy %ING_TST%\suites\userenv\ibmpc.map %II_SYSTEM%\ingres\files\
:SKIPTHIS

REM ------------------------------------------------------------------
REM Start the server and verify the user table default_page_size is 8K
REM ------------------------------------------------------------------
 
ECHO Starting the server now to verify the correct user table default page size
ingstart -service
IF ERRORLEVEL 1 GOTO MESSAGE_2

ECHO Executing defpagesz.sep to verify correct user table default_page_size
sep -b %ING_TST%\suites\userenv\defpagesz.sep

REM ------------------------------------
REM Set some server parameters.
REM ------------------------------------

copy %II_SYSTEM%\ingres\files\config.dat %II_SYSTEM%\ingres\files\config_org.dat

iisetres ii.%SERVER_HOST%.gcn.session_limit 26
iisetres ii.%SERVER_HOST%.star.*.connect_limit 26

ECHO Turning all cache sizes ON
iisetres ii.%SERVER_HOST%.dbms.private.*.cache.p2k_status ON
iisetres ii.%SERVER_HOST%.dbms.private.*.cache.p4k_status ON
iisetres ii.%SERVER_HOST%.dbms.private.*.cache.p8k_status ON
iisetres ii.%SERVER_HOST%.dbms.private.*.cache.p16k_status ON
iisetres ii.%SERVER_HOST%.dbms.private.*.cache.p32k_status ON
iisetres ii.%SERVER_HOST%.dbms.private.*.cache.p64k_status ON

ECHO Increasing max_tuple_length on DBMS to 32767
iisetres ii.%SERVER_HOST%.dbms."*".max_tuple_length 32767

ECHO Increasing max_tuple_length on STAR to 32767
iisetres ii.%SERVER_HOST%.star."*".max_tuple_length 32767

ECHO Increasing opf_memory on DBMS to 322870400
iisetres ii.%SERVER_HOST%.dbms."*".opf_memory 322870400

ECHO Setting result_structure on DBMS to cheap
iisetres ii.%SERVER_HOST%.dbms."*".result_structure cheap

ECHO Setting stack_size on DBMS to 400,000
iisetres ii.%SERVER_HOST%.dbms."*".stack_size 400000

ECHO Setting stack_size on STAR to 400,000
iisetres ii.%SERVER_HOST%.star."*".stack_size 400000

ECHO Setting default_page_size on DBMS to 2048
iisetres ii.%SERVER_HOST%.dbms."*".default_page_size 2048

ECHO Setting force abort limit to 80
iisetres ii.%SERVER_HOST%.rcp.log.force_abort_limit   80

ECHO Setting 'testenv' the same privileges as 'administrator'.
CALL ipset ADMIN=iigetres ii.%SERVER_HOST%.privileges.user.administrator
iisetres ii.%SERVER_HOST%.privileges.user.testenv %ADMIN%

ECHO ------------------------------------
ECHO Creating test users using user.sql
ECHO ------------------------------------
sql iidbdb < %ING_TST%\suites\userenv\users.sql

ECHO ------------------------------------
ECHO Giving testenv rmcmd privileges
ECHO ------------------------------------
sql imadb < %ING_TST%\suites\userenv\rmcmd.sql

ECHO ------------------------------------
ECHO Net setting using the NETSETUP.BAT
ECHO ------------------------------------
qasetuser testenv %TST_SHELL%\netsetup.bat

ECHO ---------------------------------------------
ECHO Stopping/restarting installation as a service
ECHO after changing above configuration parameters 
ECHO ---------------------------------------------

ECHO Stopping the server now
ingstop
IF ERRORLEVEL 1 GOTO MESSAGE_3

ECHO Starting the server now
ingstart -service

:DONE

ECHO Setup done

:END
call ipset datevar PCdate
echo.
PCecho "Exiting TSTSETUP @ %datevar% . . .
