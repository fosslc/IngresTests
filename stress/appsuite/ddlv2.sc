/*
**  Copyright (c) 2005, 2007 Ingres Corp.
**
**  Stress Test Application Suite
**
**  ddlv2.sc 
**
**  History:
**
**  14-Oct-2005 sarjo01: Created
**  31-May-2007 (Ralph Loen) Bug 118428
**     Ported to VMS.
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
int tablecount = 10;
int iters = 100;
int lockwait = 0;
int verbose = MINVERBOSE; 
int xperconn = 0; 
int rowcount = 100000;
int abortfatal = 0;
int cleanit = 0;
int running;

char  *syntax =
 "\n"
 "Syntax:     ddlv2 <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize ddlv2 database objects\n"
 "            run     - execute ddlv2 program, display results\n"
 "            cleanup - delete all ddlv2 database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -c   run      Enable force cleanup           none             disabled\n"
 "   -i   run      Set transaction count (per     1 to 1000000     100\n"
 "                 thread)\n"
 "   -s   run      Set table count                1 to 1000        10\n"
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
            case 'S':
               tablecount = irng(intparm, 1, 1000);
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

#ifdef _WIN32
   for (i = 0; i < nthreads; i++)
   {
      h[i] = CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)doit,
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
        int tbls, tblexist;
	char connectName[45];
        char nodename[65];
	char stmtbuff[1024];
	char tblname[64];
   EXEC SQL end declare section;

   int loopcnt, q, cmd, xs, xst, i,j,k, error_code, reconn, neverdisc;
   char *rbstr;
   char atbl[1000];

   memset(atbl, 0, sizeof(atbl));
   pval = *p;
   loopcnt = iters;
   xst = xs = xperconn;
   tbls = tablecount;

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

         EXEC SQL declare global temporary table session.tmpddlv2
                  ( col01 int, col02 int, col03 int, col04 int, col05 int, 
                    col06 int, col07 int, col08 int, col09 int, col10 int, 
                    col11 int, col12 int, col13 int, col14 int, col15 int, 
                    col16 int, col17 int, col18 int, col19 int, col20 int, 
                    col21 int, col22 int, col23 int, col24 int, col25 int )  
                  on commit preserve rows with norecovery;

      }
/*
** Transaction's queries begin here
*/
      EXEC SQL whenever sqlerror goto SQLerrorhandler; 

      q = 1;

      EXEC SQL repeated select random(1, :tbls) into :idx;
      sprintf(tblname, "ddlv2_t%02d_tbl%04d", pval, idx);

retry1:

      if (atbl[idx - 1] == 0) 
      {
         tblexist = 0;
         EXEC SQL repeated select reltid into :tblexist
                  from iirelation where lowercase(relid)=:tblname;
         atbl[idx - 1] = (tblexist == 0) ? 1 : 2;
      }
      if (atbl[idx - 1] == 1) 
      {
         cmd = 0;
         sprintf(stmtbuff,
                 "create table %s as select "
                 "* from session.tmpddlv2 with page_size=4096",
                 tblname);
         EXEC SQL execute immediate :stmtbuff;
         sprintf(stmtbuff,
                 "create index %s_idx on %s (col01, col02, col03)",
                 tblname,tblname);
         EXEC SQL execute immediate :stmtbuff;
         sprintf(stmtbuff,
                 "grant all on %s to public",
                 tblname);
         EXEC SQL execute immediate :stmtbuff;
         EXEC SQL commit;
         atbl[idx - 1] = 2;
      }
      else
      {
         cmd = 1;
         sprintf(stmtbuff,
                 "drop table %s",
                 tblname);
         EXEC SQL execute immediate :stmtbuff;
         EXEC SQL commit;
         atbl[idx - 1] = 1;
      }

      xactscompleted[pval-1]++;

      if (verbose == MAXVERBOSE)
      {
         printf("T%02d:%07d %s %s\n",
         pval, i+1, (cmd == 1)?"DROP  ":"CREATE", tblname);
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

   for (i = 1; i <= 1000; i++)
   {
      for (j = 1; j <= MAXCHILDTHREADS; j++)
      {
         sprintf(stmtbuff,
                 "drop table ddlv2_t%02d_tbl%04d",
                 j, i);
         EXEC SQL execute immediate :stmtbuff;
      }
   }
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

   EXEC SQL whenever sqlerror call print_sqlcode_exit;

   printf("Creating objects...\n");

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
   printf(" Start time               : %s\n", starttime);
   printf(" End time                 : %s\n", endtime);
   printf(" Elapsed time             : %s\n", etime);
   printf(" Threads started          : %d\n", nthreads);
   printf(" Deadlocks                : %d\n", totdeadlocks);
   printf(" Lockwait timeouts        : %d\n", totlockwaits);
   printf(" Fatal SQL errors         : %d\n", totfatal);
   printf(" Total transactions       : %d\n", totxacts);
   printf(" TPS                      : %d\n", tps);

}
