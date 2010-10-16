#!/bin/sh
#                SCRIPT FOR RUNNING LOCAL ODBC TESTS
#
#
#  Description:
#	This script is used to run the local ODBC SEP tests using
# 	listexec and excutor. 
#	
#  History:
#	28-Nov-01 (sarjo01)
#          Created.
#	29-Jan-02 (sarjo01)
#          Fixed bad '\' chars, changed to '/'
#	09-Oct-2002 (somsa01)
#	   Added "clean" to shell script.
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
	TST_OUTPUT=$TST_ROOT_OUTPUT/net/odbc

	if [ ! -d $TST_ROOT_OUTPUT/net ]
	then
	  echo "Creating Directory - $TST_ROOT_OUTPUT/net"
	  mkdir $TST_ROOT_OUTPUT/net
	fi
else 
	TST_OUTPUT=$ING_TST/output/net/odbc

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
if [ "$SEPPARAM_NODE" = "" ]
then
    SEPPARAM_NODE="odbcdb"
fi
export SEPPARAM_NODE

if [ "$work" = "init" ]
then
    echo "Creating $SEPPARAM_NODE database @ ",`date`
    echo ""

    destroydb $SEPPARAM_NODE >>$TST_OUTPUT/odbcinit.out
    createdb $SEPPARAM_NODE >>$TST_OUTPUT/odbcinit.out

    echo "Finished creating $SEPPARAM_NODE database @ ",`date`
    echo ""
else
if [ "$work" = "odbc" ]
then
    echo "Starting ODBC SEP Tests @ ",`date`
    echo ""

    executor $TST_CFG/odbc.cfg > $TST_OUTPUT/odbc.out

    echo "Finished with ODBC SEP Tests @ ",`date`
    echo ""
else
if [ "$work" = "clean" ]
then
    echo "Destroying $SEPPARAM_NODE database @ ",`date`
    echo ""

    destroydb $SEPPARAM_NODE >>$TST_OUTPUT/odbcclean.out

    echo "Finished Destroying $SEPPARAM_NODE database @ ",`date`
    echo ""

else
    echo "You must specify init or odbc. "
    echo ""
    echo " Example: sh $TST_SHELL/runodbc.sh init "
    echo ""
    echo "          or "
    echo ""
    echo "          sh $TST_SHELL/runodbc.sh odbc "
    echo ""
    exit 1
fi
fi
fi

echo "End of NET/SEP tests @ ",`date`
