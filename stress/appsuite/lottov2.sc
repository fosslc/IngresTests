/*
**  Copyright (c) 2005, 2007 Ingres Corporation
**
**  Stress Test Application Suite
**
**  lottov2.sc
**
**  History:
**
**  16-May-2003 sarjo01: Created
**  21-May-2004 sarjo01: Clean-up
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
#define TIXTABLES 32 
#define NUMS 30 
#define NUMNUMS 6 

void doit(int *p);
void results();
void adminthread();
int print_err();
void createobjs();

HANDLE  h[MAXCHILDTHREADS];

int nthreads = 8;
int sellers;
int verbose = 0; 
int xperconn = 0; 
int running;
int adminid;
int tixcounts[MAXCHILDTHREADS];

char  *syntax =
 "\n" \
 "Syntax:     lotto <database> <function> [ <option> <option> ... ]\n\n" \
 "<database>  target database name\n" \
 "<function>  run    - execute lotto program, display results\n" \
 "<option>    program option of the form -x[value]\n\n" \
 " Option Function Description                    Param Values     Default\n" \
 " ------ -------- ------------------------------ ---------------- -------\n" \
 "   -d    run     Set table forced drain count    500 to 50000    5000\n" \
 "   -m    run     Set run time in minutes         1 to 720        2\n" \
 "   -t    run     Set no. of client threads       1 to 100        8\n" \
 "   -v    run     Enable verbose output mode      none            disabled\n" \
 "   -x    run     Set no. of transactions per     1 to 1000       0\n" \
 "                 connection                      0=never discon\n" \
 "\n\n";

exec sql begin declare section;
     char   *dbname;
     int    drainage = 5000;
     char   stoptime[36];
     char   runinterval[36];
     char   starttime[36];
     char   stmtbuff[257];
     int    numaccts;
     int    runminutes = 2;
exec sql end declare section;

main(int argc, char *argv[])
{
   int i;
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
         switch (toupper(*(argv[i]+1)))
         {
            case 'D':
               drainage = atoi(argv[i]+2); 
               if (drainage < 250 || drainage > 50000)
                  drainage = 5000;
               break;
            case 'M':
               runminutes = atoi(argv[i]+2); 
               if (runminutes < 1 || runminutes > 720)
                  runminutes = 2;
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

   sellers = running = nthreads;
   nthreads++;
   adminid = nthreads;
   memset(tixcounts, '\0', sizeof(tixcounts));
/*
** Only allowable function is 'run'
*/
   if (stricmp(argv[2], "run") != 0)
   {
      printf(syntax);
      exit(-1);
   }

   exec sql whenever sqlerror stop;
   exec sql connect :dbname as 'mastercon';
   exec sql set autocommit on; 
/*
** Prepare for 'run'
** Create db objects and record starting time
*/
   createobjs();
 
   sprintf(runinterval, "%d minutes", runminutes);
   exec sql select date('now'),
                   date('now') + :runinterval
                   into :starttime, :stoptime;

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

   for (i = 0; i < nthreads; i++)
   {
      pthread_join(lpThreadId[i], NULL);
   }
#endif
/*
** Child threads have all terminated.
** Parent thread displays results
*/    
   results();
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
/*
** Function to generate numbers for a lotto ticket
*/
void dopick(unsigned char *p, unsigned char *pa)
{
   exec sql begin declare section;
      int   nums, pick;
   exec sql end declare section;

   exec sql whenever sqlerror call sqlprint;

   int   i,j;

   nums = NUMS;
   memset(pa, '\0', NUMS);

   for (i = 0; i < NUMNUMS; i++)
   {
      EXEC SQL select random(0, :nums - 1) into :pick;
      if (pick < 0 || pick > (NUMS - 1))
      {
         i--;
         printf("ERROR: Invalid random(0,%d) value: %d\n", (NUMS - 1), pick);
      }
      else if (pa[pick] == 0)
         pa[pick] = 1;
      else
         i--;
   }
   for (i = j = 0; i < NUMS; i++)
   {
      if (pa[i] == 1)
      {
         p[j] = i + 1;
         if (++j == NUMNUMS)
            break;
      }
   }
}
/*
** Child thread entry point
*/
void doit(int *p)
{
   exec sql begin declare section;
	int pval, vendorid, tabid, stopflag;
	int sc, maxsc; 
	char connectName[45];
	char sbuff[257];
        char tixid[64];
        unsigned char pix[NUMNUMS];
   exec sql end declare section;

   int xs, i, j, error_code, reconn, neverdisc;
   unsigned char pixarray[NUMS];

   pval = *p;
/*
** If this thread's ID is for the admin thread, start
** and run the admin thread from it's own entry point.
*/
   if (pval == adminid)
   {
      adminthread();
      return;
   }

   printf("T%02d START...\n", pval);

   vendorid = pval + 100000; 
/*
** Each thread generates a distinct connection name string
*/
   sprintf(connectName, "slavecon%02d", pval);

   xs = xperconn;
   neverdisc = (xperconn == 0) ? 1 : 0;

   reconn = 1;
/*
** Main loop
** Program starts every new transaction here.
*/
   for (;;)
   {
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
         EXEC SQL set lockmode on lv2tixadmin where
                   level=row, readlock=exclusive, timeout=system;
      }

retry2:
/*
** Transaction queries begin here.
**
** This SELECT returns 3 columns:
**
**      1) A universally unique ticket ID
**      2) A 1 (true) or 0 (false) value indicating if the running time
**         has expired yet
**      3) The nextval of a SEQUENCE object, used to form a ticket table
**         name
*/
      EXEC SQL select uuid_to_char(uuid_create()),
                        case when date('now') > date(:stoptime)
                             then int4(1) else int4(0) end,
                        tblseq.nextval 
                        into :tixid, :stopflag, :tabid; 
/*
** Test to see we we are still allowed to run (game still on)
*/
      if (stopflag == 1)
      {
         reconn = 0;
         break;
      }
/*
** Generate the numbers that will appear on the lotto ticket (quick pick)
*/
      dopick(pix, pixarray);
/*
** All data for the new ticket has been collected.
** Build an INSERT statement, then PREPARE and EXCUTE it (table name is
** variable, based on sequence.nextval).
*/
      sprintf(sbuff, "insert into lv2tixtable%02d values (?,?,date('now'),?)",
              tabid); 
      EXEC SQL prepare ins_stmt from :sbuff;
      EXEC SQL execute ins_stmt using :tixid, :vendorid, :pix;  
/*
** Update a table that counts the number of rows in each ticket table.
** Then SELECT the new count.
*/
      EXEC SQL repeated update lv2tixadmin set soldcount=soldcount+1,
                                            tsoldcount=tsoldcount+1
                        where tblid = :tabid;
      EXEC SQL repeated select tsoldcount, maxsoldcount into :sc, :maxsc
                        from lv2tixadmin where tblid = :tabid; 
/*
** If this ticket table's row count has reached the max,
** reset the row count to 0 and then raise the DBEVENT to wake up
** the admin thread, passing the ticket table number as a message.
** The admin thread will drain the rows from the ticket table, adding
** them to the master ticket table.
*/
      if (sc >= maxsc)
      {
         EXEC SQL repeated update lv2tixadmin set tsoldcount=0
                        where tblid = :tabid;
         sprintf(sbuff, "raise dbevent lv2tixtblevent '%02d'", tabid);
         EXEC SQL execute immediate :sbuff;
      }
/*
** COMMIT the work
*/
      EXEC SQL commit; 
/*
** Check option flag verbose to see about printing some console output
*/
      if (verbose)
         printf("*%02d,%02d,%02d,%02d,%02d,%02d* sold by %d\n",
                pix[0],pix[1],pix[2],pix[3],pix[4],pix[5],vendorid); 
/*
** Do some independent ticket counting to use for integrity check
*/
      ++(tixcounts[pval - 1]);

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
      print_err();
      EXEC SQL whenever sqlerror continue; 
      EXEC SQL rollback;
      EXEC SQL disconnect :connectName;
      if (error_code == -49900 || error_code == -39100)
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
   if (running == 1)
   {
      if (reconn == 1)
         EXEC SQL connect :dbname as :connectName;         
/*
** If this is the last thread to terminate,
** raise DBEVENT to signal admin thread to go away.
*/
      EXEC SQL raise dbevent lv2tixtblevent 'DONE';
      reconn = 0;
   }
   running--;
   if (reconn == 0)
      EXEC SQL disconnect :connectName;
   printf("T%02d DONE\n", pval);
}
/*
** Function to create db objects needed by the program
*/
void createobjs()
{
   EXEC SQL begin declare section;
      int  i, maxsold;
   EXEC SQL end declare section;

   EXEC SQL whenever sqlerror continue;

   printf("Cleaning up...\n");
   EXEC SQL drop lv2tixadmin, lv2tixtable;
   EXEC SQL drop dbevent lv2tixtblevent;
   EXEC SQL drop sequence tblseq; 

   for (i = 0; i < TIXTABLES; i++)
   {
      sprintf(stmtbuff, "drop lv2tixtable%02d", i);
      EXEC SQL execute immediate :stmtbuff;
   }
   EXEC SQL commit;

   EXEC SQL whenever sqlerror stop;

   printf("Creating dbevents...\n");
   EXEC SQL create dbevent lv2tixtblevent;

   printf("Creating ticket tables...\n");
   for (i = 0; i < TIXTABLES; i++)
   {
      sprintf(stmtbuff,
              "create table lv2tixtable%02d (tixid char(36), vendor int," \
              "ts date, pix byte(6)) with page_size=4096",
              i);
      EXEC SQL execute immediate :stmtbuff;
   }
   EXEC SQL create table lv2tixtable (tixid char(36), vendor int,
                         ts date, pix byte(6)) with page_size=4096;

   printf("Creating ticket admin table...\n");
   maxsold = drainage;
   EXEC SQL create table lv2tixadmin (tblid int, soldcount int,
                   tsoldcount int, maxsoldcount int)
            with page_size=4096;
   for (i = 0; i < TIXTABLES; i++) 
   {
      EXEC SQL insert into lv2tixadmin values (:i, 0, 0, :maxsold); 
   }
   EXEC SQL modify lv2tixadmin to hash unique on tblid;
   EXEC SQL create sequence tblseq start with 0 minvalue 0
                   maxvalue 31 cycle cache 31;

   EXEC SQL commit;

}
/*
** Function to complete lotto game simulation and
** perform data integrity check.
*/
void results()
{
   EXEC SQL begin declare section;
      int   sold, totalsold, admintotal;
      int   i, winners;
      unsigned char pix[NUMNUMS];
      unsigned char pixarray[NUMS];
      char sbuff[257];
   EXEC SQL end declare section;

   int   tixcounttotal = 0;
   int   runsecs = runminutes * 60;

   EXEC SQL whenever sqlerror stop; 

   printf("\nGAME OVER.\n");
/*
** Drain any tickets remaining in numbered ticket tables
** into the master ticket table.
*/
   printf("Draining ticket tables...\n");
   EXEC SQL commit;
   for (i = 0; i < TIXTABLES; i++)
   {
      sprintf(sbuff,
              "insert into lv2tixtable select * from lv2tixtable%02d", i);
      EXEC SQL execute immediate :sbuff;
      sprintf(sbuff,
              "modify lv2tixtable%02d to truncated", i);
      EXEC SQL execute immediate :sbuff;
      EXEC SQL commit;
   }
   printf("Sorting/counting tickets...\n");
   EXEC SQL modify lv2tixtable to hash on pix; 
   EXEC SQL select count(*) into :totalsold from lv2tixtable;
   printf("Total tickets sold (ticket table): %d\n", totalsold);

   printf("Verifying ticket count...\n");
   EXEC SQL select sum(soldcount) into :admintotal from lv2tixadmin;
   printf("Total tickets sold (verify, 1)   : %d\n", admintotal);
   for (i = 0; i < sellers; i++)
      tixcounttotal += tixcounts[i];
   printf("Total tickets sold (verify, 2)   : %d\n", tixcounttotal);
   printf("Tickets sold per second          : %d\n", tixcounttotal / runsecs);

   if (admintotal != totalsold || tixcounttotal != totalsold)
   {
      printf("ERROR: database is logically inconsistent\n");
      return;
   }

   printf("\nPicking the winning numbers...\n\n");
   dopick(pix, pixarray);
   printf("The winning numbers are:\n");
   printf("*%02d,%02d,%02d,%02d,%02d,%02d*\n\n",
                pix[0],pix[1],pix[2],pix[3],pix[4],pix[5]); 
   printf("Checking for winning tickets...\n");
   EXEC SQL select count(*) into :winners from lv2tixtable where
            pix = :pix;
   if (winners == 0)
      printf("There were no winners.\n");
   else if (winners == 1)
      printf("There was one winning ticket!\n");
   else
      printf("There were %d winning tickets!\n", winners);
}
/*
** Admin thread entry point
*/
void adminthread()
{
   EXEC SQL begin declare section;
      char etext[257];
      char sbuff[257];
   EXEC SQL end declare section;

   printf("ADMIN START...\n");

   EXEC SQL whenever sqlerror stop; 
   EXEC SQL connect :dbname as 'adminthread'; 
   EXEC SQL set autocommit off;
   EXEC SQL set lockmode session where
                level=table, readlock=exclusive, timeout=system;
   EXEC SQL set lockmode on lv2tixadmin where
                level=row, readlock=exclusive, timeout=system;
/*
** Register the DBEVENT with this session
*/
   EXEC SQL register dbevent lv2tixtblevent;

   for (;;)
   {
/*
** Go to sleep waiting for DBEVENT
*/
      EXEC SQL get dbevent with wait;
/*
** Get the event message, either a ticket table number to drain,
** or the signal "DONE", telling admin to terminate.
*/
      EXEC SQL inquire_sql(:etext=dbeventtext);
   
      if (strcmp(etext, "DONE") == 0)
      {
         EXEC SQL disconnect 'adminthread'; 
         printf("ADMIN DONE\n");
/*
** Admin thread exits here
*/
         return;
      }
/*
** Drain the ticket table, adding rows to master ticket table.
*/
      printf("ADMIN Draining ticket table %2.2s\n", etext);
      sprintf(sbuff,
              "insert into lv2tixtable select * from lv2tixtable%2.2s", etext);
      EXEC SQL execute immediate :sbuff;
      sprintf(sbuff,
              "modify lv2tixtable%2.2s to truncated", etext);
      EXEC SQL execute immediate :sbuff;
      EXEC SQL commit;
   }
}
