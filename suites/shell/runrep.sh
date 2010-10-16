#!/bin/sh
# Copyright (c) 2007 Ingres Corporation
#
#                SCRIPT FOR RUNNING THE REPLICATOR TESTS
#
#
# Description:
#	This script is used to setup and run the Replicator SEP tests using
# listexec and excutor. 
#
#  History:
#	06-May-1997 (li$to01)
#
#	 This script can be run as follows:
#	   a) To initialize all databases:
#
#	      sh $TST_SHELL/runrep.sh init all
#
#	   b) To initialize selected databases, choose the modules 
#		      shown between the pipes:
#		   
#	      sh $TST_SHELL/runrep.sh init backup|benign|branch|cascade|
#					   datatypes|dbtypes|errors|hubspoke|
#					   maintain|peer|repmgr|storage|uflag
#
#	   c) To run all Replicator test suites:
#
#	      sh $TST_SHELL/runrep.sh rep all
#
#	   d) To run selected test modules, choose the modules shown
#	      between the pipes:
#
#	      sh $TST_SHELL/runrep.sh rep backup|benign|branch|cascade|
#					  datatypes|dbtypes|errors|hubspoke|
#					  maintain|peer|repmgr|storage|uflag
#	02-Mar-1998 (vande02)
#		Created based on QA's runrep.sh for porting handoff_qa
#	04-Jun-1998 (vande02)
#		Added Clean Up Area to destroy all or some testing databases
#	06-aug-1998 (walro03)
#		Stop replicator servers before destroydb.
#	30-oct-1998 (walro03)
#		Add back the following sub-suites from the reptest20 code line:
#		benign, branch, datatypes, dbtypes, errors, hubspoke, maintain, 
#		peer, repmgr, storage
#       04-Jun-1999 (vande02
#               Added calls to qawtl for messages to be written to errlog.log
#       11-jan-2000 (vande02)
#           Checking the return code for each database initialization step, so
#           if the createdb fails, the script will echo a message and exit.
#	03-Jul-2007 (vande02)
#		Added SEP_DIFF_SLEEP and SEP_CMD_SLEEP syntax to be set to
#		default of 250 so the SEP commands can complete before the
#		next one is executed.
#	07-Nov-2008 (vande02)
#		Added SEPPARAM_CHARSET to enable tests to run with UTF8.
#	11-Dec-2008 (wanfr01)
#		SIR 121355
#               Allow SEPPARAM_SLEEP and SEPPARAM_SLEEP_2 to be set by shell
#               and change default to 5 seconds
#---------------------------------------------------------------------------
#			Setup Area
#---------------------------------------------------------------------------
umask 2 
umask 
# Set a variable that can be used to check the character set
#
ii_code=`ingprenv II_INSTALLATION`
SEPPARAM_CHARSET=`ingprenv II_CHARSET$ii_code`
export SEPPARAM_CHARSET

# Set the output directory for test results.
#
if [ "$TST_ROOT_OUTPUT" != "" ]
then
	TST_OUTPUT=$TST_ROOT_OUTPUT/rep
else 
	TST_OUTPUT=$TST_OUTPUT/rep

fi
	if [ ! -d $TST_OUTPUT ]
	then
	  echo "Creating Directory - $TST_OUTPUT"
	  mkdir $TST_OUTPUT
	fi
	export TST_OUTPUT

	echo "Output files will be written to $TST_OUTPUT"
#
if [ ! -d $TST_OUTPUT ]
then
  echo "Creating Directory - $TST_OUTPUT"
  mkdir $TST_OUTPUT
fi
     
# Initialize variable input to shell script

work=$1
shift

# Check for init or clean parameter

if [ "$1" = "init" ]
then 
	work=$1
	shift
fi

if [ "$1" = "clean" ]
then 
	work=$1
	shift
fi
#---------------------------------------------------------------------------
#			End of Setup
#---------------------------------------------------------------------------
#---------------------------------------------------------------------------
#			Initialization Area
#---------------------------------------------------------------------------
#
# Start the initialization of Replicator Databases
#
if [ "$work" = "init" ]
then
       	if [ "$*" = "" ]
	then
	     echo "You must input the modules to be initalized "
	     echo "or specify ""all""."
	     echo ""
	     echo " Example: sh $TST_SHELL/runrep.sh init all "
	     echo ""
	     echo "          or "
	     echo ""
	     echo "          sh $TST_SHELL/runrep.sh init backup|branch"
	     echo ""

	     exit 1
        else
		echo " Creating REP/Tests databases @ ", `date`
			echo raise dbevent dd_stop_server\\g | sql repdb1 >> $TST_OUTPUT/repinit.out
			echo raise dbevent dd_stop_server\\g | sql repdb2 >> $TST_OUTPUT/repinit.out
			echo raise dbevent dd_stop_server\\g | sql repdb3 >> $TST_OUTPUT/repinit.out
			echo raise dbevent dd_stop_server\\g | sql repdb4 >> $TST_OUTPUT/repinit.out
			echo raise dbevent dd_stop_server\\g | sql repdb5 >> $TST_OUTPUT/repinit.out
			sleep 10
                        destroydb repdb1 >>$TST_OUTPUT/repinit.out
                        createdb repdb1 >>$TST_OUTPUT/repinit.out

                        if [ $? != 0 ]
                        then
                          echo Creation of Replicator database repdb1 Failed.
                          echo See $TST_OUTPUT/repinit.out for error messages.
                          exit 1
                        fi

                        destroydb repdb2 >>$TST_OUTPUT/repinit.out
                        createdb repdb2 >>$TST_OUTPUT/repinit.out

                        if [ $? != 0 ]
                        then
                          echo Creation of Replicator database repdb2 Failed.
                          echo See $TST_OUTPUT/repinit.out for error messages.
                          exit 1
                        fi

                        destroydb repdb3 >>$TST_OUTPUT/repinit.out
                        createdb repdb3 >>$TST_OUTPUT/repinit.out

                        if [ $? != 0 ]
                        then
                          echo Creation of Replicator database repdb3 Failed.
                          echo See $TST_OUTPUT/repinit.out for error messages.
                          exit 1
                        fi

                        destroydb repdb4 >>$TST_OUTPUT/repinit.out
                        createdb repdb4 >>$TST_OUTPUT/repinit.out

                        if [ $? != 0 ]
                        then
                          echo Creation of Replicator database repdb4 Failed.
                          echo See $TST_OUTPUT/repinit.out for error messages.
                          exit 1
                        fi

                        destroydb repdb5 >>$TST_OUTPUT/repinit.out
                        createdb repdb5 >>$TST_OUTPUT/repinit.out

                        if [ $? != 0 ]
                        then
                          echo Creation of Replicator database repdb5 Failed.
                          echo See $TST_OUTPUT/repinit.out for error messages.
                          exit 1
                        fi

	     for fac in $*
	     do
#
#		Initialize the BACKUP tests
#
		if [ "$fac" = "all" -o "$fac" = "backup" ]
		then 
			if [ ! -d $TST_OUTPUT/backup ]
			then
			  echo "Creating Directory - $TST_OUTPUT/backup"
			  mkdir $TST_OUTPUT/backup
		        fi
		fi
#
#		Initialize the BENIGN tests
#
		if [ "$fac" = "all" -o "$fac" = "benign" ]
		then
			if [ ! -d $TST_OUTPUT/benign ]
			then
			  echo "Creating Directory - $TST_OUTPUT/benign"
         		  mkdir $TST_OUTPUT/benign
			fi
		fi
#
#		Initialize the BRANCH tests
#
		if [ "$fac" = "all" -o "$fac" = "branch" ]
	        then
			if [ ! -d $TST_OUTPUT/branch ]
			then
		  	  echo "Creating Directory - $TST_OUTPUT/branch"
	          	  mkdir $TST_OUTPUT/branch
	        	fi
		fi
#
#		Initialize the CASCADE tests
#
		if [ "$fac" = "all" -o "$fac" = "cascade" ]
		then
			if [ ! -d $TST_OUTPUT/cascade ]
			then
			  echo "Creating Directory - $TST_OUTPUT/cascade"
   		          mkdir $TST_OUTPUT/cascade
		        fi
		fi
#
#		Initialize the DATATYPES tests
#
		if [ "$fac" = "all" -o "$fac" = "datatypes" ]
		then
			if [ ! -d $TST_OUTPUT/datatypes ]
			then
                          echo "Creating Directory - $TST_OUTPUT/datatypes"
			  mkdir $TST_OUTPUT/datatypes
			fi
		fi
#
#		Initialize the DBTYPES tests
#
		if [ "$fac" = "all" -o "$fac" = "dbtypes" ]
		then
			if [ ! -d $TST_OUTPUT/dbtypes ]
			then
                          echo "Creating Directory - $TST_OUTPUT/dbtypes"
			  mkdir $TST_OUTPUT/dbtypes
			fi
		fi
#
#		Initialize the ERROR MODE tests
#
		if [ "$fac" = "all" -o "$fac" = "errors" ]
		then
			if [ ! -d $TST_OUTPUT/errors ]
			then
                          echo "Creating Directory - $TST_OUTPUT/errors"
			  mkdir $TST_OUTPUT/errors
			fi
		fi
#
#		Initialize the HUBSPOKE tests
#
		if [ "$fac" = "all" -o "$fac" = "hubspoke" ]
		then
			if [ ! -d $TST_OUTPUT/hubspoke ]
			then
			  echo "Creating Directory - $TST_OUTPUT/hubspoke"
   		          mkdir $TST_OUTPUT/hubspoke
			fi
		fi
#
#		Initialize the MAINTENANCE tests
#
                if [ "$fac" = "all" -o "$fac" = "maintain" ]
                then
                        if [ ! -d $TST_OUTPUT/maintain ]
                        then
                          echo "Creating Directory - $TST_OUTPUT/maintain"
                          mkdir $TST_OUTPUT/maintain
                        fi
                fi
#
#		Initialize the PEER TO PEER tests
#
                if [ "$fac" = "all" -o "$fac" = "peer" ]
                then
                        if [ ! -d $TST_OUTPUT/peer ]
                        then
                          echo "Creating Directory - $TST_OUTPUT/peer"
                          mkdir $TST_OUTPUT/peer
                        fi
                fi
#
#		Initialize the REPMGR tests
#
		if [ "$fac" = "all" -o "$fac" = "repmgr" ]
		then
			if [ ! -d $TST_OUTPUT/repmgr ]
			then
			  echo "Creating Directory - $TST_OUTPUT/repmgr"
  		          mkdir $TST_OUTPUT/repmgr
			fi
		fi
#
#		Initialize the STORAGE tests
#
		if [ "$fac" = "all" -o "$fac" = "storage" ]
		then
			if [ ! -d $TST_OUTPUT/storage ]
			then
			  echo "Creating Directory - $TST_OUTPUT/storage"
			  mkdir $TST_OUTPUT/storage
			fi
		fi
#
	    done
        fi
#
	echo Replicator databases are created and ready for testing.
#

fi

#---------------------------------------------------------------------------
#			End of Initialization
#---------------------------------------------------------------------------

#---------------------------------------------------------------------------
#			Main Work Area
#---------------------------------------------------------------------------
#
# Run the Replicator Sep Test Modules.
#
if [ "$work" = "rep" ]
then
	SEPPARAMDB_1=repdb1
	SEPPARAMDB_2=repdb2
	SEPPARAMDB_3=repdb3
	SEPPARAMDB_4=repdb4
	SEPPARAMDB_5=repdb5
	SEPPARAMDB_6=repdb6
	SEPPARAMDB_7=repdb7
	if [ "$SEPPARAM_SLEEP" = "" ]
	then
	    SEPPARAM_SLEEP=5
	fi
	if [ "$SEPPARAM_SLEEP_2" = "" ]
	then
	    SEPPARAM_SLEEP_2=5
	fi
	SEPPARAMDRIVERDB_1=repdb1
	SEPPARAMDRIVERDB_2=repdb2
	SEPPARAMDRIVERDB_3=repdb3
	SEPPARAMDRIVERDB_4=repdb4
	SEPPARAMDRIVERDB_5=repdb5
	SEP_CMD_SLEEP=250
	SEP_DIFF_SLEEP=250
	export SEPPARAMDB_1
	export SEPPARAMDB_2
	export SEPPARAMDB_3
	export SEPPARAMDB_4
	export SEPPARAMDB_5
	export SEPPARAM_SLEEP
	export SEPPARAM_SLEEP_2
	export SEPPARAMDRIVERDB_1
	export SEPPARAMDRIVERDB_2
	export SEPPARAMDRIVERDB_3
	export SEPPARAMDRIVERDB_4
	export SEPPARAMDRIVERDB_5
	export SEP_CMD_SLEEP
	export SEP_DIFF_SLEEP
#
 	for fac in $*
	do 
		if [ "$fac" = "all" -o "$fac" = "backup" ]
		then
#
#		Run the BACKUP tests
#
		echo "Running the Central-to-Backup tests @ ", `date`
		echo ""
		qawtl RUNNING BACKUP REPLICATOR TESTS
			executor -d50 -t10 $TST_CFG/repbackup.cfg >$TST_OUTPUT/backup/repbackup.out
		qawtl END BACKUP REPLICATOR TESTS
		fi
#
		if [ "$fac" = "all" -o "$fac" = "benign" ]
		then
#
#		Run the BENIGN tests
#
		echo "Running the BENIGN tests @ ", `date` 
		echo ""
		qawtl RUNNING BENIGN REPLICATOR TESTS
			executor -d50 -t10 $TST_CFG/repbenign.cfg >$TST_OUTPUT/benign/repbenign.out
		qawtl END BENIGN REPLICATOR TESTS
		fi
#
		if [ "$fac" = "all" -o "$fac" = "branch" ]
		then
#
#		Run the BRANCH tests
#
		echo "Running the Central-to-Branch tests @ ", `date` 
		echo ""
		qawtl RUNNING BRANCH REPLICATOR TESTS
			executor -d50 -t10 $TST_CFG/repbranch.cfg >$TST_OUTPUT/branch/repbranch.out
		qawtl END BRANCH REPLICATOR TESTS
		fi
		if [ "$fac" = "all" -o "$fac" = "cascade" ]
		then
#
#		Run the CASCADE tests
#
		echo "Running the CASCADE tests @ ", `date` 
		echo ""
		qawtl RUNNING CASCADE REPLICATOR TESTS
			executor -d50 -t10 $TST_CFG/repcascade.cfg >$TST_OUTPUT/cascade/repcascade.out
		qawtl END CASCADE REPLICATOR TESTS
		fi
#
                if [ "$fac" = "all" -o "$fac" = "datatypes" ]
                then

#		Run the DATATYPES tests
#
                echo "Running the DATATYPES tests @ ", `date`
                echo ""
		qawtl RUNNING DATATYPES REPLICATOR TESTS
                        executor -d50 -t10 $TST_CFG/repdata.cfg >$TST_OUTPUT/datatypes/repdata.out
		qawtl END DATATYPES REPLICATOR TESTS
                fi
#
		if [ "$fac" = "all" -o "$fac" = "dbtypes" ]
		then
#
#		Run the DBTYPES tests
#
		echo "Running the DBTYPES tests @ ", `date`
		echo ""
		qawtl RUNNING DBTYPES REPLICATOR TESTS
			executor -d50 -t10 $TST_CFG/repdbtypes.cfg >$TST_OUTPUT/dbtypes/repdbtypes.out
		qawtl END DBTYPES REPLICATOR TESTS
		fi
#
		if [ "$fac" = "all" -o "$fac" =  "errors" ]
		then
#
#		Run the ERROR MODE tests
#
		echo "Running the ERROR MODE tests @ ", `date`
		echo ""
		qawtl RUNNING ERROR REPLICATOR TESTS
			executor -d50 -t10 $TST_CFG/reperror.cfg >$TST_OUTPUT/errors/reperror.out
		qawtl END ERROR REPLICATOR TESTS
		fi
#
		if [ "$fac" = "all" -o "$fac" = "hubspoke" ]
		then
#
#		Run the HUBSPOKE tests
#
		echo "Running the HUBSPOKE tests @ ", `date` 
		echo ""
		qawtl RUNNING HUBSPOKE REPLICATOR TESTS
			executor -d50 -t10 $TST_CFG/rephubspoke.cfg >$TST_OUTPUT/hubspoke/rephubspoke.out
		qawtl END HUBSPOKE REPLICATOR TESTS
		fi
#
		if [ "$fac" = "all" -o "$fac" = "maintain" ]
		then
#
#		Run the MAINTENANCE tests
#
		echo "Running the MAINTENANCE tests @ ", `date`
		echo ""
		qawtl RUNNING MAINTENANCE REPLICATOR TESTS
			executor -d50 -t10 $TST_CFG/repmaintain.cfg >$TST_OUTPUT/maintain/repmaintain.out
		qawtl END MAINTENANCE REPLICATOR TESTS
		fi
#
		if [ "$fac" = "all" -o "$fac" = "peer" ]
		then
#
#		Run the PEER TO PEER tests
#
		echo "Running the Peer to Peer tests @ ", `date`
		echo ""
		qawtl RUNNING PEER TO PEER REPLICATOR TESTS
        		executor -d50 -t10 $TST_CFG/reppeer.cfg >$TST_OUTPUT/peer/reppeer.out
		qawtl END PEER TO PEER REPLICATOR TESTS
		fi
#
		if [ "$fac" = "all" -o "$fac" = "repmgr" ]
                then
#
#		Run the REPMGR tests
#
                echo "Running the REPMGR tests @ ", `date`
                echo ""
		qawtl RUNNING REPMGR REPLICATOR TESTS
                        executor -d50 -t10 $TST_CFG/reprepmgr.cfg >$TST_OUTPUT/repmgr/reprepmgr.out
		qawtl END REPMGR REPLICATOR TESTS
                fi
#
                if [ "$fac" = "all" -o "$fac" = "storage" ]
                then
#
#		Run the STORAGE tests
#
                echo "Running the STORAGE tests @ ", `date`
                echo ""
		qawtl RUNNING STORAGE REPLICATOR TESTS
                        executor -d50 -t10 $TST_CFG/repstorage.cfg >$TST_OUTPUT/storage/repstorage.out
		qawtl END STORAGE REPLICATOR TESTS
                fi
#
	done
#
	echo "Replicator tests are complete at ", `date` 
#
fi
#---------------------------------------------------------------------------
#			Clean up Area
#---------------------------------------------------------------------------
#
# Start the clean up of Replicator Databases
#
if [ "$work" = "clean" ]
then
       	if [ "$*" = "" ]
	then
	     echo "You must input the modules to be destroyed "
	     echo "or specify ""all""."
	     echo ""
	     echo " Example: sh $TST_SHELL/runrep.sh clean all "
	     echo ""
	     echo "          or "
	     echo ""
	     echo "          sh $TST_SHELL/runrep.sh clean backup|branch"
	     echo ""

	     exit 1
        else
		echo " Destroying REP/Tests databases @ ", `date`
			echo raise dbevent dd_stop_server\\g | sql repdb1 >> $TST_OUTPUT/repinit.out
			echo raise dbevent dd_stop_server\\g | sql repdb2 >> $TST_OUTPUT/repinit.out
			echo raise dbevent dd_stop_server\\g | sql repdb3 >> $TST_OUTPUT/repinit.out
			echo raise dbevent dd_stop_server\\g | sql repdb4 >> $TST_OUTPUT/repinit.out
			echo raise dbevent dd_stop_server\\g | sql repdb5 >> $TST_OUTPUT/repinit.out
			sleep 10
                        destroydb repdb1 >>$TST_OUTPUT/repclean.out
                        destroydb repdb2 >>$TST_OUTPUT/repclean.out
                        destroydb repdb3 >>$TST_OUTPUT/repclean.out
                        destroydb repdb4 >>$TST_OUTPUT/repclean.out
                        destroydb repdb5 >>$TST_OUTPUT/repclean.out


        fi

		echo Replicator databases are destroyed for clean up

fi

#---------------------------------------------------------------------------
#			End of Initialization
#---------------------------------------------------------------------------
