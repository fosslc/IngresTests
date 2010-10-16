@echo off
REM Copyright (c) 2004 Ingres Corporation
REM 
REM  
REM SCRIPT FOR RUNNING AUTOMATED BACK-END TESTS FOR INGRES r3 (WINDOWS).
REM
REM
REM Description:
REM	This script is used to setup and run the backend SEP tests using
REM     listexec and excutor.
REM     Script will call files :  RUNDOBE.BAT
REM
REM Command line :  runbe init|be 'area(s)'
REM     examples :  runbe init accntl
REM                 runbe be lar access
REM                 runbe be all
REM
REM IMPORTANT:  MKS Toolkit or Cygwin must be installed !
REM
REM          
REM  History:
REM	??-Sep-1995 (belar01)
REM		Created from runbe.sh .
REM	04-Jan-1996 (somsa01)
REM		Cleaned up batch file to resemble more closely the shell script.
REM	30-Jan-1996 (somsa01) Added raat tests.
REM	26-Nov-1996 (karye01) created 
REM     28-Jan-1997 (rusfr01) modified
REM	19-MAR-1997 (vissu01) Added testdb statements for lar tests
REM	26-MAR-1997 (vissu01) Added fastload
REM     04-JUN-1997 (wanya01) added ttpp and SOL be subarea
REM     13-JUN-1997 (karye01) modified to run multiple page sizes
REM      5-May-1998 (karye01) changed berlldb to rlldb.
REM	22-feb-1999 (somsa01) Added cleanup area.
REM	26-feb-1999 (somsa01) Added blob test suite.
REM	19-mar-1999 (somsa01) Corrected cleanup of SOL and FASTLOAD databases.
REM	 1-apr-1999 (vande02) Added running of initlar.sep for lar.
REM			      Removed the need for VPG_SZ and STR_ST pre-
REM			      processing.  Also, removed VPS suite because it
REM			      has been separated into its own bat file and still
REM			      contains the preprocessing logic (RUNBEVPS.BAT).
REM     25-Dec-1998 (somsa01) Added running of initlar.sep for lar.
REM	06-Apr-1999 (vande02) removed repeated API lines under RUNTEST routine.
REM     09-Jun-2000 (sarjo01) Added new suite: MISCFUNC for miscellaneous new
REM                           functionality and features.
REM     21-Aug-2000 (vande02) Added test databases, b75662db, b77231db, and
REM                           lar58db to the cleanup section for lar.  And,
REM                           added eia40db to qryproc clean up section.
REM     13-Sep-2000 (sarjo01) Added RODB suite.
REM     26-Apr-2001 (wu$ca01) Added createdb utildb3/utildb4, and initutil.sep
REM                           for the UTIL area for testing copydb new tests.
REM                           Added SEPPARAMDB=beudtdb in UDT area.
REM     06-Jun-2001 (BOIJA02) Creating utildb is wasted effort, it gets 
REM                           destroyed and recreated by the first test, and 
REM			      the next 10 after that for good measure, so we
REM                           now just destroy it and tell the tester that.
REM	27-Sep-2001 (BOIJA02) another year passes, added section specific 
REM			      titles.
REM     20-May-2002 (rigka01) Change $TST_OUTPUT/ to %TST_OUTPUT%\ in four
REM			      instances.	
REM
REM	10-Jul-2001 (wu$ca01) Re-implemented RODB suite. Added miscfunc and
REM                           rodb, and edited blob in the cleanup area.
REM	14-Nov-2001 (wu$ca01) Added createdb -n unidb and set SEPPARAMDB3=unidb
REM                           to the DATATYPES area for testing the new unicode
REM                           datatypes. Added destroydb unidb in Cleanup area.
REM	30-Oct-2001 (shafa01)
REM		Removed UDT/SOL suites from 'all' parameter, and added UDT and
REM		Spatial Objects setup step (executing iilink, ingstop and
REM		restart ingres).  This will enable these two suites to run but
REM		the remainder of the back-end suites should be run with a
REM		generic DBMS that is without udt's installed.  After UDT/SOL
REM		run, when you run 'clean udt' or 'clean sol' the DBMS Server
REM		will be relinked with out UDTs and Spatial Objects and will be
REM		Generic.  If the DBMS server still has UDTs and SOL linked in
REM		(i.e. oiudtnt.dll > oiudtnt.dist = UDTs still exist) only a
REM		WARNING will be put to the beinit.out file and to the
REM		errlog.log during the initialization of all the back-end test
REM		databases except for UDT/SOL. For UDT/SOL, if oiudtnt.dll >
REM		oiudtnt.dist, initialization will continue, if oiudtnt.dll <=
REM		oiudtnt.dist, then the initialization will exit with warnings.
REM	06-oct-2003 (somsa01)
REM	    Updated UDT test for 64-bit.
REM	11-Sep-2004	(rogch01 and legru01)
REM		Changes for r3 open source: dll names changed.
REM	15-Sep-2004 (legru01)
REM		Removed Changes submitted by shafa01 on 30-Oct-2001 because
REM		UDT and SOL tests will not be provided with the OpenSource
REM		Acceptance Test package.
REM		Removed irrelevant test sections for the OpenSource Acceptance
REM		Tests package (i.e., RAAT, RLL, RODB, UDT, SOL).
REM		Moved c2secure testing to the end of the RUNTEST
REM		and CLEAN test areas. Added commands to enable and disable
REM             c2 audit and recycle Ingres after both instances (enable,
REM		disable). This change prevents the need to execute the 
REM             c2secure testing as a standalone test option outside of
REM             running RUNBE.BAT from start to finish.
REM	05-May-2005 (vande02)
REM             Adding cleanlar.sep and cleanmsfn.sep during the clean phase
REM             to cleanup database locations created during suites for LAR and
REM             MISCFUNC.
REM	08-Aug-2005 (vande02)
REM             Because acc10.sep is really an initialization test for database
REM             grantdb used by 32 subsequent tests, this change adds acc10.sep
REM             to be run during the initialization phase.  This will allow the
REM             rerun of single tests out of sequence in the be/accntl suite
REM             as long as 'runbe init accntl' was run at least once.
REM	08-Mar-2007 (vande02)
REM             Added -n flag to alttbldb creation to enable Unicode.
REM     10-Sep-2007 (sarjo01)
REM             Added setting of SEPPARAMDB, SEPPARAMAPI_INC variables to API
REM             run section for new test, api13.sep
REM     02-Nov-2007 (vande02)
REM             Skipped the 06-Apr-2007 change during last integration (old change
REM             486205).  Decided to add ANSIDATE data type suite datetime.
REM
REM ---------------------------------------------------------------------------
REM			Setup Area
REM ---------------------------------------------------------------------------

setlocal
cls
color 0b

REM    Set the output directory for test results.
REM

if not "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%TST_ROOT_OUTPUT%\be

if "%TST_ROOT_OUTPUT%"=="" set TST_OUTPUT=%ING_TST%\output\be
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul PCecho "Creating Directory - %ING_TST%\output
	title  Creating Directory - %ING_TST%\output
if "%TST_ROOT_OUTPUT%"=="" if not exist %ING_TST%\output\nul mkdir %ING_TST%\output

if not exist %TST_OUTPUT%\nul PCecho "Creating Directory - %TST_OUTPUT%"
	title  Creating Directory - %TST_OUTPUT%
if not exist %TST_OUTPUT%\nul mkdir %TST_OUTPUT%


echo               --------------------------------------------
echo               Output files will be written to %TST_OUTPUT%
echo               --------------------------------------------

set flag=%1

if "%1"=="init" if not "%2"=="" goto INIT
if "%1"=="be" goto NEXT
if "%1"=="clean" goto CLEAN
	color 0c
	echo ***  User must input the modules to be initalized 
	echo ***  or specify "all".
	echo.
	echo ***  Example: runbe init all
	echo               or 
	echo               runbe init access util
	echo.
	pause
	color
	goto END2
:NEXT
if "%1"=="be" if not "%2"=="" goto RUNTEST
	color 0c
	echo ***  User must input the modules to be run
	echo ***  or specify all
	echo. 
	echo ***  Example: runbe be all
	echo               or
	echo               runbe be access util
	echo.
	pause
	color
	goto END2
rem ----------------------------------------------------------------------------
rem                        Initialization Area
rem ----------------------------------------------------------------------------


:INIT
if "%2"=="" goto END
	if not "%2"=="all" if not "%2"=="access" goto InitAccntl
	  if not exist %TST_OUTPUT%\access\nul echo Creating Directory - %TST_OUTPUT%\access
	  if not exist %TST_OUTPUT%\access\nul mkdir %TST_OUTPUT%\access
	  call ipset datevar PCdate
	  PCecho " Creating BE\Access database @ %datevar% . . ."
	  title   Creating BE\Access database @ %datevar% . . .
 	  destroydb accessdb1 >>%TST_OUTPUT%\beinit.out
	  createdb accessdb1 >>%TST_OUTPUT%\beinit.out
	  set AREA=access
          if "%2"=="all" goto InitAccntl
	  shift
	  goto INIT

:InitAccntl
	if not "%2"=="all" if not "%2"=="accntl" goto InitDatetime
	  if not exist %TST_OUTPUT%\accntl\nul echo Creating Directory - %TST_OUTPUT%\accntl
	  if not exist %TST_OUTPUT%\accntl\nul mkdir %TST_OUTPUT%\accntl
	  call ipset datevar PCdate
	  PCecho " Creating BE\Accntl database @ %datevar% . . ."
	  title   Creating BE\Accntl database @ %datevar% . . .
	  destroydb accntldb >>%TST_OUTPUT%\beinit.out
	  createdb accntldb >>%TST_OUTPUT%\beinit.out
          set SEPPARAMDB3=grantdb
          sep -b -v %ING_TST%\be\accntl\sep\acc10.sep -o%TST_OUTPUT%\accntl\initaccntl.out
	  set AREA=accntl
          if "%2"=="all" goto InitDatetime
	  shift
	  goto INIT 

:InitDatetime
	if not "%2"=="all" if not "%2"=="datetime" goto InitDatatypes
	  if not exist %TST_OUTPUT%\datetime\nul echo Creating Directory - %TST_OUTPUT%\datetime
	  if not exist %TST_OUTPUT%\datetime\nul mkdir %TST_OUTPUT%\datetime
	  call ipset datevar PCdate
	  PCecho " Creating BE\Datetime database @ %datevar% . . ."
	  title   Creating BE\Datetime database @ %datevar% . . .
	  destroydb datetimedb >>%TST_OUTPUT%\beinit.out
	  createdb datetimedb >>%TST_OUTPUT%\beinit.out
	  set AREA=datetime
          if "%2"=="all" goto InitDatatypes
	  shift
	  goto INIT 

:InitDatatypes
	if not "%2"=="all" if not "%2"=="datatypes" goto InitMiscfunc
	  if not exist %TST_OUTPUT%\datatypes\nul echo Creating Directory - %TST_OUTPUT%\datatypes
	  if not exist %TST_OUTPUT%\datatypes\nul mkdir %TST_OUTPUT%\datatypes
	  call ipset datevar PCdate
	  PCecho " Creating BE\DataTypes databases @ %datevar% . . ."
	  title   Creating BE\DataTypes databases @ %datevar% . . .
	  destroydb datatypedb >>%TST_OUTPUT%\beinit.out
	  createdb datatypedb >>%TST_OUTPUT%\beinit.out
	  destroydb datatypedb2 >>%TST_OUTPUT%\beinit.out
	  createdb datatypedb2 >>%TST_OUTPUT%\beinit.out
          destroydb unidb >>%TST_OUTPUT%\beinit.out
          createdb -n unidb >>%TST_OUTPUT%\beinit.out
          set AREA=datatypes
	  if "%2"=="all" goto InitMiscfunc
	  shift
	  goto INIT

:InitMiscfunc
        if not "%2"=="all" if not "%2"=="miscfunc" goto InitLar
	  if not exist %TST_OUTPUT%\miscfunc\nul echo Creating Directory - %TST_OUTPUT%\miscfunc
          if not exist %TST_OUTPUT%\miscfunc\nul mkdir %TST_OUTPUT%\miscfunc
          call ipset datevar PCdate
          PCecho " Creating BE\miscfunc databases @ %datevar% . . ."
	  title   Creating BE\miscfunc databases @ %datevar% . . .
          destroydb msfndb >>%TST_OUTPUT%\beinit.out
          createdb msfndb >>%TST_OUTPUT%\beinit.out
          set AREA=miscfunc
          if "%2"=="all" goto InitLar
          shift
          goto INIT

:InitLar
	if not "%2"=="all" if not "%2"=="lar" goto InitTtpp
	  if not exist %TST_OUTPUT%\lar\nul echo Creating Directory - %TST_OUTPUT%\lar
	  if not exist %TST_OUTPUT%\lar\nul mkdir %TST_OUTPUT%\lar
	  call ipset datevar PCdate
	  PCecho " Creating BE\LAR database @ %datevar% . . ."
	  title   Creating BE\LAR database @ %datevar% . . .
	  destroydb lardb >>%TST_OUTPUT%\beinit.out
	  createdb lardb >>%TST_OUTPUT%\beinit.out
	  destroydb testdb >>%TST_OUTPUT%\beinit.out
	  createdb testdb >>%TST_OUTPUT%\beinit.out
          set SEPPARAMDB=lardb
          sep -b -v %TST_INIT%\initlar.sep -o%TST_OUTPUT%\lar\initlar.out
	  set AREA=lar
	  if "%2"=="all" goto InitTtpp
	  shift
	  goto INIT

:InitTtpp
	if not "%2"=="all" if not "%2"=="ttpp" goto InitQryproc
	  if not exist %TST_OUTPUT%\ttpp\nul echo Creating Directory - %TST_OUTPUT%\ttpp
	  if not exist %TST_OUTPUT%\ttpp\nul mkdir %TST_OUTPUT%\ttpp
	  call ipset datevar PCdate
	  PCecho " Creating BE\ttpp database @ %datevar% . . ."
	  title   Creating BE\ttpp database @ %datevar% . . .
	  destroydb ttppdb >>%TST_OUTPUT%\beinit.out
	  createdb ttppdb >>%TST_OUTPUT%\beinit.out
	  set AREA=ttpp
	  if "%2"=="all" goto InitQryproc
	  shift
	  goto INIT

:InitQryproc
	if not "%2"=="all" if not "%2"=="qryproc" goto InitApi
	  if not exist %TST_OUTPUT%\qryproc\nul echo Creating Directory - %TST_OUTPUT%\qryproc
	  if not exist %TST_OUTPUT%\qryproc\nul mkdir %TST_OUTPUT%\qryproc
	  call ipset datevar PCdate
	  PCecho " Creating BE\Qryproc database @ %datevar% . . ."
	  title   Creating BE\Qryproc database @ %datevar% . . .
	  destroydb qryprocdb >>%TST_OUTPUT%\beinit.out
	  createdb qryprocdb >>%TST_OUTPUT%\beinit.out
	  set AREA=qryproc
	  if "%2"=="all" goto InitApi
	  shift
	  goto INIT

:InitApi
	if not "%2"=="all" if not "%2"=="api" goto InitUtil
	  if not exist %TST_OUTPUT%\api\nul echo Creating Directory - %TST_OUTPUT%\api
	  if not exist %TST_OUTPUT%\api\nul mkdir %TST_OUTPUT%\api
	  call ipset datevar PCdate
	  PCecho " Creating BE\API databases @ %datevar% . . ."
	  title   Creating BE\API databases @ %datevar% . . .
	  destroydb apidb1 >>%TST_OUTPUT%\beinit.out
	  createdb apidb1 >>%TST_OUTPUT%\beinit.out
	  destroydb apidb2 >>%TST_OUTPUT%\beinit.out
	  createdb apidb2 >>%TST_OUTPUT%\beinit.out
	  set AREA=api
	  if "%2"=="all" goto InitUtil
	  shift
	  goto INIT

:InitUtil
	if not "%2"=="all" if not "%2"=="util" goto InitAlttbl
	  if not exist %TST_OUTPUT%\util\nul echo Creating Directory - %TST_OUTPUT%\util
	  if not exist %TST_OUTPUT%\util\nul mkdir %TST_OUTPUT%\util
	  call ipset datevar PCdate
	  PCecho " Creating BE\UTILITY database @ %datevar% . . ."
	  title   Creating BE\UTILITY database @ %datevar% . . .
	  destroydb utildb >>%TST_OUTPUT%\beinit.out
          destroydb utildb3 >>%TST_OUTPUT%\beinit.out
          createdb utildb3 >>%TST_OUTPUT%\beinit.out
          destroydb utildb4 >>%TST_OUTPUT%\beinit.out
          createdb utildb4 >>%TST_OUTPUT%\beinit.out
          set SEPPARAMDB3=utildb3
          sep -b -v %TST_INIT%\initutil.sep -o%TST_OUTPUT%\util\initutil.out
	  set AREA=util
	  if "%2"=="all" goto InitAlttbl
	  shift
	  goto INIT

:InitAlttbl
	if not "%2"=="all" if not "%2"=="alttbl" goto InitFastload
	  if not exist %TST_OUTPUT%\alttbl\nul echo Creating Directory - %TST_OUTPUT%\alttbl
	  if not exist %TST_OUTPUT%\alttbl\nul mkdir %TST_OUTPUT%\alttbl
	  call ipset datevar PCdate
	  PCecho " Creating BE\ALTTBL database @ %datevar% . . ."
	  title   Creating BE\ALTTBL database @ %datevar% . . .
	  destroydb alttbldb >>%TST_OUTPUT%\beinit.out
	  createdb -n alttbldb >>%TST_OUTPUT%\beinit.out
	  set AREA=alttbl
	  if "%2"=="all" goto InitFastload
	  shift
	  goto INIT


:InitFastload
	if not "%2"=="all" if not "%2"=="fastload" if not "%2"=="fld" goto InitBlob
	  if not exist %TST_OUTPUT%\fld\nul echo Creating Directory - %TST_OUTPUT%\fld
	  if not exist %TST_OUTPUT%\fld\nul mkdir %TST_OUTPUT%\fld
	  call ipset datevar PCdate
	  PCecho " Creating BE\FASTLOAD database @ %datevar% . . ."
	  title   Creating BE\FASTLOAD database @ %datevar% . . .
	  destroydb flddb >>%TST_OUTPUT%\beinit.out
	  createdb flddb >>%TST_OUTPUT%\beinit.out
	  set AREA=fld
	  if "%2"=="all" goto InitBlob
	  shift
	  goto INIT

:InitBlob
	if not "%2"=="all" if not "%2"=="blob" goto InitC2secure
	  if not exist %TST_OUTPUT%\blob\nul echo Creating Directory - %TST_OUTPUT%\blob
	  if not exist %TST_OUTPUT%\blob\nul mkdir %TST_OUTPUT%\blob
	  call ipset datevar PCdate
	  PCecho " Creating BE\BLOB database @ %datevar% . . ."
	  title   Creating BE\BLOB database @ %datevar% . . .
	  destroydb blobdb >>%TST_OUTPUT%\beinit.out
	  createdb blobdb >>%TST_OUTPUT%\beinit.out
	  set AREA=blob
	  if "%2"=="all" goto InitC2Secure
	  shift
	  goto INIT
	  
:InitC2secure
	if not "%2"=="all" if not "%2"=="c2secure" goto END
	  if not exist %TST_OUTPUT%\c2secure\nul echo Creating Directory - %TST_OUTPUT%\c2secure
	  if not exist %TST_OUTPUT%\c2secure\nul mkdir %TST_OUTPUT%\c2secure
	  call ipset datevar PCdate
	  PCecho " Creating BE\C2SECURE database @ %datevar% . . ."
	  title   Creating BE\C2SECURE database @ %datevar% . . .
	  destroydb securedb >>%TST_OUTPUT%\beinit.out
	  createdb securedb >>%TST_OUTPUT%\beinit.out
	  set AREA=c2secure
	  if "%2"=="all" goto END
	  shift
	  goto INIT

:RUNTEST

if "%2"=="" goto END
	if not "%2"=="all" if not "%2"=="access" goto ACCNTL
	  set SEPPARAMDB=accessdb1
	  set SEPPARAMDRIVERDB=-daccessdb1
	  set AREA=access
	  set CFG_FILE=beam.cfg
	  set LIS_FILE=beam.lis
	  set OUT_FILE=beam.out
	  call ipset datevar PCdate
	  echo.
	  PCecho " Running the Access tests @ %datevar%   . . ."
	  title   Running the Access tests @ %datevar%   . . .
	  call rundobe
	  if "%2"=="all" goto ACCNTL
	  shift
	  goto RUNTEST

:ACCNTL
	if not "%2"=="all" if not "%2"=="accntl" goto DATETIME
	  set SEPPARAMDB=accntldb
	  set SEPPARAMDB3=grantdb
	  set SEPPARAMDRIVERDB=-dgrantdb
	  set TST_TESTTOOLS=%ING_TST%\testtool
	  set AREA=accntl
	  set CFG_FILE=beacc.cfg
	  set LIS_FILE=beacc.lis
	  set OUT_FILE=beacc.out
	  call ipset datevar PCdate
	  PCecho " Running the ACCNTL tests @ %datevar%    . . ."
	  title   Running the ACCNTL tests @ %datevar%    . . .
	  call rundobe
	  if "%2"=="all" goto DATETIME
	  shift
	  goto RUNTEST

:DATETIME
       if not "%2"=="all" if not "%2"=="datetime" goto DATATYPES
         set SEPPARAMDB=datetimedb
         set AREA=datetime
         set CFG_FILE=bedatetime.cfg
         set LIS_FILE=bedatetime.lis
         set OUT_FILE=bedatetime.out
         call ipset datevar PCdate
         PCecho " Running the DATETIME tests @ %datevar%    . . ."
         title   Running the DATETIME tests @ %datevar%    . . .
         call rundobe
	 if "%2"=="all" goto DATATYPES
	 shift
	 goto RUNTEST

:DATATYPES
	if not "%2"=="all" if not "%2"=="datatypes" goto MISCFUNC
	  set SEPPARAMDB1=datatypedb
	  set SEPPARAMDB2=datatypedb2
	  set SEPPARAMDB=datatypedb2
          set SEPPARAMDB3=unidb
	  set AREA=datatypes
	  set CFG_FILE=bedt.cfg
	  set LIS_FILE=bedt.lis
	  set OUT_FILE=bedt.out
	  call ipset datevar PCdate
	  PCecho " Running the DATATYPES tests @ %datevar%     . . ."
	  title   Running the DATATYPES tests @ %datevar%     . . .
	  call rundobe
	  if "%2"=="all" goto MISCFUNC
	  shift
	  goto RUNTEST

:MISCFUNC
        if not "%2"=="all" if not "%2"=="miscfunc" goto LAR
          set SEPPARAMDB=msfndb
          set AREA=miscfunc
          set CFG_FILE=bemsfn.cfg
          set LIS_FILE=bemsfn.lis
          set OUT_FILE=bemsfn.out
          call ipset datevar PCdate
          PCecho " Running the MISCFUNC tests @ %datevar%     . . ."
	  title   Running the MISCFUNC tests @ %datevar%     . . .
          call rundobe
          if "%2"=="all" goto LAR
          shift
          goto RUNTEST

:LAR
	if not "%2"=="all" if not "%2"=="lar" goto TTPP
	  set SEPPARAMDB=lardb
	  set SEPPARAMDB2=testdb
	  set AREA=lar
	  set CFG_FILE=belar.cfg
	  set LIS_FILE=belar.lis
	  set OUT_FILE=belar.out
	  call ipset datevar PCdate
	  PCecho " Running the LAR tests @ %datevar%     . . ."
	  title   Running the LAR tests @ %datevar%     . . .
	  call rundobe
	  if "%2"=="all" goto TTPP
	  shift
	  goto RUNTEST

:TTPP
	if not "%2"=="all" if not "%2"=="ttpp" goto  QRYPROC
	  set SEPPARAMDB=ttppdb
	  set TST_TESTTOOLS=%ING_TST%\testtool
	  set AREA=ttpp
	  set CFG_FILE=bettpp.cfg
	  set LIS_FILE=bettpp.lis
	  set OUT_FILE=bettpp.out
	  call ipset datevar PCdate
	  PCecho " Running the TTPP tests @ %datevar%    . . ."
	  title   Running the TTPP tests @ %datevar%    . . .
	  call rundobe
	  if "%2"=="all" goto QRYPROC
	  shift
	  goto RUNTEST

:QRYPROC
	if not "%2"=="all" if not "%2"=="qryproc" goto API
	  set SEPPARAMDB=qryprocdb
	  set SEPPARAMDRIVERDB=-dqryprocdb
	  set TST_TESTTOOLS=%ING_TST%\testtool
	  set AREA=qryproc
	  set CFG_FILE=beqp.cfg
	  set LIS_FILE=beqp.lis
	  set OUT_FILE=beqp.out
	  call ipset datevar PCdate
	  PCecho " Running the QRYPROC tests @ %datevar%    . . ."
	  title   Running the QRYPROC tests @ %datevar%    . . .
	  call rundobe
	  if "%2"=="all" goto API
	  shift
	  goto RUNTEST
	
:API
	if not "%2"=="all" if not "%2"=="api" goto UTIL
	  set TST_TESTTOOLS=%ING_TST%\testtool
	  set AREA=api
	  set CFG_FILE=beapi.cfg
	  set LIS_FILE=beapi.lis
	  set OUT_FILE=beapi.out
          set SEPPARAMAPI_INC=-I%II_SYSTEM%\ingres\files
          set SEPPARAMDB=apidb2
	  call ipset datevar PCdate
	  PCecho " Running the API tests @ %datevar%   . . ."
	  title   Running the API tests @ %datevar%   . . .
	  call rundobe
	  if "%2"=="all" goto UTIL
	  shift
	  goto RUNTEST

:UTIL
	if not "%2"=="all" if not "%2"=="util" goto FASTLOAD
	  set SEPPARAMDB=utildb
          set SEPPARAMDB3=utildb3
          set SEPPARAMDB4=utildb4
	  set AREA=util
	  set CFG_FILE=beutil.cfg
	  set LIS_FILE=beutil.lis
	  set OUT_FILE=beutil.out
	  call ipset datevar PCdate
	  PCecho " Running the BE UTILITY tests @ %datevar%    . . ."
	  title   Running the BE UTILITY tests @ %datevar%    . . .
	  call rundobe
	  if "%2"=="all" goto FASTLOAD
	  shift
	  goto RUNTEST

:FASTLOAD
	if not "%2"=="all" if not "%2"=="fastload" if not "%2"=="fld" goto BLOB
	  set SEPPARAMDB=flddb
	  set AREA=fld
	  set CFG_FILE=befld.cfg
	  set LIS_FILE=befld.lis
	  set OUT_FILE=befld.out
	  call ipset datevar PCdate
	  PCecho " Running the BE FASTLOAD tests @ %datevar%    . . ."
	  title   Running the BE FASTLOAD tests @ %datevar%    . . .
	  call rundobe
	  if "%2"=="all" goto BLOB
	  shift
	  goto RUNTEST

:BLOB
	if not "%2"=="all" if not "%2"=="blob" goto ALTTBL
	  set SEPPARAMDB=blobdb
	  set AREA=blob
	  set CFG_FILE=beblob.cfg
	  set LIS_FILE=beblob.lis
	  set OUT_FILE=beblob.out
	  call ipset datevar PCdate
	  PCecho " Running the BE BLOB tests @ %datevar%    . . ."
	  title   Running the BE BLOB tests @ %datevar%    . . .
	  call rundobe
	  if "%2"=="all" goto ALTTBL
	  shift
	  goto RUNTEST

:ALTTBL
	if not "%2"=="all" if not "%2"=="alttbl" goto C2SECURE
	  set SEPPARAMDB=alttbldb
	  set AREA=alttbl
	  set CFG_FILE=bealttbl.cfg
	  set LIS_FILE=bealttbl.lis
	  set OUT_FILE=bealttbl.out
	  call ipset datevar PCdate
	  PCecho " Running the BE Alter Table tests @ %datevar%    . . ."
	  title   Running the BE Alter Table tests @ %datevar%    . . .
	  call rundobe
	  if "%2"=="all" goto C2SECURE
	  shift
	  goto RUNTEST
	  	  
:C2SECURE
	if not "%2"=="all" if not "%2"=="c2secure" goto END
	  set SEPPARAMDB=securedb
	  set AREA=c2secure
	  set CFG_FILE=bec2.cfg
	  set LIS_FILE=bec2.lis
	  set OUT_FILE=bec2.out
	  call ipset datevar PCdate
	  PCecho " Running the BE C2 Secure tests @ %datevar%    . . ."
	  title   Running the BE C2 Secure tests @ %datevar%    . . .
	  call ipset SERVER_HOST=iigetres ii.*.config.server_host
	  if %SERVER_HOST% == "" PCecho "Test Running Error" goto END
	  qawtl "TURNING C2 SECURITY AUDITING ON THEN STOPPING AND RESTARTING INGRES"
	  qasetuser ingres iisetres ii."*".c2.security_auditing ON
	  qasetuser ingres ingstop 
	  sleep 5
	  qasetuser ingres ingstart -service
	  sleep 5
	  call rundobe
	  qawtl "TURNING C2 SECURITY AUDITING OFF THEN STOPPING AND RESTARTING INGRES"
	  qasetuser ingres iisetres ii."*".c2.security_auditing OFF
	  qasetuser ingres ingstop 
	  sleep 5
	  qasetuser ingres ingstart -service
	  sleep 5
	  if "%2"=="all" goto END
	  shift
	  goto RUNTEST


:CLEAN
REM ----------------------------------------------------------------------------
REM                        Cleanup Area
REM ----------------------------------------------------------------------------

if not "%2" == "" goto CleanLoop
	PCecho "You must input the modules to be cleaned up "
	PCecho "or specify "all"."
	echo.
	PCecho " Example: runbe clean all "
	echo.
	PCecho "          or "
	echo.
	PCecho "          runbe clean access util ... "
	echo.
	goto END2

:CleanLoop
    if "%2"=="" goto END

:CleanAccess
    if not "%2"=="all" if not "%2"=="access" goto CleanAccntl
	call ipset datevar PCdate
	PCecho "Destroying BE/Access database @ %datevar%"
	title  Destroying BE/Access database @ %datevar%
	echo.
	destroydb accessdb1 >>%TST_OUTPUT%\beclean.out
	if "%2"=="all" goto CleanAccntl
	shift
	goto CleanLoop

:CleanAccntl
    if not "%2"=="all" if not "%2"=="accntl" goto CleanApi
	call ipset datevar PCdate
	PCecho "Destroying BE/Accntl database @ %datevar%"
	title  Destroying BE/Accntl database @ %datevar%
	echo.
	destroydb accntldb >>%TST_OUTPUT%\beclean.out
	destroydb -upvusr1 grantdb >>%TST_OUTPUT%\beclean.out
	if "%2"=="all" goto CleanApi
	shift
	goto CleanLoop

:CleanApi
    if not "%2"=="all" if not "%2"=="api" goto CleanDt
	call ipset datevar PCdate
	PCecho "Destroying BE/Api database @ %datevar%"
	title  Destroying BE/Api database @ %datevar%
	echo.
	destroydb apidb1 >>%TST_OUTPUT%\beclean.out
	destroydb apidb2 >>%TST_OUTPUT%\beclean.out
	if "%2"=="all" goto CleanDt
	shift
	goto CleanLoop

:CleanDt
    if not "%2"=="all" if not "%2"=="datatypes" goto CleanLar
	call ipset datevar PCdate
	PCecho "Destroying BE/DataTypes databases @ %datevar%"
	title  Destroying BE/DataTypes databases @ %datevar%
	destroydb datatypedb >>%TST_OUTPUT%\beclean.out
	echo.
	destroydb datatypedb2 >>%TST_OUTPUT%\beclean.out
        destroydb unidb >>%TST_OUTPUT%\beclean.out
	if "%2"=="all" goto CleanLar
	shift
	goto CleanLoop

:CleanLar
    if not "%2"=="all" if not "%2"=="lar" goto CleanQryproc
	call ipset datevar PCdate
	PCecho "Destroying BE/LAR database @ %datevar%"
	title  Destroying BE/LAR database @ %datevar%
	echo.
	destroydb lardb >>%TST_OUTPUT%\beclean.out
	destroydb lar44db >>%TST_OUTPUT%\beclean.out
	destroydb lar45db >>%TST_OUTPUT%\beclean.out
	destroydb lar58db >>%TST_OUTPUT%\beclean.out
	destroydb lar11_ckp_table >>%TST_OUTPUT%\beclean.out
	destroydb lar_ckp_table >>%TST_OUTPUT%\beclean.out
	destroydb b77231db >>%TST_OUTPUT%\beclean.out
	destroydb b75662db >>%TST_OUTPUT%\beclean.out
        sep -b -v %TST_INIT%\cleanlar.sep -o%TST_OUTPUT%\lar\cleanlar.out
	if "%2"=="all" goto CleanQryproc
	shift
	goto CleanLoop

:CleanQryproc
    if not "%2"=="all" if not "%2"=="qryproc" goto CleanUtil
	call ipset datevar PCdate
	PCecho "Destroying BE/QryProc database @ %datevar%"
	title  Destroying BE/QryProc database @ %datevar%
	echo.
	destroydb qryprocdb >>%TST_OUTPUT%\beclean.out
	qasetuser ingres destroydb eia40db >>%TST_OUTPUT%\beclean
	if "%2"=="all" goto CleanUtil
	shift
	goto CleanLoop

:CleanUtil
    if not "%2"=="all" if not "%2"=="util" goto CleanBlob
	call ipset datevar PCdate
	PCecho "Destroying BE/UTILITY database @ %datevar%"
	title  Destroying BE/UTILITY database @ %datevar%
	echo.
	destroydb utildb >>%TST_OUTPUT%\beclean.out
        destroydb utildb3 >>%TST_OUTPUT%\beclean.out
        destroydb utildb4 >>%TST_OUTPUT%\beclean.out
	if "%2"=="all" goto CleanBlob
	shift
	goto CleanLoop

:CleanBlob
    if not "%2"=="all" if not "%2"=="blob" goto CleanAlttbl
	call ipset datevar PCdate
	PCecho "Destroying BE/BLOB database @ %datevar%"
	title  Destroying BE/BLOB database @ %datevar%
	echo.
	destroydb blobdb >>%TST_OUTPUT%\beclean.out
	if "%2"=="all" goto CleanAlttbl
	shift
	goto CleanLoop

:CleanAlttbl
    if not "%2"=="all" if not "%2"=="alttbl" goto CleanDatetime
	call ipset datevar PCdate
	PCecho "Destroying BE/ALTABLE database @ %datevar%"
	title  Destroying BE/ALTABLE database @ %datevar%
	echo.
	destroydb alttbldb >>%TST_OUTPUT%\beclean.out
	if "%2"=="all" goto CleanDatetime
	shift
	goto CleanLoop

:CleanDatetime
    if not "%2"=="all" if not "%2"=="datetime" goto CleanTtpp
        call ipset datevar PCdate
        PCecho "Destroying BE/DATETIME database @ %datevar%"
        title  Destroying BE/DATETIME database @ %datevar%
        echo.
        destroydb datetimedb >>%TST_OUTPUT%\beclean.out
        if "%2"=="all" goto CleanTtpp
        shift
        goto CleanLoop

:CleanTtpp
    if not "%2"=="all" if not "%2"=="ttpp" goto CleanFstld
	call ipset datevar PCdate
	PCecho "Destroying BE/TTPP database @ %datevar%"
	title  Destroying BE/TTPP database @ %datevar%
	echo.
	destroydb ttppdb >>%TST_OUTPUT%\beclean.out
	if "%2"=="all" goto CleanFstld
	shift
	goto CleanLoop

:CleanFstld
    if not "%2"=="all" if not "%2"=="fastload" goto CleanMiscfunc
	call ipset datevar PCdate
	PCecho "Destroying BE/FASTLOAD database @ %datevar%"
	title  Destroying BE/FASTLOAD database @ %datevar%
	echo.
	destroydb flddb >>%TST_OUTPUT%\beclean.out
	if "%2"=="all" goto CleanMiscfunc
	shift
	goto CleanLoop

:CleanMiscfunc
    if not "%2"=="all" if not "%2"=="miscfunc" goto CleanC2secure
        call ipset datevar PCdate
        PCecho "Destroying BE/MISCFUNC database @ %datevar%"
        echo.
        destroydb msfndb >>%TST_OUTPUT%\beclean.out
        sep -b -v %TST_INIT%\cleanmsfn.sep -o%TST_OUTPUT%\miscfunc\cleanmsfn.out
        if "%2"=="all" goto CleanC2Secure
        shift
        goto CleanLoop
        
:CleanC2secure
    if not "%2"=="all" if not "%2"=="c2secure" goto END
    	call ipset datevar PCdate
	PCecho "Destroying BE/C2SECURE database @ %datevar%"
	title  Destroying BE/C2SECURE database @ %datevar%
	destroydb securedb >>%TST_OUTPUT%\beclean.out
	echo.
	call ipset SERVER_HOST=iigetres ii.*.config.server_host
	if %SERVER_HOST% == "" PCecho "Test Cleanup Error" goto END
	qawtl "TURNING C2 SECURITY AUDITING OFF THEN STOPPING AND RESTARTING INGRES"
	qasetuser ingres iisetres ii."*".c2.security_auditing OFF
	qasetuser ingres ingstop 
	sleep 5
	qasetuser ingres ingstart -service
	sleep 5
	if "%2"=="all" goto END
	shift
	goto CleanLoop

:END
if "%flag%"=="init" echo Backend databases are created and ready for testing.
if "%flag%"=="be" call ipset datevar PCdate
if "%flag%"=="be" echo Backend tests are complete at %datevar%
if "%flag%"=="clean" echo Backend databases are destroyed.
goto END2

:END2
endlocal
color
title Done  
