@echo off
REM
REM Copyright (c) 2008 Ingres Corporation
REM
REM Script to build a single Appsuite stress test executable
REM
REM History:
REM         15-May-2008 (sarjo01) Created. 
REM

setlocal

cp %ING_TST%\stress\appsuite\%1.sc . 
call %II_SYSTEM%\ingres\bin\esqlc -multi %1
call %ING_TOOLS%\bin\sepcc.bat %1
call %ING_TOOLS%\bin\seplnk.bat %1
rm ./%1.sc ./%1.c ./%1.obj

endlocal
