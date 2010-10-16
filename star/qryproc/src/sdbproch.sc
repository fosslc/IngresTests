/* 
**	sdbproch.sc
**
**	header file for star dbproc tests.
**
**	history:
**		jan 16/89 sgp
**			- created.
**		aug 11 1993 barbh
**			-modified rulesh.sc file to be sdbproch.sc 
**			 to be used with star dbproc sep tests.
*/

#define	DULL			1
#define	GRAPH_3_1		2
#define	GRAPH_3_2		3
#define	GRAPH_3_3		4
#define GRAPH_3_4               5
#define GRAPH_3_5               6
#define	LOG			7
#define GRAPH_3			8

exec sql begin declare section;

struct dull_ {
	int	dull;
} dull;

struct ruleslog_ {
	char	entry[81];
} ruleslog;

struct graph_3_ {
	int	from_node;
	int	to_node;
} graph_3;

struct graph_3_1_ {
	int     from_node;
	int     to_node;
} graph_3_1;

struct graph_3_2_ {
	int     from_node;
	int     to_node;
} graph_3_2;

struct graph_3_3_ {
	int     from_node;
	int     to_node;
} graph_3_3;

struct graph_3_4_ {
	int     from_node;
	int     to_node;
} graph_3_4;

struct graph_3_5_ {
	int     from_node;
	int     to_node;
} graph_3_5;

exec sql end declare section;
