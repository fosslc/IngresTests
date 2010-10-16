/*
**  Copyright (c) 2005, 2007 Ingre Corp.
**  This file is distributed under the CA Trusted Open Source License (CATOSL).
**  For the exact terms of the license go to http://ca.com/opensource/catosl
**
**  Stress Test Application Suite
**
**  seqv1.sc 
**
**  History:
**
**  06-Oct-2005 sarjo01: Created from updv1 + seq1
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
void createobjs(int createall);
void cleanup(int cleanall);

HANDLE  h[MAXCHILDTHREADS];

int xactscompleted[MAXCHILDTHREADS];
int deadlocks[MAXCHILDTHREADS];
int lockwaits[MAXCHILDTHREADS];
int fatalerrs[MAXCHILDTHREADS];
int rollbacks[MAXCHILDTHREADS];
int seqcounts[MAXCHILDTHREADS];
int nthreads = 8;
int iters = 1000;
int lockwait = 0;
int rbfreq = 0;
int verbose = MINVERBOSE; 
int xperconn = 0; 
int abortfatal = 0;
int running;
int cache = 1000;

char  *syntax =
 "\n"
 "Syntax:     seqv1 <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize seqv1 database objects\n"
 "            run     - execute seqv1 program, display results\n"
 "            cleanup - delete all seqv1 database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -b   run      Set forced rollback frequency  0 to 100         0\n"
 "   -c   init     Set cache value (0=no caching) 0 to 10000       1000\n"
 "   -i   run      Set transaction count (per     1 to 1000000     1000\n"
 "                 thread)\n"
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
     char   stmtbuff[1024];
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
            case 'C':
               cache = irng(intparm, 0, 10000); 
               break;
            case 'I':
               iters = irng(intparm, 1, 1000000);
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
      cleanup(1);
      EXEC SQL disconnect 'mastercon';
      printf("\n");
      exit(0);
   }
   if (stricmp(argv[2], "init") == 0)
   {
      createobjs(1);
      EXEC SQL disconnect 'mastercon';
      printf("\n");
      exit(0);
   }
   else if (stricmp(argv[2], "run") != 0)
   {
      printf(syntax);
      exit(-1);
   }

   memset(seqcounts, '\0', sizeof(seqcounts));
   memset(xactscompleted, 0, sizeof(xactscompleted));
   memset(deadlocks, 0, sizeof(deadlocks)); 
   memset(lockwaits, 0, sizeof(lockwaits)); 
   memset(fatalerrs, 0, sizeof(fatalerrs)); 
   memset(rollbacks, 0, sizeof(fatalerrs));

   createobjs(0);

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
        int hk,lk,rb,tabid;
	char connectName[45];
        char nodename[65];
	char stmtbuff[257];
   EXEC SQL end declare section;

   int upded, loopcnt, q, cmd, xs, xst, i,j,k, error_code, reconn, neverdisc;
   int updedx, rows, trows, dorb;
   char *rbstr;

   pval = *p;
   tabid = pval;
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
      }
/*
** Transaction's queries begin here
*/
      EXEC SQL repeated select random(1, 100) into :rb;
      dorb = (rb > rbfreq) ? 0 : 1;
      if (dorb == 1)
         rbstr = "ROLLBACK";
      else
         rbstr = "";

retry1:

      cmd = 1;
      q = 1;
      sprintf(stmtbuff,
              "insert into seqv1tbl%02d select seqv1seq1.nextval",
               tabid);
      EXEC SQL execute immediate :stmtbuff;

      if (dorb == 1)
      {
         EXEC SQL rollback;
         rollbacks[pval-1]++;
      }
      else
      {
         EXEC SQL commit;
         seqcounts[pval - 1]++;
         xactscompleted[pval-1]++;
      }

      if (verbose == MAXVERBOSE)
      {
         printf("T%02d:%07d %s\n", pval, i+1, rbstr);
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
      break;
   }

   if (reconn == 0)
      EXEC SQL disconnect :connectName;
   running--;
   if (verbose == MAXVERBOSE)
      printf("T%02d DONE, %d running\n", pval, running);
}
void cleanup(int cleanall)
{
   EXEC SQL begin declare section;
      int  i;
   EXEC SQL end declare section;

   EXEC SQL whenever sqlerror continue;
   EXEC SQL set autocommit on;

   printf("Cleaning up...\n");

   if (cleanall)
   {
      EXEC SQL drop sequence seqv1seq1;
      EXEC SQL drop table sessionseqv1;
   }

   for (i = 1; i <= MAXCHILDTHREADS; i++)
   {
      sprintf(stmtbuff, "drop table seqv1tbl%02d", i);
      EXEC SQL execute immediate :stmtbuff;
   }

   EXEC SQL commit;

}
/*
** Function to create db objects
*/
void createobjs(int createall)
{
   EXEC SQL begin declare section;
      int i, keyval;
   EXEC SQL end declare section;

   EXEC SQL set autocommit on;

   cleanup(createall);

   EXEC SQL whenever sqlerror call print_sqlcode_exit;

   printf("Creating objects...\n");

   for (i = 1; i <= MAXCHILDTHREADS; i++)
   {
      sprintf(stmtbuff,
              "create table seqv1tbl%02d (seqval int) with page_size=4096",
              i);
      EXEC SQL execute immediate :stmtbuff;
   }

   if (createall)
   {
      if (cache == 0)
         EXEC SQL create sequence seqv1seq1 nocache;
      else
      {
         sprintf(stmtbuff, "create sequence seqv1seq1 cache %d", cache);
         EXEC SQL execute immediate :stmtbuff;
      }
   
      EXEC SQL create table sessionseqv1 (seqval int);
      EXEC SQL modify sessionseqv1 to btree on seqval;
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
      int totrows, totxacts, totdeadlocks, totlockwaits;
      int totx, totfatal;
      int totsecs;
      int totrollbacks, totseqsexp;
      int totalseq, distseq, minseq, maxseq;
   EXEC SQL end declare section;

   int  i;
   totfatal = totxacts = totdeadlocks = totlockwaits = 0;
   totrollbacks = totseqsexp = 0;

   printf("\nCompiling results...\n\n");

   for (i=0; i < nthreads; i++)
   {
      totseqsexp += seqcounts[i];
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

   for (i = 1; i <= nthreads; i++)
   {
      sprintf(stmtbuff,
              "insert into sessionseqv1 select * from seqv1tbl%02d", i);
      EXEC SQL execute immediate :stmtbuff;
   }

   EXEC SQL select count(*), count(distinct seqval),
                   min(seqval), max(seqval)
                   into :totalseq, :distseq, :minseq, :maxseq
                   from sessionseqv1;

   printf(" Data Integrity Check\n");
   printf("-------------------------------------\n");
   printf(" Nextvals taken           : % 8d\n", totalseq);
   printf(" Distinct values          : % 8d\n", distseq);
   printf(" Min value                : % 8d\n", minseq);
   printf(" Max value                : % 8d\n\n", maxseq);

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
