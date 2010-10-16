# Copyright (c) 2007 Ingres Corporation
#
#           BASH Environment Variables and Paths Testing Icebreaker
#
# 12-Apr-2007 (vande02) Created
# 
# Edit this file and customize as needed.
#
# Be sure to search for < .. > for values you need to fill in for
# JDBC, ODBC, and optional Language testing.
#
# Source this script to set up the Ingres test environment
#
# 29-Sep-2008 (vande02) Added the ING_CHARSET variable to enble UTF8 testing.
#
#=======================================================================
#           GENERAL INGRES SYSTEM SETTINGS FOR TEST ENVIRONMENT
#=======================================================================
# Root directory of Ingres instance to use for the test environment
II_SYSTEM=/opt/Ingres/IngresII;		export II_SYSTEM
# Root directory for test suites
ING_TST=/qa1/tst;			export ING_TST
# Root directory for test tools
ING_TOOLS=/qa1/tools;			export ING_TOOLS
# Root directory for test output
TST_OUTPUT=/qa1/output/2006r2;		export TST_OUTPUT
# Directory holding test lists
TST_LISTEXEC=$ING_TST/suites/acceptst;  export TST_LISTEXEC
# Where Ingres should get configuration info.
II_CONFIG=$II_SYSTEM/ingres/files;	export II_CONFIG
# Two digit year above this value are in the previous century.
II_DATE_CENTURY_BOUNDARY=10;		export II_DATE_CENTURY_BOUNDARY
# On BSD change this to /usr/ucb/vi
ING_EDIT=/usr/bin/vi;			export ING_EDIT
# Set to character set under test
ING_CHARSET=< .. >;			export ING_CHARSET

#=======================================================================
# SHARED LIBRARY PATH SETTINGS 
#=======================================================================
# Example for Linux
LD_LIBRARY_PATH="/lib:/usr/lib:$II_SYSTEM/ingres/lib" ; export LD_LIBRARY_PATH

#=======================================================================
#                     ING_TOOLS TEST SETTINGS
#=======================================================================
TOOLS_DIR=$ING_TOOLS;			export TOOLS_DIR
TST_TOOLS=$ING_TOOLS/bin;		export TST_TOOLS
TST_TESTOOLS=$ING_TST/testtool;		export TST_TESTOOLS
TST_SEP=$ING_TOOLS/files;		export TST_SEP
PEDITOR=$TST_TOOLS/peditor;		export PEDITOR

#=======================================================================
#                      DERIVED TEST SETUP SETTINGS
#=======================================================================
TST_ROOT_OUTPUT=$TST_OUTPUT;		export TST_ROOT_OUTPUT
TST_CFG=$TST_LISTEXEC;			export TST_CFG
TST_DOC=$ING_TST/suites/doc;		export TST_DOC
TST_DCL=$ING_TST/suites/dcl;		export TST_DCL
TST_SHELL=$ING_TST/suites/shell;	export TST_SHELL
TST_TESTENV=$ING_TST;			export TST_TESTENV
TST_INIT=$ING_TST/basis/init;		export TST_INIT
TST_DATA=$ING_TST/gcf/gcc/data;		export TST_DATA
REP_TST=$ING_TST;			export REP_TST

#=======================================================================
#                            SEP SETTINGS
#=======================================================================
# A suitable value for SEP_CMD_SLEEP and SEP_DIFF_SLEEP on a fast machine
# would be 10.  The default within sep of 250 milliseconds will affect
# how fast (or slow) SEP will run. Some platforms cannot diff automated
# tests as fast as other platforms.
#
SEP_CMD_SLEEP=10;			export SEP_CMD_SLEEP
SEP_DIFF_SLEEP=10;			export SEP_DIFF_SLEEP

# Time in seconds before a SEP test times out (hang protection)
SEP_TIMEOUT=600;			export SEP_TIMEOUT

# OS System Type
SEPPARAM_SYSTEM=`uname`;		export SEPPARAM_SYTEM

#=======================================================================
#                          TERMINAL SETTINGS
#=======================================================================
TERM_INGRES=vt100fx;			export TERM_INGRES
TERM=vt100;				export TERM

#=======================================================================
# JDBC SETTINGS MUST BE MODIFIED TO FIT YOUR TESTING
# ENVIRONMENT AND UNCOMMENT THE VARIABLE LINES.
#
# Remove all comments next to variables after setting JDBC variables.
#
# Syntax for the SEPPARAM_URL is:
#       jdbc:ingres://hostname:port#/db_name
#
# Syntax for the SEPPARAMEDBC_URL is:
#       jdbc:edbc://hostname:port#/db_name
#
# Customize the SEPPARAM_URL and the SEPPARAMEDBC_URL parameters as follows:
#       hostname = actual name of test machine
#       port#    = your II_INSTALLATION code + 7 such as II7
#       db_name  = test database for JDBC such as 'jdbcdb'
#
#=======================================================================
# CLASSPATH=.:$II_SYSTEM/ingres/lib/iijdbc.jar:$II_SYSTEM/ingres/lib/edbc.jar ; export CLASSPATH
# SEPPARAM_URL=< .. >  (See syntax example above) ;
# export SEPPARAM_URL
# SEPPARAMEDBC_URL=< .. >  (See syntax example above) ;
# export SEPPARAMEDBC_URL
# SEPPARAMDB=< .. >  (Your test database name,i.e.jdbcdb) ;
# export SEPPARAMDB
# SEPPARAM_JUSER=ingres ; export SEPPARAM_JUSER
# SEPPARAM_JPASSWORD=< .. >  (Your ingres user password) ;
# export SEPPARAM_JPASSWORD

#=======================================================================
# ODBC SETTINGS MUST BE MODIFIED TO FIT YOUR TESTING
# ENVIRONMENT AND UNCOMMENT THE VARIABLE LINES.
#
# Remove all comments next to ODBC variables while setting them.
#=======================================================================
# Full path to the driver manager shared library file, e.g. libodbc.sl
# SEPPARAM_ODBCLIB=< .. > ; export SEPPARAM_ODBCLIB
# Path to your ODBC installation
# ODBC_INSTALL_DIR=< .. >  ; export ODBC_INSTALL_DIR
# ODBCINI=$ODBC_INSTALL_DIR/odbc.ini ; export ODBCINI
# LD_LIBRARY_PATH=$ODBC_INSTALL_DIR/lib:${LD_LIBRARY_PATH} ; export LD_LIBRARY_PATH
# Password of user running tests, probably testenv
# SEPPARAM_OPASSWORD=< .. > ; export SEPPARAM_OPASSWORD

#=======================================================================
# DO NOT MODIFY ANY VARIABLES AFTER THIS POINT UNLESS ADDITIONAL FE3GL
# TESTING FOR OTHER COMPILERS WILL BE EXECUTED - DEPENDING ON THE TYPE
# OF TESTING, YOU MAY HAVE TO UNCOMMENT AND MODIFY SOME LINES SUCH AS
# THE LANGUAGE VARIABLES - READ THE COMMENTS CAREFULLY 
#=======================================================================

#=======================================================================
#                        PROGRAM LANGUAGE SETTINGS
#=======================================================================
# Set up defaults here, make changes for your platform below and
# uncomment the variable lines.
# Be sure to add the paths of all the relevant languages to your PATH
#=======================================================================
# F77=< .. > 		; export F77
# COBDIR=< .. >		; export COBDIR
# Set Cobol linker path if testing cobol.
# SEP_COBOL_LD='II_SYSTEM/ingres/lib/libingres.a -lsocket -lnsl -lm -lc'
# export SEP_COBOL_LD;
# ADA=< .. >		; export ADA
# SEP_ADA_LD=$II_SYSTEM/ingres/lib/libingres.a;	    export SEP_ADA_LD
# SEP_FORTRAN_LD=$II_SYSTEM/ingres/lib/libingres.a; export SEP_FORTRAN_LD
# Does your fortran compiler generate symbols with trailing underscores ?
# SEPPARAM_FORT_UNDER=TRUE; export SEPPARAM_FORT_UNDER
# For AIX < .. >
# F77=xlf                   ; export F77
# SEPPARAM_FORT_UNDER=FALSE ; export SEPPARAM_FORT_UNDER
# COBDIR=/usr/lpp/COBOL     ; export COBDIR
# For HP < .. >
# SEPPARAM_FORT_UNDER=FALSE ; export SEPPARAM_FORT_UNDER
# SEPPARAM_FORT_UNDER=FALSE ; export SEPPARAM_FORT_UNDER
# PATH=$PATH:$COBDIR/bin:$ADA/bin
# export PATH

#=======================================================================
#                          DEBUGGING SETTINGS
#
# Uncomment these variables for debugging then restart INGRES installation.
#=======================================================================
# Location for logs holding extended error information for Ingres
# servers. %p is replaced at runtime with PID of server.
# II_DBMS_LOG=$II_CONFIG/dbms.log;		export II_DBMS_LOG
# Additional GCC tracing information.
# II_GCC_LOG=$II_SYSTEM/ingres/iigcc.log;	export II_GCC_LOG

# Additional GCN tracing information.
# II_GCN_LOG=$II_SYSTEM/ingres/iigcn.log;	export II_GCN_LOG
# I/O slave tracing. Ingres INTERNAL threads only.
# II_SLAVE_LOG=$II_SYSTEM/ingres/ioslave.log;	export II_SLAVE_LOG


#=======================================================================
#                     PATH AND PROMPT TEST SETTINGS
#=======================================================================

export PATH=$II_SYSTEM/ingres/bin:\
$II_SYSTEM/ingres/utility:\
$II_SYSTEM/ingres/lib:\
$PATH:\
$TOOLS_DIR/bin:\
$TOOLS_DIR/utility:\
\.

#  Prompt
[ -r $II_SYSTEM/ingres/bin/ingprenv ] &&\
PS1='\u@\h  `ingprenv II_INSTALLATION`  \t  \w\n\$ ' &&\
PS2='> '
umask 002

#=======================================================================
#                   SET AND DISPLAY INSTALLATION CODE 
#=======================================================================

ii_code=`$II_SYSTEM/ingres/bin/ingprenv II_INSTALLATION`
( [ -r $II_SYSTEM/ingres/version.rel ] && ii_version="`awk '{x=x" "$0};END {print x}' $II_SYSTEM/ingres/version.rel`" ) || ii_version='Cannot read version.rel'

echo
echo ====================================================================
echo  $ii_version Test Installation: $ii_code for Icebreaker
echo ====================================================================

echo Variable settings are ...
echo II_SYSTEM.........$II_SYSTEM
echo ING_TST...........$ING_TST
echo ING_TOOLS.........$ING_TOOLS
echo TERM_INGRES.......$TERM_INGRES
echo ""
echo TST_CFG...........$TST_CFG
echo TST_LISTEXEC......$TST_LISTEXEC
echo TST_OUTPUT........$TST_OUTPUT
echo LD_LIBRARY_PATH...$LD_LIBRARY_PATH
echo =============================================================
