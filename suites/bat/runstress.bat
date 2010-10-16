@ECHO OFF
REM Copyright (c) 2004 Ingres Corporation
REM
REM
REM  	Ingres Acceptance QA Stress Test
REM
REM  	History:
REM 
REM	13-Jul-2004	Created.
REM
REM  	08-Oct-2004	(legru01)
REM			Convert from runstress.sh to runstress.bat.
REM			Original script was create by sarjo01.
REM
REM Note: Several data integrity checks are performed at the end
REM       of the test following rollforwarddb. When the test completes,
REM       examine the output file and verify that these data integrity
REM       checks are successful. 
REM
REM  The Stress test applications used in this script are taken from the
REM  Ingres Acceptance QA suite stress!appsuite. Further information about
REM  these programs is available in the document suites!doc!stresstest.txt.
REM  

if not exist %TST_OUTPUT%\stress\nul PCecho "Creating Directory - %TST_OUTPUT%\stress"
if not exist %TST_OUTPUT%\stress\nul mkdir %TST_OUTPUT%\stress
chdir %TST_OUTPUT%\stress

set STESTOUT=%TST_OUTPUT%\stress\stress.out

PCecho "Test output written to %STESTOUT%"
PCdate
PCdate > %STESTOUT% 
PCecho "Begin test..." 

PCecho "**" >> %STESTOUT% 
PCecho "**" >> %STESTOUT%
PCecho "**  Build executables" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
copy /Y %ING_TST%\stress\appsuite\*.sc   %TST_OUTPUT%\stress\ > nul
copy /Y %ING_TST%\stress\appsuite\*.data %TST_OUTPUT%\stress\ > nul

esqlc -multi zsum.sc >> %STESTOUT%
call sepcc  zsum >> %STESTOUT%
call seplnk zsum >> %STESTOUT%

esqlc -multi btree3.sc >> %STESTOUT%
call sepcc  btree3 >> %STESTOUT%
call seplnk btree3 >> %STESTOUT%

esqlc -multi ubtreev1.sc >> %STESTOUT%
call sepcc  ubtreev1 >> %STESTOUT%
call seplnk ubtreev1 >> %STESTOUT%

PCecho "**" >> %STESTOUT% 
PCecho "**" >> %STESTOUT%
PCecho "**  Create database" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%

destroydb stressdb >> %STESTOUT%
createdb stressdb >> %STESTOUT%

PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**  Initialize tables" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
zsum.exe stressdb init -cb -u -d >> %STESTOUT%
btree3.exe stressdb run -mi -r25000 -s  >> %STESTOUT%
ubtreev1.exe stressdb init -c -r50000 >> %STESTOUT%

PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**  Checkpoint" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
ckpdb +j +w stressdb >> %STESTOUT%

PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**  Execute program 1" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
zsum.exe stressdb run -t8 -i15000 -lr >> %STESTOUT%

PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**  Execute program 2" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
btree3.exe stressdb run -mr -h50000 -t8 -i15000 -d10 >> %STESTOUT%

PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**  Execute program 3" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
ubtreev1.exe stressdb run -h50000 -t8 -i5000 >> %STESTOUT%

PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**  Rollforward" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
rollforwarddb +w stressdb >> %STESTOUT%

PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**  Data integrity check 1" >> %STESTOUT%
PCecho "**  Verify:   " >> %STESTOUT%
PCecho "**         value1 = 1" >> %STESTOUT%
PCecho '**         value2 = $1000.00' >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "select count(distinct a1.bal + a2.bal) as value1 from zsumacct1 a1, zsumacct2 a2 where a1.accid = a2.accid; select distinct a1.bal + a2.bal as value2 from zsumacct1 a1, zsumacct2 a2 where a1.accid = a2.accid and a1.accid = 0;\g"> di1.sql
sql stressdb  < di1.sql >> %STESTOUT%

PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**  Data integrity check 2" >> %STESTOUT%
PCecho "**  Verify:   " >> %STESTOUT%
PCecho "**         value1 = value2" >> %STESTOUT%
PCecho "**         value3 = 0,0" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "select count(*) as value1 from btree3tbl1_idx; drop index btree3tbl1_idx2; create index btree3tbl1_idx2 on btree3tbl1(data1) with structure = btree, page_size=4096; select count(*) as value2 from btree3tbl1_idx2;select count(*) as value3 from btree3tbl1_idx x1 where not exists (select * from btree3tbl1_idx2 x2 where x1.tidp = x2.tidp and x1.data1 = x2.data1) union all select count(*) as value4 from btree3tbl1_idx2 x1 where not exists (select * from btree3tbl1_idx x2 where x1.tidp = x2.tidp and x1.data1 = x2.data1);\g"> di2.sql
sql stressdb < di2.sql >> %STESTOUT%

PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**  Data integrity check 3" >> %STESTOUT%
PCecho "**  Verify:   " >> %STESTOUT%
PCecho "**         value1 = 0" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "**" >> %STESTOUT%
PCecho "select count(distinct keyval) as value1 from ubtreev1tbl1 where char(left(v0,1)+left(v1,1)+left(v2,1)+left(v3,1)+left(v4,1)+left(v5,1)+left(v6,1)+left(v7,1)+left(v8,1)+left(v9,1) , 10) != 'ABCDEFGHIJ';\g"> di3.sql
sql  stressdb < di3.sql >> %STESTOUT%

PCecho "Cleaning up..."

destroydb stressdb

del /Q *.sc *.data *.c *.exe *.obj *.sql
 
PCecho "Test completed..." 
PCdate
