/*
**      efa33_utf8.sc
**
**      Test program for QRYPROC test qp099.sep - default charset
**
**      history:
**      17-Nov-2008 (vande02)
**			Creating this program outside of the SEP test so two
**			versions can be referenced in the test - one for
**			default charset and the other for UTF8
**                      
**                      The maximum varchar column length for a default
**                      charset is 16,000.
*/
exec sql include sqlca;

exec sql whenever sqlerror call sqlprint;
exec sql whenever not found call sqlprint;
exec sql whenever sqlwarning call sqlprint;
exec sql whenever sqlmessage call sqlprint;

main(argc, argv)
exec sql begin declare section;
  int  argc;
  char **argv;
exec sql end declare section;
{
int testno;
	exec sql connect :argv[1];
	testno = atoi(argv[2]);
	switch (testno)
	{ case 1: efa33_1();
	  break;
	  case 2: efa33_2();
	  break;
	  case 3: efa33_3();
	  break;
	  case 4: efa33_4();
	  break;
	  default:
	  break; 
	}
	exec sql disconnect;
}  /* main */

efa33_1()
{
exec sql begin declare section;
  short  i1, i2;
  int    i4;
  float  f4, m, deci;
  double f8;
  char   c10[11], char10[11], vchar10[11], varchar10[11], d[26];
exec sql end declare section;

        /**********/
	/* test 1 */
        /**********/

	printf ("\ntest 1:  passing byref parameter of all datatypes\n\n");
	exec sql drop procedure efa33_1;

	exec sql create procedure efa33_1 (
		i1		i1		not null with default,
		i2		i2		not null with default,
		i4		i4		not null with default,
		f4		f4		not null with default,
		f8		f8		not null with default,
		c10		c10		not null with default,
		char10		char(10)	not null with default,
		vchar10		text(10)	not null with default,
		varchar10	varchar(10)	not null with default,
		d		date		not null with default,
		m		money		not null with default,
		deci		decimal(6,4) 	not null with default) as
	begin
		message 'return byref parameter of all datatypes';
		i1		=  2;
		i2		=  20;
		i4		=  200;
		f4		=  .2;
		f8		=  .02;
		c10		=  'b';
		char10		=  'bb';
		vchar10		=  'bbb';
		varchar10	=  'bbbb';
		d		=  '02-jan-1993';
		m		=  2.45;
		deci		=  22.2222;
	end;

        i1 =  1;
	i2 =  10;
	i4 =  100;
	f4 =  .1;
	f8 =  .01;
        strcpy (c10, "a");
	strcpy (char10, "aa");
	strcpy (vchar10, "aaa");
	strcpy (varchar10, "aaaa");
	strcpy (d, "01-jan-1993");
        m = 100.00;
        deci = 11.1111;

	printf ("passing i1         = %d\n", i1);
        printf ("passing i2         = %d\n", i2);
        printf ("passing i4         = %d\n", i4);
        printf ("passing f4         = %f\n", f4);
        printf ("passing f8         = %f\n", f8);
        printf ("passing c10        = %s\n", c10);
        printf ("passing char10     = %s\n", char10);
        printf ("passing vchar10    = %s\n", vchar10);
	printf ("passing varchar10  = %s\n", varchar10);
	printf ("passing d          = %s\n", d);
	printf ("passing m          = %f\n", m);
	printf ("passing deci       = %f\n", deci);

	exec sql execute procedure efa33_1 (i1        = byref(:i1),
					  i2        = byref(:i2),
					  i4        = byref(:i4),
					  f4        = byref(:f4),
					  f8        = byref(:f8),
					  c10       = byref(:c10),
					  char10    = byref(:char10),
					  vchar10   = byref(:vchar10),
					  varchar10 = byref(:varchar10),
					  d         = byref(:d),
					  m         = byref(:m),
					  deci      = byref(:deci));

	printf ("returned i1        = %d\n", i1);
        printf ("returned i2        = %d\n", i2);
        printf ("returned i4        = %d\n", i4);
        printf ("returned f4        = %f\n", f4);
        printf ("returned f8        = %f\n", f8);
        printf ("returned c10       = %s\n", c10);
        printf ("returned char10    = %s\n", char10);
        printf ("returned vchar10   = %s\n", vchar10);
	printf ("returned varchar10 = %s\n", varchar10);
	printf ("returned d         = %s\n", d);
	printf ("returned m         = %f\n", m);
	printf ("returned deci      = %f\n", deci);

	exec sql commit;

} /* efa33_1() */

efa33_2()
{
exec sql begin declare section;
  char lchar[16008];
exec sql end declare section;

        /************/
	/* test 2.1 */
        /************/

	printf ("\ntest 2.1:  passing byref parameter max data length\n\n");

	exec sql drop procedure efa33_2_1;

	exec sql create procedure efa33_2_1 (bob varchar(16000) not null) as

        declare mesg_buf varchar(16000) not null;
	begin
		mesg_buf = :bob;
		message mesg_buf;
		bob = 'This is a test for table xyz1 in the database procedure.';
		mesg_buf = :bob;
		message mesg_buf;
	end;

	exec sql drop table xyz1;
	exec sql create table xyz1 (bob varchar(16000));
	exec sql insert into xyz1 (bob) values ('This is a test for table xyz1.');
	exec sql select bob into :lchar from xyz1;

        printf ("passing lchar  = %s\n", lchar);
	exec sql execute procedure efa33_2_1 (bob = byref(:lchar));
        printf ("returned lchar = %s\n", lchar);

	exec sql commit;

        /************/
	/* test 2.2 */
        /************/

	printf ("\n------------------------------------------------------\n\n");
	printf ("\ntest 2.2:  passing byref parameter max+1 data length\n\n");

	exec sql drop procedure efa33_2_2;

	exec sql create procedure efa33_2_2 (bob varchar(16001) not null) as

        declare mesg_buf varchar(16001) not null;
	begin
		mesg_buf = :bob;
		message mesg_buf;
		bob = 'This is a test for table xyz2 in the database procedure.';
		mesg_buf = :bob;
		message mesg_buf;
	end;

	exec sql drop table xyz2;
	exec sql create table xyz2 (bob varchar(16001));
	exec sql insert into xyz2 (bob) values ('This is a test for table xyz2.');
	exec sql select bob into :lchar from xyz2;

        printf ("passing lchar  = %s\n", lchar);
	exec sql execute procedure efa33_2_2 (bob = byref(:lchar));
        printf ("returned lchar = %s\n", lchar);

	exec sql commit;

} /* efa33_2() */

efa33_3()
{
exec sql begin declare section;
  short  i1, i2;
  int    i4 ;
  float  f4, f4_2, deci;
  double f8;
  char   c1[11], c10[11];
exec sql end declare section;

        /************/
	/* test 3.1 */
        /************/

	printf ("\ntest 3.1:  passing byref parameter of various datatypes to integer type\n\n");

	exec sql drop procedure efa33_3_1;

	exec sql create procedure efa33_3_1 (i1	i1 not null with default) as
	begin
		i1 = 5;
	end;

	i1 = 2;
	printf ("passing integer = %d\n",i1);
	exec sql execute procedure efa33_3_1 (i1 = byref(:i1));
	printf ("returned        = %d\n",i1);

	i2 = 20;
	printf ("passing integer = %d\n",i2);
	exec sql execute procedure efa33_3_1 (i1 = byref(:i2));
	printf ("returned        = %d\n",i2);

	i4 = 127;
	printf ("passing integer = %d\n",i4);
	exec sql execute procedure efa33_3_1 (i1 = byref(:i4));
	printf ("returned        = %d\n",i4);

	i4 = 200;
	printf ("passing integer = %d\n",i4);
	exec sql execute procedure efa33_3_1 (i1 = byref(:i4));
	printf ("returned        = %d\n",i4);

	f4 = 1.1;
	printf ("passing float   = %f\n",f4);
	exec sql execute procedure efa33_3_1 (i1 = byref(:f4));
	printf ("returned        = %f\n",f4);

	f8 = 1.01;
	printf ("passing float   = %f\n",f8);
	exec sql execute procedure efa33_3_1 (i1 = byref(:f8));
	printf ("returned        = %f\n",f8);

	strcpy (c1, "a");
	printf ("passing char    = %s\n",c1);
	exec sql execute procedure efa33_3_1 (i1 = byref(:c1));
	printf ("returned        = %s\n",c1);

	strcpy (c10, "aa");
	printf ("passing char    = %s\n",c10);
	exec sql execute procedure efa33_3_1 (i1 = byref(:c10));
	printf ("returned        = %s\n",c10);

	exec sql commit;

        /************/
	/* test 3.2 */
        /************/

	printf ("\n------------------------------------------------------\n\n");
	printf ("\ntest 3.2:  passing byref parameter of various datatypes to character type\n\n");

	exec sql drop procedure efa33_3_2;

	exec sql create procedure efa33_3_2 (c1	c(1) not null with default) as
	begin
		c1 = 'z';
	end;

	strcpy (c1, "a");
	printf ("passing char    = %s\n",c1);
	exec sql execute procedure efa33_3_2 (c1 = byref(:c1));
	printf ("returned        = %s\n",c1);

	strcpy (c10, "aa");
	printf ("passing too many char = %s\n",c10);
	exec sql execute procedure efa33_3_2 (c1 = byref(:c10));
	printf ("returned              = %s\n",c10);

	i1 = 2;
	printf ("passing integer = %d\n",i1);
	exec sql execute procedure efa33_3_2 (c1 = byref(:i1));
	printf ("returned        = %d\n",i1);

	f4 = 1.1;
	printf ("passing float   = %f\n",f4);
	exec sql execute procedure efa33_3_2 (c1 = byref(:f4));
	printf ("returned        = %f\n",f4);

	exec sql commit;

        /************/
	/* test 3.3 */
        /************/

	printf ("\n------------------------------------------------------\n\n");
	printf ("\ntest 3.3:  passing byref parameter of various datatypes to float type\n\n");

	exec sql drop procedure efa33_3_3;

	exec sql create procedure efa33_3_3 (f4	f4 not null with default) as
	begin
		f4 = 4.2;
	end;

	f4 = 1.1;
	printf ("passing float   = %f\n",f4);
	exec sql execute procedure efa33_3_3 (f4 = byref(:f4));
	printf ("returned        = %f\n",f4);

	f8 = 1.01;
	printf ("passing float   = %f\n",f8);
	exec sql execute procedure efa33_3_3 (f4 = byref(:f8));
	printf ("returned        = %f\n",f8);

	strcpy (c1, "a");
	printf ("passing char    = %s\n",c1);
	exec sql execute procedure efa33_3_3 (f4 = byref(:c1));
	printf ("returned        = %s\n",c1);

	i1 = 2;
	printf ("passing integer = %d\n",i1);
	exec sql execute procedure efa33_3_3 (f4 = byref(:i1));
	printf ("returned        = %d\n",i1);

	i4 = 200;
	printf ("passing integer = %d\n",i4);
	exec sql execute procedure efa33_3_3 (f4 = byref(:i4));
	printf ("returned        = %d\n",i4);

	exec sql commit;

        /************/
	/* test 3.4 */
        /************/

	printf ("\n------------------------------------------------------\n\n");
	printf ("\ntest 3.4:  passing byref parameter of various datatypes length\n\n");

	exec sql drop procedure efa33_3_4;

	exec sql create procedure efa33_3_4 (
		f8	f8 not null with default,
                f8_2    f8 not null with default,
		i4	i4 not null with default,
		i2	i2 not null with default) as
	begin
		f8 = 2.02;
                f8_2 = .02;
		i4 = 500;
		i2 = 50;
	end;

	f4 = 1.1;
        f4_2 = 9.9;
	i2 = 20;
	i4 = 200;
	printf ("passing f4 -> f8 = %f %f, i2 -> i4 = %d, i4 -> i2 = %d\n",f4,f4_2,i2,i4);
	exec sql execute procedure efa33_3_4 (f8 = byref(:f4),
                                            f8_2 = byref(:f4_2),
					    i4 = byref(:i2),
					    i2 = byref(:i4));
	printf ("returned         = %f %f,            %d,            %d\n",f4,f4_2,i2,i4);

	exec sql commit;

        /************/
	/* test 3.5 */
        /************/

	printf ("\n------------------------------------------------------\n\n");
	printf ("\ntest 3.5:  passing byref parameter of various datatypes to decimal type\n\n");

	exec sql drop procedure efa33_3_5;

	exec sql create procedure efa33_3_5 (deci	decimal(5,3) not null with default) as
	declare mesg_buf  varchar(81) not null;
	begin
		mesg_buf = varchar(:deci);
		message mesg_buf;
		deci = 50.789;
		mesg_buf = varchar(:deci);
		message mesg_buf;
	end;

	f4 = 1.1;
	printf ("passing float   = %3.3f\n",f4);
	exec sql execute procedure efa33_3_5 (deci = byref(:f4));
	printf ("returned        = %3.3f\n",f4);

	f4 = .6;
	printf ("passing float   = %3.3f\n",f4);
	exec sql execute procedure efa33_3_5 (deci = byref(:f4));
	printf ("returned        = %3.3f\n",f4);

	f8 = 1.01;
	printf ("passing float   = %f\n",f8);
	exec sql execute procedure efa33_3_5 (deci = byref(:f8));
	printf ("returned        = %f\n",f8);

	strcpy (c1, "a");
	printf ("passing char    = %s\n",c1);
	exec sql execute procedure efa33_3_5 (deci = byref(:c1));
	printf ("returned        = %s\n",c1);

	i1 = 2;
	printf ("passing integer = %d\n",i1);
	exec sql execute procedure efa33_3_5 (deci = byref(:i1));
	printf ("returned        = %d\n",i1);

	i4 = 200;
	printf ("passing integer = %d\n",i4);
	exec sql execute procedure efa33_3_5 (deci = byref(:i4));
	printf ("returned        = %d\n",i4);

	exec sql commit;

} /* efa33_3() */

efa33_4()
{
exec sql begin declare section;
  int   i;
  float f4;
  char  c10[11];
exec sql end declare section;

        /************/
	/* test 4.1 */
        /************/

	printf ("\ntest 4.1:  passing byref parameter from values to nullable datatypes\n\n");
	exec sql drop procedure efa33_4;

	exec sql create procedure efa33_4 (
		i		integer 	with null,
		f4		float		with null,
		c10		c10 		with null) as
	begin
		i = 0;
		f4 = 0;
		c10 = '';
	end;

	i = 100;
	f4 = 20843.324;
        strcpy (c10, "howdy!");

	printf ("passing i       = %d\n", i);
	printf ("passing f4      = %5.3f\n", f4);
	printf ("passing c10     = %s\n", c10);

	exec sql execute procedure efa33_4 (i  = byref(:i),
					    f4  = byref(:f4),
					    c10 = byref(:c10));

	printf ("returned i      = %d\n", i);
	printf ("returned f4     = %5.3f\n", f4);
	printf ("returned c10    = %s\n", c10);

        /************/
	/* test 4.2 */
        /************/

	printf ("\n------------------------------------------------------\n\n");
	printf ("\ntest 4.2:  passing byref parameter nullable to nullable datatypes\n\n");

	i = 0;
	f4 = 0;
        strcpy (c10, "");

	printf ("passing i       = %d\n", i);
	printf ("passing f4      = %f\n", f4);
	printf ("passing c10     = %s\n", c10);

	exec sql execute procedure efa33_4 (i  = byref(:i),
					    f4  = byref(:f4),
					    c10 = byref(:c10));

	printf ("returned i      = %d\n", i);
	printf ("returned f4     = %f\n", f4);
	printf ("returned c10    = %s\n", c10);
	exec sql commit;

} /* efa33_4() */
