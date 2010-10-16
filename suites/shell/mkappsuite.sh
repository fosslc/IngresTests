#!/bin/sh
#
#  Copyright (c) 2009 Ingres Corporation. All Rights Reserved.
#
#
#  Appsuite Stress Test executable build script 
#
#  13-May-2008 (sarjo01) Created. 
#  01-Oct-2008 (sarjo01) Added dbpv1. 
#  06-Jan-2010 (sarjo01) Added zsum. 
#

#
# Function: Build executable
# 
makeit() {
    cp $ING_TST/stress/appsuite/$appname.sc .
    chmod +w ./$appname.sc
    echo $appname
    esqlc -multi $appname
    sepcc $appname
    seplnk $appname
    if [ ! -f ./$appname.exe ]
    then
        echo "ERROR: Failed to build $appname.exe"
        exit 1 
    fi
}
#
# Function: Display command syntax
#
errorHelp() {
    echo ""
    echo "Usage:"
    echo ""
    echo "  sh \$TST_SHELL/mkappsuite.sh all"
    echo "     or"
    echo "  sh \$TST_SHELL/mkappsuite.sh test [ test test ... ]"
    echo "     where test is any of"
    echo "           dbpv1 ddlv1 insdel ordent qp1 qp3 selv1 updv1 zsum"
    echo ""
    exit 1
}

appsuitelist="dbpv1 ddlv1 insdel ordent qp1 qp3 selv1 updv1 zsum"
dolist=
appname=

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

for appname in $dolist
do
    export appname 
    makeit
    rm -f ./$appname.sc ./$appname.c ./$appname.o
    if [ "$1" = "all" ]
    then
        mv $appname.exe $TST_TOOLS
    fi
done
