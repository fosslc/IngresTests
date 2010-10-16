/*
              drivercd.c
*/

#include <stdio.h>

char	*driver_name = "cd";
char	*driver_description = "DML-STATEMENTS";

int     cda24();



struct {
	char	*name;
	int	(*eunc)();
} driver_list[] = {
	{	"cda24",	cda24	},
	{	NULL,		NULL	}
};
