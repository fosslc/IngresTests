/*
**  Copyright (c) 2005, 2007 Ingres Corp.
**
**  Stress Test Application Suite
**
**  updv3.sc 
**
**  History:
**
**  06-Oct-2005 sarjo01: Created from btreeupdseg + updv1 
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
void print_sqlcode_exit();
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
int parts = 1;
int verbose = MINVERBOSE; 
int xperconn = 0; 
int rowcount = 10000;
int abortfatal = 0;
int rbfreq = 0;
int page_size = 4;
char tblstruct = 'B';
int running;

char  *syntax =
 "\n"
 "Syntax:     updv3 <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize updv3 database objects\n"
 "            run     - execute updv3 program, display results\n"
 "            cleanup - delete all updv3 database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -b   run      Set forced rollback frequency  0 to 100         0\n"
 "   -g   run      Set page size                  2(k),4(k),8(k),  4\n"
 "                                                1(6k),3(2k)\n"
 "   -i   run      Set transaction count (per     1 to 1000000     1000\n"
 "                 thread)\n"
 "   -p   init     Enable partitions              1 to 64          1\n"
 "   -r   init     Set table row count            1 to 100000      10000\n"
 "   -s   init     Set table structure            B(tree),I(sam),  B\n"
 "                                                H(ash)\n"
 "   -t   run      Set no. of client threads      1 to 99          8\n"
 "   -v   run      Set verbose output level       0 to 2           0\n"
 "                                                0=no output\n"
 "   -w   run      Set lock wait seconds          0 to 10          0\n"
 "                                                99=nowait\n"
 "   -x   run      Set no. of transactions per    1 to 1000,       0\n"
 "                 connection                     0=never disconn\n"
 "   -y   run      Abort thread on fatal error    none             disabled\n"
 "\n";

EXEC SQL begin declare section;
     char   *dbname;
     char   starttime[257];
     char   endtime[257];
     char   etime[257];
     char   stmtbuff[257];
     int    highkey = 100000;
     int    initucount; 
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
   char *a;
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
            case 'G':
               page_size = irng(intparm, 1, 8);
               break;
            case 'I':
               iters = irng(intparm, 1, 1000000);
               break;
            case 'P':
               parts = irng(intparm, 1, 64);
               break;
            case 'R':
               rowcount = irng(intparm, 1, 10000);
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
         }
      } 
   }

   running = nthreads;

   EXEC SQL whenever sqlerror call print_sqlcode_exit;
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

/*
   EXEC SQL update updv3tbl1 set updcount = 0;
*/
   EXEC SQL select sum(updcount), max(keyval) into
            :initucount, :highkey from updv3tbl1;

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
   for (i = 0; i < nthreads; i++)
   {
      pthread_join(lpThreadId[i], NULL);
   }
#endif
/*
** Child threads have all terminated.
*/    
   EXEC SQL select date('now') into :endtime; 
   if (verbose < MAXVERBOSE)
      printf("%d THREADS DONE\n", nthreads);
   checkit();
   EXEC SQL commit;
   EXEC SQL disconnect 'mastercon';
   printf("\n");

   exit(0);
}

void print_sqlcode_exit()
{
   EXEC SQL begin declare section;
      char dsp[550];
   EXEC SQL end declare section;

   EXEC SQL inquire_sql (:dsp = ERRORTEXT);
   printf("%s\n", dsp);
   exit(-1);
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
	int pval, dval;
        int hk,lk,rb;
	char connectName[45];
	char stmtbuff[257];
   EXEC SQL end declare section;

   int upded, loopcnt, q, cmd, xs, xst, i,j,k, error_code, reconn, neverdisc;
   int updedx, rows, trows, dorb;
   char *rbstr;

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

         if (lockwait == 99)
            EXEC SQL set lockmode session where level=row, timeout=nowait; 
         else
         {
            sprintf(stmtbuff,
                    "set lockmode session where level=row, timeout=%d",
                    lockwait);
            EXEC SQL execute immediate :stmtbuff;
         }
      }
/*
** Transaction's queries begin here
**
** SELECT a random key value, then use it to UPDATE row(s)
*/
      EXEC SQL repeated select random(1, 100), random(1, :hk)
               into :rb, :dval;
      dorb = (rb > rbfreq) ? 0 : 1;
      if (dorb == 1)
         rbstr = "ROLLBACK";
      else
         rbstr = "";

retry1:

      cmd = 1;
      q = 1;

      EXEC SQL repeated update updv3tbl1 set
               ts = 'now',
               updcount = updcount + 1
               where keyval = :dval;

      trows = sqlca.sqlerrd[2];

      if (dorb == 1)
      {
         EXEC SQL rollback;
         rollbacks[pval-1]++;
      }
      else
      {
         EXEC SQL commit;
         upded += trows;
         xactscompleted[pval-1]++;
      }

      if (verbose == MAXVERBOSE)
      {
         printf("T%02d:%07d r:%d %s\n", pval, i+1, trows, rbstr);
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
         PCsleep(500);
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

   EXEC SQL drop table updv3tbl1;
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

   char *pgsz, *sstruct;

   EXEC SQL set autocommit on;

   cleanup();

   EXEC SQL whenever sqlerror call print_sqlcode_exit;

   printf("Creating objects...\n");

   EXEC SQL create table updv3tbl1
            (keyval int,
             data1 varchar(5000),
             kv2 int,
             data2 varchar(5000),
             kv3 int,
             data3 varchar(5000),
             kv4 int,
             data4 varchar(5000),
             kv5 int,
             data5 varchar(5000),
             kv6 int,
             data6 varchar(5000),
             ts date,
             updcount int);

   for (i=1; i<=rowcount; i++)
   {
      keyval = i;
      EXEC SQL repeated insert into updv3tbl1 values (
               :keyval,
               'data1',
               :keyval,
               'data2',
               :keyval,
               'data3',
               :keyval,
               'data4',
               :keyval,
               'data5',
               :keyval,
               'data6',
               date('now'),
               0 );
   }

   switch (tblstruct)
   {
      case 'B':  
         sstruct = "btree";
         break;
      case 'H':  
         sstruct = "hash";
         break;
      case 'I':  
         sstruct = "isam";
         break;
   }

   switch (page_size)
   {
      case 1:
         pgsz = "16384";
         break;
      case 2:
         pgsz = "2048";
         break;
      case 3:
         pgsz = "32768";
         break;
      case 8:
         pgsz = "8192";
         break;
      default:
         pgsz = "4096";
         break;
   }

   sprintf(stmtbuff,
           "modify updv3tbl1 to %s on keyval with page_size=%s",
           sstruct, pgsz);
   EXEC SQL execute immediate :stmtbuff;

   if (parts > 1)
   {
      sprintf(stmtbuff,
              "modify updv3tbl1 to reconstruct with partition="\
              "(hash on keyval %d partitions)", parts);
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
      int totupds, totrows, totxacts, totdeadlocks, totlockwaits;
      int totx, totfatal;
      int totsecs, chkinteg;
      int actupds, totrollbacks;
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

   EXEC SQL select count(*), sum(updcount) - :initucount
            into :totrows, :actupds from updv3tbl1;

   EXEC SQL select count(*) into :chkinteg from updv3tbl1
            where ((kv2+kv3+kv4+kv5+kv6)/5 <> keyval) or
                  (char(left(data1,5)+left(data2,5)+left(data3,5)+
                        left(data4,5)+left(data5,5)+left(data6,5),30)
                   <> 'data1data2data3data4data5data6');

   EXEC SQL select
        int4(interval('seconds', date(:endtime) - date(:starttime)))
                       into :totsecs;
   if (totsecs == 0)
      totsecs = 1;
   EXEC SQL select date(:endtime) - date(:starttime) into :etime;

   printf(" Data Integrity Check\n");
   printf("-------------------------------------\n");
   printf(" Row count                : % 8d\n", totrows);
   printf(" Expected updates         : % 8d\n", totupds);
   printf(" Actual updates           : % 8d\n", actupds);
   printf(" Data integrity           : %s\n\n",
          chkinteg == 0 ? "OK" : "ERROR");

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
