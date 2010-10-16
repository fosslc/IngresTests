#!/bin/sh
#
# History
#    28-may-94 (vijay)
#	Created.
#    07-jun-94 (vijay)
#	Check if the setup is ok, atleast for loopback.
#    24-Aug-95 (wooke01)
#	Changed prompt for vms node for hetnet testing to prompt
#	for VMS/UNIX machine name rather than just vms name.
#	Changed references to hetnode and het_listen to 
#	hetnode and het_listen to be more meaningful.  Netutil 
#	has also been changed accordingly.
#    02-Mar-98 (vande02)
#	Added 'sets up vnode required for Replicator testing.
#    25-Jun-98 (vande02)
#	Added comments to echo's to just press RETURN to prompts referencing
#	HETNET.
#    07-Jun-1999 (vande02)
#       Added calls to qawtl for messages to be written to errlog.log
#
# This script uses userenv/netutil.setup file to setup Ingres/Net for testing 
# Star, Net and MTS and Replicator. 
#
#
#
qawtl BEGIN RUNNING NETUTIL SETUP SCRIPT
. iisysdep
. iishlib

[ "$WHOAMI" != "testenv" ] &&
{
   echo "You should be running this as testenv"
   prompt "Do you want to continue ?" y || exit 1
}

while :
do
  echo "Enter the hostname of the machine you are working on:"
  iiechonn "[ $HOSTNAME ] : "
  read localnode junk
  [ -z "$localnode" ] && localnode=$HOSTNAME

  echo "Enter the current password of testenv:"
  read passwd junk

  echo "Enter hostname of  VMS/UNIX machine (press RETURN if you are NOT running HETNET) for HETNET tests:"
  iiechonn "[ hetnode ] : "
  read hetnode junk
  [ -z "$hetnode" ] && hetnode=hetnode


  echo "Enter listen address of VMS/UNIX machine (press RETURN if you are NOT running HETNET) : II_INSTALLATION:"
  iiechonn "[ II ] : "
  read het_listen junk
  [ -z "$het_listen" ] && het_listen=II

  echo
  echo " local node = $localnode"
  echo " testenv password = $passwd"
  echo " hetnet node = $hetnode"
  echo " hetnet listen address = $het_listen"
  echo
  if prompt "Are the above right"
  then 
	break
  fi
done

listen_address=`ingprenv II_INSTALLATION`

echo " "
echo "Running netutil -file-"

cat $ING_TST/suites/userenv/netutil.setup |
	sed -e "s/<localnode>/$localnode/g
		s/<hetnode>/$hetnode/g
		s/<passwd>/$passwd/g
		s/<listen_address>/$listen_address/g
		s/<het_listen>/$het_listen/g" | netutil -file-

echo " "
echo Checking loopback node
echo sql lback::iidbdb
echo " "
sql lback::iidbdb << !
select _version() \p\g
\q
!
qawtl END NETUTIL SETUP SCRIPT
