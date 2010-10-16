@echo off
REM		Script to initialize BE SEP Tests for OpenIngres 2.0 (Windows NT).
REM		
REM
REM	used by: RUNBE.BAT , RUNCTS.BAT , RUNFE.BAT
REM
REM	History:
REM	26-Nov-1996  <karye01> created
REM     13-Jun-1997  <karye01> modified

setlocal
set total=%PS%
set x=64
rem goto MAIN

:MAIN
call test %total% -lt %x%
if errorlevel 1 goto SUB
if "%total%" == "0" goto END
if "%x%"=="4"   set x=2
if "%x%"=="8"   set x=4
if "%x%"=="16"  set x=8
if "%x%"=="32"  set x=16
if "%x%"=="64"  set x=32
goto MAIN


:SUB
if "%x%"=="2" set PAGE=2048
if "%x%"=="2" if exist %TST_OUTPUT%\%AREA%\%x%K  PCecho " .. Directory - %TST_OUTPUT%\%AREA%\%x%K already exist"
if "%x%"=="2" if not exist %TST_OUTPUT%\%AREA%\%x%K  PCecho "Creating Directory - %TST_OUTPUT%\%AREA%\%x%K"
if "%x%"=="2" if not exist %TST_OUTPUT%\%AREA%\%x%K mkdir %TST_OUTPUT%\%AREA%\%x%K
if "%x%"=="2" goto LOOP
rem  ===========================================================================
if "%x%"=="4" set PAGE=4096
if "%x%"=="4" if not exist %TST_OUTPUT%\%AREA%\%x%K  PCecho "Creating Directory - %TST_OUTPUT%\%AREA%\%x%K"
if "%x%"=="4" if exist %TST_OUTPUT%\%AREA%\%x%K  PCecho " .. Directory - %TST_OUTPUT%\%AREA%\%x%K already exist"
if "%x%"=="4" if not exist %TST_OUTPUT%\%AREA%\%x%K mkdir %TST_OUTPUT%\%AREA%\%x%K
if "%x%"=="4" goto LOOP
rem ============================================================================
if "%x%"=="8" set PAGE=8192
if "%x%"=="8" if exist %TST_OUTPUT%\%AREA%\%x%K  PCecho " .. Directory - %TST_OUTPUT%\%AREA%\%x%K already exist"
if "%x%"=="8" if not exist %TST_OUTPUT%\%AREA%\%x%K  PCecho "Creating Directory - %TST_OUTPUT%\%AREA%\%x%"K
if "%x%"=="8" if not exist %TST_OUTPUT%\%AREA%\%x%K mkdir %TST_OUTPUT%\%AREA%\%x%K
if "%x%"=="8" goto LOOP
rem ============================================================================
if "%x%"=="16" set PAGE=16384
if "%x%"=="16" if exist %TST_OUTPUT%\%AREA%\%x%K  PCecho " .. Directory - %TST_OUTPUT%\%AREA%\%x%K already exist"
if "%x%"=="16" if not exist %TST_OUTPUT%\%AREA%\%x%K  PCecho "Creating Directory - %TST_OUTPUT%\%AREA%\%x%K"
if "%x%"=="16" if not exist %TST_OUTPUT%\%AREA%\%x%K mkdir %TST_OUTPUT%\%AREA%\%x%K
if "%x%"=="16" goto LOOP
rem ============================================================================
if "%x%"=="32" set PAGE=32768
if "%x%"=="32" if exist %TST_OUTPUT%\%AREA%\%x%K  PCecho " .. Directory - %TST_OUTPUT%\%AREA%\%x%K already exist"
if "%x%"=="32" if not exist %TST_OUTPUT%\%AREA%\%x%K  PCecho "Creating Directory - %TST_OUTPUT%\%AREA%\%x%K"
if "%x%"=="32" if not exist %TST_OUTPUT%\%AREA%\%x%K mkdir %TST_OUTPUT%\%AREA%\%x%K
if "%x%"=="32" goto LOOP
rem ============================================================================
if "%x%"=="64" set PAGE=65536
if "%x%"=="64" if exist %TST_OUTPUT%\%AREA%\%x%K  PCecho " .. Directory - %TST_OUTPUT%\%AREA%\%x%K already exist"
if "%x%"=="64" if not exist %TST_OUTPUT%\%AREA%\%x%K  PCecho "Creating Directory - %TST_OUTPUT%\%AREA%\%x%K"
if "%x%"=="64" if not exist %TST_OUTPUT%\%AREA%\%x%K mkdir %TST_OUTPUT%\%AREA%\%x%K
if "%x%"=="64" goto LOOP
REM goto END

:LOOP 
if "%TS%"=="heap" set TABLE=heap
if "%TS%"=="HEAP" set TABLE=heap
if "%TS%"=="hash" set TABLE=hash
if "%TS%"=="HASH" set TABLE=hash
if "%TS%"=="btree" set TABLE=btree
if "%TS%"=="BTREE" set TABLE=btree
if "%TS%"=="isam" set TABLE=isam
if "%TS%"=="ISAM" set TABLE=isam
if "%TS%"=="all" set TABLE=heap
if "%TS%"=="ALL" set TABLE=heap

if "%TABLE%"=="heap" if not exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% PCecho  "Creating Directory - %TST_OUTPUT%\%AREA%\%x%K\%TABLE%"
if "%TABLE%"=="heap" if exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% PCecho  " ..... Directory - %TST_OUTPUT%\%AREA%\%x%K\%TABLE%  already exist"
if "%TABLE%"=="heap" if not exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% mkdir   %TST_OUTPUT%\%AREA%\%x%K\%TABLE% 
if "%TS%"=="all" set TABLE=hash
if "%TS%"=="ALL" set TABLE=hash
if "%TABLE%"=="hash" if not exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% PCecho  "Creating Directory - %TST_OUTPUT%\%AREA%\%x%K\%TABLE%"
if "%TABLE%"=="hash" if exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% PCecho  " ..... Directory - %TST_OUTPUT%\%AREA%\%x%K\%TABLE%  already exist"
if "%TABLE%"=="hash" if not exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% mkdir   %TST_OUTPUT%\%AREA%\%x%K\%TABLE% 
if "%TS%"=="all" set TABLE=btree
if "%TS%"=="ALL" set TABLE=btree
if "%TABLE%"=="btree" if not exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% PCecho  "Creating Directory - %TST_OUTPUT%\%AREA%\%x%K\%TABLE%"
if "%TABLE%"=="btree" if exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% PCecho  " ..... Directory - %TST_OUTPUT%\%AREA%\%x%K\%TABLE%  already exist"
if "%TABLE%"=="btree" if not exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% mkdir   %TST_OUTPUT%\%AREA%\%x%K\%TABLE% 
if "%TS%"=="all" set TABLE=isam
if "%TS%"=="ALL" set TABLE=isam
if "%TABLE%"=="isam" if not exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% PCecho  "Creating Directory - %TST_OUTPUT%\%AREA%\%x%K\%TABLE%"
if "%TABLE%"=="isam" if exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% PCecho  " ..... Directory - %TST_OUTPUT%\%AREA%\%x%K\%TABLE%  already exist"
if "%TABLE%"=="isam" if not exist %TST_OUTPUT%\%AREA%\%x%K\%TABLE% mkdir   %TST_OUTPUT%\%AREA%\%x%K\%TABLE% 

call ipset total expr %total% - %x%
if "%total%"=="0" goto END
if "%x%"=="4"   set x=2
if "%x%"=="8"   set x=4
if "%x%"=="16"  set x=8
if "%x%"=="32"  set x=16
if "%x%"=="64"  set x=32
goto MAIN

:END
echo.
echo Output directories created and ready for Testing.
endlocal
