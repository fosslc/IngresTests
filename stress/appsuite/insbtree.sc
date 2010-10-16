/*
**  Copyright (c) 2005, 2007 Ingres Corporation
**
**  Stress Test Application Suite
**
**  insbtree.sc 
**
**  History:
**
**  01-May-2005 sarjo01: Created from btree3
**  01-Jun-2005 sarjo01: Added -m flag (remodify tables) 
**  28-Jun-2005 sarjo01: Re-submitted, wrong file used on last submit 
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

#define MAXCHILDTHREADS 100
#define MAXKEY 2147483647
#define MAXVERBOSE 2
#define MINVERBOSE 0
#define DELFREQ 10

void doit(int *p);
void checkit();
void print_err();
void createobjs();
void cleanup();
void remodtables();

HANDLE  h[MAXCHILDTHREADS];

int xactscompleted[MAXCHILDTHREADS];
int addcount[MAXCHILDTHREADS];
int delcount[MAXCHILDTHREADS];
int deadlocks[MAXCHILDTHREADS];
int lockwaits[MAXCHILDTHREADS];
int fatalerrs[MAXCHILDTHREADS];
int nthreads = 8;
int iters = 1000;
int lockwait = 0;
int dodelete = 0;
int secondary = 0;
int parts = 1;
int nodes = 0;
int compression = 0;
char *cnodes[16];
int nodestats[16];
int verbose = MINVERBOSE; 
int xperconn = 0; 
int running;
int delfreq = 0;
int abortfatal = 0;
int remod = 0;

int startkey = 1;
int sfactor = 1;
int rowcount = 10000;

char  *syntax =
 "\n"
 "Syntax:     insbtree <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize insbtree database objects\n"
 "            run     - execute insbtree program, display results\n"
 "            cleanup - delete all insbtree database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -d   run      Set delete frequency           0 to 100         0\n"
 "   -f   init     Set key sparseness factor      1 to 100000      1\n"
 "   -h   run      Set high key value             0 to 2147483647  0\n"
 "                                                0=use current high\n"
 "   -i   run      Set transaction count (per     1 to 1000000     1000\n"
 "                 thread)\n"
 "   -k   init     Set starting key value         1 to 100000      1\n"
 "   -l   run      Set low key value              1 to 100000      1\n"
 "   -m   run      Remodify tables                none             disabled\n"
 "   -n   run      Add nodename to tracking list  nodename         none\n"
 "   -p   init     Enable partitions              1 to 64          1\n"
 "   -r   init     Set initial row count          1 to 100000      10000\n"
 "   -s   run      Enable secondary index         none             disabled\n"
 "   -t   run      Set no. of client threads      1 to 100         8\n"
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
     int    lowkey = 1;
     int    currhighkey; 
     int    highkey = 0;
     int    initrows;
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
            case 'D':
               delfreq = irng(intparm, 0, 100);
               break;
            case 'F':
               sfactor = irng(intparm, 1, 10000); 
               break;
            case 'H':
               highkey = irng(intparm, 0, MAXKEY); 
               break;
            case 'I':
               iters = irng(intparm, 1, 1000000); 
               break;
            case 'K':
               startkey = irng(intparm, 1, 100000);
               break;
            case 'L':
               lowkey = irng(intparm, 1, 100000); 
               break;
            case 'M':
               remod = 1;
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
               secondary = 1;
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

   if (remod == 1)
      remodtables();

   memset(addcount, 0, sizeof(addcount));
   memset(delcount, 0, sizeof(delcount));
   memset(nodestats, 0, sizeof(nodestats));
   memset(xactscompleted, 0, sizeof(xactscompleted));
   memset(deadlocks, 0, sizeof(deadlocks)); 
   memset(lockwaits, 0, sizeof(lockwaits)); 
   memset(fatalerrs, 0, sizeof(fatalerrs)); 

   printf("Cleaning up...\n");

   EXEC SQL select count(*), max(keyval)
            into :initrows, :currhighkey from insbtreetbl1;

   if (highkey == 0)
      highkey = currhighkey;
   if (lowkey > highkey)
   { 
      i = lowkey;
      lowkey = highkey;
      highkey = i;
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
        int hk,lk;
	char connectName[45];
        char nodename[65];
	char stmtbuff[257];
   EXEC SQL end declare section;

   int loopcnt, q, xs, xst, i,j,k, error_code, reconn, neverdisc;
   int opr, deled, rows, trows;

   pval = *p;
   hk = highkey;
   lk = lowkey;
   loopcnt = iters;
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
      EXEC SQL repeated select random(1, 100), random(:lk, :hk)
               into :cmdval, :dval;

      opr = (cmdval > delfreq) ? 1 : 2;
      q = 1;
 
retry1:

      if (opr == 1)  /* insert */
      {
         EXEC SQL repeated insert into insbtreetbl1 values (
                     random(:lk, :hk),
                     uuid_to_char(uuid_create()),
                     date('now'),
                     'New Filler Data' );
         EXEC SQL commit;
         addcount[pval-1]++;
      }
      else /* delete */
      {
         EXEC SQL repeated delete from insbtreetbl1
                  where keyval = :dval;
         deled = sqlca.sqlerrd[2];
         EXEC SQL commit;
         delcount[pval-1] += deled;
      }

      xactscompleted[pval-1]++;

      if (verbose == MAXVERBOSE)
      {
         if (opr == 1)
            printf("T%02d:%07d\n", pval, i+1);
         else
            printf("T%02d:%07d, d:%d\n", pval, i+1, deled);
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
                    pval, opr, q);
         deadlocks[pval-1]++; 
         goto retry1;
      }
      else if (error_code == -39100)
      {
         EXEC SQL commit;
         if (verbose > MINVERBOSE)
            printf("T%02d: LOCKWAIT TIMEOUT (%d,%d)\n",
                    pval, opr, q);
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
                 pval, opr, q);
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
   running--;
   if (verbose == MAXVERBOSE)
      printf("T%02d DONE, %d running\n", pval, running);
}
void cleanup()
{
   EXEC SQL whenever sqlerror continue;
   EXEC SQL set autocommit on;

   printf("Cleaning up...\n");

   EXEC SQL drop table insbtreetbl1;
   EXEC SQL commit;

}
/*
** Function to create db objects
*/
void createobjs()
{
   EXEC SQL begin declare section;
      int i, keyval;
   EXEC SQL end declare section;

   EXEC SQL set autocommit on;

   cleanup();

   EXEC SQL whenever sqlerror call sqlprint;

   printf("Creating objects...\n");

   EXEC SQL create table insbtreetbl1
            (keyval int, data1 char(36), ts date, filler char(400))
            with page_size=4096;
   for (i=1, keyval=startkey; i<=rowcount; i++)
   {
      EXEC SQL repeated insert into insbtreetbl1 values (
               :keyval,
               'Initial Data Value',
               date('now'),
               'Original Filler Data' );
      keyval += sfactor;
      if (keyval < 0 || keyval > MAXKEY)
         keyval = startkey;
   }
   EXEC SQL modify insbtreetbl1 to btree on keyval;

   if (secondary)
   {
      EXEC SQL create index insbtreetbl1_idx on insbtreetbl1 (data1)
               with persistence, structure = btree, page_size=4096;
   }
   if (parts > 1)
   {
      sprintf(stmtbuff,
              "modify insbtreetbl1 to reconstruct with partition="
              "(hash on keyval %d partitions)", parts);
      EXEC SQL execute immediate :stmtbuff;
   }

   EXEC SQL commit;

}
/*
** Function to check database integrity and display results
** at completion of run.
*/
/*
   EXEC SQL select count(*) into :secondary_exists from iirelation
            where relid = 'btree3tbl1_idx';
   if (secondary_exists)
   {
      EXEC SQL drop index btree3tbl1_idx2;
      printf("Secondary index integrity check...\n");
      EXEC SQL select count(*) into :totidxrows from btree3tbl1_idx;
      printf("IDX  ROWS:  %d\n", totidxrows);
      EXEC SQL create index btree3tbl1_idx2 on btree3tbl1(data1)
               with structure = btree, page_size=4096;
      EXEC SQL select count(*) into :totidxrows2 from btree3tbl1_idx2;
      printf("IDX2 ROWS:  %d\n", totidxrows2);
      EXEC SQL select count(*) into :test1 from
               btree3tbl1_idx x1 full join btree3tbl1_idx2 x2
               on x1.tidp = x2.tidp and x1.data1 = x2.data1
               where x1.tidp <> x2.tidp and x1.data1 <> x2.data1;
      printf("IDX INTEG:  %s\n", test1 ? "ERROR" : "OK");
   }
}
*/
void remodtables()
{
   printf("Remodifying tables...\n");

   EXEC SQL modify insbtreetbl1 to btree on keyval;
}

void checkit()
{
   EXEC SQL begin declare section;
      int totupds, totrows, totxacts, totdeadlocks, totlockwaits;
      int totx, totfatal, minkey, maxkey;
      int totsecs;
      int totadds, totdels;
   EXEC SQL end declare section;

   int  i;
   totfatal = totxacts = totdeadlocks = totlockwaits = 0;
   totadds = totdels = 0;

   printf("\nCompiling results...\n\n");

   for (i=0; i < nthreads; i++)
   {
      totadds += addcount[i];
      totdels += delcount[i];
      totxacts += xactscompleted[i];
      totdeadlocks += deadlocks[i];
      totlockwaits += lockwaits[i];
      totfatal += fatalerrs[i];
   }

   EXEC SQL select count(*), min(keyval), max(keyval) into
            :totrows, :minkey, :maxkey from insbtreetbl1;

   EXEC SQL select
        int4(interval('seconds', date(:endtime) - date(:starttime)))
                       into :totsecs;
   if (totsecs == 0)
      totsecs = 1;
   EXEC SQL select date(:endtime) - date(:starttime) into :etime;

   printf(" Data Integrity Check\n");
   printf("-------------------------------------\n");
   printf(" Initial row count        : % 8d\n", initrows);
   printf(" Expected inserts         : % 8d\n", totadds);
   printf(" Expected deletes         : % 8d\n", totdels);
   printf(" Expected row count       : % 8d\n", initrows+totadds-totdels);
   printf(" Actual row count         : % 8d\n", totrows);
   printf(" Low keyval               : % 8d\n", minkey);
   printf(" High keyval              : % 8d\n\n", maxkey);

   printf(" Runtime Summary\n");
   printf("-------------------------------------------------\n");
   printf(" Start time               : %s\n", starttime);
   printf(" End time                 : %s\n", endtime);
   printf(" Elapsed time             : %s\n", etime);
   printf(" Threads started          : %d\n", nthreads);

   if (nodes)
   {
      for (i = 0; i < nodes; i++)
         printf(" %s          : %d connections\n", cnodes[i], nodestats[i]);
   }

   printf(" Deadlocks                : %d\n", totdeadlocks);
   printf(" Lockwait timeouts        : %d\n", totlockwaits);
   printf(" Fatal SQL errors         : %d\n", totfatal);
   printf(" Transactions completed   : %d\n", totxacts);
   printf(" TPS                      : %d\n", totxacts/totsecs);

}
