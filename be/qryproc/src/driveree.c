/*
**	driveree.c
*/

#include <stdio.h>

char	*driver_name = "ee";
char	*driver_description = "database cursor tests";

int	eea01();
int	eea02();
int	eea03();
int	eea04();
int	eea05();
int	eea06();
int	eea07();
int	eea08();
int     eea09();
int     eea10();
int     eea11();
int     eea12();
int     eea13();
int     eea14();
int     eea15();
int     eea16();



struct {
	char	*name;
	int	(*eunc)();
} driver_list[] = {
	{	"eea01",	eea01	},
	{	"eea02",	eea02	},
	{	"eea03",	eea03	},
	{	"eea04",	eea04	},
	{	"eea05",	eea05	},
	{	"eea06",	eea06	},
	{	"eea07",	eea07	},
	{	"eea08",	eea08	},
	{       "eea09",        eea09   },
        {       "eea10",        eea10   },
        {       "eea11",        eea11   },
        {       "eea12",        eea12   },
        {       "eea13",        eea13   },
        {       "eea14",        eea14   },
        {       "eea15",        eea15   },
        {       "eea16",        eea16   },
	{	NULL,		NULL	}
};
