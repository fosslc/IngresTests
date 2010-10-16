@echo off
setlocal
REM
REM Description:
REM	This script is used in the RUN*.BAT files to create the databases and
REM run SEP tests through listexec and excutor. 
REM
REM  History:
REM 27-May-1996 (clate01)
REM		Created. 
REM 24-May-2001	(rogch01)
REM	Add ODBC


if "%2"=="gaa" goto GAA
if "%2"=="gba" goto GBA
if "%2"=="gca" goto GCA
if "%2"=="odbc" goto ODBC

echo Parameter ERROR in the call dbcreate function
goto END


:GAA
set TST_DATA=%ING_TST%\gcf\gcc\data
set SEPPARAM_NODE=lback%val%::gaadb%val%
if "%1"=="init" destroydb gaadb%val% >>%TST_OUTPUT%\netinit%val%.out
if "%1"=="init" createdb gaadb%val% >>%TST_OUTPUT%\netinit%val%.out
if "%1"=="init" goto END
executor %TST_CFG%\gaaunix.cfg > %TST_OUTPUT%\gaa%val%.out 
goto END                   

:GBA  
set TST_DATA=%ING_TST%\gcf\gcc\data
set SEPPARAM_NODE=lback%val%::gbadb%val%
if "%1"=="init" destroydb gbadb%val% >>%TST_OUTPUT%\netinit%val%.out
if "%1"=="init" createdb gbadb%val% >>%TST_OUTPUT%\netinit%val%.out
if "%1"=="init" goto END
executor %TST_CFG%\gbaunix.cfg > %TST_OUTPUT%\gba%val%.out
goto END

:GCA
set TST_DATA=%ING_TST%\gcf\gcc\data
set SEPPARAM_NODE=lback%val%::gcadb%val%
if "%1"=="init" destroydb gcadb%val% >>%TST_OUTPUT%\netinit%val%.out
if "%1"=="init" createdb gcadb%val% >>%TST_OUTPUT%\netinit%val%.out
if "%1"=="init" goto END
executor %TST_CFG%\gcaunix.cfg > %TST_OUTPUT%\gca%val%.out
goto END

:ODBC
if "%1"=="init" destroydb -utestenv %SEPPARAM_ODB% >>%TST_OUTPUT%\odbcinit.out
if "%1"=="init" createdb -utestenv %SEPPARAM_ODB% >>%TST_OUTPUT%\odbcinit.out
if "%1"=="init" goto END
executor %TST_CFG%\odbc.cfg > %TST_OUTPUT%\odbc.out
goto END

:END
endlocal

