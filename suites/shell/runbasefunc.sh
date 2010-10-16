#!/bin/csh
#
# Ingres Base Functionality Test
#
# History:
#
# 13-Jul-2004    Created.
#
IBFOUT=$TST_OUTPUT/basefunc/basefunc.log
TSTDB=basefuncdb1
mkdir $TST_OUTPUT/basefunc >& /dev/null

date | tee $IBFOUT
echo "Ingres Base Functionality Test" | tee -a $IBFOUT
echo "Test output located in $IBFOUT"

cd $TST_OUTPUT/basefunc

echo | tee -a $IBFOUT
echo "==== Getting Ingres Version information..." | tee -a $IBFOUT
cat $II_SYSTEM/ingres/version.rel | tee -a $IBFOUT

echo | tee -a $IBFOUT
echo "==== Starting Ingres installation..." | tee -a $IBFOUT
ingstart >>$IBFOUT

destroydb $TSTDB >>$IBFOUT >& /dev/null

echo >>$IBFOUT
echo "====" >>$IBFOUT
echo "====" >>$IBFOUT
echo "==== Testing create database, checkpoint, rollforward..." >>$IBFOUT
echo "====" >>$IBFOUT
echo "====" >>$IBFOUT

echo | tee -a $IBFOUT
echo "==== Creating database $TSTDB..." | tee -a $IBFOUT
createdb $TSTDB >>$IBFOUT
if [ $? != 0 ]
then
    echo
    echo ERROR: Creation of test database $TSTDB has failed.
    echo See $IBFOUT for details.
    echo The Ingres installation may not be running. 
    echo Execute program again after starting Ingres installation.
    echo
    exit 1
fi

echo | tee -a $IBFOUT
echo "==== Creating table in $TSTDB and inserting data..." | tee -a $IBFOUT

sql $TSTDB <<END>>$IBFOUT
create table ckptest (col1 int,col2 int,col3 int,col4 int,col5 int);
insert into ckptest values (1,2,3,4,5);
insert into ckptest values (6,7,8,9,10);
insert into ckptest values (11,12,13,14,15);
insert into ckptest values (16,17,18,149,20);
insert into ckptest values (21,22,23,24,25);\p\g\q
END

echo >>$IBFOUT
echo "Table created and data inserted." >>$IBFOUT
echo "Should show 1 row X 5 above..." >>$IBFOUT
echo >>$IBFOUT

echo
echo "==== Checkpointing database $TSTDB..." | tee -a $IBFOUT
echo >>$IBFOUT
ckpdb +j +w $TSTDB >>$IBFOUT

echo
echo >>$IBFOUT
echo "==== Deleting data from table..." | tee -a $IBFOUT
echo >>$IBFOUT
sql $TSTDB <<END>>$IBFOUT
delete from ckptest;\p\g\q
END

echo >>$IBFOUT
echo "Deletion of data completed." >>$IBFOUT
echo "Should show 5 rows above." >>$IBFOUT
echo >>$IBFOUT

echo
echo "==== Rolling forward database $TSTDB (with journals)..." | tee -a $IBFOUT
echo >>$IBFOUT
rollforwarddb +j +w $TSTDB >>$IBFOUT 

echo | tee -a $IBFOUT
echo "==== Verifying that journals have deleted the data..." | tee -a $IBFOUT
echo >>$IBFOUT
sql $TSTDB <<END>>$IBFOUT
select * from ckptest;\p\g\q
END

echo "Verification completed." >>$IBFOUT 
echo "Should show (0 rows) in table above." >>$IBFOUT
echo >>$IBFOUT

echo | tee -a $IBFOUT
echo "==== Rolling forward database $TSTDB (without journals)..." | tee -a $IBFOUT
rollforwarddb -j +w $TSTDB >>$IBFOUT

echo | tee -a $IBFOUT
echo "==== Verifying that data has not been deleted..." | tee -a $IBFOUT
echo >>$IBFOUT
sql $TSTDB <<END>>$IBFOUT
select * from ckptest;\p\g\q
END

echo "Verification completed." >>$IBFOUT
echo "Should show 5 rows in table above." >>$IBFOUT
echo >>$IBFOUT

echo "====" >>$IBFOUT
echo "====" >>$IBFOUT
echo "==== Testing utilities..." >>$IBFOUT
echo "====" >>$IBFOUT
echo "====" >>$IBFOUT
echo | tee -a $IBFOUT

echo "==== Testing sysmod..." | tee -a $IBFOUT
sysmod +w $TSTDB >>$IBFOUT 

echo
echo "==== Testing errhelp..." | tee -a $IBFOUT
echo "Check for 2 displayed error codes, (200705) and (16):" >>$IBFOUT
$II_SYSTEM/ingres/sig/errhelp/errhelp E_DM1001 >>$IBFOUT 
$II_SYSTEM/ingres/sig/errhelp/errhelp E_US0010 >>$IBFOUT

echo | tee -a $IBFOUT
echo "==== Testing unloaddb/unload.ing..." | tee -a $IBFOUT

unloaddb $TSTDB >>$IBFOUT 
./unload.ing >>$IBFOUT 

echo | tee -a $IBFOUT
echo "==== Testing reload.ing..." | tee -a $IBFOUT
echo >>$IBFOUT

sql $TSTDB <<END>>$IBFOUT
drop table ckptest;\p\g\q
END

./reload.ing >>$IBFOUT 

echo | tee -a $IBFOUT
echo "==== Verifying reload..." | tee -a $IBFOUT
echo >>$IBFOUT

sql $TSTDB <<END>>$IBFOUT
select * from ckptest\p\g\q
END

echo "Reload verification completed." >>$IBFOUT
echo "Should show 5 rows in table above." >>$IBFOUT

echo | tee -a $IBFOUT
echo "==== Testing copydb..." | tee -a $IBFOUT
echo >>$IBFOUT

copydb $TSTDB >>$IBFOUT 
sql $TSTDB <copy.out >>$IBFOUT
sql $TSTDB <<END >>$IBFOUT
drop table ckptest;\p\g\q
END

sql $TSTDB <copy.in >>$IBFOUT 
sql $TSTDB <<END >>$IBFOUT
select * from ckptest\p\g\q
END

echo "Copydb completed." >>$IBFOUT
echo "Should show 5 rows in table above." >>$IBFOUT

echo | tee -a $IBFOUT
echo "==== Checking character sets..." | tee -a $IBFOUT
ls -l $II_SYSTEM/ingres/files/charsets >>$IBFOUT

echo | tee -a $IBFOUT
echo "==== Testing infodb..." | tee -a $IBFOUT
echo >>$IBFOUT
infodb $TSTDB |head >>$IBFOUT

echo | tee -a $IBFOUT
echo "==== Testing iinamu, iimonitor..." | tee -a $IBFOUT
echo >>$IBFOUT
iinamu << END >>iinamu.tmp
show
quit
END

IBFSERVER=`grep INGRES iinamu.tmp | tail -1 | awk '{print $4}'`
iimonitor $IBFSERVER <<END| head >>$IBFOUT
show all sessions
quit
END

echo | tee -a $IBFOUT
echo "==== Testing logstat..." | tee -a $IBFOUT
echo >>$IBFOUT
logstat | head >>$IBFOUT

echo | tee -a $IBFOUT
echo "==== Testing auditdb..." | tee -a $IBFOUT
echo >>$IBFOUT
auditdb -wait iidbdb | head >>$IBFOUT

echo | tee -a $IBFOUT
echo "==== Testing alterdb..." | tee -a $IBFOUT
echo >>$IBFOUT
alterdb -disable_journaling $TSTDB >>$IBFOUT 

echo | tee -a $IBFOUT
echo "==== Testing ckpdb +j +w..." | tee -a $IBFOUT
echo >>$IBFOUT
ckpdb +j +w $TSTDB >>$IBFOUT 

echo | tee -a $IBFOUT
echo "==== Cleaning up..." | tee -a $IBFOUT

rm ./*._ingres ./*.ing* ./iinamu.tmp ./copy.*
destroydb $TSTDB >>$IBFOUT >& /dev/null

echo
echo "Test output located in $IBFOUT"

echo | tee -a $IBFOUT
echo "Ingres Base Functionality Test completed." | tee -a $IBFOUT
date | tee -a $IBFOUT
