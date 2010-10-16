#!/bin/sh
# Copyright (c) 2008 Ingres Corporation
#
#
# 01-mar-1998 (vande02) Created based on QA's runbe.sh for porting's
#			handoff_qa requirements.
# 03-jun-1998 (vande02) Added Clean Up work area to destroy databases
#			after testing is completed (all, or some dbs)
# 25-jun-1998 (vande02) The syntax for running all or some of the back-end
# 08-jul-1998 (vande02) Removed the destroydb b74453db from clean up
#
#	sh $TST_SHELL/runbe.sh {pagesize} {storagestruct} init {suites}
#
#	sh $TST_SHELL/runbe.sh {pagesize} {storagestruct} be {suites} 
#
#	You must set the page size to one of 2, 4, 8, 16, 32, 64, or all
#	You must set the storagestruct to heap, hash, btree, isame, or all
#	You must select what suites to run or all
#
#	The handoffqa standard run for back-end is:
#
#	sh $TST_SHELL/runbe.sh 8 heap init/be all
#
# 15-jul-1998 (vande02) Removed the chmod 776 to the convert sed file
#	Did not eleviate UX:sh error after all.
# 08-Sep-1998 (vande02) Added to the LAR init step the execution of $TST_INIT
#	initlar.sep which creates lardb, creates 5 db locations, and extends
#	lardb to 5 additional locations for testing.
#	07-dec-1998 (hanch04)
#	    Added TABLE_VERSION
# 26-feb-1999 (somsa01)
#	Added blob test suite.
# 26-mar-1999 (vande02)
#           Removed the preprocessing to convert VPG_SZ and STR_ST which are
#           now going to be hard coded throughout the back-end test suites with
#           the exception of be/vps.  A new shell script has been created for
#           be/vps which will still use and exercise VPG_SZ and STR_ST.
#           Also, removed the TABLE_VERSION variable now that SEP's
#           grammar.lex code will mask out 2.5 version strings.  And, a check
#	    for cache sizes of 4k and 8k will be done before executing tests.
#	    Moved the c2secure suite to the end.
##      25-may-1999 (popri01)
##              Validate all parameters at start. Improve error messages.
##              Remove extraneous messages.
#       07-Jun-1999 (vande02)
#               Added calls to qawtl for messages to be written to errlog.log
#       18-jun-1999 (musro02)
#           Cosmetic changes to echo output
##	15-jul-1999 (popri01)
##		The LAR config file specifies a diff level of 200 for every
##		LAR test, but the default diff level for executor is 20. So
##		every LAR test that has more than 20 DIFFs is treated as an
##		ABORT. Use a DIFF level of 200 on the executor command
##		line so that the config value diff level is respected.
##		NOTE: To accomplish this required modifying doit to pass 
##		along command line parameters.
##	15-aug-1999 (popri01)
##		Re-work of preceding change (15-jul):
##		After additional research, it appears that the executor
##		diff level has a different function than the sep diff level.
##		The purpose of the sep parameter is to control the maximum
##		diff level for an individual test, and is honored regardless
##		of the executor diff level. The executor parameter seems
##		intended to act a governor for the suite as a whole; i.e.,
##		abort if more than X tests have more than Y difs, where
##		X is tolerance level (-t) and Y is diff level (-d), as
##		specified by executor defaults (20 and 5 respectively) or
##		executor command line parameters. So, for example, a suite
##		whose list file specifies a diff level, for each test, of 
##		200 (and assuming the executor defaults), would continue
##		to run even if four individual tests aborted with more than
##		200 difs each, but would fail if 6 tests had more than 20
##		difs. Thus, the better solution to the LAR problem (the
##		suite aborts because too many tests had more than 20 difs),
##		is to override the tolerance level, not the diff level.
##		Furthermore, the better implementation is simply to allow
##		these executor command line parameters to be specified as
##		parameters for this script.
##		Also, since we shouldn't be re-configuring Ingres during a
##		test suite, I moved the cache size check out of the test
##		loop so that it is only performed once.
# 11-jan-2000 (vande02)
#           Checking the return code for each database initialization step, so
#           if the createdb fails, the script will echo a message and exit.
# 19-mar-2000 (vande02)
#           Removing the second occurrance of belar under work section. Must
#	    have come from an incorrect integration.
# 09-Jun-2000 (sarjo01)
#           Added new suite: MISCFUNC for miscellaneous new functionality 
#	    and features.
# 21-Aug-2000 (vande02)
#           Added test databases, b75662db, b77231db, and lar58db to the clean
#           up section for lar.  And, added eia40db to qryproc clean up section.
# 13-Sep-2000 (sarjo01)
#	    Added RODB suite.
# 25-Oct-2000 (vande02)
#	    Added checks for ALL cache page sizes.  The be/miscfunc tests
#	    require all page sizes.  This may be a problem for some machines.
# 26-Apr-2001 (wu$ca01)
#           Added createdb utildb3/utildb4, and initutil.sep for the UTIL area
#           for testing copydb new tests. Added SEPPARAMDB=beudtdb for UDT area.
# 6-June-2001 (BOIJA02) Creating utildb is wasted effort, the first 10 tests all
#	      destroy it as their first command. We now destroy it and let the 
#             first test create it instead. edited the surrounding text to fit.
# 26-Sep-2001 (BOIJA02) Removing error check, setup failed to complete if util
#  	      database didn't exist at start of setup. Making other error checks
#   	      more specific
# 30-Oct-2001 (wu$ca01) Added createdb -n unidb and set SEPPARAMDB3=unidb for 
#             testing unicode in the datatypes suite, and destroydb unidb in 
#             the Cleanup area.
# 28-Feb-2002 (devjo01) Skip RLL suite if running clustered.
#	      Correct test for cache page sizes being on to only look
#	      on current node.
# 23-Aug-2002 (devjo01) Skip RLL suite if installation configured for
#	      DMCM.
# 08-Oct-2002 (vande02) Remove UDT/SOL suites from all parameter, and add
#             UDT and Spatial Objects setup step (executing iilink, ingstop and
#             restart ingres).  This will enable these two suites to run but the
#             remainder of the back-end suites should be run with a generic DBMS
#             that is without udts installed.  After UDT/SOL run, when you run
#	      clean udt or clean sol the DBMS Server will be relinked with
#	      out UDTs and Spatial Objects and will be Generic.
#
#	      If the DBMS server still has UDTs and SOL linked in (i.e. 
#	      iimerge.udt still exits) only a WARNING will be put to the
#	      beinit.out file and to the errlog.log during the initialization
#	      of all the back-end test databases except for UDT/SOL.
#	      For UDT/SOL, if iimerge.udt exists, initialization will continue,
#	      if it does not exist, the initialization will exit with warnings.
#
#	      Added missing clean up section for miscfunc and added alttbl35db, 
#	      and exdb, to appropriate clean up sections.
# 08-Nov-2002 (vande02) Adding word WARNING to messages and putting them out to
#             beinit.out and beclean.out instead of only to the screen for the
#	      UDT and SOL suites only.  Also, during the attempt to run the
#	      actual UDT and SOL suites, if iimerge.udt does not exist, then
#	      warning messages and instructions on what to do to setup UDTs is
#	      put to output file $TST_OUTPUT/WARNING_udtsol.out.
# 12-Feb-2003 (hanje04)
#		RAAT is now only supported on Solaris, AIX, HPUX, Tru64
#		and Windows so	only run the raat suite on these platforms.
# 14-May-2003 (vande02)
#               Modifying this script so it will continue to run and create the
#               needed databases even after putting out the WARNINGS that UDTs
#               are installed in your server and you should be using a generic
#               server.  The script will detect the setting of TST_LISTEXEC 
#               and will only put out the warnings if it is set to listexec.
#               Development can run their tests with UDTs installed and QA
#		will not until BE suites UDT/SOL are run.
#
#               Put echo and qawtl warnings in a routine to be called under
#               each suite in the initialization phase named udts_installed.
#
#               Removed the AREA variable setting in initialization phase for
#               each suite as this appeared to not be used.
#
#		Moved the location of UDT/SOL suites to the end of each section
#		such as init, work, and clean up so these two suites will
#		always be last.
#
#		Changed init output file name for RODB suite from berodb.out
#		to the generic name beinit.out.  Added rodb to clean up section.
#
# 02-Jun-2003 (boija02)
#		Last change means raat error message displayed whether or not 
#		raat suite is being run. Reversed nesting of if structures to 
#		avoid confusing output.
#
# 14-Oct-2003 (vande02)
#		The initialization phase will check if udts are installed under
#		the $II_SYSTEM/ingres/bin/lp64 directory for 64 bit installs,
#               and put echo and qawtl warnings out if udts are installed AND
#		if TST_LISTEXEC is set to 'listexec' to allow UDT/SOL to run on
#               64 bit installs.
#
#		The clean up section only moves the iimerge.udt binary to a new
#		name but DOES NOT uninsall UDTS for 64 bit installations only.
#		Steps to uninstall UDTS are put to beclean.out and errlog.log.
#
#		Corrected the raat initialization section so it executes the
#		same as all the other suites.  Raat was being skipped when it
#		SHOULD not be skipped.
# 08-Jul-2004 (vande02)
#		Turning C2 Security Auditing ON (and ingstop/ingstart) during
#		be/c2secure initialization then turing it OFF (and ingstop/
#		ingstart) during be/c2scure cleanup.
# 20-Sep-2004 (vande02)
#		Removing SOL suite as Spatial Objects will not be available from
#		Ingres r3 open source.  Removing the following suites, UDT, RLL,
#		RAAT, and RODB.  Added copyright lines.
# 05-May-2005 (vande02)
#               Adding cleanlar.sep and cleanmsfn.sep during the clean phase
#               to cleanup database locations created during suites for LAR and
#               MISCFUNC.
# 08-Aug-2005 (vande02)
#               Because acc10.sep is really an initialization test for database
#		grantdb used by 32 subsequent tests, this change adds acc10.sep
#		to be run during the initialization phase.  This will allow the
#		rerun of single tests out of sequence in the be/accntl suite
#		as long as 'runbe.sh init accntl' was run at least once.
# 16-Sep-2006 (hanal04)
#               Use $TST_OUTPUT as the working directory. Failure to do so
#               leads to hangs if all QA users do not have write permission
#               in the cwd.
# 08-Mar-2007 (vande02)
#               Added -n flag to alttbldb creation to enable Unicode.
# 06-Apr-2007 (vande02)
#		Added ANSIDATE data type suite datetime.
# 10-Sep-2007 (sarjo01)
#               Added setting of SEPPARAMDB, SEPPARAMAPI_INC variables to API
#               run section for new test, api13.sep
# 08-Apr-2008 (sarjo01)
#               Added setting of shell variable SEPPARAM_CHARSET.
# 08-Sep-2008 (sarjo01)
#               Moved setting of SEPPARAMAPI_INC to global location. 
# 09-Sep-2008 (sarjo01)
#               Broke up SEPPARAMAPI_INC setting into 2 commands for Solaris 
# 20-Oct-2008 (vande02)
#               Added Unicode enabled database for new qryproc regression tests.
# 30-Oct-2008 (vande02)
#               Changed Unicode enabled db variable for qryproc to SEPPARAMDB3.
# 16-Dec-2008 (wanfr01)
#		'be clean lar' doesn't clean lar98db
# 02-Feb-2009 (sarjo01)
#	        Set ING_CHARSET to match SEPPARAM_CHARSET.
# 27-Feb-2009 (boija02)
#		Replacing all ING_CHARSET references with SEPPARAM_CHARSET
#
betestlist="access accntl alttbl api blob c2secure datetime datatypes miscfunc fastload lar qryproc ttpp util"
diflvl=
joblvl=
hostname="`iipmhost`"
sys=`uname -s`

# ACTUAL WORK IS DONE HERE
doit() {

# Check for tolerance/DIF level overrides
if [ -z "$diflvl" ]
then
	dp=
else
	dp="-d$diflvl"
fi
if [ -z "$joblvl" ]
then
	tp=
else
	tp="-t$joblvl"
fi

# Set the output directory for test results.
#
		 if [ "$TST_ROOT_OUTPUT" != "" ]
		 then
			 TST_OUTPUT=$TST_ROOT_OUTPUT/be/$AREA
		 fi
		 echo "Executor $dp $tp $TST_CFG/$CFG_FILE > $TST_OUTPUT/$OUT_FILE"
		 executor $dp $tp $TST_CFG/$CFG_FILE > $TST_OUTPUT/$OUT_FILE
}

errorHelp() {
	
	echo "You must specify type of processing and which suites to process."
	echo "Where type of processing is one of the following:"
	echo ""
	echo "	clean init be"
	echo ""
	echo "and suites to process is either:"
	echo ""
	echo "	all"
	echo ""
	echo "or one or more of the following:"
	echo ""
	echo "	$betestlist"
	echo ""
	echo "For example:"
	echo "          runbe init access util "
	echo ""
	echo "You may also override the executor tolerance and diff levels for 'be'"
	echo "processing using -tNNN and/or -dNNN respectively, For example, to allow"
	echo "the LAR suite to run with up to 10 aborts, specify:"
	echo "          runbe be -t10 lar"
	echo ""
	echo "note: No space is allowed between the flag and the number."
	echo ""
}

umask 2 

# Set a variable that can be used to check the character set
#
ii_code=`ingprenv II_INSTALLATION`
SEPPARAM_CHARSET=`ingprenv II_CHARSET$ii_code`
export SEPPARAM_CHARSET

# Set variable needed for api program compilation
SEPPARAMAPI_INC=-I$II_SYSTEM/ingres/files
export SEPPARAMAPI_INC

# Set the output directory for test results.
#
if [ "$TST_ROOT_OUTPUT" != "" ]
then
	TST_OUTPUT=$TST_ROOT_OUTPUT/be
else 
	TST_OUTPUT=$ING_TST/output/be

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
if [ ! -d $TST_OUTPUT ]
then
  echo "Creating Directory - $TST_OUTPUT"
  mkdir $TST_OUTPUT
fi
     
# Initialize variable input to shell script

case $1 in
	init|clean|be)
		work=$1
		shift
		;;
	*)
		echo "!!! Invalid processing type: $1"
		errorHelp
		exit 1
		;;
esac

if [ "$*" = "" ]
then
	echo "!!! No suite list specified"
	errorHelp
	exit 1
fi

for x in $*
do

# Ignore flags for the moment
	case $x in
            -d*|-t*)
                continue
		;;
	esac

	badtest=true
	for y in all $betestlist
	do
		if [ "$x" = "$y" ]
		then
			badtest=false
			break
		fi
	done
	if $badtest
	then
		echo "!!! Invalid suite list: $x / $*"
		errorHelp
		exit 1
	fi
done

# Avoid qasetuser hangs because the target user doe snot have permission
# to write to the pwd
cd $TST_OUTPUT

CLUSTER=false
[ "1X" = "`ingprenv II_CLUSTER`X" ] && CLUSTER=true


#---------------------------------------------------------------------------
#			 End of Setup
#---------------------------------------------------------------------------
#---------------------------------------------------------------------------
#			 Initialization Area
#---------------------------------------------------------------------------
#
# Start the initialization of Backend Databases
#
if [ "$work" = "init" ]

then 
     
     for fac in $*
     do

#
# ACCESS
		if [ "$fac" = "all" -o "$fac" = "access" ]
		then 

		   if [ ! -d $TST_OUTPUT/access ]
			then
			  echo "Creating Directory - $TST_OUTPUT/access"
			  mkdir $TST_OUTPUT/access
		   fi
			echo "Creating BE/Access database @ ", `date`
			echo ""
			destroydb accessdb1 >>$TST_OUTPUT/beinit.out
			createdb accessdb1 >>$TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database accessdb1 Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
		fi
#
# ACCNTL
		if [ "$fac" = "all" -o "$fac" = "accntl" ]
		then

		   if [ ! -d $TST_OUTPUT/accntl ]
			then
			  echo "Creating Directory - $TST_OUTPUT/accntl"
         		  mkdir $TST_OUTPUT/accntl
		   fi

			echo "Creating BE/Accntl database @ ", `date`
			echo ""
			destroydb accntldb >>$TST_OUTPUT/beinit.out
			createdb accntldb >>$TST_OUTPUT/beinit.out

			SEPPARAMDB3=grantdb
			export SEPPARAMDB3

			sep -b -v $ING_TST/be/accntl/sep/acc10.sep -o$TST_OUTPUT/accntl/initaccntl.out
			
			if [ $? != 0 ]
			then
			  echo Creation of Backend database accntldb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
		fi
#
# API
#

		if [ "$fac" = "all" -o "$fac" = "api" ]
		then

		   if [ ! -d $TST_OUTPUT/api ]
			then
			  echo "Creating Directory - $TST_OUTPUT/api"
			  mkdir $TST_OUTPUT/api
		   fi

			echo "Creating BE/Api databases @ ", `date`
			echo ""
			destroydb apidb2 >>$TST_OUTPUT/beinit.out
			destroydb apidb1 >>$TST_OUTPUT/beinit.out
			createdb apidb2 >>$TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database apidb2 Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
			createdb apidb1 >>$TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database apidb1 Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
                fi

#
# DATETIME
                if [ "$fac" = "all" -o "$fac" = "datetime" ]
                then

                   if [ ! -d $TST_OUTPUT/datetime ]
                        then
                          echo "Creating Directory - $TST_OUTPUT/datetime"
                          mkdir $TST_OUTPUT/datetime
                   fi

			echo "Creating BE/Datetime database @ ", `date`
			echo ""
			destroydb datetimedb >>$TST_OUTPUT/beinit.out
			createdb datetimedb >>$TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database datetimedb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi

                fi

#
# DATATYPES
		if [ "$fac" = "all" -o "$fac" = "datatypes" ]
		then

		   if [ ! -d $TST_OUTPUT/datatypes ]
			then
		  	  echo "Creating Directory - $TST_OUTPUT/datatypes"
	          	  mkdir $TST_OUTPUT/datatypes
	           fi

			echo "Creating BE/DataTypes databases @ ", `date`
			echo ""
			destroydb datatypedb >>$TST_OUTPUT/beinit.out
			createdb datatypedb >>$TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database datatypedb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi

			destroydb datatypedb2 >>$TST_OUTPUT/beinit.out
			createdb datatypedb2 >>$TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database datatypedb2 Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
                        fi

                        destroydb unidb >>$TST_OUTPUT/beinit.out
                        createdb -n unidb >>$TST_OUTPUT/beinit.out
                        if [ $? != 0 ]
                        then
                          echo Creation of Backend database unidb Failed.
                          echo See $TST_OUTPUT/beinit.out for error messages.
                          exit 1
			fi
		fi

#
# MISCFUNC
                if [ "$fac" = "all" -o "$fac" = "miscfunc" ]
                then

                   if [ ! -d $TST_OUTPUT/miscfunc ]
                        then
                          echo "Creating Directory - $TST_OUTPUT/miscfunc"
                          mkdir $TST_OUTPUT/miscfunc
                   fi

                        echo "Creating BE/miscfunc database @ ", `date`
                        echo ""
                        destroydb msfndb >>$TST_OUTPUT/beinit.out
                        createdb msfndb >>$TST_OUTPUT/beinit.out
                        if [ $? != 0 ]
                        then
                          echo Creation of Backend database msfndb Failed.
                          echo See $TST_OUTPUT/beinit.out for error messages.
                          exit 1
                        fi
                fi

#
# LAR
		if [ "$fac" = "all" -o "$fac" = "lar" ]
		then

		   if [ ! -d $TST_OUTPUT/lar ]
			then
			  echo "Creating Directory - $TST_OUTPUT/lar"
   		          mkdir $TST_OUTPUT/lar
		   fi


			echo "Initializing BE/LAR database @ ", `date`
			
			SEPPARAMDB=lardb
			export SEPPARAMDB

			sep -b -v $TST_INIT/initlar.sep -o$TST_OUTPUT/lar/initlar.out
			
			if [ $? != 0 ]
			then
			  echo Creation of Backend database lardb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
		fi
#
# QRYPROC
		if [ "$fac" = "all" -o "$fac" = "qryproc" ]
		then

		   if [ ! -d $TST_OUTPUT/qryproc ]
			then
                          echo "Creating Directory - $TST_OUTPUT/qryproc"
			  mkdir $TST_OUTPUT/qryproc
		   fi

			echo "Creating BE/QryProc database @ ", `date`
			echo ""
			destroydb qryprocdb >>$TST_OUTPUT/beinit.out
			createdb qryprocdb >>$TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database qryprocdb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
			destroydb qryprocunidb >>$TST_OUTPUT/beinit.out
			createdb -n qryprocunidb >>$TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database qryprocdb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
		fi

#
# UTIL
		if [ "$fac" = "all" -o "$fac" = "util" ]
		then

		   if [ ! -d $TST_OUTPUT/util ]
			then
			  echo "Creating Directory - $TST_OUTPUT/util"
  		          mkdir $TST_OUTPUT/util
		   fi

			echo "Destroying BE/UTILITY databases @ ", `date`
			echo ""
			destroydb utildb >>$TST_OUTPUT/beinit.out
			echo "Creating BE/UTILITY databases @ ", `date`
                        echo ""
                        destroydb utildb3 >>$TST_OUTPUT/beinit.out
                        createdb utildb3 >>$TST_OUTPUT/beinit.out
                        if [ $? != 0 ]
                        then
                          echo Creation of Backend database utildb3 Failed.
                          echo See $TST_OUTPUT/beinit.out for error messages.
                          exit 1
                        fi
                        echo "Creating BE/UTILITY database @ ", `date`
                        echo ""
                        destroydb utildb4 >>$TST_OUTPUT/beinit.out
                        createdb utildb4 >>$TST_OUTPUT/beinit.out
                        if [ $? != 0 ]
                        then
                          echo Creation of Backend database utildb4 Failed.
                          echo See $TST_OUTPUT/beinit.out for error messages.
                          exit 1
                        fi
                        echo "Initializing BE/UTIL database @ ", `date`

                        SEPPARAMDB3=utildb3
                        export SEPPARAMDB3

                        sep -b -v $TST_INIT/initutil.sep -o$TST_OUTPUT/util/initutil.out
		fi
#
# BLOB
		if [ "$fac" = "all" -o "$fac" = "blob" ]
		then

		   if [ ! -d $TST_OUTPUT/blob ]
			then
			  echo "Creating Directory - $TST_OUTPUT/blob"
  		          mkdir $TST_OUTPUT/blob
		   fi

			echo "Creating BE/BLOB database @ ", `date`
			echo ""
			destroydb blobdb >>$TST_OUTPUT/beinit.out
			createdb blobdb >>$TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database blobdb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
		fi
#
# ALTTBL 
		if [ "$fac" = "all" -o "$fac" = "alttbl" ]
		then

		   if [ ! -d $TST_OUTPUT/alttbl ]
			then
			  echo "Creating Directory - $TST_OUTPUT/alttbl"
  		          mkdir $TST_OUTPUT/alttbl
		   fi

			echo "Creating BE/ALTER TABLE database @ ", `date`
			echo ""
			destroydb alttbldb >>$TST_OUTPUT/beinit.out
			createdb -n alttbldb >>$TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database alttbldb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
		fi

#
# TTPP
		if [ "$fac" = "all" -o "$fac" = "ttpp" ]
		then

		   if [ ! -d $TST_OUTPUT/ttpp ]
			then
			  echo "Creating Directory - $TST_OUTPUT/ttpp"
  		          mkdir $TST_OUTPUT/ttpp
		   fi

			echo "Creating BE/TTPP database @ ", `date`
			echo ""
			destroydb ttppdb >>$TST_OUTPUT/beinit.out
 			createdb ttppdb >>$TST_OUTPUT/beinit.out
			
			if [ $? != 0 ]
			then
			  echo Creation of Backend database ttppdb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.

		       	exit 1
			fi
		fi

#
# C2SECURE
		if [ "$fac" = "all" -o "$fac" = "c2secure" ]
		then
		   
		   if [ ! -d $TST_OUTPUT/c2secure ]
			then
			  echo "Creating Directory - $TST_OUTPUT/c2secure"
			  mkdir $TST_OUTPUT/c2secure
		   fi

			echo "Creating BE/C2SECURE database @ ", `date`
			echo ""
			destroydb securedb >>$TST_OUTPUT/beinit.out
			createdb securedb >>$TST_OUTPUT/beinit.out

qawtl "TURNING C2 SECURITY AUDITING ON THEN STOPPING AND RESTARTING INGRES"

			SERVER_HOST=`iigetres ii."*".config.server_host` || \
			{ echo "ERROR 1"; exit 1 ; }

			echo ""
			echo "Turning C2 Security Auditing ON"
			echo ""
			qasetuser ingres iisetres ii."*".c2.security_auditing ON
			qasetuser ingres ingstop 
			sleep 5
			qasetuser ingres ingstart
			sleep 5

			if [ $? != 0 ]
			then
			  echo Creation of Backend database securedb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
		fi
		
#
#FASTLOAD
#
		if [ "$fac" = "all" -o "$fac" = "fastload" ]
		then

		   if [ ! -d $TST_OUTPUT/fld ]
			then
			echo "Creating Directory - $TST_OUTPUT/fld"
			mkdir $TST_OUTPUT/fld
		   fi

			echo "Creating BE/FASTLOAD database @ ", `date`
			echo ""
			destroydb fastloaddb >> $TST_OUTPUT/beinit.out
			createdb fastloaddb >> $TST_OUTPUT/beinit.out
			if [ $? != 0 ]
			then
			  echo Creation of Backend database fastloaddb Failed.
			  echo See $TST_OUTPUT/beinit.out for error messages.
			  exit 1
			fi
		fi
      done
    
    echo Backend databases are created and ready for testing.
fi

#------------------------------------------------------------------------------
#			End of Initialization
#------------------------------------------------------------------------------

#------------------------------------------------------------------------------
#			Main Work Area
#------------------------------------------------------------------------------
#
# Run the Backend Sep Test Modules.
#
if [ "$work" = "be" ]
then
	   if grep -q \
	     "ii\.${hostname}\..*\.cache\.p[123468][246]*k_status: OFF" \
	     $II_SYSTEM/ingres/files/config.dat
           then
             echo " "
             echo " ********************************************************* "
             echo "                          ERROR "
             echo "   Some of the cache sizes were not turned ON."
             echo "   Check CBF and make sure that cache is ON for every page."
             echo "        Program terminated...                              "
             echo " ********************************************************* "
             echo ""
             exit 1
	   fi

 	for fac in $*
	do 

# check tolerance/DIF level overrides first

	case $fac in

            -d*)
                diflvl=`echo $fac|cut -b3-|egrep '^[0-9]*$'`
		;;

            -t*)
                joblvl=`echo $fac|cut -b3-|egrep '^[0-9]*$'`
		;;

	esac

		if [ "$fac" = "all" -o "$fac" = "access" ]
		then
#
# Run the ACCESS tests
#
			SEPPARAMDB=accessdb1
			export SEPPARAMDB
			SEPPARAMDRIVERDB=-daccessdb1
			export SEPPARAMDRIVERDB
	
			AREA=access
			CFG_FILE=beam.cfg
			LIS_FILE=beam.lis
			OUT_FILE=beam.out
			export AREA

		echo "Running the ACCESS tests @ ", `date`
		echo ""
		qawtl RUNNING BE/ACCESS TESTS

			doit;
		fi
#

		if [ "$fac" = "all" -o "$fac" = "datatypes" ]
		then
#
# Run the DATATYPES tests
#
			SEPPARAMDB1=datatypedb
			SEPPARAMDB2=datatypedb2
			SEPPARAMDB=datatypedb2
                        SEPPARAMDB3=unidb
			export SEPPARAMDB1 SEPPARAMDB2 SEPPARAMDB SEPPARAMDB3
		
			AREA=datatypes
			CFG_FILE=bedt.cfg
			LIS_FILE=bedt.lis
			OUT_FILE=bedt.out
			export AREA

		echo "Running the DATATYPES tests @ ", `date` 
		echo ""
		qawtl RUNNING BE/DATATYPES TESTS
			doit;
		fi
#

                if [ "$fac" = "all" -o "$fac" = "datetime" ]
                then
#
# Run the DATETIME tests
#
                        SEPPARAMDB=datetimedb
                        export SEPPARAMDB

                        AREA=datetime
                        CFG_FILE=bedatetime.cfg
                        LIS_FILE=bedatetime.lis
                        OUT_FILE=bedatetime.out
                        export AREA

                echo "Running the DATETIME tests @ ", `date`
                echo ""
                qawtl RUNNING BE/DATETIME TESTS
                        doit;
                fi

                if [ "$fac" = "all" -o "$fac" = "miscfunc" ]
                then
#
# Run the MISCFUNC tests
#
                        SEPPARAMDB=msfndb
                        export SEPPARAMDB

                        AREA=miscfunc
                        CFG_FILE=bemsfn.cfg
                        LIS_FILE=bemsfn.lis
                        OUT_FILE=bemsfn.out
                        export AREA

                echo "Running the MISCFUNC tests @ ", `date`
                echo ""
                qawtl RUNNING BE/MISCFUNC TESTS
                        doit;
                fi

		if [ "$fac" = "all" -o "$fac" = "qryproc" ]
		then
#
# Run the QRYPROC tests
#
			SEPPARAMDB=qryprocdb
			SEPPARAMDB3=qryprocunidb
			SEPPARAMDRIVERDB=-dqryprocdb
			TST_TESTOOLS=$ING_TST/testtool
			export SEPPARAMDB SEPPARAMDB3 SEPPARAMDRIVERDB
			export TST_TESTOOLS
		
			AREA=qryproc
			CFG_FILE=beqp.cfg
			LIS_FILE=beqp.lis
			OUT_FILE=beqp.out
			export AREA

		echo "Running the QRYPROC tests @ ", `date` 
		echo ""
		qawtl RUNNING BE/QRYPROC TESTS
			doit;
		fi
#

		if [ "$fac" = "all" -o "$fac" =  "accntl" ]
		then
#
# Run the ACCNTL tests
#
			SEPPARAMDB=accntldb
			SEPPARAMDB3=grantdb
			SEPPARAMDRIVERDB=-dgrantdb
			TST_TESTOOLS=$ING_TST/testtool
			export SEPPARAMDB SEPPARAMDB3 
			export SEPPARAMDRIVERDB TST_TESTOOLS
		
			AREA=accntl
			CFG_FILE=beacc.cfg
			LIS_FILE=beacc.lis
			OUT_FILE=beacc.out
			export AREA

		echo "Running the ACCNTL tests @ ", `date`
		echo ""
		qawtl RUNNING BE/ACCNTL TESTS
			doit;
		fi
#

                if [ "$fac" = "all" -o "$fac" =  "api" ]
                then
#
# Run the API tests
#
                        AREA=api
                        CFG_FILE=beapi.cfg
                        LIS_FILE=beapi.lis
                        OUT_FILE=beapi.out
                        export AREA
			SEPPARAMDB=apidb2
                        export SEPPARAMDB
 
                	echo "Running the API tests @ ", `date`
	                echo ""
			qawtl RUNNING BE/API TESTS
                        doit;
                fi

		if [ "$fac" = "all" -o "$fac" = "util" ]
		then
#
# Run the UTIL tests
#
        		SEPPARAMDB=utildb
                        SEPPARAMDB3=utildb3
                        SEPPARAMDB4=utildb4
        		export SEPPARAMDB SEPPARAMDB3 SEPPARAMDB4
 	
			AREA=util
			TST_DATA=$ING_TST
			CFG_FILE=beutil.cfg
			LIS_FILE=beutil.lis
			OUT_FILE=beutil.out
			export AREA TST_DATA      

		echo "Running the BE Utility tests @ ", `date`
		echo ""
		qawtl RUNNING BE/UTILITY TESTS
			doit;
		fi
#

		if [ "$fac" = "all" -o "$fac" = "blob" ]
		then
#
# Run the BLOB tests
#
        		SEPPARAMDB=blobdb
        		export SEPPARAMDB
 	
			AREA=blob
			TST_DATA=$ING_TST
			CFG_FILE=beblob.cfg
			LIS_FILE=beblob.lis
			OUT_FILE=beblob.out
			export AREA TST_DATA      

		echo "Running the BE Blob tests @ ", `date`
		echo ""
		qawtl RUNNING BE/BLOB TESTS
			doit;
		fi
#

		if [ "$fac" = "all" -o "$fac" = "alttbl" ]
		then
#
# Run the ALTTBL tests
#
        		SEPPARAMDB=alttbldb
        		export SEPPARAMDB
 	
			AREA=alttbl
			CFG_FILE=bealttbl.cfg
			LIS_FILE=bealttbl.lis
			OUT_FILE=bealttbl.out
			export AREA       

		echo "Running the BE Alter Table tests @ ", `date`
		echo ""
		qawtl RUNNING BE/ALTER TABLE TESTS
			doit;
		fi
#
		if [ "$fac" = "all" -o "$fac" = "ttpp" ]
		then
#
# Run the TTPP tests
#
        		SEPPARAMDB=ttppdb
        		export SEPPARAMDB
 	
			AREA=ttpp
			CFG_FILE=bettpp.cfg
			LIS_FILE=bettpp.lis
			OUT_FILE=bettpp.out
			export AREA       

		echo "Running the BE TTPP tests @ ", `date`
		echo ""
		qawtl RUNNING BE/TTPP TESTS
			doit;
		fi
#
		if [ "$fac" = "all" -o "$fac" = "fastload" ]
		then
#
# Run the FASTLOAD tests
#
			SEPPARAMDB=fastloaddb
			export SEPPARAMDB

			AREA=fld
			CFG_FILE=befld.cfg
			LIS_FILE=befld.lis
			OUT_FILE=befld.out
			export AREA

		echo "Running the BE FASTLOAD tests @ ", `date`
		echo ""
		qawtl RUNNING BE/FASTLOAD TESTS
			doit;

		fi
#
		if [ "$fac" = "all" -o "$fac" = "lar" ]
		then
#
# Run the LAR tests
#
			SEPPARAMDB=lardb
			export SEPPARAMDB
	
			AREA=lar
			CFG_FILE=belar.cfg
			LIS_FILE=belar.lis
			OUT_FILE=belar.out
			export AREA

		echo "Running the LAR tests @ ", `date`
		echo ""
		qawtl RUNNING BE/LAR TESTS
			doit;
		fi
#
	if [ "$fac" = "all" -o "$fac" = "c2secure" ]
	then
#
# Run the C2SECURE tests
#
        		SEPPARAMDB=securedb
        		export SEPPARAMDB
 	
			AREA=c2secure
			CFG_FILE=bec2.cfg
			LIS_FILE=bec2.lis
			OUT_FILE=bec2.out
			export AREA       



		echo "Running the BE C2 Secure tests @ ", `date`
		echo ""
		qawtl RUNNING BE/C2SECURE TESTS
			doit;
		fi

	done
#
	echo Backend tests are complete at `date` 
#
fi
#---------------------------------------------------------------------------
#			Cleanup Area
#---------------------------------------------------------------------------
#
# Start the cleanup of Backend Databases
#
if [ "$work" = "clean" ]
then 
       	if [ "$*" = "" ]
	then
	     echo "You must input the modules to be cleaned up "
	     echo "or specify ""all""."
	     echo ""
	     echo " Example: sh $TST_SHELL/runbe.sh clean all "
	     echo ""
	     echo "          or "
	     echo ""
	     echo "          sh $TST_SHELL/runbe.sh clean access util "
	     echo ""

	     exit 1
        else	

	     for fac in $*
	     do

#
# ACCESS
		if [ "$fac" = "all" -o "$fac" = "access" ]
		then 

			echo "Destroying BE/Access database @ ", `date`
			echo ""
			destroydb accessdb1 >>$TST_OUTPUT/beclean.out
			AREA=access
			export AREA
		fi
#
# ACCNTL
		if [ "$fac" = "all" -o "$fac" = "accntl" ]
		then

			echo "Destroying BE/Accntl databases @ ", `date`
			echo ""
			destroydb accntldb >>$TST_OUTPUT/beclean.out
			qasetuser pvusr1 destroydb grantdb >>$TST_OUTPUT/beclean.out
			AREA=accntl
			export AREA
		fi
#
# API
#

		if [ "$fac" = "all" -o "$fac" = "api" ]
		then

			echo "Destroying BE/Api databases @ ", `date`
			echo ""
			destroydb apidb2 >>$TST_OUTPUT/beclean.out
			destroydb apidb1 >>$TST_OUTPUT/beclean.out
                        AREA=api
                        export AREA
                fi

#
# DATETIME 
		if [ "$fac" = "all" -o "$fac" = "datetime" ]
		then 

			echo "Destroying BE/Datetime database @ ", `date`
			echo ""
			destroydb datetimedb >>$TST_OUTPUT/beclean.out
			AREA=datetime
			export AREA
		fi
#
# DATATYPES
		if [ "$fac" = "all" -o "$fac" = "datatypes" ]
		then

			echo "Destroying BE/DataTypes dbases @ ",`date`
			echo ""
			destroydb datatypedb >>$TST_OUTPUT/beclean.out
			destroydb datatypedb2 >>$TST_OUTPUT/beclean.out
                        destroydb unidb >>$TST_OUTPUT/beclean.out
			AREA=datatypes
			export AREA
		fi
#
# MISCFUNC
		if [ "$fac" = "all" -o "$fac" = "miscfunc" ]
		then

			echo "Destroying BE/Miscellaneous Function dbases @ ",`date`
			echo ""
			destroydb msfndb >>$TST_OUTPUT/beclean.out
                        sep -b -v $TST_INIT/cleanmsfn.sep -o$TST_OUTPUT/miscfunc/cleanmsfn.out
			AREA=miscfunc
			export AREA
		fi
#
# LAR
		if [ "$fac" = "all" -o "$fac" = "lar" ]
		then

			echo "Destroying BE/LAR databases @ ", `date`
                        echo ""
			destroydb lardb >>$TST_OUTPUT/beclean.out
			destroydb lar44db >>$TST_OUTPUT/beclean.out
			destroydb lar45db >>$TST_OUTPUT/beclean.out
			destroydb lar58db >>$TST_OUTPUT/beclean.out
			destroydb lar98db >>$TST_OUTPUT/beclean.out
			destroydb lar11_ckp_table >>$TST_OUTPUT/beclean.out
			destroydb lar_ckp_table >>$TST_OUTPUT/beclean.out
			destroydb b77231db >>$TST_OUTPUT/beclean.out
			destroydb b75662db >>$TST_OUTPUT/beclean.out
                        sep -b -v $TST_INIT/cleanlar.sep -o$TST_OUTPUT/lar/cleanlar.out
			AREA=lar
			export AREA
		fi
#
# QRYPROC
		if [ "$fac" = "all" -o "$fac" = "qryproc" ]
		then

			echo "Destroying BE/QryProc databases @ ", `date`
                        echo ""
			destroydb qryprocdb >>$TST_OUTPUT/beclean.out
			destroydb qryprocunidb >>$TST_OUTPUT/beclean.out
			qasetuser ingres destroydb eia40db >>$TST_OUTPUT/beclean
			AREA=qryproc
			export AREA
		fi
#
# UTIL
		if [ "$fac" = "all" -o "$fac" = "util" ]
		then

			echo "Destroying BE/UTILITY databases @ ", `date`
			echo ""
			destroydb utildb >>$TST_OUTPUT/beclean.out
			destroydb utildb3 >>$TST_OUTPUT/beclean.out
                        destroydb utildb4 >>$TST_OUTPUT/beclean.out
                        destroydb exdb >>$TST_OUTPUT/beclean.out
                        AREA=util
			export AREA
		fi
#
# BLOB
		if [ "$fac" = "all" -o "$fac" = "blob" ]
		then

			echo "Destroying BE/BLOB database @ ", `date`
			echo ""
			destroydb blobdb >>$TST_OUTPUT/beclean.out
			AREA=blob
			export AREA
		fi
#
# ALTTBL 
		if [ "$fac" = "all" -o "$fac" = "alttbl" ]
		then

			echo "Destroying BE/ALTABLE databases @ ", `date`
			echo ""
			destroydb alttbldb >>$TST_OUTPUT/beclean.out
			destroydb alttbl35db >>$TST_OUTPUT/beclean.out
			AREA=alttbl
			export AREA
		fi

#
# TTPP
		if [ "$fac" = "all" -o "$fac" = "ttpp" ]
		then

			echo "Destroying BE/TTPP database @ ", `date`
			echo ""
			destroydb ttppdb >>$TST_OUTPUT/beclean.out
			AREA=ttpp
			export AREA
		fi

#
# C2SECURE
		if [ "$fac" = "all" -o "$fac" = "c2secure" ]
		then

			echo "Destroying BE/C2SECURE database @ ",`date`
			echo ""
			destroydb securedb >>$TST_OUTPUT/beclean.out
			AREA=c2secure
			export AREA

qawtl "TURNING C2 SECURITY AUDITING OFF THEN STOPPING AND RESTARTING INGRES"

		       SERVER_HOST=`iigetres ii."*".config.server_host` || \
		       { echo "ERROR 1"; exit 1 ; }

		       echo ""
		       echo "Turning C2 Security Auditing OFF"
		       echo ""
		       qasetuser ingres iisetres ii."*".c2.security_auditing OFF
		       qasetuser ingres ingstop 
		       sleep 5
		       qasetuser ingres ingstart
		       sleep 5
		fi
		
#
#FASTLOAD
#
		if [ "$fac" = "all" -o "$fac" = "fastload" ]
		then

			echo "Destroying BE/FASTLOAD database @ ",`date`
			echo ""
			destroydb fastloaddb >> $TST_OUTPUT/beclean.out
			AREA=fld
			export AREA
		fi
	    done
        fi
	echo Backend databases are destroyed.
fi

#---------------------------------------------------------------------------
#			End of cleanup
#---------------------------------------------------------------------------
