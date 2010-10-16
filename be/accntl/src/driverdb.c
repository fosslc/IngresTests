/*
**	driverdb.c
*/

#include <stdio.h>

char	*driver_name = "db";
char	*driver_description = "database procedures tests";

int	dba29();
int	dba30();
int	dba31();
int	dba35();
int	dba45();

struct {
	char	*name;
	int	(*func)();
} driver_list[] = {
	{	"dba29",	dba29	},
	{	"dba30",	dba30	},
	{	"dba31",	dba31	},
	{	"dba35",	dba35	},
	{	"dba45",	dba45	},
	{	NULL,		NULL	}
};
