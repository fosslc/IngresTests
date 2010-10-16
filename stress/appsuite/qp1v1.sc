/*
**  Copyright (c) 2005, 2007 Ingres Corporation
**
**  Stress Test Application Suite
**
**  qp1v1.sc
**
**  History:
**
**  23-Apr-2004 sarjo01: Created
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
#define RESULTROWS 15
#define MAGICNUMBER1 435825
#define MAGICNUMBER2 (MAGICNUMBER1+(RESULTROWS*pval)) 

void doit(int *p);
void checkit();
int print_err();
void createobjs();

HANDLE  h[MAXCHILDTHREADS];

int nthreads = 2;
int iters = 2;
int goahead = 0;
int verbose = 0; 
int xperconn = 1; 
int running;

char  *syntax =
 "\n" \
 "Syntax:     qp1v1 <database> <function> [ <option> <option> ... ]\n\n" \
 "<database>  target database name\n" \
 "<function>  init   - initialize database\n" \
 "            run    - execute qp1v1 program, display results\n" \
 "<option>    program option of the form -x[value]\n\n" \
 " Option Function Description                    Param Values     Default\n" \
 " ------ -------- ------------------------------ ---------------- -------\n" \
 "   -i    run     Set transaction count (per      1 to 100000     2\n" \
 "                 thread)\n" \
 "   -t    run     Set no. of client threads       1 to 100        2\n" \
 "   -v    run     Enable verbose output mode      none            disabled\n" \
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
            case 'I':
               iters = atoi(argv[i]+2); 
               if (iters < 1 || iters > 100000)
                  iters = 2;
               break;
            case 'T':
               nthreads = atoi(argv[i]+2); 
               if (nthreads < 1 || nthreads > MAXCHILDTHREADS)
                  nthreads = 2;
               break;
            case 'V':
               verbose = 1;
               break;
         }
      } 
   }
   running = nthreads;

   exec sql whenever sqlerror stop;
   exec sql connect :dbname as 'mastercon';
/*
** Only allowable functions are 'init' and 'run'
*/
   if (stricmp(argv[2], "init") == 0)
   {
      createobjs();
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
** Record starting time
*/ 
   exec sql select date('now') into :starttime; 
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
      pthread_create(&lpThreadId[i], NULL, doit, &param[i]);
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
** Parent thread records ending time.
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
/*
** Child thread entry point
*/
void doit(int *p)
{
   exec sql begin declare section;
	int pval;
	int dval1, dval2;
        double x;
	char connectName[45];
	short loop;
        int accts, acctid;
   exec sql end declare section;

   int rows, xs, i, error_code, reconn, neverdisc;
   char   rowbuff[256], vbuff[8];

   pval = *p;

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
retry1:
      if (reconn == 1)
      {
         EXEC SQL whenever sqlerror goto discon2; 
         EXEC SQL connect :dbname as :connectName; 
         EXEC SQL set joinop newenum;
         EXEC SQL set autocommit off; 
         EXEC SQL whenever sqlerror goto doretry; 

         EXEC SQL set lockmode session where  
                   readlock=nolock;
      }
/*
** Transaction's queries begin here
**
** Perform SELECT statement 
*/
      EXEC SQL declare global temporary table session.qp1v1 (tcol1)
               as select distinct e.e_idf
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
               on commit preserve rows with norecovery;
      EXEC SQL repeated select sum(tcol1) into :dval1 from session.qp1v1;
      EXEC SQL repeated update session.qp1v1 set tcol1 = tcol1 + :pval;
      EXEC SQL repeated select sum(tcol1) into :dval2 from session.qp1v1;
      EXEC SQL drop table session.qp1v1;
/*
** Issue COMMIT
*/
      EXEC SQL commit; 

      if (verbose)
         printf("T%02d, i=%d\n", pval, i+1);

      if (dval1 != MAGICNUMBER1 || dval2 != MAGICNUMBER2)
         printf("ERROR: T%02d: Expected: %d, %d, got %d, %d\n",
                 MAGICNUMBER1, MAGICNUMBER2, dval1, dval2);

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
/*
** Function to create db objects
*/
void createobjs()
{
   EXEC SQL begin declare section;
   EXEC SQL end declare section;

   EXEC SQL WHENEVER SQLERROR CONTINUE; 
   EXEC SQL set autocommit on; 

   printf("Cleaning up...\n");
   EXEC SQL drop table qp1tbl1;
   EXEC SQL drop table qp1tbl3;
   EXEC SQL drop table qp1tbl6;
   EXEC SQL drop table qp1tbl8;
   EXEC SQL drop table qp1tbl10;

   EXEC SQL WHENEVER SQLERROR call sqlprint;

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
              e_pfambio smallint not null default 0);
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
              e_icoco integer not null not default);
   EXEC SQL create table qp1tbl6(
              c_idf integer not null default 0,
              e_idf integer not null default 0,
              r_idf integer not null default 0,
              col_c_idf integer not null default 0,
              pe_date date not null default ' ',
              pe_comment text(255) not null default ' ',
              pe_type char(1) not null default ' ');
   EXEC SQL create table qp1tbl8(
              r_idf integer not null default 0,
              r_desf text(40) not null default ' ',
              r_desa text(40) not null default ' ',
              r_desi text(40) not null default ' ',
              r_code char(1) not null default ' ');
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
              t_stat char(1) not null default ' ');
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
   EXEC SQL copy table qp1tbl1(
                   e_idf= c0tab,
                   ent_e_idf= c0tab with null(']^NULL^['),
                   e_lemf= varchar (0) tab,
                   e_lema= varchar (0) tab,
                   e_lemi= varchar (0) tab,
                   e_lemfup= varchar (0) tab,
                   e_lemaup= varchar (0) tab,
                   e_lemiup= varchar (0) tab,
                   e_cof= varchar (0) tab,
                   e_coa= varchar (0) tab,
                   e_coi= varchar (0) tab,
                   e_cofup= varchar (0) tab,
                   e_coaup= varchar (0) tab,
                   e_coiup= varchar (0) tab,
                   e_exf= varchar (0) tab,
                   e_exa= varchar (0) tab,
                   e_exi= varchar (0) tab,
                   e_exfup= varchar (0) tab,
                   e_exaup= varchar (0) tab,
                   e_exiup= varchar (0) tab,
                   e_renvoi= c0tab,
                   e_tria= varchar (0) tab,
                   e_trif= varchar (0) tab,
                   e_trii= varchar (0) tab,
                   fa_nord= varchar (0) tab,
                   e_sgml= varchar (0) tab,
                   e_mult= c0tab,
                   e_ut= c0tab,
                   e_pfambio= c0nl,
                   nl= d1)
             from 'qp1tbl1.data';
   EXEC SQL copy table qp1tbl3(
                   e_idf= c0tab,
                   e_lg= varchar (0) tab,
                   e_nbprev= c0tab,
                   e_cat= varchar (0) tab,
                   e_volf= c0tab with null(']^NULL^['),
                   e_vola= c0tab with null(']^NULL^['),
                   e_voli= c0tab with null(']^NULL^['),
                   e_lir= c0tab,
                   e_seqf= c0tab with null(']^NULL^['),
                   e_seqa= c0tab with null(']^NULL^['),
                   e_seqi= c0tab with null(']^NULL^['),
                   e_datcr= c0tab,
                   e_datco= c0tab,
                   e_datsu= c0tab,
                   e_doc= c0tab,
                   e_stat= varchar (0) tab,
                   e_comme= varchar (0) tab,
                   e_commi= varchar (0) tab,
                   e_prec= varchar (0) tab,
                   e_dyna= varchar (0) tab,
                   e_dynf= varchar (0) tab,
                   e_dyni= varchar (0) tab,
                   e_expa= varchar (0) tab,
                   e_expf= varchar (0) tab,
                   e_expi= varchar (0) tab,
                   e_vlemf= varchar (0) tab,
                   e_vlemi= varchar (0) tab,
                   e_vlema= varchar (0) tab,
                   e_vcof= varchar (0) tab,
                   e_vcoi= varchar (0) tab,
                   e_vcoa= varchar (0) tab,
                   e_pima= c0tab,
                   e_datdi= c0tab,
                   e_datvi= c0tab,
                   e_nbeffa= c0tab,
                   e_nbefff= c0tab,
                   e_nbeffi= c0tab,
                   e_datpub= c0tab,
                   e_datmj= c0tab,
                   e_sexe= c0tab,
                   e_icoco= c0nl,
                   nl= d1)
             from 'qp1tbl3.data';
   EXEC SQL copy table qp1tbl6(
                   c_idf= c0tab,
                   e_idf= c0tab,
                   r_idf= c0tab,
                   col_c_idf= c0tab,
                   pe_date= c0tab,
                   pe_comment= varchar(0)tab,
                   pe_type= varchar(0)nl,
                   nl= d1)
             from 'qp1tbl6.data';
   EXEC SQL copy table qp1tbl8(
                   r_idf= c0tab,
                   r_desf= varchar(0)tab,
                   r_desa= varchar(0)tab,
                   r_desi= varchar(0)tab,
                   r_code= varchar(0)nl,
                   nl= d1)
             from 'qp1tbl8.data';
   EXEC SQL copy table qp1tbl10(
                   t_idf= c0tab,
                   e_idf= c0tab,
                   t_lg= varchar(0)tab,
                   t_comme= varchar(0)tab,
                   t_commi= varchar(0)tab,
                   t_datco= c0tab,
                   t_datcr= c0tab,
                   t_datsu= c0tab,
                   t_desa= varchar(0)tab,
                   t_desf= varchar(0)tab,
                   t_desi= varchar(0)tab,
                   t_desaup= varchar(0)tab,
                   t_desfup= varchar(0)tab,
                   t_desiup= varchar(0)tab,
                   t_datmj= c0tab,
                   t_nbprev= c0tab with null(']^NULL^['),
                   t_nbeffa= c0tab with null(']^NULL^['),
                   t_nbefff= c0tab with null(']^NULL^['),
                   t_nbeffi= c0tab with null(']^NULL^['),
                   t_seq= c0tab,
                   t_stat= varchar(0)nl,
                   nl= d1)
             from 'qp1tbl10.data';
   printf("Creating indexes...\n");
   EXEC SQL create index qp1tbl1_idx1 on qp1tbl1(
              e_lemaup,
              e_exaup,
              e_coaup)
            with structure = isam,
              nocompression,
              key = (e_lemaup, e_exaup, e_coaup),
              fillfactor = 80,
              location = (ii_database);
   EXEC SQL create index qp1tbl1_idx2 on qp1tbl1(
              e_lemfup,
              e_exfup,
              e_cofup)
            with structure = isam,
              nocompression,
              key = (e_lemfup, e_exfup, e_cofup),
              fillfactor = 80,
              location = (ii_database);
   EXEC SQL create index qp1tbl1_idx3 on qp1tbl1(
              e_lemiup,
              e_exiup,
              e_coiup)
            with structure = isam,
              nocompression,
              key = (e_lemiup, e_exiup, e_coiup),
              fillfactor = 80,
              location = (ii_database);
   EXEC SQL create index qp1tbl1_idx4 on qp1tbl1(
              e_sgml)
            with structure = hash,
              nocompression,
              key = (e_sgml),
              minpages = 2,
              fillfactor = 50,
              location = (ii_database);
   EXEC SQL create index qp1tbl1_idx5 on qp1tbl1(
              e_tria)
            with structure = isam,
              nocompression,
              key = (e_tria),
              fillfactor = 80,
              location = (ii_database);
   EXEC SQL create index qp1tbl1_idx6 on qp1tbl1(
              e_trif)
            with structure = isam,
              nocompression,
              key = (e_trif),
              fillfactor = 80,
              location = (ii_database);
   EXEC SQL create index qp1tbl1_idx7 on qp1tbl1(
              e_trii)
            with structure = isam,
              nocompression,
              key = (e_trii),
              fillfactor = 80,
              location = (ii_database);
   EXEC SQL create index qp1tbl3_idx1 on qp1tbl3(
              e_cat,
              e_idf)
            with structure = isam,
              nocompression,
              key = (e_cat, e_idf),
              fillfactor = 80,
              location = (ii_database);
   EXEC SQL create index qp1tbl6_idx1 on qp1tbl6(
              col_c_idf,
              e_idf,
              r_idf)
            with structure = isam,
              nocompression,
              key = (col_c_idf, e_idf, r_idf),
              fillfactor = 80,
              location = (ii_database);
   EXEC SQL create index qp1tbl6_idx2 on qp1tbl6(
              c_idf,
              e_idf)
            with structure = isam,
              nocompression,
              key = (c_idf, e_idf),
              fillfactor = 80,
              location = (ii_database);
   EXEC SQL create index qp1tbl6_idx3 on qp1tbl6(
              pe_type,
              r_idf)
            with structure = isam,
              nocompression,
              key = (pe_type, r_idf),
              fillfactor = 80,
              location = (ii_database);
   EXEC SQL create index qp1tbl10_idx1 on qp1tbl10(
              e_idf)
            with structure = hash,
              nocompression,
              key = (e_idf),
              minpages = 2,
              fillfactor = 50,
              location = (ii_database);
   EXEC SQL commit;

}
/*
** Fumction to display results
*/
void checkit()
{
   EXEC SQL select date(:endtime) - date(:starttime) into :etime;
   printf("\nSTART: %s\nEND:   %s\nET:    %s\n", starttime, endtime, etime);
}
