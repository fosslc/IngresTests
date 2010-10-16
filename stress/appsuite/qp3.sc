/*
**  Copyright (c) 2005, 2007 Ingres Corporation
**
**  Stress Test Application Suite
**
**  qp3.sc
**
**  History:
**
**  21-Jun-2005 sarjo01: Created from qp2
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
#define MAGICNUMBER 560489 

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
int xperconn = 0; 
int abortfatal = 0;
int running;

char  *syntax =
 "\n"
 "Syntax:     qp3 <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize qp3 database objects\n"
 "            run     - execute qp3 program, display results\n"
 "            cleanup - delete all qp3 database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -d   init     Set data file path             path             current dir\n"
 "   -i   run      Set transaction count (per     1 to 1000000     10\n"
 "                 thread)\n"
 "   -l   run      Set lockmode                   n(olock),s(ystem) n\n"
 "   -n   run      Add nodename to tracking list  nodename         none\n"
 "   -p   run      Enable set parallel            0,1              enabled\n"
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
        int magicnumber;
	char connectName[45];
        char nodename[65];
	char stmtbuff[257];
   EXEC SQL end declare section;

   int loopcnt, q, cmd, xs, xst, i,j,k, error_code, reconn, neverdisc;
   int rows, trows, rb;
   char qres[40];

   pval = *p;
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
      cmd=1;
      q=1;
      EXEC SQL select first 1 max(V1.versionobjid) into :magicnumber
               from harversions v1, haritems i1
         where (
            exists (
               select v2.versionobjid
               from harversions v2, harversioninview iv2, harview w
               where w.viewobjid = 301179
                     and iv2.versionobjid = v2.versionobjid
                     and w.baselineviewid = iv2.viewobjid
                     and v1.versionobjid = v2.versionobjid
            )
            or exists (
               select v3.versionobjid
               from harversions v3, harversioninview IV3, harbranch b3
               where iv3.versionobjid = v3.versionobjid
                     and v3.inbranch = b3.branchobjid
                     and iv3.viewobjid = 301179
                     and v3.inbranch = 0
                     and v1.versionobjid = v3.versionobjid
            )
         )
         and (
            i1.parentobjid = 300000
            or exists (
               select acp.childitemid
               from harallchildrenPath acp
               where acp.itemobjid = 300000
                     and i1.parentobjid = acp.childitemid
            )
         )
         and v1.itemobjid = i1.itemobjid
         and not exists (
            select ir.refitemid
            from harVersioninview iv5, haritemrelationship ir
            where iv5.viewobjid = 301179
                  and ir.relationship = 'rename'
                  and iv5.versionobjid = ir.versionobjid
                  and v1.itemobjid = ir.refitemid
          )
          group by v1.itemobjid order by 1;

      EXEC SQL commit;
      xactscompleted[pval-1]++;
      if (magicnumber == MAGICNUMBER)
         strcpy(qres, " ");
      else
         sprintf(qres, "WARNING: expected %d, got %d ",
                 MAGICNUMBER, magicnumber);

      if (verbose == MAXVERBOSE)
      {
         if (nodes)
            printf("T%02d:%06d %s %s\n", pval, i+1, nodename, qres);
         else
            printf("T%02d:%06d %s\n", pval, i+1, qres);
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

   EXEC SQL drop table harversions;             /* 1 */
   EXEC SQL drop table haritemrelationship;     /* 2 */
   EXEC SQL drop table haritems;                /* 3 */
   EXEC SQL drop table harversioninview;        /* 4 */
   EXEC SQL drop table harview;                 /* 5 */
   EXEC SQL drop table harbranch;               /* 6 */
   EXEC SQL drop table harallchildrenpath;      /* 7 */

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

   EXEC SQL create table harversions(
                versionobjid integer not null,
                itemobjid integer not null,
                packageobjid integer not null,
                parentversionid integer not null,
                mergedversionid integer not null,
                inbranch integer not null,
                mappedversion char(16),
                versionstatus char(1),
                creationtime date not null,
                creatorid integer not null,
                modifiedtime date not null,
                modifierid integer not null,
                description varchar(2000),
                versiondataobjid integer not null,
                clientmachine varchar(50),
                clientpath varchar(1024),
                ancestorversionid integer not null default 0)
        with page_size = 16384;

   sprintf(stmtbuff, "copy table harversions("
                "versionobjid= c0tab,"
                "itemobjid= c0tab,"
                "packageobjid= c0tab,"
                "parentversionid= c0tab,"
                "mergedversionid= c0tab,"
                "inbranch= c0tab,"
                "mappedversion= varchar(0)tab with null(']^NULL^['),"
                "versionstatus= varchar(0)tab with null(']^NULL^['),"
                "creationtime= c0tab,"
                "creatorid= c0tab,"
                "modifiedtime= c0tab,"
                "modifierid= c0tab,"
                "description= varchar(0)tab with null(']^NULL^['),"
                "versiondataobjid= c0tab,"
                "clientmachine= varchar(0)tab with null(']^NULL^['),"
                "clientpath= varchar(0)tab with null(']^NULL^['),"
                "ancestorversionid= c0nl,"
                "nl= d1)"
        "from '%sqp3tbl1.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;

   EXEC SQL modify harversions to btree unique on versionobjid;

   EXEC SQL create index harversions_foo on harversions (versionobjid)
            with structure = btree;

   EXEC SQL create unique index harversions_idx_001 on harversions
               (versionobjid, itemobjid, inbranch)
            with structure = btree, key = (versionobjid);

   EXEC SQL create index harversions_item_ind on harversions
               (itemobjid, versionobjid, creatorid, modifierid, inbranch,
                packageobjid)
            with structure = btree, key = (itemobjid);

   EXEC SQL create index harversions_merged_ind on harversions
               (mergedversionid)
            with structure = btree, key = (mergedversionid);

   EXEC SQL create index harversions_new2_ind on harversions
               (versionstatus, itemobjid, versionobjid, creatorid,
                modifierid, inbranch, packageobjid)
            with structure = btree,
                 key = (versionstatus, itemobjid, versionobjid,
                        creatorid, modifierid, inbranch, packageobjid);

   EXEC SQL create index harversions_par_ind on harversions
               (parentversionid)
            with structure = btree, key = (parentversionid);

   EXEC SQL create index harversions_pkg_ind on harversions
               (packageobjid)
            with structure = btree, key = (packageobjid);

   EXEC SQL create table haritems(
                itemobjid integer not null,
                itemname varchar(256) not null,
                itemtype integer not null,
                parentobjid integer not null,
                repositobjid integer not null,
                creationtime date not null,
                creatorid integer not null,
                modifiedtime date not null,
                modifierid integer not null)
        with page_size=4096; 

   sprintf(stmtbuff, "copy table haritems("
                "itemobjid= c0tab,"
                "itemname= varchar(0)tab,"
                "itemtype= c0tab,"
                "parentobjid= c0tab,"
                "repositobjid= c0tab,"
                "creationtime= c0tab,"
                "creatorid= c0tab,"
                "modifiedtime= c0tab,"
                "modifierid= c0nl,"
                "nl= d1)"
        "from '%sqp3tbl3.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;


   EXEC SQL modify haritems to btree unique on itemobjid;

   EXEC SQL create index haritems_26_july_2002 on haritems
               (itemtype, parentobjid, itemobjid)
            with structure = btree, key = (itemtype);

   EXEC SQL create index haritems_idx_001 on haritems
               (itemtype, itemobjid, parentobjid, itemname,
                repositobjid)
            with structure = btree,
                 key = (itemtype,itemobjid, parentobjid, itemname,
                        repositobjid);

   EXEC SQL create index haritems_idx_002 on haritems
               (parentobjid, itemobjid)
            with structure = btree, key = (parentobjid);

   EXEC SQL create index haritems_ind_type on haritems
               (itemtype, parentobjid)
            with structure = btree,
                 key = (itemtype, parentobjid);

   EXEC SQL create table haritemrelationship(
                itemobjid integer not null,
                refitemid integer not null,
                relationship char(10) not null,
                versionobjid integer not null)
        with page_size=4096;

   sprintf(stmtbuff, "copy table haritemrelationship("
                "itemobjid= c0tab,"
                "refitemid= c0tab,"
                "relationship= varchar(0)tab,"
                "versionobjid= c0nl,"
                "nl= d1)"
        "from '%sqp3tbl2.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;


   EXEC SQL modify haritemrelationship to btree unique on
                itemobjid, refitemid, versionobjid;

   EXEC SQL create index haritemrelationship_jps3 on haritemrelationship
              (relationship, refitemid, versionobjid)
            with structure = btree, key = (relationship);

   EXEC SQL create table harversioninview(
                viewobjid integer not null,
                versionobjid integer not null)
            with page_size=4096;

   sprintf(stmtbuff, "copy table harversioninview("
                "viewobjid= c0tab,"
                "versionobjid= c0nl,"
                "nl= d1)"
        "from '%sqp3tbl4.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;


   EXEC SQL modify harversioninview to btree unique on
                viewobjid, versionobjid;

   EXEC SQL create index jps_1 on harversioninview
               (versionobjid, viewobjid)
            with structure = btree, key = (versionobjid);

   EXEC SQL create table harview(
                viewobjid integer not null,
                viewname char(128) not null,
                viewtype char(16) not null,
                envobjid integer not null,
                canviewexternally char(1) not null,
                baselineviewid integer not null,
                creationtime date not null,
                creatorid integer not null,
                modifiedtime date not null,
                modifierid integer not null,
                snapshottime date,
                note varchar(2000))
        with page_size = 16384;

   sprintf(stmtbuff, "copy table harview("
                "viewobjid= c0tab,"
                "viewname= varchar(0)tab,"
                "viewtype= varchar(0)tab,"
                "envobjid= c0tab,"
                "canviewexternally= varchar(0)tab,"
                "baselineviewid= c0tab,"
                "creationtime= c0tab,"
                "creatorid= c0tab,"
                "modifiedtime= c0tab,"
                "modifierid= c0tab,"
                "snapshottime= c0tab with null(']^NULL^['),"
                "note= varchar(0)nl with null(']^NULL^['),"
                "nl= d1)"
        "from '%sqp3tbl5.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;


   EXEC SQL modify harview to btree unique on viewobjid;

   EXEC SQL create unique index harview_idx_002 on harview
               (viewobjid, baselineviewid)
            with structure = btree, key = (viewobjid, baselineviewid);

   EXEC SQL create unique index harview_name_ind on harview
               (viewname, envobjid)
            with structure = btree, key = (viewname, envobjid);

   EXEC SQL create index harview_viewobjid_jps2 on harview
               (envobjid, viewobjid, viewtype)
            with structure = btree, key = (envobjid);

   EXEC SQL create table harbranch(
                branchobjid integer not null,
                packageobjid integer not null,
                itemobjid integer not null,
                ismerged integer not null)
        with page_size=4096; 

   sprintf(stmtbuff, "copy table harbranch("
                "branchobjid= c0tab,"
                "packageobjid= c0tab,"
                "itemobjid= c0tab,"
                "ismerged= c0nl,"
                "nl= d1)"
        "from '%sqp3tbl6.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;


   EXEC SQL modify harbranch to btree unique on branchobjid;

   EXEC SQL create index harbranch_item_ind on harbranch (itemobjid)
            with structure = btree, key = (itemobjid);

   EXEC SQL create index harbranch_itemid_ind on harbranch (itemobjid)
            with structure = btree, key = (itemobjid);

   EXEC SQL create table harallchildrenpath(
                itemobjid integer not null,
                childitemid integer not null
                )
        with page_size=4096; 

   sprintf(stmtbuff, "copy table harallchildrenpath("
                "itemobjid= c0tab,"
                "childitemid= c0nl,"
                "nl= d1)"
        "from '%sqp3tbl7.data'", datapath);
   EXEC SQL execute immediate :stmtbuff;

   EXEC SQL modify harallchildrenpath to btree unique on
                itemobjid, childitemid;

   EXEC SQL create index harallchildrenpath_cld_idx on harallchildrenpath
               (childitemid, itemobjid)
            with structure = btree, key = (childitemid);

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
