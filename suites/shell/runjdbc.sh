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
#
#       04-Sep-08 (boija02)     ING_TST/output is unlikely to work, so changed
#                               to II_SYSTEM. Don't create SEPPARAMDB if it's
#                               defined, which is likely to mean remote testing
#	10-Mar-09 (boija02)	Correcting typo in conditional.
#
#
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
	TST_OUTPUT=$II_SYSTEM/output/net/jdbc

	if [ ! -d $II_SYSTEM/output ]
        then
          echo "Creating Directory - $II_SYSTEM/output"
          mkdir $II_SYSTEM/output
        fi

	if [ ! -d $II_SYSTEM/output/net ]
	then
	  echo "Creating Directory - $II_SYSTEM/output/net"
	  mkdir $II_SYSTEM/output/net
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
  if [ "$SEPPARAMDB" = "" ]
  then
    echo "Creating JDBC test database @ ",`date`
    echo ""


    destroydb jdbcdb >>$TST_OUTPUT/jdbcinit.out
    createdb jdbcdb  >>$TST_OUTPUT/jdbcinit.out

    echo "Finished creating JDBC test database @ ",`date`
    echo ""
  else
    echo "SEPPARAMDB defined as "$SEPPARAMDB
    echo "Assuming this db exists and is unicode-enabled."
    echo
  fi
else
if [ "$work" = "jdbc" ]
then
    echo "Starting JDBC SEP Tests @ ",`date`
    echo ""

  if [ "$SEPPARAMDB" = "" ]
  then
    SEPPARAMDB=jdbcdb
    export SEPPARAMDB
    echo "Using local jdbcdb as testing database"
  else
    echo "Using $SEPPARAMDB as testing database"
  fi

    executor $TST_CFG/jdbc.cfg > $TST_OUTPUT/jdbc.out

    echo "Finished with JDBC SEP Tests @ ",`date`
    echo ""

else
    echo "You must specify init or jdbc. "
    echo "Note, init just creates local jdbcdb, which isn't necessary"
    echo "when testing over net."
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
