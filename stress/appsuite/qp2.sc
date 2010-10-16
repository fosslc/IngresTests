/*
**  Copyright (c) 2005, 2007 Ingres Corporation
**
**  Stress Test Application Suite
**
**  qp2.sc
**
**  History:
**
**  06-Jun-2003 sarjo01: Created
**  21-May-2004 sarjo01: Clean-up
**  08-Feb-2005 sarjo01: Added parallelism option
**  18-Mar-2005 sarjo01: Change MAXTHREADS to MAXCHILDTHREADS
**  14-Jun-2005 sarjo01: Added new error handler, changed page_size to 4096
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

#define MAXCHILDTHREADS 100
#define MAXVERBOSE 2
#define MINVERBOSE 0

void doit(int *p);
void checkit();
void print_err();
void createobjs();
void cleanup();

HANDLE  h[MAXCHILDTHREADS];

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
int parallel = 1;
int autocommit = 0;
int xperconn = 0; 
int abortfatal = 0;
int running;

char  *syntax =
 "\n"
 "Syntax:     qp2 <database> <function> [ <option> <option> ... ]\n\n"
 "<database>  target database name\n" \
 "<function>  init    - initialize qp2 database objects\n"
 "            run     - execute qp2 program, display results\n"
 "            cleanup - delete all qp2 database objects\n"
 "<option>    program option of the form -x[value]\n\n"
 " Option Function Description                    Param Values     Default\n"
 " ------ -------- ------------------------------ ---------------- -------\n"
 "   -a   run      Enable autocommit              0,1              disabled\n"
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
            case 'A':
               autocommit = 1;
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
	char connectName[45];
        char nodename[65];
	char stmtbuff[257];
   EXEC SQL end declare section;

   int loopcnt, q, cmd, xs, xst, i,j,k, error_code, reconn, neverdisc;
   int rows, trows, rb;

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
         if (parallel)
            EXEC SQL set parallel;
         else
            EXEC SQL set noparallel;
         if (autocommit)
            EXEC SQL set autocommit on;
         else
            EXEC SQL set autocommit off;
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
      EXEC SQL declare global temporary table SESSION.bills
                (sequence_number        integer4        not null with default,
                 del_file_tp            char(6)         not null with default,
                 del_format             char(6)         not null with default,
                 del_method             char(6)         not null with default,
                 original_flg           char(1)         not null with default,
                 bill_number            integer4        not null with default,
                 invoice_number         integer4        not null with default,
                 cust_id                integer4        not null with default,
                 ves_no                 integer4        not null with default,
                 ves_name               VARCHAR(30)     not null with default,
                 business_unit_id       char(5)         not null with default,
                 profit_centre_id       char(6)         not null with default,
                 posting_type_id        char(3)         not null with default,
                 posting_number         integer4        not null with default,
                 address_no             integer2        not null with default,
                 amount                 decimal(12,4)   not null with default,
                 gst                    decimal(12,4)   not null with default,
                 bill_run_dt            date            not null with default,
                 trans_date             date            not null with default,
                 wharf_cd               char(1)         not null with default,
                 manifest_direction     char(2)         not null with default)
        on commit preserve rows with norecovery;

      q=2;
      EXEC SQL insert into SESSION.bills
           select  tt.sequence_number,
                tt.del_file_tp,
                tt.del_format,
                tt.del_method,
                tt.original_flg,
                tt.bill_number,
                tt.invoice_number,
                tt.cust_id,
                tt.ves_no,
                tt.ves_name,
                tt.business_unit_id,
                tt.profit_centre_id,
                tt.posting_type_id,
                bc.posting_number,
                bc.address_no,
                bc.amount,
                bc.gst,
                bc.bill_run_dt,
                bc.trans_date,
                ac.wharf_cd,
                ac.manifest_direction
        from    qp2tbl2 tt,
                qp2tbl16 bc,
                qp2tbl15 ac
        where   tt.del_file_tp          != 'REPORT'
        and     bc.bill_number          = tt.bill_number
        and     (bc.invoice_number != 0 OR 'A' = 'V')
        and     ac.posting_number       = bc.posting_number;
 
      q=3;
      EXEC SQL declare global temporary table SESSION.billnos AS
        select  distinct
                posting_number,
                bill_number
        from    SESSION.bills
        on commit preserve rows with norecovery;

      q=4;
      EXEC SQL declare global temporary table SESSION.postings AS
        select  ap.*
        from    SESSION.billnos bb,
                qp2tbl10 ap
        where   ap.posting_number       = bb.posting_number
        and     ap.bill_number          = bb.bill_number
        on commit preserve rows with norecovery;

      q=5;
      EXEC SQL declare global temporary table SESSION.tois AS
        select  distinct
                acct_posting_key_orig,
                total_toi_discount      = sum(amount)
        from    SESSION.postings
        where   link_type               = 'D'
        and     total_on_invoice        = 'Y'
        group by
                acct_posting_key_orig
        on commit preserve rows with norecovery;

      q=6;
      EXEC SQL update  SESSION.postings
        set     nett_amount = amount
        where   link_type               = 'D'
        and     total_on_invoice        = 'Y';

      q=7;
      EXEC SQL update  SESSION.postings p
        from    SESSION.tois t
        set     nett_amount = p.nett_amount - t.total_toi_discount
        where   p.link_type             != 'D'
        and     t.acct_posting_key_orig = p.acct_posting_key;

      q=8;
      EXEC SQL declare global temporary table SESSION.vessel
                (bill_number            integer4        not null with default,
                 ves_ref                char(7)         not null with default,
                 voy_ib                 char(6)         not null with default,
                 voy_ob                 char(6)         not null with default,
                 hp_visit_no            integer4        not null with default,
                 hdr_wharf              char(2)         not null with default,
                 line_oper              char(3)         not null with default)
        on commit preserve rows with norecovery;

      q=9;
      EXEC SQL insert into SESSION.vessel
        select  ap.bill_number,
                ves_ref         = ifnull(max(ap.ves_ref),' '),
                voy_ib          = ifnull(max(ap.voy_ib),' '),
                voy_ob          = ifnull(max(ap.voy_ob),' '),
                hp_visit_no     = ifnull(max(ap.hp_visit_no),0),
                hdr_wharf       = ifnull(max(ap.wharf),' '),
                line_oper       = ifnull(max(ap.line_oper),' ')
        from    SESSION.bills bb,
                SESSION.postings ap
        where   ap.posting_number       = bb.posting_number
        and     ap.bill_number          = bb.bill_number
        group by
                ap.bill_number;

      q=10;
      EXEC SQL declare global temporary table SESSION.headers
                (sequence_number        integer4        not null with default,
                 del_file_tp            char(6)         not null with default,
                 del_format             char(6)         not null with default,
                 del_method             char(6)         not null with default,
                 original_flg           char(1)         not null with default,
                 bill_number            integer4        not null with default,
                 posting_number         integer4        not null with default,
                 invoice_number         integer4        not null with default,
                 bill_run_dt            date            not null with default,
                 trans_date             date            not null with default,
                 bill_amount            decimal(12,4)   not null with default,
                 bill_gst               decimal(12,4)   not null with default,
                 ves_no                 integer4        not null with default,
                 wharf_cd               char(1)         not null with default,
                 manifest_direction     char(2)         not null with default,
                 posting_type_id        char(3)         not null with default,
                 ves_ref                char(7)         not null with default,
                 voy_ib                 char(6)         not null with default,
                 voy_ob                 char(6)         not null with default,
                 hp_visit_no            VARCHAR(7)      not null with default,
                 hdr_wharf              char(2)         not null with default,
                 line_oper              char(3)         not null with default,
                 hdr_port               VARCHAR(30)     not null with default,
                 ves_name               VARCHAR(30)     not null with default,
                 lop_name               VARCHAR(30)     not null with default,
                 profit_centre_name     VARCHAR(30)     not null with default,
                 contact_phone          VARCHAR(20)     not null with default,
                 business_unit_name     VARCHAR(30)     not null with default,
                 invoice_address_1      VARCHAR(25)     not null with default,
                 invoice_address_2      VARCHAR(25)     not null with default,
                 invoice_address_3      VARCHAR(25)     not null with default,
                 invoice_address_4      VARCHAR(25)     not null with default,
                 invoice_post_code      char(4)         not null with default,
                 gst_number             char(10)        not null with default,
                 cust_name              VARCHAR(36)     not null with default,
                 address_no             smallint        not null with default,
                 cust_address_1         VARCHAR(26)     not null with default,
                 cust_address_2         VARCHAR(26)     not null with default,
                 cust_address_3         VARCHAR(26)     not null with default,
                 cust_address_4         VARCHAR(26)     not null with default,
                 post_code              char(4)         not null with default,
                 posting_type_desc      VARCHAR(30)     not null with default,
                 number_lines           smallint        not null with default)
        on commit preserve rows with norecovery;

      q=11;
      EXEC SQL insert into SESSION.headers
        select  bb.sequence_number,
                bb.del_file_tp,
                bb.del_format,
                bb.del_method,
                bb.original_flg,
                bb.bill_number,
                bb.posting_number,
                bb.invoice_number,
                bb.bill_run_dt,
                bb.trans_date,
                bill_amount             = bb.amount,
                bill_gst                = bb.gst,
                bb.ves_no,
                bb.wharf_cd,
                bb.manifest_direction,
                bb.posting_type_id,
                sv.ves_ref,
                sv.voy_ib,
                sv.voy_ob,
                hp_visit_no             = VARCHAR(sv.hp_visit_no),
                sv.hdr_wharf,
                sv.line_oper,
                hdr_port                = '                              ',
                ves_name                = bb.ves_name,
                lop_name                = '                              ',
                profit_centre_name      = ifnull(pc.profit_centre_name,' '),
                contact_phone           = ifnull(pc.contact_phone,' '),
                business_unit_name      = ifnull(bu.business_unit_name,' '),
                invoice_address_1       = ifnull(bu.invoice_address_1,' '),
                invoice_address_2       = ifnull(bu.invoice_address_2,' '),
                invoice_address_3       = ifnull(bu.invoice_address_3,' '),
                invoice_address_4       = ifnull(bu.invoice_address_4,' '),
                invoice_post_code       = ifnull(bu.invoice_post_code,' '),
                gst_number              = ifnull(bu.gst_number,' '),
                cust_name               = ifnull(cu.cust_name,' '),
                address_no              = ifnull(ca.address_no,0),
                cust_address_1          = ifnull(ca.cust_address_1,' '),
                cust_address_2          = ifnull(ca.cust_address_2,' '),
                cust_address_3          = ifnull(ca.cust_address_3,' '),
                cust_address_4          = ifnull(ca.cust_address_4,' '),
                post_code               = ifnull(ca.post_code,' '),
                posting_type_desc       = ifnull(pt.posting_type_desc,' '),
                number_lines            = 0
        from    SESSION.bills bb
                left join       qp2tbl13 cu
                        on      cu.business_unit_id     = bb.business_unit_id
                        and     cu.cust_id              = bb.cust_id
                left join       qp2tbl7 pc
                        on      pc.profit_centre_id     = bb.profit_centre_id
                left join       qp2tbl9 bu
                        on      bu.business_unit_id     = bb.business_unit_id
                left join       qp2tbl11 ca
                        on      ca.business_unit_id     = bb.business_unit_id
                        and     ca.cust_id              = bb.cust_id
                        and     ca.address_no           = bb.address_no
                left join       qp2tbl5 pt
                        on      pt.posting_type_id      = bb.posting_type_id
                ,
                SESSION.vessel sv
        where   sv.bill_number = bb.bill_number;

      q=12;
      EXEC SQL update  SESSION.headers h
        from    qp2tbl1 w,
                qp2tbl6 c
        set     hdr_port = c.cd_desc
        where   w.wharf = h.hdr_wharf
        and     c.cd_tp = 'REVHBR'
        and     c.cd    = w.harbour;

      q=13;
      EXEC SQL update  SESSION.headers h
        from    qp2tbl6 c
        set     lop_name = c.cd_desc
        where   c.cd_tp = 'LINE'
        and     c.cd    = h.line_oper;

      q=14;
      EXEC SQL update  SESSION.headers
        set     cust_name = ' '
        where   address_no > 1;

      q=15;
      EXEC SQL declare global temporary table SESSION.lines
                (sequence_number        integer4        not null with default,
                 bill_number            integer4        not null with default,
                 bill_line              integer2        not null with default,
                 wharf                  char(2)         not null with default,
                 service_date           date            not null with default,
                 item_code              char(10)        not null with default,
                 charge_basis           char(8)         not null with default,
                 rate_type              char(3)         not null with default,
                 item_type              char(1)         not null with default,
                 link_type              char(1)         not null with default,
                 no_of_units            float4          not null with default,
                 units_1                float4          not null with default,
                 units_2                float4          not null with default,
                 rate                   float4          not null with default,
                 amount                 decimal(12,4)   not null with default,
                 nett_amount            decimal(12,4)   not null with default,
                 gst                    decimal(12,4)   not null with default,
                 docket_number          integer4        not null with default,
                 bill_of_lading         char(10)        not null with default,
                 port                   char(3)         not null with default,
                 port_name              VARCHAR(30)     not null with default,
                 plant_id               char(6)         not null with default,
                 plant_category_id      char(6)         not null with default,
                 ctr_cat                char(1)         not null with default,
                 ctr_sts_cd             char(1)         not null with default,
                 summarise_on_invoice   char(1)         not null with default,
                 total_on_invoice       char(1)         not null with default,
                 item_code_key          integer4        not null with default,
                 line_comment           VARCHAR(200)    not null with default,
                 acct_posting_key       integer4        not null with default,
                 ves_dep_act            date            not null with default,
                 ctr_no                 char(12)        not null with default,
                 ctr_key                integer         not null with default,
                 ctr_iso                char(4)         not null with default,
                 wt_ctr_gr              float4          not null with default,
                 haz_flg                char(1)         not null with default,
                 from_loc               char(7)         not null with default,
                 to_loc                 char(7)         not null with default,
                 arr_ts                 date            not null with default,
                 dep_ts                 date            not null with default,
                 commod_cd              char(4)         not null with default,
                 reef_ctrl_temp         float4          not null with default,
                 crn_id                 char(1)         not null with default,
                 start_date             date            not null with default,
                 end_date               date            not null with default,
                 country_port           char(5)         not null with default,
                 country_fdp            char(5)         not null with default,
                 bkg_ref                char(14)        not null with default,
                 haz_cl_min             char(4)         not null with default,
                 ves_ref                char(7)         not null with default,
                 dem_ts                 date            not null with default,
                 stop_cd                char(1)         not null with default)
        on commit preserve rows with norecovery;

      q=16;
      EXEC SQL insert into SESSION.lines
                (sequence_number,
                 bill_number,
                 bill_line,
                 wharf,
                 service_date,
                 item_code,
                 charge_basis,
                 rate_type,
                 item_type,
                 link_type,
                 no_of_units,
                 units_1,
                 units_2,
                 rate,
                 amount,
                 nett_amount,
                 gst,
                 docket_number,
                 bill_of_lading,
                 port,
                 plant_id,
                 plant_category_id,
                 ctr_cat,
                 ctr_sts_cd,
                 summarise_on_invoice,
                 total_on_invoice,
                 item_code_key)
        select  bb.sequence_number,
                bb.bill_number,
                ap.bill_line,
                wharf                   = max(ap.wharf),
                service_date            = max(ap.service_date),
                item_code               = max(ap.item_code),
                charge_basis            = max(ap.charge_basis),
                rate_type               = max(ap.rate_type),
                item_type               = max(ap.item_type),
                link_type               = max(ap.link_type),
                no_of_units             = sum(ap.no_of_units),
                units_1                 = sum(ap.units_1),
                units_2                 = sum(ap.units_2),
                rate                    = max(ap.rate),
                amount                  = sum(ap.amount),
                nett_amount             = sum(ap.nett_amount),
                gst                     = sum(ap.gst),
                docket_number           = max(ap.docket_number),
                bill_of_lading          = max(ap.bill_of_lading),
                port                    = max(ap.port),
                plant_id                = max(ap.plant_id),
                plant_category_id       = max(ap.plant_category_id),
                ctr_cat                 = max(ap.ctr_cat),
                ctr_sts_cd              = max(ap.ctr_sts_cd),
                summarise_on_invoice    = max(ap.summarise_on_invoice),
                total_on_invoice        = max(ap.total_on_invoice),
                item_code_key           = max(ap.item_code_key)
        from    SESSION.bills bb,
                SESSION.postings ap
        where   ap.posting_number       = bb.posting_number
        and     ap.bill_number          = bb.bill_number
        and     (ap.amount != 0 OR 'A' = 'V')
        and     left(bb.del_file_tp,3)  = 'INV'
        group by
                bb.sequence_number,
                bb.bill_number,
                ap.bill_line;

      q=17;
      EXEC SQL insert into SESSION.lines
                (sequence_number,
                 bill_number,
                 bill_line,
                 wharf,
                 service_date,
                 item_code,
                 charge_basis,
                 rate_type,
                 item_type,
                 link_type,
                 no_of_units,
                 units_1,
                 units_2,
                 rate,
                 amount,
                 nett_amount,
                 gst,
                 docket_number,
                 bill_of_lading,
                 port,
                 plant_id,
                 plant_category_id,
                 ctr_cat,
                 ctr_sts_cd,
                 summarise_on_invoice,
                 total_on_invoice,
                 item_code_key,
                 acct_posting_key,
                 ves_dep_act,
                 ctr_no,
                 ctr_key,
                 ctr_iso,
                 wt_ctr_gr,
                 haz_flg,
                 from_loc,
                 to_loc,
                 arr_ts,
                 dep_ts,
                 commod_cd,
                 reef_ctrl_temp,
                 crn_id,
                 start_date,
                 end_date,
                 country_port,
                 country_fdp,
                 bkg_ref,
                 ves_ref)
        select  bb.sequence_number,
                bb.bill_number,
                ap.bill_line,
                ap.wharf,
                ap.service_date,
                ap.item_code,
                ap.charge_basis,
                ap.rate_type,
                ap.item_type,
                ap.link_type,
                ap.no_of_units,
                ap.units_1,
                ap.units_2,
                ap.rate,
                ap.amount,
                ap.nett_amount,
                ap.gst,
                ap.docket_number,
                ap.bill_of_lading,
                ap.port,
                ap.plant_id,
                ap.plant_category_id,
                ap.ctr_cat,
                ap.ctr_sts_cd,
                ap.summarise_on_invoice,
                ap.total_on_invoice,
                ap.item_code_key,
                ap.acct_posting_key,
                ap.ves_dep_act,
                ap.ctr_no,
                ap.ctr_key,
                ap.ctr_iso,
                ap.wt_ctr_gr,
                ap.haz_flg,
                ap.from_loc,
                ap.to_loc,
                ap.arr_ts,
                ap.dep_ts,
                ap.commod_cd,
                ifnull(ap.reef_ctrl_temp,-666),
                ap.crn_id,
                ap.start_date,
                ap.end_date,
                ap.port,
                ap.fdp,
                ap.bkg_ref,
                ap.ves_ref
        from    SESSION.bills bb,
                SESSION.postings ap
        where   ap.posting_number       = bb.posting_number
        and     ap.bill_number          = bb.bill_number
        and     (ap.amount != 0 OR 'A' = 'V')
        and     left(bb.del_file_tp,3)  = 'DET';

      q=18;
      EXEC SQL update  SESSION.lines l
        from    qp2tbl4 p
        set     port_name = p.port_name
        where   l.port != ' '
        and     p.port_cd = l.port;

      q=19;
      EXEC SQL update  SESSION.lines l
        from    qp2tbl4 p
        set     country_port = p.un_ctry_cd + p.port_cd
        where   l.country_port != ' '
        and     p.port_cd = l.country_port;

      q=20;
      EXEC SQL update  SESSION.lines l
        from    qp2tbl4 p
        set     country_fdp = p.un_ctry_cd + p.port_cd
        where   l.country_fdp != ' '
        and     p.port_cd = l.country_fdp;

      q=21;
      EXEC SQL update  SESSION.lines l
        from    SESSION.headers h,
                qp2tbl8 v
        set     dem_ts = v.dem_ts
        where   l.ves_ref               != ' '
        and     h.sequence_number       = l.sequence_number
        and     v.ves_ref               = l.ves_ref
        and     v.wharf_cd              = h.wharf_cd;

      q=22;
      EXEC SQL update  SESSION.lines l
        from    qp2tbl18 c
        set     haz_cl_min = c.haz_cl_min
        where   c.ctr_key = l.ctr_key;

      q=23;
      EXEC SQL update  SESSION.lines l
        from    SESSION.bills b
        set     line_comment = line_comment + '; ' + VARCHAR(l.port_name)
        where   b.sequence_number = l.sequence_number
        and     b.posting_type_id = 'GW'
        and     l.port_name != ' '
        and     l.link_type != 'D';

      q=24;
      EXEC SQL update  SESSION.lines
        set     line_comment = line_comment + '; ' + VARCHAR(docket_number)
        where   docket_number != 0
        and     link_type != 'D';

      q=25;
      EXEC SQL update  SESSION.lines
        set     line_comment = line_comment + '; ' + VARCHAR(bill_of_lading)
        where   bill_of_lading != ' '
        and     link_type != 'D';

      q=26;
      EXEC SQL update  SESSION.lines
        set     line_comment = line_comment + '; ' + VARCHAR(plant_id)
        where   plant_id != ' '
        and     link_type != 'D';

      q=27;
      EXEC SQL declare global temporary table SESSION.notes
                (sequence_number        integer4        not null with default,
                 bill_number            integer4        not null with default,
                 bill_line              integer2        not null with default,
                 note_locn              char(1)         not null with default,
                 note_key               integer4        not null with default,
                 note_txt               VARCHAR(800)    not null with default)
        on commit preserve rows with norecovery;

      q=28;
      EXEC SQL insert into SESSION.notes
        select  b.sequence_number,
                b.bill_number,
                n.bill_line,
                n.note_locn,
                n.note_key,
                n.note_txt
        from    SESSION.bills b,
                qp2tbl19 n
        where   n.bill_number   = b.bill_number
        and     n.bill_number   != 0
        and     n.note_locn     != 'I';

      q=29;
      EXEC SQL insert into SESSION.notes
        select  b.sequence_number,
                b.bill_number,
                l.bill_line,
                note_locn       = 'F',
                note_key        = 0,
                note_txt        = shift(l.line_comment,-2)
        from    SESSION.bills b,
                SESSION.lines l
        where   l.sequence_number       = b.sequence_number
        and     l.line_comment          != ' '
        and     l.summarise_on_invoice  != 'Y'
        and     left(b.del_file_tp,3)   = 'INV';

      q=30;
      EXEC SQL declare global temporary table SESSION.counts
                (sequence_number        integer4        not null with default,
                 line_type              char(4)         not null with default,
                 bill_line              integer2        not null with default,
                 rec_count              integer2        not null with default)
        on commit preserve rows with norecovery;

      q=31;
      EXEC SQL insert into SESSION.counts
        select  sequence_number,
                line_type       = 'line',
                bill_line       = 0,
                rec_count       = COUNT(*)
        from    SESSION.lines
        where   stop_cd = ' '
        group by
                sequence_number;

      q=32;
      EXEC SQL insert into SESSION.counts
        select  sequence_number,
                line_type       = 'note',
                bill_line       = 0,
                rec_count       = COUNT(*)
        from    SESSION.notes n
        group by
                sequence_number;

      q=33;
      EXEC SQL insert into SESSION.counts
        select  sequence_number,
                line_type       = 'lnnt',
                bill_line,
                rec_count       = COUNT(*)
        from    SESSION.notes
        group by
                sequence_number,
                bill_line;

      q=34;
      EXEC SQL insert into SESSION.counts
        select  sequence_number,
                line_type       = 'lnnf',
                bill_line,
                rec_count       = COUNT(*)
        from    SESSION.notes
        where   note_locn = 'F'
        group by
                sequence_number,
                bill_line;

      q=35;
      EXEC SQL update  SESSION.headers h
        from    SESSION.counts c
        set     number_lines = c.rec_count
        where   h.del_file_tp           != 'INVNOT'
        and     c.sequence_number       = h.sequence_number
        and     c.line_type             = 'line';

      q=36;
      EXEC SQL update  SESSION.headers h
        from    SESSION.counts c
        set     number_lines = h.number_lines + c.rec_count
        where   right(h.del_file_tp,3)  != 'LIN'
        and     c.sequence_number       = h.sequence_number
        and     c.line_type             = 'note';

      q=37;
      EXEC SQL declare global temporary table SESSION.thelot as
        select  h.sequence_number,
                h.del_file_tp,
                h.del_format,
                h.del_method,
                h.original_flg,
                h.bill_number,
                h.posting_number,
                h.invoice_number,
                h.bill_run_dt,
                h.trans_date,
                h.bill_amount,
                h.bill_gst,
                h.hdr_port,
                h.ves_ref,
                h.voy_ib,
                h.voy_ob,
                h.hp_visit_no,
                h.ves_name,
                h.line_oper,
                h.lop_name,
                h.profit_centre_name,
                h.contact_phone,
                h.business_unit_name,
                h.invoice_address_1,
                h.invoice_address_2,
                h.invoice_address_3,
                h.invoice_address_4,
                h.invoice_post_code,
                h.gst_number,
                h.cust_name,
                h.cust_address_1,
                h.cust_address_2,
                h.cust_address_3,
                h.cust_address_4,
                h.post_code,
                h.manifest_direction,
                h.posting_type_id,
                h.posting_type_desc,
                h.number_lines,
                l.bill_line,
                note_count              = ifnull(x.rec_count,0),
                footer_count            = ifnull(y.rec_count,0),
                l.wharf,
                l.service_date,
                l.item_code,
                l.charge_basis,
                l.rate_type,
                l.item_type,
                l.link_type,
                l.no_of_units,
                l.units_1,
                l.units_2,
                l.rate,
                l.amount,
                l.nett_amount,
                l.gst,
                l.docket_number,
                l.bill_of_lading,
                l.ctr_cat,
                l.ctr_sts_cd,
                l.total_on_invoice,
                l.acct_posting_key,
                l.ves_dep_act,
                l.ctr_no,
                l.ctr_key,
                l.ctr_iso,
                l.wt_ctr_gr,
                l.haz_flg,
                l.from_loc,
                l.to_loc,
                l.arr_ts,
                l.dep_ts,
                l.commod_cd,
                l.reef_ctrl_temp,
                l.crn_id,
                l.start_date,
                l.end_date,
                l.country_port,
                l.country_fdp,
                l.bkg_ref,
                l.haz_cl_min,
                l.dem_ts,
                l.plant_id,
                l.plant_category_id,
                l.stop_cd,
                charge_basis_desc       = ifnull(c.charge_basis_desc,' '),
                charge_unit_1           = ifnull(c.charge_unit_1,' '),
                charge_unit_2           = ifnull(c.charge_unit_2,' '),
                charge_unit_desc        = ifnull(c.charge_unit_desc,' '),
                item_desc               = ifnull(i.item_desc,' '),
                crn_desc                = ifnull(k.cd_desc,' '),
                plant_desc              = ifnull(p.plant_desc,' '),
                plant_category_desc     = ifnull(q.plant_category_desc,' '),
                note_locn               = 'G',
                note_key                = int4(0)
        from    session.lines l
                left join       qp2tbl12 c
                        on      c.charge_basis = l.charge_basis
                left join       qp2tbl3 i
                        on      i.pkey = l.item_code_key
                left join       qp2tbl6 k
                        on      k.cd_tp = 'CRANE'
                        and     k.cd    = l.crn_id
                left join       qp2tbl17 p
                        on      p.plant_id = l.plant_id
                left join       qp2tbl14 q
                        on      q.plant_category_id = l.plant_category_id
                left join       session.counts x
                        on      x.sequence_number = l.sequence_number
                        and     x.line_type = 'lnnt'
                        and     x.bill_line = l.bill_line
                left join       session.counts y
                        on      y.sequence_number = l.sequence_number
                        and     y.line_type = 'lnnt'
                        and     y.bill_line = l.bill_line
                ,
                session.headers h
                left join       session.notes n
                        on      n.tid = -1
        where   h.sequence_number = l.sequence_number
        on commit preserve rows with norecovery;

      q=37;
      EXEC SQL insert  into SESSION.thelot
                (sequence_number,
                 del_file_tp,
                 del_format,
                 del_method,
                 original_flg,
                 bill_number,
                 posting_number,
                 invoice_number,
                 bill_run_dt,
                 trans_date,
                 bill_amount,
                 bill_gst,
                 hdr_port,
                 ves_ref,
                 voy_ib,
                 voy_ob,
                 hp_visit_no,
                 ves_name,
                 line_oper,
                 lop_name,
                 profit_centre_name,
                 contact_phone,
                 business_unit_name,
                 invoice_address_1,
                 invoice_address_2,
                 invoice_address_3,
                 invoice_address_4,
                 invoice_post_code,
                 gst_number,
                 cust_name,
                 cust_address_1,
                 cust_address_2,
                 cust_address_3,
                 cust_address_4,
                 post_code,
                 manifest_direction,
                 posting_type_id,
                 posting_type_desc,
                 number_lines,
                 bill_line,
                 note_count,
                 note_locn,
                 note_key)
        select  h.sequence_number,
                h.del_file_tp,
                h.del_format,
                h.del_method,
                h.original_flg,
                h.bill_number,
                h.posting_number,
                h.invoice_number,
                h.bill_run_dt,
                h.trans_date,
                h.bill_amount,
                h.bill_gst,
                h.hdr_port,
                h.ves_ref,
                h.voy_ib,
                h.voy_ob,
                h.hp_visit_no,
                h.ves_name,
                h.line_oper,
                h.lop_name,
                h.profit_centre_name,
                h.contact_phone,
                h.business_unit_name,
                h.invoice_address_1,
                h.invoice_address_2,
                h.invoice_address_3,
                h.invoice_address_4,
                h.invoice_post_code,
                h.gst_number,
                h.cust_name,
                h.cust_address_1,
                h.cust_address_2,
                h.cust_address_3,
                h.cust_address_4,
                h.post_code,
                h.manifest_direction,
                h.posting_type_id,
                h.posting_type_desc,
                h.number_lines,
                n.bill_line,
                ifnull(x.rec_count,0),
                n.note_locn,
                n.note_key
        from    session.notes n
                left join       session.counts x
                        on      x.sequence_number = n.sequence_number
                        and     x.line_type = 'lnnt'
                        and     x.bill_line = n.bill_line
                ,
                session.headers h
        where   h.sequence_number = n.sequence_number;

      EXEC SQL commit;
      xactscompleted[pval-1]++;

      if (verbose == MAXVERBOSE)
      {
         if (nodes)
            printf("T%02d:%06d %s\n", pval, i+1, nodename);
         else
            printf("T%02d:%06d\n", pval, i+1);
      }

      if (neverdisc == 0 && --xs == 0)
      { 
         EXEC SQL disconnect :connectName;
         xs = xst;
         reconn = 1;
      }
      else
      {
         EXEC SQL drop table session.bills;
         EXEC SQL drop table session.billnos;
         EXEC SQL drop table session.postings;
         EXEC SQL drop table session.tois;
         EXEC SQL drop table session.vessel;
         EXEC SQL drop table session.headers;
         EXEC SQL drop table session.lines;
         EXEC SQL drop table session.notes;
         EXEC SQL drop table session.counts;
         EXEC SQL drop table session.thelot;
         EXEC SQL commit;
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

   EXEC SQL drop table qp2tbl1;
   EXEC SQL drop table qp2tbl2;
   EXEC SQL drop table qp2tbl3;
   EXEC SQL drop table qp2tbl4;
   EXEC SQL drop table qp2tbl5;
   EXEC SQL drop table qp2tbl6;
   EXEC SQL drop table qp2tbl7;
   EXEC SQL drop table qp2tbl8;
   EXEC SQL drop table qp2tbl9;
   EXEC SQL drop table qp2tbl10;
   EXEC SQL drop table qp2tbl11;
   EXEC SQL drop table qp2tbl12;
   EXEC SQL drop table qp2tbl13;
   EXEC SQL drop table qp2tbl14;
   EXEC SQL drop table qp2tbl15;
   EXEC SQL drop table qp2tbl16;
   EXEC SQL drop table qp2tbl17;
   EXEC SQL drop table qp2tbl18;
   EXEC SQL drop table qp2tbl19;

}
/*
** Function to create db objects
*/
void createobjs()
{
   EXEC SQL begin declare section;
   EXEC SQL end declare section;

   EXEC SQL set autocommit on;

   cleanup();

   EXEC SQL whenever sqlerror call sqlprint;

   printf("Creating tables...\n");

   EXEC  SQL create table qp2tbl1(
        wharf char(2) not null not default,
        wharf_desc varchar(30) not null default ' ',
        harbour char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL copy table qp2tbl1(
        wharf= varchar(0)tab,
        wharf_desc= varchar(0)tab,
        harbour= varchar(0)nl,
        nl= d1)
   from 'qp2tbl1.data';

   EXEC SQL create table qp2tbl2(
        posting_number integer not null not default,
        bill_number integer not null not default,
        invoice_number integer not null default 0,
        cust_id integer not null default 0,
        ves_no integer not null default 0,
        trans_date date not null default ' ',
        bill_run_dt date not null default ' ',
        ves_name varchar(30) not null default ' ',
        business_unit_id char(5) not null default ' ',
        profit_centre_id char(6) not null default ' ',
        posting_type_id char(3) not null not default,
        del_file_tp char(6) not null not default,
        del_format char(6) not null not default,
        del_method char(6) not null not default,
        del_details varchar(50) not null not default,
        original_flg char(1) not null default ' ',
        sequence_number integer not null,
        business_type varchar(4) not null,
        delivery_status varchar(6) not null,
        file_type varchar(3) not null,
        file_suffix varchar(3) not null,
        filename varchar(29) not null,
        title varchar(78) not null
   ) with page_size=4096;
   EXEC SQL copy table qp2tbl2(
        posting_number= c0tab,
        bill_number= c0tab,
        invoice_number= c0tab,
        cust_id= c0tab,
        ves_no= c0tab,
        trans_date= c0tab,
        bill_run_dt= c0tab,
        ves_name= varchar(0)tab,
        business_unit_id= varchar(0)tab,
        profit_centre_id= varchar(0)tab,
        posting_type_id= varchar(0)tab,
        del_file_tp= varchar(0)tab,
        del_format= varchar(0)tab,
        del_method= varchar(0)tab,
        del_details= varchar(0)tab,
        original_flg= varchar(0)tab,
        sequence_number= c0tab,
        business_type= varchar(0)tab,
        delivery_status= varchar(0)tab,
        file_type= varchar(0)tab,
        file_suffix= varchar(0)tab,
        filename= varchar(0)tab,
        title= varchar(0)nl,
        nl= d1)
   from 'qp2tbl2.data';

   EXEC SQL create table qp2tbl3(
        pkey integer not null not default,
        item_code char(10) not null not default,
        item_desc varchar(40) not null default ' ',
        item_short_desc char(8) not null default ' ',
        plant_catg_reqd char(1) not null default ' ',
        gst_type char(1) not null default ' ',
        item_type char(1) not null default ' ',
        discount_method_default char(1) not null default ' ',
        discount_sequence_default smallint not null default 0,
        record_eff_date date not null default ' ',
        record_end_date date not null default ' ',
        record_create_date date not null default ' ',
        record_modify_date date not null default ' ',
        record_delete_date date not null default ' ',
        record_create_user char(8) not null default ' ',
        record_modify_user char(8) not null default ' ',
        record_delete_user char(8) not null default ' ',
        record_status char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl3 to hash unique on
        pkey with fillfactor = 50,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl3(
        pkey= c0tab,
        item_code= varchar(0)tab,
        item_desc= varchar(0)tab,
        item_short_desc= varchar(0)tab,
        plant_catg_reqd= varchar(0)tab,
        gst_type= varchar(0)tab,
        item_type= varchar(0)tab,
        discount_method_default= varchar(0)tab,
        discount_sequence_default= c0tab,
        record_eff_date= c0tab,
        record_end_date= c0tab,
        record_create_date= c0tab,
        record_modify_date= c0tab,
        record_delete_date= c0tab,
        record_create_user= varchar(0)tab,
        record_modify_user= varchar(0)tab,
        record_delete_user= varchar(0)tab,
        record_status= varchar(0)nl,
        nl= d1)
   from 'qp2tbl3.data';

   EXEC SQL create table qp2tbl4(
        port_cd char(3) not null not default,
        un_ctry_cd char(2) not null default ' ',
        un_loc_cd char(3) not null default ' ',
        port_name varchar(30) not null default ' ',
        usr_name char(8) not null default ' ',
        dt_mod date not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl4 to btree unique on
        port_cd with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl4(
        port_cd= varchar(0)tab,
        un_ctry_cd= varchar(0)tab,
        un_loc_cd= varchar(0)tab,
        port_name= varchar(0)tab,
        usr_name= varchar(0)tab,
        dt_mod= c0nl,
        nl= d1)
   from 'qp2tbl4.data';

   EXEC SQL create table qp2tbl5(
        posting_type_id char(3) not null not default,
        posting_type_desc varchar(30) not null default ' ',
        record_create_date date not null default ' ',
        record_modify_date date not null default ' ',
        record_delete_date date not null default ' ',
        record_create_user char(8) not null default ' ',
        record_modify_user char(8) not null default ' ',
        record_delete_user char(8) not null default ' ',
        record_status char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl5 to btree unique on
        posting_type_id with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl5(
        posting_type_id= varchar(0)tab,
        posting_type_desc= varchar(0)tab,
        record_create_date= c0tab,
        record_modify_date= c0tab,
        record_delete_date= c0tab,
        record_create_user= varchar(0)tab,
        record_modify_user= varchar(0)tab,
        record_delete_user= varchar(0)tab,
        record_status= varchar(0)nl,
        nl= d1)
   from 'qp2tbl5.data';

   EXEC SQL create table qp2tbl6(
        cd_tp char(6) not null not default,
        cd char(6) not null not default,
        cd_desc varchar(50) not null default ' ',
        usr_name char(8) not null default ' ',
        dt_mod date not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl6 to btree unique on
        cd_tp, cd with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl6(
        cd_tp= varchar(0)tab,
        cd= varchar(0)tab,
        cd_desc= varchar(0)tab,
        usr_name= varchar(0)tab,
        dt_mod= c0nl,
        nl= d1)
   from 'qp2tbl6.data';

   EXEC SQL create table qp2tbl7(
        profit_centre_id char(6) not null not default,
        profit_centre_name varchar(30) not null default ' ',
        business_unit_id char(5) not null default ' ',
        contact_logon_name varchar(8) not null default ' ',
        contact_phone varchar(20) not null default ' ',
        manager_logon_name varchar(8) not null default ' ',
        manager_email varchar(50) not null default ' ',
        invoice_lpr_cd char(6) not null default ' ',
        report_lpr_cd char(6) not null default ' ',
        record_create_date date not null default ' ',
        record_modify_date date not null default ' ',
        record_delete_date date not null default ' ',
        record_create_user char(8) not null default ' ',
        record_modify_user char(8) not null default ' ',
        record_delete_user char(8) not null default ' ',
        record_status char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl7 to btree unique on
        profit_centre_id with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl7(
        profit_centre_id= varchar(0)tab,
        profit_centre_name= varchar(0)tab,
        business_unit_id= varchar(0)tab,
        contact_logon_name= varchar(0)tab,
        contact_phone= varchar(0)tab,
        manager_logon_name= varchar(0)tab,
        manager_email= varchar(0)tab,
        invoice_lpr_cd= varchar(0)tab,
        report_lpr_cd= varchar(0)tab,
        record_create_date= c0tab,
        record_modify_date= c0tab,
        record_delete_date= c0tab,
        record_create_user= varchar(0)tab,
        record_modify_user= varchar(0)tab,
        record_delete_user= varchar(0)tab,
        record_status= varchar(0)nl,
        nl= d1)
   from 'qp2tbl7.data';

   EXEC SQL create table qp2tbl8(
        ves_ref char(7) not null not default,
        wharf_cd char(1) not null not default,
        ves_cd char(3) not null default ' ',
        sop char(3) not null default ' ',
        stevedore char(3) not null default ' ',
        ves_ser char(6) not null default ' ',
        sop_voy_ib char(6) not null default ' ',
        sop_voy_ob char(6) not null default ' ',
        rec_cutof_flg char(1) not null default ' ',
        ves_berth char(5) not null default ' ',
        ves_pilot_est date not null default ' ',
        ves_pilot_act date not null default ' ',
        ves_berth_est date not null default ' ',
        ves_berth_act date not null default ' ',
        ves_comm_est date not null default ' ',
        ves_comm_act date not null default ' ',
        ves_compl_est date not null default ' ',
        ves_compl_act date not null default ' ',
        ves_dep_est date not null default ' ',
        ves_dep_act date not null default ' ',
        ves_beg_rec date not null default ' ',
        ves_end_rec date not null default ' ',
        pacts_purge_ts date not null default ' ',
        sparcs_purge_ts date not null default ' ',
        lbr_abd_ts date not null default ' ',
        lbr_ashr_ts date not null default ' ',
        left_pilot_ts date not null default ' ',
        dem_ts date not null default ' ',
        hbr_plan_vst char(7) not null default ' ',
        hbr_plan_pos integer not null default 0,
        usr_name char(8) not null default ' ',
        dt_mod date not null default ' ',
        upd_cntr smallint not null default 0
   ) with page_size=4096;
   EXEC SQL modify qp2tbl8 to btree unique on
        ves_ref, wharf_cd with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl8(
        ves_ref= varchar(0)tab,
        wharf_cd= varchar(0)tab,
        ves_cd= varchar(0)tab,
        sop= varchar(0)tab,
        stevedore= varchar(0)tab,
        ves_ser= varchar(0)tab,
        sop_voy_ib= varchar(0)tab,
        sop_voy_ob= varchar(0)tab,
        rec_cutof_flg= varchar(0)tab,
        ves_berth= varchar(0)tab,
        ves_pilot_est= c0tab,
        ves_pilot_act= c0tab,
        ves_berth_est= c0tab,
        ves_berth_act= c0tab,
        ves_comm_est= c0tab,
        ves_comm_act= c0tab,
        ves_compl_est= c0tab,
        ves_compl_act= c0tab,
        ves_dep_est= c0tab,
        ves_dep_act= c0tab,
        ves_beg_rec= c0tab,
        ves_end_rec= c0tab,
        pacts_purge_ts= c0tab,
        sparcs_purge_ts= c0tab,
        lbr_abd_ts= c0tab,
        lbr_ashr_ts= c0tab,
        left_pilot_ts= c0tab,
        dem_ts= c0tab,
        hbr_plan_vst= varchar(0)tab,
        hbr_plan_pos= c0tab,
        usr_name= varchar(0)tab,
        dt_mod= c0tab,
        upd_cntr= c0nl,
        nl= d1)
   from 'qp2tbl8.data';

   EXEC SQL create table qp2tbl9(
        business_unit_id char(5) not null not default,
        business_unit_name varchar(30) not null default ' ',
        invoice_address_1 varchar(25) not null default ' ',
        invoice_address_2 varchar(25) not null default ' ',
        invoice_address_3 varchar(25) not null default ' ',
        invoice_address_4 varchar(25) not null default ' ',
        invoice_post_code char(4) not null default ' ',
        gst_number char(10) not null default ' ',
        invoice_number integer not null default 0,
        record_create_date date not null default ' ',
        record_modify_date date not null default ' ',
        record_delete_date date not null default ' ',
        record_create_user char(8) not null default ' ',
        record_modify_user char(8) not null default ' ',
        record_delete_user char(8) not null default ' ',
        record_status char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl9 to btree unique on
        business_unit_id with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl9(
        business_unit_id= varchar(0)tab,
        business_unit_name= varchar(0)tab,
        invoice_address_1= varchar(0)tab,
        invoice_address_2= varchar(0)tab,
        invoice_address_3= varchar(0)tab,
        invoice_address_4= varchar(0)tab,
        invoice_post_code= varchar(0)tab,
        gst_number= varchar(0)tab,
        invoice_number= c0tab,
        record_create_date= c0tab,
        record_modify_date= c0tab,
        record_delete_date= c0tab,
        record_create_user= varchar(0)tab,
        record_modify_user= varchar(0)tab,
        record_delete_user= varchar(0)tab,
        record_status= varchar(0)nl,
        nl= d1)
   from 'qp2tbl9.data';

   EXEC SQL create table qp2tbl10(
        ves_ref char(7) not null default ' ',
        posting_number integer not null default 0,
        line_oper char(3) not null default ' ',
        crn_id char(1) not null default ' ',
        report_group smallint not null default 0,
        sub_group smallint not null default 0,
        bundle_type char(1) not null default ' ',
        charge_type char(1) not null default ' ',
        ctr_no char(12) not null default ' ',
        item_code char(10) not null default ' ',
        charge_basis char(8) not null default ' ',
        ctr_iso char(4) not null default ' ',
        ctr_cat char(1) not null default ' ',
        ctr_key integer not null default 0,
        arr_ts date not null default ' ',
        dep_ts date not null default ' ',
        arr_car_ql char(1) not null default ' ',
        dep_car_ql char(1) not null default ' ',
        arr_car_ql_orig char(1) not null default ' ',
        ctr_sts_cd char(1) not null default ' ',
        commod_cd char(4) not null default ' ',
        reef_ctrl_temp f4,
        haz_flg char(1) not null default ' ',
        wt_ctr_gr f4 not null default 0,
        org char(3) not null default ' ',
        destination char(6) not null default ' ',
        importer char(6) not null default ' ',
        exporter char(6) not null default ' ',
        from_loc char(7) not null default ' ',
        to_loc char(7) not null default ' ',
        no_of_units f4 not null default 0,
        ves_no integer not null default 0,
        hp_visit_no integer not null default 0,
        ves_dep_act date not null default ' ',
        vehicle varchar(12) not null default ' ',
        start_date date not null default ' ',
        end_date date not null default ' ',
        cust_id integer not null default 0,
        address_no smallint not null default 0,
        ves_ser char(6) not null default ' ',
        port char(3) not null default ' ',
        fdp char(3) not null default ' ',
        olp char(3) not null default ' ',
        bkg_ref char(14) not null default ' ',
        attch_unit_no char(12) not null default ' ',
        summarise_on_invoice char(1) not null default ' ',
        print_on_report smallint not null default 0,
        event_key char(13) not null not default,
        item_code_key integer not null default 0,
        account_id_key integer not null default 0,
        acct_posting_key integer not null not default,
        acct_posting_key_prev integer not null default 0,
        acct_posting_key_orig integer not null not default,
        link_type char(1) not null default ' ',
        item_type char(1) not null default ' ',
        disc_type char(1) not null default ' ',
        total_on_invoice char(1) not null default ' ',
        bill_number integer not null default 0,
        bill_line smallint not null default 0,
        rate_type char(3) not null default ' ',
        rate decimal(12,4) not null default 0,
        amount decimal(12,4) not null default 0,
        nett_amount decimal(12,4) not null default 0,
        gst decimal(12,4) not null default 0,
        service_date date not null default ' ',
        wharf char(2) not null default ' ',
        department_id char(3) not null default ' ',
        account_id char(6) not null default ' ',
        voy_ib char(6) not null default ' ',
        voy_ob char(6) not null default ' ',
        plant_id char(6) not null default ' ',
        plant_category_id char(6) not null default ' ',
        docket_number integer not null default 0,
        units_1 f4 not null default 0,
        units_2 f4 not null default 0,
        bill_of_lading char(10) not null default ' ',
        record_status char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl10 to btree on
        posting_number with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl10(
        ves_ref= varchar(0)tab,
        posting_number= c0tab,
        line_oper= varchar(0)tab,
        crn_id= varchar(0)tab,
        report_group= c0tab,
        sub_group= c0tab,
        bundle_type= varchar(0)tab,
        charge_type= varchar(0)tab,
        ctr_no= varchar(0)tab,
        item_code= varchar(0)tab,
        charge_basis= varchar(0)tab,
        ctr_iso= varchar(0)tab,
        ctr_cat= varchar(0)tab,
        ctr_key= c0tab,
        arr_ts= c0tab,
        dep_ts= c0tab,
        arr_car_ql= varchar(0)tab,
        dep_car_ql= varchar(0)tab,
        arr_car_ql_orig= varchar(0)tab,
        ctr_sts_cd= varchar(0)tab,
        commod_cd= varchar(0)tab,
        reef_ctrl_temp= c0tab with null(']^NULL^['),
        haz_flg= varchar(0)tab,
        wt_ctr_gr= c0tab,
        org= varchar(0)tab,
        destination= varchar(0)tab,
        importer= varchar(0)tab,
        exporter= varchar(0)tab,
        from_loc= varchar(0)tab,
        to_loc= varchar(0)tab,
        no_of_units= c0tab,
        ves_no= c0tab,
        hp_visit_no= c0tab,
        ves_dep_act= c0tab,
        vehicle= varchar(0)tab,
        start_date= c0tab,
        end_date= c0tab,
        cust_id= c0tab,
        address_no= c0tab,
        ves_ser= varchar(0)tab,
        port= varchar(0)tab,
        fdp= varchar(0)tab,
        olp= varchar(0)tab,
        bkg_ref= varchar(0)tab,
        attch_unit_no= varchar(0)tab,
        summarise_on_invoice= varchar(0)tab,
        print_on_report= c0tab,
        event_key= varchar(0)tab,
        item_code_key= c0tab,
        account_id_key= c0tab,
        acct_posting_key= c0tab,
        acct_posting_key_prev= c0tab,
        acct_posting_key_orig= c0tab,
        link_type= varchar(0)tab,
        item_type= varchar(0)tab,
        disc_type= varchar(0)tab,
        total_on_invoice= varchar(0)tab,
        bill_number= c0tab,
        bill_line= c0tab,
        rate_type= varchar(0)tab,
        rate= varchar(0)tab,
        amount= varchar(0)tab,
        nett_amount= varchar(0)tab,
        gst= varchar(0)tab,
        service_date= c0tab,
        wharf= varchar(0)tab,
        department_id= varchar(0)tab,
        account_id= varchar(0)tab,
        voy_ib= varchar(0)tab,
        voy_ob= varchar(0)tab,
        plant_id= varchar(0)tab,
        plant_category_id= varchar(0)tab,
        docket_number= c0tab,
        units_1= c0tab,
        units_2= c0tab,
        bill_of_lading= varchar(0)tab,
        record_status= varchar(0)nl,
        nl= d1)
   from 'qp2tbl10.data';

   EXEC SQL create table qp2tbl11(
        business_unit_id char(5) not null not default,
        cust_id integer not null not default,
        address_no smallint not null not default,
        cust_address_1 varchar(26) not null default ' ',
        cust_address_2 varchar(26) not null default ' ',
        cust_address_3 varchar(26) not null default ' ',
        cust_address_4 varchar(26) not null default ' ',
        post_code char(4) not null default ' ',
        record_create_date date not null default ' ',
        record_modify_date date not null default ' ',
        record_delete_date date not null default ' ',
        record_create_user char(8) not null default ' ',
        record_modify_user char(8) not null default ' ',
        record_delete_user char(8) not null default ' ',
        record_status char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl11 to btree unique on
        business_unit_id,
        cust_id,
        address_no with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl11(
        business_unit_id= varchar(0)tab,
        cust_id= c0tab,
        address_no= c0tab,
        cust_address_1= varchar(0)tab,
        cust_address_2= varchar(0)tab,
        cust_address_3= varchar(0)tab,
        cust_address_4= varchar(0)tab,
        post_code= varchar(0)tab,
        record_create_date= c0tab,
        record_modify_date= c0tab,
        record_delete_date= c0tab,
        record_create_user= varchar(0)tab,
        record_modify_user= varchar(0)tab,
        record_delete_user= varchar(0)tab,
        record_status= varchar(0)nl,
        nl= d1)
   from 'qp2tbl11.data';

   EXEC SQL create table qp2tbl12(
        charge_basis char(8) not null not default,
        charge_basis_desc varchar(30) not null default ' ',
        charge_unit_1 varchar(20) not null default ' ',
        charge_unit_2 varchar(20) not null default ' ',
        charge_unit_desc varchar(30) not null default ' ',
        psoft_key integer not null default 0,
        record_create_date date not null default ' ',
        record_modify_date date not null default ' ',
        record_delete_date date not null default ' ',
        record_create_user char(8) not null default ' ',
        record_modify_user char(8) not null default ' ',
        record_delete_user char(8) not null default ' ',
        record_status char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl12 to btree unique on
        charge_basis with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl12(
        charge_basis= varchar(0)tab,
        charge_basis_desc= varchar(0)tab,
        charge_unit_1= varchar(0)tab,
        charge_unit_2= varchar(0)tab,
        charge_unit_desc= varchar(0)tab,
        psoft_key= c0tab,
        record_create_date= c0tab,
        record_modify_date= c0tab,
        record_delete_date= c0tab,
        record_create_user= varchar(0)tab,
        record_modify_user= varchar(0)tab,
        record_delete_user= varchar(0)tab,
        record_status= varchar(0)nl,
        nl= d1)
   from 'qp2tbl12.data';

   EXEC SQL create table qp2tbl13(
        business_unit_id char(5) not null not default,
        cust_id integer not null not default,
        cust_short_name varchar(30) not null default ' ',
        cust_name varchar(36) not null default ' ',
        contact_name varchar(30) not null default ' ',
        business_telephone char(12) not null default ' ',
        business_fax char(12) not null default ' ',
        cust_type char(1) not null default ' ',
        summarise_invoice char(1) not null default ' ',
        pacts_customer char(1) not null default ' ',
        bill_delivery_mode char(1) not null default ' ',
        date_account_closed date not null default ' ',
        pymnt_terms_cd char(5) not null default ' ',
        dist_id_ar char(10) not null default ' ',
        gst_charged char(1) not null default ' ',
        auth_comment varchar(500) not null default ' ',
        auth_po_reqd char(1) not null default ' ',
        upd_cntr smallint not null default 0,
        record_create_date date not null default ' ',
        record_modify_date date not null default ' ',
        record_delete_date date not null default ' ',
        record_create_user char(8) not null default ' ',
        record_modify_user char(8) not null default ' ',
        record_delete_user char(8) not null default ' ',
        record_status char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl13 to btree unique on
        business_unit_id,
        cust_id with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl13(
        business_unit_id= varchar(0)tab,
        cust_id= c0tab,
        cust_short_name= varchar(0)tab,
        cust_name= varchar(0)tab,
        contact_name= varchar(0)tab,
        business_telephone= varchar(0)tab,
        business_fax= varchar(0)tab,
        cust_type= varchar(0)tab,
        summarise_invoice= varchar(0)tab,
        pacts_customer= varchar(0)tab,
        bill_delivery_mode= varchar(0)tab,
        date_account_closed= c0tab,
        pymnt_terms_cd= varchar(0)tab,
        dist_id_ar= varchar(0)tab,
        gst_charged= varchar(0)tab,
        auth_comment= varchar(0)tab,
        auth_po_reqd= varchar(0)tab,
        upd_cntr= c0tab,
        record_create_date= c0tab,
        record_modify_date= c0tab,
        record_delete_date= c0tab,
        record_create_user= varchar(0)tab,
        record_modify_user= varchar(0)tab,
        record_delete_user= varchar(0)tab,
        record_status= varchar(0)nl,
        nl= d1)
   from 'qp2tbl13.data';

   EXEC SQL create table qp2tbl14(
        plant_category_id char(6) not null not default,
        plant_category_desc varchar(30) not null default ' ',
        record_create_date date not null default ' ',
        record_modify_date date not null default ' ',
        record_delete_date date not null default ' ',
        record_create_user char(8) not null default ' ',
        record_modify_user char(8) not null default ' ',
        record_delete_user char(8) not null default ' ',
        record_status char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl14 to btree unique on
        plant_category_id with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl14(
        plant_category_id= varchar(0)tab,
        plant_category_desc= varchar(0)tab,
        record_create_date= c0tab,
        record_modify_date= c0tab,
        record_delete_date= c0tab,
        record_create_user= varchar(0)tab,
        record_modify_user= varchar(0)tab,
        record_delete_user= varchar(0)tab,
        record_status= varchar(0)nl,
        nl= d1)
   from 'qp2tbl14.data';

   EXEC SQL create table qp2tbl15(
        posting_type_id char(3) not null not default,
        ves_ref char(7) not null default ' ',
        wharf_cd char(1) not null default ' ',
        posting_number integer not null default 0,
        business_unit_id char(5) not null default ' ',
        profit_centre_id char(6) not null default ' ',
        area_id char(6) not null default ' ',
        manifest_direction char(2) not null default ' ',
        ves_no integer not null default 0,
        hp_visit_no integer not null default 0,
        link_type char(1) not null default ' ',
        control_type char(1) not null default ' ',
        bill_credits_alone char(1) not null default ' ',
        check_run_dt date not null default ' ',
        extract_run_dt date not null default ' ',
        bill_run_dt date not null default ' ',
        deliver_run_dt date not null default ' ',
        archive_run_dt date not null default ' ',
        post_from_date date not null default ' ',
        post_to_date date not null default ' ',
        run_in_progress char(1) not null default ' ',
        record_status char(1) not null default ' ',
        usr_name char(8) not null default ' ',
        dt_mod date not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl15 to btree unique on
        posting_number with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl15(
        posting_type_id= varchar(0)tab,
        ves_ref= varchar(0)tab,
        wharf_cd= varchar(0)tab,
        posting_number= c0tab,
        business_unit_id= varchar(0)tab,
        profit_centre_id= varchar(0)tab,
        area_id= varchar(0)tab,
        manifest_direction= varchar(0)tab,
        ves_no= c0tab,
        hp_visit_no= c0tab,
        link_type= varchar(0)tab,
        control_type= varchar(0)tab,
        bill_credits_alone= varchar(0)tab,
        check_run_dt= c0tab,
        extract_run_dt= c0tab,
        bill_run_dt= c0tab,
        deliver_run_dt= c0tab,
        archive_run_dt= c0tab,
        post_from_date= c0tab,
        post_to_date= c0tab,
        run_in_progress= varchar(0)tab,
        record_status= varchar(0)tab,
        usr_name= varchar(0)tab,
        dt_mod= c0nl,
        nl= d1)
   from 'qp2tbl15.data';

   EXEC SQL create table qp2tbl16(
        bill_number integer not null not default,
        posting_number integer not null not default,
        invoice_number integer not null default 0,
        cust_id integer not null default 0,
        address_no smallint not null default 0,
        ves_no integer not null default 0,
        amount decimal(10,2) not null default 0,
        gst decimal(10,2) not null default 0,
        bill_run_dt date not null default ' ',
        report_run_dt date not null default ' ',
        deliver_run_dt date not null default ' ',
        trans_date date not null default ' ',
        posting_run_dt date not null default ' ',
        run_in_progress char(1) not null default ' ',
        record_status char(1) not null default ' ',
        usr_name char(8) not null default ' ',
        dt_mod date not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl16 to hash unique on
        bill_number with fillfactor = 50,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl16(
        bill_number= c0tab,
        posting_number= c0tab,
        invoice_number= c0tab,
        cust_id= c0tab,
        address_no= c0tab,
        ves_no= c0tab,
        amount= varchar(0)tab,
        gst= varchar(0)tab,
        bill_run_dt= c0tab,
        report_run_dt= c0tab,
        deliver_run_dt= c0tab,
        trans_date= c0tab,
        posting_run_dt= c0tab,
        run_in_progress= varchar(0)tab,
        record_status= varchar(0)tab,
        usr_name= varchar(0)tab,
        dt_mod= c0nl,
        nl= d1)
   from 'qp2tbl16.data';

   EXEC SQL create table qp2tbl17(
        plant_id char(6) not null not default,
        plant_desc varchar(30) not null default ' ',
        plant_category_id char(6) not null default ' ',
        record_create_date date not null default ' ',
        record_modify_date date not null default ' ',
        record_delete_date date not null default ' ',
        record_create_user char(8) not null default ' ',
        record_modify_user char(8) not null default ' ',
        record_delete_user char(8) not null default ' ',
        record_status char(1) not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl17 to btree unique on
        plant_id with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl17(
        plant_id= varchar(0)tab,
        plant_desc= varchar(0)tab,
        plant_category_id= varchar(0)tab,
        record_create_date= c0tab,
        record_modify_date= c0tab,
        record_delete_date= c0tab,
        record_create_user= varchar(0)tab,
        record_modify_user= varchar(0)tab,
        record_delete_user= varchar(0)tab,
        record_status= varchar(0)nl,
        nl= d1)
   from 'qp2tbl17.data';

   EXEC SQL create table qp2tbl18(
        ctr_key integer not null not default,
        ctr_no char(12) not null not default,
        ctr_no_rev char(12) not null not default,
        ctr_iso char(4) not null default ' ',
        ctr_lop_sn_grp char(1) not null default ' ',
        ctr_tynes char(1) not null default ' ',
        ctr_cd_ql char(1) not null default ' ',
        ctr_len smallint not null default 0,
        ctr_ht f4 not null default 0,
        wt_ctr_gr f4 not null default 0,
        wt_tare f4 not null default 0,
        ctr_cat char(1) not null default ' ',
        ctr_sts_cd char(1) not null default ' ',
        ra_no char(12) not null default ' ',
        commod_cd char(4) not null default ' ',
        line_oper char(3) not null default ' ',
        agt_cd char(6) not null default ' ',
        bkg_ref char(14) not null default ' ',
        ves_ref char(7) not null default ' ',
        arr_car char(7) not null default ' ',
        arr_car_ql char(1) not null default ' ',
        dep_car char(7) not null default ' ',
        dep_car_ql char(1) not null default ' ',
        ctr_seal_no varchar(12) not null default ' ',
        cus_clr varchar(12) not null default ' ',
        ag_clr varchar(12) not null default ' ',
        forest_clr varchar(12) not null default ' ',
        lop_clr varchar(12) not null default ' ',
        org char(3) not null default ' ',
        destination char(6) not null default ' ',
        importer char(6) not null default ' ',
        exporter char(6) not null default ' ',
        lp char(3) not null default ' ',
        dp char(3) not null default ' ',
        fdp char(3) not null default ' ',
        deliv_inst char(6) not null default ' ',
        ctr_attch char(1) not null default ' ',
        ctr_dam_flg char(1) not null default ' ',
        haz_flg char(1) not null default ' ',
        haz_cl_min char(4) not null default ' ',
        ctr_key_orig integer not null default 0,
        ctr_key_prev integer not null default 0,
        ctr_key_master integer not null default 0,
        reef_tp char(1) not null default ' ',
        reef_cou char(1) not null default ' ',
        reef_ctrl_temp f4,
        reef_rec_temp f4,
        stop_flg char(1) not null default ' ',
        od_flg char(1) not null default ' ',
        od_top smallint not null default 0,
        od_right smallint not null default 0,
        od_left smallint not null default 0,
        od_front smallint not null default 0,
        od_back smallint not null default 0,
        note_flg char(1) not null default ' ',
        fan_power char(1) not null default ' ',
        ctr_wsh_sts char(1) not null default ' ',
        ctr_pretrip_sts char(1) not null default ' ',
        plan_flg char(1) not null default ' ',
        wharf_cd char(1) not null default ' ',
        loc varchar(12) not null default ' ',
        loc_ql char(1) not null default ' ',
        loc_blk char(3) not null default ' ',
        loc_u_x char(3) not null default ' ',
        loc_u_y char(2) not null default ' ',
        loc_u_z char(2) not null default ' ',
        arr_ts date not null default ' ',
        dep_ts date not null default ' ',
        ctr_pos_cntr smallint not null default 0,
        usr_name char(8) not null default ' ',
        dt_mod date not null default ' ',
        upd_cntr smallint not null default 0
   ) with page_size=4096;
   EXEC SQL modify qp2tbl18 to btree unique on
        ctr_key with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80,
        extend = 16,
        allocation = 4;
   EXEC SQL copy table qp2tbl18(
        ctr_key= c0tab,
        ctr_no= varchar(0)tab,
        ctr_no_rev= varchar(0)tab,
        ctr_iso= varchar(0)tab,
        ctr_lop_sn_grp= varchar(0)tab,
        ctr_tynes= varchar(0)tab,
        ctr_cd_ql= varchar(0)tab,
        ctr_len= c0tab,
        ctr_ht= c0tab,
        wt_ctr_gr= c0tab,
        wt_tare= c0tab,
        ctr_cat= varchar(0)tab,
        ctr_sts_cd= varchar(0)tab,
        ra_no= varchar(0)tab,
        commod_cd= varchar(0)tab,
        line_oper= varchar(0)tab,
        agt_cd= varchar(0)tab,
        bkg_ref= varchar(0)tab,
        ves_ref= varchar(0)tab,
        arr_car= varchar(0)tab,
        arr_car_ql= varchar(0)tab,
        dep_car= varchar(0)tab,
        dep_car_ql= varchar(0)tab,
        ctr_seal_no= varchar(0)tab,
        cus_clr= varchar(0)tab,
        ag_clr= varchar(0)tab,
        forest_clr= varchar(0)tab,
        lop_clr= varchar(0)tab,
        org= varchar(0)tab,
        destination= varchar(0)tab,
        importer= varchar(0)tab,
        exporter= varchar(0)tab,
        lp= varchar(0)tab,
        dp= varchar(0)tab,
        fdp= varchar(0)tab,
        deliv_inst= varchar(0)tab,
        ctr_attch= varchar(0)tab,
        ctr_dam_flg= varchar(0)tab,
        haz_flg= varchar(0)tab,
        haz_cl_min= varchar(0)tab,
        ctr_key_orig= c0tab,
        ctr_key_prev= c0tab,
        ctr_key_master= c0tab,
        reef_tp= varchar(0)tab,
        reef_cou= varchar(0)tab,
        reef_ctrl_temp= c0tab with null(']^NULL^['),
        reef_rec_temp= c0tab with null(']^NULL^['),
        stop_flg= varchar(0)tab,
        od_flg= varchar(0)tab,
        od_top= c0tab,
        od_right= c0tab,
        od_left= c0tab,
        od_front= c0tab,
        od_back= c0tab,
        note_flg= varchar(0)tab,
        fan_power= varchar(0)tab,
        ctr_wsh_sts= varchar(0)tab,
        ctr_pretrip_sts= varchar(0)tab,
        plan_flg= varchar(0)tab,
        wharf_cd= varchar(0)tab,
        loc= varchar(0)tab,
        loc_ql= varchar(0)tab,
        loc_blk= varchar(0)tab,
        loc_u_x= varchar(0)tab,
        loc_u_y= varchar(0)tab,
        loc_u_z= varchar(0)tab,
        arr_ts= c0tab,
        dep_ts= c0tab,
        ctr_pos_cntr= c0tab,
        usr_name= varchar(0)tab,
        dt_mod= c0tab,
        upd_cntr= c0nl,
        nl= d1)
   from 'qp2tbl18.data';

   EXEC SQL create table qp2tbl19(
        note_key integer not null not default,
        posting_number integer not null default 0,
        acct_posting_key integer not null default 0,
        event_key char(13) not null default ' ',
        bill_number integer not null default 0,
        bill_line smallint not null default 0,
        note_locn char(1) not null default ' ',
        note_txt varchar(1000) not null default ' ',
        usr_name char(8) not null default ' ',
        dt_mod date not null default ' '
   ) with page_size=4096;
   EXEC SQL modify qp2tbl19 to btree unique on
        note_key with nonleaffill = 80,
        leaffill = 70,
        fillfactor = 100,
        extend = 16,
        allocation = 4,
        compression = (nokey, data);
   EXEC SQL copy table qp2tbl19(
        note_key= c0tab,
        posting_number= c0tab,
        acct_posting_key= c0tab,
        event_key= varchar(0)tab,
        bill_number= c0tab,
        bill_line= c0tab,
        note_locn= varchar(0)tab,
        note_txt= varchar(0)tab,
        usr_name= varchar(0)tab,
        dt_mod= c0nl,
        nl= d1)
   from 'qp2tbl19.data';

   printf("Creating indexes...\n");
   EXEC SQL create index container_ves_ref_idx on qp2tbl18 (
        ves_ref)
   with structure = btree,
        nocompression,
        key = (ves_ref),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create unique index ports_idx on qp2tbl4 (
        un_ctry_cd,
        un_loc_cd)
   with structure = btree,
        nocompression,
        key = (un_ctry_cd, un_loc_cd),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index acct_posting_event_key_idx on qp2tbl10 (
        event_key)
   with structure = btree,
        nocompression,
        key = (event_key),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index psoft_cust_idx on qp2tbl13 (
        cust_id)
   with structure = btree,
        nocompression,
        key = (cust_id),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index acct_posting_bill_idx on qp2tbl10 (
        bill_number)
   with structure = btree,
        nocompression,
        key = (bill_number),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index bill_control_vessel_idx1 on qp2tbl16 (
        ves_no)
   with structure = hash,
        nocompression,
        key = (ves_no),
        minpages = 2,
        fillfactor = 50;
   EXEC SQL create unique index bill_notes_posting_number_idx on qp2tbl19 (
        posting_number,
        note_key)
   with structure = btree,
        nocompression,
        key = (posting_number, note_key),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index bill_control_invoice_idx1 on qp2tbl16 (
        invoice_number)
   with structure = hash,
        nocompression,
        key = (invoice_number),
        minpages = 2,
        fillfactor = 50;
   EXEC SQL create unique index invoice_item_code_idx on qp2tbl3 (
        item_code,
        record_eff_date)
   with structure = btree,
        nocompression,
        key = (item_code, record_eff_date),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index apc_pacts_idx on qp2tbl15 (
        posting_type_id,
        ves_ref,
        wharf_cd)
   with structure = btree,
        nocompression,
        key = (posting_type_id, ves_ref, wharf_cd),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create unique index bill_notes_event_key_idx on qp2tbl19 (
        event_key,
        note_key)
   with structure = btree,
        nocompression,
        key = (event_key, note_key),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index plant_category_idx on qp2tbl17 (
        plant_category_id)
   with structure = btree,
        nocompression,
        key = (plant_category_id),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create unique index acct_posting_ap_key_idx on qp2tbl10 (
        acct_posting_key)
   with structure = btree,
        nocompression,
        key = (acct_posting_key),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index ctr_cat_lop on qp2tbl18 (
        ctr_cat,
        line_oper)
   with structure = btree,
        nocompression,
        key = (ctr_cat, line_oper),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index profit_centre_idx on qp2tbl7 (
        business_unit_id)
   with structure = btree,
        nocompression,
        key = (business_unit_id),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index ctr_loc_xyz_idx on qp2tbl18 (
        wharf_cd,
        loc_u_x,
        loc_u_y,
        loc_u_z)
   with structure = btree,
        nocompression,
        key = (wharf_cd, loc_u_x, loc_u_y, loc_u_z),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index container_ctr_key_orig on qp2tbl18 (
        ctr_key_orig,
        ctr_key)
   with structure = btree,
        nocompression,
        key = (ctr_key_orig, ctr_key),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create unique index container_ctr_no_idx on qp2tbl18 (
        ctr_no)
   with structure = btree,
        nocompression,
        key = (ctr_no),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index bill_control_posting_idx on qp2tbl16 (
        posting_number)
   with structure = hash,
        nocompression,
        key = (posting_number),
        minpages = 2,
        fillfactor = 50;
   EXEC SQL create index acct_posting_ap_key_orig_idx on qp2tbl10 (
        acct_posting_key_orig,
        acct_posting_key)
   with structure = btree,
        nocompression,
        key = (acct_posting_key_orig, acct_posting_key),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index apc_ves_no_idx on qp2tbl15 (
        ves_no)
   with structure = btree,
        nocompression,
        key = (ves_no),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create unique index bill_notes_posting_key_idx on qp2tbl19 (
        acct_posting_key,
        note_key)
   with structure = btree,
        nocompression,
        key = (acct_posting_key, note_key),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index container_ra_no on qp2tbl18 (
        ra_no,
        ctr_key)
   with structure = btree,
        nocompression,
        key = (ra_no, ctr_key),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create unique index ves_sched_hbr_plan_vst on qp2tbl8 (
        hbr_plan_vst,
        wharf_cd)
   with structure = btree,
        nocompression,
        key = (hbr_plan_vst, wharf_cd),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create unique index bill_notes_bill_number_idx on qp2tbl19 (
        bill_number,
        note_key)
   with structure = btree,
        nocompression,
        key = (bill_number, note_key),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create unique index container_ctr_no_rev_idx on qp2tbl18 (
        ctr_no_rev)
   with structure = btree,
        nocompression,
        key = (ctr_no_rev),
        nonleaffill = 80,
        leaffill = 70,
        fillfactor = 80;
   EXEC SQL create index bill_control_cust_idx1 on qp2tbl16 (
        cust_id)
   with structure = hash,
        nocompression,
        key = (cust_id),
        minpages = 2,
        fillfactor = 50;

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
