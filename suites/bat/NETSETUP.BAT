@echo off
REM Copyright (c) 2007 Ingres Corporation
REM
REM Usage: 
REM	This script uses userenv\netutil_wnt.setup file to setup Ingres/Net. 
REM 
REM History
REM	04-Jan-1996 (somsa01)
REM		Created from netsetup.sh.
REM	05-Dec-2003 (somsa01)
REM		Added flag to create tcp_ip vnodes vs. wintcp vnodes.
REM     17-Nov-2000 (madsu02)
REM 		 Updated the script to run on NT4.0, win2000.  This script
REM 		 wasusing 'choice' which is in /WINDOWS/COMMAND. NT and win2000
REM 		 does not recognize 'choice' as system command. This modified
REM 		 script is using ipchoice, which is in %II_SYSTEM%\ingres\bin.
REM		 Added environment variable <name>, now users have the choice
REM		 to create nodes as ingres or as a testenv user.
REM 		 Updated the script to run on NT4.0, win2000.
REM
REM NOTE:
REM This script was using 'choice' which is in /WINDOWS/COMMAND. 
REM NT and win2000 does not recognize 'choice' as system command. This modified
REM script is using ipchoice, which is in %II_SYSTEM%\ingres\bin. Added      
REM environment variable <name>, now users have the choice to create nodes as
REM ingres or as a testenv user.
REM
REM	15-Sep-2004 (legru01)
REM		Removed ALL references to Hetnet because Hetnet tests
REM             will not be supplied with the opensource package
REM             as of this date. Removed test choice Ingres because suite
REM		should be run as user testenv.
REM
REM	28-Nov-2007 (vande02)
REM		 Removed the <name> and <protocol> variables because NET is 
REM		 always run as 'testenv' and the 'tcp_ip' protocol.  One NET
REM		 scenario uses a listen_address with wintcp which must be set
REM		 on the Windows machine used in testing (any desktop).
REM
REM		 Removing the need for prompts with this script so this script
REM		 will now be called by tstsetup.bat. Added
REM		 the 'for /F' command to create a tempfile to pass the needed
REM		 parameters to 'cat' which edits 'netutil_wnt.setup' and passes
REM		 the customized 'netutil_wnt.setup' file to 'netutil -file'.
REM	06-Dec-2007 (vande02)
REM		 Changed ING_LISTEN_ADDRESS to a temporary variable used with
REM		 the 'for /F' command when this script is run.  Eliminates the
REM		 the need for any preset value for ING_LISTEN_ADDRESS.

setlocal

echo Begin Netutil setup

for /F "usebackq" %%i in (`ingprenv II_TEMPORARY`) do set tmpdir=%%i

for /F "usebackq" %%i in (`ingprenv II_INSTALLATION`) do set ING_LISTEN_ADDRESS=%%i

set localnode=%COMPUTERNAME%

set passwd=ca-testenv

set het_listen=II

set listen_address=%ING_LISTEN_ADDRESS%

  echo.
  echo local node = %localnode%
  echo testenv password = %passwd%
  echo local listen address = %listen_address%
  echo.
  echo Running netutil -file-


cat %ING_TST%\suites\userenv\netutil_wnt.setup | sed "s/<localnode>/%localnode%/g" | sed "s/<passwd>/%passwd%/g" | sed "s/<listen_address>/%listen_address%/g" | netutil -file- 

echo.
echo Checking loopback node
echo sql lback::iidbdb
echo.
echo select _version() \p\g\q | sql lback::iidbdb
:DONE
:END
call ipset datevar PCdate
echo.
PCecho "Exiting NETSETUP @ %datevar% . . .
endlocal
