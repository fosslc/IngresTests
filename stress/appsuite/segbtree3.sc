/*
**  Copyright (c) 2005, 2007 Ingres Corporation
**
**  Stress Test Application Suite
**
**  segbtree3.sc
**
**  History:
**
**  22-Dec-2004 sarjo01: Created
**  18-Mar-2005 sarjo01: Change MAXTHREADS to MAXCHILDTHREADS
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
exec sql include sqlca;   

#define MAXCHILDTHREADS 100
#define MAXKEY 2147483647
#define DELFREQ 10

void doit(int *p);
void checkit();
int print_err();
void createobjs();

HANDLE  h[MAXCHILDTHREADS];

int delcount[MAXCHILDTHREADS];
int addcount[MAXCHILDTHREADS];
int deadlocks[MAXCHILDTHREADS];
int nthreads = 8;
int iters = 1000;
int parts = 1;
int verbose = 0; 
int dodelete = 0; 
int xperconn = 0; 
int page_size = 4; 
char pgmmode = 'B';

int startkey = 1;
int rowcount = 10000;
int sfactor = 1; 

int delfreq = DELFREQ; 
int running;

char  *syntax =
 "\n" \
 "Syntax:     segbtree3 <database> <function> [ <option> <option> ... ]\n\n" \
 "<database>  target database name\n" \
 "<function>  run    - execute segbtree3 program, display results\n" \
 "<option>    program option of the form -x[value]\n\n" \
 " Option Function Description                    Param Values     Default\n" \
 " ------ -------- ------------------------------ ---------------- -------\n" \
 "   -d    run     Enable row delete per n xacts   1 to 100000     disabled\n" \
 "   -f    run     Set key sparseness factor       1 to 100000     1\n" \
 "   -g    run     Set page size                   2(k),4(k),8(k), 4\n" \
 "                                                 1(6k),3(2k)      \n" \
 "   -h    run     Set high key value              1 to 2147483647 10000\n" \
 "   -i    run     Set transaction count (per      1 to 1000000    1000\n" \
 "                 thread)\n" \
 "   -k    run     Set starting key value          1 to 100000     1\n" \
 "   -l    run     Set low key value               1 to 100000     1\n" \
 "   -m    run     Set pgm mode                    I(nit), R(un),  B\n" \
 "                                                 B(oth)\n" \
 "   -p    run     Enable partitions               1 to 64         1\n" \
 "   -r    run     Set initial row count           1 to 100000     10000\n" \
 "   -t    run     Set no. of client threads       1 to 100        8\n" \
 "   -v    run     Enable verbose output mode      none            disabled\n" \
 "   -x    run     Set no. of transactions per     1 to 1000,      0\n" \
 "                 connection                      0=never disconn\n"\
 "\n";

exec sql begin declare section;
     char   *dbname;
     int    eval;
     char   cmdline[257];
     char   starttime[257];
     char   endtime[257];
     char   etime[257];
     char   stmtbuff[257];
     int    numaccts;
     int    lowkey = 1;
     int    highkey = 10000;
     int    initrows; 
exec sql end declare section;

main(int argc, char *argv[])
{
   int i, stat;
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
   dbname   = argv[1];
   strcpy(cmdline, ""); 
/*
** Process command line options
*/
   for (i = 3; i < argc ; i++)
   {
      if (*argv[i] == '-' && *(argv[i]+1) != '\0')
      {
         switch (toupper(*(argv[i]+1)))
         {
            case 'D':
               dodelete = 1;
               delfreq = atoi(argv[i]+2); 
               if (delfreq < 1 || delfreq > 100000)
                  delfreq = DELFREQ;
               break;
            case 'F':
               sfactor = atoi(argv[i]+2); 
               if (sfactor < 1 || sfactor > 10000)
                  sfactor = 1;
               break;
            case 'G':
               page_size = atoi(argv[i]+2); 
               if (page_size < 1 || page_size > 8)
                  page_size = 8;
               break;
            case 'H':
               highkey = atoi(argv[i]+2); 
               if (highkey < 1 || highkey > MAXKEY)
                  highkey = 10000;
               break;
            case 'I':
               iters = atoi(argv[i]+2); 
               if (iters < 1 || iters > 1000000)
                  iters = 1000;
               break;
            case 'K':
               startkey = atoi(argv[i]+2); 
               if (startkey < 1 || startkey > 100000)
                  startkey = 1;
               break;
            case 'L':
               lowkey = atoi(argv[i]+2); 
               if (lowkey < 1 || lowkey > 100000)
                  lowkey = 1;
               break;
            case 'M':
               pgmmode = toupper(*(argv[i]+2)); 
               if (pgmmode != 'B' &&
                   pgmmode != 'I' &&
                   pgmmode != 'R')
                   pgmmode = 'B';
               break;
            case 'P':
               parts = atoi(argv[i]+2);
               if (parts < 1 || parts > 64)
                  parts = 1;
               break;
            case 'R':
               rowcount = atoi(argv[i]+2); 
               if (rowcount < 1 || rowcount > 100000)
                  rowcount = 10000;
               break;
            case 'T':
               nthreads = atoi(argv[i]+2); 
               if (nthreads < 1 || nthreads > MAXCHILDTHREADS)
                  nthreads = 8;
               break;
            case 'V':
               verbose = 1;
               break;
            case 'X':
               xperconn = atoi(argv[i]+2); 
               if (xperconn < 0 || xperconn > 1000)
                  xperconn = 0;
               break;
         }
      } 
   }
   running = nthreads;

   exec sql whenever sqlerror stop;
   exec sql connect :dbname as 'mastercon';
/*
** Only allowable function is 'run'
*/
   if (stricmp(argv[2], "run") != 0)
   {
      printf(syntax);
      exit(-1);
   }
/*
** Make sure lowkey <= highkey
*/
   if (highkey < lowkey)
   {
      i = highkey;
      highkey = lowkey;
      lowkey = i;
   } 
/*
** Prepare for 'run'
** Create objects, reset transaction counts and record starting time
*/ 
   if (pgmmode != 'R') 
      createobjs();
   if (pgmmode == 'I') 
   {
      exec sql disconnect 'mastercon';
      printf("\n");
      exit(0);
   }

   EXEC SQL select count(*) into :initrows from segbtree3tbl1;
   exec sql select date('now') into :starttime; 
   exec sql commit;

   memset(delcount, 0, sizeof(delcount)); 
   memset(addcount, 0, sizeof(addcount)); 
   memset(deadlocks, 0, sizeof(deadlocks)); 

   for (i = 1; i <= nthreads; i++)
      param[i-1] = i;
/*
** Original parent thread now creates n child threads that will execute
** the program, beginning in the function doit().
** Parent thread will wait for them to finish.
*/
#ifdef _WIN32
   for (i = 0; i < nthreads; i++)
   {
      h[i] = CreateThread( NULL, 0, (LPTHREAD_START_ROUTINE)doit,
				 &param[i], 0, &lpThreadId[i]);
   }
   if (!verbose)
      printf("STARTED %d THREADS...\n", nthreads);
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
   if (!verbose)
      printf("STARTED %d THREADS...\n", nthreads);

   for (i = 0; i < nthreads; i++)
   {
      pthread_join(lpThreadId[i], NULL);
   }
#endif
/*
** Child threads have all terminated.
** Parent thread records ending time and performs data
** integrity check.
*/    
   exec sql select date('now') into :endtime; 
   checkit();
   exec sql disconnect 'mastercon';
   printf("\n");

   exit(0);
}

int print_err()
{
   exec sql begin declare section;
      char dsp[550];
   exec sql end declare section;

   exec sql inquire_sql (:dsp = ERRORTEXT);
   printf("%s\n",dsp);

   return -1;
}
void fatalerr(int where, int accid, int ecode)
{
   printf("Fatal error: %d, %d, %d\n", where, accid, ecode);
}
/*
** Child thread entry point
*/
void doit(int *p)
{
   exec sql begin declare section;
	int pval;
	int dval;
        int hk,lk;
        double x, bal1, bal2;
	char connectName[45];
	short loop;
        int accts, acctid;
   exec sql end declare section;

   int opr, rows, xs, i, error_code, reconn, neverdisc, deled;
   char   rowbuff[256], vbuff[8];

   pval = *p;
   hk = highkey;
   lk = lowkey;

   if (verbose)
      printf("T%02d START...\n", pval);
/*
** Each thread generates a distinct connection name string
*/
   sprintf(connectName, "slavecon%d", pval);

   xs = xperconn;
   neverdisc = (xperconn == 0) ? 1 : 0;
   
   reconn = 1;
/*
** Main loop
** Program starts every new transaction here
*/
   for (i = 0; i < iters; i++)
   {
      opr = 0;
retry1:
/*
** Flag reconn indicates if we need to (re)connect
*/
      if (reconn == 1)
      {
         EXEC SQL whenever sqlerror goto discon2; 
         EXEC SQL connect :dbname as :connectName; 
         EXEC SQL set autocommit off; 
         EXEC SQL whenever sqlerror goto doretry; 

         EXEC SQL set lockmode session where level = row; 
      }
/*
** Transaction's queries begin here
**
** INSERT a row with a random key value 
*/
      if (opr == 0)
      {
         EXEC SQL repeated insert into segbtree3tbl1 values (
               random(:lk, :hk),
               uuid_to_char(uuid_create()), 
               date('now'),
               'New Filler Data' ); 
/*
** COMMIT the INSERT 
*/
         EXEC SQL commit; 
         addcount[pval-1] += 1; 
      }
/*
** Optionally, do a DELETE, check SQLCA and record how many
** got deleted. 
*/
      if (dodelete)
      {
         deled = 0;
         if ((i % delfreq) == 0)
         {
            EXEC SQL select random(:lk, :hk) into :dval;
            opr = 1;
            EXEC SQL repeated delete from segbtree3tbl1 
                     where keyval = :dval;
            deled = sqlca.sqlerrd[2]; 
/*
** COMMIT the DELETE 
*/
            EXEC SQL commit;
            delcount[pval-1] += deled; 
            if (verbose)
               printf("T%02d, i=%d, d=%d\n", pval, i+1, deled);
         }
         else if (verbose)
            printf("T%02d, i=%d\n", pval, i+1);
         opr = 0;
      }
      else if (verbose)
         printf("T%02d, i=%d\n", pval, i+1);

      if (neverdisc == 1)
      {
         reconn = 0;
         continue;
      }
      if (--xs == 0)
      { 
         xs = xperconn;
         reconn = 1;
         EXEC SQL disconnect :connectName;
         continue;
      }
      else
      {
         reconn = 0;
         continue;
      }
doretry:
      error_code = sqlca.sqlcode;
      if (error_code != -30210 && error_code != -49900)
         print_err();
      if (error_code == -49900)
      {
         if (verbose)
            printf("T%02d DEADLOCK on %s\n", pval, opr ? "DELETE" : "INSERT");
         deadlocks[pval-1] += 1; 
      }
      EXEC SQL whenever sqlerror continue; 
      EXEC SQL rollback;
      EXEC SQL disconnect :connectName;
      if (error_code == -49900 || error_code == 100 ||
          error_code == -30210 || error_code == -39100)
      {
         reconn = 1;
         xs = xperconn;
         goto retry1;
      }
      else
      {
         printf("Fatal SQL error: %d\n", error_code);
         break;
      }
discon2:
      printf("Loop connect fail: %s\n", connectName);
      print_err();
      break;
   }
   if (reconn == 0)
      EXEC SQL disconnect :connectName;
   running--;
   if (verbose)
      printf("T%02d DONE, %d running\n", pval, running);
   else if (running == 0)
      printf("%d THREADS DONE.\n", nthreads);
}
/*
** Function to create db objects
*/
void createobjs()
{
   EXEC SQL begin declare section;
      int i, keyval;
   EXEC SQL end declare section;

   char *pgsz;

   EXEC SQL WHENEVER SQLERROR CONTINUE; 
   EXEC SQL set autocommit on; 

   printf("Cleaning up...\n");
   EXEC SQL drop table segbtree3tbl1;

   EXEC SQL WHENEVER SQLERROR call sqlprint;

   printf("Creating tables...\n");
   EXEC SQL create table segbtree3tbl1
            (keyval int, data1 char(36), ts date, filler char(30000));
   for (i=1, keyval=startkey; i<=rowcount; i++)
   {
      EXEC SQL repeated insert into segbtree3tbl1 values (
               :keyval,
               'Initial Data Value', 
               date('now'),
               'Original Filler Data' ); 
      keyval += sfactor;
      if (keyval < 0 || keyval > MAXKEY)
         keyval = startkey;
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
           "modify segbtree3tbl1 to btree on keyval with page_size=%s",
           pgsz);
   EXEC SQL execute immediate :stmtbuff;

   if (parts > 1)
   {
      sprintf(stmtbuff,
              "modify segbtree3tbl1 to reconstruct with partition="\
              "(hash on keyval %d partitions)", parts);
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
      int totrows;
      int totx;
      int totsecs;
      int totidxrows;
      int totidxrows2;
      int test1;
      int page_size, row_width;
   EXEC SQL end declare section;

   EXEC SQL whenever sqlerror continue;

   int  exptotal, expadd = 0, i, totdeleted = 0;
   int  totrb = 0;

   if (dodelete)
      for (i=0; i < nthreads; i++)
         totdeleted += delcount[i];
   for (i=0; i < nthreads; i++)
   {
      expadd += addcount[i];
      totrb += deadlocks[i];
   }
   exptotal = expadd + initrows - totdeleted;

   EXEC SQL select row_width, table_pagesize into
            :row_width, :page_size from iitables
            where table_name = 'segbtree3tbl1';

   EXEC SQL select
        int4(interval('seconds', date(:endtime) - date(:starttime)))
                       into :totsecs;
   EXEC SQL select date(:endtime) - date(:starttime) into :etime;
   printf("\nSTART:      %s\nEND:        %s\nET:         %s\n",
          starttime, endtime, etime);
   if (totsecs == 0)
      totsecs = 1;


   printf("INIT ROWS:  %d\n", initrows);
   printf("EXP ADD:    %d\n", expadd);
   printf("EXP DEL:    %d\n", totdeleted);
   printf("DEADLOCKS:  %d\n", totrb);
   printf("EXP ROWS:   %d\n", exptotal);

   EXEC SQL select count(*) into :totrows from segbtree3tbl1;

   printf("ACT ROWS:   %d\n", totrows);
   printf("ROW WIDTH:  %d\n", row_width);
   printf("PAGE SIZE:  %dK\n", page_size / 1024);
   printf("THREADS:    %d\n", nthreads);
   printf("TPS:        %d\n", expadd / totsecs);
}
