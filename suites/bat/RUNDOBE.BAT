@echo off
REM Copyright (c) 2004 Ingres Corporation
REM 
REM
REM	Script to run Automated SEP Tests for Ingres r3 (Windows)
REM
REM	      call from RUNBE.BAT
REM                        
REM	History:
REM	  26-Nov-1996  <karye01> created
REM       28-Jan-1997  <rusfr01> modified
REM       14-Jul-1997  <karye01> modified
REM       5-May-1998  <karye01> added message for RLL, and copy for RAAT.
REM	  03-Mar-1999  <miski01) Took out VPG and STR variables
REM	  27-Sep-2004  (legru01) Extended the check page size section to
REM				 include page sizes 32k and 64k.
REM   	  28-Oct-2004  (legru01) Added a slash to separate
REM                              %TST_OUTPUT% and %OUT_FILE% env variables
REM				 without the slash, output files were 
REM				 written to wrong location. 
REM   	  10-Jun-2008  (vande02) Changed II_CONFIG to ING_CONFIG.
REM   	  25-Jun-2008  (vande02) Changed %ING_CONFIG% to what it literally needs
REM                              to be %II_SYSTEM%\ingres\files.
REM			      
setlocal

REM  check to see if the cache for the page sizes of 2,4,8, 16
REM  32, and 64 is turned on.
   
findstr "cache\.p4k.*ON" %II_SYSTEM%\ingres\files\config.dat > tmp.out
if not errorlevel 1 goto CHKNP8
goto CACHERR

:CHKNP8
erase tmp.out
findstr "cache\.p8k.*ON" %II_SYSTEM%\ingres\files\config.dat > tmp.out
if not errorlevel 1 goto CHKNP16
goto CACHERR

:CHKNP16
erase tmp.out
findstr "cache\.p16k.*ON" %II_SYSTEM%\ingres\files\config.dat > tmp.out
if not errorlevel 1 goto CHKNP32
goto CACHERR

:CHKNP32
erase tmp.out
findstr "cache\.p32k.*ON" %II_SYSTEM%\ingres\files\config.dat > tmp.out
if not errorlevel 1 goto CHKNP64
goto CACHERR

:CHKNP64
erase tmp.out
findstr "cache\.p64k.*ON" %II_SYSTEM%\ingres\files\config.dat > tmp.out
if not errorlevel 1 goto LOOP3
goto CACHERR

:CACHERR
echo.
echo *****************************************************************************
echo *                          Error                                            *
echo * Make sure your DBMS Server is set to support 2,4,8,16,32 & 64k page sizes.*
echo * One of the page sizes cache was not turned ON.                            *
echo * Check CBF and make sure that cache is ON for each of these page sizes.    *
echo * Ending Program...                                                         *
echo *****************************************************************************
echo.
erase tmp.out
goto ENDIT


:LOOP3
erase tmp.out
rem  ============ do main work here =============

set TST_OUTPUT=%TST_OUTPUT%\%AREA%
if exist %TST_OUTPUT% goto CONTINU
echo.
echo Will not process %AREA% tests because the output directory does not exist.  
echo %TST_OUTPUT%
echo.
goto DOT1

:CONTINU 
echo.
echo Running the %AREA% Tests .........
echo.
echo.

echo executor %TST_CFG%\%CFG_FILE% > %TST_OUTPUT%\%OUT_FILE%
executor %TST_CFG%\%CFG_FILE% > %TST_OUTPUT%\%OUT_FILE%

if exist %TST_OUTPUT%\%AREA%\*.stf erase %TST_OUTPUT%\%AREA%\*.stf
if exist %TST_OUTPUT%\%AREA%\*.syn erase %TST_OUTPUT%\%AREA%\*.syn 
goto DOT1

:DOT1
if exist %TST_OUTPUT%\%AREA%\*.sep erase %TST_OUTPUT%\%AREA%\*.sep

:ENDIT
echo.
endlocal
