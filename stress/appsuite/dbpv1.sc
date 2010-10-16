/*
**  Copyright (c) 2009 Ingres Corp.
**
**  Stress Test Application Suite
**
**  dbpv1.sc 
**
**  History:
**
**  01-Oct-2009 sarjo01: Created.
**
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
#define MAXPROCS 100000
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
int deadlocks[MAXCHILDTHREADS];
int lockwaits[MAXCHILDTHREADS];
int fatalerrs[MAXCHILDTHREADS];
int nthreads = 8;
int iters = 100;
int lockwait = 0;
int verbose = MINVERBOSE; 
int xperconn = 0; 
int abortfatal = 0;
int cleanit = 0;
int running;
int proctype = 1;

char  *syntax =
 "\n"
 "Syntax:     dbpv1 <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize dbpv1 database objects\n"
 "            run     - execute dbpv1 program, display results\n"
 "            cleanup - delete all dbpv1 database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -c   init/run Enable force cleanup           none             disabled\n"
 "   -i   run      Set transaction count (per     1 to 1000000     100\n"
 "                 thread)\n"
 "   -n   init     Set procedure count            1 to 100000      1000\n"
 "   -p   init/run Set procedure type             0 to 1           0\n"
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
     int    proccount = 1000;
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
            case 'C':
               cleanit = 1;
               break;
            case 'I':
               iters = irng(intparm, 1, 1000000);
               break;
            case 'N':
               proccount = irng(intparm, 1, MAXPROCS);
               break;
            case 'P':
               proctype = irng(intparm, 0, 1);
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

   memset(xactscompleted, 0, sizeof(xactscompleted));
   memset(deadlocks, 0, sizeof(deadlocks)); 
   memset(lockwaits, 0, sizeof(lockwaits)); 
   memset(fatalerrs, 0, sizeof(fatalerrs)); 

   if (cleanit)
      cleanup();

   for (i = 1; i <= nthreads; i++)
      param[i-1] = i;

   sprintf(stmtbuff,
           "set random_seed %d",
           iters * nthreads);
   EXEC SQL execute immediate :stmtbuff;
   EXEC SQL select count(*) into :proccount from iiprocedure 
            where dbp_name like 'dbpv1%';

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
	int idx, pval, dval;
        int hk,lk,rb;
        int procs;
	char connectName[45];
        char nodename[65];
	char stmtbuff[1024];
	char procname[258];
   EXEC SQL end declare section;

   int loopcnt, q, cmd, xs, xst, i,j,k, error_code, reconn, neverdisc;
   char *rbstr;

   pval = *p;
   loopcnt = iters;
   xst = xs = xperconn;
   procs = proccount;

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

         EXEC SQL set autocommit off; 
         EXEC SQL set session with on_error = rollback transaction;

         EXEC SQL set lockmode session where readlock=nolock; 

         if (lockwait == 99)
            EXEC SQL set lockmode session where level=row, timeout=nowait; 
         else
         {
            sprintf(stmtbuff,
                    "set lockmode session where level=row, timeout=%d",
                    lockwait);
            EXEC SQL execute immediate :stmtbuff;
         }

         EXEC SQL whenever sqlerror call sqlprint; 

         EXEC SQL declare global temporary table session.dbpv1_temp
                  as select * from dbpv1_template on commit preserve rows
                  with norecovery;

      }
/*
** Transaction's queries begin here
*/
      EXEC SQL whenever sqlerror goto SQLerrorhandler; 

      EXEC SQL repeated select random(1, :procs) into :idx;

retry1:

      cmd = 1;
      q = 1;
      switch (proctype) {
         case 0:
            sprintf(procname, "dbpv1_proc_0_%06d", idx);
            sprintf(stmtbuff, "execute procedure %s", procname);
            break;
         case 1:
            sprintf(procname, "dbpv1_proc_1_%06d", idx);
            sprintf(stmtbuff, "execute procedure %s (p1=session.dbpv1_temp)",
                    procname);
            EXEC SQL execute immediate :stmtbuff;
            break;
      }
      EXEC SQL execute immediate :stmtbuff;
      EXEC SQL commit;

      xactscompleted[pval-1]++;

      if (verbose == MAXVERBOSE)
      {
         printf("T%02d:%07d %s\n",
         pval, i+1, procname);
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
   running--;
   if (verbose == MAXVERBOSE)
      printf("T%02d DONE, %d running\n", pval, running);
}
void cleanup()
{
   EXEC SQL begin declare section;
      int i, j, keyval;
      char  stmtbuff[1024];
   EXEC SQL end declare section;

   EXEC SQL whenever sqlerror continue;
   EXEC SQL set autocommit on;

   printf("Cleaning up...\n");

   for (i = 1; i <= MAXPROCS; i++)
   {
      sprintf(stmtbuff, "drop procedure dbpv1_0_proc%06d", i);
      EXEC SQL execute immediate :stmtbuff;
      sprintf(stmtbuff, "drop procedure dbpv1_1_proc%06d", i);
      EXEC SQL execute immediate :stmtbuff;
   }
   EXEC SQL drop table dbpv1_template; 
}
/*
** Function to create db objects
*/
void createobjs()
{
   EXEC SQL begin declare section;
      int i, keyval;
      char  stmtbuff[2048];
   EXEC SQL end declare section;

   EXEC SQL set autocommit on;

   if (cleanit)
      cleanup();

   printf("Creating objects...\n");

   EXEC SQL create table dbpv1_template (col1 int, col2 char(100));
   EXEC SQL insert into dbpv1_template values
                   (1000, 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA');
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;
   EXEC SQL insert into dbpv1_template select * from dbpv1_template;

   for (i = 1; i <= proccount; i++)
   {
      switch (proctype) {
         case 0:
            sprintf(stmtbuff,
                 "create procedure dbpv1_proc_0_%06d as declare i integer;"
                 "begin i=1; end", i);
            break;
         case 1:
            sprintf(stmtbuff,
                 "create procedure dbpv1_proc_1_%06d "
                 "(gtt1 set of (col1 int, col2 char(100))) as "
                 "declare v1 integer; begin "
                 "select avg(col1) into :v1 from gtt1 where "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAE' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAF' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAG' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAH' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAI' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAJ' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAK' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAL' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAN' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAO' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQ' and "
                 "col2 != 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAR';"
                 "end", i);
            break;
      }
      EXEC SQL execute immediate :stmtbuff;
   }
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
      int totsecs;
      int actupds, totrollbacks;
   EXEC SQL end declare section;

   int  i, tps;
   totfatal = totupds = totxacts = totdeadlocks = totlockwaits = 0;
   totrollbacks = 0;

   printf("\nCompiling results...\n\n");

   for (i=0; i < nthreads; i++)
   {
      totxacts += xactscompleted[i];
      totdeadlocks += deadlocks[i];
      totlockwaits += lockwaits[i];
      totfatal += fatalerrs[i];
   }

   EXEC SQL select
        int4(interval('seconds', date(:endtime) - date(:starttime)))
                       into :totsecs;
   if (totsecs == 0)
      totsecs = 1;
   EXEC SQL select date(:endtime) - date(:starttime) into :etime;

   tps = totxacts/totsecs;
   if (tps == 0)
      tps = 1;

   printf(" Runtime Summary\n");
   printf("-------------------------------------------------\n");
   printf(" Procedure count         : %d\n", proccount);
   printf(" Procedure type          : %d\n", proctype);
   printf(" Start time              : %s\n", starttime);
   printf(" End time                : %s\n", endtime);
   printf(" Elapsed time            : %s\n", etime);
   printf(" Threads started         : %d\n", nthreads);
   printf(" Deadlocks               : %d\n", totdeadlocks);
   printf(" Lockwait timeouts       : %d\n", totlockwaits);
   printf(" Fatal SQL errors        : %d\n", totfatal);
   printf(" Total transactions      : %d\n", totxacts);
   printf(" TPS                     : %d\n", tps);

}
