/*
**	ingclude.sc
*/

exec sql include sqlca;

exec sql whenever sqlerror call sqlprint;
exec sql whenever not found call sqlprint;
exec sql whenever sqlwarning call sqlprint;

exec sql begin declare section;

char	*db_name;

char	stmt_buf[256];
char	qual_buf[256];
char	exim_buf[256];

/* inquire_ingres vars */
int	Dbmserror;
int	Endquery;
int	Errorno;
char	Errortext[256];
char	Errortype[32];
int	Messagenumber;
char	Messagetext[256];
int	Rowcount;
int	Session;
int	Transaction;

exec sql end declare section;

#define TRUE	1
#define FALSE	0

#define MAXOBJLNG	32
