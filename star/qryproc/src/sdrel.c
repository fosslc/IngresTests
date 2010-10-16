/*
**	sdrel.c
*/

#include <stdio.h>

char	*driver_name = "sz";
char	*driver_description = "dbproc star tests";

void	sza01();
void	sza02();
void	sza03();
void	sza04();
void	sza05();
void	sza06();
void	sza07();
void	sza08();
void	sza09();
void	sza10();
void	sza11();
void	sza12();
void	sza13();
void	sza14();
void	sza15();
void	sza16();

struct {
	char	*name;
	void	(*func)();
} driver_list[] = {
	{	"sza01",	sza01	},
	{	"sza02",	sza02	},
	{	"sza03",	sza03	},
	{	"sza04",	sza04	},
	{	"sza05",	sza05	},
	{	"sza06",	sza06	},
	{	"sza07",	sza07	},
	{	"sza08",	sza08	},
	{	"sza09",	sza09	},
	{	"sza10",	sza10	},
	{	"sza11",	sza11	},
	{	"sza12",	sza12	},
	{	"sza13",	sza13	},
	{	"sza14",	sza14	},
	{	"sza15",	sza15	},
	{       "sza16",        sza16   },
	{	NULL,		NULL	}
};
