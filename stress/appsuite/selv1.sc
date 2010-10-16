/*
**  Copyright (c) 2005, 2007 Ingres Corp.
**  This file is distributed under the CA Trusted Open Source License (CATOSL).
**  For the exact terms of the license go to http://ca.com/opensource/catosl
**
**  Stress Test Application Suite
**
**  selv1.sc 
**
**  History:
**
**  01-Sep-2005 sarjo01: Created from updv2 + sbtree
**  14-Oct-2005 sarjo01: Feature sync up
**  31-May-2007 (Ralph Loen) Bug 118428
**     Ported to VMS.
**  15-Nov-2007 sarjo01: Moved incorrectly placed COMMIT statement 
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

#define MAXROWSIZE 10000

void doit(int *p);
void checkit();
void print_err();
void createobjs();
void print_sqlcode_exit();
void cleanup();

HANDLE  h[MAXCHILDTHREADS];

int xactscompleted[MAXCHILDTHREADS];
int deadlocks[MAXCHILDTHREADS];
int lockwaits[MAXCHILDTHREADS];
int fatalerrs[MAXCHILDTHREADS];
int dataerrs[MAXCHILDTHREADS];

int nthreads = 8;
int iters = 1000;
int lockwait = 0;
int nolock = 0;
int qv = 1;
int locklevel = 'R';
int parts = 1;
int verbose = MINVERBOSE; 
int xperconn = 0; 
int secondary = 0; 
int abortfatal = 0;
int retryconnect = 0;
char tblstruct = 'B';
int page_size = 4;
int row_size = 250;
int running;

char  *syntax =
 "\n"
 "Syntax:     selv1 <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize selv1 database objects\n"
 "            run     - execute selv1 program, display results\n"
 "            cleanup - delete all selv1 database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -d   init     Enable secondary index         none             disabled\n"
 "   -g   init     Set page size                  2(k),4(k),8(k),  4\n"
 "                                                1(6k),3(2k)\n"
 "   -i   run      Set transaction count (per     1 to 1000000     1000\n"
 "                 thread)\n"
 "   -n   run      Enable readlock=nolock         none             disabled\n"
 "   -o   init     Set table row size             20 to 10000      250\n"
 "   -p   init     Enable partitions              1 to 64          1\n"
 "   -q   run      Set query variant              1 to 5           1\n"
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
            case 'D':
               secondary = 1; 
               break;
            case 'G':
               page_size = irng(intparm, 1, 8);
               break;
            case 'I':
               iters = irng(intparm, 1, 1000000);
               break;
            case 'N':
               nolock = 1; 
               break;
            case 'O':
               row_size = irng(intparm, 20, MAXROWSIZE);
               break;
            case 'P':
               parts = irng(intparm, 1, 64);
               break;
            case 'Q':
               qv = irng(intparm, 1, 2);
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
   memset(dataerrs, 0, sizeof(dataerrs)); 

   printf("Cleaning up...\n");

   EXEC SQL select count(*), max(keyval) into
            :rowcount, :highkey from selv1tbl1;

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
	int pval, dval, cmdval;
        int hk, cntval;
        int keyval;
	char connectName[45];
	char stmtbuff[257];
        char keyvalc[9];
   EXEC SQL end declare section;

   int loopcnt, q, cmd, xs, xst, i,j,k, error_code, reconn, neverdisc;
   int rows, trows, rb, derror;

   pval = *p;
   hk = highkey;
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

         if (nolock == 0)
            EXEC SQL set lockmode session where level=row, readlock=system;
         else
            EXEC SQL set lockmode session where level=row, readlock=nolock;

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
      cmd=1;
      q=1;
      EXEC SQL repeated select random(1, :hk) into :keyval;

      keyvalc[0] = '\0';

retry1:

      switch (qv)
      {
         case 1:
            q=2;
            EXEC SQL repeated select keyvalc into :keyvalc
                     from selv1tbl1 where keyval = :keyval; 
            break;
         case 2:
            q=3;
            EXEC SQL repeated select first 1 keyvalc into :keyvalc
                     from selv1tbl1 where keyval = :keyval order by 1; 
            break;
      }
      rows = sqlca.sqlerrd[2];

      EXEC SQL commit;

      if (rows == 0)
      {
         printf("T%02d:%07d K:%08d NO ROW\n", pval, i+1, keyval);
      }
      else
      {
         derror = (keyval != atoi(keyvalc)) ? 1 : 0;
         if (derror)
         {
            dataerrs[pval-1]++; 
            printf("T%02d:%07d K:%08d DATA ERROR\n", pval, i+1, keyval);
         }
         else if (verbose == MAXVERBOSE)
            printf("T%02d:%07d K:%08d\n", pval, i+1, keyval);
   
      }

      if (neverdisc == 0 && --xs == 0)
      { 
         EXEC SQL disconnect :connectName;
         xs = xst;
         reconn = 1;
      }

      q=4;
      xactscompleted[pval-1]++;

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
   running--;
   if (verbose == MAXVERBOSE)
      printf("T%02d DONE, %d running\n", pval, running);
}
void cleanup()
{
   EXEC SQL whenever sqlerror continue;
   EXEC SQL set autocommit on;

   printf("Cleaning up...\n");

   EXEC SQL drop table selv1tbl1;
   EXEC SQL commit;

}
/*
** Function to create db objects
*/
void createobjs()
{
   EXEC SQL begin declare section;
      int i, keyval;
      char keyvalc[9];
      varchar struct {
         short slen;
         char  dbuff[256];
         char  stmtbuff[1024];
      } vch1;
   EXEC SQL end declare section;

   char *structstr, *pgstr, padstr[10], rsstr[9];

   EXEC SQL set autocommit on;

   cleanup();

   EXEC SQL whenever sqlerror call print_sqlcode_exit;

   printf("Creating objects...\n");

   sprintf(padstr, "%d", row_size - 12);

   switch (page_size)
   {
      case 1:
         pgstr = "16384";
         break;
      case 2:
         pgstr = "2048";
         break;
      case 3:
         pgstr = "32768";
         break;
      case 4:
         pgstr = "4096";
         break;
      case 8:
         pgstr = "8192";
         break;
   }
   switch (tblstruct)
   {
      case 'B':
         structstr = "btree";
         break;
      case 'H':
         structstr = "hash";
         break;
      case 'I':
         structstr = "isam";
         break;
   }
   sprintf(stmtbuff,
           "create table selv1tbl1 " 
           "(keyval int not null, keyvalc char(8) not null,"
           " reserved char(%s) not null)"
           " with page_size=%s",
           padstr, pgstr);
   EXEC SQL execute immediate :stmtbuff;

   for (i = 1; i <= rowcount; i++)
   {
      sprintf(keyvalc, "%08d", i); 
      EXEC SQL repeated insert into selv1tbl1 values
               (:i, :keyvalc, 'Filler');
   }

   if (parts > 1)
   {
      sprintf(stmtbuff,
              "modify selv1tbl1 to reconstruct with partition="\
              "(hash on keyval %d partitions)", parts);
      EXEC SQL execute immediate :stmtbuff;
   }

   if (secondary == 0)
   {
      sprintf(stmtbuff,
              "modify selv1tbl1 to %s unique on keyval",
              structstr); 
      EXEC SQL execute immediate :stmtbuff;
   }
   else
   {
      sprintf(stmtbuff,
              "create unique index selv1tbl1_idx1 on selv1tbl1"
              " (keyval) with structure=%s, page_size=%s",
              structstr, pgstr);
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
      int u1, u2, u2c; 
      int t1cnt;
   EXEC SQL end declare section;

   int  i, totdataerrs;
   totfatal = totupds = totxacts =
   totdeadlocks = totdataerrs = totlockwaits = 0;

   printf("\nCompiling results...\n\n");

   for (i=0; i < nthreads; i++)
   {
      totxacts += xactscompleted[i];
      totdeadlocks += deadlocks[i];
      totlockwaits += lockwaits[i];
      totfatal += fatalerrs[i];
      totdataerrs += dataerrs[i];
   }

   EXEC SQL select
        int4(interval('seconds', date(:endtime) - date(:starttime)))
                       into :totsecs;
   if (totsecs == 0)
      totsecs = 1;
   EXEC SQL select date(:endtime) - date(:starttime) into :etime;

   EXEC SQL select count(*) into :u1 from selv1tbl1; 

   printf(" Data Integrity Check\n");
   printf("-------------------------------------\n");
   printf(" Initial row count        : % 8d\n\n", rowcount);

   printf(" Runtime Summary\n");
   printf("-------------------------------------------------\n");
   printf(" Start time               : %s\n", starttime);
   printf(" End time                 : %s\n", endtime);
   printf(" Elapsed time             : %s\n", etime);
   printf(" Threads started          : %d\n", nthreads);
   printf(" Deadlocks                : %d\n", totdeadlocks);
   printf(" Lockwait timeouts        : %d\n", totlockwaits);
   printf(" Fatal SQL errors         : %d\n", totfatal);
   printf(" Data errors              : %d\n", totdataerrs);
   printf(" Total transactions       : %d\n", totxacts);
   printf(" TPS                      : %d\n",
            totxacts/totsecs);
}
