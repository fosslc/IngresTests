#!/bin/sh
#                SCRIPT FOR RUNNING STAR PHASE1 TESTS
#
#
# Description:
#	This script is used to run the STAR SEP tests using
# 	listexec and excutor. 
#	
#	Flags are: -create (create and fill ldb/ddb databases for star)
#
#  History:
#	7-29-92 (barbh)
#		Created to run the STAR tests automatically.
#
#	13-feb-93 (barbh)
#		Added new TST_OUTPUT logic and put the script in same format
#		as all other shell scripts used in testing.
#
#               Script will be run as follows for initialization:
#
#               sh $TST_SHELL/runstar.sh init all
#
#               or
#
#               sh $TST_SHELL/runstar.sh init <list of init modules>
#
#		Script will run as follows for the test modules:
#
#               sh $TST_SHELL/runstar.sh star all
#
#               or
#
#               sh $TST_SHELL/runstar.sh star <list of star modules>
#
#	07-apr-93 (barbh)
#		Added logic to send error message if initialization is not
#		requested correctly.
#	29-apr-93 (barbh)
#		A space is required between the " and the ] for the if
#		command.   if [ "$fac" =  "all" -o "$fac" = "ldbs" ]
#		Also change ref. to $ING_TST/listexec/ug to $TST_CFG to make
#               script more portable for 6.5.
#
#	15-sep-93 (barbh)
#		Added new modules for testing aggregates,predicates,dbprocs,
#		subsel clauses. 
#		Changed STAR_INIT to TST_INIT to be consistent with the 
#		rest of the test kit.
#		Added new logic that will create and copy large tables into
#		the first 2 ldb's that are created.
#		The star modules are as follows:
#		
#		| qryproc | ddl | tpf | aggs | pred | subs | dbproc |
#
#
#	20-jan-94 (garys)
#		Added if statements before mv statements in init section of
#		script to test if .log file exists before attempting a 
#		mv on .log files. This will cut down on unnecessary error 
#		messages displayed when .log file doesn't exist for mv.
#	20-Sep-95 (wadag01)
#		Changed the TST_OUTPUT initialisation to align with other
#		run scripts to:
#				TST_OUTPUT=$TST_ROOT_OUTPUT/star
#		instead of =$TST_ROOT_OUTPUT/output/star.
#		This change is intentional and has been made in most of
#		the .sh files.
#		Also added mkdir for $TST_OUTPUT.
#
#		Re:  Create and Initialize STAR LDB and DDB databases........
#		Added tests for existance of log and output files to get rid
#		of unsightly error messages that files do not exist.
#
#		Created another level of directories so the output goes	to
#		separate directories qryproc, ddl, tpf, aggs, pred, subs,dbproc.
#	28-Sep-95 (wadag01)
#		Corrected the definition of SEPPARAMDB and SEPPARAMDRIVERDB.
#	07-Aug-98 (popri01)
#		Re-work SEPPARAM settings for dbproc (see dbproc tests).
#	18-Sep-00 (vande02)
#		Adding the 'clean' code to easily clean up test databases.
#       14-Jul-04 (madsu02)
#               Defined SEPPARAMDB1=ldb1 in ddl area
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
	TST_OUTPUT=$TST_ROOT_OUTPUT/star/phase1

	if [ ! -d $TST_ROOT_OUTPUT/star ]
	then
	  echo "Creating Directory - $TST_ROOT_OUTPUT/star"
	  mkdir $TST_ROOT_OUTPUT/star
	fi
else 
	TST_OUTPUT=$ING_TST/output/star/phase1

	if [ ! -d $ING_TST/output ]
        then
          echo "Creating Directory - $ING_TST/output"
	  mkdir $ING_TST/output
        fi

	if [ ! -d $ING_TST/output/star ]
        then
          echo "Creating Directory - $ING_TST/output/star"
	  mkdir $ING_TST/output/star
        fi
fi

	if [ ! -d $TST_OUTPUT ]
	then
	  echo "Creating Directory - $TST_OUTPUT"
	  mkdir $TST_OUTPUT
	fi
	export TST_OUTPUT

	echo "Output files will be written to $TST_OUTPUT"
#

# Location of star init sep tests
TST_INIT=$ING_TST/star/init/sep
export TST_INIT

# Location of the star data files 
TST_DATA=$ING_TST/star/data
export TST_DATA

# Initialize variable input to shell script

work=$1
shift

# Check for init parameter

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

#-------------------------------------------------------------------------------
#                       End of Setup
#-------------------------------------------------------------------------------
#
#------------------------------------------------------------------------------
#                       Initialization Area
#------------------------------------------------------------------------------
#
# Create and Initialize STAR LDB and DDB databases........

if [ "$work" = "init" ]
then
     if [ "$*" = "" ]
     then
	  echo "You must input the modules to be initalized "
	  echo "or specify ""all""."
	  echo ""
	  echo " Example: sh $TST_SHELL/runstar.sh init all "
	  echo ""
	  echo "          or "
	  echo ""
	  echo "          sh $TST_SHELL/runstar.sh init ldbs "
	  echo ""

	  exit 1

      else
	  for fac in $*
	  do
	      if [ "$fac" =  "all" -o "$fac" = "ldbs" ] 
	      then
	
# Get the SEP test names from the list, because we have to rename
# the log files later:

		  testlist=`     \
   		  cat $TST_CFG/sinitldb.lis |   \
   		  awk 'BEGIN {FS = ":" } {print $3 }'|   \
   		  awk 'BEGIN {FS = "." } {print $1}'`

# Everything we do here is straightforward, but we do it seven
# times:

		  for i in 1 2 3 4 5 6 7
		  do

     		    SEPPARAMDB=ldb${i}
		    export SEPPARAMDB   
	
		      if [ $i -lt 3 ]
		      then 

			echo "Creating STAR $SEPPARAMDB @ ", `date`
			echo ""
			executor $TST_CFG/sinitldb.cfg >>$TST_OUTPUT/ldb.out 
			executor $TST_CFG/sinitlrg.cfg >>$TST_OUTPUT/ldb.out

		      else
			echo "Creating STAR $SEPPARAMDB @ ", `date`
		 	echo ""
		    	executor $TST_CFG/sinitldb.cfg >>$TST_OUTPUT/ldb.out

		      fi

# Give the listexec output file and log files a unique name:

			echo "Moving output and log files to unique name."
		   	echo ""
     		  	test -f  $TST_OUTPUT/ldb.out && \
     		  	   mv $TST_OUTPUT/ldb.out $TST_OUTPUT/ldb.${i}out 
		      
			if [ -r $TST_OUTPUT/sldbtbl.log ]
			then
                           test -f $TST_OUTPUT/sldbtbl.log && \
                              mv $TST_OUTPUT/sldbtbl.log \
			      $TST_OUTPUT/sldbtbl.${i}log
			fi
     			   for tstname in $testlist
     			   do
			     if [ -r $TST_OUTPUT/${tstname}.log ]
			     then
     		  	       test -f $TST_OUTPUT/${tstname}.log  && \
     		  	          mv $TST_OUTPUT/${tstname}.log  \
				  $TST_OUTPUT/${tstname}.${i}log 
 			     fi	
			   done
		done
			echo "Finished creating LDB databases @ ",`date`
			echo ""
		fi

		if [ "$fac" = "all" -o "$fac" = "ddb" ]
		then


# Create and Initialize the Master database

# Set the star database name
			
			SEPPARAMDB=starddb1/star
			SEPPARAMDB_D=starddb1
			
			export SEPPARAMDB
			export SEPPARAMDB_D

			echo "Creating STAR database $SEPPARAMDB @ ",`date`

			executor $TST_CFG/sinitddb.cfg >>$TST_OUTPUT/ddb.out
			echo "Finished creating DDB databases @ ",`date`
		fi
	  done
#
	fi
#
	echo "All STAR databases are created @ ",`date`
fi

#
#------------------------------------------------------------------------------
#                       End of Initialization Area
#------------------------------------------------------------------------------
#
#------------------------------------------------------------------------------
#                       Main Work Area
#------------------------------------------------------------------------------
#
#          Set up and run STAR SEP tests via listexec
#          ---------------------------------------------

if [ "$work" = "star" ]
then

	SEPPARAMDB=starddb1/star
	export SEPPARAMDB

	for fac in $*
	do
		if [ "$fac" = "all" -o "$fac" = "qryproc" ]
		then
#
# Run the STAR qryproc tests
#
			echo "Running STAR qryproc tests @ ",`date` 
			echo ""
			test ! -d $TST_OUTPUT/qryproc && \
					mkdir $TST_OUTPUT/qryproc

			executor $TST_CFG/starqprc.cfg \
					>>$TST_OUTPUT/qryproc/sqryprc.out

			echo "Finished the STAR qryproc tests @ ", `date`
		fi

#

		if [ "$fac" = "all" -o "$fac" = "ddl" ]
		then

# Set the databases used for ddl testing

			SEPPARAMDB1=ldb1
			export SEPPARAMDB1 

#
# Run the STAR ddl tests
#
			echo "Running STAR ddl tests @ ", `date`
			echo ""
			test ! -d $TST_OUTPUT/ddl && mkdir $TST_OUTPUT/ddl

			executor $TST_CFG/starddl.cfg >>$TST_OUTPUT/ddl/sddl.out

			echo "Finished running ddl tests @ ", `date`
		fi

#
#
		if [ "$fac" = "all" -o "$fac" = "tpf" ]
		then

# Run STAR tpf tests
#
			echo "Running STAR tpf tests ",`date`

# Re-initialize database before running TPF tests which require that the
# data be in its original order before running tests.

			test ! -d $TST_OUTPUT/tpf && mkdir $TST_OUTPUT/tpf

			echo "Reinitialize the STAR db before running tpf test."

			sep -b $TST_INIT/xda09.sep >>$TST_OUTPUT/tpf/xda09.out2
		  	sep -b $TST_INIT/xda10.sep >>$TST_OUTPUT/tpf/xda10.out2

			echo "Starting the STAR tpf tests @ ", `date`
			echo ""

			executor $TST_CFG/startpf.cfg >> \
					$TST_OUTPUT/tpf/startpf.out

			echo "Finished STAR tpf tests @ ", `date`
		fi

#
#
		if [ "$fac" = "all" -o "$fac" = "aggs" ]
		then

# Run STAR aggregate tests
#
			echo "Starting the STAR aggregate tests @ ", `date`
			echo ""
			test ! -d $TST_OUTPUT/aggs && mkdir $TST_OUTPUT/aggs

			executor $TST_CFG/saggs.cfg >>$TST_OUTPUT/aggs/saggs.out

			echo "Finished STAR aggregate tests @ ", `date`
		fi
#
#
		if [ "$fac" = "all" -o "$fac" = "pred" ]
		then

# Run STAR predicate tests
#
			echo "Starting the STAR predicate tests @ ", `date`
			echo ""

			test ! -d $TST_OUTPUT/pred && mkdir $TST_OUTPUT/pred

			executor $TST_CFG/spred.cfg >>$TST_OUTPUT/pred/spred.out

			echo "Finished STAR predicate tests @ ", `date`
		fi
#
#
		if [ "$fac" = "all" -o "$fac" = "subs" ]
		then

# Run STAR subsel tests
#
			echo "Starting the STAR subsel tests @ ", `date`
			echo ""

			test ! -d $TST_OUTPUT/subsel && mkdir $TST_OUTPUT/subsel

			executor $TST_CFG/ssubsel.cfg >> \
						$TST_OUTPUT/subsel/ssubsel.out

			echo "Finished STAR subsel tests @ ", `date`
												fi
#
#
		if [ "$fac" = "all" -o "$fac" = "dbproc" ]
		then
#
# Set the databases used for db procedure testing

			SEPPARAMDB1=ldb1
			SEPPARAMDRIVERDB=-dstarddb1/star
			export SEPPARAMDB1 SEPPARAMDRIVERDB

# Run STAR dbproc tests
#
			echo "Starting the STAR dbproc tests @ ", `date`
			echo ""

			test ! -d $TST_OUTPUT/dbproc && mkdir $TST_OUTPUT/dbproc

			executor $TST_CFG/sdbproc.cfg >> \
						$TST_OUTPUT/dbproc/sdbproc.out

			echo "Finished STAR dbproc tests @ ", `date`
		fi
	done

fi

# End of STAR SEP testing

			echo "End of STAR/SEP tests @ `date` .  ."

#-----------------------------------------------------------------------
#			Clean Up Area
#-----------------------------------------------------------------------
#
if [ "$work" = "clean" ]
then
    if [ "$*" = "" ]
    then
  	    echo "You must input the modules to be cleaned up "
	    echo "or specify ""all""."
	    echo ""
	    echo " Example: sh $TST_SHELL/runstar.sh clean all "
	    echo ""
	    echo "          or "
	    echo ""
	    echo "          sh $TST_SHELL/runstar.sh clean ldbs | ddb "
	    echo ""

	    exit 1
     else	
	    
        for fac in $*
        do
	    
	    if [ "$fac" = "all" -o "$fac" = "ldbs" ]
	    then

		echo "Destroying local star databases @ ", `date`
                echo ""
		destroydb ldb1 >>$TST_OUTPUT/starclean.out
		destroydb ldb2 >>$TST_OUTPUT/starclean.out
		destroydb ldb3 >>$TST_OUTPUT/starclean.out
		destroydb ldb4 >>$TST_OUTPUT/starclean.out
		destroydb ldb5 >>$TST_OUTPUT/starclean.out
		destroydb ldb6 >>$TST_OUTPUT/starclean.out
		destroydb ldb7 >>$TST_OUTPUT/starclean.out

 	    fi

 	    if [ "$fac" = "all" -o "$fac" = "ddb" ]
 	    then


		echo "Destroying STAR starddb1 database @ ", `date`
                echo ""
        	destroydb starddb1 >>$TST_OUTPUT/starclean.out


	     fi

	done 
#
     fi
#
	echo "Finished destroying databases @ ",`date`
fi
#
#----------------------------------------------------------------------
#			End of Clean Up Area
#----------------------------------------------------------------------
