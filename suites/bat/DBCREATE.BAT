@echo off
setlocal
REM Copyright (c) 2009 Ingres Corporation
REM
REM
setlocal
REM
REM Description:
REM	This script is used in the RUN*.BAT files to create the databases and
REM run SEP tests through listexec and excutor. 
REM
REM  History:
REM 	??-Sep-1995 (belar01)
REM		Created.
REM	04-Jan-1996 (somsa01)
REM		Cleaned up to resemble more closely the run*.sh files.
REM	30-Jan-1996 (somsa01)
REM		Added raat tests.
REM	13-Feb-1996 (somsa01)
REM		Added embedded tests (c and cobol).
REM	16-Jul-1996 (somsa01)
REM		Cleaned up.
REM	30-Oct-1996 (karye01)
REM		Added hetnet tests (HETNETA,HETNETB,HETNETC)
REM		(calls from runhetnet.bat)
REM     19-Mar-1997 (vissu01)
REM		Added SEPPARAMDB2 for lar tests
REM	26-Mar-1997 (vissu01)
REM		Added FASTLOAD tests	
REM	26-Mar-1997 (chiel02)
REM             Changed the IMAGE output directory path.  
REM     17-Sep-1997 (wanya01)
REM             Add api tests
REM	29-Oct-1998 (walro03)
REM		Updated with replicator tests.
REM	22-feb-1999 (somsa01)
REM		For STARDBPROC tests, set SEPPARAMDB1 as well.
REM	11-may-1999 (somsa01)
REM		Corrected running of Replicator Datatypes tests.
REM	02-Aug-2000
REM		Relabel hetnet entries because they were ambiguous on w95.
REM	26-Jan-2001 (xu$we01)
REM		Change "set SEPPARAMDB=imagedb1" to "set SEPPARAMDB=imagedb"
REM	12-Apr-2001 (rogch01)
REM		Add FORTRAN tests.
REM     19-Jun-2001 (wu$ca01)
REM             Added SEPPARAMDB=beudtdb in the UDT area. Added createdb
REM             utildb3 and utildb4 in UTIL area.		
REM     30-Oct-2001 (wu$ca01)
REM             Added createdb -n unidb in the DATATYPES area for testing the
REM             new unicode datatypes.
REM     08-Sep-2003 (vande02)
REM             Changed initialization database name for RW from basisdb to 
REM             rwdb so fe/rw will be run just as it is on other platforms.
REM		This is consistent with the clean section of this script which
REM		destroys rwdb not basisdb.
REM	05-dec-2003 (somsa01)
REM		dbextention should be dbext.
REM     14-Jul-2004 (madsu02)
REM             Defined SEPPARAMDB1=ldb1 in starddl area.
REM     23-Sep-2004	(legru01) Remove UDT, RAAT, CTS, HETNETA, 
REM		HETNETB, HETNETC, FORMUTIL, EMBEDMT, EMBEDCOB, EMBEDFOR,
REM		REPREGR, CASCADE, DBTYPES, ERRORS, HUBSPOKE, MAINTAIN,
REM		PEER, REPDATA, STORAGE since these will not be provided
REM		to Open Source. Removed references to 3GL MT, COB, and FOR test
REM		components.
REM     28-Oct-2004 (legru01) removed line that reference an UNIX 
REM             style variables.
REM            "sep -b -v $TST_INIT/initlar.sep -o$TST_OUTPUT/lar/initlar.out"
REM     03-Jul-2007 (vande02)
REM             Added SEP_DIFF_SLEEP and SEP_CMD_SLEEP syntax to be set to
REM             default of 250 so the SEP commands can complete before the
REM             next one is executed.
REM	11-Dec-2008 (wanfr01)
REM		SIR 121355
REM		Allow SEPPARAM_SLEEP and SEPPARAM_SLEEP_2 to be set by shell
REM		and change default to 5 seconds
REM	31-Jul-2009 (vande02)
REM		Removed the % sign around 'lback' and name the vnode for the
REM		Loopback tests specifically which is 'lback'.  It doesn't need
REM		to be a variable.



if "%1"=="starldbs" goto STARLDBS
if "%1"=="starddb" goto STARDDB
if "%1"=="starqprc" goto STARQPRC
if "%1"=="starddl" goto STARDDL
if "%1"=="startpf" goto STARTPF
if "%1"=="staraggs" goto STARAGGS
if "%1"=="starpred" goto STARPRED
if "%1"=="starsubs" goto STARSUBS
if "%1"=="stardbproc" goto STARDBPROC
if "%2"=="access" goto ACCESS
if "%2"=="accntl" goto ACCNTL
if "%2"=="datatypes" goto DATATYPES
if "%2"=="lar" goto LAR
if "%2"=="util" goto UTIL
if "%2"=="api" goto API
if "%2"=="qryproc" goto QRYPROC
if "%2"=="c2secure" goto C2SECURE
if "%2"=="4gl" goto 4GL
if "%2"=="copy" goto COPY
if "%2"=="abf" goto ABF
if "%2"=="image" goto IMAGE  
if "%2"=="frs" goto FRS  
if "%2"=="fstm" goto FSTM 
if "%2"=="qbf" goto QBF  
if "%2"=="rbf" goto RBF 
if "%2"=="rw" goto RW 
if "%2"=="vifred" goto VIFRED
if "%2"=="vision" goto VISION
if "%2"=="genutil" goto GENUTIL
if "%2"=="c" goto EMBEDC
if "%2"=="gaa" goto GAA
if "%2"=="gba" goto GBA
if "%2"=="gca" goto GCA
if "%2"=="repregr" goto REPREGR
if "%2"=="replicator" goto REPLICATOR
if "%2"=="backup" goto REPLICATOR
if "%2"=="benign" goto REPLICATOR
if "%2"=="branch" goto REPLICATOR
if "%2"=="repmgr" goto REPLICATOR
 
echo Parameter ERROR in the call dbcreate function
goto END

:ACCESS
if "%1"=="init" destroydb accessdb1 >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb accessdb1 >>%TST_OUTPUT%\beinit.out
if "%1"=="init" goto END
set SEPPARAMDB=accessdb1
set SEPPARAMDRIVERDB=-daccessdb1
executor %TST_CFG%\beam.cfg >%TST_OUTPUT%\access\beam.out
goto END

:ACCNTL
if "%1"=="init" destroydb accntldb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb accntldb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" goto END
set SEPPARAMDB=accntldb
set SEPPARAMDB3=grantdb
set SEPPARAMDRIVERDB=-dgrantdb
executor %TST_CFG%\beacc.cfg >%TST_OUTPUT%\accntl\beacc.out
goto END

:DATATYPES  
if "%1"=="init" destroydb datatypedb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb datatypedb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" destroydb datatypedb2 >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb datatypedb2 >>%TST_OUTPUT%\beinit.out
if "%1"=="init" destroydb unidb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb -n unidb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" goto END
set SEPPARAMDB1=datatypedb
set SEPPARAMDB2=datatypedb2
set SEPPARAMDB3=unidb
executor %TST_CFG%\bedt.cfg >%TST_OUTPUT%\datatypes\bedt.out
set SEPPARAMDB=datatypedb
executor %TST_CFG%\bedt2.cfg >%TST_OUTPUT%\datatypes\bedt2.out
goto END

:LAR  
if "%1"=="init" destroydb testdb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb testdb >>%TST_OUTPUT%\beinit.out
set SEPPARAMDB=lardb
if "%1"=="init" goto END
set SEPPARAMDB=lardb
set SEPPARAMDB2=testdb
executor %TST_CFG%\belar.cfg >%TST_OUTPUT%\lar\belar.out
goto END

:QRYPROC 
if "%1"=="init" destroydb qryprocdb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb qryprocdb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" goto END
set SEPPARAMDB=qryprocdb
set SEPPARAMDRIVERDB=-dqryprocdb
executor -t10 %TST_CFG%\beqp.cfg >%TST_OUTPUT%\qryproc\beqp.out
goto END

:API
if "%1"=="init" destroydb apidb1   >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb apidb1   >>%TST_OUTPUT%\beinit.out
if "%1"=="init" destroydb apidb2   >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb apidb2   >>%TST_OUTPUT%\beinit.out
if "%1"=="init" goto END
executor %TST_CFG%\beapi.cfg >%TST_OUTPUT%\api\beapi.out
goto END

:UTIL     
if "%1"=="init" destroydb utildb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb utildb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" destroydb utildb3 >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb utildb3 >>%TST_OUTPUT%\beinit.out
if "%1"=="init" destroydb utildb4 >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb utildb4 >>%TST_OUTPUT%\beinit.out
if "%1"=="init" goto END
set SEPPARAMDB=utildb
set SEPPARAMDB3=utildb3
set SEPPARAMDB4=utildb4
executor %TST_CFG%\beutil.cfg >%TST_OUTPUT%\util\beutil.out
goto END

:FASTLOAD
if "%1"=="init" destroydb flddb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb flddb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" goto END
set SEPPARAMDB=flddb
executor %TST_CFG%\c2_aud.cfg >%TST_OUTPUT%\fld\fld.out
goto END

:C2SECURE
if "%1"=="init" destroydb securedb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" createdb securedb >>%TST_OUTPUT%\beinit.out
if "%1"=="init" goto END
set SEPPARAMDB=securedb
executor %TST_CFG%\c2_aud.cfg >%TST_OUTPUT%\c2secure\c2secure.out
goto END

:4GL
if "%1"=="init" destroydb abf4gldb >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb abf4gldb >>%TST_OUTPUT%\feinit.out
set SEPPARAMDB=abf4gldb
if "%1"=="init" sep -b %TST_INIT%\init4gl.sep -o%TST_OUTPUT%\4gl
if "%1"=="init" goto END
executor -t10 %TST_CFG%\fe4gl.cfg >%TST_OUTPUT%\4gl\fe4gl.out
goto END

:ABF
if "%1"=="init" destroydb abfdb1 >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb abfdb1 >>%TST_OUTPUT%\feinit.out
if "%1"=="init" destroydb abfdb2 >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb abfdb2 >>%TST_OUTPUT%\feinit.out
set SEPPARAMDB=abfdb1
set SEPPARAMDB1=abfdb2
if "%1"=="init" sep -b %TST_INIT%\initabf1.sep -o%TST_OUTPUT%\abf
if "%1"=="init" sep -b %TST_INIT%\initabf2.sep -o%TST_OUTPUT%\abf
if "%1"=="init" goto END
executor %TST_CFG%\feabf.cfg >%TST_OUTPUT%\abf\feabf.out
goto END

:IMAGE
if "%1"=="init" destroydb imagedb >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb imagedb >>%TST_OUTPUT%\feinit.out
if "%1"=="init" destroydb imagedb2 >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb imagedb2 >>%TST_OUTPUT%\feinit.out
set SEPPARAMDB=imagedb
set SEPPARAMDB1=imagedb2
if "%1"=="init" goto END
executor -t24 -d100 %TST_CFG%\feimage.cfg >%TST_OUTPUT%\image\feimage.out
goto END

:COPY
if "%1"=="init" destroydb copydb1 >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb copydb1 >>%TST_OUTPUT%\feinit.out
set SEPPARAMDB=copydb1
if "%1"=="init" sep -b %TST_INIT%\initcopy.sep -o%TST_OUTPUT%\copy
if "%1"=="init" goto END
executor %TST_CFG%\fecopy.cfg >%TST_OUTPUT%\copy\fecopy.out
goto END

:FRS
if "%1"=="init" destroydb frsdb >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb frsdb >>%TST_OUTPUT%\feinit.out
if "%1"=="init" goto END
set SEPPARAMDB=frsdb
executor %TST_CFG%\fefrs.cfg >%TST_OUTPUT%\frs\fefrs.out
goto END

:FSTM
if "%1"=="init" destroydb basisdb >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb basisdb >>%TST_OUTPUT%\feinit.out
set SEPPARAMDB=basisdb
if "%1"=="init" sep -b %TST_INIT%\initfstm.sep -o%TST_OUTPUT%\fstm
if "%1"=="init" goto END
executor %TST_CFG%\fefstm.cfg >%TST_OUTPUT%\fstm\fefstm.out
goto END

:QBF
if "%1"=="init" destroydb qbf1 >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb qbf1 >>%TST_OUTPUT%\feinit.out
set SEPPARAMDB=qbf1
if "%1"=="init" sep -b %TST_INIT%\initqbf1.sep -o%TST_OUTPUT%\qbf 
if "%1"=="init" sep -b %TST_INIT%\initqbf2.sep -o%TST_OUTPUT%\qbf 
if "%1"=="init" sep -b %TST_INIT%\initqbf3.sep -o%TST_OUTPUT%\qbf 
if "%1"=="init" sep -b %TST_INIT%\initqbf4.sep -o%TST_OUTPUT%\qbf 
if "%1"=="init" sep -b %TST_INIT%\initqbf5.sep -o%TST_OUTPUT%\qbf 
if "%1"=="init" sep -b %TST_INIT%\initqbf6.sep -o%TST_OUTPUT%\qbf 
if "%1"=="init" sep -b %TST_INIT%\initqbf7.sep -o%TST_OUTPUT%\qbf 
if "%1"=="init" sep -b %TST_INIT%\initqbf8.sep -o%TST_OUTPUT%\qbf 
if "%1"=="init" sep -b %TST_INIT%\initqbf9.sep -o%TST_OUTPUT%\qbf 
if "%1"=="init" goto END
executor %TST_CFG%\feqbf.cfg >%TST_OUTPUT%\qbf\feqbf.out
goto END

:RBF 
if "%1"=="init" destroydb rbfdb1 >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb rbfdb1  >>%TST_OUTPUT%\feinit.out
set SEPPARAMDB=rbfdb1 
if "%1"=="init" sep -b %TST_INIT%\initrbf.sep -o%TST_OUTPUT%\rbf 
if "%1"=="init" goto END
executor %TST_CFG%\ferbf.cfg >%TST_OUTPUT%\rbf\ferbf.out
goto END

:RW  
if "%1"=="init" createdb rwdb >>%TST_OUTPUT%\feinit.out
set SEPPARAMDB=rwdb
if "%1"=="init" sep -b %TST_INIT%\initrw.sep -o%TST_OUTPUT%\rw
if "%1"=="init" goto END
executor %TST_CFG%\ferw.cfg >%TST_OUTPUT%\rw\ferw.out
goto END

:VIFRED
if "%1"=="init" destroydb vifred1 >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb vifred1 >>%TST_OUTPUT%\feinit.out
set SEPPARAMDB=vifred1
if "%1"=="init" sep -b %TST_INIT%\initvif.sep -o%TST_OUTPUT%\vifred
if "%1"=="init" goto END
executor %TST_CFG%\fevifred.cfg >%TST_OUTPUT%\vifred\fevifred.out
goto END

:VISION
if "%1"=="init" destroydb gourmet >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb gourmet >>%TST_OUTPUT%\feinit.out
set SEPPARAMDB=gourmet
if "%1"=="init" sep -b %TST_INIT%\initvis.sep -o%TST_OUTPUT%\vision
if "%1"=="init" goto END
executor %TST_CFG%\fevis.cfg >%TST_OUTPUT%\vision\fevis.out
goto END

:GENUTIL
if "%1"=="init" destroydb genutildb >>%TST_OUTPUT%\feinit.out
if "%1"=="init" createdb genutildb >>%TST_OUTPUT%\feinit.out
if "%1"=="init" goto END
set SEPPARAMDB=genutildb
executor %TST_CFG%\fegenutil.cfg >%TST_OUTPUT%\genutil\fegenutil.out
goto END

:EMBEDC
set SEPPARAMDB=personnel
set SEPPARAMDB2=gourmet
executor %TST_CFG%\fe3glc.cfg >%TST_OUTPUT%\c\fe3glc.out
goto END

:GAA
set TST_DATA=%ING_TST%\gcf\gcc\data
set SEPPARAM_NODE=lback::gaadb
if "%1"=="init" destroydb gaadb >>%TST_OUTPUT%\netinit.out
if "%1"=="init" createdb gaadb >>%TST_OUTPUT%\netinit.out
if "%1"=="init" goto END
executor %TST_CFG%\gaaunix.cfg > %TST_OUTPUT%\gaa.out 
goto END 

:GBA  
set TST_DATA=%ING_TST%\gcf\gcc\data
set SEPPARAM_NODE=lback::gbadb
if "%1"=="init" destroydb gbadb >>%TST_OUTPUT%\netinit.out
if "%1"=="init" createdb gbadb >>%TST_OUTPUT%\netinit.out
if "%1"=="init" goto END
executor %TST_CFG%\gbaunix.cfg > %TST_OUTPUT%\gba.out
goto END

:GCA
set TST_DATA=%ING_TST%\gcf\gcc\data
set SEPPARAM_NODE=lback::gcadb
if "%1"=="init" destroydb gcadb >>%TST_OUTPUT%\netinit.out
if "%1"=="init" createdb gcadb >>%TST_OUTPUT%\netinit.out
if "%1"=="init" goto END
executor %TST_CFG%\gcaunix.cfg > %TST_OUTPUT%\gca.out
goto END

:STARLDBS
set SEPPARAMDB=ldb%2
if not "%2"=="1" if not "%2"=="2" executor %TST_CFG%\sinitldb.cfg >>%TST_OUTPUT%\ldb.out
if not "%2"=="1" if not "%2"=="2" goto END
executor %TST_CFG%\sinitldb.cfg >>%TST_OUTPUT%\ldb.out
executor %TST_CFG%\sinitlrg.cfg >>%TST_OUTPUT%\ldb.out
goto END

:STARDDB
set SEPPARAMDB=starddb1/star
set SEPPARAMDB_D=starddb1
executor %TST_CFG%\sinitddb.cfg >>%TST_OUTPUT%\ddb.out
goto END

:STARQPRC
set SEPPARAMDB=starddb1/star
executor %TST_CFG%\starqprc.cfg >>%TST_OUTPUT%\qryproc\sqryprc.out
goto END

:STARDDL
set SEPPARAMDB=starddb1/star
set SEPPARAMDB1=ldb1
executor %TST_CFG%\starddl.cfg >>%TST_OUTPUT%\ddl\sddl.out
goto END

:STARTPF
set SEPPARAMDB=starddb1/star
PCecho "Reinitialize the STAR db before running tpf test."
sep -b %TST_INIT%\xda09.sep >>%TST_OUTPUT%\tpf\xda09.out2
sep -b %TST_INIT%\xda10.sep >>%TST_OUTPUT%\tpf\xda10.out2
call ipset datevar PCdate
PCecho "Starting the STAR tpf tests @ %datevar%"
echo.
executor %TST_CFG%\startpf.cfg >>%TST_OUTPUT%\tpf\startpf.out
goto END

:STARAGGS
set SEPPARAMDB=starddb1/star
executor %TST_CFG%\saggs.cfg >>%TST_OUTPUT%\aggs\saggs.out
goto END

:STARPRED
set SEPPARAMDB=starddb1/star
executor %TST_CFG%\spred.cfg >>%TST_OUTPUT%\pred\spred.out
goto END

:STARSUBS
set SEPPARAMDB=starddb1/star
executor %TST_CFG%\ssubsel.cfg >>%TST_OUTPUT%\subsel\ssubsel.out
goto END

:STARDBPROC
set SEPPARAMDB=starddb1/star
set SEPPARAMDB1=ldb1
set SEPPARAMDRIVERDB=-dstarddb1/star
executor %TST_CFG%\sdbproc.cfg >>%TST_OUTPUT%\dbproc\sdbproc.out
goto END

:REPLICATOR
if "%1"=="init" destroydb repdb1 >>%TST_OUTPUT%\repinit.out
if "%1"=="init" createdb repdb1 >>%TST_OUTPUT%\repinit.out
if "%1"=="init" destroydb repdb2 >>%TST_OUTPUT%\repinit.out
if "%1"=="init" createdb repdb2 >>%TST_OUTPUT%\repinit.out
if "%1"=="init" destroydb repdb3 >>%TST_OUTPUT%\repinit.out
if "%1"=="init" createdb repdb3 >>%TST_OUTPUT%\repinit.out
if "%1"=="init" destroydb repdb4 >>%TST_OUTPUT%\repinit.out
if "%1"=="init" createdb repdb4 >>%TST_OUTPUT%\repinit.out
if "%1"=="init" destroydb repdb5 >>%TST_OUTPUT%\repinit.out
if "%1"=="init" createdb repdb5 >>%TST_OUTPUT%\repinit.out
if "%1"=="init" goto END
set SEPPARAMDB_1=repdb1
set SEPPARAMDB_2=repdb2
set SEPPARAMDB_3=repdb3
set SEPPARAMDB_4=repdb4
set SEPPARAMDB_5=repdb5
set SEPPARAMDRIVERDB_1=repdb1
set SEPPARAMDRIVERDB_2=repdb2
set SEPPARAMDRIVERDB_3=repdb3
set SEPPARAMDRIVERDB_4=repdb4
if "%SEPPARAM_SLEEP%"=="" set SEPPARAM_SLEEP=5
if "%SEPPARAM_SLEEP_2%"=="" set SEPPARAM_SLEEP_2=5
set SEP_CMD_SLEEP=250
set SEP_DIFF_SLEEP=250
if "%2"=="backup" goto BACKUP
if "%2"=="benign" goto BENIGN
if "%2"=="branch" goto BRANCH
if "%2"=="repmgr" goto REPMGR
goto END

:BACKUP
executor %TST_CFG%\repbackup.cfg >%TST_OUTPUT%\backup\repbackup.out
goto END

:BENIGN
executor %TST_CFG%\repbenign.cfg >%TST_OUTPUT%\benign\repbenign.out
goto END

:BRANCH
executor %TST_CFG%\repbranch.cfg >%TST_OUTPUT%\branch\repbranch.out
goto END

:REPMGR
executor %TST_CFG%\reprepmgr.cfg >%TST_OUTPUT%\repmgr\reprepmgr.out
goto END

:END
endlocal
