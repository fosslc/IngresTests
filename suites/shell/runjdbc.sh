#!/bin/sh
# Copyright (c) 2007 Ingres Corporation
#
#                SCRIPT FOR RUNNING LOCAL JDBC TESTS
#
#
#  Description:
#	This script is used to run the local JDBC SEP tests using
# 	listexec and excutor. 
#	
#  History:
#	28-Nov-01 (sinra04)  	Created.
#       
#	27-Jul-04 (vande02)	Hard-coded the test database here to be jdbcdb
#				and setting SEPPARAMDB=jdbcdb here locally.
#
#	18-Apr-07 (vande02)	Removed the SEPPARAMDB=jdbcdb here because
#				the test can be run locally or to a remote
#				server (vnode::jdbcdb).  Also, the test
#				environment script sets it.  Having SEPPARAMDB
#				in this script will override the other settings
#				and you won't know it is happening.
#------------------------------------------------------------------------------
#			Setup Area
#------------------------------------------------------------------------------
umask 2 
umask 

# Set the output directory for test results.
#
if [ "$TST_ROOT_OUTPUT" != "" ]
then
	TST_OUTPUT=$TST_ROOT_OUTPUT/net/jdbc

	if [ ! -d $TST_ROOT_OUTPUT/net ]
	then
	  echo "Creating Directory - $TST_ROOT_OUTPUT/net"
	  mkdir $TST_ROOT_OUTPUT/net
	fi
else 
	TST_OUTPUT=$ING_TST/output/net/jdbc

	if [ ! -d $ING_TST/output ]
        then
          echo "Creating Directory - $ING_TST/output"
          mkdir $ING_TST/output
        fi

	if [ ! -d $ING_TST/output/net ]
	then
	  echo "Creating Directory - $ING_TST/output/net"
	  mkdir $ING_TST/output/net
	fi
fi

	if [ ! -d $TST_OUTPUT ]
        then
          echo "Creating Directory - $TST_OUTPUT"
          mkdir $TST_OUTPUT
        fi

	export TST_OUTPUT

	echo "Output files will be written to $TST_OUTPUT"


work=$1

#---------------------------------------------------------------------
#      End of Setup
#---------------------------------------------------------------------


if [ "$work" = "init" ]
then
    echo "Creating JDBC test database @ ",`date`
    echo ""


    destroydb jdbcdb >>$TST_OUTPUT/jdbcinit.out
    createdb jdbcdb  >>$TST_OUTPUT/jdbcinit.out

    echo "Finished creating JDBC test database @ ",`date`
    echo ""
else
if [ "$work" = "jdbc" ]
then
    echo "Starting JDBC SEP Tests @ ",`date`
    echo ""

    executor $TST_CFG/jdbc.cfg > $TST_OUTPUT/jdbc.out

    echo "Finished with JDBC SEP Tests @ ",`date`
    echo ""

else
    echo "You must specify init or jdbc. "
    echo ""
    echo " Example: sh $TST_SHELL/runjdbc.sh init "
    echo ""
    echo "          or "
    echo ""
    echo "          sh $TST_SHELL/runjdbc.sh jdbc "
    echo ""
    exit 1
fi
fi

echo "End of NET/JDBC SEP tests @ ",`date`
