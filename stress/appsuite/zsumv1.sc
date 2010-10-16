/*
**  Copyright (c) 2005, 2007 Ingres Corporation
**
**  Stress Test Application Suite
**
**  zsumv1.sc
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
#define BALANCE 1000.00 

void doit(int *p);
void checkit();
int print_err();
void createobjs(int count);

HANDLE  h[MAXCHILDTHREADS];

int nthreads = 8;
int ltype = 'P';
int iters = 1000;
int goahead = 0;
int addxrec = 0;
int caccts = 10000;
int rbfreq = 0; 
int verbose = 0; 
int xperconn = 0; 
int running;

char  *syntax =
 "\n" \
 "Syntax:     zsumv1 <database> <function> [ <option> <option> ... ]\n\n" \
 "<database>  target database name\n" \
 "<function>  init   - initialize database\n" \
 "            run    - execute zsumv1 program, display results\n" \
 "<option>    program option of the form -x[value]\n\n" \
 " Option Function Description                    Param Values     Default\n" \
 " ------ -------- ------------------------------ ---------------- -------\n" \
 "   -a    init    Set no. of account records      1 to 100000     10000\n" \
 "   -b    run     Set forced rollback frequency   2 to 1000       disabled\n" \
 "   -c    init    Set account table structure     H(ash), B(tree) H\n" \
 "   -d    init    Enable secondary indexes on     none            disabled\n" \
 "                 account tables (heap tables)\n" \
 "   -i    run     Set transaction count (per      1 to 1000000    1000\n" \
 "                 thread)\n" \
 "   -l    run     Set lock type                   P(age), R(ow)   P\n" \
 "   -r    init    Set account table row size      L(arge),        L\n" \
 "                                                 S(mall)\n" \
 "   -s    run     Enable transaction log table    none            disabled\n" \
 "   -t    run     Set no. of client threads       1 to 64         8\n" \
 "   -u    init    Specify unique keys             none            disabled\n" \
 "   -v    run     Enable verbose output mode      none            disabled\n" \
 "   -x    run     Set no. of transactions per     1 to 1000,      0\n" \
 "                 connection                      0=never disconn\n"\
 "\n";

exec sql begin declare section;
     char   *dbname;
     int    eval;
     char   cmdline[257];
     char   stmtbuff[257];
     int    numaccts;
     char   rsize[2] = "L";
     int    useidx = 0;
     char   tstruct[2] = "H";
     int    uniquevals = 0; 
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
            case 'A':
               caccts = atoi(argv[i]+2); 
               if (caccts < 1 || caccts > 100000)
                  caccts = 10000;
               break;
            case 'B':
               rbfreq = atoi(argv[i]+2); 
               if (rbfreq < 2 || rbfreq > 1000)
                  rbfreq = 0;
               break;
            case 'C':
               *tstruct = toupper(*(argv[i]+2));
               if (*tstruct != 'B' && *tstruct != 'H')
                  *tstruct = 'H';
               break;
            case 'D':
               useidx = 1;
               break;
            case 'I':
               iters = atoi(argv[i]+2); 
               if (iters < 1 || iters > 1000000)
                  iters = 1000;
               break;
            case 'L':
               ltype = toupper(*(argv[i]+2));
               if (ltype != 'R' && ltype != 'P')
                  ltype = 'P';
               break;
            case 'R':
               *rsize = toupper(*(argv[i]+2));
               if (*rsize != 'L' && *rsize != 'S')
                  *rsize = 'L';
               break;
            case 'S':
               addxrec = 1;
               break;
            case 'T':
               nthreads = atoi(argv[i]+2); 
               if (nthreads < 1 || nthreads > MAXCHILDTHREADS)
                  nthreads = 8;
               break;
            case 'U':
               uniquevals = 1;
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
/*
** Build a command line image record for admin table
*/
   sprintf(cmdline,
   "threads: %d, xacts: %d, x/conn: %d, locks: %c, rb: %d, log: %d",
   nthreads, iters, xperconn, ltype, rbfreq, addxrec); 

   running = nthreads;

   exec sql whenever sqlerror stop;
   exec sql connect :dbname as 'mastercon';
   exec sql set autocommit on; 
/*
** Only allowable functions are 'run' and 'init'
*/
   if (stricmp(argv[2], "init") == 0)
   {
      createobjs(caccts);
      exec sql disconnect 'mastercon';
      exit(0);
   }
   if (stricmp(argv[2], "run") != 0)
   {
      printf(syntax);
      exit(-1);
   }
/*
** Prepare for 'run'
** Reset transaction counts and record starting time
*/ 
   printf("Cleaning up...\n");
   exec sql update zsumv1acct1 set rxacts = 0;
   exec sql update zsumv1acct2 set rxacts = 0;
   exec sql modify zsumv1transrec to truncated; 
   exec sql modify zsumv1transrec to hash on seqnum;
   exec sql update zsumv1admin1 set t1 = 'now', cmdline = :cmdline;
   exec sql select accts into :numaccts from zsumv1admin1;
   exec sql commit;

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
   goahead = 1;
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
   goahead = 1;

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
   exec sql update zsumv1admin1 set t2 = 'now';
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
        double x, bal1, bal2;
	char connectName[45];
	short loop;
        int accts, acctid;
   exec sql end declare section;

   int xs, i, error_code, reconn, neverdisc;

   pval = *p;

   printf("T%02d START...\n", pval);
/*
** Each thread generates a distinct connection name string
*/
   sprintf(connectName, "slavecon%d", pval);
/*
** Threads spin here until all are ready
*/
   while (!goahead)
      PCsleep(0);
   xs = xperconn;
   neverdisc = (xperconn == 0) ? 1 : 0;
   
   reconn = 1;
/*
** Main loop
** Program starts every new transaction here
*/
   for (i = 0; i < iters; i++)
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

         if (ltype == 'P')
         {
            EXEC SQL set lockmode session where  
                   level=page, readlock=exclusive, timeout=system;
         }
         else
         {
            EXEC SQL set lockmode session where  
                   level=row, readlock=exclusive, timeout=system;
         }
         EXEC SQL set lockmode on zsumv1transrec where readlock=shared;
      }
/*
** Transaction's queries begin here
**
** SELECT a random account number
*/
      EXEC SQL repeated select random(0, :numaccts - 1) into :acctid; 
/*
** SELECT the current balance for this account from each acct table.
** This query will result in an exclusive lock on both tables.
*/
      EXEC SQL repeated select a1.bal, a2.bal into :bal1, :bal2
               from zsumv1acct1 a1, zsumv1acct2 a2
               where a1.accid = a2.accid and 
                     a2.accid = :acctid;
      if (sqlca.sqlcode > 0)
         fatalerr(1, acctid, sqlca.sqlcode);
/*
** This logic goes as follows:
**
** If the balance in acct1 is still > 0.00 then
**    Deduct 1.00 from acct1 and add 1.00 to acct2
**    Sum of acct1 + acct2 should always be 1000.00
** Else
**    Switch the 2 balances (acct1 and acct2).
**    Acct1 should now be 1000.00 and acct2 should be 0.00.
** Endif
*/
      if (bal1 > 0.0)
      {
         EXEC SQL repeated update zsumv1acct1 set bal = bal - 1.00,
                                  xacts = xacts + 1, rxacts = rxacts + 1
                         where accid = :acctid;

         if (sqlca.sqlcode > 0)
            fatalerr(2, acctid, sqlca.sqlcode);
         EXEC SQL repeated update zsumv1acct2 set bal = bal + 1.00,
                                  xacts = xacts + 1, rxacts = rxacts + 1
                         where accid = :acctid;
         if (sqlca.sqlcode > 0)
            fatalerr(3, acctid, sqlca.sqlcode);
      } 
      else
      {
         EXEC SQL repeated update zsumv1acct1 set bal = money(:bal2),
                                  xacts = xacts + 1, rxacts = rxacts + 1
                           where accid = :acctid;
         if (sqlca.sqlcode > 0)
            fatalerr(4, acctid, sqlca.sqlcode);
         EXEC SQL repeated update zsumv1acct2 set bal = money(:bal1),
                                  xacts = xacts + 1, rxacts = rxacts + 1
                           where accid = :acctid;
         if (sqlca.sqlcode > 0)
            fatalerr(5, acctid, sqlca.sqlcode);
      } 
/*
** Check option flag addxrec to see if we should do an INSERT
*/
      if (addxrec)
         EXEC SQL repeated insert into zsumv1transrec values
                  (:acctid, :pval, 'now', 0);
/*
** Check option flag rbfreq to see if we should do a ROLLBACK
*/      
      if (rbfreq != 0 && ((acctid + (int) bal1 + i) % rbfreq) == 0)
      {
         printf("T%02d, %d, ROLLING BACK\n", pval, i+1);
         EXEC SQL rollback;
         if (neverdisc == 1)
         {
            reconn = 0;
         }
         else
         {
            EXEC SQL disconnect :connectName;
            xs = xperconn;
            reconn = 1;
            goto retry1;
         }
      }
/*
** COMMIT the work
*/
      EXEC SQL commit; 
/*
** Check option flag verbose to see about printing some console output
*/
      if (verbose)
         printf("T%02d, %d\n", pval, i+1);

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
      if (error_code == -49900 || error_code == 100 || error_code == -39100)
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
   printf("T%02d DONE, %d running\n", pval, running);
}
void domod(char *tblname)
{
   char  *str, *unq;
   
   str = (*tstruct == 'H') ? "hash" : "btree"; 
   unq = (uniquevals == 0) ? "" : "unique"; 
   sprintf(stmtbuff, "modify %s to %s %s on accid", tblname, str, unq);
   
   EXEC SQL WHENEVER SQLERROR STOP;
   EXEC SQL execute immediate :stmtbuff; 
}
void doidx(char *tblname)
{
   char  *str, *unq;

   str = (*tstruct == 'H') ? "hash" : "btree"; 
   unq = (uniquevals == 0) ? "" : "unique"; 
   sprintf(stmtbuff,
           "create %s index %six on %s(accid) with structure=%s",
           unq, tblname, tblname, str); 
   EXEC SQL WHENEVER SQLERROR STOP;
   EXEC SQL execute immediate :stmtbuff; 
}
/*
** Function to create db objects needed by the program
*/
void createobjs(int count)
{
   EXEC SQL begin declare section;
      int  i;
      int  accts;
      double bal;
   EXEC SQL end declare section;

   accts = count;
   bal = BALANCE;

   EXEC SQL WHENEVER SQLERROR CONTINUE;

   printf("Cleaning up...\n");
   EXEC SQL drop zsumv1admin1, zsumv1acct1, zsumv1acct2, zsumv1transrec;
   EXEC SQL drop procedure zsumv1transrec;
   EXEC SQL drop sequence zsumv1transrecseq;

   EXEC SQL WHENEVER SQLERROR STOP;

   printf("Creating table zsumv1admin1...\n");
   EXEC SQL create table zsumv1admin1 (accts int, xacts int, rsize char(1),
                                 useidx int, tstruct char(1), uniquevals int, 
                                 t1 date, t2 date, cmdline char(256));

   printf("Creating table zsumv1acct1...\n");
   if (*rsize == 'L')
   {
      EXEC SQL create table zsumv1acct1
            (accid int, bal money, xacts int, rxacts int, reserved char(1500))
             with page_size=4096;
   }
   else
   {
      EXEC SQL create table zsumv1acct1
            (accid int, bal money, xacts int, rxacts int, reserved char(150))
             with page_size=4096;
   }
   EXEC SQL insert into zsumv1admin1 values
            (:accts, 0, :rsize, :useidx, :tstruct,
             :uniquevals, 'now', 'now', '');

   for (i = 0; i < count; i++)
   {
      EXEC SQL REPEATED INSERT into zsumv1acct1 values
               (:i, money(:bal), 0, 0, 'stuff');
   } 
   printf("Creating table zsumv1acct2...\n");
   EXEC SQL create table zsumv1acct2 (accid, bal, xacts, rxacts, reserved)
               as select accid, money(0.0), xacts, rxacts, reserved
               from zsumv1acct1 with page_size=4096;

   if (useidx == 0)
   {
      printf("Modifying acct tables...\n");
      domod("zsumv1acct1");
      domod("zsumv1acct2");
   }
   else
   {
      printf("Creating acct table secondary indexes...\n");
      doidx("zsumv1acct1");
      doidx("zsumv1acct2");
   }

   printf("Creating table zsumv1transrec...\n");
   EXEC SQL create table zsumv1transrec
            (accid int, slaveid int, ts date, seqnum int);
   EXEC SQL modify zsumv1transrec to hash on seqnum;
/*
** Optional transaction log table has a RULE / DB PROCEDURE
** that uses a SEQUENCE object
*/
   EXEC SQL create sequence zsumv1transrecseq cache 100;
   EXEC SQL create procedure zsumv1transrec (tidval int) as
            begin
               update zsumv1transrec set seqnum = zsumv1transrecseq.nextval
                  where tid = :tidval;
            end;
   EXEC SQL create rule trans1 after insert into zsumv1transrec
            execute procedure zsumv1transrec(tidval = new.tid);

   EXEC SQL commit;

}
/*
** Function to check database integrity and display results
** at completion of run.
*/
void checkit()
{
   EXEC SQL begin declare section;
      int  dbals, x1, x2, x3, x4, x5, x6;
      double bal;
      int accts, accts1, accts2;
      char    c1[2], t1[64], t2[64], t3[64], cmdline[257];
      char    c2[2], c3[2];
   EXEC SQL end declare section;

   EXEC SQL whenever sqlerror stop; 
 
   printf("\nChecking logical database consistency...\n");
/*
** All accounts must have a net total balance (acct1 + acct2)
** of 1000.00
*/

   EXEC SQL select accts, rsize, t1, t2, t2 - t1,
                   int4(interval('seconds', t2-t1)), trim(cmdline),
                   rsize, useidx, tstruct, uniquevals
            into :accts, :c1, :t1, :t2, :t3, :x4, :cmdline,
                 :c2, :x5, :c3, :x6  from zsumv1admin1;

   EXEC SQL select count(distinct a1.bal + a2.bal)
                   into :dbals from zsumv1acct1 a1, zsumv1acct2 a2
                   where a1.accid = a2.accid;

   EXEC SQL select distinct a1.bal + a2.bal into :bal
                   from zsumv1acct1 a1, zsumv1acct2 a2
                   where a1.accid = a2.accid and a1.accid = 0; 

   EXEC SQL select sum(rxacts) into :x1 from zsumv1acct1;
   EXEC SQL select sum(rxacts) into :x2 from zsumv1acct2;
   EXEC SQL select count(*) into :x3 from zsumv1transrec; 

   printf("Distinct balance count: %d\n", dbals);
   if (dbals != 1)
   {
      printf("\nError: All accounts must have same balance.\n");
      return;
   }
   printf("Account balance: $%5.2f\n", bal);
   if (bal != BALANCE)
   {
      printf("\nError: Incrrect balance. Expected value: $%5.2f\n", bal);
      return;
   } 

   printf("Total transactions: Acct1: %d, Acct2: %d\n", x1, x2);
   if (x1 != x2)
   {
      printf("\nError: transaction count mismatch.\n");
      return;
   } 

   if (x3 != 0)
   {
      printf("Logged transactions: %d\n", x3);
      if (x1 != x3)
      {
         printf("Error: Logged transaction count incorrect.\n");
         return;
      }  
   }
   printf("OK\n\n");

   printf("Args:   %s\n", cmdline);
   printf("DB:     %s\n", dbname);
   printf("Tables: indexes: %s, structure: %s, unique keys: %s\n", 
          x5 ? "yes" : "no", (*c3) == 'H' ? "hash" : "btree",
          x6 ? "yes" : "no");  
   printf("Data:   accounts: %d, row size: %c\n", accts, c2[0]);
   printf("Begin:  %s\nEnd:    %s\n", t1, t2); 
   printf("ET:     %s\n", t3); 
   printf("TPS:    %d\n",  x1 / x4);
    
}
