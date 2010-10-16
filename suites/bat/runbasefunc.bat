@ECHO OFF
REM Copyright (c) 2004 Ingres Corporation
REM
REM 
REM Ingres Base Functionality Test
REM 
REM History:
REM 
REM 13-Jul-2004	Created.
REM 11-Oct-2004 (legru01)
REM		Converted from file runbasefunc.sh to runbasefunc.bat file
REM

REM set variable for the correct file editing command
SET fedcmd=""
FOR %%P in (%PATH%) do IF EXIST %%P\gawk.exe SET fedcmd=gawk
IF %fedcmd%=="" SET fedcmd=awk

PCecho "Running the Ingres Base Functionality Test"
PCdate

set IBFOUT=%TST_OUTPUT%\basefunc\basefunc.log
set TSTDB=basefuncdb1

if not exist %TST_OUTPUT%\basefunc\nul PCecho "Creating Directory - %TST_OUTPUT%\basefunc"
if not exist %TST_OUTPUT%\basefunc\nul mkdir %TST_OUTPUT%\basefunc

PCdate > %IBFOUT%
PCecho "Ingres Base Functionality Test" >> %IBFOUT%
PCecho "Test output located in %IBFOUT%"

chdir %TST_OUTPUT%\basefunc

PCecho " " >> %IBFOUT%
PCecho "==== Getting Ingres Version information..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

type %II_SYSTEM%\ingres\version.rel >> %IBFOUT%

PCecho " ">> %IBFOUT%
PCecho "==== Starting Ingres installation..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

ingstart -service >> %IBFOUT%

destroydb %TSTDB% >> %IBFOUT% > nul

PCecho " " >> %IBFOUT%
PCecho "====" >> %IBFOUT%
PCecho "====" >> %IBFOUT%
PCecho "==== Testing create database, checkpoint, rollforward..." >> %IBFOUT%
PCecho "====" >> %IBFOUT%
PCecho "====" >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Creating database %TSTDB%..." >> %IBFOUT%
PCecho " " >> %IBFOUT%
createdb %TSTDB% >> %IBFOUT%

if not errorlevel 0 goto END

PCecho " ">> %IBFOUT%
PCecho "==== Creating table in %TSTDB% and inserting data..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

PCecho " " >> %IBFOUT%

PCecho "create table ckptest (col1 int,col2 int,col3 int,col4 int,col5 int)\g" > file1.sql
PCecho "insert into ckptest values (1,2,3,4,5)\g" >> file1.sql
PCecho "insert into ckptest values (6,7,8,9,10)\g" >> file1.sql
PCecho "insert into ckptest values (11,12,13,14,15)\g" >> file1.sql
PCecho "insert into ckptest values (16,17,18,149,20)\g" >> file1.sql
PCecho "insert into ckptest values (21,22,23,24,25)\g\q" >> file1.sql
sql %TSTDB% < file1.sql >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "Table created and data inserted." >> %IBFOUT%
PCecho "Should show 1 row X 5 above..." >> %IBFOUT%
PCecho " " >>%IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Checkpointing database %TSTDB%..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

ckpdb +j +w %TSTDB% >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Deleting data from table..." >> %IBFOUT%
PCecho " " >> %IBFOUT%


PCecho "delete from ckptest\g\q" > file2.sql
sql %TSTDB% < file2.sql >>%IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "Deletion of data completed." >> %IBFOUT%
PCecho "Should show 5 rows above." >> %IBFOUT%
PCecho " ">> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Rolling forward database %TSTDB% (with journals)..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

rollforwarddb +j +w %TSTDB% >> %IBFOUT% 

PCecho " " >> %IBFOUT%
PCecho "==== Verifying that journals have deleted the data..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

PCecho "select * from ckptest\g\q" > file3.sql
sql %TSTDB% < file3.sql >> %IBFOUT%

PCecho "Verification completed." >> %IBFOUT% 
PCecho "Should show (0 rows) in table above." >> %IBFOUT%
PCecho " ">> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Rolling forward database %TSTDB% (without journals)..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

rollforwarddb -j +w %TSTDB% >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Verifying that data has not been deleted..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

PCecho "select * from ckptest\g\q" > file4.sql
sql %TSTDB% < file4.sql >> %IBFOUT%

PCecho "Verification completed." >> %IBFOUT%
PCecho "Should show 5 rows in table above." >> %IBFOUT%
PCecho " " >> %IBFOUT%

PCecho "====" >> %IBFOUT%
PCecho "====" >> %IBFOUT%
PCecho "==== Testing utilities..." >> %IBFOUT%
PCecho "====" >> %IBFOUT%
PCecho "====" >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Testing sysmod..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

sysmod +w %TSTDB% >>%IBFOUT% 

PCecho " " >> %IBFOUT%
PCecho "==== Testing errhelp..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

PCecho "Check for 2 displayed error codes, (200705) and (16):" >> %IBFOUT%
%II_SYSTEM%\ingres\sig\errhelp\errhelp E_DM1001 >> %IBFOUT% 
%II_SYSTEM%\ingres\sig\errhelp\errhelp E_US0010 >> %IBFOUT%

PCecho " " >>  %IBFOUT%
PCecho "==== Testing unloaddb/unload.ing..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

unloaddb %TSTDB% >> %IBFOUT% 
call unload.bat >> %IBFOUT% 

PCecho " " >> %IBFOUT%
PCecho "==== Testing reload.ing..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

PCecho "drop table ckptest\g\q" > file5.sql
sql %TSTDB% < file5.sql >> %IBFOUT%

call reload.bat >> %IBFOUT% 

PCecho " " >> %IBFOUT%
PCecho "==== Verifying reload..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

PCecho "select * from ckptest\g\q" > file6.sql
sql %TSTDB% < file6.sql >> %IBFOUT%

PCecho "Reload verification completed." >> %IBFOUT%
PCecho "Should show 5 rows in table above." >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Testing copydb..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

copydb %TSTDB% >> %IBFOUT% 
sql %TSTDB% < copy.out >> %IBFOUT%

PCecho "drop table ckptest\g\q" > file7.sql
sql %TSTDB% < file7.sql >> %IBFOUT%

sql %TSTDB% < copy.in >> %IBFOUT% 

PCecho "select * from ckptest\g\q" > file8.sql
sql %TSTDB% < file8.sql >> %IBFOUT%

PCecho "Copydb completed." >> %IBFOUT%
PCecho "Should show 5 rows in table above." >> %IBFOUT%

PCecho " ">> %IBFOUT%
PCecho "==== Checking character sets..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

dir /W %II_SYSTEM%\ingres\files\charsets >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Testing infodb..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

infodb %TSTDB% |head >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Testing iinamu, iimonitor..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

PCecho "show" > iinameucnt.tmp
PCecho "quit" >> iinameucnt.tmp
iinamu < iinameucnt.tmp >> iinamu.tmp

grep INGRES iinamu.tmp | tail -1 | %fedcmd% '{print $4}' > ibfserver.tmp
call ipset IBFSERVER type ibfserver.tmp

PCecho "show all session" >  showallsession.tmp
REM PCecho "all"  >> showallsession.tmp
REM PCecho "session" >> showallsession.tmp
PCecho "quit" >> showallsession.tmp

iimonitor %IBFSERVER% < showallsession.tmp | head >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Testing logstat..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

logstat | head >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Testing auditdb..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

auditdb -wait iidbdb | head >> %IBFOUT%

PCecho " " >> %IBFOUT%
PCecho "==== Testing alterdb..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

alterdb -disable_journaling %TSTDB% >>%IBFOUT% 

PCecho " " >> %IBFOUT%
PCecho "==== Testing ckpdb +j +w..." >> %IBFOUT%
PCecho " " >> %IBFOUT%

ckpdb +j +w %TSTDB% >> %IBFOUT% 

PCecho " " >> %IBFOUT%
PCecho "==== Cleaning up..." >>  %IBFOUT%
PCecho " " >> %IBFOUT%

del /Q *._ingres *.tmp copy.* *.sql ckptest.* *load.bat
destroydb %TSTDB% >> %IBFOUT% >nul

PCecho " " >> %IBFOUT%
PCecho "Ingres Base Functionality Test completed." >> %IBFOUT%
PCecho " " >> %IBFOUT%

GOTO END1

:END
PCecho " " >> %IBFOUT%
PCecho "ERROR: Creation of test database %TSTDB% has failed."
PCecho "See %IBFOUT% for details."
PCecho "The Ingres installation may not be running." 
PCecho "Execute program again after starting Ingres installation."
PCdate

:END1
PCecho "Finished the Ingres Base Functionality Test"
PCdate
PCdate >> %IBFOUT%
