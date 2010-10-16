#!/bin/sh
#
#  Copyright (c) 2009 Ingres Corporation.
#
#
#  Appsuite Stress Test Execution Script
#
#  09-Feb-2006 (sarjo01) Created. 
#  02-Jun-2006 (sarjo01) Modified to use mkappsuite.sh; added data file
#                        path option to qp1, qp3; lowered iteration counts
#                        to reduce overall execution time.
#  24-Jul-2009 (sarjo01) Added -p32 to ordent init (finds more bugs). 
#  01-Oct-2009 (sarjo01) Changed ordent iso level to default (serializable); 
#                        Added new test dbpv1.
#

#
# Function: Display command syntax
#
errorHelp() {
    echo ""
    echo "Usage:"
    echo ""
    echo "  sh \$TST_SHELL/runappsuite.sh all"
    echo "     or"
    echo "  sh \$TST_SHELL/runappsuite.sh test [ test test ... ]"
    echo "     where test is any of"
    echo "           dbpv1 ddlv1 insdel ordent qp1 qp3 selv1 updv1"
    echo ""
    exit 1
}

appsuitelist="dbpv1 ddlv1 insdel ordent qp1 qp3 selv1 updv1"
dolist=
appname=
appsuitedb=

if [ "$1" = "" ]
then
    errorHelp
fi
if [ "$1" = "all" ]
then
    dolist=$appsuitelist
else
    dolist=$*
fi

umask 2 

#
# Set up output directory
#
if [ "$TST_ROOT_OUTPUT" != "" ]
then
    outputdir=$TST_ROOT_OUTPUT/appsuite
else 
    echo "ERROR: TST_ROOT_OUTPUT not set"
    exit 1 
fi
export outputdir 
if [ ! -d $outputdir ]
then
    echo "Creating output directory $outputdir"
    mkdir $outputdir
fi
if [ ! -d $outputdir ]
then
    echo "ERROR: Could not create output directory $outputdir"
    exit 1
fi

echo ""
echo "Output directory: $outputdir"
cd $outputdir
echo `date`, "Begin Appsuite tests" > $outputdir/appsuite.out

if [ "$SEPPARAMDB" != "" ]
then
    appsuitedb=$SEPPARAMDB 
else 
    appsuitedb="appsuitedb"
fi
export appsuitedb 
destroydb $appsuitedb >> $outputdir/appsuite.out
createdb $appsuitedb >> $outputdir/appsuite.out

echo `date`," Building executables..."
sh $TST_SHELL/mkappsuite.sh all >> $outputdir/appsuite.out

for appname in $dolist
do
    case $appname in
                  dbpv1|ddlv1|insdel|ordent|qp1|qp3|selv1|updv1)
                      ;;
                  *)
                      errorHelp
                      ;;
    esac
    export appname 
    echo ""
    echo `date`," Executing $appname..."
    echo `date`," Executing $appname..." >> $outputdir/appsuite.out
    qawtl APPSUITE: Begin $appname... 
    case $appname in
         dbpv1)
             dbpv1.exe $appsuitedb init -n10000 -p1 > ./$appname.out
             dbpv1.exe $appsuitedb run -t16 -v0 -i10000 -x100 >> ./$appname.out
             ;;
         ddlv1)
             ddlv1.exe $appsuitedb init > ./$appname.out
             ddlv1.exe $appsuitedb run -t16 -v0 -i5000 -s250 >> ./$appname.out
             ;;
         insdel)
             insdel.exe $appsuitedb init -p32 > ./$appname.out
             insdel.exe $appsuitedb run -t24 -v0 -i50000 -d10 -b5 -h50000 >> ./$appname.out
             ;;
         ordent)
             ordent.exe $appsuitedb init -d -p32 > ./$appname.out
             ordent.exe $appsuitedb run -t24 -v0 -i50000 -w1 >> ./$appname.out
             ;;
         qp1)
             qp1.exe $appsuitedb init -d$ING_TST/stress/appsuite/ > ./$appname.out
             optimizedb -zk $appsuitedb
             qp1.exe $appsuitedb run -t10 -v0 -i250 -p >> ./$appname.out
             ;;
         qp3)
             qp3.exe $appsuitedb init -d$ING_TST/stress/appsuite/ > ./$appname.out
             optimizedb -zk $appsuitedb
             qp3.exe $appsuitedb run -t4 -v0 -i15 -p >> ./$appname.out
             ;;
         selv1)
             selv1.exe $appsuitedb init -r50000 -p32 -o500 > ./$appname.out
             selv1.exe $appsuitedb run -t24 -v0 -i100000 -z >> ./$appname.out
             ;;
         updv1)
             updv1.exe $appsuitedb init -p10 -c > ./$appname.out
             updv1.exe $appsuitedb run -t24 -v0 -i20000 -b5 >> ./$appname.out
             ;;
         *)
              echo "$appname...?"
             ;;
    esac
    $appname.exe $appsuitedb cleanup >> ./$appname.out

done
echo ""
echo `date`," Done"
echo `date`," Done" >> $outputdir/appsuite.out
