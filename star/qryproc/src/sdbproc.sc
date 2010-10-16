/*
**	sdbproc.sc
**
**	a potpurri of functions to be used by the star dbproc tests.
*/

#include <stdio.h>

exec sql include 'ingclude.sc';
exec sql include 'sdbproch.sc';

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

/******************/
/* print_tables() */
/******************/

print_tables(i)
int	i;
{
	switch(i)
	{
		case GRAPH_3:

			printf("contents of graph_3:\n");
			printf("--------------------\n");
			exec sql select *
				into :graph_3
				from graph_3
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_3.from_node);
				printf(" to_node = %d",graph_3_1.to_node);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case GRAPH_3_1 :

			printf("contents of graph_3_1:\n");
			printf("--------------------\n");
			exec sql select *
				into :graph_3_1
				from graph_3_1
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_3_1.from_node);
				printf(" to_node = %d",graph_3_1.to_node);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

		case GRAPH_3_2 :

			printf("contents of graph_3_2:\n");
			printf("--------------------\n");
			exec sql select *
				into :graph_3_2
				from graph_3_2
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_3_2.from_node);
				printf(" to_node = %d",graph_3_2.to_node);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

                case GRAPH_3_3 :

			printf("contents of graph_3_3:\n");
			printf("--------------------\n");
			exec sql select *
				into :graph_3_3
				from graph_3_3
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_3_3.from_node);
				printf(" to_node = %d",graph_3_3.to_node);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

                case GRAPH_3_4 :

			printf("contents of graph_3_4:\n");
			printf("--------------------\n");
			exec sql select *
				into :graph_3_4
				from graph_3_4
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_3_4.from_node);
				printf(" to_node = %d",graph_3_4.to_node);
				printf("\n");
			exec sql end;
			printf("--------------------\n");

		break;

                case GRAPH_3_5 :

			printf("contents of graph_3_5:\n");
			printf("--------------------\n");
			exec sql select *
				into :graph_3_5
				from graph_3_5
				order by 1, 2;
			exec sql begin;
				printf(" from_node = %d,",graph_3_5.from_node);
				printf(" to_node = %d",graph_3_5.to_node);
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

} /* create_logger_proc() */

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

} /* create_grclean3_proc() */

/**************************/
/* create_cascade_proc() */
/**************************/

create_cascade_proc()
{
	exec sql drop procedure sza13_1;

	exec sql create procedure sza13_1 (bar	i4 not null with default) as
	declare
		foo  i4 not null with default;
	begin
		message 'in sza13_1';
		foo = 5000;
		message :foo 'value of local foo';
		message :bar 'value of bar before the reassignment';
		bar = 78900;
		message :bar 'value of bar after the reassignment';
	end;

} /* create_cascade_proc() */

/**************************/
/* create_cascade2_proc() */
/**************************/

create_cascade2_proc()
{

	exec sql drop procedure sza13_2;

	exec sql create procedure sza13_2 (bar	i4 not null with default) as
	declare
		foo  i4 not null with default;
	begin
		message 'in sza13_2';
		foo = 2000;
		message :foo 'value of local foo';
		message :bar 'value of bar before the reassignment';
		bar = 10;
		message :bar 'value of bar after the reassignment';

	        execute procedure sza13_1 (bar = byref(:foo));
		message 'after executing sza13_1';
		message :foo 'value of local foo';
		message :bar 'value of bar';
	end;

} /* create_cascade2_proc() */

/**************************/
/* create_sza11_1_proc() */
/**************************/

create_sza11_1_proc()
{
	exec sql drop procedure sza11_1;
	exec sql create procedure sza11_1 as
	begin
		message 'initial sza11_1';
	end;

} /* create_sza11_1_proc() */


/**************************/
/* create_sza11_2_proc() */
/**************************/

create_sza11_2_proc()
{
	exec sql drop procedure sza11_2;
	exec sql create procedure sza11_2 (
		x           integer		not null with default,
		y           integer             not null with default,
		maxdepth    integer             not null with default) as
	begin
		message 'in sza11_2';
		message x ' <-  x';
		message y ' <-  y';
		x = x + 1;
		y = y + 1;
		if ( y < :maxdepth ) then
		  execute procedure sza11_1 (x = byref(:x),
					     y = :y,
					     maxdepth = :maxdepth);
		  message x ' <-  x';
		  message y ' <-  y';
		endif;
	end;

} /* create_sza11_2_proc() */


/**************************/
/* create_sza11_3_proc() */
/**************************/

create_sza11_3_proc()
{
        exec sql drop procedure sza11_1;
	exec sql create procedure sza11_1 (
		x           integer		not null with default,
		y           integer             not null with default,
		maxdepth    integer             not null with default) as
	begin
		message 'new sza11_1';
		message x ' <-  x';
		message y ' <-  y';
		x = x + 1;
		y = y + 1;
		execute procedure sza11_2 (x = byref(:x), 
					   y = :y,
					   maxdepth = :maxdepth);
		message x ' <-  x';
		message y ' <-  y';
	end;

} /* create_sza11_3_proc() */

/**************************/
/* create_blob_1_1() */
/**************************/

create_blob_1_1()
{

	exec sql drop procedure blob_1_1;

	exec sql create procedure blob_1_1 (
	  col001 i4 not null with default,
	  col002 i4 not null with default,
	  col003 i4 not null with default,
	  col004 i4 not null with default,
	  col005 i4 not null with default,
	  col006 i4 not null with default,
	  col007 i4 not null with default,
	  col008 i4 not null with default,
	  col009 i4 not null with default,
	  col010 i4 not null with default,
	  col011 i4 not null with default,
	  col012 i4 not null with default,
	  col013 i4 not null with default,
	  col014 i4 not null with default,
	  col015 i4 not null with default,
	  col016 i4 not null with default,
	  col017 i4 not null with default,
	  col018 i4 not null with default,
	  col019 i4 not null with default,
	  col020 i4 not null with default,
	  col021 i4 not null with default,
	  col022 i4 not null with default,
	  col023 i4 not null with default,
	  col024 i4 not null with default,
	  col025 i4 not null with default,
	  col026 i4 not null with default,
	  col027 i4 not null with default,
	  col028 i4 not null with default,
	  col029 i4 not null with default,
	  col030 i4 not null with default,
	  col031 i4 not null with default,
	  col032 i4 not null with default,
	  col033 i4 not null with default,
	  col034 i4 not null with default,
	  col035 i4 not null with default,
	  col036 i4 not null with default,
	  col037 i4 not null with default,
	  col038 i4 not null with default,
	  col039 i4 not null with default,
	  col040 i4 not null with default,
	  col041 i4 not null with default,
	  col042 i4 not null with default,
	  col043 i4 not null with default,
	  col044 i4 not null with default,
	  col045 i4 not null with default,
	  col046 i4 not null with default,
	  col047 i4 not null with default,
	  col048 i4 not null with default,
	  col049 i4 not null with default,
	  col050 i4 not null with default,
	  col051 i4 not null with default,
	  col052 i4 not null with default,
	  col053 i4 not null with default,
	  col054 i4 not null with default,
	  col055 i4 not null with default,
	  col056 i4 not null with default,
	  col057 i4 not null with default,
	  col058 i4 not null with default,
	  col059 i4 not null with default,
	  col060 i4 not null with default,
	  col061 i4 not null with default,
	  col062 i4 not null with default,
	  col063 i4 not null with default,
	  col064 i4 not null with default,
	  col065 i4 not null with default,
	  col066 i4 not null with default,
	  col067 i4 not null with default,
	  col068 i4 not null with default,
	  col069 i4 not null with default,
	  col070 i4 not null with default,
	  col071 i4 not null with default,
	  col072 i4 not null with default,
	  col073 i4 not null with default,
	  col074 i4 not null with default,
	  col075 i4 not null with default,
	  col076 i4 not null with default,
	  col077 i4 not null with default,
	  col078 i4 not null with default,
	  col079 i4 not null with default,
	  col080 i4 not null with default,
	  col081 i4 not null with default,
	  col082 i4 not null with default,
	  col083 i4 not null with default,
	  col084 i4 not null with default,
	  col085 i4 not null with default,
	  col086 i4 not null with default,
	  col087 i4 not null with default,
	  col088 i4 not null with default,
	  col089 i4 not null with default,
	  col090 i4 not null with default,
	  col091 i4 not null with default,
	  col092 i4 not null with default,
	  col093 i4 not null with default,
	  col094 i4 not null with default,
	  col095 i4 not null with default,
	  col096 i4 not null with default,
	  col097 i4 not null with default,
	  col098 i4 not null with default,
	  col099 i4 not null with default,
	  col100 i4 not null with default,
	  col101 i4 not null with default,
	  col102 i4 not null with default,
	  col103 i4 not null with default,
	  col104 i4 not null with default,
	  col105 i4 not null with default,
	  col106 i4 not null with default,
	  col107 i4 not null with default,
	  col108 i4 not null with default,
	  col109 i4 not null with default,
	  col110 i4 not null with default,
	  col111 i4 not null with default,
	  col112 i4 not null with default,
	  col113 i4 not null with default,
	  col114 i4 not null with default,
	  col115 i4 not null with default,
	  col116 i4 not null with default,
	  col117 i4 not null with default,
	  col118 i4 not null with default,
	  col119 i4 not null with default,
	  col120 i4 not null with default,
	  col121 i4 not null with default,
	  col122 i4 not null with default,
	  col123 i4 not null with default,
	  col124 i4 not null with default,
	  col125 i4 not null with default,
	  col126 i4 not null with default,
	  col127 i4 not null with default,
	  col128 i4 not null with default,
	  col129 i4 not null with default,
	  col130 i4 not null with default,
	  col131 i4 not null with default,
	  col132 i4 not null with default,
	  col133 i4 not null with default,
	  col134 i4 not null with default,
	  col135 i4 not null with default,
	  col136 i4 not null with default,
	  col137 i4 not null with default,
	  col138 i4 not null with default,
	  col139 i4 not null with default,
	  col140 i4 not null with default,
	  col141 i4 not null with default,
	  col142 i4 not null with default,
	  col143 i4 not null with default,
	  col144 i4 not null with default,
	  col145 i4 not null with default,
	  col146 i4 not null with default,
	  col147 i4 not null with default,
	  col148 i4 not null with default,
	  col149 i4 not null with default,
	  col150 i4 not null with default,
	  col151 i4 not null with default,
	  col152 i4 not null with default,
	  col153 i4 not null with default,
	  col154 i4 not null with default,
	  col155 i4 not null with default,
	  col156 i4 not null with default,
	  col157 i4 not null with default,
	  col158 i4 not null with default,
	  col159 i4 not null with default,
	  col160 i4 not null with default,
	  col161 i4 not null with default,
	  col162 i4 not null with default,
	  col163 i4 not null with default,
	  col164 i4 not null with default,
	  col165 i4 not null with default,
	  col166 i4 not null with default,
	  col167 i4 not null with default,
	  col168 i4 not null with default,
	  col169 i4 not null with default,
	  col170 i4 not null with default,
	  col171 i4 not null with default,
	  col172 i4 not null with default,
	  col173 i4 not null with default,
	  col174 i4 not null with default,
	  col175 i4 not null with default,
	  col176 i4 not null with default,
	  col177 i4 not null with default,
	  col178 i4 not null with default,
	  col179 i4 not null with default,
	  col180 i4 not null with default,
	  col181 i4 not null with default,
	  col182 i4 not null with default,
	  col183 i4 not null with default,
	  col184 i4 not null with default,
	  col185 i4 not null with default,
	  col186 i4 not null with default,
	  col187 i4 not null with default,
	  col188 i4 not null with default,
	  col189 i4 not null with default,
	  col190 i4 not null with default,
	  col191 i4 not null with default,
	  col192 i4 not null with default,
	  col193 i4 not null with default,
	  col194 i4 not null with default,
	  col195 i4 not null with default,
	  col196 i4 not null with default,
	  col197 i4 not null with default,
	  col198 i4 not null with default,
	  col199 i4 not null with default,
	  col200 i4 not null with default,
	  col201 i4 not null with default,
	  col202 i4 not null with default,
	  col203 i4 not null with default,
	  col204 i4 not null with default,
	  col205 i4 not null with default,
	  col206 i4 not null with default,
	  col207 i4 not null with default,
	  col208 i4 not null with default,
	  col209 i4 not null with default,
	  col210 i4 not null with default,
	  col211 i4 not null with default,
	  col212 i4 not null with default,
	  col213 i4 not null with default,
	  col214 i4 not null with default,
	  col215 i4 not null with default,
	  col216 i4 not null with default,
	  col217 i4 not null with default,
	  col218 i4 not null with default,
	  col219 i4 not null with default,
	  col220 i4 not null with default,
	  col221 i4 not null with default,
	  col222 i4 not null with default,
	  col223 i4 not null with default,
	  col224 i4 not null with default,
	  col225 i4 not null with default,
	  col226 i4 not null with default,
	  col227 i4 not null with default,
	  col228 i4 not null with default,
	  col229 i4 not null with default,
	  col230 i4 not null with default,
	  col231 i4 not null with default,
	  col232 i4 not null with default,
	  col233 i4 not null with default,
	  col234 i4 not null with default,
	  col235 i4 not null with default,
	  col236 i4 not null with default,
	  col237 i4 not null with default,
	  col238 i4 not null with default,
	  col239 i4 not null with default,
	  col240 i4 not null with default,
	  col241 i4 not null with default,
	  col242 i4 not null with default,
	  col243 i4 not null with default,
	  col244 i4 not null with default,
	  col245 i4 not null with default,
	  col246 i4 not null with default,
	  col247 i4 not null with default,
	  col248 i4 not null with default,
	  col249 i4 not null with default,
	  col250 i4 not null with default,
	  col251 i4 not null with default,
	  col252 i4 not null with default,
	  col253 i4 not null with default,
	  col254 i4 not null with default,
	  col255 i4 not null with default,
	  col256 i4 not null with default,
	  col257 i4 not null with default,
	  col258 i4 not null with default,
	  col259 i4 not null with default,
	  col260 i4 not null with default,
	  col261 i4 not null with default,
	  col262 i4 not null with default,
	  col263 i4 not null with default,
	  col264 i4 not null with default,
	  col265 i4 not null with default,
	  col266 i4 not null with default,
	  col267 i4 not null with default,
	  col268 i4 not null with default,
	  col269 i4 not null with default,
	  col270 i4 not null with default,
	  col271 i4 not null with default,
	  col272 i4 not null with default,
	  col273 i4 not null with default,
	  col274 i4 not null with default,
	  col275 i4 not null with default,
	  col276 i4 not null with default,
	  col277 i4 not null with default,
	  col278 i4 not null with default,
	  col279 i4 not null with default,
	  col280 i4 not null with default,
	  col281 i4 not null with default,
	  col282 i4 not null with default,
	  col283 i4 not null with default,
	  col284 i4 not null with default,
	  col285 i4 not null with default,
	  col286 i4 not null with default,
	  col287 i4 not null with default,
	  col288 i4 not null with default,
	  col289 i4 not null with default,
	  col290 i4 not null with default,
	  col291 i4 not null with default,
	  col292 i4 not null with default,
	  col293 i4 not null with default,
	  col294 i4 not null with default,
	  col295 i4 not null with default,
	  col296 i4 not null with default,
	  col297 i4 not null with default,
	  col298 i4 not null with default,
	  col299 i4 not null with default,
	  col300 i4 not null with default) as

	begin

	  message col001;
	  col001  = col001 +1;
	  message col001;
	  col002  = col002 +1;
	  col003  = col003 +1;
	  col004  = col004 + 1;
	  col005  = col005 + 1;
	  col006  = col006 + 1;
	  col007  = col007 + 1;
	  col008  = col008 + 1;
	  col009  = col009 + 1;
	  col010  = col010 + 1;
	  col011  = col011 + 1;
	  col012  = col012 + 1;
	  col013  = col013 + 1;
	  col014  = col014 + 1;
	  col015  = col015 + 1;
	  col016  = col016 + 1;
	  col017  = col017 + 1;
	  col018  = col018 + 1;
	  col019  = col019 + 1;
	  col020  = col020 + 1;
	  col021  = col021 + 1;
	  col022  = col022 + 1;
	  col023  = col023 + 1;
	  col024  = col024 + 1;
	  col025  = col025 + 1;
	  col026  = col026 + 1;
	  col027  = col027 + 1;
	  col028  = col028 + 1;
	  col029  = col029 + 1;
	  col030  = col030 + 1;
	  col031  = col031 + 1;
	  col032  = col032 + 1;
	  col033  = col033 + 1;
	  col034  = col034 + 1;
	  col035  = col035 + 1;
	  col036  = col036 + 1;
	  col037  = col037 + 1;
	  col038  = col038 + 1;
	  col039  = col039 + 1;
	  col040  = col040 + 1;
	  col041  = col041 + 1;
	  col042  = col042 + 1;
	  col043  = col043 + 1;
	  col044  = col044 + 1;
	  col045  = col045 + 1;
	  col046  = col046 + 1;
	  col047  = col047 + 1;
	  col048  = col048 + 1;
	  col049  = col049 + 1;
	  col050  = col050 + 1;
	  col051  = col051 + 1;
	  col052  = col052 + 1;
	  col053  = col053 + 1;
	  col054  = col054 + 1;
	  col055  = col055 + 1;
	  col056  = col056 + 1;
	  col057  = col057 + 1;
	  col058  = col058 + 1;
	  col059  = col059 + 1;
	  col060  = col060 + 1;
	  col061  = col061 + 1;
	  col062  = col062 + 1;
	  col063  = col063 + 1;
	  col064  = col064 + 1;
	  col065  = col065 + 1;
	  col066  = col066 + 1;
	  col067  = col067 + 1;
	  col068  = col068 + 1;
	  col069  = col069 + 1;
	  col070  = col070 + 1;
	  col071  = col071 + 1;
	  col072  = col072 + 1;
	  col073  = col073 + 1;
	  col074  = col074 + 1;
	  col075  = col075 + 1;
	  col076  = col076 + 1;
	  col077  = col077 + 1;
	  col078  = col078 + 1;
	  col079  = col079 + 1;
	  col080  = col080 + 1;
	  col081  = col081 + 1;
	  col082  = col082 + 1;
	  col083  = col083 + 1;
	  col084  = col084 + 1;
	  col085  = col085 + 1;
	  col086  = col086 + 1;
	  col087  = col087 + 1;
	  col088  = col088 + 1;
	  col089  = col089 + 1;
	  col090  = col090 + 1;
	  col091  = col091 + 1;
	  col092  = col092 + 1;
	  col093  = col093 + 1;
	  col094  = col094 + 1;
	  col095  = col095 + 1;
	  col096  = col096 + 1;
	  col097  = col097 + 1;
	  col098  = col098 + 1;
	  col099  = col099 + 1;
	  col100  = col100 + 1;
	  col101  = col101 + 1;
	  col102  = col102 + 1;
	  col103  = col103 + 1;
	  col104  = col104 + 1;
	  col105  = col105 + 1;
	  col106  = col106 + 1;
	  col107  = col107 + 1;
	  col108  = col108 + 1;
	  col109  = col109 + 1;
	  col110  = col110 + 1;
	  col111  = col111 + 1;
	  col112  = col112 + 1;
	  col113  = col113 + 1;
	  col114  = col114 + 1;
	  col115  = col115 + 1;
	  col116  = col116 + 1;
	  col117  = col117 + 1;
	  col118  = col118 + 1;
	  col119  = col119 + 1;
	  col120  = col120 + 1;
	  col121  = col121 + 1;
	  col122  = col122 + 1;
	  col123  = col123 + 1;
	  col124  = col124 + 1;
	  col125  = col125 + 1;
	  col126  = col126 + 1;
	  col127  = col127 + 1;
	  col128  = col128 + 1;
	  col129  = col129 + 1;
	  col130  = col130 + 1;
	  col131  = col131 + 1;
	  col132  = col132 + 1;
	  col133  = col133 + 1;
	  col134  = col134 + 1;
	  col135  = col135 + 1;
	  col136  = col136 + 1;
	  col137  = col137 + 1;
	  col138  = col138 + 1;
	  col139  = col139 + 1;
	  col140  = col140 + 1;
	  col141  = col141 + 1;
	  col142  = col142 + 1;
	  col143  = col143 + 1;
	  col144  = col144 + 1;
	  col145  = col145 + 1;
	  col146  = col146 + 1;
	  col147  = col147 + 1;
	  col148  = col148 + 1;
	  col149  = col149 + 1;
	  col150  = col150 + 1;
	  col151  = col151 + 1;
	  col152  = col152 + 1;
	  col153  = col153 + 1;
	  col154  = col154 + 1;
	  col155  = col155 + 1;
	  col156  = col156 + 1;
	  col157  = col157 + 1;
	  col158  = col158 + 1;
	  col159  = col159 + 1;
	  col160  = col160 + 1;
	  col161  = col161 + 1;
	  col162  = col162 + 1;
	  col163  = col163 + 1;
	  col164  = col164 + 1;
	  col165  = col165 + 1;
	  col166  = col166 + 1;
	  col167  = col167 + 1;
	  col168  = col168 + 1;
	  col169  = col169 + 1;
	  col170  = col170 + 1;
	  col171  = col171 + 1;
	  col172  = col172 + 1;
	  col173  = col173 + 1;
	  col174  = col174 + 1;
	  col175  = col175 + 1;
	  col176  = col176 + 1;
	  col177  = col177 + 1;
	  col178  = col178 + 1;
	  col179  = col179 + 1;
	  col180  = col180 + 1;
	  col181  = col181 + 1;
	  col182  = col182 + 1;
	  col183  = col183 + 1;
	  col184  = col184 + 1;
	  col185  = col185 + 1;
	  col186  = col186 + 1;
	  col187  = col187 + 1;
	  col188  = col188 + 1;
	  col189  = col189 + 1;
	  col190  = col190 + 1;
	  col191  = col191 + 1;
	  col192  = col192 + 1;
	  col193  = col193 + 1;
	  col194  = col194 + 1;
	  col195  = col195 + 1;
	  col196  = col196 + 1;
	  col197  = col197 + 1;
	  col198  = col198 + 1;
	  col199  = col199 + 1;
	  col200  = col200 + 1;
	  col201  = col201 + 1;
	  col202  = col202 + 1;
	  col203  = col203 + 1;
	  col204  = col204 + 1;
	  col205  = col205 + 1;
	  col206  = col206 + 1;
	  col207  = col207 + 1;
	  col208  = col208 + 1;
	  col209  = col209 + 1;
	  col210  = col210 + 1;
	  col211  = col211 + 1;
	  col212  = col212 + 1;
	  col213  = col213 + 1;
	  col214  = col214 + 1;
	  col215  = col215 + 1;
	  col216  = col216 + 1;
	  col217  = col217 + 1;
	  col218  = col218 + 1;
	  col219  = col219 + 1;
	  col220  = col220 + 1;
	  col221  = col221 + 1;
	  col222  = col222 + 1;
	  col223  = col223 + 1;
	  col224  = col224 + 1;
	  col225  = col225 + 1;
	  col226  = col226 + 1;
	  col227  = col227 + 1;
	  col228  = col228 + 1;
	  col229  = col229 + 1;
	  col230  = col230 + 1;
	  col231  = col231 + 1;
	  col232  = col232 + 1;
	  col233  = col233 + 1;
	  col234  = col234 + 1;
	  col235  = col235 + 1;
	  col236  = col236 + 1;
	  col237  = col237 + 1;
	  col238  = col238 + 1;
	  col239  = col239 + 1;
	  col240  = col240 + 1;
	  col241  = col241 + 1;
	  col242  = col242 + 1;
	  col243  = col243 + 1;
	  col244  = col244 + 1;
	  col245  = col245 + 1;
	  col246  = col246 + 1;
	  col247  = col247 + 1;
	  col248  = col248 + 1;
	  col249  = col249 + 1;
	  col250  = col250 + 1;
	  col251  = col251 + 1;
	  col252  = col252 + 1;
	  col253  = col253 + 1;
	  col254  = col254 + 1;
	  col255  = col255 + 1;
	  col256  = col256 + 1;
	  col257  = col257 + 1;
	  col258  = col258 + 1;
	  col259  = col259 + 1;
	  col260  = col260 + 1;
	  col261  = col261 + 1;
	  col262  = col262 + 1;
	  col263  = col263 + 1;
	  col264  = col264 + 1;
	  col265  = col265 + 1;
	  col266  = col266 + 1;
	  col267  = col267 + 1;
	  col268  = col268 + 1;
	  col269  = col269 + 1;
	  col270  = col270 + 1;
	  col271  = col271 + 1;
	  col272  = col272 + 1;
	  col273  = col273 + 1;
	  col274  = col274 + 1;
	  col275  = col275 + 1;
	  col276  = col276 + 1;
	  col277  = col277 + 1;
	  col278  = col278 + 1;
	  col279  = col279 + 1;
	  col280  = col280 + 1;
	  col281  = col281 + 1;
	  col282  = col282 + 1;
	  col283  = col283 + 1;
	  col284  = col284 + 1;
	  col285  = col285 + 1;
	  col286  = col286 + 1;
	  col287  = col287 + 1;
	  col288  = col288 + 1;
	  col289  = col289 + 1;
	  col290  = col290 + 1;
	  col291  = col291 + 1;
	  col292  = col292 + 1;
	  col293  = col293 + 1;
	  col294  = col294 + 1;
	  col295  = col295 + 1;
	  col296  = col296 + 1;
	  col297  = col297 + 1;
	  col298  = col298 + 1;
	  col299  = col299 + 1;
	  message col300;
	  col300  = col300 + 1;
	  message col300;

      end;
} /* create_blob_1_1() */

/**************************/
/* create_blob_1_2() */
/**************************/

create_blob_1_2()
{

	exec sql drop procedure blob_1_2;

	exec sql create procedure blob_1_2 (
	  col001 i4 not null with default,
	  col002 i4 not null with default,
	  col003 i4 not null with default,
	  col004 i4 not null with default,
	  col005 i4 not null with default,
	  col006 i4 not null with default,
	  col007 i4 not null with default,
	  col008 i4 not null with default,
	  col009 i4 not null with default,
	  col010 i4 not null with default,
	  col011 i4 not null with default,
	  col012 i4 not null with default,
	  col013 i4 not null with default,
	  col014 i4 not null with default,
	  col015 i4 not null with default,
	  col016 i4 not null with default,
	  col017 i4 not null with default,
	  col018 i4 not null with default,
	  col019 i4 not null with default,
	  col020 i4 not null with default,
	  col021 i4 not null with default,
	  col022 i4 not null with default,
	  col023 i4 not null with default,
	  col024 i4 not null with default,
	  col025 i4 not null with default,
	  col026 i4 not null with default,
	  col027 i4 not null with default,
	  col028 i4 not null with default,
	  col029 i4 not null with default,
	  col030 i4 not null with default,
	  col031 i4 not null with default,
	  col032 i4 not null with default,
	  col033 i4 not null with default,
	  col034 i4 not null with default,
	  col035 i4 not null with default,
	  col036 i4 not null with default,
	  col037 i4 not null with default,
	  col038 i4 not null with default,
	  col039 i4 not null with default,
	  col040 i4 not null with default,
	  col041 i4 not null with default,
	  col042 i4 not null with default,
	  col043 i4 not null with default,
	  col044 i4 not null with default,
	  col045 i4 not null with default,
	  col046 i4 not null with default,
	  col047 i4 not null with default,
	  col048 i4 not null with default,
	  col049 i4 not null with default,
	  col050 i4 not null with default,
	  col051 i4 not null with default,
	  col052 i4 not null with default,
	  col053 i4 not null with default,
	  col054 i4 not null with default,
	  col055 i4 not null with default,
	  col056 i4 not null with default,
	  col057 i4 not null with default,
	  col058 i4 not null with default,
	  col059 i4 not null with default,
	  col060 i4 not null with default,
	  col061 i4 not null with default,
	  col062 i4 not null with default,
	  col063 i4 not null with default,
	  col064 i4 not null with default,
	  col065 i4 not null with default,
	  col066 i4 not null with default,
	  col067 i4 not null with default,
	  col068 i4 not null with default,
	  col069 i4 not null with default,
	  col070 i4 not null with default,
	  col071 i4 not null with default,
	  col072 i4 not null with default,
	  col073 i4 not null with default,
	  col074 i4 not null with default,
	  col075 i4 not null with default,
	  col076 i4 not null with default,
	  col077 i4 not null with default,
	  col078 i4 not null with default,
	  col079 i4 not null with default,
	  col080 i4 not null with default,
	  col081 i4 not null with default,
	  col082 i4 not null with default,
	  col083 i4 not null with default,
	  col084 i4 not null with default,
	  col085 i4 not null with default,
	  col086 i4 not null with default,
	  col087 i4 not null with default,
	  col088 i4 not null with default,
	  col089 i4 not null with default,
	  col090 i4 not null with default,
	  col091 i4 not null with default,
	  col092 i4 not null with default,
	  col093 i4 not null with default,
	  col094 i4 not null with default,
	  col095 i4 not null with default,
	  col096 i4 not null with default,
	  col097 i4 not null with default,
	  col098 i4 not null with default,
	  col099 i4 not null with default,
	  col100 i4 not null with default,
	  col101 i4 not null with default,
	  col102 i4 not null with default,
	  col103 i4 not null with default,
	  col104 i4 not null with default,
	  col105 i4 not null with default,
	  col106 i4 not null with default,
	  col107 i4 not null with default,
	  col108 i4 not null with default,
	  col109 i4 not null with default,
	  col110 i4 not null with default,
	  col111 i4 not null with default,
	  col112 i4 not null with default,
	  col113 i4 not null with default,
	  col114 i4 not null with default,
	  col115 i4 not null with default,
	  col116 i4 not null with default,
	  col117 i4 not null with default,
	  col118 i4 not null with default,
	  col119 i4 not null with default,
	  col120 i4 not null with default,
	  col121 i4 not null with default,
	  col122 i4 not null with default,
	  col123 i4 not null with default,
	  col124 i4 not null with default,
	  col125 i4 not null with default,
	  col126 i4 not null with default,
	  col127 i4 not null with default,
	  col128 i4 not null with default,
	  col129 i4 not null with default,
	  col130 i4 not null with default,
	  col131 i4 not null with default,
	  col132 i4 not null with default,
	  col133 i4 not null with default,
	  col134 i4 not null with default,
	  col135 i4 not null with default,
	  col136 i4 not null with default,
	  col137 i4 not null with default,
	  col138 i4 not null with default,
	  col139 i4 not null with default,
	  col140 i4 not null with default,
	  col141 i4 not null with default,
	  col142 i4 not null with default,
	  col143 i4 not null with default,
	  col144 i4 not null with default,
	  col145 i4 not null with default,
	  col146 i4 not null with default,
	  col147 i4 not null with default,
	  col148 i4 not null with default,
	  col149 i4 not null with default,
	  col150 i4 not null with default,
	  col151 i4 not null with default,
	  col152 i4 not null with default,
	  col153 i4 not null with default,
	  col154 i4 not null with default,
	  col155 i4 not null with default,
	  col156 i4 not null with default,
	  col157 i4 not null with default,
	  col158 i4 not null with default,
	  col159 i4 not null with default,
	  col160 i4 not null with default,
	  col161 i4 not null with default,
	  col162 i4 not null with default,
	  col163 i4 not null with default,
	  col164 i4 not null with default,
	  col165 i4 not null with default,
	  col166 i4 not null with default,
	  col167 i4 not null with default,
	  col168 i4 not null with default,
	  col169 i4 not null with default,
	  col170 i4 not null with default,
	  col171 i4 not null with default,
	  col172 i4 not null with default,
	  col173 i4 not null with default,
	  col174 i4 not null with default,
	  col175 i4 not null with default,
	  col176 i4 not null with default,
	  col177 i4 not null with default,
	  col178 i4 not null with default,
	  col179 i4 not null with default,
	  col180 i4 not null with default,
	  col181 i4 not null with default,
	  col182 i4 not null with default,
	  col183 i4 not null with default,
	  col184 i4 not null with default,
	  col185 i4 not null with default,
	  col186 i4 not null with default,
	  col187 i4 not null with default,
	  col188 i4 not null with default,
	  col189 i4 not null with default,
	  col190 i4 not null with default,
	  col191 i4 not null with default,
	  col192 i4 not null with default,
	  col193 i4 not null with default,
	  col194 i4 not null with default,
	  col195 i4 not null with default,
	  col196 i4 not null with default,
	  col197 i4 not null with default,
	  col198 i4 not null with default,
	  col199 i4 not null with default,
	  col200 i4 not null with default,
	  col201 i4 not null with default,
	  col202 i4 not null with default,
	  col203 i4 not null with default,
	  col204 i4 not null with default,
	  col205 i4 not null with default,
	  col206 i4 not null with default,
	  col207 i4 not null with default,
	  col208 i4 not null with default,
	  col209 i4 not null with default,
	  col210 i4 not null with default,
	  col211 i4 not null with default,
	  col212 i4 not null with default,
	  col213 i4 not null with default,
	  col214 i4 not null with default,
	  col215 i4 not null with default,
	  col216 i4 not null with default,
	  col217 i4 not null with default,
	  col218 i4 not null with default,
	  col219 i4 not null with default,
	  col220 i4 not null with default,
	  col221 i4 not null with default,
	  col222 i4 not null with default,
	  col223 i4 not null with default,
	  col224 i4 not null with default,
	  col225 i4 not null with default,
	  col226 i4 not null with default,
	  col227 i4 not null with default,
	  col228 i4 not null with default,
	  col229 i4 not null with default,
	  col230 i4 not null with default,
	  col231 i4 not null with default,
	  col232 i4 not null with default,
	  col233 i4 not null with default,
	  col234 i4 not null with default,
	  col235 i4 not null with default,
	  col236 i4 not null with default,
	  col237 i4 not null with default,
	  col238 i4 not null with default,
	  col239 i4 not null with default,
	  col240 i4 not null with default,
	  col241 i4 not null with default,
	  col242 i4 not null with default,
	  col243 i4 not null with default,
	  col244 i4 not null with default,
	  col245 i4 not null with default,
	  col246 i4 not null with default,
	  col247 i4 not null with default,
	  col248 i4 not null with default,
	  col249 i4 not null with default,
	  col250 i4 not null with default,
	  col251 i4 not null with default,
	  col252 i4 not null with default,
	  col253 i4 not null with default,
	  col254 i4 not null with default,
	  col255 i4 not null with default,
	  col256 i4 not null with default,
	  col257 i4 not null with default,
	  col258 i4 not null with default,
	  col259 i4 not null with default,
	  col260 i4 not null with default,
	  col261 i4 not null with default,
	  col262 i4 not null with default,
	  col263 i4 not null with default,
	  col264 i4 not null with default,
	  col265 i4 not null with default,
	  col266 i4 not null with default,
	  col267 i4 not null with default,
	  col268 i4 not null with default,
	  col269 i4 not null with default,
	  col270 i4 not null with default,
	  col271 i4 not null with default,
	  col272 i4 not null with default,
	  col273 i4 not null with default,
	  col274 i4 not null with default,
	  col275 i4 not null with default,
	  col276 i4 not null with default,
	  col277 i4 not null with default,
	  col278 i4 not null with default,
	  col279 i4 not null with default,
	  col280 i4 not null with default,
	  col281 i4 not null with default,
	  col282 i4 not null with default,
	  col283 i4 not null with default,
	  col284 i4 not null with default,
	  col285 i4 not null with default,
	  col286 i4 not null with default,
	  col287 i4 not null with default,
	  col288 i4 not null with default,
	  col289 i4 not null with default,
	  col290 i4 not null with default,
	  col291 i4 not null with default,
	  col292 i4 not null with default,
	  col293 i4 not null with default,
	  col294 i4 not null with default,
	  col295 i4 not null with default,
	  col296 i4 not null with default,
	  col297 i4 not null with default,
	  col298 i4 not null with default,
	  col299 i4 not null with default,
	  col300 i4 not null with default,
	  col301 i4 not null with default) as

	begin

	  message col001;
	  col001  = col001 +1;
	  message col001;
	  col002  = col002 +1;
	  col003  = col003 +1;
	  col004  = col004 + 1;
	  col005  = col005 + 1;
	  col006  = col006 + 1;
	  col007  = col007 + 1;
	  col008  = col008 + 1;
	  col009  = col009 + 1;
	  col010  = col010 + 1;
	  col011  = col011 + 1;
	  col012  = col012 + 1;
	  col013  = col013 + 1;
	  col014  = col014 + 1;
	  col015  = col015 + 1;
	  col016  = col016 + 1;
	  col017  = col017 + 1;
	  col018  = col018 + 1;
	  col019  = col019 + 1;
	  col020  = col020 + 1;
	  col021  = col021 + 1;
	  col022  = col022 + 1;
	  col023  = col023 + 1;
	  col024  = col024 + 1;
	  col025  = col025 + 1;
	  col026  = col026 + 1;
	  col027  = col027 + 1;
	  col028  = col028 + 1;
	  col029  = col029 + 1;
	  col030  = col030 + 1;
	  col031  = col031 + 1;
	  col032  = col032 + 1;
	  col033  = col033 + 1;
	  col034  = col034 + 1;
	  col035  = col035 + 1;
	  col036  = col036 + 1;
	  col037  = col037 + 1;
	  col038  = col038 + 1;
	  col039  = col039 + 1;
	  col040  = col040 + 1;
	  col041  = col041 + 1;
	  col042  = col042 + 1;
	  col043  = col043 + 1;
	  col044  = col044 + 1;
	  col045  = col045 + 1;
	  col046  = col046 + 1;
	  col047  = col047 + 1;
	  col048  = col048 + 1;
	  col049  = col049 + 1;
	  col050  = col050 + 1;
	  col051  = col051 + 1;
	  col052  = col052 + 1;
	  col053  = col053 + 1;
	  col054  = col054 + 1;
	  col055  = col055 + 1;
	  col056  = col056 + 1;
	  col057  = col057 + 1;
	  col058  = col058 + 1;
	  col059  = col059 + 1;
	  col060  = col060 + 1;
	  col061  = col061 + 1;
	  col062  = col062 + 1;
	  col063  = col063 + 1;
	  col064  = col064 + 1;
	  col065  = col065 + 1;
	  col066  = col066 + 1;
	  col067  = col067 + 1;
	  col068  = col068 + 1;
	  col069  = col069 + 1;
	  col070  = col070 + 1;
	  col071  = col071 + 1;
	  col072  = col072 + 1;
	  col073  = col073 + 1;
	  col074  = col074 + 1;
	  col075  = col075 + 1;
	  col076  = col076 + 1;
	  col077  = col077 + 1;
	  col078  = col078 + 1;
	  col079  = col079 + 1;
	  col080  = col080 + 1;
	  col081  = col081 + 1;
	  col082  = col082 + 1;
	  col083  = col083 + 1;
	  col084  = col084 + 1;
	  col085  = col085 + 1;
	  col086  = col086 + 1;
	  col087  = col087 + 1;
	  col088  = col088 + 1;
	  col089  = col089 + 1;
	  col090  = col090 + 1;
	  col091  = col091 + 1;
	  col092  = col092 + 1;
	  col093  = col093 + 1;
	  col094  = col094 + 1;
	  col095  = col095 + 1;
	  col096  = col096 + 1;
	  col097  = col097 + 1;
	  col098  = col098 + 1;
	  col099  = col099 + 1;
	  col100  = col100 + 1;
	  col101  = col101 + 1;
	  col102  = col102 + 1;
	  col103  = col103 + 1;
	  col104  = col104 + 1;
	  col105  = col105 + 1;
	  col106  = col106 + 1;
	  col107  = col107 + 1;
	  col108  = col108 + 1;
	  col109  = col109 + 1;
	  col110  = col110 + 1;
	  col111  = col111 + 1;
	  col112  = col112 + 1;
	  col113  = col113 + 1;
	  col114  = col114 + 1;
	  col115  = col115 + 1;
	  col116  = col116 + 1;
	  col117  = col117 + 1;
	  col118  = col118 + 1;
	  col119  = col119 + 1;
	  col120  = col120 + 1;
	  col121  = col121 + 1;
	  col122  = col122 + 1;
	  col123  = col123 + 1;
	  col124  = col124 + 1;
	  col125  = col125 + 1;
	  col126  = col126 + 1;
	  col127  = col127 + 1;
	  col128  = col128 + 1;
	  col129  = col129 + 1;
	  col130  = col130 + 1;
	  col131  = col131 + 1;
	  col132  = col132 + 1;
	  col133  = col133 + 1;
	  col134  = col134 + 1;
	  col135  = col135 + 1;
	  col136  = col136 + 1;
	  col137  = col137 + 1;
	  col138  = col138 + 1;
	  col139  = col139 + 1;
	  col140  = col140 + 1;
	  col141  = col141 + 1;
	  col142  = col142 + 1;
	  col143  = col143 + 1;
	  col144  = col144 + 1;
	  col145  = col145 + 1;
	  col146  = col146 + 1;
	  col147  = col147 + 1;
	  col148  = col148 + 1;
	  col149  = col149 + 1;
	  col150  = col150 + 1;
	  col151  = col151 + 1;
	  col152  = col152 + 1;
	  col153  = col153 + 1;
	  col154  = col154 + 1;
	  col155  = col155 + 1;
	  col156  = col156 + 1;
	  col157  = col157 + 1;
	  col158  = col158 + 1;
	  col159  = col159 + 1;
	  col160  = col160 + 1;
	  col161  = col161 + 1;
	  col162  = col162 + 1;
	  col163  = col163 + 1;
	  col164  = col164 + 1;
	  col165  = col165 + 1;
	  col166  = col166 + 1;
	  col167  = col167 + 1;
	  col168  = col168 + 1;
	  col169  = col169 + 1;
	  col170  = col170 + 1;
	  col171  = col171 + 1;
	  col172  = col172 + 1;
	  col173  = col173 + 1;
	  col174  = col174 + 1;
	  col175  = col175 + 1;
	  col176  = col176 + 1;
	  col177  = col177 + 1;
	  col178  = col178 + 1;
	  col179  = col179 + 1;
	  col180  = col180 + 1;
	  col181  = col181 + 1;
	  col182  = col182 + 1;
	  col183  = col183 + 1;
	  col184  = col184 + 1;
	  col185  = col185 + 1;
	  col186  = col186 + 1;
	  col187  = col187 + 1;
	  col188  = col188 + 1;
	  col189  = col189 + 1;
	  col190  = col190 + 1;
	  col191  = col191 + 1;
	  col192  = col192 + 1;
	  col193  = col193 + 1;
	  col194  = col194 + 1;
	  col195  = col195 + 1;
	  col196  = col196 + 1;
	  col197  = col197 + 1;
	  col198  = col198 + 1;
	  col199  = col199 + 1;
	  col200  = col200 + 1;
	  col201  = col201 + 1;
	  col202  = col202 + 1;
	  col203  = col203 + 1;
	  col204  = col204 + 1;
	  col205  = col205 + 1;
	  col206  = col206 + 1;
	  col207  = col207 + 1;
	  col208  = col208 + 1;
	  col209  = col209 + 1;
	  col210  = col210 + 1;
	  col211  = col211 + 1;
	  col212  = col212 + 1;
	  col213  = col213 + 1;
	  col214  = col214 + 1;
	  col215  = col215 + 1;
	  col216  = col216 + 1;
	  col217  = col217 + 1;
	  col218  = col218 + 1;
	  col219  = col219 + 1;
	  col220  = col220 + 1;
	  col221  = col221 + 1;
	  col222  = col222 + 1;
	  col223  = col223 + 1;
	  col224  = col224 + 1;
	  col225  = col225 + 1;
	  col226  = col226 + 1;
	  col227  = col227 + 1;
	  col228  = col228 + 1;
	  col229  = col229 + 1;
	  col230  = col230 + 1;
	  col231  = col231 + 1;
	  col232  = col232 + 1;
	  col233  = col233 + 1;
	  col234  = col234 + 1;
	  col235  = col235 + 1;
	  col236  = col236 + 1;
	  col237  = col237 + 1;
	  col238  = col238 + 1;
	  col239  = col239 + 1;
	  col240  = col240 + 1;
	  col241  = col241 + 1;
	  col242  = col242 + 1;
	  col243  = col243 + 1;
	  col244  = col244 + 1;
	  col245  = col245 + 1;
	  col246  = col246 + 1;
	  col247  = col247 + 1;
	  col248  = col248 + 1;
	  col249  = col249 + 1;
	  col250  = col250 + 1;
	  col251  = col251 + 1;
	  col252  = col252 + 1;
	  col253  = col253 + 1;
	  col254  = col254 + 1;
	  col255  = col255 + 1;
	  col256  = col256 + 1;
	  col257  = col257 + 1;
	  col258  = col258 + 1;
	  col259  = col259 + 1;
	  col260  = col260 + 1;
	  col261  = col261 + 1;
	  col262  = col262 + 1;
	  col263  = col263 + 1;
	  col264  = col264 + 1;
	  col265  = col265 + 1;
	  col266  = col266 + 1;
	  col267  = col267 + 1;
	  col268  = col268 + 1;
	  col269  = col269 + 1;
	  col270  = col270 + 1;
	  col271  = col271 + 1;
	  col272  = col272 + 1;
	  col273  = col273 + 1;
	  col274  = col274 + 1;
	  col275  = col275 + 1;
	  col276  = col276 + 1;
	  col277  = col277 + 1;
	  col278  = col278 + 1;
	  col279  = col279 + 1;
	  col280  = col280 + 1;
	  col281  = col281 + 1;
	  col282  = col282 + 1;
	  col283  = col283 + 1;
	  col284  = col284 + 1;
	  col285  = col285 + 1;
	  col286  = col286 + 1;
	  col287  = col287 + 1;
	  col288  = col288 + 1;
	  col289  = col289 + 1;
	  col290  = col290 + 1;
	  col291  = col291 + 1;
	  col292  = col292 + 1;
	  col293  = col293 + 1;
	  col294  = col294 + 1;
	  col295  = col295 + 1;
	  col296  = col296 + 1;
	  col297  = col297 + 1;
	  col298  = col298 + 1;
	  col299  = col299 + 1;
	  col300  = col300 + 1;
	  message col301;
	  col301  = col301 + 1;
	  message col301;

	end;

} /* create_blob_1_2() */

/**************************/
/* create_blob_2() */
/**************************/

create_blob_2()
{

	exec sql drop procedure blob_2;

	exec sql create procedure blob_2 (bob long varchar not null) as

	begin

	    message 'in blob_2';
	    update xyz set bob = :bob;

	end;

} /* create_blob_2() */


/**************************/
/* create_sza12_1() */
/**************************/

create_sza12_1()
{
	exec sql drop procedure sza12_1;

	exec sql create procedure sza12_1 (
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
} /* create_sza12_1() */

/**************************/
/* create_sza12_2_1() */
/**************************/

create_sza12_2_1()
{

	exec sql drop procedure sza12_2_1;

	exec sql create procedure sza12_2_1 (bob varchar(2000) not null) as

	/*exec sql create procedure sza12_2_1 (bob long varchar not null) as*/

        declare mesg_buf varchar(2000) not null;
	begin
		mesg_buf = :bob;
		message mesg_buf;
		bob = 'This is a test for table xyz1 in the database procedure.';
		mesg_buf = :bob;
		message mesg_buf;
	end;
} /* create_sza12_2_1() */

/**************************/
/* create_sza12_2_2() */
/**************************/

create_sza12_2_2()
{

	exec sql drop procedure sza12_2_2;

	exec sql create procedure sza12_2_2 (bob varchar(2001) not null) as

        declare mesg_buf varchar(2001) not null;
	begin
		mesg_buf = :bob;
		message mesg_buf;
		bob = 'This is a test for table xyz2 in the database procedure.';
		mesg_buf = :bob;
		message mesg_buf;
	end;
} /* create_sza12_2_2() */

/**************************/
/* create_sza12_3_1() */
/**************************/

create_sza12_3_1()
{

	exec sql drop procedure sza12_3_1;

	exec sql create procedure sza12_3_1 (i1	i1 not null with default) as
	begin
		i1 = 5;
	end;

} /* create_sza12_3_1() */

/**************************/
/* create_sza12_3_2() */
/**************************/

create_sza12_3_2()
{

	exec sql drop procedure sza12_3_2;

	exec sql create procedure sza12_3_2 (c1	c(1) not null with default) as
	begin
		c1 = 'z';
	end;
} /* create_sza12_3_2() */

/**************************/
/* create_sza12_3_3() */
/**************************/

create_sza12_3_3()
{

	exec sql drop procedure sza12_3_3;

	exec sql create procedure sza12_3_3 (f4	f4 not null with default) as
	begin
		f4 = 4.2;
	end;
} /* create_sza12_3_3() */

/**************************/
/* create_sza12_3_4() */
/**************************/

create_sza12_3_4()
{
	exec sql drop procedure sza12_3_4;

	exec sql create procedure sza12_3_4 (
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
} /* create_sza12_3_4() */

/**************************/
/* create_sza12_3_5() */
/**************************/

create_sza12_3_5()
{
 
	exec sql drop procedure sza12_3_5;

	exec sql create procedure sza12_3_5 (deci	decimal(5,3) not null with default) as
	declare mesg_buf  varchar(81) not null;
	begin
		mesg_buf = varchar(:deci);
		message mesg_buf;
		deci = 50.789;
		mesg_buf = varchar(:deci);
		message mesg_buf;
	end;

} /* create_sza12_3_5() */

/**************************/
/* create_sza12_4() */
/**************************/

create_sza12_4()
{

	exec sql drop procedure sza12_4;

	exec sql create procedure sza12_4 (
		i1		integer 	with null,
		f4		float		with null,
		c10		c10 		with null) as
	begin
		i1 = 0;
		f4 = 0;
		c10 = '';
	end;

} /* create_sza12_3_5() */

/**************************/
/* create_sza14_1() */
/**************************/

create_sza14_1()
{

	exec sql drop procedure sza14_1;

	exec sql create procedure sza14_1 (i integer not null with default, c varchar(81) not null with default) as declare mesg_buf varchar(81) not null with default;
	begin
                message i;
                i = int4(c) + 1000;
		mesg_buf = 'value of i = ' + varchar(i);
                message mesg_buf;

                message c;
		c = c + varchar(i);
		mesg_buf = 'value of c = ' + c;
                message mesg_buf;
	end;

} /* create_sza14_1() */

/**************************/
/* create_sza14_2_1() */
/**************************/

create_sza14_2_1()
{
	exec sql drop procedure sza14_2_1;

	exec sql create procedure sza14_2_1 (f4 f4 not null with default, deci decimal(6,4) not null with default, dobyref integer not null with default) as declare mesg_buf varchar(81) not null with default;
	begin
	   f4 = 22.2222;
	   deci = 44.45567;
           insert into bob (f4, deci) values (:f4, :deci);
           if dobyref = 1 then
	     execute procedure sza14_2_2(f4 = byref(:f4), deci = byref(:deci));
             else 
             execute procedure sza14_2_2(f4 = :f4, deci = :deci);
           endif;
	end;
} /* create_sza14_2_1() */

/**************************/
/* create_sza14_2_2() */
/**************************/

create_sza14_2_2()
{
	exec sql drop procedure sza14_2_2;

	exec sql create procedure sza14_2_2 (f4   f4 not null with default, deci decimal(6,4) not null with default) as declare mesg_buf varchar(81) not null with default; 
	begin
		mesg_buf = varchar(f4);
                message mesg_buf;
		f4 = 777.7777; 
		mesg_buf = 'return varchar(f4) = ' + varchar(f4);
                message mesg_buf;

		mesg_buf = varchar(deci);
                message mesg_buf;
		deci = 88.8555;
		mesg_buf = 'return varchar(deci) = ' + varchar(deci);
                message mesg_buf;
                insert into bob (f4, deci) values (:f4, :deci);

		f4 = decimal(f4);
                mesg_buf = 'return decimal(f4) = ' + varchar(f4);
                message mesg_buf;

                deci = float4(deci);
                mesg_buf = 'return float4(deci) = ' + varchar(deci);
                message mesg_buf;
                insert into bob (f4, deci) values (:f4, :deci);
	end;
} /* create_sza14_2_2() */

/**************************/
/* create_sza15_1() */
/**************************/

create_sza15_1()
{
	exec sql drop procedure sza15_1;
	exec sql create procedure sza15_1 (
		i1		i1		not null with default,
		var1            c80             not null with default) as
	begin
		message i1;
		i1		=  2;
		message i1;
		message var1;
		var1   = 'value in sza15_1';
		message var1;
	end;
} /* create_sza15_1() */

/**************************/
/* create_sza15_2_1() */
/**************************/

create_sza15_2_1()
{

	exec sql drop procedure sza15_2_1;
	exec sql create procedure sza15_2_1 (
		a		integer		not null with default,
		b		integer		not null with default,
		c		integer		not null with default,
		d		integer		not null with default,
		e		integer		not null with default,
		f		integer		not null with default,
		g		integer		not null with default,
		h               integer         not null with default) as
	begin
		message 'calling sza15_2_1';
		message a;
		a =  a + 1;
		message a '<-  a';
		message b;
		b =  b + 1;
		message b '<-  b';
		message c;
		c =  c + 1;
		message c '<-  c';
		message d;
		d =  d + 1;
		message d '<-  d';
		message e;
		e =  e + 1;
		message e '<-  e';
		message f;
		f =  f + 1;
		message f '<-  f';
		message g;
		g =  g + 1;
		message g '<-  g';
		message h;
		h =  h + 1;
		message h '<-  h';
		execute procedure sza15_2_2 (a = byref(:a),
					 b = :b,
					 c = byref(:c),
					 d = :d);
	end;

} /* create_sza15_2_1() */

/**************************/
/* create_sza15_2_2() */
/**************************/

create_sza15_2_2()
{

	exec sql drop procedure sza15_2_2;
	exec sql create procedure sza15_2_2 (
		a		integer		not null with default,
		b		integer		not null with default,
		c		integer		not null with default,
		d		integer		not null with default,
		e		integer		not null with default,
		f		integer		not null with default,
		g		integer		not null with default,
		h               integer         not null with default) as
	begin
		message 'calling sza15_2_2';
		message a;
		a =  a + 1;
		message a '<-  a';
		message b;
		b =  b + 1;
		message b '<-  b';
		message c;
		c =  c + 1;
		message c '<-  c';
		message d;
		d =  d + 1;
		message d '<-  d';
                message e '<-  e';
                message f '<-  f';
                message g '<-  g';
                message h '<-  h';
		execute procedure sza15_2_3 (a = :a,
					 b = byref(:b),
					 c = :c,
					 d = byref(:d),
					 e = :e,
					 f = byref(:f));
	end;
} /* create_sza15_2_2() */

/**************************/
/* create_sza15_2_3() */
/**************************/

create_sza15_2_3()
{

	exec sql drop procedure sza15_2_3;
	exec sql create procedure sza15_2_3 (
		a		integer		not null with default,
		b		integer		not null with default,
		c		integer		not null with default,
		d		integer		not null with default,
		e		integer		not null with default,
		f		integer		not null with default,
		g		integer		not null with default,
		h               integer         not null with default) as
	begin
		message 'calling sza15_2_3';
		message a;
		a =  a + 1;
		message a '<-  a';
		message b;
		b =  b + 1;
		message b '<-  b';
		message c;
		c =  c + 1;
		message c '<-  c';
		message d;
		d =  d + 1;
		message d '<-  d';
		message e;
		e =  e + 1;
		message e '<-  e';
		message f;
		f =  f + 1;
		message f '<-  f';
                message g '<-  g';
                message h '<-  h';
		execute procedure sza15_2_4 (a = byref(:a),
					 b = :b,
					 c = byref(:c),
					 d = :d,
					 e = byref(:e),
					 f = :f,
					 g = byref(:g),
					 h = :h);

	end;

} /* create_sza15_2_3() */

/**************************/
/* create_sza15_2_4() */
/**************************/

create_sza15_2_4()
{

	exec sql drop procedure sza15_2_4;
	exec sql create procedure sza15_2_4 (
		a		integer		not null with default,
		b		integer		not null with default,
		c		integer		not null with default,
		d		integer		not null with default,
		e		integer		not null with default,
		f		integer		not null with default,
		g		integer		not null with default,
		h               integer         not null with default) as
	begin
		message 'calling sza15_2_4';
		message a;
		a =  a + 1;
		message a '<-  a';
		message b;
		b =  b + 1;
		message b '<-  b';
		message c;
		c =  c + 1;
		message c '<-  c';
		message d;
		d =  d + 1;
		message d '<-  d';
		message e;
		e =  e + 1;
		message e '<-  e';
		message f;
		f =  f + 1;
		message f '<-  f';
		message g;
		g =  g + 1;
		message g '<-  g';
		message h;
		h =  h + 1;
		message h '<-  h';
		execute procedure sza15_2_5 (a = byref(:a),
					 b = byref(:b),
					 c = byref(:c),
					 d = byref(:d),
					 e = byref(:e),
					 f = byref(:f),
					 g = byref(:g),
					 h = byref(:h));

	end;

} /* create_sza15_2_4() */

/**************************/
/* create_sza15_2_5() */
/**************************/

create_sza15_2_5()
{

	exec sql drop procedure sza15_2_5;
	exec sql create procedure sza15_2_5 (
		a		integer		not null with default,
		b		integer		not null with default,
		c		integer		not null with default,
		d		integer		not null with default,
		e		integer		not null with default,
		f		integer		not null with default,
		g		integer		not null with default,
		h               integer         not null with default) as
	begin
		message 'calling sza15_2_5';
		message a;
		a =  a + 10;
		message a '<-  a';
		message b;
		b =  b + 10;
		message b '<-  b';
		message c;
		c =  c + 10;
		message c '<-  c';
		message d;
		d =  d + 10;
		message d '<-  d';
		message e;
		e =  e + 10;
		message e '<-  e';
		message f;
		f =  f + 10;
		message f '<-  f';
		message g;
		g =  g + 10;
		message g '<-  g';
		message h;
		h =  h + 10;
		message h '<-  h';
	end;
} /* create_sza15_2_5() */

/**************************/
/* create_sza15_3_1() */
/**************************/

create_sza15_3_1()
{

	exec sql drop procedure sza15_3_1;
	exec sql create procedure sza15_3_1 (
		i1		i1		not null with default,
		f4		f4		not null with default,
		c50             c50             not null with default) as
	declare mesg_buf varchar(81) not null with default;
	begin
		i1  =  i1 + 5;
		f4  =  f4 + .05;
		c50 = 'value in sza15_3_1' + c50;
		message i1;
		mesg_buf = varchar(:f4);
		message mesg_buf;
		message c50;
	        execute procedure sza15_3_2 (i1        = byref(:i1),
					      f4        = byref(:f4),
					      c50       = byref(:c50));
	end;
} /* create_sza15_3_1() */

/**************************/
/* create_sza15_3_2() */
/**************************/

create_sza15_3_2()
{

	exec sql drop procedure sza15_3_2;
	exec sql create procedure sza15_3_2 (
		i1		i1		with null,
		f4		f4		with null,
		c50             c50             with null) as
	begin
		i1  =  0;
		f4  =  0;
		c50 = '';
	end;
} /* create_sza15_3_2() */

/**************************/
/* create_sza15_3_3() */
/**************************/

create_sza15_3_3()
{

	exec sql drop procedure sza15_3_3;
	exec sql create procedure sza15_3_3 (
		i1		i1		with null,
		f4		f4		with null,
		c50             c50             with null) as
	begin
		i1  =  7;
		f4  =  .05;
		c50 = 'value in sza15_3_3';
	end;
} /* create_sza15_3_3() */

/**************************/
/* create_sza15_foo() */
/**************************/

create_sza15_foo()
{

	exec sql drop procedure foo;
	exec sql create procedure foo (
		i1		i1		not null with default,
		var1		c80		not null with default) as
	begin
		message i1;
		i1		=  8;
		message i1;
		message var1;
		var1 = 'value in foo proc';
		message var1;
	        execute procedure sza15_1 (var1 = byref(:var1),
					    i1   = byref(:i1));
	end;

} /* create_sza15_foo() */

/**************************/
/* create_sza16_foo() */
/**************************/

create_sza16_foo()
{

	exec sql drop procedure sza16_foo;
	exec sql create procedure sza16_foo (
		i1		i1		not null with default,
		var1		c80		not null with default) as
	begin
		message i1;
		i1		=  8;
		message i1;
		message var1;
		var1 = 'value in sza16_foo proc';
		message var1;
	        execute procedure sza16_1 (var1 = byref(:var1),
					    i1   = byref(:i1));
	end;

} /* create_sza16_foo() */

/**************************/
/* create_sza16_1() */
/**************************/

create_sza16_1()
{

	exec sql drop procedure sza16_1;
	exec sql create procedure sza16_1 (
		i1		i1		not null with default,
		var1            c80             not null with default) as
	begin
		message i1;
		i1		=  2;
		message i1;
		message var1;
		var1   = 'value in sza16_1';
		message var1;
	end;


} /* create_sza16_1() */

/**************************/
/* create_sza16_2_1() */
/**************************/

create_sza16_2_1()
{

	exec sql drop procedure sza16_2_1;
	exec sql create procedure sza16_2_1 (
		a		integer		not null with default,
		b		integer		not null with default,
		c		integer		not null with default,
		d		integer		not null with default,
		e		integer		not null with default,
		f		integer		not null with default,
		g		integer		not null with default,
		h               integer         not null with default) as
	begin
		message 'calling sza16_2_1';
		message a;
		a =  a + 1;
		message a '<-  a';
		message b;
		b =  b + 1;
		message b '<-  b';
		message c;
		c =  c + 1;
		message c '<-  c';
		message d;
		d =  d + 1;
		message d '<-  d';
		message e;
		e =  e + 1;
		message e '<-  e';
		message f;
		f =  f + 1;
		message f '<-  f';
		message g;
		g =  g + 1;
		message g '<-  g';
		message h;
		h =  h + 1;
		message h '<-  h';
		execute procedure sza16_2_2 (a = byref(:a),
					 b = :b,
					 c = byref(:c),
					 d = :d);
	end;


} /* create_sza16_2_1() */

/**************************/
/* create_sza16_2_2() */
/**************************/

create_sza16_2_2()
{

	exec sql drop procedure sza16_2_2;
	exec sql create procedure sza16_2_2 (
		a		integer		not null with default,
		b		integer		not null with default,
		c		integer		not null with default,
		d		integer		not null with default,
		e		integer		not null with default,
		f		integer		not null with default,
		g		integer		not null with default,
		h               integer         not null with default) as
	begin
		message 'calling sza16_2_2';
		message a;
		a =  a + 1;
		message a '<-  a';
		message b;
		b =  b + 1;
		message b '<-  b';
		message c;
		c =  c + 1;
		message c '<-  c';
		message d;
		d =  d + 1;
		message d '<-  d';
                message e '<-  e';
                message f '<-  f';
                message g '<-  g';
                message h '<-  h';
		execute procedure sza16_2_3 (a = :a,
					 b = byref(:b),
					 c = :c,
					 d = byref(:d),
					 e = :e,
					 f = byref(:f));
	end;


} /* create_sza16_2_2() */

/**************************/
/* create_sza16_2_3() */
/**************************/

create_sza16_2_3()
{

	exec sql drop procedure sza16_2_3;
	exec sql create procedure sza16_2_3 (
		a		integer		not null with default,
		b		integer		not null with default,
		c		integer		not null with default,
		d		integer		not null with default,
		e		integer		not null with default,
		f		integer		not null with default,
		g		integer		not null with default,
		h               integer         not null with default) as
	begin
		message 'calling sza16_2_3';
		message a;
		a =  a + 1;
		message a '<-  a';
		message b;
		b =  b + 1;
		message b '<-  b';
		message c;
		c =  c + 1;
		message c '<-  c';
		message d;
		d =  d + 1;
		message d '<-  d';
		message e;
		e =  e + 1;
		message e '<-  e';
		message f;
		f =  f + 1;
		message f '<-  f';
                message g '<-  g';
                message h '<-  h';
		execute procedure sza16_2_4 (a = byref(:a),
					 b = :b,
					 c = byref(:c),
					 d = :d,
					 e = byref(:e),
					 f = :f,
					 g = byref(:g),
					 h = :h);

	end;


} /* create_sza16_2_3() */

/**************************/
/* create_sza16_2_4() */
/**************************/

create_sza16_2_4()
{

	exec sql drop procedure sza16_2_4;
	exec sql create procedure sza16_2_4 (
		a		integer		not null with default,
		b		integer		not null with default,
		c		integer		not null with default,
		d		integer		not null with default,
		e		integer		not null with default,
		f		integer		not null with default,
		g		integer		not null with default,
		h               integer         not null with default) as
	begin
		message 'calling sza16_2_4';
		message a;
		a =  a + 1;
		message a '<-  a';
		message b;
		b =  b + 1;
		message b '<-  b';
		message c;
		c =  c + 1;
		message c '<-  c';
		message d;
		d =  d + 1;
		message d '<-  d';
		message e;
		e =  e + 1;
		message e '<-  e';
		message f;
		f =  f + 1;
		message f '<-  f';
		message g;
		g =  g + 1;
		message g '<-  g';
		message h;
		h =  h + 1;
		message h '<-  h';
		execute procedure sza16_2_5 (a = byref(:a),
					 b = byref(:b),
					 c = byref(:c),
					 d = byref(:d),
					 e = byref(:e),
					 f = byref(:f),
					 g = byref(:g),
					 h = byref(:h));

	end;


} /* create_sza16_2_4() */

/**************************/
/* create_sza16_2_5() */
/**************************/

create_sza16_2_5()
{

	exec sql drop procedure sza16_2_5;
	exec sql create procedure sza16_2_5 (
		a		integer		not null with default,
		b		integer		not null with default,
		c		integer		not null with default,
		d		integer		not null with default,
		e		integer		not null with default,
		f		integer		not null with default,
		g		integer		not null with default,
		h               integer         not null with default) as
	begin
		message 'calling sza16_2_5';
		message a;
		a =  a + 10;
		message a '<-  a';
		message b;
		b =  b + 10;
		message b '<-  b';
		message c;
		c =  c + 10;
		message c '<-  c';
		message d;
		d =  d + 10;
		message d '<-  d';
		message e;
		e =  e + 10;
		message e '<-  e';
		message f;
		f =  f + 10;
		message f '<-  f';
		message g;
		g =  g + 10;
		message g '<-  g';
		message h;
		h =  h + 10;
		message h '<-  h';
	end;


} /* create_sza16_2_5() */

/**************************/
/* create_sza16_3_1() */
/**************************/

create_sza16_3_1()
{

	exec sql drop procedure sza16_3_1;
	exec sql create procedure sza16_3_1 (
		i1		i1		not null with default,
		f4		f4		not null with default,
		c50             c50             not null with default) as
	declare mesg_buf varchar(81) not null with default;
	begin
		i1  =  i1 + 5;
		f4  =  f4 + .05;
		c50 = 'value in sza16_3_1' + c50;
		message i1;
		mesg_buf = varchar(:f4);
		message mesg_buf;
		message c50;
	        execute procedure sza16_3_2 (i1        = byref(:i1),
					      f4        = byref(:f4),
					      c50       = byref(:c50));
	end;

} /* create_sza16_3_1() */

/**************************/
/* create_sza16_3_2() */
/**************************/

create_sza16_3_2()
{

	exec sql drop procedure sza16_3_2;
	exec sql create procedure sza16_3_2 (
		i1		i1		with null,
		f4		f4		with null,
		c50             c50             with null) as
	begin
		i1  =  0;
		f4  =  0;
		c50 = '';
	end;

} /* create_sza16_3_2() */

/**************************/
/* create_sza16_3_3() */
/**************************/

create_sza16_3_3()
{

	exec sql drop procedure sza16_3_3;
	exec sql create procedure sza16_3_3 (
		i1		i1		with null,
		f4		f4		with null,
		c50             c50             with null) as
	begin
		i1  =  7;
		f4  =  .05;
		c50 = 'value in sza16_3_3';
	end;

} /* create_sza16_3_3() */
