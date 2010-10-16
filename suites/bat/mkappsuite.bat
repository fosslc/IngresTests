@echo off
REM
REM Copyright (c) 2009 Ingres Corporation
REM
REM Script to build Appsuite stress test executables 
REM
REM History:
REM         15-May-2008 (sarjo01) Created. 
REM         01-Oct-2009 (sarjo01) Added dbpv1.
REM

setlocal

if "%1"=="" (
    echo Usage:
    echo     %%TST_SHELL%%\mkappsuite.bat test [ test test ... ]
    echo        where test is any of
    echo     dbpv1 ddlv1 insdel ordent qp1 qp3 selv1 updv1
    echo or
    echo     %%TST_SHELL%%\mkappsuite.bat all
    echo.
    goto :END
)

:CONTINUE

    if not "%1"=="all" if not "%1"=="ddlv1" goto :DO_DDLV1

    call %TST_SHELL%\mk1app.bat dbpv1

    if not exist ".\dbpv1.exe" (
       echo Aborting, failed to build dbpv1.exe.
       goto :END
    )

    if not "%1"=="all" (
        shift
        goto :CONTINUE
    )
    mv .\dbpv1.exe %ING_TOOLS%\bin

:DO_DDLV1

    if not "%1"=="all" if not "%1"=="ddlv1" goto :DO_INSDEL

    call %TST_SHELL%\mk1app.bat ddlv1

    if not exist ".\ddlv1.exe" (
       echo Aborting, failed to build ddlv1.exe.
       goto :END
    )

    if not "%1"=="all" (
        shift
        goto :CONTINUE
    )
    mv .\ddlv1.exe %ING_TOOLS%\bin

:DO_INSDEL

    if not "%1"=="all" if not "%1"=="insdel" goto :DO_ORDENT

    call %TST_SHELL%\mk1app.bat insdel

    if not exist ".\insdel.exe" (
       echo Aborting, failed to build insdel.exe.
       goto :END
    )

    if not "%1"=="all" (
        shift
        goto :CONTINUE
    )
    mv .\insdel.exe %ING_TOOLS%\bin

:DO_ORDENT

    if not "%1"=="all" if not "%1"=="ordent" goto :DO_QP1

    call %TST_SHELL%\mk1app.bat ordent

    if not exist ".\ordent.exe" (
       echo Aborting, failed to build ordent.exe.
       goto :END
    )

    if not "%1"=="all" (
        shift
        goto :CONTINUE
    )
    mv .\ordent.exe %ING_TOOLS%\bin

:DO_QP1

    if not "%1"=="all" if not "%1"=="qp1" goto :DO_QP3

    call %TST_SHELL%\mk1app.bat qp1

    if not exist ".\qp1.exe" (
        echo Aborting, failed to build qp1.exe.
        goto :END
    )

    if not "%1"=="all" (
        shift
        goto :CONTINUE
    )
    mv .\qp1.exe %ING_TOOLS%\bin

:DO_QP3

    if not "%1"=="all" if not "%1"=="qp3" goto :DO_SELV1

    call %TST_SHELL%\mk1app.bat qp3

    if not exist ".\qp3.exe" (
        echo Aborting, failed to build qp3.exe.
        goto :END
    )

    if not "%1"=="all" (
        shift
        goto :CONTINUE
    )
    mv .\qp3.exe %ING_TOOLS%\bin

:DO_SELV1

    if not "%1"=="all" if not "%1"=="selv1" goto :DO_UPDV1

    call %TST_SHELL%\mk1app.bat selv1

     if not exist ".\selv1.exe" (
        echo Aborting, failed to build selv1.exe.
        GOTO :END
     )

    if not "%1"=="all" (
        shift
        goto :CONTINUE
    )
    mv .\selv1.exe %ING_TOOLS%\bin

:DO_UPDV1

    if not "%1"=="all" if not "%1"=="updv1" goto :END

    call %TST_SHELL%\mk1app.bat updv1

    if not exist ".\updv1.exe" (
        echo Aborting, failed to build updv1.exe.
        GOTO :END
    )

    if not "%1"=="all" (
        shift
        goto :CONTINUE
    )
    mv .\updv1.exe %ING_TOOLS%\bin

:END
endlocal
