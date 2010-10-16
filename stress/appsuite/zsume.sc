/*
**  Copyright (c) 2005, 2007 Ingres Corporation
**
**  Stress Test Application Suite
**
**  zsume.sc 
**
**  History:
**
**  15-May-2005 sarjo01: Created from zsum + updbtree 
**  10-Aug-2005 sarjo01: Added -z (retry on connection error) 
**  14-Oct-2005 sarjo01: Feature sync up
**  30-Jan-2006	(boija02) Updated copyright info for Ingres Corp.
**  31-May-2007 (Ralph Loen) Bug 118428
**     Ported to VMS.
*/
#ifdef _WIN32
   #include <windows.h>
   #define pthread_t int
#else
   #include <pthread.h>
   #define HANDLE int
   #define min(a,b) ((a<b)?a:b)
   #define stricmp strcasecmp
   #define strnicmp strncasecmp
#endif

#include <stdio.h>
#include <stdlib.h>

EXEC SQL include sqlca;   

#define MAXCHILDTHREADS 99
#define MAXKEY 2147483647
#define MAXVERBOSE 2
#define MINVERBOSE 0
#define BALANCE 1000.00

void doit(int *p);
void checkit();
void print_err();
void createobjs();
void cleanup();

HANDLE  h[MAXCHILDTHREADS];

int xactscompleted[MAXCHILDTHREADS];
int updcount[MAXCHILDTHREADS];
int deadlocks[MAXCHILDTHREADS];
int lockwaits[MAXCHILDTHREADS];
int fatalerrs[MAXCHILDTHREADS];
int rollbacks[MAXCHILDTHREADS];

int nthreads = 8;
int iters = 1000;
int lockwait = 0;
int locklevel = 'R';
int parts = 1;
int nodes = 0;
char *cnodes[16];
int nodestats[16];
int rbfreq = 0;
int verbose = MINVERBOSE; 
int xperconn = 0; 
int abortfatal = 0;
int retryconnect = 0;
char tblstruct = 'B';
int running;

char  *syntax =
 "\n"
 "Syntax:     zsume <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize zsume database objects\n"
 "            run     - execute zsume program, display results\n"
 "            cleanup - delete all zsume database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -b   run      Set forced rollback frequency  0 to 100         0\n"
 "   -i   run      Set transaction count (per     1 to 1000000     1000\n"
 "                 thread)\n"
 "   -l   run      Set lock level                 P(age), R(ow)    R\n"
 "   -n   run      Add nodename to tracking list  nodename         none\n"
 "   -p   init     Enable partitions              1 to 64          1\n"
 "   -r   init     Set account table row count    1 to 1000000     25000\n"
 "   -s   init     Set account table structure    B(tree),I(sam),  B\n"
 "                                                H(ash)\n"
 "   -t   run      Set no. of client threads      1 to 99          8\n"
 "   -v   run      Set verbose output level       0 to 2           0\n"
 "                                                0=no output\n"
 "   -w   run      Set lock wait seconds          0 to 10          0\n"
 "                                                99=nowait\n"
 "   -x   run      Set no. of transactions per    1 to 1000,       0\n"
 "                 connection                     0=never disconn\n"
 "   -y   run      Abort thread on SQL error      none             disabled\n"
 "   -z   run      Retry on connect error         none             disabled\n"

 "\n";

EXEC SQL begin declare section;
     char   *dbname;
     char   starttime[257];
     char   endtime[257];
     char   etime[257];
     char   stmtbuff[257];
     int    highkey = 25000;
     int    rowcount = 25000;
EXEC SQL end declare section;

int irng(int val, int minval, int maxval)
{
   if (val < minval)
      val = minval;
   else if (val > maxval)
      val = maxval;
   return val;
}

main(int argc, char *argv[])
{
   int i, intparm;
   int param[MAXCHILDTHREADS];
   pthread_t lpThreadId[MAXCHILDTHREADS];
# ifdef VMS
   pthread_attr_t attr;
   size_t stksize = 320000;
   void *stkaddr;
   int statp;
# endif

   if (argc < 3)
   {
      printf(syntax);
      exit(-1);
   }
   dbname = argv[1];
/*
** Process command line options
*/
   for (i = 3; i < argc ; i++)
   {
      if (*argv[i] == '-' && *(argv[i]+1) != '\0')
      {
         intparm = atoi(argv[i]+2);
         switch (toupper(*(argv[i]+1)))
         {
            case 'B':
               rbfreq = irng(intparm, 0, 100);
               break;
            case 'I':
               iters = irng(intparm, 1, 1000000);
               break;
            case 'L':
               locklevel = toupper(*(argv[i]+2));
               if (locklevel != 'R' && locklevel != 'P')
                  locklevel = 'R';
               break;
            case 'N':
               cnodes[nodes] = argv[i]+2;
               nodes++;
               break;
            case 'P':
               parts = irng(intparm, 1, 64);
               break;
            case 'R':
               rowcount = irng(intparm, 1, 1000000);
               break;
            case 'S':
               tblstruct = toupper(*(argv[i]+2));
               if (tblstruct != 'B' &&
                   tblstruct != 'I' &&
                   tblstruct != 'H')
                  tblstruct = 'B';
               break;
            case 'T':
               nthreads = irng(intparm, 1, MAXCHILDTHREADS);
               break;
            case 'V':
               verbose = irng(intparm, MINVERBOSE, MAXVERBOSE);
               break;
            case 'W':
               if (intparm == 99)
                  lockwait = 99;
               else
                  lockwait = irng(intparm, 0, 10);
               break;
            case 'X':
               xperconn = irng(intparm, 1, 1000);
               break;
            case 'Y':
               abortfatal = 1;
               break;
            case 'Z':
               retryconnect = 1;
               break;

         }
      } 
   }

   running = nthreads;

   EXEC SQL whenever sqlerror stop;
   EXEC SQL connect :dbname as 'mastercon';
   EXEC SQL set autocommit on;

   if (stricmp(argv[2], "cleanup") == 0)
   {
      cleanup();
      EXEC SQL disconnect 'mastercon';
      printf("\n");
      exit(0);
   }
   if (stricmp(argv[2], "init") == 0)
   {
      createobjs();
      EXEC SQL disconnect 'mastercon';
      printf("\n");
      exit(0);
   }
   else if (stricmp(argv[2], "run") != 0)
   {
      printf(syntax);
      exit(-1);
   }

   memset(updcount, 0, sizeof(updcount));
   memset(nodestats, 0, sizeof(nodestats));
   memset(xactscompleted, 0, sizeof(xactscompleted));
   memset(deadlocks, 0, sizeof(deadlocks)); 
   memset(lockwaits, 0, sizeof(lockwaits)); 
   memset(fatalerrs, 0, sizeof(fatalerrs)); 
   memset(rollbacks, 0, sizeof(fatalerrs)); 

   printf("Cleaning up...\n");

   EXEC SQL update zsumetbl1 set ucount = 0;
   EXEC SQL update zsumetbl2 set ucount = 0;
   EXEC SQL select count(*), max(accid) into
            :rowcount, :highkey from zsumetbl1;

   for (i = 1; i <= nthreads; i++)
      param[i-1] = i;

   sprintf(stmtbuff,
           "set random_seed %d",
           iters * nthreads);
   EXEC SQL execute immediate :stmtbuff;


#ifdef _WIN32
   for (i = 0; i < nthreads; i++)
   {
      h[i] = CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)doit,
				 &param[i], 0, &lpThreadId[i]);
   }
   if (verbose < MAXVERBOSE)
      printf("STARTED %d THREADS...\n", nthreads);
   EXEC SQL select date('now') into :starttime; 
   EXEC SQL disconnect 'mastercon';
   WaitForMultipleObjects(nthreads, h, 1, INFINITE);
#else
   for (i = 0; i < nthreads; i++)
   {
# ifdef VMS
        pthread_attr_init(&attr);
        pthread_attr_setstacksize(&attr, stksize);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);
        pthread_create(&lpThreadId[i],&attr, doit, &param[i]);
        pthread_attr_destroy(&attr);
# else
        pthread_create(&lpThreadId[i],NULL, doit, &param[i]);
# endif

   }
   if (verbose < MAXVERBOSE)
      printf("STARTED %d THREADS...\n", nthreads);
   EXEC SQL select date('now') into :starttime; 
   EXEC SQL disconnect 'mastercon';
   for (i = 0; i < nthreads; i++)
   {
      pthread_join(lpThreadId[i], NULL);
   }
#endif
/*
** Child threads have all terminated.
*/    
   EXEC SQL connect :dbname as 'mastercon';
   EXEC SQL select date('now') into :endtime; 
   if (verbose < MAXVERBOSE)
      printf("%d THREADS DONE\n", nthreads);
   checkit();
   EXEC SQL commit;
   EXEC SQL disconnect 'mastercon';
   printf("\n");

   exit(0);
}

void print_err(int ecode)
{
   EXEC SQL begin declare section;
      char dsp[550];
   EXEC SQL end declare section;

   EXEC SQL inquire_sql (:dsp = ERRORTEXT);
   printf("(%d) %s\n", ecode, dsp);
}
/*
** Child thread entry point
*/
void doit(int *p)
{
   EXEC SQL begin declare section;
        double bal1, bal2;
	int pval, dval, cmdval;
        int hk;
        int acctid;
	char connectName[45];
        char nodename[65];
	char stmtbuff[257];
   EXEC SQL end declare section;

   int upded, loopcnt, q, cmd, xs, xst, i,j,k, error_code, reconn, neverdisc;
   int updedx, rows, trows, rb;

   pval = *p;
   hk = highkey;
   loopcnt = iters;
   upded = updedx = 0;
   xst = xs = xperconn;

   if (verbose == MAXVERBOSE)
      printf("T%02d START...\n", pval);
/*
** Generate a distinct connection name string
*/
   sprintf(connectName, "slavecon%d", pval);

   neverdisc = (xst == 0) ? 1 : 0;
   
   reconn = 1;
/*
** Main loop
*/
   for (i = 0; i < loopcnt; i++)
   {
retry2:
      if (reconn == 1)
      {
         EXEC SQL whenever sqlerror goto CONNerrorhandler; 
         EXEC SQL connect :dbname as :connectName; 
         reconn = 0;
         EXEC SQL whenever sqlerror goto SQLerrorhandler; 
         EXEC SQL set autocommit off; 
         EXEC SQL set session with on_error = rollback transaction;

         if (locklevel == 'R')
            EXEC SQL set lockmode session where level=row, readlock=exclusive;
         else
            EXEC SQL set lockmode session where level=page, readlock=exclusive;

         if (lockwait == 99)
            EXEC SQL set lockmode session where timeout=nowait; 
         else
         {
            sprintf(stmtbuff,
                    "set lockmode session where timeout=%d",
                    lockwait);
            EXEC SQL execute immediate :stmtbuff;
         }
         EXEC SQL select dbmsinfo('db_cluster_node') into :nodename;
         if (nodes)
         {
            for (k = 0; k < nodes; k++)
            {
               if (stricmp(nodename, cnodes[k]) == 0)
                  break;
            }
            if (k < nodes)
               (nodestats[k])++;
         }
      }
/*
** Transaction's queries begin here
*/
      updedx = 0;
      cmd=1;
      q=1;
      EXEC SQL repeated select random(1, 100), random(1, :hk)
               into :cmdval, :acctid;
      rb = (cmdval > rbfreq) ? 0 : 1;

retry1:

      q=2;
      EXEC SQL repeated select a1.bal, a2.bal into :bal1, :bal2
               from zsumetbl1 a1, zsumetbl2 a2
               where a1.accid = a2.accid and
                     a2.accid = :acctid;

      if (bal1 > 0.0)
      {
         q=3;
         EXEC SQL repeated update zsumetbl1 set
                                  bal = bal - 1.00,
                                  ucount = ucount + 1 
                           where accid = :acctid;
         updedx += sqlca.sqlerrd[2];
         q=4;
         EXEC SQL repeated update zsumetbl2 set
                                  bal = bal + 1.00,
                                  ucount = ucount + 1 
                           where accid = :acctid;
         updedx += sqlca.sqlerrd[2];
      }
      else
      {
         q=5;
         EXEC SQL repeated update zsumetbl1 set
                                  bal = money(:bal2),
                                  ucount = ucount + 1 
                           where accid = :acctid;
         updedx += sqlca.sqlerrd[2];
         q=6;
         EXEC SQL repeated update zsumetbl2 set
                                  bal = money(:bal1),
                                  ucount = ucount + 1 
                           where accid = :acctid;
         updedx += sqlca.sqlerrd[2];
      }

      if (!rb)
      {
         q=7;
         EXEC SQL commit;
         xactscompleted[pval-1]++;
         upded += updedx;

         if (verbose == MAXVERBOSE)
         {
            if (nodes)
               printf("T%02d:%07d %s\n", pval, i+1, nodename);
            else
               printf("T%02d:%07d\n", pval, i+1);
         }
      }
      else
      {
         q=8;
         EXEC SQL rollback;
         rollbacks[pval-1]++;
         if (verbose == MAXVERBOSE)
         {
            if (nodes)
               printf("T%02d:%07d %s ROLLBACK\n", pval, i+1, nodename);
            else
               printf("T%02d:%07d ROLLBACK\n", pval, i+1);
         }
      }

      if (neverdisc == 0 && --xs == 0)
      { 
         EXEC SQL disconnect :connectName;
         xs = xst;
         reconn = 1;
      }

      continue;

SQLerrorhandler:

      error_code = sqlca.sqlcode;

      if (error_code == -49900)
      {
         EXEC SQL commit;
         if (verbose > MINVERBOSE)
            printf("T%02d: DEADLOCK (%d,%d)\n",
                    pval, cmd, q);
         deadlocks[pval-1]++; 
         goto retry1;
      }
      else if (error_code == -39100)
      {
         EXEC SQL commit;
         if (verbose > MINVERBOSE)
            printf("T%02d: LOCKWAIT TIMEOUT (%d,%d)\n",
                    pval, cmd, q);
         lockwaits[pval-1]++; 
         PCsleep(1000);
         goto retry1;
      }
      else /* fatal SQL error */
      {
         print_err(error_code);
         EXEC SQL whenever sqlerror continue; 
         EXEC SQL rollback;
         EXEC SQL disconnect :connectName;
         printf("T%02d: Fatal SQL error (%d,%d)\n",
                 pval, cmd, q);
         fatalerrs[pval-1]++; 
         reconn = 1;
         if (abortfatal)
            break;
         else
            goto retry2;
      }

CONNerrorhandler:

      printf("Connect error: %s\n", connectName);
      print_err(sqlca.sqlcode);
      if (retryconnect)
      {
         PCsleep(10000);
         reconn = 1;
         goto retry2;
      }
      else
         break;
   }

   if (reconn == 0)
      EXEC SQL disconnect :connectName;
   updcount[pval-1] += upded;
   running--;
   if (verbose == MAXVERBOSE)
      printf("T%02d DONE, %d running\n", pval, running);
}
void cleanup()
{
   EXEC SQL whenever sqlerror continue;
   EXEC SQL set autocommit on;

   printf("Cleaning up...\n");

   EXEC SQL drop table zsumetbl1;
   EXEC SQL drop table zsumetbl2;
   EXEC SQL commit;

}
/*
** Function to create db objects
*/
void createobjs()
{
   EXEC SQL begin declare section;
      int i, keyval;
      double bal;
      varchar struct {
         short slen;
         char  dbuff[256];
         char  stmtbuff[1024];
      } vch1;
   EXEC SQL end declare section;

   EXEC SQL set autocommit on;

   cleanup();

   EXEC SQL whenever sqlerror call sqlprint;

   printf("Creating objects...\n");

   EXEC SQL create table zsumetbl1 
            (accid int, bal money, ucount int, reserved char(950))
             with page_size=4096;

   bal = BALANCE;

   for (i = 1; i <= rowcount; i++)
   {
      EXEC SQL repeated insert into zsumetbl1 values
               (:i, money(:bal), 0, 'Filler Data');
   }

   EXEC SQL create table zsumetbl2 (accid, bal, ucount, reserved)
               as select accid, money(0.0), ucount, reserved
               from zsumetbl1 with page_size=4096;

   if (tblstruct == 'B')
   {
      EXEC SQL modify zsumetbl1 to btree unique on accid;
      EXEC SQL modify zsumetbl2 to btree unique on accid;
   }
   else if (tblstruct == 'H')
   {
      EXEC SQL modify zsumetbl1 to hash unique on accid;
      EXEC SQL modify zsumetbl2 to hash unique on accid;
   }
   else
   {
      EXEC SQL modify zsumetbl1 to isam unique on accid;
      EXEC SQL modify zsumetbl2 to isam unique on accid;
   }

   if (parts > 1)
   {
      sprintf(stmtbuff,
              "modify zsumetbl1 to reconstruct with partition="\
              "(hash on accid %d partitions)", parts);
      EXEC SQL execute immediate :stmtbuff;
      sprintf(stmtbuff,
              "modify zsumetbl2 to reconstruct with partition="\
              "(hash on accid %d partitions)", parts);
      EXEC SQL execute immediate :stmtbuff;
   }

   EXEC SQL commit;

}
/*
** Function to check database integrity and display results
** at completion of run.
*/
void checkit()
{
   EXEC SQL begin declare section;
      int totrollbacks, totupds, totrows, totxacts, totdeadlocks, totlockwaits;
      int totx, totfatal;
      int totsecs;
      int u1, u2; 
      int dbals;
      double expbal, bal;
   EXEC SQL end declare section;

   int  i;
   totfatal = totupds = totxacts = totdeadlocks = totlockwaits = 0;
   totrollbacks = 0;
   expbal = BALANCE;

   printf("\nCompiling results...\n\n");

   for (i=0; i < nthreads; i++)
   {
      totupds += updcount[i];
      totxacts += xactscompleted[i];
      totdeadlocks += deadlocks[i];
      totlockwaits += lockwaits[i];
      totfatal += fatalerrs[i];
      totrollbacks += rollbacks[i];
   }

   EXEC SQL select
        int4(interval('seconds', date(:endtime) - date(:starttime)))
                       into :totsecs;
   if (totsecs == 0)
      totsecs = 1;
   EXEC SQL select date(:endtime) - date(:starttime) into :etime;

   EXEC SQL select sum(ucount) into :u1 from zsumetbl1; 
   EXEC SQL select sum(ucount) into :u2 from zsumetbl2; 

   EXEC SQL select count(a1.bal + a2.bal)
                   into :dbals from zsumetbl1 a1, zsumetbl2 a2
                   where a1.accid = a2.accid and
                         a1.bal + a2.bal = :expbal;

   printf(" Data Integrity Check\n");
   printf("-------------------------------------\n");
   printf(" Row count                : % 8d\n", rowcount);
   printf(" Expected updates         : % 8d\n", totupds);
   printf(" Actual updates           : % 8d\n", u1+u2);
   printf(" Data integrity           : %s\n\n",
            (dbals == rowcount)?"OK":"FAILED");

   printf(" Runtime Summary\n");
   printf("-------------------------------------------------\n");
   printf(" Start time               : %s\n", starttime);
   printf(" End time                 : %s\n", endtime);
   printf(" Elapsed time             : %s\n", etime);
   printf(" Threads started          : %d\n", nthreads);

   if (nodes)
   {
      for (i = 0; i < nodes; i++)
         printf(" %25.25s: %d connections\n", cnodes[i],
                nodestats[i]);
   }

   printf(" Deadlocks                : %d\n", totdeadlocks);
   printf(" Lockwait timeouts        : %d\n", totlockwaits);
   printf(" Fatal SQL errors         : %d\n", totfatal);
   printf(" Committed transactions   : %d\n", totxacts);
   printf(" Forced rollbacks         : %d\n", totrollbacks);
   printf(" Total transactions       : %d\n", totxacts+totrollbacks);
   printf(" TPS                      : %d\n",
            (totxacts+totrollbacks)/totsecs);

}
