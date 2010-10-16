@echo off
REM Copyright (c) 2008 Ingres Corporation
REM
REM Usage:
REM       Script to run back-end VPS SEP Tests for Windows and this script
REM       is called from RUNBEVPS.BAT
REM                        
REM	History:
REM	  03-Mar-1999  <miski01> created
REM	  28-Mar-2006  <mosmi04> replaced four sed commands with one awk
REM                              command and removed the redundant erase
REM                              commands for sed txt files.
REM       11-Jun-2008  (vande02) Changed II_CONFIG to ING_CONFIG, fixed header.
REM       16-Jun-2008  (vande02) Added check if MKS or CYGWIN is being used to
REM                              then use the appropriate awk command.
REM       25-Jun-2008  (vande02) Changed %ING_CONFIG% to what it literally needs
REM                              to be %II_SYSTEM%\ingres\files.
REM       28-Jun-2010  (kschendel)  Use %AWK_CMD% for correct awk.

setlocal

REM AWK_CMD should be set by bldenv, but check here just in case it's not.
REM Certainly USE_CYGWIN needs to be set (the build depends on it being right).
REM
if not "%AWK_CMD%"=="" goto AWKOK
set AWK_CMD=gawk
if "%USE_CYGWIN%"=="" set AWK_CMD=awk
:AWKOK

set total=%PS%
set x=64

REM set variable for the correct file editing command
SET fedcmd=""
FOR %%P in (%PATH%) do IF EXIST %%P\gawk.exe SET fedcmd=gawk
IF %fedcmd%=="" SET fedcmd=awk

goto MAIN

:MAIN
call test %total% -lt %x%
if errorlevel 1 goto SUB
REM if %total% GEQ %x% goto SUB
if "%x%"=="4"  set x=2
if "%x%"=="8"  set x=4
if "%x%"=="16" set x=8
if "%x%"=="32" set x=16
if "%x%"=="64" set x=32
if "%total%" == "0" goto ENDO
goto MAIN

:SUB
if "%TS%"=="heap" set t=1
if "%TS%"=="HEAP" set t=1
if "%TS%"=="hash" set t=2
if "%TS%"=="HASH" set t=2
if "%TS%"=="btree" set t=3
if "%TS%"=="BTREE" set t=3
if "%TS%"=="isam" set t=4
if "%TS%"=="ISAM" set t=4
if "%TS%"=="all" set t=1
if "%TS%"=="ALL" set t=1

if "%x%"=="2" set PAGE=2048
if "%x%"=="2" echo  ***  page=2048  ***
if "%x%"=="2" goto LOOP2
REM ===========================
if "%x%"=="4" set PAGE=4096
if "%x%"=="4" echo  ***  page=4096  ***
if "%x%"=="4" goto LOOP2
REM ===========================
if "%x%"=="8" set PAGE=8192
if "%x%"=="8" echo  ***  page=8192  ***
if "%x%"=="8" goto LOOP2
REM ===========================
if "%x%"=="16" set PAGE=16384
if "%x%"=="16" echo  ***  page=16384  ***
if "%x%"=="16" goto LOOP2
REM ===========================
if "%x%"=="32" set PAGE=32768
if "%x%"=="32" echo  ***  page=32768  ***
if "%x%"=="32" goto LOOP2
REM ===========================
if "%x%"=="64" set PAGE=65536
if "%x%"=="64" echo  ***  page=65536  ***
if "%x%"=="64" goto LOOP2
REM ===========================
goto ENDO

:LOOP2
if "%x%"=="2" goto SETTAB
REM check to see if the cache for the page size is turned on.
findstr "cache\.p%x%k.*ON" %II_SYSTEM%\ingres\files\config.dat > tmp.out
if not errorlevel 1 goto SETTAB
echo.
echo *****************************************************
echo *                   ERROR                           *
echo *   The DBMS Server is not set for %x%K page size.  *
echo *   Use CBF to turn page size ON.                   *
echo *****************************************************
echo.
erase tmp.out
goto CHEKPS

:SETTAB
if exist tmp.out erase tmp.out
if "%t%"=="1" set TABLE=heap
if "%t%"=="1" echo  === TABLE=heap  ===
if "%t%"=="1" goto LOOP3
REM ----------------------------------------------------------
if "%t%"=="2" set TABLE=hash
if "%t%"=="2" echo  === TABLE=hash  ===
if "%t%"=="2" goto LOOP3
REM ----------------------------------------------------------
if "%t%"=="3" set TABLE=btree
if "%t%"=="3" echo  ===  TABLE=btree  ===
if "%t%"=="3" goto LOOP3
REM ----------------------------------------------------------
if "%t%"=="4" set TABLE=isam
if "%t%"=="4" echo  ===  TABLE=isam  ===
if "%t%"=="4" goto LOOP3
REM ----------------------------------------------------------
goto SUBEND

:LOOP3

REM  ============ do main work here =============

set TST_OUTPUT=%TST_OUTPUT%\%AREA%\%x%K\%TABLE%
if exist %TST_OUTPUT% goto CONTINUE
echo.
echo Will not process %AREA% tests on %x%K pages with %TABLE% storage structure.
echo because the output directory does not exist.  
echo %TST_OUTPUT%
echo.
goto DOT1

:CONTINUE
echo.
echo Running the %x%K\%TABLE%  %AREA% Tests .........
echo.
echo.

REM Check if MKS is being used.
REM Believe the USE_CYGWIN setting which bldenv generates, if available.
if not "%USE_CYGWIN"=="" goto QA_USE_CYGWIN
if "%QA_USE_MKS%"=="TRUE" goto QA_USE_MKS
SET QA_USE_MKS=
which cygpath.exe > nul: 2>&1
if errorlevel 0 goto QA_USE_CYGWIN
SET QA_USE_MKS=TRUE

:QA_USE_MKS
echo Use MKS Toolkit version of this awk command
type %TST_LISTEXEC%\bevps.lis|%AWK_CMD% -F: -v page=%PAGE% -v table=%TABLE% -v area=%AREA% -v lisfile=%LIS_FILE% -v ingtst=%%ING_TST%% -v listexec=%%TST_LISTEXEC%% -v output=%%TST_ROOT_OUTPUT%% -v size=%x% '{print "@echo sed -e s/VPG_SZ/"page"/g -e s/STR_ST/"table"/g "ingtst"\\be\\"area"\\sep\\"$3" > "output"\\be\\"area"\\"size"K\\"table"\\"$3}' > %TST_ROOT_OUTPUT%\be\%AREA%\%x%K\%TABLE%\convert.bat
goto CONTINUE2

:QA_USE_CYGWIN
echo Use Cygwin version of this awk command
type %TST_LISTEXEC%\bevps.lis|%AWK_CMD% -F: -v page=%PAGE% -v table=%TABLE% -v area=%AREA% -v lisfile=%LIS_FILE% -v ingtst=%%ING_TST%% -v listexec=%%TST_LISTEXEC%% -v output=%%TST_ROOT_OUTPUT%% -v size=%x% '{print "@echo sed -e s/VPG_SZ/"page"/g -e s/STR_ST/"table"/g "ingtst"\\\be\\\"area"\\\sep\\\"$3" > "output"\\\be\\\"area"\\\"size"K\\\"table"\\\"$3}' > %TST_ROOT_OUTPUT%\be\%AREA%\%x%K\%TABLE%\convert.bat

:CONTINUE2
call %TST_OUTPUT%\convert
echo executor %TST_CFG%\%CFG_FILE% > %TST_OUTPUT%%OUT_FILE%
executor %TST_CFG%\%CFG_FILE% > %TST_OUTPUT%\%OUT_FILE%

erase %TST_ROOT_OUTPUT%\be\%AREA%\%x%K\%TABLE%\convert.bat  
if exist %TST_ROOT_OUTPUT%\be\%AREA%\%x%K\%TABLE%\*.stf erase %TST_ROOT_OUTPUT%\be\%AREA%\%x%K\%TABLE%\*.stf
if exist %TST_ROOT_OUTPUT%\be\%AREA%\%x%K\%TABLE%\*.syn erase %TST_ROOT_OUTPUT%\be\%AREA%\%x%K\%TABLE%\*.syn 
if exist %TST_OUTPUT%\*.sep erase %TST_OUTPUT%\*.sep
goto DOT1

:DOT1
set TST_OUTPUT=%TST_ROOT_OUTPUT%\be
if "%TS%"=="all" goto SETT
if "%TS%"=="ALL" goto SETT
set t=5
:SETT
if "%t%"=="4" set t=5
if "%t%"=="3" set t=4
if "%t%"=="2" set t=3
if "%t%"=="1" set t=2
goto LOOP2

:SUBEND
call ipset total expr %total% - %x%
if "%total%" == "0" goto ENDO
if "%x%"=="4" set x=2
if "%x%"=="8" set x=4
if "%x%"=="16" set x=8
if "%x%"=="32" set x=16
if "%x%"=="64" set x=32
goto MAIN

:ENDO
if exist %TST_OUTPUT%\%AREA%\*.sep erase %TST_OUTPUT%\%AREA%\*.sep
echo.
endlocal
