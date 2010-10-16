/*
**  Copyright (c) 2005, 2007 Ingres Corporation
**
**  Stress Test Application Suite
**
**  qp1.sc
**
**  History:
**
**  16-May-2003 sarjo01: Created
**  21-May-2004 sarjo01: Clean-up
**  24-Jan-2005 sarjo01: Added parallelism option
**  10-Mar-2005 sarjo01: Changed page size to 4096
**  18-Mar-2005 sarjo01: Change MAXTHREADS to MAXCHILDTHREADS
**  14-Jun-2005 sarjo01: Added variable result set mode, new error handler
**  30-Jan-2006	(boija02) Updated copyright info for Ingres Corp.
**  31-May-2007 (Ralph Loen) Bug 118428
**     Ported to VMS.
**  01-Aug-2007 sarjo01: Redo -p (set parallel) to allow running against 2.6 
**  03-Jan-2008 sarjo01: Added -d to specify data file path 
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
#define MAXVERBOSE 2
#define MINVERBOSE 0
#define RESULTROWS 15

void doit(int *p);
void checkit();
void print_err();
void createobjs();
void cleanup();

HANDLE  h[MAXCHILDTHREADS];

char datapath[256] = "";

int xactscompleted[MAXCHILDTHREADS];
int deadlocks[MAXCHILDTHREADS];
int lockwaits[MAXCHILDTHREADS];
int fatalerrs[MAXCHILDTHREADS];

int nthreads = 4;
int iters = 10;
int lockwait = 0;
int lockmode = 'N';
int nodes = 0;
char *cnodes[16];
int nodestats[16];
int verbose = MINVERBOSE; 
int parallel = -1;
int varreset = 0;
int xperconn = 0; 
int abortfatal = 0;
int running;

char  *syntax =
 "\n"
 "Syntax:     qp1 <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize qp1 database objects\n"
 "            run     - execute qp1 program, display results\n"
 "            cleanup - delete all qp1 database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -d   init     Set data file path             path             current dir\n"
 "   -i   run      Set transaction count (per     1 to 1000000     10\n"
 "                 thread)\n"
 "   -l   run      Set lockmode                   N(olock),S(ystem) N\n"
 "   -n   run      Add nodename to tracking list  nodename         none\n"
 "   -p   run      Enable set parallel            0,1              enabled\n"
 "   -r   run      Enable variable result set     none             disabled\n"
 "   -t   run      Set no. of client threads      1 to 100         4\n"
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
   char *dp;
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
               dp = argv[i]+2; 
               if (!isspace(*dp))
                  strcpy(datapath, dp);
               break;
            case 'I':
               iters = irng(intparm, 1, 1000000);
               break;
            case 'L':
               lockmode = toupper(*(argv[i]+2));
               if (lockmode != 'N' && lockmode != 'S')
                  lockmode = 'N';
               break;
            case 'N':
               cnodes[nodes] = argv[i]+2;
               nodes++;
               break;
            case 'P':
               parallel = irng(intparm, 0, 1);
               break;
            case 'R':
               varreset = 1;
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

   memset(nodestats, 0, sizeof(nodestats));
   memset(xactscompleted, 0, sizeof(xactscompleted));
   memset(deadlocks, 0, sizeof(deadlocks)); 
   memset(lockwaits, 0, sizeof(lockwaits)); 
   memset(fatalerrs, 0, sizeof(fatalerrs)); 


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
	int keyval, pval, dval, cmdval;
        int hk;
	char connectName[45];
        char nodename[65];
	char stmtbuff[257];
   EXEC SQL end declare section;

   int loopcnt, q, cmd, xs, xst, i,j,k, error_code, reconn, neverdisc;
   int rows, trows, rb;

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
         if (parallel == 1)
            EXEC SQL set parallel;
         else if (parallel == 0)
            EXEC SQL set noparallel;
         EXEC SQL set joinop newenum; 
         EXEC SQL whenever sqlerror goto SQLerrorhandler; 
         EXEC SQL set autocommit off; 
         EXEC SQL set session with on_error = rollback transaction;

         if (lockmode == 'N')
            EXEC SQL set lockmode session where readlock=nolock;
         if (lockwait == 99)
            EXEC SQL set lockmode session where timeout=nowait; 
         else
         {
            sprintf(stmtbuff,
                    "set lockmode session where timeout=%d",
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
retry1:
      if (varreset == 0)
      {   
         cmd=1;
         q=1;
         rows=0;
         EXEC SQL select distinct e.e_idf into :dval
                  from qp1tbl1 e, qp1tbl3 l, qp1tbl6 ca, qp1tbl8 ra, qp1tbl10 uu
                  where e.e_ut = 1
                  and e.e_renvoi = 0
                  and e.e_idf = l.e_idf
                  and (l.e_cat = 'FAM' OR l.e_cat = 'BIO')
                  and e.e_idf = ca.e_idf
                  and uu.t_idf = ca.r_idf
                  and ca.r_idf = ra.r_idf
                  and ra.r_code = 'a'
                  and ca.pe_type = 'c'
                  and ca.col_c_idf = 447
                  order by 1;
         EXEC SQL begin;
            rows++;
         EXEC SQL end;
         q=2;
         EXEC SQL commit;
         xactscompleted[pval-1]++;

         if (rows != RESULTROWS)
            printf("ERROR: T%02d:%06d Expected %d rows, got %d\n",
                   pval, i+1, RESULTROWS, rows);
         else
         {
            if (verbose == MAXVERBOSE)
            {
               if (nodes)
                  printf("T%02d:%06d %s r:%d\n", pval, i+1, nodename, rows);
               else
                  printf("T%02d:%06d r:%d\n", pval, i+1, rows);
            }
         }
      }
      else
      {
         cmd=2;
         q=1;
         EXEC SQL repeated select random(0, 3874) into :keyval;
         EXEC SQL repeated select count(distinct e.e_idf) into :dval
              from qp1tbl1 e, qp1tbl3 l, qp1tbl6 ca, qp1tbl8 ra, qp1tbl10 uu
                  where e.e_ut = 1
                  and e.e_renvoi = 0
                  and e.e_idf = l.e_idf
                  and (l.e_cat = 'FAM' OR l.e_cat = 'BIO')
                  and e.e_idf = ca.e_idf
                  and uu.t_idf = ca.r_idf
                  and ca.r_idf = ra.r_idf
                  and ra.r_code = 'a'
                  and ca.pe_type = 'c'
                  and ca.col_c_idf = :keyval;
         q=2;
         EXEC SQL commit;
         xactscompleted[pval-1]++;
         if (verbose == MAXVERBOSE)
         {
            if (nodes)
               printf("T%02d:%06d %s k:%04d r:%d\n", pval, i+1, nodename,
                      keyval, dval);
            else
               printf("T%02d:%06d k:%04d r:%d\n", pval, i+1, keyval, dval);
         }
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
            printf("T%02d: DEADLOCK (%d,%d)\n", pval, cmd, q);
         deadlocks[pval-1]++; 
         goto retry1;
      }
      else if (error_code == -39100)
      {
         EXEC SQL commit;
         if (verbose > MINVERBOSE)
            printf("T%02d: LOCKWAIT TIMEOUT (%d,%d)\n", pval, cmd, q);
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
   EXEC SQL whenever sqlerror continue;
   EXEC SQL set autocommit on;

   printf("Cleaning up...\n");

   EXEC SQL drop table qp1tbl1;
   EXEC SQL drop table qp1tbl3;
   EXEC SQL drop table qp1tbl6;
   EXEC SQL drop table qp1tbl8;
   EXEC SQL drop table qp1tbl10;

}
/*
** Function to create db objects
*/
void createobjs()
{
   EXEC SQL begin declare section;
      char stmtbuff[2048];
   EXEC SQL end declare section;

   EXEC SQL set autocommit on;

   cleanup();

   EXEC SQL whenever sqlerror call sqlprint;

   printf("Creating tables...\n");

   EXEC SQL create table qp1tbl1(
              e_idf integer not null not default,
              ent_e_idf integer,
              e_lemf varchar(80) not null default ' ',
              e_lema varchar(80) not null default ' ',
              e_lemi varchar(80) not null default ' ',
              e_lemfup varchar(80) not null default ' ',
              e_lemaup varchar(80) not null default ' ',
              e_lemiup varchar(80) not null default ' ',
              e_cof varchar(40) not null default ' ',
              e_coa varchar(40) not null default ' ',
              e_coi varchar(40) not null default ' ',
              e_cofup varchar(40) not null default ' ',
              e_coaup varchar(40) not null default ' ',
              e_coiup varchar(40) not null default ' ',
              e_exf varchar(40) not null default ' ',
              e_exa varchar(40) not null default ' ',
              e_exi varchar(40) not null default ' ',
              e_exfup varchar(40) not null default ' ',
              e_exaup varchar(40) not null default ' ',
              e_exiup varchar(40) not null default ' ',
              e_renvoi integer not null default 0,
              e_tria varchar(100) not null default ' ',
              e_trif varchar(100) not null default ' ',
              e_trii varchar(100) not null default ' ',
              fa_nord varchar(10) not null default ' ',
              e_sgml varchar(10) not null default ' ',
              e_mult smallint not null default 0,
              e_ut smallint not null default 0,
              e_pfambio smallint not null default 0)
              with page_size=4096;
   EXEC SQL create table qp1tbl3(
              e_idf integer not null not default,
              e_lg char(1) not null not default,
              e_nbprev integer not null not default,
              e_cat text(3) not null not default,
              e_volf integer,
              e_vola integer,
              e_voli integer,
              e_lir i1 not null not default,
              e_seqf integer,
              e_seqa integer,
              e_seqi integer,
              e_datcr date not null not default,
              e_datco date not null not default,
              e_datsu date not null not default,
              e_doc i1 not null not default,
              e_stat char(1) not null not default,
              e_comme text(70) not null not default,
              e_commi text(140) not null not default,
              e_prec text(30) not null not default,
              e_dyna text(5) not null not default,
              e_dynf text(5) not null not default,
              e_dyni text(5) not null not default,
              e_expa text(5) not null not default,
              e_expf text(5) not null not default,
              e_expi text(5) not null not default,
              e_vlemf text(30) not null not default,
              e_vlemi text(30) not null not default,
              e_vlema text(30) not null not default,
              e_vcof text(30) not null not default,
              e_vcoi text(30) not null not default,
              e_vcoa text(30) not null not default,
              e_pima i1 not null not default,
              e_datdi date not null not default,
              e_datvi date not null not default,
              e_nbeffa integer not null not default,
              e_nbefff integer not null not default,
              e_nbeffi integer not null not default,
              e_datpub date not null not default,
              e_datmj date not null not default,
              e_sexe i1 not null not default,
              e_icoco integer not null not default)
              with page_size=4096;
   EXEC SQL create table qp1tbl6(
              c_idf integer not null default 0,
              e_idf integer not null default 0,
              r_idf integer not null default 0,
              col_c_idf integer not null default 0,
              pe_date date not null default ' ',
              pe_comment text(255) not null default ' ',
              pe_type char(1) not null default ' ')
              with page_size=4096;
   EXEC SQL create table qp1tbl8(
              r_idf integer not null default 0,
              r_desf text(40) not null default ' ',
              r_desa text(40) not null default ' ',
              r_desi text(40) not null default ' ',
              r_code char(1) not null default ' ')
              with page_size=4096;
   EXEC SQL create table qp1tbl10(
              t_idf integer not null default 0,
              e_idf integer not null default 0,
              t_lg char(1) not null default ' ',
              t_comme text(100) not null default ' ',
              t_commi text(100) not null default ' ',
              t_datco date not null default ' ',
              t_datcr date not null default ' ',
              t_datsu date not null default ' ',
              t_desa text(80) not null default ' ',
              t_desf text(80) not null default ' ',
              t_desi text(80) not null default ' ',
              t_desaup text(80) not null default ' ',
              t_desfup text(80) not null default ' ',
              t_desiup text(80) not null default ' ',
              t_datmj date not null default ' ',
              t_nbprev integer,
              t_nbeffa integer,
              t_nbefff integer,
              t_nbeffi integer,
              t_seq integer not null default 0,
              t_stat char(1) not null default ' ')
              with page_size=4096;

   printf("Modifying tables...\n");
   EXEC SQL modify qp1tbl1 to isam unique on
                   e_idf with fillfactor = 50,
                   extend = 16,
                   allocation = 4;
   EXEC SQL modify qp1tbl3 to hash unique on
                   e_idf with fillfactor = 50,
                   extend = 16,
                   allocation = 4;
   EXEC SQL modify qp1tbl6 to isam on
                   e_idf,
                   c_idf,
                   r_idf with fillfactor = 80,
                   extend = 16,
                   allocation = 4;
   EXEC SQL modify qp1tbl8 to hash unique on
                   r_idf with fillfactor = 50,
                   extend = 16,
                   allocation = 4;
   EXEC SQL modify qp1tbl10 to hash unique on
                   t_idf with fillfactor = 50,
                   extend = 16,
                   allocation = 4;

   printf("Loading tables...\n");
   sprintf(stmtbuff, "copy table qp1tbl1("
                   "e_idf= c0tab,"
                   "ent_e_idf= c0tab with null(']^NULL^['),"
                   "e_lemf= varchar (0) tab,"
                   "e_lema= varchar (0) tab,"
                   "e_lemi= varchar (0) tab,"
                   "e_lemfup= varchar (0) tab,"
                   "e_lemaup= varchar (0) tab,"
                   "e_lemiup= varchar (0) tab,"
                   "e_cof= varchar (0) tab,"
                   "e_coa= varchar (0) tab,"
                   "e_coi= varchar (0) tab,"
                   "e_cofup= varchar (0) tab,"
                   "e_coaup= varchar (0) tab,"
                   "e_coiup= varchar (0) tab,"
                   "e_exf= varchar (0) tab,"
                   "e_exa= varchar (0) tab,"
                   "e_exi= varchar (0) tab,"
                   "e_exfup= varchar (0) tab,"
                   "e_exaup= varchar (0) tab,"
                   "e_exiup= varchar (0) tab,"
                   "e_renvoi= c0tab,"
                   "e_tria= varchar (0) tab,"
                   "e_trif= varchar (0) tab,"
                   "e_trii= varchar (0) tab,"
                   "fa_nord= varchar (0) tab,"
                   "e_sgml= varchar (0) tab,"
                   "e_mult= c0tab,"
                   "e_ut= c0tab,"
                   "e_pfambio= c0nl,"
                   "nl= d1)"
             "from '%sqp1tbl1.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;

   sprintf(stmtbuff, "copy table qp1tbl3("
                   "e_idf= c0tab,"
                   "e_lg= varchar (0) tab,"
                   "e_nbprev= c0tab,"
                   "e_cat= varchar (0) tab,"
                   "e_volf= c0tab with null(']^NULL^['),"
                   "e_vola= c0tab with null(']^NULL^['),"
                   "e_voli= c0tab with null(']^NULL^['),"
                   "e_lir= c0tab,"
                   "e_seqf= c0tab with null(']^NULL^['),"
                   "e_seqa= c0tab with null(']^NULL^['),"
                   "e_seqi= c0tab with null(']^NULL^['),"
                   "e_datcr= c0tab,"
                   "e_datco= c0tab,"
                   "e_datsu= c0tab,"
                   "e_doc= c0tab,"
                   "e_stat= varchar (0) tab,"
                   "e_comme= varchar (0) tab,"
                   "e_commi= varchar (0) tab,"
                   "e_prec= varchar (0) tab,"
                   "e_dyna= varchar (0) tab,"
                   "e_dynf= varchar (0) tab,"
                   "e_dyni= varchar (0) tab,"
                   "e_expa= varchar (0) tab,"
                   "e_expf= varchar (0) tab,"
                   "e_expi= varchar (0) tab,"
                   "e_vlemf= varchar (0) tab,"
                   "e_vlemi= varchar (0) tab,"
                   "e_vlema= varchar (0) tab,"
                   "e_vcof= varchar (0) tab,"
                   "e_vcoi= varchar (0) tab,"
                   "e_vcoa= varchar (0) tab,"
                   "e_pima= c0tab,"
                   "e_datdi= c0tab,"
                   "e_datvi= c0tab,"
                   "e_nbeffa= c0tab,"
                   "e_nbefff= c0tab,"
                   "e_nbeffi= c0tab,"
                   "e_datpub= c0tab,"
                   "e_datmj= c0tab,"
                   "e_sexe= c0tab,"
                   "e_icoco= c0nl,"
                   "nl= d1)"
             "from '%sqp1tbl3.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;

   sprintf(stmtbuff, "copy table qp1tbl6("
                   "c_idf= c0tab,"
                   "e_idf= c0tab,"
                   "r_idf= c0tab,"
                   "col_c_idf= c0tab,"
                   "pe_date= c0tab,"
                   "pe_comment= varchar(0)tab,"
                   "pe_type= varchar(0)nl,"
                   "nl= d1)"
             "from '%sqp1tbl6.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;

   sprintf(stmtbuff, "copy table qp1tbl8("
                   "r_idf= c0tab,"
                   "r_desf= varchar(0)tab,"
                   "r_desa= varchar(0)tab,"
                   "r_desi= varchar(0)tab,"
                   "r_code= varchar(0)nl,"
                   "nl= d1)"
             "from '%sqp1tbl8.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;

   sprintf(stmtbuff, "copy table qp1tbl10("
                   "t_idf= c0tab,"
                   "e_idf= c0tab,"
                   "t_lg= varchar(0)tab,"
                   "t_comme= varchar(0)tab,"
                   "t_commi= varchar(0)tab,"
                   "t_datco= c0tab,"
                   "t_datcr= c0tab,"
                   "t_datsu= c0tab,"
                   "t_desa= varchar(0)tab,"
                   "t_desf= varchar(0)tab,"
                   "t_desi= varchar(0)tab,"
                   "t_desaup= varchar(0)tab,"
                   "t_desfup= varchar(0)tab,"
                   "t_desiup= varchar(0)tab,"
                   "t_datmj= c0tab,"
                   "t_nbprev= c0tab with null(']^NULL^['),"
                   "t_nbeffa= c0tab with null(']^NULL^['),"
                   "t_nbefff= c0tab with null(']^NULL^['),"
                   "t_nbeffi= c0tab with null(']^NULL^['),"
                   "t_seq= c0tab,"
                   "t_stat= varchar(0)nl,"
                   "nl= d1)"
             "from '%sqp1tbl10.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;

   printf("Creating indexes...\n");
   EXEC SQL create index qp1tbl1_idx1 on qp1tbl1(
              e_lemaup,
              e_exaup,
              e_coaup)
            with structure = isam,
              nocompression,
              key = (e_lemaup, e_exaup, e_coaup),
              fillfactor = 80,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl1_idx2 on qp1tbl1(
              e_lemfup,
              e_exfup,
              e_cofup)
            with structure = isam,
              nocompression,
              key = (e_lemfup, e_exfup, e_cofup),
              fillfactor = 80,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl1_idx3 on qp1tbl1(
              e_lemiup,
              e_exiup,
              e_coiup)
            with structure = isam,
              nocompression,
              key = (e_lemiup, e_exiup, e_coiup),
              fillfactor = 80,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl1_idx4 on qp1tbl1(
              e_sgml)
            with structure = hash,
              nocompression,
              key = (e_sgml),
              minpages = 2,
              fillfactor = 50,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl1_idx5 on qp1tbl1(
              e_tria)
            with structure = isam,
              nocompression,
              key = (e_tria),
              fillfactor = 80,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl1_idx6 on qp1tbl1(
              e_trif)
            with structure = isam,
              nocompression,
              key = (e_trif),
              fillfactor = 80,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl1_idx7 on qp1tbl1(
              e_trii)
            with structure = isam,
              nocompression,
              key = (e_trii),
              fillfactor = 80,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl3_idx1 on qp1tbl3(
              e_cat,
              e_idf)
            with structure = isam,
              nocompression,
              key = (e_cat, e_idf),
              fillfactor = 80,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl6_idx1 on qp1tbl6(
              col_c_idf,
              e_idf,
              r_idf)
            with structure = isam,
              nocompression,
              key = (col_c_idf, e_idf, r_idf),
              fillfactor = 80,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl6_idx2 on qp1tbl6(
              c_idf,
              e_idf)
            with structure = isam,
              nocompression,
              key = (c_idf, e_idf),
              fillfactor = 80,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl6_idx3 on qp1tbl6(
              pe_type,
              r_idf)
            with structure = isam,
              nocompression,
              key = (pe_type, r_idf),
              fillfactor = 80,
              location = (ii_database),
              page_size=4096;
   EXEC SQL create index qp1tbl10_idx1 on qp1tbl10(
              e_idf)
            with structure = hash,
              nocompression,
              key = (e_idf),
              minpages = 2,
              fillfactor = 50,
              location = (ii_database),
              page_size=4096;

}
/*
** Function to check database integrity and display results
** at completion of run.
*/
void checkit()
{
   EXEC SQL begin declare section;
      int totsecs;
   EXEC SQL end declare section;

   int i, totxacts, totdeadlocks, totlockwaits, totfatal;
   totxacts=totdeadlocks=totlockwaits=totfatal=totsecs=0;

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
   printf(" TPS                      : %03.2f\n",
           (double)totxacts/(double)totsecs);

}
