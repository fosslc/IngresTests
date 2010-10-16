@echo off
REM Copyright (c) 2008 Ingres Corporation
REM
REM SCRIPT FOR RUNNING VPS BACK-END TESTS FOR WINDOWS
REM
REM
REM Description:
REM	This script is used to setup and run the backend VPS SEP tests using
REM     listexec and executor. User will be able to test six different
REM     page sizes   (2K 4K 8K 16K 32K 64K), and four table structures
REM     (heap, hash, btree, isam ) for each page size.
REM
REM Command line :  runbevps 'pagesize' 'table' init/vps
REM     examples :  runbevps 4 heap init 
REM                 runbevps 16 hash vps
REM
REM     Notice that 'pagesize' is number equal page_size(s) user want to run,
REM            for more than one pagesize user have to put SUM of page sizes,
REM            for example: for 2K and 4K        enter=6,
REM                         for 8K and 32K       enter=40,
REM                         for all pagesizes    enter=126.
REM
REM IMPORTANT:  MKS Toolkit must be installed !
REM
REM     Notice that 'table' can take values: heap,hash,btree,isam,all.
REM
REM     
REM          
REM  History:
REM 03-Mar-1999 (miski01) Created from runbe.bat 
REM 12-Apr-2007 (vande02) Modified init step to allow Unicode data types
REM			  in vpsdb.
REM 12-Jun-2008 (vande02) General cleanup by removing pause/color statements,
REM                       and updated header.
REM
REM ---------------------------------------------------------------------------
REM			Setup Area
REM --------------------------------------------------------------------------

setlocal

REM    Set the output directory for test results.

if not "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%TST_ROOT_OUTPUT%\be

if "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%ING_TST%\output\be
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul PCecho "Creating Directory - %ING_TST%\output
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul mkdir %ING_TST%\output

if not exist %TST_OUTPUT%\nul PCecho "Creating Directory - %TST_OUTPUT%"
if not exist %TST_OUTPUT%\nul mkdir %TST_OUTPUT%

echo               --------------------------------------------
echo               Output files will be written to %TST_OUTPUT%
echo               --------------------------------------------

set PS=%1
set TS=%2
if "%PS%" == "all" set PS=126
if "%PS%" == "ALL" set PS=126
goto RIGHT

:RIGHT
call test %PS% -lt 2
if errorlevel 1 goto SUBRIGHT
echo.
echo ***   ERROR in PAGE_SIZE, please set it to one of 
echo ***   2, 4, 8, 16, 32, 64, or SUM of the page sizes 
echo.
echo ***   Syntax:
echo              runbevps pagesize tablestruct vps/init
echo.
goto END2

:SUBRIGHT
call test %PS% -gt 126
if errorlevel 1 goto CHEKTAB
echo.
echo ***   ERROR in PAGE_SIZE, please set it to one of 
echo ***   2, 4, 8, 16, 32, 64, or SUM of the page sizes 
echo.
echo ***   Syntax:
echo              runbevps pagesize tablestruct vps/init
echo.
goto END2 


:CHEKTAB

if "%TS%"=="heap" goto CHEKPARM
if "%TS%"=="hash" goto CHEKPARM
if "%TS%"=="btree" goto CHEKPARM
if "%TS%"=="isam" goto CHEKPARM
if "%TS%"=="HEAP" goto CHEKPARM
if "%TS%"=="HASH" goto CHEKPARM
if "%TS%"=="BTREE" goto CHEKPARM
if "%TS%"=="ISAM" goto CHEKPARM
if "%TS%"=="all" goto CHEKPARM
if "%TS%"=="ALL" goto CHEKPARM
echo.
echo ***   ERROR in TABLE structure, please set it to
echo ***   heap, hash, btree, isam, or all
echo.
echo ***   Syntax:
echo              runbevps pagesize tablestruct vps/init area(s)
echo.
goto END2

:CHEKPARM

set flag=%3

if "%3"=="init" goto INIT
if "%3"=="vps" goto RUNTEST
	echo ***  User must specify what to do. 
	echo.
	echo ***  Example: runbevps 32 heap init
	echo               or 
	echo               runbevps 32 heap vps
	echo.
	goto END2

rem ----------------------------------------------------------------------------
rem                        Initialization Area
rem ----------------------------------------------------------------------------

:INIT
if not exist %TST_OUTPUT%\vps\nul echo Creating Directory - %TST_OUTPUT%\vps
if not exist %TST_OUTPUT%\vps\nul mkdir %TST_OUTPUT%\vps
call ipset datevar PCdate
PCecho " Creating BE\VPS database @ %datevar% . . ."
destroydb vpsdb >>%TST_OUTPUT%\beinit.out
createdb -n vpsdb >>%TST_OUTPUT%\beinit.out
set AREA=vps
call runinit 
goto END


:RUNTEST
set SEPPARAMDB=vpsdb
set AREA=vps
set CFG_FILE=bevps.cfg
set LIS_FILE=bevps.lis
set OUT_FILE=bevps.out
call ipset datevar PCdate
PCecho " Running the BE VPS tests @ %datevar%    . . ."
call rundvbe
goto END


:END
if "%flag%"=="init" echo Backend databases are created and ready for testing.
if "%flag%"=="vps" call ipset datevar PCdate
if "%flag%"=="vps" echo Backend vps tests are complete at %datevar%

:END2
endlocal
title Done  

