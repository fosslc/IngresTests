#!/usr/bin/csh
#
# Ingres Acceptance QA Stress Test
#
# History:
#
# 13-Jul-2004     Created.
#
# Note: Several data integrity checks are performed at the end
#       of the test following rollforwarddb. When the test completes,
#       examine the output file and verify that these data integrity
#       checks are successful. 
#
# The Stress test applications used in this script are taken from the
# Ingres Acceptance QA suite stress!appsuite. Further information about
# these programs is available in the document suites!doc!stresstest.txt.
#  
mkdir $TST_OUTPUT/stress >& /dev/null
cd $TST_OUTPUT/stress
STESTOUT=$TST_OUTPUT/stress/stress.out
echo Test output written to $STESTOUT

date
date > $STESTOUT 
echo Begin test... 

echo "**" >> $STESTOUT 
echo "**" >>  $STESTOUT
echo "**  Build executables" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
esqlc -multi -f./zsum.c $ING_TST/stress/appsuite/zsum.sc >> $STESTOUT
sepcc  zsum >> $STESTOUT
seplnk zsum >> $STESTOUT
esqlc -multi -f./btree3.c $ING_TST/stress/appsuite/btree3.sc >> $STESTOUT
sepcc  btree3 >> $STESTOUT
seplnk btree3 >> $STESTOUT
esqlc -multi -f./ubtreev1.c $ING_TST/stress/appsuite/ubtreev1.sc >> $STESTOUT
sepcc  ubtreev1 >> $STESTOUT
seplnk ubtreev1 >> $STESTOUT

echo "**" >> $STESTOUT 
echo "**" >> $STESTOUT
echo "**  Create database" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
destroydb stressdb >& /dev/null
createdb stressdb >> $STESTOUT

echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**  Initialize tables" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
./zsum.exe stressdb init -cb -u >> $STESTOUT
./btree3.exe stressdb run -mi -r25000 -s  >> $STESTOUT
./ubtreev1.exe stressdb init -c -r50000 >> $STESTOUT

echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**  Checkpoint" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
ckpdb +j +w stressdb >> $STESTOUT

echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**  Execute program 1" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
./zsum.exe stressdb run -t8 -i15000 -lr >> $STESTOUT

echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**  Execute program 2" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
./btree3.exe stressdb run -mr -h50000 -t8 -i15000 -d10 >> $STESTOUT

echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**  Execute program 3" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
./ubtreev1.exe stressdb run -h50000 -t8 -i5000 >> $STESTOUT

echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**  Rollforward" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
rollforwarddb +w stressdb >> $STESTOUT

echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**  Data integrity check 1" >> $STESTOUT
echo "**  Verify:   " >> $STESTOUT
echo "**         value1 = 1" >> $STESTOUT
echo '**         value2 = $1000.00' >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "select count(distinct a1.bal + a2.bal) as value1 from zsumacct1 a1, zsumacct2 a2 where a1.accid = a2.accid; select distinct a1.bal + a2.bal as value2 from zsumacct1 a1, zsumacct2 a2 where a1.accid = a2.accid and a1.accid = 0;\\g" | sql -s stressdb >> $STESTOUT

echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**  Data integrity check 2" >> $STESTOUT
echo "**  Verify:   " >> $STESTOUT
echo "**         value1 = value2" >> $STESTOUT
echo "**         value3 = 0,0" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "select count(*) as value1 from btree3tbl1_idx; drop index btree3tbl1_idx2; create index btree3tbl1_idx2 on btree3tbl1(data1) with structure = btree, page_size=4096; select count(*) as value2 from btree3tbl1_idx2;select count(*) as value3 from btree3tbl1_idx x1 where not exists (select * from btree3tbl1_idx2 x2 where x1.tidp = x2.tidp and x1.data1 = x2.data1) union all select count(*) as value4 from btree3tbl1_idx2 x1 where not exists (select * from btree3tbl1_idx x2 where x1.tidp = x2.tidp and x1.data1 = x2.data1);\\g" | sql -s stressdb >> $STESTOUT

echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**  Data integrity check 3" >> $STESTOUT
echo "**  Verify:   " >> $STESTOUT
echo "**         value1 = 0" >> $STESTOUT
echo "**" >> $STESTOUT
echo "**" >> $STESTOUT
echo "select count(distinct keyval) as value1 from ubtreev1tbl1 where char(left(v0,1)+left(v1,1)+left(v2,1)+left(v3,1)+left(v4,1)+left(v5,1)+left(v6,1)+left(v7,1)+left(v8,1)+left(v9,1) , 10) <> 'ABCDEFGHIJ';\\g" | sql -s stressdb >> $STESTOUT

echo Cleaning up...
rm ./zsum.* ./btree3.* ./ubtreev1.* >& /dev/null
destroydb stressdb >& /dev/null
echo Test completed. 
date
date >> $STESTOUT
