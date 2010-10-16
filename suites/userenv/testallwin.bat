@ECHO OFF
REM Copyright (c) 2008 Ingres Corporation
REM
REM
REM 		Sample script to facilitate running ALL or a combination
REM 		of the Ingres Acceptance test suites in one background 
REM 		script as user 'testenv' AND, AFTER running tstsetup.bat.
REM 
REM 		Copy this file to your %TST_OUTPUT% directory to run the
REM		desired	test suites. The file should be saved as a .bat 
REM 		file before execution.
REM
REM 
REM 12-Oct-2004 (legru01)
REM		Converted from the original file created by (vande02)
REM  		to operate on Windows.
REM 24-Apr-2007 (vande02)
REM             Removed runstress.bat which is now runappsuite.bat and should be
REM             run outside of this batch job with specific criteria.
REM 02-Nov-2007 (vande02)
REM             Added datetime suite.
REM 27-Aug-2008 (vande02)
REM             Removed the Replicator Service remove/repinst commands from this
REM             script and put them in RUNREP.BAT in the init/clean blocks.
REM

call %TST_SHELL%\runlbnet.bat init  all
call %TST_SHELL%\runlbnet.bat lbnet all
call %TST_SHELL%\runlbnet.bat clean all
call %TST_SHELL%\runbe.bat init  access accntl alttbl api blob
call %TST_SHELL%\runbe.bat be    access accntl alttbl api blob
call %TST_SHELL%\runbe.bat clean access accntl alttbl api blob
call %TST_SHELL%\runbe.bat init  datatypes datetime fastload qryproc util ttpp
call %TST_SHELL%\runbe.bat be    datatypes datetime fastload qryproc util ttpp
call %TST_SHELL%\runbe.bat clean datatypes datetime fastload qryproc util ttpp
call %TST_SHELL%\runbe.bat init  lar
call %TST_SHELL%\runbe.bat be    lar
call %TST_SHELL%\runbe.bat clean lar
call %TST_SHELL%\runbevps.bat  8 all init vps
call %TST_SHELL%\runbevps.bat  8 all vps vps
call %TST_SHELL%\runbevps.bat  64 all init vps
call %TST_SHELL%\runbevps.bat  64 all vps vps
call %TST_SHELL%\runbe.bat init  miscfunc
call %TST_SHELL%\runbe.bat be    miscfunc
call %TST_SHELL%\runbe.bat clean miscfunc
call %TST_SHELL%\runbe.bat init  c2secure
call %TST_SHELL%\runbe.bat be    c2secure
call %TST_SHELL%\runbe.bat clean c2secure
call %TST_SHELL%\runfe.bat init  all
call %TST_SHELL%\runfe.bat fe    all
call %TST_SHELL%\runfe.bat clean all
call %TST_SHELL%\runfe3gl.bat init all
call %TST_SHELL%\runfe3gl.bat 3gl  c

REM ---------------------------------------------------------------------------
REM
REM Make sure the replicator server directories exist
REM
REM ---------------------------------------------------------------------------

if not exist %II_SYSTEM%\ingres\rep md "%II_SYSTEM%\ingres\rep"
if not exist %II_SYSTEM%\ingres\rep\servers md "%II_SYSTEM%\ingres\rep\servers"
if not exist %II_SYSTEM%\ingres\rep\servers\server1 md "%II_SYSTEM%\ingres\rep\servers\server1"
if not exist %II_SYSTEM%\ingres\rep\servers\server2 md "%II_SYSTEM%\ingres\rep\servers\server2"
if not exist %II_SYSTEM%\ingres\rep\servers\server3 md "%II_SYSTEM%\ingres\rep\servers\server3"
if not exist %II_SYSTEM%\ingres\rep\servers\server4 md "%II_SYSTEM%\ingres\rep\servers\server4"
if not exist %II_SYSTEM%\ingres\rep\servers\server5 md "%II_SYSTEM%\ingres\rep\servers\server5"
if not exist %II_SYSTEM%\ingres\rep\servers\server6 md "%II_SYSTEM%\ingres\rep\servers\server6"
if not exist %II_SYSTEM%\ingres\rep\servers\server7 md "%II_SYSTEM%\ingres\rep\servers\server7"
if not exist %II_SYSTEM%\ingres\rep\servers\server8 md "%II_SYSTEM%\ingres\rep\servers\server8"
if not exist %II_SYSTEM%\ingres\rep\servers\server9 md "%II_SYSTEM%\ingres\rep\servers\server9"
if not exist %II_SYSTEM%\ingres\rep\servers\server10 md "%II_SYSTEM%\ingres\rep\servers\server10"

call %TST_SHELL%\runrep.bat init  all
call %TST_SHELL%\runrep.bat rep   backup benign branch repmgr
call %TST_SHELL%\runrep.bat clean all

REM ---------------------------------------------------------------------------
REM 	The star component must be installed before
REM	tests execution. To install it do the following
REM
REM	go to 'start -> settings -> control panel -> add remove programs,'
REM	then highlight Ingres.
REM		
REM		- click the change/remove button
REM            	- choose the modify option then click next
REM            	- expand the Ingres DBMS branch
REM            	- add the Distribution Option then click next
REM            	- click finish 
REM 		- lastly, don't forget to remove the three REM below or start 
REM               Ingres as a service
REM		- execute this script
REM ---------------------------------------------------------------------------

REM call %TST_SHELL%\runstar.bat init  all
REM call %TST_SHELL%\runstar.bat star  all
REM call %TST_SHELL%\runstar.bat clean all
