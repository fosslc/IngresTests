/*
              drivereg.c
*/

#include <stdio.h>

char	*driver_name = "eg";
char	*driver_description = "REPEATE QUERIES";

int     ega06();
int     ega07();
int     ega08();

struct {
	char	*name;
	int	(*eunc)();
} driver_list[] = {
	{	"ega06",	ega06	},
	{	"ega07",	ega07	},
	{	"ega08",	ega08	},
	{	NULL,		NULL	}
};
