#!/bin/sh
#           SCRIPT FOR RUNNING THE EMBEDDED 3GL LANGUAGE TESTS
#
#
# Description:
#	This script is used to setup and run the embedded 3gl language tests
# 	using listexec and excutor.
#
#  History:
#	16-FEB-93 (jpark)
#		Created.
#	24-FEB-93 (jpark)
#		name changes.
#	15-mar-93 (dianeh)
#		Added "clean" argument to destroy test databases.
#	29-mar-93 (barbh)
#		Removed the "clean" argument. Put the script into the same 
# 		format as all testing shell scripts.
#
#	31-mar-93 (barbh)
#		Added logic for pascal,pl1 & basic modules.
#
#	07-apr-93 (barbh)
#		Added logic to send an error message if the initialization 
#		is not requested correctly.
#
#	09-apr-93 (barbh)
#		Fixed typo in echo statement - missing ending quote.
#		Fixed typo in test statement - changed brace to bracket.
#	30-apr-93 (judi)
#		Added directory extension under TST_OUTPUT on the executor
#		command lines for each test area so the .out files will go to
#		the individual test area like the .rpt files do.
#		Ex. TST_OUTPUT/fortran, or TST_OUTPUT/c.
#	09-jun-93 (sandhya)
#               fixed typo for error message under "Main test area"
#       20-oct-93 (alices)
#               added SEPPARAMDB and SEPPARAMDB2 in order to run language
#               tests.  SEPPARAMPERSONNE and SEPPARAMGOURMET will be removed
#               later on.
#       28-oct-93 (alices)
#               Took out SEPPARAMPERSONNE and SEPPARAMGOURMET 'cause
#               these two parameters have been replaced by SEPPARAMDB and
#               SEPPARAMDB2.
#	20-Sep-95 (wadag01)
#		Changed the TST_OUTPUT initialisation to align with other
#		run scripts to:
#				TST_OUTPUT=$TST_ROOT_OUTPUT/embed
#		instead of =$TST_ROOT_OUTPUT/output/embed
#		This change is intentional and has been made in most of
#		the .sh files.
#		Also added mkdir for it.
#	19-Jun-00 (sarjo01)
#		Added MT (Multi-threaded) suite.
#	19-Feb-01 (rogch01)
#		Correct typo to allow C to be run separately.
#	11-Sep-06 (sarjo01)
#	        Add -n flag to 'createdb personnel' to enable testing of
#               unicode types. 
#	07-Mar-07 (sarjo01)
#	        Fixed last comment (bad year, missing '#'). 
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
	TST_OUTPUT=$TST_ROOT_OUTPUT/embed
else 
	TST_OUTPUT=$ING_TST/output/embed

	if [ ! -d $ING_TST/output ]
	then
	  echo "Creating Directory - $ING_TST/output"
	  mkdir $ING_TST/output
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

# Check for init parameter

if [ "$1" = "init" ]
then
	work=$1
	shift
fi
#-------------------------------------------------------------------------------
#                       End of Setup
#-------------------------------------------------------------------------------
#
#-------------------------------------------------------------------------------#			Initialization Area
#-------------------------------------------------------------------------------#
if [ "$work" = "init" ]
then 
     if [ "$*" = "" ]
     then
	   echo "You must input the modules to be initalized "
	   echo "or specify ""all""."
	   echo ""
	   echo " Example: sh $TST_SHELL/runfe3gl.sh init all "
	   echo ""
	   echo "          or "
           echo ""
	   echo "          sh $TST_SHELL/runfe3gl.sh init 3gl1 "
	   echo ""

	   exit 1

      else	

	  for fac in $*
	  do
		if [ "$fac" = "all" -o "$fac" = "3gl1" ]
	        then

		 	echo "Creating the personnel database @ ",`date`
			echo ""

			destroydb personnel >>$TST_OUTPUT/feinit3gl.out
			createdb -n personnel >>$TST_OUTPUT/feinit3gl.out

			echo "Finished creating personnel database @ ",`date`
			echo ""
 		fi

		if [ "$fac" = "all" -o "$fac" = "3gl2" ]
		then 

			echo "Creating the gourmet database @ ",`date`
			echo ""

			destroydb gourmet >>$TST_OUTPUT/feinit3gl.out
			createdb gourmet >>$TST_OUTPUT/feinit3gl.out

			echo "Finished creating gourmet database @ ",`date`
			echo ""
		fi

	  done
#
      fi
#

	echo "3gl databases have been created @ ",`date`
fi

#
#------------------------------------------------------------------------------
#		End of Initialization Area
#------------------------------------------------------------------------------

#------------------------------------------------------------------------------
#		Main test Area
#------------------------------------------------------------------------------
#
if [ "$work" = "3gl" ]
then
       if [ "$*" = "" ]
       then
            echo "You must input the 3gl modules to be tested "
	    echo "as designated in the PRDF for your hardware."
	    echo ""
	    echo " Example: sh $TST_SHELL/runfe3gl.sh 3gl fortran c"
	    echo ""

	    exit 1

        else	

	    for fac in $*
	    do
		if [ "$fac" = "ada" ]
		then
#
# Run the embedded ada tests
#
			echo "Running the EMBEDDED ADA tests @ ",`date`
			echo ""
			if [ ! -d $TST_OUTPUT/ada ]
			then
			  echo "Creating Directory - $TST_OUTPUT/ada"
			  mkdir $TST_OUTPUT/ada
			fi

		executor $TST_CFG/fe3glada.cfg >$TST_OUTPUT/ada/3glada.out

			echo "Finished with the Embedded ADA tests @ ",`date`
			echo ""
		fi

		if [ "$fac" = "mt" ]
		then
#
# Run the embedded MT tests
#
			echo "Running the EMBEDDED MT tests @ ",`date`
			echo ""
			if [ ! -d $TST_OUTPUT/mt ]
			then
			  echo "Creating Directory - $TST_OUTPUT/mt"
			  mkdir $TST_OUTPUT/mt
			fi
	
			executor $TST_CFG/fe3glmt.cfg >$TST_OUTPUT/mt/fe3glmt.out

			echo "Finished with the Embedded MT tests @ ",`date`
			echo ""
		fi

		if [ "$fac" = "c" ]
		then
#
# Run the embedded c tests
#
			SEPPARAMDB=personnel 
			SEPPARAMDB2=gourmet

			export SEPPARAMDB SEPPARAMDB2

			echo "Running the EMBEDDED C tests @ ",`date`
			echo ""
			if [ ! -d $TST_OUTPUT/c ]
			then
			  echo "Creating Directory - $TST_OUTPUT/c"
			  mkdir $TST_OUTPUT/c
			fi
	
			executor $TST_CFG/fe3glc.cfg >$TST_OUTPUT/c/fe3glc.out

			echo "Finished with the Embedded C tests @ ",`date`
			echo ""
		fi

		if [ "$fac" = "cobol" ]
		then
#
# Run the embedded cobol tests
#
			echo "Running the EMBEDDED COBOL tests @ ",`date`
			echo ""
			if [ ! -d $TST_OUTPUT/cobol ]
			then
			  echo "Creating Directory - $TST_OUTPUT/cobol"
			  mkdir $TST_OUTPUT/cobol
			fi

		executor $TST_CFG/fe3glcob.cfg >$TST_OUTPUT/cobol/fe3glcob.out

			echo "Finished with the Embedded Cobol tests @ ",`date`
			echo ""
		fi

		if [ "$fac" = "fortran" ]
		then
#
# Run the embedded fortran tests
#
		     echo "Running the EMBEDDED FORTRAN tests @ ",`date`
		     echo ""
			if [ ! -d $TST_OUTPUT/fortran ]
			then
			  echo "Creating Directory - $TST_OUTPUT/fortran"
			  mkdir $TST_OUTPUT/fortran
			fi

	            executor $TST_CFG/fe3glf.cfg >$TST_OUTPUT/fortran/fe3glf.out

		     echo "Finished with the Embedded Fortran tests @ ",`date`
		     echo ""

		fi

		if [ "$fac" = "basic" ]
		then
#
# Run the embedded basic tests
#
			
			echo "Running the EMBEDDED BASIC tests @ ",`date` 
			echo ""
			if [ ! -d $TST_OUTPUT/basic ]
			then
			  echo "Creating Directory - $TST_OUTPUT/basic"
			  mkdir $TST_OUTPUT/basic
			fi

		  executor $TST_CFG/fe3glbas.cfg >$TST_OUTPUT/basic/fe3glbas.out

		      echo "Finished with Embedded Basic tests @ ",`date`
		      echo ""

		fi

		if [ "$fac" = "pl1" ]
		then
#
# Run the embedded PL1 tests
#
			echo "Running the EMBEDDED PL1 tests @ ",`date` 
			echo ""
			if [ ! -d $TST_OUTPUT/pl1 ]
			then
			  echo "Creating Directory - $TST_OUTPUT/pl1"
			  mkdir $TST_OUTPUT/pl1
			fi

		executor $TST_CFG/fe3glpl1.cfg >$TST_OUTPUT/pl1/fe3glpl1.out

			echo "Finished with Embedded PL1 tests @ ",`date`
			echo ""

		fi

		if [ "$fac" = "pascal" ]
		then
#
# Run the embedded pascal tests
#
			echo "Running the EMBEDDED Pascal tests @ ",`date` 
			echo ""
			if [ ! -d $TST_OUTPUT/pascal ]
			then
			  echo "Creating Directory - $TST_OUTPUT/pascal"
			  mkdir $TST_OUTPUT/pascal
			fi

		executor $TST_CFG/fe3glpas.cfg >$TST_OUTPUT/pascal/fe3glpas.out

			echo "Finished with Embedded Pascal tests @ ",`date`
			echo ""

		fi
	  done

     fi
 	echo "End of EMBEDDED 3GL tests @ ",`date`
fi
