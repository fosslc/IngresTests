/*
**	rulestest.sc
**
**	a potpurri of functions to be used by the rules tests.
*/

#include <stdio.h>

exec sql include 'ingclude.sc';
exec sql include 'rulesh.sc';

/*------------------------------------------------------------------------*/
/*                                                                        */
/*	manage tables                                                     */
/*                                                                        */
/*------------------------------------------------------------------------*/

/******************/
/* build_tables() */
/******************/

build_tables(s)
char	*s;
{
	drop_tables(s);

	printf("  creating %s table...\n",s);

	sprintf(stmt_buf,"create table %s as select * from bk_%s",s,s);

	exec sql execute immediate :stmt_buf;

	build_indexes(s);

	exec sql commit;

} /* build_tables() */

/*******************/
/* build_indexes() */
/*******************/

build_indexes(s)
char	*s;
{

} /* build_indexes() */

/*****************/
/* drop_tables() */
/*****************/

drop_tables(s)

char	*s;
{
	printf("  dropping %s table...\n",s);

	sprintf(stmt_buf,"drop table %s",s);

	exec sql execute immediate :stmt_buf;

} /* drop_tables() */

/******************/
/* flush_tables() */
/******************/

flush_tables(s)
char	*s;
{
	printf("  flushing %s table...\n",s);

	sprintf(stmt_buf,"modify %s to truncated",s);

	exec sql execute immediate :stmt_buf;

} /* flush_tables() */

/*****************/
/* diff_tables() */
/*****************/

diff_tables(i)
int	i;
{
	switch(i)
	{
		case BLDG :

			printf("=====================\n");
			printf("in bldg, not in orig:\n");
			printf("---------------------\n");

			exec sql select *
				into :bldg
				from bldg a
				where not exists (
					select * from bk_bldg b
					where	a.bldg		= b.bldg
					and	a.st_adr	= b.st_adr
					and	a.city		= b.city
					and	a.state		= b.state
					and	a.zip		= b.zip
				)
				order by 1, 2, 3, 4, 5;
			exec sql begin;
				printf(" bldg = %s,",bldg.bldg);
				printf(" st_adr = %s,",bldg.st_adr);
				printf(" city = %s,",bldg.city);
				printf(" state = %s,",bldg.state);
				printf(" zip = %s",bldg.zip);
				printf("\n");
			exec sql end;
			printf("\n");

			printf("in orig, not in bldg:\n");
			printf("---------------------\n");
			exec sql select *
				into :bldg
				from bk_bldg a
				where not exists (
					select * from bldg b
					where	a.bldg		= b.bldg
					and	a.st_adr	= b.st_adr
					and	a.city		= b.city
					and	a.state		= b.state
					and	a.zip		= b.zip
				)
				order by 1, 2, 3, 4, 5;
			exec sql begin;
				printf(" bldg = %s,",bldg.bldg);
				printf(" st_adr = %s,",bldg.st_adr);
				printf(" city = %s,",bldg.city);
				printf(" state = %s,",bldg.state);
				printf(" zip = %s",bldg.zip);
				printf("\n");
			exec sql end;
			printf("---------------------\n");
			printf("\n");

		break;

		case CHAIN_1 :

			printf("========================\n");
			printf("in chain_1, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :chain_1
				from chain_1 a
				where not exists (
					select * from bk_chain_1 b
					where	a.id	= b.id
				);
			exec sql begin;
				printf(" id = %d",chain_1.id);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in chain_1:\n");
			printf("------------------------\n");
			exec sql select *
				into :chain_1
				from bk_chain_1 a
				where not exists (
					select * from chain_1 b
					where	a.id	= b.id
				);
			exec sql begin;
				printf(" id = %d",chain_1.id);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case CHAIN_2 :

			printf("========================\n");
			printf("in chain_2, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :chain_2
				from chain_2 a
				where not exists (
					select * from bk_chain_2 b
					where	a.id	= b.id
				)
				order by 1;
			exec sql begin;
				printf(" id = %d",chain_2.id);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in chain_2:\n");
			printf("------------------------\n");
			exec sql select *
				into :chain_2
				from bk_chain_2 a
				where not exists (
					select * from chain_2 b
					where	a.id	= b.id
				)
				order by 1;
			exec sql begin;
				printf(" id = %d",chain_2.id);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case CHAIN_3 :

			printf("========================\n");
			printf("in chain_3, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :chain_3
				from chain_3 a
				where not exists (
					select * from bk_chain_3 b
					where	a.id	= b.id
				)
				order by 1;
			exec sql begin;
				printf(" id = %d",chain_3.id);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in chain_3:\n");
			printf("------------------------\n");
			exec sql select *
				into :chain_3
				from bk_chain_3 a
				where not exists (
					select * from chain_3 b
					where	a.id	= b.id
				)
				order by 1;
			exec sql begin;
				printf(" id = %d",chain_3.id);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case DEPT :

			printf("=====================\n");
			printf("in dept, not in orig:\n");
			printf("---------------------\n");

			exec sql select *
				into :dept
				from dept a
				where not exists (
					select * from bk_dept b
					where	a.dname		= b.dname
					and	a.div		= b.div
					and	a.sales		= b.sales
					and	a.bldg		= b.bldg
					and	a.floor		= b.floor
					and	a.num_emp	= b.num_emp
				)
				order by 1, 2, 3, 4, 5, 6;
			exec sql begin;
				printf(" dname = %s,",dept.dname);
				printf(" div = %s,",dept.div);
				printf(" sales = %d,",dept.sales);
				printf(" bldg = %s,",dept.bldg);
				printf(" floor = %d,",dept.floor);
				printf(" num_emp = %d",dept.num_emp);
				printf("\n");
			exec sql end;
			printf("\n");

			printf("in orig, not in dept:\n");
			printf("---------------------\n");
			exec sql select *
				into :dept
				from bk_dept a
				where not exists (
					select * from dept b
					where	a.dname		= b.dname
					and	a.div		= b.div
					and	a.sales		= b.sales
					and	a.bldg		= b.bldg
					and	a.floor		= b.floor
					and	a.num_emp	= b.num_emp
				)
				order by 1, 2, 3, 4, 5, 6;
			exec sql begin;
				printf(" dname = %s,",dept.dname);
				printf(" div = %s,",dept.div);
				printf(" sales = %d,",dept.sales);
				printf(" bldg = %s,",dept.bldg);
				printf(" floor = %d,",dept.floor);
				printf(" num_emp = %d",dept.num_emp);
				printf("\n");
			exec sql end;
			printf("---------------------\n");
			printf("\n");

		break;

		case EMP :

			printf("====================\n");
			printf("in emp, not in orig:\n");
			printf("--------------------\n");

			exec sql select *
				into :emp
				from emp a
				where not exists (
					select * from bk_emp b
					where	a.name		= b.name
					and	a.salary	= b.salary
					and	a.dept		= b.dept
					and	a.div		= b.div
					and	a.mgr		= b.mgr
					and	a.birthdate	= b.birthdate
					and	a.num_dep	= b.num_dep
				)
				order by 1, 2, 3, 4, 5, 6, 7;
			exec sql begin;
				printf(" name = %s,",emp.name);
				printf(" salary = %f,",emp.salary);
				printf(" dept = %s,",emp.dept);
				printf(" div = %s,",emp.div);
				printf(" mgr = %s,",emp.mgr);
				printf(" birthdate = %s,",emp.birthdate);
				printf(" num_dep = %d",emp.num_dep);
				printf("\n");
			exec sql end;
			printf("\n");

			printf("in orig, not in emp:\n");
			printf("--------------------\n");
			exec sql select *
				into :emp
				from bk_emp a
				where not exists (
					select * from emp b
					where	a.name		= b.name
					and	a.salary	= b.salary
					and	a.dept		= b.dept
					and	a.div		= b.div
					and	a.mgr		= b.mgr
					and	a.birthdate	= b.birthdate
					and	a.num_dep	= b.num_dep
				)
				order by 1, 2, 3, 4, 5, 6, 7;
			exec sql begin;
				printf(" name = %s,",emp.name);
				printf(" salary = %f,",emp.salary);
				printf(" dept = %s,",emp.dept);
				printf(" div = %s,",emp.div);
				printf(" mgr = %s,",emp.mgr);
				printf(" birthdate = %s,",emp.birthdate);
				printf(" num_dep = %d",emp.num_dep);
				printf("\n");
			exec sql end;
			printf("--------------------\n");
			printf("\n");

		break;

		case GRAPH_1 :

			printf("========================\n");
			printf("in graph_1, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :graph_1
				from graph_1 a
				where not exists (
					select * from bk_graph_1 b
					where	a.from_node	= b.from_node
					and	a.to_node	= b.to_node
				)
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_1.from_node);
				printf(" to_node = %d",graph_1.to_node);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in graph_1:\n");
			printf("------------------------\n");
			exec sql select *
				into :graph_1
				from bk_graph_1 a
				where not exists (
					select * from graph_1 b
					where	a.from_node	= b.from_node
					and	a.to_node	= b.to_node
				)
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_1.from_node);
				printf(" to_node = %d",graph_1.to_node);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case GRAPH_2 :

			printf("========================\n");
			printf("in graph_2, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :graph_2
				from graph_2 a
				where not exists (
					select * from bk_graph_2 b
					where	a.from_node	= b.from_node
					and	a.to_node	= b.to_node
				)
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_2.from_node);
				printf(" to_node = %d",graph_2.to_node);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in graph_2:\n");
			printf("------------------------\n");
			exec sql select *
				into :graph_2
				from bk_graph_2 a
				where not exists (
					select * from graph_2 b
					where	a.from_node	= b.from_node
					and	a.to_node	= b.to_node
				)
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_2.from_node);
				printf(" to_node = %d",graph_2.to_node);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case GRAPH_3 :

			printf("========================\n");
			printf("in graph_3, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :graph_3
				from graph_3 a
				where not exists (
					select * from bk_graph_3 b
					where	a.from_node	= b.from_node
					and	a.to_node	= b.to_node
				)
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_3.from_node);
				printf(" to_node = %d",graph_3.to_node);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in graph_3:\n");
			printf("------------------------\n");
			exec sql select *
				into :graph_3
				from bk_graph_3 a
				where not exists (
					select * from graph_3 b
					where	a.from_node	= b.from_node
					and	a.to_node	= b.to_node
				)
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_3.from_node);
				printf(" to_node = %d",graph_3.to_node);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case SMALL_1 :

			printf("========================\n");
			printf("in small_1, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_1
				from small_1 a
				where not exists (
					select * from bk_small_1 b
					where	a.ida	= b.ida
					and	a.idb	= b.idb
				)
				order by 1, 2;
			exec sql begin;
				printf(" ida = %d,",small_1.ida);
				printf(" idb = %d",small_1.idb);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in small_1:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_1
				from bk_small_1 a
				where not exists (
					select * from small_1 b
					where	a.ida	= b.ida
					and	a.idb	= b.idb
				)
				order by 1, 2;
			exec sql begin;
				printf(" ida = %d,",small_1.ida);
				printf(" idb = %d",small_1.idb);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case SMALL_2 :

			printf("========================\n");
			printf("in small_2, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_2
				from small_2 a
				where not exists (
					select * from bk_small_2 b
					where	a.ida	= b.ida
				)
				order by 1;
			exec sql begin;
				printf(" ida = %d",small_2.ida);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in small_2:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_2
				from bk_small_2 a
				where not exists (
					select * from small_2 b
					where	a.ida	= b.ida
				)
				order by 1;
			exec sql begin;
				printf(" ida = %d",small_2.ida);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case SMALL_3 :

			printf("========================\n");
			printf("in small_3, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_3
				from small_3 a
				where not exists (
					select * from bk_small_3 b
					where	a.ida		= b.ida
					and	a.ida_sub	= b.ida_sub
				)
				order by 1, 2;
			exec sql begin;
				printf(" ida = %d,",small_3.ida);
				printf(" ida_sub = %d",small_3.ida_sub);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in small_3:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_3
				from bk_small_3 a
				where not exists (
					select * from small_3 b
					where	a.ida		= b.ida
					and	a.ida_sub	= b.ida_sub
				)
				order by 1, 2;
			exec sql begin;
				printf(" ida = %d,",small_3.ida);
				printf(" ida_sub = %d",small_3.ida_sub);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case SMALL_4 :

			printf("========================\n");
			printf("in small_4, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_4
				from small_4 a
				where not exists (
					select * from bk_small_4 b
					where	a.ida		= b.ida
					and	a.ida_sub	= b.ida_sub
				)
				order by 1, 2;
			exec sql begin;
				printf(" ida = %d,",small_4.ida);
				printf(" ida_sub = %d",small_4.ida_sub);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in small_4:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_4
				from bk_small_4 a
				where not exists (
					select * from small_4 b
					where	a.ida		= b.ida
					and	a.ida_sub	= b.ida_sub
				)
				order by 1, 2;
			exec sql begin;
				printf(" ida = %d,",small_4.ida);
				printf(" ida_sub = %d",small_4.ida_sub);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case SMALL_5 :

			printf("========================\n");
			printf("in small_5, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_5
				from small_5 a
				where not exists (
					select * from bk_small_5 b
					where	a.idb	= b.idb
				)
				order by 1;
			exec sql begin;
				printf(" idb = %d",small_5.idb);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in small_5:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_5
				from bk_small_5 a
				where not exists (
					select * from small_5 b
					where	a.idb	= b.idb
				)
				order by 1;
			exec sql begin;
				printf(" idb = %d",small_5.idb);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case SMALL_6 :

			printf("========================\n");
			printf("in small_6, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_6
				from small_6 a
				where not exists (
					select * from bk_small_6 b
					where	a.idb	= b.idb
					and	a.idc	= b.idc
				)
				order by 1, 2;
			exec sql begin;
				printf(" idb = %d,",small_6.idb);
				printf(" idc = %d",small_6.idc);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in small_6:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_6
				from bk_small_6 a
				where not exists (
					select * from small_6 b
					where	a.idb	= b.idb
					and	a.idc	= b.idc
				)
				order by 1, 2;
			exec sql begin;
				printf(" idb = %d,",small_6.idb);
				printf(" idc = %d",small_6.idc);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case SMALL_7 :

			printf("========================\n");
			printf("in small_7, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_7
				from small_7 a
				where not exists (
					select * from bk_small_7 b
					where	a.idc		= b.idc
					and	a.idc_sub	= b.idc_sub
				)
				order by 1, 2;
			exec sql begin;
				printf(" idc = %d,",small_7.idc);
				printf(" idc_sub = %d",small_7.idc_sub);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in small_7:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_7
				from bk_small_7 a
				where not exists (
					select * from small_7 b
					where	a.idc		= b.idc
					and	a.idc_sub	= b.idc_sub
				)
				order by 1, 2;
			exec sql begin;
				printf(" idc = %d,",small_7.idc);
				printf(" idc_sub = %d",small_7.idc_sub);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case SMALL_8 :

			printf("========================\n");
			printf("in small_8, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_8
				from small_8 a
				where not exists (
					select * from bk_small_8 b
					where	a.idc	= b.idc
					and	a.ida	= b.ida
				)
				order by 1, 2;
			exec sql begin;
				printf(" idc = %d,",small_8.idc);
				printf(" ida = %d",small_8.ida);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in small_8:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_8
				from bk_small_8 a
				where not exists (
					select * from small_8 b
					where	a.idc	= b.idc
					and	a.ida	= b.ida
				)
				order by 1, 2;
			exec sql begin;
				printf(" idc = %d,",small_8.idc);
				printf(" ida = %d",small_8.ida);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case SMALL_9 :

			printf("========================\n");
			printf("in small_9, not in orig:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_9
				from small_9 a
				where not exists (
					select * from bk_small_9 b
					where	a.idc	= b.idc
				)
				order by 1;
			exec sql begin;
				printf(" idc = %d",small_9.idc);
				printf("\n");
			exec sql end;
			printf("------------------------\n");
			printf("\n");

			printf("in orig, not in small_9:\n");
			printf("------------------------\n");
			exec sql select *
				into :small_9
				from bk_small_9 a
				where not exists (
					select * from small_9 b
					where	a.idc	= b.idc
				)
				order by 1;
			exec sql begin;
				printf(" idc = %d",small_9.idc);
				printf("\n");
			exec sql end;
			printf("------------------------\n");

		break;

		case LOG :

			printf("====================\n");
			printf("in log, not in orig:\n");
			printf("--------------------\n");
			exec sql select *
				into :ruleslog
				from log a
				where not exists (
					select * from bk_log b
					where	a.entry	= b.entry
				)
				order by 1;
			exec sql begin;
				printf(" entry = %s",ruleslog.entry);
				printf("\n");
			exec sql end;
			printf("--------------------\n");
			printf("\n");

			printf("in orig, not in log:\n");
			printf("--------------------\n");
			exec sql select *
				into :ruleslog
				from bk_log a
				where not exists (
					select * from log b
					where	a.entry	= b.entry
				)
				order by 1;
			exec sql begin;
				printf(" entry = %s",ruleslog.entry);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case DULL :

			printf("=====================\n");
			printf("in dull, not in orig:\n");
			printf("---------------------\n");
			exec sql select *
				into :dull
				from dull a
				where not exists (
					select * from bk_dull b
					where	a.dull	= b.dull
				)
				order by 1;
			exec sql begin;
				printf(" dull = %d",dull.dull);
				printf("\n");
			exec sql end;
			printf("---------------------\n");
			printf("\n");

			printf("in orig, not in dull:\n");
			printf("---------------------\n");
			exec sql select *
				into :dull
				from bk_dull a
				where not exists (
					select * from dull b
					where	a.dull	= b.dull
				)
				order by 1;
			exec sql begin;
				printf(" dull = %d",dull.dull);
				printf("\n");
			exec sql end;
			printf("---------------------\n");

		break;

		default :
			printf("diff_tables() error: undefined arg\n");
		break;

	} /* switch */

} /* diff_tables() */

/******************/
/* print_tables() */
/******************/

print_tables(i)
int	i;
{
	switch(i)
	{
		case BLDG :

			printf("contents of bldg:\n");
			printf("-----------------\n");
			exec sql select *
				into :bldg
				from bldg
				order by 1, 2, 3, 4, 5;
			exec sql begin;
				printf(" bldg = %s,",bldg.bldg);
				printf(" st_adr = %s,",bldg.st_adr);
				printf(" city = %s,",bldg.city);
				printf(" state = %s,",bldg.state);
				printf(" zip = %s",bldg.zip);
				printf("\n");
			exec sql end;
			printf("-----------------\n");
			printf("\n");

		break;

		case CHAIN_1 :

			printf("contents of chain_1:\n");
			printf("--------------------\n");
			exec sql select *
				into :chain_1
				from chain_1
				order by 1;
			exec sql begin;
				printf(" id = %d",chain_1.id);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case CHAIN_2 :

			printf("contents of chain_2:\n");
			printf("--------------------\n");
			exec sql select *
				into :chain_2
				from chain_2
				order by 1;
			exec sql begin;
				printf(" id = %d",chain_2.id);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case CHAIN_3 :

			printf("contents of chain_3:\n");
			printf("--------------------\n");
			exec sql select *
				into :chain_3
				from chain_3
				order by 1;
			exec sql begin;
				printf(" id = %d",chain_3.id);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case DEPT :

			printf("contents of dept:\n");
			printf("-----------------\n");
			exec sql select *
				into :dept
				from dept
				order by 1, 2, 3, 4, 5, 6;
			exec sql begin;
				printf(" dname = %s,",dept.dname);
				printf(" div = %s,",dept.div);
				printf(" sales = %d,",dept.sales);
				printf(" bldg = %s,",dept.bldg);
				printf(" floor = %d,",dept.floor);
				printf(" num_emp = %d",dept.num_emp);
				printf("\n");
			exec sql end;
			printf("-----------------\n");
			printf("\n");

		break;

		case EMP :

			printf("contents of emp:\n");
			printf("----------------\n");
			exec sql select *
				into :emp
				from emp
				order by 1, 2, 3, 4, 5, 6, 7;
			exec sql begin;
				printf(" name = %s,",emp.name);
				printf(" salary = %f,",emp.salary);
				printf(" dept = %s,",emp.dept);
				printf(" div = %s,",emp.div);
				printf(" mgr = %s,",emp.mgr);
				printf(" birthdate = %s,",emp.birthdate);
				printf(" num_dep = %d",emp.num_dep);
				printf("\n");
			exec sql end;
			printf("----------------\n");
			printf("\n");

		break;

		case GRAPH_1 :

			printf("contents of graph_1:\n");
			printf("--------------------\n");
			exec sql select *
				into :graph_1
				from graph_1
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_1.from_node);
				printf(" to_node = %d",graph_1.to_node);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case GRAPH_2 :

			printf("contents of graph_2:\n");
			printf("--------------------\n");
			exec sql select *
				into :graph_2
				from graph_2
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_2.from_node);
				printf(" to_node = %d",graph_2.to_node);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case GRAPH_3 :

			printf("contents of graph_3:\n");
			printf("--------------------\n");
			exec sql select *
				into :graph_3
				from graph_3
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_3.from_node);
				printf(" to_node = %d",graph_3.to_node);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case SMALL_1 :

			printf("contents of small_1:\n");
			printf("--------------------\n");
			exec sql select *
				into :small_1
				from small_1
				order by 1, 2;
			exec sql begin;
				printf(" ida = %d,",small_1.ida);
				printf(" idb = %d",small_1.idb);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case SMALL_2 :

			printf("contents of small_2:\n");
			printf("--------------------\n");
			exec sql select *
				into :small_2
				from small_2
				order by 1;
			exec sql begin;
				printf(" ida = %d",small_2.ida);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case SMALL_3 :

			printf("contents of small_3:\n");
			printf("--------------------\n");
			exec sql select *
				into :small_3
				from small_3
				order by 1, 2;
			exec sql begin;
				printf(" ida = %d,",small_3.ida);
				printf(" ida_sub = %d",small_3.ida_sub);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case SMALL_4 :

			printf("contents of small_4:\n");
			printf("--------------------\n");
			exec sql select *
				into :small_4
				from small_4
				order by 1, 2;
			exec sql begin;
				printf(" ida = %d,",small_4.ida);
				printf(" ida_sub = %d",small_4.ida_sub);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case SMALL_5 :

			printf("contents of small_5:\n");
			printf("--------------------\n");
			exec sql select *
				into :small_5
				from small_5
				order by 1;
			exec sql begin;
				printf(" idb = %d",small_5.idb);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case SMALL_6 :

			printf("contents of small_6:\n");
			printf("--------------------\n");
			exec sql select *
				into :small_6
				from small_6
				order by 1, 2;
			exec sql begin;
				printf(" idb = %d,",small_6.idb);
				printf(" idc = %d",small_6.idc);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case SMALL_7 :

			printf("contents of small_7:\n");
			printf("--------------------\n");
			exec sql select *
				into :small_7
				from small_7
				order by 1, 2;
			exec sql begin;
				printf(" idc = %d,",small_7.idc);
				printf(" idc_sub = %d",small_7.idc_sub);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case SMALL_8 :

			printf("contents of small_8:\n");
			printf("--------------------\n");
			exec sql select *
				into :small_8
				from small_8
				order by 1, 2;
			exec sql begin;
				printf(" idc = %d,",small_8.idc);
				printf(" ida = %d",small_8.ida);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case SMALL_9 :

			printf("contents of small_9:\n");
			printf("--------------------\n");
			exec sql select *
				into :small_9
				from small_9
				order by 1;
			exec sql begin;
				printf(" idc = %d",small_9.idc);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case LOG :

			printf("contents of log:\n");
			printf("----------------\n");
			exec sql select *
				into :ruleslog
				from log
				order by 1;
			exec sql begin;
				printf(" entry = %s",ruleslog.entry);
				printf("\n");
			exec sql end;
			printf("----------------\n");

		break;

		case DULL :

			printf("contents of dull:\n");
			printf("-----------------\n");
			exec sql select *
				into :dull
				from dull
				order by 1;
			exec sql begin;
				printf(" dull = %d",dull.dull);
				printf("\n");
			exec sql end;
			printf("-----------------\n");

		break;

		case ALL_TYPES :

			printf("contents of all_types:\n");
			printf("----------------------\n");
			exec sql select *
				into :all_types
				from all_types
				order by 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11;
			exec sql begin;
				printf(" i1 = %d,",all_types.i1);
				printf(" i2 = %d,",all_types.i2);
				printf(" i4 = %d,",all_types.i4);
				printf(" f4 = %f,",all_types.f4);
				printf(" f8 = %f,",all_types.f8);
				printf(" c10 = %s,",all_types.c10);
				printf(" char10 = %s,",all_types.char10);
				printf(" vchar10 = %s,",all_types.vchar10);
				printf(" varchar10 = %s,",all_types.varchar10);
				printf(" d = %s,",all_types.d);
				printf(" m = %f",all_types.m);
				printf("\n");
			exec sql end;
			printf("----------------------\n");

		break;

		default :
			printf("print_tables() error: undefined arg\n");
		break;

	} /* switch */

} /* print_tables() */

/*------------------------------------------------------------------------*/
/*                                                                        */
/*	general procedures                                                */
/*                                                                        */
/*------------------------------------------------------------------------*/

/************************/
/* create_logger_proc() */
/************************/

create_logger_proc()
{
	printf("  drop and create logger db proc...\n");

	exec sql drop procedure logger;

	exec sql create procedure logger (
		txt_buf	varchar(81)	not null with default
	) as
	begin
		message txt_buf;
		insert into log (
			entry
		) values (
			:txt_buf
		);
	end;

	exec sql commit;

} /* create_logger_proc() */

/*****************************/
/* create_loggernomsg_proc() */
/*****************************/

create_loggernomsg_proc()
{
	printf("  drop and create loggernomsg db proc...\n");

	exec sql drop procedure loggernomsg;

	exec sql create procedure loggernomsg (
		txt_buf	varchar(81)	not null with default
	) as
	begin
		insert into log (
			entry
		) values (
			:txt_buf
		);
	end;

	exec sql commit;

} /* create_loggernomsg_proc() */

/***************************/
/* create_lognewold_proc() */
/***************************/

create_lognewold_proc()
{
	printf("  drop and create lognewold db proc...\n");

	exec sql drop procedure lognewold;

	exec sql create procedure lognewold (
		txt_buf	varchar(81)	not null with default,
		oldval	integer		not null with default,
		newval	integer		not null with default
	) as
	declare
		msg_buf	varchar(81)	not null with default;
	begin
		msg_buf = 't: '
			+ txt_buf
			+ ', o: '
			+ varchar(oldval)
			+ ', n: '
			+ varchar(newval);

		message msg_buf;
		insert into log (
			entry
		) values (
			:msg_buf
		);
	end;

	exec sql commit;

} /* create_lognewold_proc() */

/*************************/
/* create_raiserr_proc() */
/*************************/

create_raiserr_proc()
{
	printf("  drop and create raiserr db proc...\n");

	exec sql drop procedure raiserr;

	exec sql create procedure raiserr as
	begin
		message 'raiserr proc';

		raise error 1 'for the heck of it';
	end;

	exec sql commit;

} /* create_raiserr_proc() */

/**************************/
/* create_grclean1_proc() */
/**************************/

create_grclean1_proc()
{
	printf("  drop and create grclean1 db proc...\n");

	exec sql drop procedure grclean1;

	exec sql create procedure grclean1 (
		oldf	integer		not null with default,
		oldt	integer		not null with default,
		newf	integer		not null with default,
		newt	integer		not null with default
	) as
	declare
		msg_buf	varchar(81)	not null with default;
	begin
		msg_buf = 'of: '
			+ varchar(oldf)
			+ ', ot: '
			+ varchar(oldt)
			+ ', nf: '
			+ varchar(newf)
			+ ', nt: '
			+ varchar(newt);

		message msg_buf;

		delete from graph_1 where graph_1.from_node = oldt;
	end;

	exec sql commit;

} /* create_grclean1_proc() */

/**************************/
/* create_grclean2_proc() */
/**************************/

create_grclean2_proc()
{
	printf("  drop and create grclean2 db proc...\n");

	exec sql drop procedure grclean2;

	exec sql create procedure grclean2 (
		oldf	integer		not null with default,
		oldt	integer		not null with default,
		newf	integer		not null with default,
		newt	integer		not null with default
	) as
	declare
		msg_buf	varchar(82)	not null with default;
	begin
		msg_buf = 'of: '
			+ varchar(oldf)
			+ ', ot: '
			+ varchar(oldt)
			+ ', nf: '
			+ varchar(newf)
			+ ', nt: '
			+ varchar(newt);

		message msg_buf;

		delete from graph_2 where graph_2.from_node = oldt;
	end;

	exec sql commit;

} /* create_grclean2_proc() */

/**************************/
/* create_grclean3_proc() */
/**************************/

create_grclean3_proc()
{
	printf("  drop and create grclean3 db proc...\n");

	exec sql drop procedure grclean3;

	exec sql create procedure grclean3 (
		oldf	integer		not null with default,
		oldt	integer		not null with default,
		newf	integer		not null with default,
		newt	integer		not null with default
	) as
	declare
		msg_buf	varchar(82)	not null with default;
	begin
		msg_buf = 'cascade '
			+ 'delete from graph_3 where graph_3.from_node = '
			+ varchar(oldt);

		message msg_buf;

		delete from graph_3 where graph_3.from_node = oldt;
	end;

	exec sql commit;

} /* create_grclean3_proc() */

/***************************/
/* create_delchain1_proc() */
/***************************/

create_delchain1_proc()
{
	printf("  drop and create delchain1 db proc...\n");

	exec sql drop procedure delchain1;

	exec sql create procedure delchain1 (
		target_id	integer		not null with default
	) as
	declare
		msg_buf	varchar(82)	not null with default;
	begin
		msg_buf = 'deleting id '
			+ varchar(target_id)
			+ ' from chain_1';

		message msg_buf;

		delete from chain_1 where chain_1.id = target_id;
	end;

	exec sql commit;

} /* create_delchain1_proc() */

/***************************/
/* create_delchain2_proc() */
/***************************/

create_delchain2_proc()
{
	printf("  drop and create delchain2 db proc...\n");

	exec sql drop procedure delchain2;

	exec sql create procedure delchain2 (
		target_id	integer		not null with default
	) as
	declare
		msg_buf	varchar(82)	not null with default;
	begin
		msg_buf = 'deleting id '
			+ varchar(target_id)
			+ ' from chain_2';

		message msg_buf;

		delete from chain_2 where chain_2.id = target_id;
	end;

	exec sql commit;

} /* create_delchain2_proc() */

/***************************/
/* create_delchain3_proc() */
/***************************/

create_delchain3_proc()
{
	printf("  drop and create delchain3 db proc...\n");

	exec sql drop procedure delchain3;

	exec sql create procedure delchain3 (
		target_id	integer		not null with default
	) as
	declare
		msg_buf	varchar(82)	not null with default;
	begin
		msg_buf = 'deleting id '
			+ varchar(target_id)
			+ ' from chain_3';

		message msg_buf;

		delete from chain_3 where chain_3.id = target_id;
	end;

	exec sql commit;

} /* create_delchain3_proc() */

/************************/
/* create_prallt_proc() */
/************************/

create_prallt_proc()
{
	printf("  drop and create prallt db proc...\n");

	exec sql drop procedure prallt;

	exec sql create procedure prallt (
		chatter		varchar(20)	not null with default,
		i1		i1		not null with default,
		i2		i2		not null with default,
		i4		i4		not null with default,
		f4		f4		not null with default,
		f8		f8		not null with default,
		c10		c(10)		not null with default,
		char10		char(10)	not null with default,
		vchar10		vchar(10)	not null with default,
		varchar10	varchar(10)	not null with default,
		d		date		not null with default,
		m		money		not null with default
	) as
	declare
		msg_buf	varchar(256)	not null with default;
	begin
		msg_buf = chatter
			+ ' i1='
			+ varchar(:i1)
			+ ', i2='
			+ varchar(i2)
			+ ', i4='
			+ varchar(i4)
			+ ', f4='
			+ varchar(f4)
			+ ', f8='
			+ varchar(f8)
			+ ', c10='
			+ varchar(c10)
			+ ', char10='
			+ varchar(char10)
			+ ', vchar10='
			+ varchar(vchar10)
			+ ', varchar10='
			+ varchar(varchar10)
			+ ', d='
			+ varchar(d)
			+ ', m='
			+ varchar(m);

		message msg_buf;

	end;

	exec sql commit;

} /* create_prallt_proc() */

/*------------------------------------------------------------------------*/
/*                                                                        */
/*	general rules                                                     */
/*                                                                        */
/*------------------------------------------------------------------------*/

/*****************************/
/* cr_graph_3_dc_rule_rule() */
/*****************************/

cr_graph_3_dc_rule()
{
	printf("\n");
	printf("  drop and create rule graph_3_dc:\n");
	printf("  (rule may not exist)\n");
	exec sql drop rule graph_3_dc;
	exec sql create rule graph_3_dc after delete on graph_3
		execute procedure grclean3 (
			oldf = old.from_node,
			oldt = old.to_node,
			newf = new.from_node,
			newt = new.to_node
		);
	exec sql commit;

} /* cr_graph_3_dc_rule_rule() */

/*************************/
/* cr_dull_logger_rule() */
/*************************/

cr_dull_logger_rule()
{
	printf("  drop and create rule logger:\n");
	printf("  (rule may not exist)\n");
	exec sql drop rule logger;
	exec sql create rule logger after insert, update, delete on dull
		execute procedure logger (
			txt_buf = 'logger rule'
		);
	printf("\n");

} /* cr_dull_logger_rule() */

/*************************/
/* cr_dull_lognw_rule() */
/*************************/

cr_dull_lognw_rule()
{
	printf("  drop and create rule lognw:\n");
	printf("  (rule may not exist)\n");
	exec sql drop rule lognw;
	exec sql create rule lognw after insert, update, delete on dull
		execute procedure lognewold (
			txt_buf = 'no excitement',
			oldval = old.dull,
			newval = new.dull
		);
	printf("\n");

} /* cr_dull_lognw_rule() */
