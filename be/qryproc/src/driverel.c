/*
**	driverel.c
*/

#include <stdio.h>

char	*driver_name = "el";
char	*driver_description = "rules tests";

int	ela01();
int	ela02();
int	ela03();
int	ela04();
int	ela05();
int	ela06();
int	ela07();
int	ela08();
int	ela09();
int	ela10();
int	ela11();
int	ela12();
int	ela13();
int	ela14();
int	ela15();

struct {
	char	*name;
	int	(*func)();
} driver_list[] = {
	{	"ela01",	ela01	},
	{	"ela02",	ela02	},
	{	"ela03",	ela03	},
	{	"ela04",	ela04	},
	{	"ela05",	ela05	},
	{	"ela06",	ela06	},
	{	"ela07",	ela07	},
	{	"ela08",	ela08	},
	{	"ela09",	ela09	},
	{	"ela10",	ela10	},
	{	"ela11",	ela11	},
	{	"ela12",	ela12	},
	{	"ela13",	ela13	},
	{	"ela14",	ela14	},
	{	"ela15",	ela15	},
	{	NULL,		NULL	}
};
