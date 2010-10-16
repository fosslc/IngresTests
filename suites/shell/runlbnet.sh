#!/bin/sh
#                SCRIPT FOR RUNNING LOCAL NET TESTS
#
#
# Description:
#	This script is used to run the local NET SEP tests using
# 	listexec and excutor. 
#	
#  History:
#	9-17-92 (barbh)
#		Created to run the local NET tests automatically.
#	9-26-92 (rodneyy)
#		Fixed a few bugs.  Added missing "mkdir" to the routine
#		which creates the output directories.  Also changed
#		SEPPARAM_NODE to point to "lback::dbname" instead of
#		just "dbname".
#
#	9-29-92 (barbh)
#		Added "rtchkenv" back in place of mkdir. This was 
#		changed incorrectly.
#
#       1-11-93 (judi)
#               Change name of script from runnet.sh to runlbnet.sh to better
#               accomodate naming conventions.
#
#	3-4-93  (barbh)
#		Deleted old setup code and added new setup code. 
#		Added code to input more than one module or all. 
#
#               Script will be run as follows for initializing net db:
#
#               sh $TST_SHELL/runlbnet.sh init all
#
#		or choose a module
#
#		sh $TST_SHELL/runlbnet.sh init |gaa|gba|gca| 
#
#               Script will be run as follows for running the net modules:
#
#               sh $TST_SHELL/runlbnet.sh lbnet all
#
#               or
#
#               sh $TST_SHELL/runlbnet.sh lbnet <list of net modules>
#
# 	30-aug-1993 (judi) Added new environment variable, TST_DATA, which
#		will point to the location of the .dat files.  This variable
#		is used within the sep tests.
#	28-sep-1993 (judi) Need to export the TST_DATA variable.
#	20-Sep-95 (wadag01)
#		Changed the TST_OUTPUT initialisation to align with other
#		run scripts to:
#				TST_OUTPUT=$TST_ROOT_OUTPUT/net/lback
#		instead of =$TST_ROOT_OUTPUT/output/net/lback
#		This change is intentional and has been made in most of
#		the .sh files.
#		Added mkdir for $TST_OUTP.
#	02-Mar-1998 (vande02)
#		Created based upon QA's runlbnet.sh for porting's handoff_qa.
#	04-Jun-1998 (vande02
#		Added a Clean Up Area to destroy test lback databases
#	04-Jun-1999 (vande02)
#		Added calls to qawtl for messages to be written to errlog.log
#       11-jan-2000 (vande02)
#           Checking the return code for each database initialization step, so
#           if the createdb fails, the script will echo a message and exit.
#---------------------------------------------------------------------------
#			Setup Area
#------------------------------------------------------------------------------
umask 2 
umask 

# Set the output directory for test results.
#
if [ "$TST_ROOT_OUTPUT" != "" ]
then
	TST_OUTPUT=$TST_ROOT_OUTPUT/net/lback

	if [ ! -d $TST_ROOT_OUTPUT/net ]
	then
	  echo "Creating Directory - $TST_ROOT_OUTPUT/net"
	  mkdir $TST_ROOT_OUTPUT/net
	fi
else 
	TST_OUTPUT=$ING_TST/output/net/lback

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
#

# Initialize variable input to shell script

work=$1
shift

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
#-------------------------------------------------------------------------------#			End of Setup
#-------------------------------------------------------------------------------#
#------------------------------------------------------------------------------
#			Initialization Area
#------------------------------------------------------------------------------
#
if [ "$work" = "init" ]
then
     if [ "$*" = "" ]
     then
         echo "You must input the modules to be initalized "
	 echo "or specify ""all""."
	 echo ""
	 echo " Example: sh $TST_SHELL/runlbnet.sh init all "
	 echo ""
	 echo "          or "
	 echo ""
	 echo "          sh $TST_SHELL/runlbnet.sh init gaa gca "
         echo ""

	 exit 1

      else

	 for fac in $*
	 do
		if [ "$fac" = "all" -o "$fac" = "gaa" ]
		then
			echo "Creating gaa database @ ",`date`
			echo "" 
			destroydb gaadb >>$TST_OUTPUT/netinit.out
			createdb gaadb >>$TST_OUTPUT/netinit.out

                        if [ $? != 0 ]
                        then
                          echo Creation of Net Loopback database gaadb Failed.
                          echo See $TST_OUTPUT/netinit.out for error messages.
                          exit 1
                        fi

			echo "Finished creating gaa database @ ",`date`
			echo "" 
		fi

		if [ "$fac" = "all" -o "$fac" = "gba" ]
		then
			echo "Creating gba database @ ",`date`
			echo ""
			destroydb gbadb >>$TST_OUTPUT/netinit.out
			createdb gbadb >>$TST_OUTPUT/netinit.out

                        if [ $? != 0 ]
                        then
                          echo Creation of Net Loopback database gbadb Failed.
                          echo See $TST_OUTPUT/netinit.out for error messages.
                          exit 1
                        fi

			echo "Finished creating gba database @ ",`date`
			echo ""
		fi

		if [ "$fac" = "all" -o "$fac" = "gca" ]
		then
			echo "Creating gca database @ ",`date`
			echo ""
			destroydb gcadb >>$TST_OUTPUT/netinit.out
			createdb gcadb >>$TST_OUTPUT/netinit.out

                        if [ $? != 0 ]
                        then
                          echo Creation of Net Loopback database gcadb Failed.
                          echo See $TST_OUTPUT/netinit.out for error messages.
                          exit 1
                        fi

			echo "Finished creating gca database @ ",`date`
			echo ""
		fi

	 done
#
      fi
#
	echo "Databases for local net testing have been created. "
fi

#------------------------------------------------------------------------------
#                       Main Work Area
#------------------------------------------------------------------------------
#

TST_DATA=$ING_TST/gcf/gcc/data
export TST_DATA

if [ "$work" = "lbnet" ]
then
	for fac in $*
	    do
		   if [ "$fac" = "all" -o "$fac" = "gaa" ]
		   then

# Execute GAA SEP tests
 
     			SEPPARAM_NODE=lback::gaadb
			export SEPPARAM_NODE   
			echo "Starting GAA sep tests @ ",`date`
			echo ""
			qawtl RUNNING LOOPBACK GAA TESTS
			executor $TST_CFG/gaaunix.cfg >$TST_OUTPUT/gaa.out 

			echo "Finshed with GAA sep tests @ ",`date`
			echo ""
			qawtl END LOOPBACK GAA TESTS
		   fi

#
		   if [ "$fac" = "all" -o "$fac" = "gba" ]
		   then

# Execute GBA sep tests

			SEPPARAM_NODE=lback::gbadb
			export SEPPARAM_NODE
			echo "Running GBA net tests @ ",`date` 
			echo "" 
			qawtl RUNNING LOOPBACK GBA TESTS
			executor $TST_CFG/gbaunix.cfg >>$TST_OUTPUT/gba.out

			echo "Finished running the GBA net tests @ ", `date`
			echo ""
			qawtl END LOOPBACK GBA TESTS
		   fi

#
		   if [ "$fac" = "all" -o "$fac" = "gca" ]
		   then

			SEPPARAM_NODE=lback::gcadb
			export SEPPARAM_NODE
			echo "Running GCA net tests @ ", `date`
			echo ""
			qawtl RUNNING LOOPBACK GCA TESTS
			executor $TST_CFG/gcaunix.cfg >>$TST_OUTPUT/gca.out

			echo "Finished running GCA net tests @ ",`date`
			echo ""
			qawtl END LOOPBACK GCA TESTS
		   fi

	    done
fi
# End of NET SEP testing

		   echo "End of NET/SEP tests @ ",`date`
#------------------------------------------------------------------------------
#			Clean Up Area
#------------------------------------------------------------------------------
#
if [ "$work" = "clean" ]
then
     if [ "$*" = "" ]
     then
         echo "You must input the modules to be cleaned up "
	 echo "or specify ""all""."
	 echo ""
	 echo " Example: sh $TST_SHELL/runlbnet.sh clean all "
	 echo ""
	 echo "          or "
	 echo ""
	 echo "          sh $TST_SHELL/runlbnet.sh clean gaa gca "
         echo ""

	 exit 1

      else

	 for fac in $*
	 do
		if [ "$fac" = "all" -o "$fac" = "gaa" ]
		then
			echo "Destroying gaa database @ ",`date`
			echo "" 
			destroydb gaadb >>$TST_OUTPUT/netclean.out

			echo "Finished Destroying gaa database @ ",`date`
			echo "" 
		fi

		if [ "$fac" = "all" -o "$fac" = "gba" ]
		then
			echo "Destroying gba database @ ",`date`
			echo ""
			destroydb gbadb >>$TST_OUTPUT/netclean.out

			echo "Finished Destroying gba database @ ",`date`
			echo ""
		fi

		if [ "$fac" = "all" -o "$fac" = "gca" ]
		then
			echo "Destroying gca database @ ",`date`
			echo ""
			destroydb gcadb >>$TST_OUTPUT/netclean.out

			echo "Finished Destroying gca database @ ",`date`
			echo ""
		fi

	 done
#
      fi
#
	echo "Databases for local net testing have been destroyed. "
fi
