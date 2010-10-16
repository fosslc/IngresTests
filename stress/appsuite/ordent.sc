/*
**  Copyright (c) 2005, 2007 Ingres Corporation
**
**  Stress Test Application Suite
**
**  ordent.sc 
**
**  History:
**
**  01-May-2005 sarjo01: Created from ordent1
**  15-Jun-2005 sarjo01: Added enhanced error handling 
**  24-Aug-2005 sarjo01: Added lowercase() to sequence name queries to make 
**                       compatible with FIPS databases
**  05-Oct-2005 sarjo01: Added isolation level flag 
**  14-Oct-2005 sarjo01: Feature sync up
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

#define MAXCHILDTHREADS 99
#define MAXVERBOSE 2
#define MINVERBOSE 0

void doit(int *p);
void checkit();
void print_err();
void createobjs();
void cleanup();
void remodtables();

HANDLE  h[MAXCHILDTHREADS];

int delordcount[MAXCHILDTHREADS];
int deldetcount[MAXCHILDTHREADS];
int addordcount[MAXCHILDTHREADS];
int adddetcount[MAXCHILDTHREADS];
int xactscompleted[MAXCHILDTHREADS];
int deadlocks[MAXCHILDTHREADS];
int lockwaits[MAXCHILDTHREADS];
int fatalerrs[MAXCHILDTHREADS];

int nthreads = 8;
int iters = 1000;
int seqcache = 10000;
int lockwait = 0;
int parts = 1;
int verbose = MINVERBOSE; 
int secondaries = 0; 
int xperconn = 0; 
int remod = 0; 
int startkey = 1;
int rowcount = 25000;
int running;
int abortfatal = 0; 
char isolevel = 'S';

int cweight = 0;
int cweights[5][6] =
{
   { 19, 58, 5, 2, 8, 8 },
   { 18, 54, 9, 3, 8, 8 },
   { 17, 51, 9, 3,10,10 },
   { 16, 48,12, 4,10,10 },
   { 15, 45,13, 5,11,11 }
};

char  *syntax =
 "\n"
 "Syntax:     ordent <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize ordent database objects\n"
 "            run     - execute ordent program, display results\n"
 "            cleanup - delete all ordent database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -c    init    Set sequence cache value        0 to 50000      10000\n"
 "   -d    init    Enable secondary indexes for    none            disabled\n"
 "                 primary key constraints\n"
 "   -i    run     Set transaction count (per      1 to 1000000    1000\n"
 "                 thread)\n"
 "   -l    run     Set isolation level             R(epeatable),   S\n"
 "                                                 S(erializable)\n"
 "   -m    run     Remodify tables                 none            disabled\n"
 "   -n    init    Set item table row count        1 to 100000     25000\n"
 "   -p    init    Enable partitions               1 to 64         1\n"
 "   -r    init    Set customer table row count    1 to 250000     25000\n"
 "   -t    run     Set no. of client threads       1 to 99         8\n"
 "   -v    run     Set verbose output level        0 to 2          0\n"
 "                                                 0=no output\n"
 "   -w    run     Set lock wait seconds           0 to 10         0\n"
 "                                                 99=nowait\n"
 "   -x    run     Set no. of transactions per     1 to 1000,      0\n"
 "                 connection                      0=never disconn\n"
 "   -y    run     Abort thread on fatal error     none            disabled\n"
 "   -z    run     Set command weighting           0 to 4          0\n"
 "                 (higher value -> more deleting)\n"
 "\n";

EXEC SQL begin declare section;
     char   *dbname;
     char   starttime[257];
     char   endtime[257];
     char   etime[257];
     char   stmtbuff[257];
     int    highkey = 10000;
     int    initcust, initord, initdet;
     int    items = 25000;
     int    nextord, nextdet, cachevals;
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
               seqcache = irng(intparm, 0, 50000); 
               break;
            case 'D':
               secondaries = 1;
               break;
            case 'I':
               iters = irng(intparm, 1, 1000000);
               break;
            case 'L':
               isolevel = toupper(*(argv[i]+2));
               if (isolevel != 'S' &&
                   isolevel != 'R')
                  isolevel = 'S';
               break;
            case 'M':
               remod = 1;
               break;
            case 'N':
               items = irng(intparm, 1, 100000); 
               break;
            case 'P':
               parts = irng(intparm, 1, 64);
               break;
            case 'R':
               rowcount = irng(intparm, 1, 250000); 
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
               xperconn = irng(intparm, 0, 1000);
               break;
            case 'Y':
               abortfatal = 1;
               break;
            case 'Z':
               cweight = irng(intparm, 0, 4); 
               break;
         }
      } 
   }

   running = nthreads;

   EXEC SQL whenever sqlerror stop;
   EXEC SQL connect :dbname as 'mastercon';

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

   EXEC SQL select count(*), max(custid) into :initcust, :highkey from oe_cust;
   EXEC SQL select count(*) into :initord from oe_ord;
   EXEC SQL select count(*) into :initdet from oe_det;
   EXEC SQL select count(*) into :items from oe_item;
   EXEC SQL select * into :nextord, :nextdet, :cachevals from oe_admin;
#if 0
   sprintf(stmtbuff,
           "alter sequence oe_ordseq restart with %d",
           nextord);
   EXEC SQL execute immediate :stmtbuff;
   sprintf(stmtbuff,
           "alter sequence oe_detseq restart with %d",
           nextdet);
   EXEC SQL execute immediate :stmtbuff;
#else
   EXEC SQL drop sequence oe_ordseq;
   EXEC SQL drop sequence oe_detseq;
   if (cachevals == 0)
   {
      sprintf(stmtbuff, "create sequence oe_ordseq nocache start with %d",
              nextord);
      EXEC SQL execute immediate :stmtbuff;
      sprintf(stmtbuff, "create sequence oe_detseq nocache start with %d",
              nextdet);
      EXEC SQL execute immediate :stmtbuff;
   }
   else
   {
      sprintf(stmtbuff, "create sequence oe_ordseq cache %d start with %d",
              cachevals, nextord);
      EXEC SQL execute immediate :stmtbuff;
      sprintf(stmtbuff, "create sequence oe_detseq cache %d start with %d",
              cachevals, nextdet);
      EXEC SQL execute immediate :stmtbuff;
   }
#endif

   EXEC SQL commit;

   memset(delordcount, 0, sizeof(delordcount));
   memset(deldetcount, 0, sizeof(deldetcount));
   memset(addordcount, 0, sizeof(addordcount));
   memset(adddetcount, 0, sizeof(adddetcount));
   memset(xactscompleted, 0, sizeof(xactscompleted));
   memset(deadlocks, 0, sizeof(deadlocks)); 
   memset(lockwaits, 0, sizeof(lockwaits)); 
   memset(fatalerrs, 0, sizeof(fatalerrs));

   for (i = 1; i <= nthreads; i++)
      param[i-1] = i;

   sprintf(stmtbuff,
           "set random_seed %d",
           (initdet + initord) / nthreads);
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
** Child threads have terminated.
*/    
   EXEC SQL select date('now') into :endtime; 
   if (verbose < MAXVERBOSE)
      printf("%d THREADS DONE\n", nthreads);
   checkit();
   EXEC SQL update oe_admin from iisequences s set
            nextordval = s.next_value
            where lowercase(s.seq_name) = 'oe_ordseq';
   EXEC SQL update oe_admin from iisequences s set
            nextdetval = s.next_value
            where lowercase(s.seq_name) = 'oe_detseq';
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
int mapcmdcode(int cmdcode)
{
   int i, j;

   for (j=i=0; i<6; i++)
   { 
      j = j + cweights[cweight][i];
      if (cmdcode <= j)
         return i+1;
   }
}
/*
** Child thread entry point
*/
void doit(int *p)
{
   EXEC SQL begin declare section;
        int orddetcnt;
	int custid;
	int ordid;
	int itemquant, itemcost, itemid, icnt;
	int detid, detitemquant, detitemcost;
	int custordcnt;
	int cmdcode;
        int delorddets;
	int pval;
        int hk,lk;
	char connectName[45];
	char stmtbuff[257];
   EXEC SQL end declare section;

   int loopcnt, q, cmd, xs, xst, i, error_code, reconn, neverdisc;

   pval = *p;
   hk = highkey;
   icnt = items;
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
** Program starts every new transaction here
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
         EXEC SQL set session with on_error = rollback transaction;
         EXEC SQL set autocommit off; 

         EXEC SQL set lockmode on oe_cust
                      where level=row, readlock=exclusive; 

         if (lockwait == 99)
            EXEC SQL set lockmode session where level=row, timeout=nowait; 
         else
         {
            sprintf(stmtbuff,
                    "set lockmode session where level=row, timeout=%d",
                    lockwait);
            EXEC SQL execute immediate :stmtbuff;
         }

         if (isolevel == 'S')
            EXEC SQL set session isolation level serializable;
         else
            EXEC SQL set session isolation level repeatable read; 
      }
/*
** Get a customer ID and command 
*/
      EXEC SQL repeated select random(1, :hk), random(1, 100),
                        random(1, :icnt), random(1, 12)
               into :custid, :cmdcode, :itemid, :detitemquant; 
      cmd = mapcmdcode(cmdcode);

retry1:

      EXEC SQL repeated select custordcnt into :custordcnt
               from oe_cust where custid = :custid;

      if (sqlca.sqlcode == 100)
      {
         EXEC SQL commit;
         if (verbose == MAXVERBOSE)
            printf("T%02d:%07d C:%07d NO CUSTOMER\n", pval, i+1, custid);
      }
      else
      {
         if (cmd > 1 && cmd < 6 && custordcnt == 0)
         {
            EXEC SQL commit;
            if (++custid > hk)
               custid = 1;
            goto retry1;
         }
         else
         {
            switch (cmd)
            {
               case 1: /* Add a new order */

                  q=1;
                  EXEC SQL repeated select oe_ordseq.nextval into :ordid;
                  q=2;
                  EXEC SQL repeated insert into oe_ord values (
                           :ordid,
                           :custid,
                           date('now'),
                           date('now'),
                           'O',
                           0,
                           0,
                           'Filler Data' );  
                  q=3;
                  EXEC SQL repeated update oe_cust
                           set custordcnt = custordcnt + 1
                           where custid = :custid;
                  q=4;
                  EXEC SQL commit;
                  addordcount[pval-1]++;
                  if (verbose == MAXVERBOSE)
                     printf("T%02d:%07d C:%07d O:%08d\n",
                            pval, i+1, custid, ordid);
                  break;

               case 2: /* Add a new detail */

                  q=1;
                  EXEC SQL repeated select itemquant, itemcost
                           into :itemquant, :itemcost
                           from oe_item where itemid = :itemid;
                  if (itemquant == 0)
                  {
                     q=2;
                     EXEC SQL repeated update oe_item
                              set itemquant = 500,
                                  restockcnt = restockcnt + 1
                              where itemid = :itemid;
                     q=3;
                     EXEC SQL commit;
                     if (verbose == MAXVERBOSE)
                        printf("T%02d:%07d I:%07d RESTOCKED\n",
                                pval, i+1, itemid);
                     break;
                  }
                  else if (detitemquant > itemquant)
                     detitemquant = itemquant;

                  q=4;
                  EXEC SQL repeated update oe_item
                           set itemquant = itemquant - :detitemquant
                           where itemid = :itemid;
                  detitemcost = detitemquant * itemcost;
                  q=5;
                  EXEC SQL repeated select first 1 ordid into :ordid
                           from oe_ord where custid = :custid
                           and ordstat = 'O'
                           order by orddetcnt;
                  q=6;
                  EXEC SQL repeated select oe_detseq.nextval into :detid;
                  q=7;
                  EXEC SQL repeated insert into oe_det values (
                           :detid,
                           :ordid,
                           date('now'),
                           :itemid,
                           :detitemquant,
                           :detitemcost,
                           'Filler Data' );  
                  q=8;
                  EXEC SQL repeated update oe_ord
                           set orddetcnt = orddetcnt + 1,
                               ordcost = ordcost + :detitemcost 
                           where ordid = :ordid;
                  q=9;
                  EXEC SQL commit;
                  adddetcount[pval-1]++;
                  if (verbose == MAXVERBOSE)
                     printf("T%02d:%07d C:%07d O:%08d D:%08d\n",
                            pval, i+1, custid, ordid, detid);
                  break;

               case 3: /* Delete a detail */

                  q=1;
                  EXEC SQL repeated select first 1 ordid, orddetcnt
                           into :ordid, :orddetcnt
                           from oe_ord where custid = :custid
                           and ordstat = 'O'
                           order by 2 desc;
                  if (orddetcnt == 0)
                  {
                     q=2;
                     EXEC SQL commit;
                     if (verbose == MAXVERBOSE)
                        printf("T%02d:%07d C:%07d NO DETAILS\n",
                               pval, i+1, custid, ordid);
                  }
                  else
                  {
                     q=3;
                     EXEC SQL repeated select min(detid) into :detid
                              from oe_det where ordid = :ordid;
                     q=4;
                     EXEC SQL repeated select detcost into :detitemcost
                              from oe_det where detid = :detid;
                     q=5;
                     EXEC SQL repeated delete from oe_det where 
                              detid = :detid;
                     q=6;
                     EXEC SQL repeated update oe_ord
                              set orddetcnt = orddetcnt - 1,
                              ordcost = ordcost - :detitemcost
                              where ordid = :ordid;
                     q=7;
                     EXEC SQL commit;
                     deldetcount[pval-1]++;
                     if (verbose == MAXVERBOSE)
                        printf("T%02d:%07d C:%07d O:%08d D:%08d DELETED\n",
                               pval, i+1, custid, ordid, detid);
                  }
                  break;

               case 4: /* Delete an order */

                  q=1;
                  EXEC SQL repeated select min(ordid)
                           into :ordid
                           from oe_ord where custid = :custid
                           and ordstat = 'O';
                  q=2;
                  EXEC SQL repeated select orddetcnt
                           into :delorddets
                           from oe_ord where ordid = :ordid;
                  if (delorddets > 0)
                  {
                     q=3;
                     EXEC SQL repeated delete from oe_det where
                              ordid = :ordid; 
                     delorddets = sqlca.sqlerrd[2];
                  }
                  q=4;
                  EXEC SQL repeated delete from oe_ord where
                           ordid = :ordid; 
                  q=5;
                  EXEC SQL repeated update oe_cust
                           set custordcnt = custordcnt - 1
                           where custid = :custid; 
                  q=6;
                  EXEC SQL commit;
                  delordcount[pval-1]++;
                  deldetcount[pval-1] += delorddets;
                  if (verbose == MAXVERBOSE)
                     printf("T%02d:%07d C:%07d O:%08d DELETED\n",
                            pval, i+1, custid, ordid);
                  break;

               case 5: /* Ship an order */

                  q=1;
                  EXEC SQL repeated select first 1 ordid
                           into :ordid
                           from oe_ord where custid = :custid
                           and ordstat = 'O'
                           and orddetcnt > 0 order by 1;
                  if (sqlca.sqlcode == 100)
                  {
                     q=2;
                     EXEC SQL commit;
                        if (verbose == MAXVERBOSE)
                        printf("T%02d:%07d C:%07d NO ORDERS TO SHIP\n",
                                pval, i+1, custid);
                  }
                  else
                  {
                     q=3;
                     EXEC SQL repeated update oe_ord
                              set ordstat = 'S',
                                  shipdate = 'now' 
                              where ordid = :ordid;
                     q=4;
                     EXEC SQL repeated update oe_cust
                              set custordcnt = custordcnt - 1
                              where custid = :custid; 
                     q=5;
                     EXEC SQL commit;
                     if (verbose == MAXVERBOSE)
                        printf("T%02d:%07d C:%07d O:%08d SHIPPED\n",
                               pval, i+1, custid, ordid);
                  }
                  break;

               case 6: /* Archive an order */

                  q=1;
                  EXEC SQL repeated select first 1 ordid
                           into :ordid
                           from oe_ord where custid = :custid
                           and ordstat = 'S' order by shipdate;
                  if (sqlca.sqlcode == 100)
                  {
                     q=2;
                     EXEC SQL commit;
                     if (verbose == MAXVERBOSE)
                        printf("T%02d:%07d C:%07d NO ORDERS TO ARCHIVE\n",
                                pval, i+1, custid);
                  }
                  else
                  {
                     q=3;
                     EXEC SQL repeated insert into oe_ordarch
                              select * from oe_ord where ordid = :ordid;
                     q=4;
                     EXEC SQL repeated insert into oe_detarch
                              select * from oe_det where ordid = :ordid;
                     q=5;
                     EXEC SQL repeated delete from oe_det where ordid = :ordid;
                     delorddets = sqlca.sqlerrd[2];
                     q=6;
                     EXEC SQL repeated delete from oe_ord where ordid = :ordid;
                     q=7;
                     EXEC SQL commit;
                     delordcount[pval-1]++;
                     deldetcount[pval-1] += delorddets;
                     if (verbose == MAXVERBOSE)
                        printf("T%02d:%07d C:%07d O:%08d ARCHIVED\n",
                               pval, i+1, custid, ordid);
                  }
                  break;

            } /* end switch (cmd) */

         }
      }
      xactscompleted[pval-1]++;

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
         printf("T%02d: Fatal SQL error (%d,%d),custid:%d,ordid:%d,"
                "detid:%d\n",
                 pval, cmd, q, custid, ordid, detid);
         EXEC SQL whenever sqlerror continue; 
         EXEC SQL rollback;
         EXEC SQL disconnect :connectName;
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

   EXEC SQL drop sequence oe_ordseq;
   EXEC SQL drop sequence oe_detseq;
   EXEC SQL drop table oe_admin;
   EXEC SQL drop table oe_item;
   EXEC SQL drop table oe_det;
   EXEC SQL drop table oe_ord;
   EXEC SQL drop table oe_cust;
   EXEC SQL drop table oe_ordarch;
   EXEC SQL drop table oe_detarch;
   EXEC SQL commit; 

}
/*
** Function to create db objects
*/
void createobjs()
{
   EXEC SQL begin declare section;
      int i, keyval;
      char stmtbuff[257]; 
      int cachevals;
   EXEC SQL end declare section;

   EXEC SQL set autocommit on; 

   cleanup();

   EXEC SQL whenever sqlerror call sqlprint;

   printf("Creating objects...\n");

/*
** Sequences
*/
   cachevals = seqcache;
   if (seqcache == 0)
   {
      EXEC SQL create sequence oe_ordseq nocache;
      EXEC SQL create sequence oe_detseq nocache;
   }
   else
   {
      sprintf(stmtbuff, "create sequence oe_ordseq cache %d", seqcache);
      EXEC SQL execute immediate :stmtbuff;
      sprintf(stmtbuff, "create sequence oe_detseq cache %d", seqcache);
      EXEC SQL execute immediate :stmtbuff;
   }

/*
** CUSTOMER table
*/
   EXEC SQL create table oe_cust
            (custid int not null, custname char(36), custdate date,
             custordcnt int,  filler char(250))
             with page_size=4096;

   if (parts > 1)
   {
      sprintf(stmtbuff,
              "modify oe_cust to reconstruct with partition="
              "(hash on custid %d partitions)", parts);
      EXEC SQL execute immediate :stmtbuff;
   }

   for (i=1; i<=rowcount; i++)
   {
      EXEC SQL repeated insert into oe_cust values (
               :i,
               uuid_to_char(uuid_create()),
               date('now'),
               1,
               'Filler Data' );
   }
   EXEC SQL modify oe_cust to btree unique on custid
            with unique_scope = statement;

   if (secondaries)
   {
      EXEC SQL create unique index oe_custix1 on oe_cust (custid)
                      with page_size=4096, structure=btree, persistence,
                           unique_scope = statement;

      sprintf(stmtbuff,
              "alter table oe_cust add constraint oe_custpk "
              "primary key (custid) with index = oe_custix1");
      EXEC SQL execute immediate :stmtbuff;
   }
   else
   {
      sprintf(stmtbuff,
              "alter table oe_cust add constraint oe_custpk "
              "primary key (custid) with index = base table structure");
      EXEC SQL execute immediate :stmtbuff;
   }

/*
** ORDER table
*/
   EXEC SQL create table oe_ord
            (ordid int not null, custid int not null,
             orddate date, shipdate date,
             ordstat char(1), orddetcnt int, ordcost int,  filler char(250))
             with page_size=4096;

   EXEC SQL create table oe_ordarch as select * from oe_ord
            with page_size=4096;
   EXEC SQL modify oe_ordarch to btree on ordid;

   if (parts > 1)
   {
      sprintf(stmtbuff,
              "modify oe_ord to reconstruct with partition="
              "(hash on ordid %d partitions)", parts);
      EXEC SQL execute immediate :stmtbuff;
   }

   for (i=1; i<=rowcount; i++)
   {
      EXEC SQL repeated insert into oe_ord values (
               oe_ordseq.nextval, 
               :i, 
               date('now'),
               date('now'),
               'O',
               0,
               0,
               'Filler Data' );
   }

   EXEC SQL modify oe_ord to btree unique on ordid
            with unique_scope = statement;

   EXEC SQL create index oe_ordix1 on oe_ord (custid)
                   with page_size=4096, structure=btree, persistence;

   sprintf(stmtbuff,
           "alter table oe_ord add constraint oe_ordfk "
           "foreign key (custid) references oe_cust (custid) "
           "with index = oe_ordix1");
   EXEC SQL execute immediate :stmtbuff;

   if (secondaries)
   {
      EXEC SQL create unique index oe_ordix2 on oe_ord (ordid)
                      with page_size=4096, structure=btree, persistence,
                           unique_scope = statement;

      sprintf(stmtbuff,
              "alter table oe_ord add constraint oe_ordpk "
              "primary key (ordid) with index = oe_ordix2");
      EXEC SQL execute immediate :stmtbuff;
   }
   else
   {
      sprintf(stmtbuff,
              "alter table oe_ord add constraint oe_ordpk "
              "primary key (ordid) with index = base table structure");
      EXEC SQL execute immediate :stmtbuff;
   }

/*
** DETAIL table
*/
   EXEC SQL create table oe_det
            (detid int not null, ordid int not null,
             detdate date, itemid int, itemquant int,
             detcost int, filler char(250))
            with page_size=4096;

   EXEC SQL create table oe_detarch as select * from oe_det
            with page_size=4096;
   EXEC SQL modify oe_detarch to btree on detid;

   if (parts > 1)
   {
      sprintf(stmtbuff,
              "modify oe_det to reconstruct with partition="
              "(hash on detid %d partitions)", parts);
      EXEC SQL execute immediate :stmtbuff;
   }

   EXEC SQL modify oe_det to btree unique on detid;

   EXEC SQL create index oe_detix1 on oe_det (ordid)
                   with page_size=4096, structure=btree, persistence;

   sprintf(stmtbuff,
           "alter table oe_det add constraint oe_detfk "
           "foreign key (ordid) references oe_ord (ordid) "
           "with index = oe_detix1");
   EXEC SQL execute immediate :stmtbuff;

/*
** ITEM table
*/

   EXEC SQL create table oe_item
            (itemid int not null, itemquant int,
             itemcost int, restockcnt int, filler char(500))
            with page_size=4096;

   for (i=1; i<=items; i++)
   {
      EXEC SQL repeated insert into oe_item values (
               :i, 
               250,
               random(5, 25),
               0,
               'Filler Data' );
   }

   EXEC SQL modify oe_item to btree unique on itemid;

   if (parts > 1)
   {
      sprintf(stmtbuff,
              "modify oe_item to reconstruct with partition="
              "(hash on itemid %d partitions)", parts);
      EXEC SQL execute immediate :stmtbuff;
   }

/*
** ADMIN table
*/
   EXEC SQL create table oe_admin
            (nextordval int, nextdetval int, cachevals int)
             with page_size=4096;

   EXEC SQL insert into oe_admin values
            (oe_ordseq.nextval, oe_detseq.nextval, :cachevals);
}
void remodtables()
{
   printf("Remodifying tables...\n");
   EXEC SQL modify oe_ord to btree unique on ordid
            with unique_scope = statement;

   EXEC SQL modify oe_det to btree unique on detid;

}
/*
** Function to check database integrity and display results
** at completion of run.
*/
void checkit()
{
   EXEC SQL begin declare section;
      int totarchord, totarchdet;
      int totsecs; 
      int totordopen;
      int chkcust, chkord;
      int totord, totdetc; 
      int totordc, totdet; 
      int chkordarch;
      int totdetcost, totordcost, totfatal;
      int minord, maxord, mindet, maxdet;
      int dord, ddet; 
   EXEC SQL end declare section;

   int delordcount_t, deldetcount_t, addordcount_t, adddetcount_t;
   int totdeadlocks, totlockwaits;
   int i, expord, expdet, totxacts;

   printf("\nCompiling results...\n\n");

   EXEC SQL select
        int4(interval('seconds', date(:endtime) - date(:starttime)))
                       into :totsecs;
   if (totsecs == 0)
      totsecs = 1;

   EXEC SQL select date(:endtime) - date(:starttime) into :etime;

   delordcount_t = deldetcount_t = addordcount_t = adddetcount_t = 0;
   totdeadlocks = totlockwaits = totxacts = totfatal = 0;

   for (i = 0; i < nthreads; i++)
   {
      delordcount_t += delordcount[i];
      deldetcount_t += deldetcount[i];
      addordcount_t += addordcount[i];
      adddetcount_t += adddetcount[i];
      totxacts += xactscompleted[i];
      totdeadlocks += deadlocks[i];
      totlockwaits += lockwaits[i];
      totfatal += fatalerrs[i];
   }

   EXEC SQL whenever sqlerror continue;

   EXEC SQL select count(*) into :totarchord from oe_ordarch;
   EXEC SQL select count(*) into :totarchdet from oe_detarch;

   EXEC SQL select count(*), sum(orddetcnt), count(distinct ordid),
            sum(ordcost)
            into :totord, :totdetc, :dord,
                 :totordcost from oe_ord;

   EXEC SQL select count(*) into :totordopen
            from oe_ord where ordstat = 'O';

   EXEC SQL select sum(custordcnt) into :totordc from oe_cust;

   EXEC SQL select count(*), count(distinct detid),
            min(detid), max(detid), sum(detcost)
            into :totdet, :ddet, :mindet,
                 :maxdet, :totdetcost from oe_det;

   EXEC SQL select count(custid) into :chkcust from oe_cust
            where custordcnt <> (select count(ordid) from oe_ord
            where oe_cust.custid = oe_ord.custid and ordstat = 'O');

   EXEC SQL select count(ordid) into :chkord from oe_ord
            where orddetcnt <> (select count(detid) from oe_det
            where oe_ord.ordid = oe_det.ordid);

   EXEC SQL select count(ordid) into :chkordarch from oe_ordarch
            where orddetcnt <> (select count(detid) from oe_detarch
            where oe_ordarch.ordid = oe_detarch.ordid);


   expord = initord + addordcount_t - delordcount_t;
   expdet = initdet + adddetcount_t - deldetcount_t;

   printf(" Data Integrity Check\n");
   printf("---------------------------------------\n");
   printf(" Customer table rows        : % 8d\n", initcust);
   printf(" Item table rows            : % 8d\n\n", items);

   printf(" Initial order table rows   : % 8d\n", initord);
   printf(" Expected rows              : % 8d <\n", expord);
   printf(" Actual rows                : % 8d <\n", totord);
   printf(" Distinct order key values  : % 8d <\n\n", dord);

   printf(" Initial detail table rows  : % 8d\n", initdet);
   printf(" Expected rows              : % 8d <\n", expdet);
   printf(" Actual rows                : % 8d <\n", totdet);
   printf(" Distinct detail key values : % 8d <\n", ddet);
   printf(" Order details              : % 8d <\n\n", totdetc);

   printf(" Total archived orders      : % 8d\n", totarchord);
   printf(" Total archived details     : % 8d\n\n", totarchdet);

   printf(" Order count integrity      : %s\n",
           (chkcust == 0) ? "OK" : "ERROR");
   printf(" Open order integrity       : %s\n",
           (totordc == totordopen) ? "OK" : "ERROR");
   printf(" Detail count integrity     : %s\n",
           (chkord == 0)  ? "OK" : "ERROR");
   printf(" Detail cost vs. Order cost : %s\n",
           (totordcost == totdetcost) ? "OK" : "ERROR");
   printf(" Archived order integrity   : %s\n\n",
           (chkordarch == 0)  ? "OK" : "ERROR");

   printf(" Runtime Summary\n");
   printf("---------------------------------------------------\n");
   printf(" Start time                 : %s\n", starttime);
   printf(" End time                   : %s\n", endtime);
   printf(" Elapsed time               : %s\n", etime);
   printf(" Threads started            : %d\n", nthreads);
   printf(" Deadlocks                  : %d\n", totdeadlocks);
   printf(" Lockwait timeouts          : %d\n", totlockwaits);
   printf(" Fatal SQL errors           : %d\n", totfatal);
   printf(" Transactions completed     : %d\n", totxacts);
   printf(" TPS                        : %d\n", totxacts/totsecs);
}
