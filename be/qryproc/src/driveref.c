/*
**	driveref.c
*/

#include <stdio.h>

char	*driver_name = "ef";
char	*driver_description = "database procedures tests";

int	efa01();
int	efa02();
int	efa03();
int	efa04();
int	efa05();
int	efa06();
int	efa07();
int	efa08();
int	efa09();
int	efa10();
int	efa11();
int	efa12();
int	efa13();
int	efa14();
int	efa15();
int	efa16();
int	efa17();
int	efa18();
int	efa19();
int	efa20();
int	efa21();
int	efa22();
int	efa23();
int	efa24();
int	efa25();
int     efa26();
int     efa27();
int     efa29();

struct {
	char	*name;
	int	(*func)();
} driver_list[] = {
	{	"efa01",	efa01	},
	{	"efa02",	efa02	},
	{	"efa03",	efa03	},
	{	"efa04",	efa04	},
	{	"efa05",	efa05	},
	{	"efa06",	efa06	},
	{	"efa07",	efa07	},
	{	"efa08",	efa08	},
	{	"efa09",	efa09	},
	{	"efa10",	efa10	},
	{	"efa11",	efa11	},
	{	"efa12",	efa12	},
	{	"efa13",	efa13	},
	{	"efa14",	efa14	},
	{	"efa15",	efa15	},
	{	"efa16",	efa16	},
	{	"efa17",	efa17	},
	{	"efa18",	efa18	},
	{	"efa19",	efa19	},
	{	"efa20",	efa20	},
	{	"efa21",	efa21	},
	{	"efa22",	efa22	},
	{	"efa23",	efa23	},
	{	"efa24",	efa24	},
	{	"efa25",	efa25	},
	{       "efa26",        efa26   },
        {       "efa27",        efa27   },
	{       "efa29",        efa29   },
	{	NULL,		NULL	}
};
