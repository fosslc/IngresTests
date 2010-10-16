/******************************************************************************************
** Copyright (C) Ingres Corporation 2000.                           **
**                                                                                       **
** Implementation File: odbctest.c                                                       **
**                                                                                       **
** Purpose:       This program is used to test the basic functionality of an ODBC driver.**
**                                                                                       **
** Arguments:                                                                            **
**                uid  - user_name                                                       **
**                pwd  - passwd                                                          **
**                dsn  - datasource_name                                                 **
**                file - SQLscript_name                                                  **
**                                                                                       **
** Outputs:                                                                              **
**                Returns:                                                               **
**                                                                                       **
** Notes:                                                                                **
**                                                                                       **
** History:                                                                              **
**                01-Jan-2000 (ngutr03)                                                  **
**                    created.                                                           **
**                14-Feb-2001 (ngutr03)                                                  **
**                    Added char dummy in ODBC_fetchData() so it can work for ODBC 2.x.  **
**                    Because, ODBC 2.x returns incorrect length of data if the function **
**                    SQLGetData() has an invalid pointer.                               **
**                01-Aug-2001 (sarjo01)                                                  **
**                    Changed char dummy to char dummy[1] to make SQLGetData() work      **
**                    correctly.                                                         **
**                    Added rudimentary Unicode support.				 **
**		  01-Sep-2004 (adasr01)							 **
**	              Fixed bug in ODBC_fileRead() writing null byte past end of	 ** 
**                    allocation.							 **
**                30-Jan-2006 (boija02)							 **
**                    Updated copyright info for Ingres Corp.				 **
**		  18-Jun-2007 (sarjo01)							 **
**		      In  ODBC_fetchData(), changed intData variable from 'long int'     **
**		      to 'SQLINTEGER' to fix problem on some 64-bit platforms		 ** 
**		  15-Jul-2008 (boija02)							 **
**		      malloc.h not present on VMS, so wrapping that include in an ifndef **
**											 **
******************************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#ifndef VMS
#include <malloc.h>
#endif
#include <math.h>
#ifdef _WIN32
#include <string.h>
#include <windows.h>
#else   
#include <strings.h>
#endif

#include "odbctest.h"
#include "wchar.h"

/***********************************************************************
** function:    ODBC_getErrorInfo                                     **
**                                                                    **
** Purpose:     Display to stdout current ODBC Errors                 **
**                                                                    **
** Arguments:   henv    _ ODBC Environment handle.                    **
**              hdbc    - ODBC Connection Handle error generated on.  **
**              hstmt   - ODBC SQL Handle error generated on.         **
**                                                                    **
** Returns:     void                                                  **
**                                                                    **
***********************************************************************/

void ODBC_getErrorInfo (                        /* Get and print ODBC error messages */
						HENV henv,              /* ODBC Environment */
						HDBC hdbc,              /* ODBC Connection Handle */
						HSTMT hstmt)            /* ODBC SQL Handle */
{
	UCHAR   sqlstate[10];
	UCHAR   errmsg[SQL_MAX_MESSAGE_LENGTH];
	SDWORD  nativeerr;
	SWORD   actualmsglen;
	RETCODE rc;
	
	while(1)
	{
		rc = SQLError(henv, hdbc, hstmt,
			sqlstate, &nativeerr, errmsg,
			SQL_MAX_MESSAGE_LENGTH - 1, &actualmsglen);
		if (rc == SQL_NO_DATA_FOUND)
                        break;
                printf ("SQLSTATE = %s\n",sqlstate);
                printf ("NATIVE ERROR = %d\n",nativeerr);
                errmsg[actualmsglen] = '\0';
                printf ("MSG = %s\n\n",errmsg);
	}
}


/*************************************************************************
** function:    ArgParse                                                **
**                                                                      **                              
** Purpose:     To parse runtime arguments.                             **
**                                                                      **
** Arguments:   argv which is a character string to be parsed.          **
**                                                                      **
*************************************************************************/
int ArgParse(char *argv)
{
	if (!strcmp(argv, "-help"))
		return (HELP_ARG);
	if (!strcmp(argv, "-pwd"))
		return (PWD_ARG);
	if (!strcmp(argv, "-uid"))
		return (UID_ARG);
	if (!strcmp(argv, "-file"))
		return (FILE_ARG);      
	if (!strcmp(argv, "-dsn")) 
		return (DSN_ARG);
	else
		return (-1);
}

/*************************************************************************
** function:    ODBC_initialize                                         **
**                                                                      **
** Purpose:     Initialize handle environment                           **
**                                                                      **
** Arguments:   henv - Pointer to environment handle                    **
**                                                                      **
*************************************************************************/
void ODBC_initialize(HENV* henv)
{
	RETCODE rc;
	
	rc = SQLAllocHandle (SQL_HANDLE_ENV, SQL_NULL_HANDLE, henv);
	if ((rc != SQL_SUCCESS) && (rc != SQL_SUCCESS_WITH_INFO))
	{
		printf("Failed to initialize ODBC environmment...\n");
		exit(255);      /* Exit with failure */
	}
}

/*************************************************************************
** function:    ODBC_prepareCon                                         **
** Purpose:     prepare the connection                                  **
**                                                                      **
** Arguments:   henv    - Pointer to environment handle                 **
**              hdbc    - Pointer to connection handle                  **
*************************************************************************/
void ODBC_prepareCon(HENV henv, HDBC* hdbc )
{
	RETCODE rc;
	
	rc = SQLSetEnvAttr( henv, SQL_ATTR_ODBC_VERSION,
		(SQLPOINTER)SQL_OV_ODBC3,
		SQL_IS_INTEGER);
	if ((rc == SQL_SUCCESS) || (rc == SQL_SUCCESS_WITH_INFO))
	{
		rc = SQLAllocHandle(SQL_HANDLE_DBC, henv, hdbc);
		if ((rc != SQL_SUCCESS) && (rc != SQL_SUCCESS_WITH_INFO))
		{
			printf("Failed to allocate the connection handle.\n");
			exit(255);      /* Exit with failure */
		}
	}
} 


/*************************************************************************
** function:    ODBC_releaseEnv                                         **
**                                                                      **
** Purpose:     Frees environment and connection handles.               **
**                                                                      **
** Arguments:   henv - environment handle                               **
**              hdbc - connection to handle                             **
**                                                                      **
*************************************************************************/
void ODBC_releaseEnv(HENV henv, HDBC hdbc)
{
	SQLDisconnect (hdbc);
	SQLFreeConnect (hdbc);
	SQLFreeEnv (henv);
}


/*************************************************************************
** function:    ODBC_getFileLength                                      **
**                                                                      **
** Purpose:	returns the length of the file                          **
**                                                                      **
** Arguments:   file   - Pointer to file                                **
**                                                                      **
** Returns:     long   - Length of file                                 **
**                                                                      **
*************************************************************************/
static long ODBC_getFileLength(FILE* file)
{
	int iCurrentPos, iFileLength;
	
	iCurrentPos = ftell (file);
	fseek (file, 0, SEEK_END);
	iFileLength = ftell(file);
	fseek (file, iCurrentPos, SEEK_SET);
	
	return (iFileLength);
}


/*************************************************************************
** function:    ODBC_fileRead                                           **
**                                                                      **
** Purpose:     Reads from a file and put into the buffer               **
**                                                                      **
** Arguments:   fileName - Pointer to fileName                          **
**              henv     - Environment handle                           **
**              hdbc     - Connection handle                            **
**              hstmt    - Statement handle                             **
**                                                                      **
*************************************************************************/
void ODBC_fileRead(char* fileName, HENV henv, HDBC hdbc, HSTMT hstmt)
{
	FILE*  file;
	int    iLength = 0;
	char*  szBuffer = NULL;
	int    currentIndex = 0;
	int    nSingleQuote = 0;
	char*  sqlStmt = NULL;
	int    stmtLength = 0;
	int    prevIndex = 0;
	char   singleQuote = '\'';
	
	/* open file */
	printf("\nNow executing test: '%s' . . .\n\n", fileName);
	printf("Reading from '%s' . . .\n\n", fileName);
	if(NULL == (file = fopen (fileName, "rb")))
	{
		printf("Failed to open file '%s'\n", fileName);
		return;
	}
	/* check out file's length */
	iLength = ODBC_getFileLength (file);
	/* allocate memory */
	if(NULL == (szBuffer = (char*)calloc( iLength + 16, sizeof (char) )))
	{
		printf( "Insufficient memory available\n" );
		fclose (file);
		return;
	}
	/* read information from file and put it into the buffer */
	fread (szBuffer, 1, iLength, file);
	szBuffer[iLength] = '\0';
	
	/* close file */
	fclose (file);
	
	while( 1 )
	{
		if ( ( szBuffer[currentIndex] == '\\' ) && ( ( nSingleQuote % 2 ) == 0 ) )
		{
			currentIndex++;
			if ( szBuffer[currentIndex] == 'g' )
			{
				stmtLength = currentIndex - prevIndex - 1;
				free ( sqlStmt );
				if(NULL != ( sqlStmt = (char*)calloc( stmtLength + 1, sizeof(char) )))
				{
					strncpy ( (char*)sqlStmt, szBuffer + prevIndex, stmtLength );
					sqlStmt[stmtLength] = '\0';
					prevIndex = currentIndex + 1;
					while ((szBuffer[prevIndex] == '\n') || (szBuffer[prevIndex] == '\r'))
					{
						prevIndex++;
						currentIndex++;
					}
					ODBC_execQuery(sqlStmt, henv, hdbc, hstmt);
				}
			}
		}
		else
		{
			if ( szBuffer[currentIndex] == singleQuote )
				nSingleQuote++;
			if( szBuffer[currentIndex] == '\0' )
				break;	
			currentIndex++;
		}
	}
	free (szBuffer);
}

/*************************************************************************
** function:    ODBC_getRowSize                                         **
**                                                                      **
** Arguments:   hstmt       - Statement handle                          **
**              nColumns    - number of columns                         **
**              columnSizes - maximum size of all columns               **
**                                                                      **
** Returns:     nRowSize                                                **
**                                                                      **
*************************************************************************/
UDWORD ODBC_getRowSize( HSTMT hstmt, SWORD nColumns, int columnSizes[])
{
	int         i;
	UDWORD      nColumnSize;   /* size of the column */
	UDWORD      nRowSize = 0;
	RETCODE     rc;
	SQLCHAR     szColumnName[SQL_MAX_COLUMN_NAME_LEN];
	SWORD       nameLength;   /* actual column name's length */
	SWORD       dataType;
	SQLSMALLINT DecimalDigitsPtr;
    SQLSMALLINT NullablePtr;
	
	for ( i = 1; i <= nColumns; i++ )
	{
		rc = SQLDescribeCol ( hstmt, i, szColumnName, SQL_MAX_COLUMN_NAME_LEN,
			&nameLength, &dataType, 
			&nColumnSize, 

			&DecimalDigitsPtr, 
			&NullablePtr ); 
		if ( rc == SQL_SUCCESS )
		{       
			switch ( dataType )
			{
			case SQL_WCHAR:
			case SQL_WVARCHAR:
				nColumnSize /= 2;
				break;
			case SQL_LONGVARCHAR:
			case SQL_WLONGVARCHAR:
				nColumnSize = 33;
				break;
			case SQL_TYPE_TIMESTAMP:
				nColumnSize = 25;
				break;
            		case SQL_FLOAT:
			case SQL_DOUBLE:
			case SQL_REAL:
			case SQL_NUMERIC:
			case SQL_DECIMAL:
			case SQL_TINYINT:
			case SQL_INTEGER:
			case SQL_SMALLINT:
				nColumnSize += 5;
				break;
			}
			if (  nColumnSize > (UDWORD) nameLength )
			{
				nRowSize += nColumnSize;
				columnSizes[ i - 1 ] = nColumnSize;
			}
			else
			{
				nRowSize += nameLength;
				columnSizes[ i - 1 ] = nameLength;
			}
		}
	}
	nRowSize = nRowSize + nColumns + 2;
	
	return ( nRowSize );
}

/*************************************************************************
** function:    ODBC_fillBlank                                          **
**                                                                      **
** Purpose:     Fills empty spaces into Buffer                          **
**                                                                      **
** Arguments:   szBuffer   - output buffer needs to be filled w/ spaces **
**              dataLength - length of data                             **
**              columnSize - size of a column                           **
** Returns:     none                                                    **
**                                                                      **
*************************************************************************/  
void ODBC_fillBlank ( char szBuffer[], SQLINTEGER dataLength, int columnSize )
{
	int i;
	int prevLength;
	
	if ( dataLength < columnSize )
	{
		prevLength = strlen ( szBuffer ); 
	       for(i = 1; i <= ( columnSize - dataLength ); i++ )
		   {
			   if ( i == 1 )
				   strcpy ( szBuffer + prevLength, " " );
			   else   
				   strcat ( szBuffer, " " );
		   }
	}
}


/*************************************************************************
** function:    ODBC_validHour                                          **
** Purpose:     To check if the value of HOUR is VALID or NOT before    **
**              displaying it.                                          **
**                                                                      **
** Arguments:   hour - the value needs to be checked                    **
**                                                                      **
** Returns:     1 - VALID                                               **
**              0 - INVALID                                             **
**                                                                      **
*************************************************************************/
int ODBC_validHour ( unsigned int hour )
{
	if ( ( hour >= 0 ) && ( hour <= 23 ) )
		return ( 1 );
	else
		return ( 0 );
}


/*************************************************************************
** function:    ODBC_validMinute                                        **
** Purpose:     To check if the value of MINUTE is VALID or NOT before  **
**              displaying it.                                          **
**                                                                      **
** Arguments:   minute - the value needs to be checked                  **
**                                                                      **
** Returns:     1 - VALID                                               **
**              0 - INVALID                                             **
**                                                                      **
*************************************************************************/
int ODBC_validMinute ( unsigned int minute )
{
	if ( ( minute >= 0 ) && ( minute <= 59 ) )
		return ( 1 );
	else
		return ( 0 );
}


/*************************************************************************
** function:    ODBC_validSecond                                        **
** Purpose:     To check if the value of SECOND is VALID or NOT before  **
**              displaying it.                                          **
**                                                                      **
** Arguments:   second - the value needs to be checked                  **
**                                                                      **
** Returns:     1 - VALID                                               **
**              0 - INVALID                                             **
**                                                                      **
*************************************************************************/
int ODBC_validSecond ( unsigned int second )
{
	if ( ( second >= 0 ) && ( second <= 61 ) )
		return ( 1 );
	else
		return ( 0 );
}


/*************************************************************************
** function:    ODBC_validBuffer                                        **
**                                                                      **
** Purpose:     Checks if the buffer is VALID or NOT                    **
**                                                                      **
** Arguments:   szBuffer - point to memory block                        **
**                                                                      **
** Returns:     none                                                    **
**                                                                      **
*************************************************************************/
void ODBC_validBuffer ( char* szBuffer )
{
	if ( szBuffer == NULL )
	{
		printf( "Insufficient memory available\n" );
		exit ( 255 );
	}
}

/*************************************************************************
** function:    ODBC_fetchData                                          **
**                                                                      **
** Purpose:     fetches and displays results from SELECT statement      **
**                                                                      **
** Arguments:   hstmt    - Statement handle                             **
**              henv     - Environment handle                           **
**              hdbc     - Connection handle                            **
**              nColumns - number of columns returned from SELECT stmt  **
**                                                                      **
** Returns:     none                                                    **
**                                                                      **
*************************************************************************/
#define INVALIDPTR ((char *)1)
void ODBC_fetchData ( HSTMT hstmt, HENV henv, HDBC hdbc, int nColumns )
{
    RETCODE              rc;
	SQL_TIMESTAMP_STRUCT dateTimeStamp;
    SQL_TIME_STRUCT      time;
	int                  *columnSizes;
	int                  reqlen; 
	SQLINTEGER           nRows = 0;
	char                 *szBuffer;
	char                 *tempBuffer = NULL;
	char                 *szBorder;
	SQLINTEGER           intData;
	float                floatData = 0.0f;
	double               doubleData = 0.0f;
	char                 decimalData[500];
	char                 utilbuff[500];
	char                 *szOutputBuffer = NULL; /* contains the output data of each row for the SELECT statement */
	int                  column = 0;   /* column index */
	SWORD                dataType;
	UDWORD               rowSize, actualSize;
	SQLINTEGER           dataLength = SQL_NULL_DATA;   /* the amount of data is waiting... */
	int                  counter = 0;
	int                  prevLength = 0;
	char				 dummy[1];
	
	/* display column name */
	columnSizes = (int *)calloc( nColumns, sizeof ( int) );
	if ( columnSizes == NULL )
	{
		printf( "Insufficient memory available\n" );
		exit ( 255 );
	}
	rowSize = ODBC_getRowSize ( 
		hstmt, 
		nColumns, 
		columnSizes );
	szBorder = (char*)calloc( rowSize, sizeof(char) );
	ODBC_validBuffer ( szBorder );
	ODBC_displayHeader( hstmt, nColumns, rowSize, columnSizes, 		szBorder ); 
	for ( nRows = 0; ; nRows++ )
	{
		rc = SQLFetch ( hstmt );
		if ( rc != SQL_SUCCESS )
			break;
		szOutputBuffer = (char*)calloc( rowSize, sizeof(char) );
		ODBC_validBuffer ( szOutputBuffer ); 
		strcpy ( szOutputBuffer, "|" );
		for ( column = 1; column <= nColumns; column++ )
		{
			SQLDescribeCol(hstmt, column, NULL, 0, NULL, &dataType, NULL, NULL, NULL);
	                switch ( dataType )
                        {		

			case SQL_DECIMAL:
                        case SQL_NUMERIC:
                                SQLGetData ( hstmt, column, SQL_C_DEFAULT, decimalData, 500, &dataLength );
				if ( dataLength > 0 )
				{      
					ODBC_fillBlank ( szOutputBuffer, dataLength, columnSizes [ column - 1 ] );
					strcat ( szOutputBuffer, decimalData );
				}
                                else
                                        ODBC_fillBlank ( szOutputBuffer, 0, columnSizes[column - 1] ); 
                                break;
                        case SQL_WVARCHAR:
			case SQL_WCHAR:
			case SQL_CHAR:
            		case SQL_VARCHAR:
            		case SQL_LONGVARCHAR:
            		case SQL_WLONGVARCHAR:
				/* Call SQLGetData to determine the amount of data that's waiting. */
                                reqlen = 0;
                		SQLGetData( hstmt, column, SQL_C_CHAR, dummy, 0, &dataLength);
				if ( dataLength > 0 )
				{
					szBuffer = (char*)calloc( dataLength +1, sizeof(char) );
					ODBC_validBuffer ( szBuffer ); 
					if ( SQLGetData ( hstmt, column, SQL_C_CHAR, szBuffer, dataLength + 1, 
						&dataLength ) == SQL_SUCCESS )
					{
						if ( dataLength > columnSizes[column - 1] )
						{
							actualSize = rowSize + ( dataLength - columnSizes[column - 1] );
							tempBuffer = (char*)calloc( actualSize, sizeof(char) );
							ODBC_validBuffer ( tempBuffer );
							strcpy ( tempBuffer, szOutputBuffer );
							free ( szOutputBuffer );
							szOutputBuffer = (char*)calloc( actualSize, sizeof(char*) );
							ODBC_validBuffer ( szOutputBuffer );
							strcpy ( szOutputBuffer, tempBuffer );
							free ( tempBuffer );
						}
						strncat ( szOutputBuffer, szBuffer, dataLength );
                                                ODBC_fillBlank( szOutputBuffer, dataLength, columnSizes [ column - 1 ] );
					}
					free ( szBuffer );
				}
                else
				{
                    ODBC_fillBlank ( szOutputBuffer, 0, columnSizes[column - 1] );
				}
			        break;
			case SQL_TINYINT:
                        case SQL_INTEGER:
                        case SQL_SMALLINT:
                                rc = SQLGetData( hstmt, column, SQL_C_LONG, &intData, sizeof ( intData ), NULL );
				if ( rc == SQL_SUCCESS )
				{
					prevLength = strlen ( szOutputBuffer );
					counter = sprintf ( szOutputBuffer + prevLength, "%d", intData );
			                szOutputBuffer[ strlen ( szOutputBuffer ) - counter ] = '\0';
					ODBC_fillBlank ( szOutputBuffer, counter, columnSizes[column - 1] );
					sprintf ( szOutputBuffer + strlen ( szOutputBuffer ), "%d", intData );
				}
                                else
                                        ODBC_fillBlank ( szOutputBuffer, 0, columnSizes[column - 1] );
			        break;
			case SQL_TYPE_TIMESTAMP:
				if ( SQLGetData( hstmt, column, SQL_C_TYPE_TIMESTAMP, &dateTimeStamp,
					sizeof ( dateTimeStamp ), NULL ) != SQL_ERROR )
				{
					counter = sprintf( szOutputBuffer + strlen(szOutputBuffer),
						"%.4d-%.2d-%.2d", dateTimeStamp.year, 
						dateTimeStamp.month, dateTimeStamp.day );
					if ( ( ODBC_validHour( dateTimeStamp.hour) == 1 ) &&
						( ODBC_validMinute( dateTimeStamp.minute) == 1 ) &&
						( ODBC_validSecond( dateTimeStamp.second) == 1) )
					{
						counter += sprintf( szOutputBuffer + strlen(szOutputBuffer),
							" %.2d:%.2d:%.2d", dateTimeStamp.hour,
							dateTimeStamp.minute, dateTimeStamp.second );
					}
					ODBC_fillBlank ( szOutputBuffer, counter, columnSizes[column - 1] );
				}  
                                else
                                        ODBC_fillBlank ( szOutputBuffer, 0, columnSizes[column - 1] );
                                break;
                        case SQL_TYPE_TIME:
                                if ( SQLGetData( hstmt, column, SQL_C_TYPE_TIME, &time, sizeof ( time ), NULL ) != SQL_ERROR )
                                {
                                        if ( ( ODBC_validHour( time.hour) == 1 ) && ( ODBC_validMinute( time.minute) == 1 ) &&
                                                ( ODBC_validSecond( time.second) == 1) )
                                        {
                                                counter = sprintf( szOutputBuffer + strlen(szOutputBuffer),
                                                                   "%.2d:%.2d:%.2d", time.hour, time.minute, time.second );
                                        }
                                        ODBC_fillBlank ( szOutputBuffer, counter, columnSizes[column - 1] );
                                }
                                else
                                        ODBC_fillBlank ( szOutputBuffer, 0, columnSizes[column - 1] );
                                break;
			case SQL_FLOAT:
                        case SQL_DOUBLE:
				if ( SQLGetData( hstmt, column, SQL_C_DOUBLE, &doubleData,
					sizeof ( szOutputBuffer ),
					NULL ) != SQL_ERROR )
				{
					counter = ODBC_formatData ( szOutputBuffer, doubleData, 
						lowerBound, upperBound );
                                        szOutputBuffer[ strlen ( szOutputBuffer ) - counter ] = '\0';  
					ODBC_fillBlank ( szOutputBuffer, counter, columnSizes[column - 1] );
					ODBC_formatData ( szOutputBuffer, doubleData, lowerBound, upperBound );
				}
                                else
                                        ODBC_fillBlank ( szOutputBuffer, 0, columnSizes[column - 1] );
                                break;
			case SQL_REAL:
				if ( SQLGetData( hstmt, column, SQL_C_FLOAT, &floatData,
					sizeof ( floatData ), NULL ) != SQL_ERROR )
				{
					counter = ODBC_formatData ( szOutputBuffer, floatData, 
						lowerValue, upperValue );
                                        szOutputBuffer[ strlen ( szOutputBuffer ) - counter ] = '\0';            
					ODBC_fillBlank ( szOutputBuffer, counter, columnSizes[column - 1] );
					ODBC_formatData ( szOutputBuffer, floatData, lowerValue, upperValue );
				}
                                else
                                        ODBC_fillBlank ( szOutputBuffer, 0, columnSizes[column - 1] );  
                                break;
                        default:
				ODBC_fillBlank ( szOutputBuffer, 3, columnSizes[column - 1] );
				strcat ( szOutputBuffer, "N/A" );
                                break;
			}
			strcat ( szOutputBuffer, "|" );
		}
		printf( "%s\n", szOutputBuffer );
		free ( szOutputBuffer );
	}
	printf( "%s\n", szBorder );
	SQLCloseCursor (hstmt);
	printf("\n(%d %s)\n", nRows, (nRows > 1) ? "rows" : "row");
	/* free memory */
	free ( columnSizes );
	free ( szBorder ); 

}

/*************************************************************************
** function:    ODBC_execQuery                                          **
**                                                                      **
** Purpose:     Executes a SQL statement                                **
**                                                                      **
** Arguments:   stmt   - Pointer to SQL stmt                            **
**              henv   - Environment handle                             **
**              hdbc   - Connection handle                              **
**              hstmt  - Statement handle                               **
** Returns:     none                                                    **
**                                                                      **
*************************************************************************/
void ODBC_execQuery(char* stmt, HENV henv, HDBC hdbc, HSTMT hstmt)
{
	RETCODE              rc;
	SQLINTEGER           nRows = 0;    /* number of rows affected by an UPDATE, SELECT, INSERT, or DELETE statement */ 
	SWORD                nColumns = 0; /* number of columns in a result set */

	/*
	* Execute SQL statement
	*/
	printf("* %s\\g\n", stmt);
	printf("Executing . . .\n\n");
	rc = SQLExecDirect(hstmt, (unsigned char*)stmt, SQL_NTS);
	if( rc != SQL_SUCCESS )
	{
		if( rc == SQL_SUCCESS_WITH_INFO )
		{
			printf("Statement returned message:\n");
			printf("\t");
		}
		ODBC_getErrorInfo (henv, hdbc, hstmt);
		if( rc != SQL_SUCCESS_WITH_INFO )
		{
			SQLCloseCursor (hstmt);
		}
	}
	else
	{
		/* check if the current statement is a SELECT statement */
		SQLNumResultCols ( hstmt, &nColumns );
		if ( nColumns == 0 ) /* No, the statement is not a SELECT statement */
		{
			/* get number of rows affected by an UPDATE, INSERT, or DELETE statement */
			SQLRowCount ( hstmt, &nRows );
			if( nRows != 0 )
			{
				printf("(%d %s)\n", nRows, (nRows > 1) ? "rows" : "row");
			}  
			SQLCloseCursor (hstmt);
		}
		else /* Yes, the current statement is a SELECT statement */
		{
                        ODBC_fetchData ( hstmt, henv, hdbc, nColumns );
		} 
	}
}


/*************************************************************************
** function:    ODBC_formatData                                         **
** Purpose:     Displays float or double data in the correct way        **
**                                                                      **
** Arguments:   szOutputBuffer - output buffer to be displayed          **
**              targetData     - value needs to be formated             **
**              lowerBound     - minimum value                          **
**              upperBound     - maximum value                          **
**                                                                      **
** Returns:     none                                                    **
**                                                                      **
*************************************************************************/   
int ODBC_formatData ( char szOutputBuffer[], double targetData, double lowerBound, double upperBound )
{
	int counter;
	
	if ( targetData > 0.0 )
	{
		counter = sprintf( szOutputBuffer + strlen ( szOutputBuffer ),
			( targetData < upperBound ) ? "%.3f" : "%.3e", targetData );
	}
	else
	{
		counter = sprintf( szOutputBuffer + strlen ( szOutputBuffer ),
			( targetData > lowerBound ) ? "%.3f" : "%.3e", targetData ); 
	}
	
	return ( counter );
}


/*************************************************************************
** function:    ODBC_displayHeader                                      **
** Purpose:     Displays all the column names from SELECT statement     **
**                                                                      **
** Arguments:   hstmt       - Statement handle                          **
**              nColumns    - number of columns                         **
**              rowSize     - maximum size of each row                  **
**              columnSizes - maximum size of each column               **
**              szBorder    - table border                              **
**                                                                      **
** Returns:     none                                                    **
**                                                                      **
*************************************************************************/
void ODBC_displayHeader( HSTMT hstmt, SWORD nColumns, UDWORD rowSize, int columnSizes[], char szBorder[] )
{
	int        i, column;    /* column index */
	SQLCHAR    szColumnName[SQL_MAX_COLUMN_NAME_LEN];
	char*      szHeader;
	SWORD      nameLength;   /* actual column name's length */
	RETCODE    rc;
	UDWORD     nLength = 0;
	SWORD      dataType;
	
	szHeader = (char*)calloc( rowSize, sizeof(char) );
	
	strcpy ( szBorder, "+" );
	strcpy ( szHeader, "|" );
	
	for ( i = 1; i < (int) (rowSize - 1); i++ )
	{
		if ( i == (int) (rowSize - 2 ) )
			strcat ( szBorder, "+" );
		else
			strcat ( szBorder, "-" );
	}
	
	szBorder[ rowSize - 1 ] = '\0';
	
	for ( column = 1; column <= nColumns; column++ )
	{
		rc = SQLDescribeCol ( hstmt, column, szColumnName, SQL_MAX_COLUMN_NAME_LEN,
  				      &nameLength, &dataType, NULL, NULL, 
					  NULL );
		
		strcat ( szHeader, (char*) szColumnName );
		if ( nameLength < columnSizes[ column - 1] )
		{
			for( i = 1; i <= abs ( columnSizes[ column - 1] - nameLength ); i++ )
				strcat ( szHeader, " " );
		}
		strcat ( szHeader, "|");
		nLength =  strlen( szHeader );
		if ( column != nColumns )
			szBorder[ nLength - 1 ] = '+';
	}
	szHeader[ rowSize - 1] = '\0';  
	
	
	printf( "%s\n", szBorder );
	printf( "%s\n", szHeader );
	printf( "%s\n", szBorder );
	
	free ( szHeader );
}

/*************************************************************************
** function:    ODBC_connect                                            **
**                                                                      **
** Purpose:     Allocates ODBC HENV and HDBC.                           **
**                                                                      **
** Arguments:   henv    - Pointer to environment handle                 **
**              hdbc    - Pointer to connection handle                  **
**                                                                      **
** Returns:     RETCODE - Return status from last ODBC Function.        **
**                                                                      **
**************************************************************************/

RETCODE ODBC_connect(                   /* Perform Driver Connection    */
					 HENV henv,         /* ODBC Environment Handle      */
					 HDBC hdbc,         /* ODBC Connection Handle       */
					 UCHAR *driver,     /* Data Source Name             */
					 UCHAR *uid,        /* User ID                      */
					 UCHAR *pwd)        /* User Password                */
{
	RETCODE rc;
	int     retries;
	
#if defined(TRACING)
	rc = SQLSetConnectOption (hdbc, SQL_OPT_TRACE, 1);
	if ((rc != SQL_SUCCESS) && (rc != SQL_SUCCESS_WITH_INFO))
		return (rc);
#endif
		/*
		** try connecting up to 3 times
    */
	printf("\nPlease wait . . .\n");
	printf("Establishing a connection to '%s' . . .\n", driver);
	for (retries = 1; retries <= 3; retries++) 
	{
		printf("Attempt connecting %d of 3 . . .\n", retries );
		rc = SQLConnect (hdbc, driver, SQL_NTS, uid, SQL_NTS, pwd, SQL_NTS);
		if ((rc == SQL_SUCCESS) || (rc == SQL_SUCCESS_WITH_INFO))
		{
			printf("Successfully, connected to '%s'.\n", driver);
			return (rc);
		}
		else 
		{
			printf("Unable to establish a connection to '%s'.\n", driver);
			ODBC_getErrorInfo (henv, hdbc, SQL_NULL_HSTMT);
			if(retries < 3)
				printf("Retrying connect . . .\n");
		}
	}
	/*
	** Attempt to obtain a meaningful error as to why connect failed.
	*/
	printf("No connection could be established.\n");
	ODBC_getErrorInfo(henv, hdbc, SQL_NULL_HSTMT);
	return (SQL_INVALID_HANDLE);
}


int main(int argc, char * argv[])
{
	HDBC    hdbc;
	HENV    henv;
	HSTMT   hstmt;
	RETCODE rc;
	UCHAR   uid[UID_LEN];
	UCHAR   pwd[PWD_LEN];
	UCHAR   file[OPT1_LEN];
	UCHAR   dsn[DSN_LEN];
	
	uid[0]  = (UCHAR)NULL;
	file[0] = (UCHAR)NULL;
	dsn[0]  = (UCHAR)NULL;
	pwd[0]  = (UCHAR)NULL;
	
	if (argc > 1) 
	{
		int argIndex;   /* Input argument index         */
		int argCount;   /* Running count of arguments   */
		
		for (argCount=argc-1, argIndex=1; argCount > 0;
		argIndex++, argCount--) 
		{
			switch (ArgParse(argv[argIndex])) 
			{
			case HELP_ARG:
				printf(USAGE_MSG1, argv[0]);
				return(1);
			case PWD_ARG:
				argIndex++;
				if (argCount <= 1) 
				{
					printf(PWD_ERR1);
					printf(USAGE_MSG1, argv[0]);
					return(1);
				}
				if (strlen(argv[argIndex]) > PWD_LEN) 
				{
					printf(PWD_MSG1, PWD_LEN);
					return(1);
				}
				strcpy((char*)pwd, argv[argIndex]);
				argCount--;
				break;
			case UID_ARG:
				argIndex++;
				if (argCount <= 1) {
					printf(UID_ERR1);
					printf(USAGE_MSG1, argv[0]);
					return(1);
				}
				if (strlen(argv[argIndex]) > UID_LEN) 
				{
					printf(UID_MSG1, UID_LEN);
					return(1);
				}
				strcpy((char*)uid, argv[argIndex]);
				argCount--;
				break;
			case DSN_ARG:
				argIndex++;
				if (argCount <= 1) 
				{
					printf(DSN_ERR1);
					printf(USAGE_MSG1, argv[0]);
					return(1);
				}
				if (strlen(argv[argIndex]) > DSN_LEN) 
				{
					printf(DSN_MSG1, DSN_LEN);
					return(1);
				}
				strcpy((char*)dsn, argv[argIndex]);
				argCount--;
				break;
			case FILE_ARG:
				argIndex++;
				if (argCount <= 1) 
				{
					printf(FILE_ERR1);
					printf(USAGE_MSG1, argv[0]);
					return(1);
				}
				if (strlen(argv[argIndex]) > OPT1_LEN)
				{
					printf(FILE_MSG1, OPT1_LEN);
					return(1);
				}
				strcpy((char*)file, argv[argIndex]);
				argCount--;
				break;
			}
		}
	}
	else 
	{
		printf(USAGE_MSG1, argv[0]);
		return(1);
	}
#if !defined (__cplusplus) && defined (hppa)
	/*
	** C programs must call the HP C++ Object initializer function.
	*/
	_main ();
#endif
	
	rc = SQLAllocHandle (SQL_HANDLE_ENV, SQL_NULL_HANDLE, &henv);
	if ((rc != SQL_SUCCESS) && (rc != SQL_SUCCESS_WITH_INFO))
	{
		printf("Failed to initialize ODBC environmment...\n");
		exit(255);      /* Exit with failure */
	}
	ODBC_initialize ( &henv );
	ODBC_prepareCon ( henv, &hdbc );
	rc = ODBC_connect(henv, hdbc, dsn, uid, pwd);
	if ((rc != SQL_SUCCESS) && (rc != SQL_SUCCESS_WITH_INFO))
		exit(255);      /* Exit with failure */
						/*
						** Allocate a HSTMT to communicate with ODBC DB Driver.
	*/
	rc = SQLAllocHandle(SQL_HANDLE_STMT, hdbc, &hstmt);
	if ((rc != SQL_SUCCESS) && (rc != SQL_SUCCESS_WITH_INFO)) 
	{
		printf ("Unable to Allocate a statement handle.\n");
		ODBC_getErrorInfo (henv, hdbc, hstmt);
		ODBC_releaseEnv(henv, hdbc);
		exit (255);
	}
	
	if(file[0] == (UCHAR) NULL)
	{
		printf("\nPlease enter SQL script file: ");
		scanf("%s", (char*)file);	
	}
	ODBC_fileRead ((char*)file, henv, hdbc, hstmt);
	
	SQLFreeHandle (SQL_HANDLE_STMT, hstmt);
	ODBC_releaseEnv (henv, hdbc);
	
	return(0);
}
