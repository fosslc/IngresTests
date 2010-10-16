#include <sql.h>
#include <sqlext.h>
/*
#include <odbcinst.h>
#include <sqltypes.h>
*/

const double upperBound = 10000000.000f;
const double lowerBound = -1000000.000f;
const float  upperValue = 9999999.999f;
const float  lowerValue = -1000000.000f;

/*
** Define Some useful defines
*/
#if !defined (NULL)
#define NULL    0
#endif

#define SQL_WCHAR               (-8)
#define SQL_WVARCHAR            (-9)
#define SQL_WLONGVARCHAR        (-10)

/*
** Defines Used by ArgParse and Parent.
*/
#define PWD_ARG         1
#define UID_ARG         2
#define DSN_ARG         3
#define FILE_ARG        4
#define HELP_ARG        5

/*
** Defines used by main program.
*/
#define PWD_LEN         32
#define UID_LEN         32
#define OPT1_LEN        255
#define OPT2_LEN        255
#define DSN_LEN         32
#define PWD_MSG1 "Requested password exceeds compiled limit of %d.\n"
#define PWD_ERR1 "Password not found after keyword -pwd on command line.\n"
#define UID_MSG1 "Requested username exceeds compiled limit of %d.\n"
#define FILE_MSG1 "Requested filename exceeds compiled limit of %d.\n"
#define DSN_MSG1 "Requested datasource name exceeds compiled limit of %d.\n"
#define FILE_ERR1 "Filename not found after keyword -file on command line.\n"
#define DSN_ERR1 "Datasource name not found after keyword -dsn on command line.\n"
#define UID_ERR1 "Username not found after keyword -uid on command line.\n"
#define USAGE_MSG1 "Usage: %s [-uid username] [-pwd password] [-dsn datasource] [-file filename].\n"

int ArgParse(char*); /* to parse runtime arguments */
void ODBC_initialize(HENV* ); /* initialize ODBC environment handle */
void ODBC_prepareCon(HENV, HDBC* ); /* setup connection properties */
void ODBC_getErrorInfo(HENV, HDBC, HSTMT ); /* Display current ODBC errors */
RETCODE ODBC_connect(HENV, HDBC, UCHAR*, UCHAR*, UCHAR* ); /* connect to the Datasource Name */
void ODBC_releaseEnv(HENV, HDBC ); /* free connection and environment handles */ 
static long ODBC_getFileLength(FILE* );   /* return the length of the file */
void ODBC_fileRead(char*, HENV, HDBC, HSTMT ); /* read SQL statements from a file and execute them */
void ODBC_execQuery(char*, HENV, HDBC, HSTMT ); /* execute SQL query */
UDWORD ODBC_getRowSize(HSTMT, SWORD, int [] ); /* return the length of entire row */
void ODBC_displayHeader(HSTMT, SWORD, UDWORD, int [], char [] ); /* display column names from a SELECT statement */
void ODBC_fillBlank ( char [], SQLINTEGER, int );
int  ODBC_formatData ( char [], double, double, double );
int  ODBC_validHour ( unsigned int );
int  ODBC_validMinute ( unsigned int );
int  ODBC_validSecond ( unsigned int );
void ODBC_validBuffer ( char* );
void ODBC_fetchData ( HSTMT, HENV, HDBC, int);
