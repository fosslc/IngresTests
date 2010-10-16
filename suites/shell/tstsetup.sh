#!/bin/sh
# 
#	Copyright (c) 2007 Ingres Corporation
# 
#		Shell Script to Setup Environment for Testing
# 
#
#	History:
#	      03-02-93  (barbh)
#		     Created shell script.
#	      03-aug-1993 (judi)
#			Change runiinamu to iigcfid for 6.5.
#             13-sept-93  (judi)
#                    Added the select version test into this script and remove
#                    it from the getstart.doc.  This makes it automated.
#	      30-sept-93  (judi)	
#		     Remove the test on the users file as this file will
#		     not be maintained for 6.5.  Add new test for users 
#		     which executes a sep test which run accessdb and lists
#		     the users.
#	      25-feb-94	(garys)
#		     Added install and install/rting sub directories to the
#		     ING_TST/output directory creation for loop.
#		28-may-94 (vijay)
#	        Misc. improvements and updates. This script now contains almost
#		all the stuff mentioned in the getstart.doc.
#		22-Aug-95 (wooke01)
#			Added line to change number of locks per transaction.	
#			This is needed by the performance tests.
#			Line added was: 
#			   iisetres ii.$SERVER_HOST.rcp.lock.per_tx_limit 320
#			Also added message telling user to add line into
#			config.dat to allow testenv to perform
#			loopback tests.  Made 28-May-94 comments standard
#			layout.
#		23-Aug-95 (wooke01)
#			changed iisetres ii.$SERVER_HOST.lock.per_tx_limit 320
#			to ii.$SERVER_HOST.rcp.lock.per_tx_limit 320
#
#		24-Aug-95 (wooke01)
#			Had to rollback the change made on 22 August.  The reason
#			for this is that there are many derived parameters from
#			changing number of locks per transaction.  It will be
#			difficult to tell what these parameters are so it has
#			been decided that it is easier to  tell the user to
#			do this manually through cbf.  Also updated info re:
#			line to add to config.dat for lbnet tests.
#		14-Sep-95 (wooke01)
#			Accidentally submitted script that echosed the same 
#			message twice (re:editing config.dat for loopback).
#			Also deleted line that was commented out before
#			regarding setting locks per tx using iisetres.
#	20-Sep-95 (wadag01)
#		Changed the OUTPUT initialisation to align with other
#		run scripts to:
#				OUTPUT=$TST_ROOT_OUTPUT
#		instead of =OUTPUT=$TST_ROOT_OUTPUT/output
#		This change is intentional and has been made in most of
#		the .sh files.
#	28-Sep-95 (wadag01)
#		Sorted out the comments section.
#		Added reminders about setting locks and privileges.
#	24-oct-1997 (walro03)
#		Set tcp-ip listen addresses for the Protocol Bridge to the
#		II_INSTALLATION plus a one.  This is to avoid listen address
#		conflict with lback when netsetup is run.
#	01-Jun-1998 (vande02)
#		Modified the list of directories that will be created by this
#		script for testing output.
#	08-Sep-1998 (vande02)
#		Removed the config.dat modification for turning 
#		"c2.security_auditing" ON because this is the "ingbuild" default
# 		and increased the "c2.max_log_size" to 1200 to help prevent the 
#		audit.1 and audit.2 log files from filling up.
#	15-Sep-1998 (vande02)
#		Added the resetting of Remote Command server to 0.  This server
#		processes is for VDBA and cannot be running if execution of
#		sysmod is expected to be done.
#	25-Mar-1999 (vande02)
#		Adding 'chown to testenv' to the output directories. This shell
#		sets umask to 0 and added itb and rep to output directory list.
#       07-Jun-1999 (vande02)
#               Added calls to qawtl for messages to be written to errlog.log
#	22-Dec-1999 (vande02)
#		Add a chmod for the qawtl executable to have sticky bit on
#	28-Jun-2000 (vande02)
#		Add the DBMS command to increase the max_tuple_length to 32767
#		which is required for be/miscfunc/sep/msfn010 - 015.sep tests.
#	29-Sep-2000 (vande02)
#		Cache_sharing shall be turned OFF for testing.It is not useful
#		to have it on unless you have multiple DBMS servers and 2 CPUS.
#	25-Oct-2000 (vande02)
#		Turning ALL cache sizes on for back-end testing.
#	13-Nov-2000 (vande02)
#		Adding code to test and setup TRUSTED privileges for 'testenv'
#		in config.dat file automatically.  
#
#		The netsetup.sh script to setup testing vnodes for 'testenv'
#		will automatically be executed at the end of this script.
#
#		Added immediate check that 'qasetuser' has root as owner and
#		the correct permissions, if not, tstsetup.sh will exit, if 
#		permissions are correct, tstsetup.sh will continue.
#       06-Jul-2001 (vande02)
#               Changing the private cache to private and changing the rmcmd
#               count to 1.  Rmcmd is needed if this host is used during
#               remote VDBA testing.
#	18-Sep-2001 (toumi01)
#		Init variable "error" to 0 to avoid shell syntax error seen
#		on Linux.
#	28-Feb-2002 (devjo01)
#		When setting up on an Ingres cluster, use local node id
#		as SERVER_HOST, and permit user to optionally skip iimerge
#		and creating the Ingres users.
#     06-May-2001 (shafa01)
#		Added sql statement which will input file rmcmd.sql, in order to
#		give testenv rmcmd privileges.
#     08-Oct-2002 (vande02)
#		Removed the iilink step so most testing will be done with a
#		generic DBMS Server and put iilink setup in runbe.sh/.bat 
#		exclusively for UDT/SOL testing suites which require udt's.
#		Most customers use a generic DBMS Server and not udt's.
#
#		Removed setting shared cache ON and changed the turning of
#		all other large cache sizes to stay with default of 'private'
#		instead of 'shared.' Most testing is done with one DBMS.  Shared
#		cache is required only when doing testing such as CTS with
#		multiple DBMS servers and on machines with enough private memory
#		configured.
#
#		Removed rmcmd count setting to 1 because this is the
#		installation default.
#     28-Jun-2004 (vande02)
#		Removed OpenIngres 1.2 reference, chmod for ciutil, and the
#		creation of 'itb' output directory which are not being used
#		in our testing any longer.
#     14-Sep-2004 (sinra04)
#		Added command to increase opf_memory on DBMS to 320K
#     03-Mar-2005 (wu$ca01)
#		Add command to change max_tuple_length to 32767 on STAR server
#		in order for test sza12.sep to run.
#     28-Mar-2005 (vande02)
#		Removed reference to opensrcqa.csh and handoffqa.csh and added
#		file name tstenv which is the one that actually exists.
#     10-Aug-2006 (rogch01)
#		Force result structure to remain as cheap instead of the new
#		default of heap to prevent a plethora of diffs.  See change
#		480264.  Set DBMS stack size to 400,000 to allow the 3gl/c
#		tests to run clean.
#     15-Sep-2006 (hanal04)
#               Make sure the OUTPUT directory is 777. Don't qasetuser to
#               ingres and chown testenv on the directories because it'll
#               never work.
#       24-Oct-2006 (vande02)
#               Set date_type_alias to ingresdate to ensure compatibility with
#		majority of our tests.  The ansidate will be explicitly used
#               when testing that data type and not be the system default.
#        4-Jun-2007 (vande02)
#		Added defpagesz.sep to verify user table default_page_size is
#		now 8K as of 2006 Release 3 and then reset this parameter to 2K
#		to be compatible with our existing regression suites.
#       26-Jul-2007 (vande02)
#               Removed the date_type_alias setting to ingresdate because this
#               has come to be the default at install time.
#       31-Jul-2007 (vande02)
#		Remove prompts/pauses, and parameterized the LISTEN_ADDRESS
#		to be set to the current 'ingprenv II_INSTALLATION' to enable
#		netsetup.sh to complete without prompts.
#       24-Sep-2007 (vande02)
#		Increasing stack_size to 400,000 on STAR Server so 1024 column
#		tests star/ddl/sep/sta07.sep will run successfully.
#       31-Oct-2007 (hanal04)
#               Set permissions to 777 on ING_ABFDIR or the ABF tests will
#               hang.
#       29-Nov-2007 (vande02)
#               Set date_type_alias to ingresdate to ensure compatibility with
#		majority of our tests.  The ansidate is the expected default
#		during installation for the Icebreaker BI Appliance because
#		Jasper requires it.  This change applies to the opensrcqa
#		branch.
#	04-Aug-2008 (boija02)
#		set_params no longer throws errors on nonexistent params
#	07-Jan-2009 (toumi01)
#		Make sure required on_error and on_log_full c2 security
#		settings are defined.
#	26-Aug-2008 (vande02)
#		Backing out change of 29-Nov-2007.  The correct parameter name
#		is date_alias as of II 9.1.1 and the install default is
#		ingresdate.

# verify qasetuser has correct permissions
# ----------------------------------------
if [ ${TST_TOOLS:-none} = "none" ]; then
  echo "TST_TOOLS not set"
  exit 1
fi

for z in 'qasetuser'
do
  error=0
  owner=`ls -l $TST_TOOLS/$z | awk '{print $3}' `
  if [ $owner != "root" ]; then
    echo "File $z is not owned by root"
    error=1
  fi
  if [ ! -u $TST_TOOLS/$z ]; then
    echo "File $z does not have the setuid bit set"
    error=1
  fi
  if [ ! -x $TST_TOOLS/$z ]; then
    echo "File $z is not executable"
    error=1
  fi
  if [ "$error" -eq 1 ]; then
    echo "File qasetuser MUST be owned by root and have 4755 permissions"
    echo "Please make these corrections as root and execute tstsetup.sh again"
    echo "File qasetuser MUST be in $TST_TOOLS"
    exit 1
  fi
 echo "File $z is set correctly"
done

qasetuser ingres chmod 4755 $TOOLS_DIR/bin/qawtl

qawtl BEGIN TSTSETUP.SH SETUP SCRIPT

. iisysdep
. iishlib

#
# Routine to set installation parameters: set_params(name, value)
# caveat: only increases a limit
#
set_params()
{
    present_val=`iigetres $1`
    if [ "$present_val" = "" ]
    then
        present_val=0
    fi
    if [ "$present_val" -gt $2 ]
    then
	echo "$1 is already set to $present_val : OK"
    else
	iisetres $1 $2
	echo "$1 is now set to $2"
    fi
}

#----------------------------------------------------------------------
# Before doing this, you need to have ...
#----------------------------------------------------------------------
cat << !

	Before you run this script to check your test environment,
	you must have done the following:

	1. Install the test suite, ingres installation, and test tools.
	2. Customized and "sourced" the environment file based on one
	   of the templates - tstbenv.sh
	   The templates are found in $ING_TST/suites/userenv.

	You may rerun this script at any time to check your environment.
!

#----------------------------------------------------------------------
# minor sanity checks: user id, system ..
#----------------------------------------------------------------------
: ${II_SYSTEM?} ${ING_TST?}

echo "Checking effective userid...."
if [ "$WHOAMI" != "ingres" ] 
then
	echo "You must be user 'ingres' to run this setup script"
	echo ""
	exit 2
fi

#
# Disk space for data and testing.
#
echo " "
echo "Checking for disk space"
data=`ingprenv II_DATABASE`
echo "You have `iidsfree $data` KB in your II_DATABASE area."
echo "Maximum space needed by a single test segment is about 200 MB"
echo " "

echo " "
echo "Checking ING_ABFDIR"
abfdir=`ingprenv ING_ABFDIR`
if [ "X$abfdir" != "X" ] && [ -d $abfdir ] ; then
    chmod 777 $abfdir
else
    echo "WARNING: ING_ABFDIR not set or directory not found."
fi
echo " "

#----------------------------------------------------------------------
# Create all the required output directories
#----------------------------------------------------------------------
umask 0
umask

echo "Checking for test output directories..."
echo ""
	if [ "$TST_ROOT_OUTPUT" != "" ]
	then
		OUTPUT=$TST_ROOT_OUTPUT
	else
		OUTPUT=$ING_TST/output
	fi
	export OUTPUT
#
	
	if [ -d $OUTPUT ]
	then
		echo "The output directory $OUTPUT exists, creating subdirs."
		echo ""
		chmod 777 $OUTPUT
	else 
		echo "The output directory $OUTPUT does not exist, creating "
		echo "$OUTPUT. "
		echo ""
		mkdir $OUTPUT
		chmod 777 $OUTPUT
	fi
		echo "Creating $OUTPUT sub directories. "
		echo ""
#
	for outdir in net net/lback fe fe/local star star/phase1 be rep
		do
			outdirl=$OUTPUT/$outdir
			if [ -d $outdirl ]
			then :
			else
				if mkdir $outdirl
				then 
				  echo "Created test output directory $outdirl"
				  chmod 777 $outdirl
				  echo " "
				else 
				  echo "Can't create output directory $outdirl"
				fi
			fi
		done
	echo ""
	echo "Finished creating all output and sub directories."
	echo ""

#----------------------------------------------------------------------
# start the server and verify the default_page_size is 8K
#----------------------------------------------------------------------
echo " "
echo " Starting the server to verify the correct user table default page size "
echo " "
ingstart

echo " "
echo "Executing SQL (via SEP) to verify correct user table default_page_size"
echo " "
echo sep $ING_TST/suites/userenv/defpagesz.sep
echo " "
sep -b $ING_TST/suites/userenv/defpagesz.sep
echo " "

#----------------------------------------------------------------------
# Set some server parameters.
#----------------------------------------------------------------------
CLUSTER=false
[ "1X" = "`ingprenv II_CLUSTER`X" ] && CLUSTER=true
if $CLUSTER
then
    SERVER_HOST=`iipmhost`
else
    SERVER_HOST=`iigetres ii."*".config.server_host` || \
     { echo "ERROR 1"; exit 1 ; }
fi
set_params ii.$SERVER_HOST.gcn.session_limit 26
set_params ii.$SERVER_HOST.star."*".connect_limit 26

echo " "
echo "Turning ALL cache sizes ON"
echo " "
iisetres ii.$SERVER_HOST.dbms.private.*.cache.p4k_status ON
iisetres ii.$SERVER_HOST.dbms.private.*.cache.p8k_status ON
iisetres ii.$SERVER_HOST.dbms.private.*.cache.p16k_status ON
iisetres ii.$SERVER_HOST.dbms.private.*.cache.p32k_status ON
iisetres ii.$SERVER_HOST.dbms.private.*.cache.p64k_status ON
echo " "
echo "Increasing max_tuple_length on DBMS to 32767"
echo " "
iisetres ii.$SERVER_HOST.dbms."*".max_tuple_length 32767
echo " "
echo "Increasing max_tuple_length on STAR to 32767"
echo " "
iisetres ii.$SERVER_HOST.star."*".max_tuple_length 32767
echo " "
echo "Increasing opf_memory on DBMS to 322870400"
echo " "
iisetres ii.$SERVER_HOST.dbms."*".opf_memory 322870400
echo " "
echo "Setting result_structure on DBMS to cheap"
echo " "
iisetres ii.$SERVER_HOST.dbms.*.result_structure cheap
echo " "
echo "Setting stack_size on DBMS to 400,000"
echo " "
iisetres ii.$SERVER_HOST.dbms.*.stack_size 400000
echo " "
echo "Setting stack_size on STAR to 400,000"
echo " "
iisetres ii.$SERVER_HOST.star.*.stack_size 400000
echo " "
echo "Setting default_page_size on DBMS to 2048"
echo " "
iisetres ii.$SERVER_HOST.dbms.*.default_page_size 2048
echo " "
echo "Setting force abort limit to 80"
echo " "
iisetres ii.$SERVER_HOST.rcp.log.force_abort_limit   80
echo " "
echo "Some C2 Security Auditing Configuration"
echo " "
iisetres ii."*".c2.max_log_size 1200
iisetres ii."*".c2.audit_log_1 $II_SYSTEM/ingres/files/audit.1
iisetres ii."*".c2.audit_log_2 $II_SYSTEM/ingres/files/audit.2
iisetres ii."*".c2.on_error STOPAUDIT
iisetres ii."*".c2.on_log_full SUSPEND
echo "Setting Protocol Bridge listen addresses"
echo " "
iisetres ii.$SERVER_HOST.gcb."*".tcp_ip.port        `ingprenv II_INSTALLATION`1
iisetres ii.$SERVER_HOST.gcb."*".tcp_ip.port.vnode  `ingprenv II_INSTALLATION`1

echo

# verify testenv config.dat entry
# -----------------------------------------------------------------------------
echo ' '
v="ii.$SERVER_HOST.privileges.user.testenv"
w="ii.$SERVER_HOST.privileges.user.ingres"
for z in a b
do
	x=`iigetres $v`
	if [ $? -ne 0 ]
	then
		echo '======= iigetres error attempting to determine testenv privileges'
		exit 9
	fi
	if [ -z "$x" ]
        then
                if [ $z = 'a' ] 
		then
                        echo 'testenv privileges not defined in config.dat'
                        echo 'Attempting to define testenv privileges'
			x=`iigetres $w`
			if [ $? -eq 0 -a -n $x ]
			then
				iisetres ii.$SERVER_HOST.privileges.user.testenv $x
			else
                       		echo '!!!!!!! attempt to obtain ingres privileges failed'
				exit 9
			fi
                else
                        echo '!!!!!!! attempt to define testenv privileges failed'
                        exit 9
                fi
	else
                echo 'testenv privileges in config.dat have been set to: ' `echo $x`
                break
        fi
done

echo " "

echo " "
echo " Stopping the server now "
ingstop
echo " "

setit=true
if $CLUSTER
then
    cat << !

    In a clustered installation, running TSTSETUP on another node
    will have already set up the necessary Ingres user accounts, so rerunning
    the step to create user accounts may be optional.

!
fi

if $setit
then

#----------------------------------------------------------------------
# start the server and set up users
#----------------------------------------------------------------------
echo " "
echo " Starting the server to set up the users in this installation "
echo " "
ingstart
echo " "
echo "sql iidbdb < users.sql"
sql iidbdb < $ING_TST/suites/userenv/users.sql > /dev/null

else
    # Need to stop and start anyway to pick up new settings.
    ingstart
fi # setit

#
# make sure users are setup on the OS
#
for i in pvusr1 pvusr2 pvusr3 pvusr4 qatest
do
    if ( grep $i /etc/passwd > /dev/null 2>&1 )
    then
	:
    else
	echo " "
	echo " Login as root and create user $i on the system "
    fi
done

#----------------------------------------------------------------------
# Does the installation have all the users ?
#----------------------------------------------------------------------

echo " "
echo "Executing accessdb (via sep) to verify for correct users"
echo sep $ING_TST/suites/userenv/users.sep
echo " "
sep -b $ING_TST/suites/userenv/users.sep

#----------------------------------------------------------------------
# Giving testenv rmcmd privileges
#----------------------------------------------------------------------

echo " "
echo "Giving testenv rmcmd privileges"
sql imadb < $ING_TST/suites/userenv/rmcmd.sql

#----------------------------------------------------------------------
# Verify version
#----------------------------------------------------------------------

echo " "
echo "VERIFY THAT YOU HAVE THE CORRECT INGRES VERSION"
echo " "
echo " "
echo 'select _version()\g' | sql iidbdb
echo " "
#----------------------------------------------------------------------
# net setup using the netsetup.sh script
#----------------------------------------------------------------------
echo " "
echo "About to setup vnodes as testenv required for testing "
HOSTNAME=`uname -n`
LISTEN_ADDRESS=`ingprenv II_INSTALLATION`
cat << ! | qasetuser testenv sh $TST_SHELL/netsetup.sh
$HOSTNAME
ca-testenv
hetnode
$LISTEN_ADDRESS
y
!
#----------------------------------------------------------------------
# message about raw log
#----------------------------------------------------------------------
cat << !

	If you haven't done so already, login as root and 
	create a rawlog for your installation if you wish to
	run the tests with a rawlog. Use "mkrawlog" to create
	the raw log.

	
	IF YOUR SYSTEM HAS SHADOW PASSWORDS, REMEMBER TO RUN
	'mkvalidpw' AS ROOT.

!

#----------------------------------------------------------------------
# Looks like the tests may run okay....
#----------------------------------------------------------------------

	echo "Finished checking test environment."
qawtl END TSTSETUP.SH SETUP SCRIPT
