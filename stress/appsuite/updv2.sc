/*
**  Copyright (c) 2005, 2007 Ingres Corp.
**
**  Stress Test Application Suite
**
**  updv2.sc 
**
**  History:
**
**  01-Sep-2005 sarjo01: Created from updv1
**  14-Oct-2005 sarjo01: Feature sync up
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
int empty = 0;
int locklevel = 'R';
int parts = 1;
int rbfreq = 0;
int verbose = MINVERBOSE; 
int xperconn = 0; 
int abortfatal = 0;
int retryconnect = 0;
int resettable1 = 0;
int qv = 1;
char tblstruct = 'B';
int running;

char  *syntax =
 "\n"
 "Syntax:     updv2 <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize updv2 database objects\n"
 "            run     - execute updv2 program, display results\n"
 "            cleanup - delete all updv2 database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -b   run      Set forced rollback frequency  0 to 100         0\n"
 "   -e   run      Run until table1 empty         none             disabled\n"
 "   -i   run      Set transaction count (per     1 to 1000000     1000\n"
 "                 thread)\n"
 "   -n   run      Reset table1                   none             disabled\n"
 "   -p   init     Enable partitions              1 to 64          1\n"
 "   -q   run      Set query variant              1,2,3,4          1\n"
 "   -r   init     Set table row count            1 to 1000000     25000\n"
 "   -s   init     Set table structure            B(tree),I(sam),  B\n"
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
            case 'E':
               empty = 1;
               break;
            case 'I':
               iters = irng(intparm, 1, 1000000);
               break;
            case 'N':
               resettable1 = 1;
               break;
            case 'P':
               parts = irng(intparm, 1, 64);
               break;
            case 'Q':
               qv = irng(intparm, 1, 4);
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
   memset(xactscompleted, 0, sizeof(xactscompleted));
   memset(deadlocks, 0, sizeof(deadlocks)); 
   memset(lockwaits, 0, sizeof(lockwaits)); 
   memset(fatalerrs, 0, sizeof(fatalerrs)); 
   memset(rollbacks, 0, sizeof(fatalerrs)); 

   printf("Cleaning up...\n");

   EXEC SQL select count(*), max(keyval) into
            :rowcount, :highkey from updv2tbl2;
   if (resettable1)
   {
      EXEC SQL update updv2tbl2 set ucount = 0, status = '1';
      EXEC SQL delete from updv2tbl1; 
      EXEC SQL insert into updv2tbl1 select * from updv2tbl2;
   }
   else
   {
/*
      EXEC SQL update updv2tbl2 set ucount = 0;
*/
   }

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
	int pval, dval, cmdval;
        int hk;
        short ind;
        int keyval;
	char connectName[45];
	char stmtbuff[257];
   EXEC SQL end declare section;

   int upded, loopcnt, q, cmd, xs, xst, i,j,k, error_code, reconn, neverdisc;
   int updedx, rows, trows, rb;

   pval = *p;
   hk = highkey;
   if (empty == 0)
      loopcnt = iters;
   else
      loopcnt = MAXKEY; 
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
         EXEC SQL whenever not found goto SQLnorow; 
         EXEC SQL set autocommit off; 
         EXEC SQL set session with on_error = rollback transaction;

         EXEC SQL set lockmode session where level=row;
         EXEC SQL set lockmode on updv2tbl1 where readlock=exclusive;

         if (lockwait == 99)
            EXEC SQL set lockmode session where timeout=nowait; 
         else
         {
            sprintf(stmtbuff,
                    "set lockmode session where timeout=%d",
                    lockwait);
            EXEC SQL execute immediate :stmtbuff;
         }
      }
/*
** Transaction's queries begin here
*/
      updedx = 0;
      cmd=1;
      q=1;
      EXEC SQL repeated select random(1, 100) into :cmdval;
      rb = (cmdval > rbfreq) ? 0 : 1;

retry1:

      q=2;
      switch (qv)
      {
         case 1:
            EXEC SQL repeated select min(keyval) into :keyval:ind
                     from updv2tbl1;
            if (ind != 0)
                goto SQLnorow;
            break;
         case 2:
            EXEC SQL repeated select first 1 keyval into :keyval 
                     from updv2tbl1 order by keyval;
            break;
         case 3:
            EXEC SQL repeated select keyval into :keyval from updv2tbl1
                     where keyval = (select min(keyval) from updv2tbl1);
            break;
         case 4:
            EXEC SQL repeated select first 1 keyval into :keyval 
                     from updv2tbl1;
            break;
      }

      q=3;
      EXEC SQL repeated delete from updv2tbl1 where keyval = :keyval; 
      q=4;
      EXEC SQL repeated update updv2tbl2
               set status = '2', ucount = ucount + 1
               where keyval = :keyval; 

      if (!rb)
      {
         q=5;
         EXEC SQL commit;
         xactscompleted[pval-1]++;
         upded += updedx;

         if (verbose == MAXVERBOSE)
         {
            printf("T%02d:%07d K:%07d\n", pval, i+1, keyval);
         }
      }
      else
      {
         q=8;
         EXEC SQL rollback;
         rollbacks[pval-1]++;
         if (verbose == MAXVERBOSE)
         {
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

SQLnorow:

   if (reconn == 0)
      EXEC SQL disconnect :connectName;
   updcount[pval-1] += upded;
   running--;
   if (verbose == MAXVERBOSE)
      printf("T%02d DONE, %d running\n", pval, running);
}
void cleanup()
{
   EXEC SQL whenever not found continue;
   EXEC SQL whenever sqlerror continue;
   EXEC SQL set autocommit on;

   printf("Cleaning up...\n");

   EXEC SQL drop table updv2tbl1;
   EXEC SQL drop table updv2tbl2;
   EXEC SQL commit;

}
/*
** Function to create db objects
*/
void createobjs()
{
   EXEC SQL begin declare section;
      int i, keyval;
      varchar struct {
         short slen;
         char  dbuff[256];
         char  stmtbuff[1024];
      } vch1;
   EXEC SQL end declare section;

   EXEC SQL set autocommit on;

   cleanup();

   EXEC SQL whenever sqlerror call sqlprint;
   EXEC SQL whenever not found continue;

   printf("Creating objects...\n");

   EXEC SQL create table updv2tbl1 
            (keyval int, status char(1), ucount int, reserved char(950))
             with page_size=4096;

   for (i = 1; i <= rowcount; i++)
   {
      EXEC SQL repeated insert into updv2tbl1 values
               (:i, '1', 0, 'Filler Data');
   }

   EXEC SQL create table updv2tbl2 as select * 
            from updv2tbl1 with page_size=4096;

   if (parts > 1)
   {
      sprintf(stmtbuff,
              "modify updv2tbl1 to reconstruct with partition="\
              "(hash on keyval %d partitions)", parts);
      EXEC SQL execute immediate :stmtbuff;
      sprintf(stmtbuff,
              "modify updv2tbl2 to reconstruct with partition="\
              "(hash on keyval %d partitions)", parts);
      EXEC SQL execute immediate :stmtbuff;
   }

   if (tblstruct == 'B')
   {
      EXEC SQL modify updv2tbl1 to btree unique on keyval;
      EXEC SQL modify updv2tbl2 to btree unique on keyval;
   }
   else if (tblstruct == 'H')
   {
      EXEC SQL modify updv2tbl1 to hash unique on keyval;
      EXEC SQL modify updv2tbl2 to hash unique on keyval;
   }
   else
   {
      EXEC SQL modify updv2tbl1 to isam unique on keyval;
      EXEC SQL modify updv2tbl2 to isam unique on keyval;
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
      int u1, u2, u2c; 
      int t1cnt;
   EXEC SQL end declare section;

   int  i;
   totfatal = totupds = totxacts = totdeadlocks = totlockwaits = 0;
   totrollbacks = 0;

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

   EXEC SQL select count(*) into :u1 from updv2tbl1; 
   EXEC SQL select sum(ucount) into :u2c from updv2tbl2; 
   EXEC SQL select count(*) into :u2 from updv2tbl2 where status = '2'; 

   printf(" Data Integrity Check\n");
   printf("-------------------------------------\n");
   printf(" Initial row count        : % 8d\n", rowcount);
   printf(" Expected updates         : % 8d\n", u2);
   printf(" Actual updates,1         : % 8d\n", rowcount - u1);
   printf(" Actual updates,2         : % 8d\n\n", u2c);

   printf(" Runtime Summary\n");
   printf("-------------------------------------------------\n");
   printf(" Start time               : %s\n", starttime);
   printf(" End time                 : %s\n", endtime);
   printf(" Elapsed time             : %s\n", etime);
   printf(" Threads started          : %d\n", nthreads);
   printf(" Deadlocks                : %d\n", totdeadlocks);
   printf(" Lockwait timeouts        : %d\n", totlockwaits);
   printf(" Fatal SQL errors         : %d\n", totfatal);
   printf(" Committed transactions   : %d\n", totxacts);
   printf(" Forced rollbacks         : %d\n", totrollbacks);
   printf(" Total transactions       : %d\n", totxacts+totrollbacks);
   printf(" TPS                      : %d\n",
            (totxacts+totrollbacks)/totsecs);

}
