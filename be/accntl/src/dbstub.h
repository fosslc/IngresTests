/*
**	driverefstub.h
**
**  History:
**	11-jun-2003 (abbjo03)
**	    Add return statements to prevent warnings on VMS.
*/

extern char     db_name_str[32];

#ifndef	DBA29
int	dba29() { return 0; }
#endif

#ifndef	DBA30
int	dba30() { return 0; }
#endif

#ifndef	DBA31
int	dba31() { return 0; }
#endif

#ifndef	DBA35
int	dba35() { return 0; }
#endif

#ifndef	DBA45
int	dba45() { return 0; }
#endif
