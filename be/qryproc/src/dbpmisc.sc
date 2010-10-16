/*
**	dbpmisc.sc
**
**	control program for running dbproc tests.
**
**	history:
**		sep 20/89 sgp
**			- created dbp.sc.
**		jan 15/90 sgp
**			- stole all this from dbp.sc while making
**			  tests driver compatible.
*/

#include <stdio.h>

exec sql include 'dbph.sc';

exec sql include sqlca;

exec sql whenever sqlerror call sqlprint;
exec sql whenever not found call sqlprint;
exec sql whenever sqlwarning call sqlprint;

/********************/
/* a_little_setup() */
/********************/

a_little_setup()
{
	exec sql set autocommit on;

} /* a_little_setup() */

/******************/
/* build_tables() */
/******************/

build_tables(table_id)
int	table_id;
{
	switch ( table_id )
	{
		case ALL_TYPES_TABLE :
			printf("  creating all_types table...\n");
			exec sql create table all_types 
				as select * from bk_all_types;
		break;

		case ALL_TYPES_SIMPLE_TABLE :
			printf("  creating all_types_simple table...\n");
			exec sql create table all_types_simple 
				as select * from bk_all_types_simple;
		break;

		case BLDG_TABLE :
			printf("  creating bldg table...\n");
			exec sql create table bldg as select * from bk_bldg;
		break;

		case DEPT_TABLE :
			printf("  creating dept table...\n");
			exec sql create table dept as select * from bk_dept;
		break;

		case EMP_TABLE :
			printf("  creating emp table...\n");
			exec sql create table emp as select * from bk_emp;
		break;

		case EMPTY_ALL_TYPES_TABLE :
			printf("  creating empty_all_types table...\n");
			exec sql create table empty_all_types 
				as select * from bk_empty_all_types;
		break;

		case STD_0100_TABLE :
			printf("  creating std_0100 table...\n");
			exec sql create table std_0100 
				as select * from bk_std_0100;
			printf("  created std_0100 table...\n");
		break;

		default :
			printf("build_tables(): unknown table %d\n",table_id);
		break;
	}

} /* build_tables() */

/******************/
/* drop_tables() */
/******************/

drop_tables(table_id)
int	table_id;
{
	switch ( table_id )
	{
		case ALL_TYPES_TABLE :
			printf("  dropping all_types table...\n");
			exec sql drop table all_types;
		break;

		case ALL_TYPES_SIMPLE_TABLE :
			printf("  dropping all_types_simple table...\n");
			exec sql drop table all_types_simple;
		break;

		case BLDG_TABLE :
			printf("  dropping bldg table...\n");
			exec sql drop table bldg;
		break;

		case DEPT_TABLE :
			printf("  dropping dept table...\n");
			exec sql drop table dept;
		break;

		case EMP_TABLE :
			printf("  dropping emp table...\n");
			exec sql drop table emp;
		break;

		case EMPTY_ALL_TYPES_TABLE :
			printf("  dropping empty_all_types table...\n");
			exec sql drop table empty_all_types;
		break;

		case STD_0100_TABLE :
			printf("  dropping std_0100 table...\n");
			exec sql drop table std_0100;
		break;

		default :
			printf("drop_tables(): unknown table %d\n",table_id);
		break;
	}

} /* drop_tables() */

/*****************/
/* dump_tables() */
/*****************/

dump_tables(table_id)
int	table_id;
{
	switch ( table_id )
	{
		case ALL_TYPES_TABLE :

			printf("contents of all_types:\n");
			printf("----------------------\n");
			exec sql select * 
				into :all_types 
				from all_types;
			exec sql begin;
				printf(" i1 = %d,",all_types.i1);
				printf(" i2 = %d,",all_types.i2);
				printf(" i4 = %d,",all_types.i4);
				printf(" f4 = %f,",all_types.f4);
				printf(" f8 = %f,",all_types.f8);
				printf("\n");
				printf(" c10 = %s,",all_types.c10);
				printf(" char10 = %s,",all_types.char10);
				printf(" vchar10 = %s,",
					all_types.vchar10
				);
				printf(" varchar10 = %s,",
					all_types.varchar10
				);
				printf("\n");
				printf(" d = %s,",all_types.d);
				printf(" m = %f",all_types.m);
				printf("\n");
				printf("----------------------\n");
			exec sql end;
		break;

		case ALL_TYPES_SIMPLE_TABLE :

			printf("contents of all_types_simple:\n");
			printf("-----------------------------\n");
			exec sql select * 
				into :all_types_simple 
				from all_types_simple;
			exec sql begin;
				printf(" i1 = %d,",all_types_simple.i1);
				printf(" i2 = %d,",all_types_simple.i2);
				printf(" i4 = %d,",all_types_simple.i4);
				printf(" f4 = %f,",all_types_simple.f4);
				printf(" f8 = %f,",all_types_simple.f8);
				printf("\n");
				printf(" c10 = %s,",all_types_simple.c10);
				printf(" char10 = %s,",all_types_simple.char10);
				printf(" vchar10 = %s,",
					all_types_simple.vchar10
				);
				printf(" varchar10 = %s,",
					all_types_simple.varchar10
				);
				printf("\n");
				printf(" d = %s,",all_types_simple.d);
				printf(" m = %f",all_types_simple.m);
				printf("\n");
				printf("-----------------------------\n");
			exec sql end;
		break;

		case BLDG_TABLE :

			printf("contents of bldg:\n");
			printf("-----------------\n");
			exec sql select * 
				into :bldg 
				from bldg;
			exec sql begin;
				printf(" bldg = %s,",bldg.bldg);
				printf(" st_adr = %s,",bldg.st_adr);
				printf(" city = %s,",bldg.city);
				printf(" state = %s,",bldg.state);
				printf(" zip = %s",bldg.zip);
				printf("\n");
				printf("-----------------\n");
			exec sql end;

		break;

		case DEPT_TABLE :

			printf("contents of dept:\n");
			printf("-----------------\n");
			exec sql select * 
				into :dept 
				from dept;
			exec sql begin;
				printf(" dname = %s,",dept.dname);
				printf(" div = %s,",dept.div);
				printf(" sales = %d,",dept.sales);
				printf(" bldg = %s,",dept.bldg);
				printf(" floor = %d,",dept.floor);
				printf(" num_emp = %d",dept.num_emp);
				printf("\n");
				printf("-----------------\n");
			exec sql end;

		break;

		case EMP_TABLE :

			printf("contents of emp:\n");
			printf("----------------\n");
			exec sql select * 
				into :emp 
				from emp;
			exec sql begin;
				printf(" name = %s,",emp.name);
				printf(" salary = %f,",emp.salary);
				printf(" dept = %s,",emp.dept);
				printf(" div = %s,",emp.div);
				printf(" mgr = %s,",emp.mgr);
				printf(" birthdate = %s,",emp.birthdate);
				printf(" num_dep = %d",emp.num_dep);
				printf("\n");
				printf("----------------\n");
			exec sql end;

		break;

		case EMPTY_ALL_TYPES_TABLE :

			printf("contents of empty_all_types:\n");
			printf("----------------------------\n");
			exec sql select * 
				into :empty_all_types 
				from empty_all_types;
			exec sql begin;
				printf(" i1 = %d,",empty_all_types.i1);
				printf(" i2 = %d,",empty_all_types.i2);
				printf(" i4 = %d,",empty_all_types.i4);
				printf(" f4 = %f,",empty_all_types.f4);
				printf(" f8 = %f,",empty_all_types.f8);
				printf("\n");
				printf(" c10 = %s,",empty_all_types.c10);
				printf(" char10 = %s,",empty_all_types.char10);
				printf(" vchar10 = %s,",
					empty_all_types.vchar10
				);
				printf(" varchar10 = %s,",
					empty_all_types.varchar10
				);
				printf("\n");
				printf(" d = %s,",empty_all_types.d);
				printf(" m = %f",empty_all_types.m);
				printf("\n");
				printf("----------------------------\n");
			exec sql end;
		break;

		default :
			printf("drop_tables(): unknown table %d\n",table_id);
		break;
	}

} /* dump_tables() */
