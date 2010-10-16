@echo off
REM
REM Copyright (c) 2007 Ingres Corporation
REM
REM Usage:  Appsuite Stress Test Execution Script
REM         Controlling script to build/run all or a selection of any stress
REM         test with some default parameters passed to each stress test by
REM         this script.
REM
REM History:
REM    05-Dec-2007 (vande02) Created. 
REM    02-Jun-2006 (sarjo01) Modified to use mkappsuite.bat; added data
REM                          file path option to qp1, qp3; lowered
REM                          iteration counts to reduce overall execution time
REM    01-Oct-2009 (sarjo01) Changed ordent iso level to default (serializable);
REM                          Added new test dbpv1.

REM

setlocal

set flag=%1

if "%1"=="" (
    echo Usage: 
    echo        runappsuite.bat test [ test test ... ]
    echo where test is one or more of:
    echo        dbpv1 ddlv1 insdel ordent qp1 qp3 selv1 updv1
    echo.
    echo or
    echo        runappsuite.bat all
    echo.
    goto :END
)

echo.

echo %DATE% %TIME%: Begin Appsuite Tests 
echo.
if "%TST_OUTPUT%" == "" (
    echo ERROR: TST_OUTPUT is not set.
    goto :END
)


echo %DATE% %TIME%: Creating output directory %TST_OUTPUT%\appsuite 
echo.
if not exist "%TST_OUTPUT%\appsuite" (
    mkdir %TST_OUTPUT%\appsuite
)
set OUTPUTDIR=%TST_OUTPUT%\appsuite
cd  %OUTPUTDIR%


if not exist "%SEPPARAMDB%" (
    set SEPPARAMDB=appsuitedb
)
echo %DATE% %TIME%: Creating database %SEPPARAMDB%
echo.
destroydb %SEPPARAMDB% > .\appsuite.out
createdb %SEPPARAMDB% >> .\appsuite.out


echo %DATE% %TIME%: Building executables
echo.
call %TST_SHELL%\mkappsuite.bat all >> .\appsuite.out


:CONTINUE

if not "%1"=="all" if not "%1"=="dbpv1" goto :RUN_DDLV1
echo %DATE% %TIME%: Running test DBPV1
echo.

dbpv1.exe %SEPPARAMDB% init -n10000 -p1 > .\dbpv1.out
dbpv1.exe %SEPPARAMDB% run -t16 -v0 -i10000 -x100 >> .\dbpv1.out 
dbpv1.exe %SEPPARAMDB% cleanup >> .\dbpv1.out 

if "%1"=="all" goto :RUN_DDLV1
shift
goto :CONTINUE

:RUN_DDLV1

if not "%1"=="all" if not "%1"=="ddlv1" goto :RUN_INSDEL
echo %DATE% %TIME%: Running test DDLV1
echo.

ddlv1.exe %SEPPARAMDB% init > .\ddlv1.out
ddlv1.exe %SEPPARAMDB% run -t16 -v0 -i5000 -s250 >> .\ddlv1.out 
ddlv1.exe %SEPPARAMDB% cleanup >> .\ddlv1.out 

if "%1"=="all" goto :RUN_INSDEL
shift
goto :CONTINUE


:RUN_INSDEL

if not "%1"=="all" if not "%1"=="insdel" goto :RUN_ORDENT
echo %DATE% %TIME%: Running test INSDEL
echo.

insdel.exe %SEPPARAMDB% init -p32 > .\insdel.out
insdel.exe %SEPPARAMDB% run -t24 -v0 -i50000 -d10 -b5 -h50000 >> .\insdel.out 
insdel.exe %SEPPARAMDB% cleanup >> .\insdel.out 

if "%1"=="all" goto :RUN_ORDENT
shift
goto :CONTINUE


:RUN_ORDENT

if not "%1"=="all" if not "%1"=="ordent" goto :RUN_QP1
echo %DATE% %TIME%: Running test ORDENT
echo.

ordent.exe %SEPPARAMDB% init -d -p32 > .\ordent.out
ordent.exe %SEPPARAMDB% run -t24 -v0 -i50000 -w1 >> .\ordent.out 
ordent.exe %SEPPARAMDB% cleanup >> .\ordent.out 

if "%1"=="all" goto :RUN_QP1
shift
GOTO :CONTINUE


:RUN_QP1

if not "%1"=="all" if not "%1"=="qp1" goto :RUN_QP3
echo %DATE% %TIME%: Running test QP1
echo.

qp1.exe %SEPPARAMDB% init -d%ING_TST%\stress\appsuite\ > .\qp1.out
optimizedb -zk %SEPPARAMDB% >> .\qp1.out
qp1.exe %SEPPARAMDB% run -t10 -v0 -i250 -p >> .\qp1.out
qp1.exe %SEPPARAMDB% cleanup >> .\qp1.out

if "%1"=="all" goto :RUN_QP3
shift
goto :CONTINUE


:RUN_QP3

if not "%1"=="all" if not "%1"=="qp3" goto :RUN_SELV1
echo %DATE% %TIME%: Running test QP3
echo.

qp3.exe %SEPPARAMDB% init -d%ING_TST%\stress\appsuite\ > .\qp3.out
optimizedb -zk %SEPPARAMDB% >> .\qp3.out
qp3.exe %SEPPARAMDB% run -t4 -v0 -i15 -p >> .\qp3.out
qp3.exe %SEPPARAMDB% cleanup >> .\qp3.out

if "%1"=="all" goto :RUN_SELV1
shift
goto :CONTINUE


:RUN_SELV1

if not "%1"=="all" if not "%1"=="selv1" goto :RUN_UPDV1
echo %DATE% %TIME%: Running test SELV1
echo.

selv1.exe %SEPPARAMDB% init -r50000 -p32 -o500 > .\selv1.out
selv1.exe %SEPPARAMDB% run -t24 -v0 -i100000 -z >> .\selv1.out
selv1.exe %SEPPARAMDB% cleanup >> .\selv1.out

if "%1"=="all" goto :RUN_UPDV1
shift
goto :CONTINUE


:RUN_UPDV1

if not "%1"=="all" if not "%1"=="updv1" GOTO :CONTINUE2
echo %DATE% %TIME%: Running test UPDV1
echo.

updv1.exe %SEPPARAMDB% init -p10 -c > .\updv1.out
updv1.exe %SEPPARAMDB% run -t24 -v0 -i20000 -b5 >> .\updv1.out
updv1.exe %SEPPARAMDB% cleanup >> .\updv1.out

:CONTINUE2
echo %DATE% %TIME%: Appsuite Tests completed 
echo.
:END
endlocal
