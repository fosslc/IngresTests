#!/bin/sh
# 15-Mar-1999 (vande02) Created based on old runbe.sh so that Variable Page
#			sizes and Storage Structures can be tested after
#			converting the sep test VPG_SZ and STR_ST to actual page
#			sizes and storage structures.
# 07-Jun-1999 (vande02) Added calls to qawtl for messages to be written to
#			errlog.log
# 11-jan-2000 (vande02) Checking the return code for each database
#			initialization step, so if the createdb fails, the
#			script will echo a message and exit.
# 31-Mar-1999 (vande02) Corrected usage echo statements under 'init' and added
#			usage echo statements under 'work'.
# 23-aug-2002 (devjo01) Use 'cat' instead of 'more' when generating converted
#			test list, to avoid possibility of hangs due to more
#			waiting for input.
# 12-Apr-2007 (vande02) Modified init step to allow Unicode data types in vpsdb.
#  8-Jul-2008 (vande02) Modified awk command to skip anything in the bevps.lis
#                       file so it won't break over the comment lines.


# ACTUAL WORK IS DONE HERE
doit() {
    if [ "$AREA" = "vps" ]
    then
        if [ -n "$TST_OUTPUT" -a -n "$AREA" -a -d $TST_OUTPUT/$AREA/ ]; then
            eval "SOURCECOUNT=`ls $ING_TST/be/$AREA/sep/*.sep | wc | awk '{print $2}'` "
            echo "Copying" $SOURCECOUNT "SEP files."
            cp $ING_TST/be/$AREA/sep/*.sep $TST_OUTPUT/$AREA/
            eval "DESTCOUNT=`ls $TST_OUTPUT/$AREA/*.sep | wc | awk '{print $2}'` "
            if [ $SOURCECOUNT != $DESTCOUNT ]; then
                echo "Copy failed.  Only" $DESTCOUNT "of" $SOURCECOUNT "files copied."
                exit 1
            fi
        else
            echo "Directory" $TST_OUTPUT/$AREA/ " does not exist."
            exit 1
        fi
        chmod 644 $TST_OUTPUT/$AREA/*.sep
        for n in $PS
        do
            for TABLE in $TS
            do
                PAGE=`expr $n \* 1024`
                export TABLE
                export PAGE
                TST_OUTPUT=$TST_OUTPUT/$AREA/${n}K/${TABLE}
                export TST_OUTPUT
                if [ ! -d $TST_OUTPUT ]; then
                    echo " Will NOT run tests, the output directory does not exist."
                    echo " Program terminated.   "
                    exit 1
                else
                    echo "Running the ${n}K/${TABLE} $AREA Tests"

			qawtl RUNNING BE/VPS TESTS

                    cat $TST_LISTEXEC/$LIS_FILE | awk -F: '/^be*/ {print "sed -e s/VPG_SZ/${PAGE}/g -e s/STR_ST/${TABLE}/g $TST_ROOT_OUTPUT/be/$AREA/"$3" > $TST_OUTPUT/"$3""}' > $TST_OUTPUT/convert

                    sh $TST_OUTPUT/convert

                    echo "executor $TST_CFG/$CFG_FILE > $TST_OUTPUT/$OUT_FILE"	
                    executor $TST_CFG/$CFG_FILE >$TST_OUTPUT/$OUT_FILE
                    rm $TST_OUTPUT/*.sep
                    rm $TST_OUTPUT/convert 
                fi
                TST_OUTPUT=$TST_ROOT_OUTPUT/be
           	    export TST_OUTPUT
            done
        done

        rm $TST_OUTPUT/$AREA/*.sep
                        
    fi

}


doinit() {
#		  AREA=$fac
#		  export AREA
		      for PAGE in $PS
		      do

                       if [ ! -d $TST_OUTPUT/$AREA/${PAGE}K ]
			then
			  echo "Creating Directory - $TST_OUTPUT/$AREA/${PAGE}K"
			  mkdir $TST_OUTPUT/$AREA/${PAGE}K
                       fi

		      for TABLE in $TS
		      do
                          
			if [ ! -d $TST_OUTPUT/$AREA/${PAGE}K/$TABLE ]
			 then
			  echo "Creating Directory - $TST_OUTPUT/$AREA/${PAGE}K/$TABLE"
			  mkdir $TST_OUTPUT/$AREA/${PAGE}K/$TABLE
			fi
		      done
                     done
		       


}
 
umask 2 
umask 

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

PS=$1
shift

if [ ! "$PS" = "all" -a ! "$PS" = "2" -a ! "$PS" = "4" -a ! "$PS" = "8" -a ! "$PS" = "16" -a ! "$PS" = "32" -a ! "$PS" = "64" ]
 then
     echo " "
     echo "  You must set the page size to one of" 
     echo "  2, 4, 8, 16, 32, 64, or all"
     echo " "
     echo "  Syntax:"
     echo "  runbevps.sh pagesize tablestruct init|vps vps"
     echo " "
     exit 1
fi

if [ "$PS" = "all" ]; then
    PS='2 4 8 16 32 64'
fi

TS=$1
shift

if [ ! "$TS" = "heap" -a ! "$TS" = "hash" -a ! "$TS" = "isam" -a ! "$TS" = "btree" -a  ! "$TS" = "all"  ]
 then
     echo " "
     echo "  You must set the table to either" 
     echo "  heap, hash, isam, btree, or all "
     echo " "
     echo "  Syntax:"
     echo "  runbevps.sh pagesize tablestruct init|vps vps"
     echo " "
     exit 1
fi

if [ "$TS" = "all" ]; then
    TS='heap hash isam btree'
fi

work=$1
shift

# Check for init parameter

if [ "$1" = "init" ]
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
# Start the initialization of Backend Databases
#
if [ "$work" = "init" ]
then 
       	if [ "$*" = "" ]
	then
	     echo "You must input the modules to be initalized "
	     echo "or specify ""all""."
	     echo ""
	     echo " Example: sh $TST_SHELL/runbevps.sh 4 all init vps"
	     echo ""
	     echo "          or "
	     echo ""
	     echo "          sh $TST_SHELL/runbevps.sh 2 heap init vps "
	     echo ""

	     exit 1
        else	

	     for fac in $*
	     do
#
# VPS
		if [ "$fac" = "all" -o "$fac" = "vps" ]
		then
			if [ ! -d $TST_OUTPUT/vps ]
			then
			  echo "Creating Directory - $TST_OUTPUT/vps"
  		          mkdir $TST_OUTPUT/vps
			fi

			echo "Creating BE/VPS database @ ", `date`
			echo ""
			destroydb vpsdb >>$TST_OUTPUT/beinit.out
			createdb -n vpsdb >>$TST_OUTPUT/beinit.out
                        if [ $? != 0 ]
                        then
                          echo Creation of Backend database vpsdb Failed.
                          echo See $TST_OUTPUT/beinit.out for error messages.
                          exit 1
                        fi
			AREA=vps
			export AREA
			doinit;
		fi
	    done
        fi
	echo Backend databases are created and ready for testing.
fi

#---------------------------------------------------------------------------
#			End of Initialization
#---------------------------------------------------------------------------

#---------------------------------------------------------------------------
#			Main Work Area
#---------------------------------------------------------------------------
#
# Run the Backend Sep Test Modules.
#
if [ "$work" = "vps" ]
then
     echo "You must input the suite to be tested "
     echo ""
     echo "  You must set the page size to one of"
     echo "  2, 4, 8, 16, 32, 64, or all"
     echo " "
     echo "  You must set the table structure to one of"
     echo "  btree, hash, heap, isam, or all"
     echo " "
     echo "  Syntax:"
     echo "  runbevps.sh pagesize tablestruct vps  vps"
     echo " "

 	for fac in $*
	do 

        if [ "$fac" = "all" -o "$fac" = "vps" ]
        then

# Run the VPS tests
#
            SEPPARAMDB=vpsdb
        	export SEPPARAMDB
 	
			AREA=vps
			CFG_FILE=bevps.cfg
			LIS_FILE=bevps.lis
			OUT_FILE=bevps.out
			export AREA       

			eval 'CACHE4K=`grep "cache.p4k_status" $II_SYSTEM/ingres/files/config.dat | sed "s/^.*: //"` '
			eval 'CACHE8K=`grep "cache.p8k_status" $II_SYSTEM/ingres/files/config.dat | sed "s/^.*: //"` '
			eval 'CACHE16K=`grep "cache.p16k_status" $II_SYSTEM/ingres/files/config.dat | sed "s/^.*: //"` '
			eval 'CACHE32K=`grep "cache.p32k_status" $II_SYSTEM/ingres/files/config.dat | sed "s/^.*: //"` '
			eval 'CACHE64K=`grep "cache.p64k_status" $II_SYSTEM/ingres/files/config.dat | sed "s/^.*: //"` '
			error=0
			for n in $PS
			do
				case $n in
				    4)     if [ "$CACHE4K" = "OFF" ]; then  echo "4K is off"; error=1; fi;;
				    8)     if [ "$CACHE8K" = "OFF" ]; then  echo "8K is off"; error=1; fi;;
				    16)    if [ "$CACHE16K" = "OFF" ]; then  echo "16K is off"; error=1; fi;;
				    32)    if [ "$CACHE32K" = "OFF" ]; then  echo "32K is off"; error=1; fi;;
				    64)    if [ "$CACHE64K" = "OFF" ]; then  echo "64K is off"; error=1; fi;;
				esac
		        done
echo "what is error equal to now $error"
			if [ $error -eq 1 ]; then
			    echo "ERROR The previous PAGE sizes are not turned on"
			    exit 1
			fi
			

		    echo "Running the BE VPS tests @ ", `date`
		    echo ""
			doit;
		fi

	done
#
	echo Backend tests are complete at `date` 
	qawtl END BE/VPS TESTS
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
	     echo " Example: sh $TST_SHELL/runbevps.sh 4 all clean vps "
	     echo ""
	     echo "          or "
	     echo ""
	     echo "          sh $TST_SHELL/runbevps.sh 2 heap clean vps "
	     echo ""

	     exit 1
        else	

	     for fac in $*
	     do
#
# VPS
		if [ "$fac" = "all" -o "$fac" = "vps" ]
		then

			echo "Destroying BE/VPS database @ ", `date`
			echo ""
			destroydb vpsdb >>$TST_OUTPUT/beclean.out
			AREA=vps
			export AREA
		fi
	    done
        fi
	echo Backend databases are destroyed.
fi

#---------------------------------------------------------------------------
#			End of cleanup
#---------------------------------------------------------------------------
