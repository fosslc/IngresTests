/*
**	driver.sc
**
**	compile:
**		esqlc driver.sc
**		sepcc driver.c
**
**	history:
**
**		may 90 sgp
**			- added dr_sqlca(), dr_inqing()
**
**		jan 90 sgp
**			- added -m
**			- merged, cleaned and commented code
**			- anointed version 1.0
**
**		dec 89 sgp
**			- added -n
**
**		nov 89 sgp
**			- created
**
**	contents:
**		signal_trap()
**		signal_setup()
**		main()
**		driver_list_search()
**		sched_all_tests()
**		print_all_tests()
**		end_clean()
**		parse_args()
**		print_syntax()
**		dump_that_main_fella()
**		dr_sqlca()
**		dr_inqing()
**
**	future:
**		1.1
**		- need a more convincing end_clean().
**		- add flag to do forms style output.
**
**		2.0
**		- listen to testinfo database.
**
**		3.0
**		- update testinfo database.
*/

/* name */
static	char	Source[] = {"source: driver.sc (Fri May 17 00:00:00 1991)"};

/* includes */
#include <signal.h>
#include <stdio.h>

/* driver info */
extern	char	*driver_name;
extern	char	*driver_description;

extern struct {
	char	*name;
	int	(*func)();
} driver_list[];

/* ingres includes */
exec sql include sqlca;

/* ingres whenevers */
exec sql whenever sqlerror call sqlprint;
exec sql whenever not found call sqlprint;
exec sql whenever sqlwarning call sqlprint;

/* true and false */
#define TRUE	1
#define FALSE	0

/* not found for searches */
#define NOTFOUND	-1

/* flags */
short	got_dbname		= FALSE; /* user has provided db name */
short	got_many_dbnames	= FALSE; /* user has provided many db names */
short	got_testname		= FALSE; /* user has provided test name */
short	do_open_db		= FALSE; /* driver opens the database */
short	test_open_db		= FALSE; /* test opens the database */
short	do_upd_testinfo		= FALSE; /* update testinfo database */
short	do_all_tests		= FALSE; /* run all the test in driver suite */
short	do_explain		= FALSE; /* explain tests and exit */
short	do_long_explain		= FALSE; /* explain tests long form & exit */
short	dump_main		= FALSE; /* dump main() code for user & exit */

/* number of runs */
int	number_of_runs		= 1;
int	curr_number_of_runs;


exec sql begin declare section;

/* database name */
char    db_name_str[32];
char    *db_name_ptr	= db_name_str;

exec sql end declare section;

/* describe structure for lists of tests to be executed */

typedef struct _TEST_LIST_ROW {
	char    *name;			/* name of test */
	int     (*func)();		/* function pointer to test */
	char    db_name_str[32];	/* name of db test is run against */
	struct  _TEST_LIST_ROW  *ptr;	/* pointer to next test in the list */
} TEST_LIST_ROW;

TEST_LIST_ROW   *Rtest_list     = NULL;	/* root of test list */
TEST_LIST_ROW   *test_list      = NULL;	/* current node in test list */

/*****************/
/* signal_trap() */
/*****************/

signal_trap(sig)
{
	/* halt ingres cleanly */
	end_clean();
	/* sgp: what exactly should be done here?  can we shut down */
	/* all known sessions easily?                               */

	/* if it ain't obvious, report it */
	if ( ( sig != SIGINT ) && ( sig != SIGQUIT ) )
	{
		fprintf(stderr,"signal_trap(): signal %d\n",sig);
	}

	exit(0);

} /* signal_trap() */

/******************/
/* signal_setup() */
/******************/

signal_setup()
{
	int	i;

	/* set up signal trapping */
	for ( i = SIGHUP; i <= SIGSEGV; i++ )
	{
		signal(i,(void *)signal_trap);
	}

} /* signal_setup() */

/**********/
/* main() */
/**********/

main(argc, argv)
int	argc;
char	*argv[];
{
	/* run off at mouth */
	printf("TEST DRIVER\n");
	printf("%s\n",Source);
	printf("%s\n",driver_description);
	printf("\n");

	/* set up signal trapping */
	signal_setup();

	/* read user arguments */
	parse_args(argc,argv);

	/* well, you've done the short explain... */
	if ( do_explain )
	{
		exit(0);
	}

	/* do the long explain */
	if ( do_long_explain )
	{
		print_all_tests();
		exit(0);
	}

	/* if all tests have been requested, schedule them */
	if ( do_all_tests )
	{
		sched_all_tests();
	}

	/* get information from testinfo database */
	if ( ( !got_dbname && !test_open_db ) || !got_testname )
	{
		fprintf(stderr,"driver: can't use testinfo database yet,\n");
		fprintf(stderr,"        please specify test and db names.\n");
		print_syntax();
		exit(0);

		/* connect w/ testinfo */

		/* get the tests if necessary */
		if ( !got_testname )
		{
		}

		/* get the database name if necessary */
		if ( !got_dbname && !test_open_db )
		{
		}

		/* disconnect w/ testinfo */
	}

	/* dump main and exit if appropriate */
	if ( dump_main )
	{
		dump_that_main_fella();
		exit(0);
	}

	/* loop thru test list to print tests scheduled */
	printf("\nXX-----------------XX\n");
	printf("XX tests scheduled XX\n");
	printf("XX-----------------XX\n");
	for (
		test_list = Rtest_list;
		test_list != NULL;
		test_list = test_list->ptr
	)
	{
		printf("   %s\n",test_list->name);
	}
	printf("XX-----------------XX\n\n");

	/* for as many times as the tests are supposed to be repeated... */
	for( curr_number_of_runs = 1; 
	     curr_number_of_runs <= number_of_runs; 
	     curr_number_of_runs++
	)
	{
		/* announce run number if it is greater than one */
		if ( number_of_runs > 1 )
		{
			printf("\n===< test run number %d of %d >===\n\n",
				curr_number_of_runs,
				number_of_runs
			);
		}

		/* if driver opens single db for all tests, do so */
		if ( do_open_db && !got_many_dbnames && !test_open_db )
		{
			/* connect w/ database */
			printf("connecting with database...\n");
			exec sql connect :db_name_str;
		}

		/* loop thru test list and execute tests */
		for (
			test_list = Rtest_list;
			test_list != NULL;
			test_list = test_list->ptr
		)
		{
			printf("\nXX running test %s XX\n\n",test_list->name);

			/* if driver opens db for indiv. test, do so */
			if ( do_open_db && got_many_dbnames )
			{
				/* connect w/ database */
				strcpy(db_name_str,test_list->db_name_str);
				printf("connecting with database...\n");
				exec sql connect :db_name_str;
			}

			/* run test */
			(*test_list->func)();

			/* if driver opened db for indiv. test, close */
			if ( do_open_db && got_many_dbnames )
			{
				/* disconnect w/ database */
				printf("disconnecting with database...\n");
				exec sql disconnect;
			}
			printf("\n");
		}

		/* if driver opened single db for all tests, close */
		if ( do_open_db && !got_many_dbnames && !test_open_db )
		{
			/* disconnect w/ database */
			printf("disconnecting with database...\n");
			exec sql disconnect;
		}

		/* update testinfo w/ test results */
		if ( do_upd_testinfo )
		{
			/* connect w/ testinfo */
			/* disconnect w/ testinfo */
		}

	} /* for number_of_runs */

} /* main() */

/************************/
/* driver_list_search() */
/************************/

/*
**	do a search on the list of tests.
*/

driver_list_search(s)
char	*s;
{
	int	i;
	short	found	= FALSE;

	/* do rock stupid search */
	for ( i = 0; driver_list[i].name; i++ )
	{
		if ( strcmp(driver_list[i].name,s) == 0 )
		{
			found = TRUE;
			break;
		}
	}

	return( ( ( found ) ? i : NOTFOUND ) );

} /* driver_list_search() */

/*********************/
/* sched_all_tests() */
/*********************/

sched_all_tests()
{
	int	i;
	int	test_ptr;

	/* find out how many tests there are */
	for ( test_ptr = 0; driver_list[test_ptr].name; test_ptr++ );
	test_ptr--;

	/* build that list of tests */
	for ( i = test_ptr; i >= 0; i-- )
	{
		test_list = (TEST_LIST_ROW *) malloc(sizeof(TEST_LIST_ROW));
		test_list->name = driver_list[i].name;
		test_list->func = driver_list[i].func;
		test_list->ptr = Rtest_list;
		Rtest_list = test_list;
	}

} /* sched_all_tests() */

/*********************/
/* print_all_tests() */
/*********************/

print_all_tests()
{
	int	i;

	/* print that test list */
	printf("\n");
	printf("test list\n");
	printf("---------\n");
	for ( i = 0; driver_list[i].name; i++ )
	{
		printf("  %s\n",driver_list[i].name);
	}

} /* print_all_tests() */

/***************/
/* end_clean() */
/***************/

end_clean()
{
	/* well, this could use more work */

	/* determine open sessions and close them */

} /* end_clean() */

/****************/
/* parse_args() */
/****************/

parse_args(argci,argvi)
int	argci;
char	*argvi[];
{
	int	i,j;		/* santa's helpers */
	char	currarg[128];	/* current arguments */

	static	short	got_fatal_err	= FALSE;	/* remember disaster */

	/* loop through the arguments */
	for ( i = 1; i < argci; i++)
	{
		/* make a copy of current argument for no particular reason */
		strcpy(currarg,argvi[i]);

		if ( currarg[0] != '-' )
		{
			/* gots to have that - */
			fprintf(stderr,"driver: args require - : %s\n",
				currarg
			);
			got_fatal_err = TRUE;
		}
		else if ( currarg[1] == 'd' )
		{
			/* take db name unless you've already got one */
			if ( got_dbname )
			{
				fprintf(stderr,"driver: only one override dbname allowed\n");
				got_fatal_err = TRUE;
			}
			else
			{
				strcpy(db_name_str,currarg+2);
				got_dbname = TRUE;
				do_open_db = TRUE;
			}
		}
		else if ( currarg[1] == 't' )
		{
			/* take test request unless doing all or unk. test */
			if ( do_all_tests )
			{
				fprintf(stderr,"error: -A and -t flags used simultaneously\n");
				got_fatal_err = TRUE;
			}

			if ( ( j = driver_list_search(currarg+2) ) == NOTFOUND )
			{
				fprintf(stderr,
					"driver: can't find test %s\n",
					currarg+2
				);
				got_fatal_err = TRUE;
			}
			else
			{
				test_list = 
					(TEST_LIST_ROW *) 
					malloc(sizeof(TEST_LIST_ROW));
				test_list->name = driver_list[j].name;
				test_list->func = driver_list[j].func;
				test_list->ptr = Rtest_list;
				Rtest_list = test_list;
				got_testname = TRUE;
			}
		}
		else if ( currarg[1] == 'n' )
		{
			/* set number of runs for test unless stupid */
			number_of_runs = atoi(currarg+2);
			printf("number of runs: %d\n",number_of_runs);
			if ( number_of_runs < 1 )
			{
				printf("error: -n argument used incorrectly\n");
				got_fatal_err = TRUE;
			}
		}
		else
		{
			for ( j = 1; currarg[j]; j++ )
			{
				switch( currarg[j] )
				{
					case 'm' :
						/* dump main() for user */
						dump_main = TRUE;
						break;

					case 'o' :
						/* tests open db */
						printf("the tests will open the database(s)...\n");
						test_open_db = TRUE;
						break;

					case 'u' :
						/* results to testinfo */
						printf("-u not working yet\n");
						got_fatal_err = TRUE;
						break;

					case 'x' :
						/* do short explain */
						do_explain = TRUE;
						break;

					case 'A' :
						/* run all tests */
						printf("schedule all tests\n");
						do_all_tests = TRUE;
						got_testname = TRUE;
						if ( Rtest_list != NULL )
						{
							fprintf(stderr,
								"error: -A and -t flags used simultaneously\n"
							);
							got_fatal_err = TRUE;
						}
						break;


					case 'X' :
						/* do long explain */
						do_long_explain = TRUE;
						break;

					default :
						printf("unknown arg %c\n",
							currarg[j]
						);
						got_fatal_err = TRUE;
						break;
				}
			}
		}
	}

	/* user misbehaved, eat the big cookie */
	if ( got_fatal_err )
	{
		print_syntax();
		end_clean();
		exit(0);
	}

} /* parse_args() */

/******************/
/* print_syntax() */
/******************/

print_syntax()
{
	/* print the syntax to stderr */

	fprintf(stderr,"syntax: driver [-t<test name>] [-d<database name>] [-n<number>] -[mouxAX]\n");
	fprintf(stderr,"\n");
	fprintf(stderr,"\t[-t<test name>]      - use test name (ignore testinfo)\n");
	fprintf(stderr,"\t[-d<database name>]  - override database name (only one per customer)\n");
	fprintf(stderr,"\t[-n<number>]         - run tests this <number> times\n");
	fprintf(stderr,"\n");
	fprintf(stderr,"\t-m\t- produce main() for user\n");
	fprintf(stderr,"\t-o\t- tests open database(s)\n");
	fprintf(stderr,"\t-u\t- status to testinfo database (not avail.)\n");
	fprintf(stderr,"\t-x\t- print bio and exit\n");
	fprintf(stderr,"\t-A\t- schedule all tests\n");
	fprintf(stderr,"\t-X\t- print long bio and exit\n");

} /* print_syntax() */

/**************************/
/* dump_that_main_fella() */
/**************************/

dump_that_main_fella()
{
	printf("/* DELETE THIS LINE AND ALL ABOVE IT */\n");

	printf("/* includes */\n");
	printf("#include <signal.h>\n");
	printf("#include <stdio.h>\n");
	printf("\n");

	printf("/* ingres includes */\n");
	printf("exec sql include sqlca;\n");
	printf("\n");

	printf("/* ingres whenevers */\n");
	printf("exec sql whenever sqlerror call sqlprint;\n");
	printf("exec sql whenever not found call sqlprint;\n");
	printf("exec sql whenever sqlwarning call sqlprint;\n");
	printf("\n");

	printf("/*****************/\n");
	printf("/* signal_trap() */\n");
	printf("/*****************/\n");
	printf("\n");

	printf("signal_trap(sig)\n");
	printf("{\n");
	printf("\t/* DEL THIS LINE & ADD DISCONNECTS, ETC. IF DESIRED */\n");
	printf("\n");

	printf("\tif ( ( sig != SIGINT ) && ( sig != SIGQUIT ) )\n");
	printf("\t{\n");
	printf("\t\tfprintf(stderr,\"signal_trap(): signal %%d\\n\",sig);\n");
	printf("\t}\n");
	printf("\n");

	printf("\texit(0);\n");
	printf("\n");

	printf("} /* signal_trap() */\n");
	printf("\n");

	printf("/******************/\n");
	printf("/* signal_setup() */\n");
	printf("/******************/\n");
	printf("\n");

	printf("signal_setup()\n");
	printf("{\n");
	printf("\tint	i;\n");
	printf("\n");

	printf("\tfor ( i = SIGHUP; i <= SIGSEGV; i++ )\n");
	printf("\t{\n");
	printf("\t\tsignal(i,signal_trap);\n");
	printf("\t}\n");
	printf("\n");

	printf("} /* signal_setup() */\n");
	printf("\n");

	printf("/**********/\n");
	printf("/* main() */\n");
	printf("/**********/\n");
	printf("\n");

	printf("main()\n");
	printf("{\n");

	/* for as many times as the tests are supposed to be repeated... */
	for( curr_number_of_runs = 1; 
	     curr_number_of_runs <= number_of_runs; 
	     curr_number_of_runs++
	)
	{
		/* provide open */
		if ( do_open_db && !got_many_dbnames && !test_open_db )
		{
			printf("\texec sql connect %s;\n",db_name_str);
			printf("\n");
		}

		/* run thru test list */
		for (
			test_list = Rtest_list;
			test_list != NULL;
			test_list = test_list->ptr
		)
		{
			/* provide individual open */
			if ( do_open_db && got_many_dbnames )
			{
				printf("\texec sql connect %s;\n",
					test_list->db_name_str
				);
			}

			printf("\t%s();\n",test_list->name);

			/* provide individual close */
			if ( do_open_db && got_many_dbnames )
			{
				printf("\texec sql disconnect;\n");
				printf("\n");
			}

		} /* for */
		printf("\n");

		/* provide close */
		if ( do_open_db && !got_many_dbnames && !test_open_db )
		{
			printf("\texec sql disconnect;\n");
			printf("\n");
		}

	} /* for */

	printf("} /* main() */\n");
	printf("\n");

	printf("/* DELETE THIS LINE AND ADD YOUR TEST MODULES HERE */\n");

} /* dump_that_main_fella() */

/*------------------------------------------------------------------------*/
/*                                                                        */
/*	public utilities                                                  */
/*                                                                        */
/*------------------------------------------------------------------------*/

/***************/
/* dr_inqing() */
/***************/

dr_inqing()
{
	exec sql begin declare section;

	int	drDbmserror;
	int	drEndquery;
	int	drErrno;
	char	drErrtext[256];
	char	drErrtype[32];
	int	drMsgnumber;
	char	drMsgtext[256];
	int	drRowcount;
	int	drSession;
	int	drTransaction;

	exec sql end declare section;

	exec sql inquire_ingres (
		:drDbmserror		= Dbmserror,
		:drEndquery		= Endquery,
		:drErrno		= Errorno,
		:drErrtext		= Errortext,
		:drErrtype		= Errortype,
		:drMsgnumber		= Messagenumber,
		:drMsgtext		= Messagetext,
		:drRowcount		= Rowcount,
		:drSession		= Session,
		:drTransaction		= Transaction
	);

	printf("\n");
	printf("   XX----------------XX\n");
	printf("   XX inquire_ingres XX\n");
	printf("   XX----------------XX\n");
	printf("     Dbmserror     = %d\n",drDbmserror);
	printf("     Endquery      = %d\n",drEndquery);
	printf("     Errorno       = %d\n",drErrno);
	printf("     Errortext     = %s\n",drErrtext);
	printf("     Errortype     = %s\n",drErrtype);
	printf("     Messagenumber = %d\n",drMsgnumber);
	printf("     Messagetext   = %s\n",drMsgtext);
	printf("     Rowcount      = %d\n",drRowcount);
	printf("     Session       = %d\n",drSession);
	printf("     Transaction   = %d\n",drTransaction);
	printf("   XX----------------XX\n");
	printf("\n");

} /* dr_inqing() */

/**************/
/* dr_sqlca() */
/**************/

dr_sqlca()
{
	printf("\n");
	printf("   XX-------XX\n");
	printf("   XX sqlca XX\n");
	printf("   XX-------XX\n");
	printf("     sqlca.sqlcaid   		= %7.7s\n",sqlca.sqlcaid);
	printf("     sqlca.sqlcabc   		= %d\n",sqlca.sqlcabc);
	printf("     sqlca.sqlcode   		= %d\n",sqlca.sqlcode);
	printf("     sqlca.sqlerrm.sqlerrml	= %d\n",sqlca.sqlerrm.sqlerrml);
	/* can contain dates...
	printf("     sqlca.sqlerrm.sqlerrmc	= %s\n",sqlca.sqlerrm.sqlerrmc);
	*/
	printf("     sqlca.sqlerrp   		= %s\n",sqlca.sqlerrp);
	printf("     sqlca.sqlerrd[0]  		= %d\n",sqlca.sqlerrd[0]);
	printf("     sqlca.sqlerrd[1]  		= %d\n",sqlca.sqlerrd[1]);
	printf("     sqlca.sqlerrd[2]  		= %d\n",sqlca.sqlerrd[2]);
	printf("     sqlca.sqlerrd[3]  		= %d\n",sqlca.sqlerrd[3]);
	printf("     sqlca.sqlerrd[4]  		= %d\n",sqlca.sqlerrd[4]);
	printf("     sqlca.sqlerrd[5]  		= %d\n",sqlca.sqlerrd[5]);
	/*
	printf("     sqlca.sqlwarn.sqlwarn0	= %s\n",sqlca.sqlwarn.sqlwarn0);
	printf("     sqlca.sqlwarn.sqlwarn1	= %s\n",sqlca.sqlwarn.sqlwarn1);
	printf("     sqlca.sqlwarn.sqlwarn2	= %s\n",sqlca.sqlwarn.sqlwarn2);
	printf("     sqlca.sqlwarn.sqlwarn3	= %s\n",sqlca.sqlwarn.sqlwarn3);
	printf("     sqlca.sqlwarn.sqlwarn4	= %s\n",sqlca.sqlwarn.sqlwarn4);
	printf("     sqlca.sqlwarn.sqlwarn5	= %s\n",sqlca.sqlwarn.sqlwarn5);
	printf("     sqlca.sqlwarn.sqlwarn6	= %s\n",sqlca.sqlwarn.sqlwarn6);
	printf("     sqlca.sqlwarn.sqlwarn7	= %s\n",sqlca.sqlwarn.sqlwarn7);
	*/
	printf("     sqlca.sqlext   		= %s\n",sqlca.sqlext);
	printf("   XX-------XX\n");
	printf("\n");

} /* dr_sqlca() */
