# include <stdio.h>
# include <stdlib.h>
# include "iiapi.h"

# define	ROW_COUNT	10000
# define	COL_COUNT	3
# define	UPDATABLE	FALSE
# define	SCROLLABLE	FALSE
# define	VERBOSE		FALSE
# define	EXPLICIT	TRUE

# define	TXT_LEN		32

typedef struct
{
    II_UINT2	len;
    char	txt[ TXT_LEN + 1 ];
} VARCHAR;

typedef struct
{
    II_INT4	key;
    II_INT4	id;
    VARCHAR	txt;
} ROW_DATA;

#define	DROP_TABLE_TEXT "drop table api_scroll00" 

#define	CREATE_TABLE_TEXT \
"create table api_scroll00( key integer not null primary key, \
                          id integer, txt varchar(%d) )"

static  char		*prog = "prog";
static  char	        test_number = '1';	

static	II_INT4		row_count = ROW_COUNT;
static	II_INT4		col_count = COL_COUNT;
static	II_BOOL		updatable = UPDATABLE;
static	II_BOOL		scrollable = SCROLLABLE;
static	II_BOOL		explicit = EXPLICIT;
static	II_BOOL		verbose = VERBOSE;
static	II_BOOL		veryverbose = FALSE;
static	II_BOOL		createtab = FALSE;

static	II_INT4		rowIDs[ ROW_COUNT + 1 ];
static	ROW_DATA	rowData[ ROW_COUNT ];
static	IIAPI_DATAVALUE	rowDV[ ROW_COUNT * COL_COUNT ];

static	II_BOOL		endOfData;
static	II_INT4		resultRows;
static	II_UINT2	cursorType;
static	II_UINT2	cursorStatus;
static	II_INT4		cursorPosition;

static	void		usage();
static	void		check_row( char *, II_PTR *, 
				   II_INT4, II_INT2, II_INT2 );
static	IIAPI_STATUS	api_init( II_PTR * );
static	IIAPI_STATUS	api_term( II_PTR * );
static	IIAPI_STATUS	api_conn( char *, II_PTR *, II_PTR * );
static	IIAPI_STATUS	api_disconn( II_PTR * );
static	IIAPI_STATUS	api_exec( II_PTR *, II_PTR *, char * );
static	IIAPI_STATUS	api_cursor_op( II_PTR *, II_PTR *, II_PTR *, 
				       IIAPI_QUERYTYPE, char * );
static	IIAPI_STATUS	api_open( II_PTR *, 
				  II_PTR *, II_PTR *, char *, II_BOOL );
static	IIAPI_STATUS	api_scroll( II_PTR *, II_UINT2, II_INT4 );
static	IIAPI_STATUS	api_pos( II_PTR *, II_UINT2, II_INT4, II_INT2 );
static	II_INT2		api_get( char *, II_PTR *, II_INT2, II_INT4 );
static	IIAPI_STATUS	api_gqi( II_PTR * );
static	IIAPI_STATUS	api_close( II_PTR * );
static	IIAPI_STATUS	api_commit( II_PTR * );
static	IIAPI_STATUS	api_rollback( II_PTR * );
static	IIAPI_STATUS	api_status( IIAPI_GENPARM * );


int
main( int argc, char **argv )
{
    II_PTR		envHndl1 = NULL;
    II_PTR		connHndl1 = NULL;
    II_PTR		tranHndl1 = NULL;
    II_PTR		stmtHndl1 = NULL;
    IIAPI_STATUS	status;
    II_INT4		target;
    II_INT2		rows, last;
    char		qtxt[128], mtxt[40];
    int			i, j;

    prog = argv[0];
    if ( argc < 2  ||  argv[1][0] == '-' )  usage();

    for( i = 2; i < argc; i++ )
    {
        if ( argv[i][0] != '-' )
	{
	    usage();
	    break;
	}

	switch( argv[i][1] )
	{
	case 'c' : createtab = TRUE;	break;
	case 'u' : 
	    updatable = TRUE;	
	    switch( argv[i][2] )
	    {
	    case 'i' : explicit = FALSE;	break;
	    case 'e' : explicit = TRUE;		break;
	    }
	    break;

	case 's' : scrollable = TRUE;	break;
	case 't' : test_number = argv[i][2]; break;
	case 'v' : verbose = TRUE;	break;
	default  : usage();
	}
    }

    api_init( &envHndl1 ); 
    if ( veryverbose )  printf( "%s: Connect to %s\n", prog, argv[1] );
    api_conn( argv[1], &envHndl1, &connHndl1 );

    if ( createtab )
    {
/*
        if ( veryverbose )  printf( "%s: DROP table\n", prog );
        sprintf( qtxt, DROP_TABLE_TEXT, TXT_LEN );
        api_exec( &connHndl1, &tranHndl1, qtxt );
*/
        if ( veryverbose )  printf( "%s: CREATE table\n", prog );
        sprintf( qtxt, CREATE_TABLE_TEXT, TXT_LEN );
        api_exec( &connHndl1, &tranHndl1, qtxt );

        for( i = 1, j = 0; i <= row_count; i++ )
        {
	    sprintf(qtxt, "insert into api_scroll00 values(%d,%d,'Row #%d')", i,i,i);
	    api_exec( &connHndl1, &tranHndl1, qtxt );
        }
    }
    for( i = 1, j = 0; i <= row_count; i++ )
    {
        rowIDs[ i ] = i;
        rowDV[ j++ ].dv_value = &rowData[ i - 1 ].key;
        rowDV[ j++ ].dv_value = &rowData[ i - 1 ].id;
        rowDV[ j++ ].dv_value = &rowData[ i - 1 ].txt;
    }

    if ( veryverbose )  printf( "%s: Open %s %s cursor\n", prog, 
	    updatable ? "updatable" : "readonly",
	    scrollable ? "scrollable" : "non-scrollable" );
    sprintf( qtxt, "select key, id, txt from api_scroll00%s", 
    		   updatable ? (explicit ? " for direct update" : "") 
		   	     : " for readonly" );
    if ( veryverbose )  printf("QUERY TEXT: %s\n", qtxt);
    api_open( &connHndl1, &tranHndl1, &stmtHndl1, qtxt, scrollable );
    api_gqi( &stmtHndl1 );

    if ( cursorType != ((scrollable ? IIAPI_CURSOR_SCROLL: 0) | 
    			(updatable ? IIAPI_CURSOR_UPDATE: 0)) )
    	printf( "%s: !!! Cursor attributes incorrect: 0x%x\n", 
		prog, cursorType );
    else  if ( veryverbose )
    	printf( "%s: Opened %s %s cursor\n", prog,
		(cursorType & IIAPI_CURSOR_UPDATE) ? "updatable" : "readonly",
		(cursorType & IIAPI_CURSOR_SCROLL) ? "scrollable" 
						   : "non-scrollable" );

# define SCROLL_CMD(MSG,SCMD,TARGET,ROWS,OFFSET) \
        sprintf(mtxt, "%03d %s", i, MSG); \
        api_scroll( &stmtHndl1, SCMD, OFFSET ); \
        rows = api_get( mtxt, &stmtHndl1, ROWS, TARGET ); \
        check_row( mtxt, &stmtHndl1, TARGET, ROWS, rows ); \
        i++;

    i = 1;

    if (test_number == '1')
    {
         SCROLL_CMD("FIR",IIAPI_SCROLL_FIRST,1,1,0)
         SCROLL_CMD("LAS",IIAPI_SCROLL_LAST,row_count,1,0)
         SCROLL_CMD("AFT",IIAPI_SCROLL_AFTER,row_count+1,0,0)
         SCROLL_CMD("BEF",IIAPI_SCROLL_BEFORE,0,0,0)
         SCROLL_CMD("ABS (rowcount/2)",IIAPI_SCROLL_ABSOLUTE,row_count/2,1,row_count/2)
    
         SCROLL_CMD("FIR",IIAPI_SCROLL_FIRST,1,1,0)
         SCROLL_CMD("NEX",IIAPI_SCROLL_NEXT,2,1,0)
         SCROLL_CMD("FIR",IIAPI_SCROLL_FIRST,1,1,0)
         SCROLL_CMD("CUR",IIAPI_SCROLL_CURRENT,1,1,0)
         SCROLL_CMD("FIR",IIAPI_SCROLL_FIRST,1,1,0)
         SCROLL_CMD("PRI",IIAPI_SCROLL_PRIOR,0,0,0)
    
         SCROLL_CMD("LAS",IIAPI_SCROLL_LAST,row_count,1,0)
         SCROLL_CMD("NEX",IIAPI_SCROLL_NEXT,row_count+1,0,0)
         SCROLL_CMD("LAS",IIAPI_SCROLL_LAST,row_count,1,0)
         SCROLL_CMD("CUR",IIAPI_SCROLL_CURRENT,row_count,1,0)
         SCROLL_CMD("LAS",IIAPI_SCROLL_LAST,row_count,1,0)
         SCROLL_CMD("PRI",IIAPI_SCROLL_PRIOR,row_count-1,1,0)

         SCROLL_CMD("ABS (rowcount/2)",IIAPI_SCROLL_ABSOLUTE,row_count/2,1,row_count/2)
         SCROLL_CMD("NEX",IIAPI_SCROLL_NEXT,(row_count/2)+1,1,0)
         SCROLL_CMD("ABS (rowcount/2)",IIAPI_SCROLL_ABSOLUTE,row_count/2,1,row_count/2)
         SCROLL_CMD("CUR",IIAPI_SCROLL_CURRENT,row_count/2,1,0)
         SCROLL_CMD("ABS (rowcount/2)",IIAPI_SCROLL_ABSOLUTE,row_count/2,1,row_count/2)
         SCROLL_CMD("PRI",IIAPI_SCROLL_PRIOR,(row_count/2)-1,1,0)

         SCROLL_CMD("BEF",IIAPI_SCROLL_BEFORE,0,0,0)
         SCROLL_CMD("NEX",IIAPI_SCROLL_NEXT,1,1,0)
         SCROLL_CMD("BEF",IIAPI_SCROLL_BEFORE,0,0,0)
         SCROLL_CMD("CUR",IIAPI_SCROLL_CURRENT,0,0,0)
         SCROLL_CMD("BEF",IIAPI_SCROLL_BEFORE,0,0,0)
         SCROLL_CMD("PRI",IIAPI_SCROLL_PRIOR,0,0,0)

         SCROLL_CMD("AFT",IIAPI_SCROLL_AFTER,row_count+1,0,0)
         SCROLL_CMD("NEX",IIAPI_SCROLL_NEXT,row_count+1,0,0)
         SCROLL_CMD("AFT",IIAPI_SCROLL_AFTER,row_count+1,0,0)
         SCROLL_CMD("CUR",IIAPI_SCROLL_CURRENT,row_count+1,0,0)
         SCROLL_CMD("AFT",IIAPI_SCROLL_AFTER,row_count+1,0,0)
         SCROLL_CMD("PRI",IIAPI_SCROLL_PRIOR,row_count,1,0)
    
         SCROLL_CMD("ABS (2)",IIAPI_SCROLL_ABSOLUTE,2,1,2)
         SCROLL_CMD("NEX",IIAPI_SCROLL_NEXT,3,1,0)
         SCROLL_CMD("ABS (2)",IIAPI_SCROLL_ABSOLUTE,2,1,2)
         SCROLL_CMD("CUR",IIAPI_SCROLL_CURRENT,2,1,0)
         SCROLL_CMD("ABS (2)",IIAPI_SCROLL_ABSOLUTE,2,1,2)
         SCROLL_CMD("PRI",IIAPI_SCROLL_PRIOR,1,1,0)

         SCROLL_CMD("ABS (rowcount-1)",IIAPI_SCROLL_ABSOLUTE,row_count-1,1,row_count-1)
         SCROLL_CMD("NEX",IIAPI_SCROLL_NEXT,row_count,1,0)
         SCROLL_CMD("ABS (rowcount-1)",IIAPI_SCROLL_ABSOLUTE,row_count-1,1,row_count-1)
         SCROLL_CMD("CUR",IIAPI_SCROLL_CURRENT,row_count-1,1,0)
         SCROLL_CMD("ABS (rowcount-1)",IIAPI_SCROLL_ABSOLUTE,row_count-1,1,row_count-1)
         SCROLL_CMD("PRI",IIAPI_SCROLL_PRIOR,row_count-2,1,0)

         SCROLL_CMD("ABS (-1)",IIAPI_SCROLL_ABSOLUTE,row_count,1,-1)
         SCROLL_CMD("ABS (-2)",IIAPI_SCROLL_ABSOLUTE,row_count-1,1,-2)
         SCROLL_CMD("ABS (-rowcount)",IIAPI_SCROLL_ABSOLUTE,1,1,-row_count)
         SCROLL_CMD("ABS (-(rowcount+1))",IIAPI_SCROLL_ABSOLUTE,0,1,-(row_count+1))
         SCROLL_CMD("ABS (-(rowcount+2))",IIAPI_SCROLL_ABSOLUTE,0,1,-(row_count+2))
         SCROLL_CMD("ABS (0)",IIAPI_SCROLL_ABSOLUTE,0,1,0)
         SCROLL_CMD("ABS (1)",IIAPI_SCROLL_ABSOLUTE,1,1,1)
         SCROLL_CMD("ABS (2)",IIAPI_SCROLL_ABSOLUTE,2,1,2)
         SCROLL_CMD("ABS (rowcount)",IIAPI_SCROLL_ABSOLUTE,row_count,1,row_count)
         SCROLL_CMD("ABS (rowcount+1)",IIAPI_SCROLL_ABSOLUTE,row_count+1,1,row_count+1)
         SCROLL_CMD("ABS (rowcount+2)",IIAPI_SCROLL_ABSOLUTE,row_count+1,1,row_count+2)

         SCROLL_CMD("BEF",IIAPI_SCROLL_BEFORE,0,0,0)
         SCROLL_CMD("REL (1)",IIAPI_SCROLL_RELATIVE,1,1,1)
         SCROLL_CMD("REL (1)",IIAPI_SCROLL_RELATIVE,2,1,1)
         SCROLL_CMD("REL (2)",IIAPI_SCROLL_RELATIVE,4,1,2)
         SCROLL_CMD("REL (4)",IIAPI_SCROLL_RELATIVE,8,1,4)
         SCROLL_CMD("REL (8)",IIAPI_SCROLL_RELATIVE,16,1,8)
         SCROLL_CMD("REL (16)",IIAPI_SCROLL_RELATIVE,32,1,16)
         SCROLL_CMD("REL (0)",IIAPI_SCROLL_RELATIVE,32,1,0)
         SCROLL_CMD("REL (rowcount-32)",IIAPI_SCROLL_RELATIVE,row_count,1,row_count-32)
         SCROLL_CMD("REL (0)",IIAPI_SCROLL_RELATIVE,row_count,1,0)
         SCROLL_CMD("REL (-(rowcount-1))",IIAPI_SCROLL_RELATIVE,1,1,-(row_count-1))
         SCROLL_CMD("REL (-(rowcount-1))",IIAPI_SCROLL_RELATIVE,row_count+1,0,row_count+100)
         SCROLL_CMD("REL (-1)",IIAPI_SCROLL_RELATIVE,row_count,1,-1)
         SCROLL_CMD("REL (-(rowcount/2))",IIAPI_SCROLL_RELATIVE,row_count/2,1,-(row_count/2))
         SCROLL_CMD("REL (-rowcount)",IIAPI_SCROLL_RELATIVE,0,0,-row_count)
         SCROLL_CMD("REL (-rowcount)",IIAPI_SCROLL_RELATIVE,0,0,-row_count)
         SCROLL_CMD("REL (1)",IIAPI_SCROLL_RELATIVE,1,1,1)
    }
 
    api_close( &stmtHndl1 );
    api_commit( &tranHndl1 );
    api_disconn( &connHndl1 );
    api_term( &envHndl1 );
}

static void
usage()
{
    printf( "Usage: %s dbname -tn -c -s -u[i,e] -v\n", prog );
    printf( "       -tn: run test no. n\n" );
    printf( "       -c:  (re)create test table\n" );
    printf( "       -s:  use scrollable cursor\n" );
    printf( "       -ui: updateable (implicit)\n" );
    printf( "       -ue: updateable (direct update)\n" );
    printf( "       -v:  verbose output mode\n" );
    exit( 0 );
}

static void
check_row( char *test, II_PTR *stmtHandle,
	   II_INT4 target, II_INT2 rows_exp, II_INT2 rows_act )
{
    II_UINT2	flags = 0;

    if ( rows_act < rows_exp )
    {
        target += rows_act;	/* After last returned */

        if ( target >= 1  &&  target <= row_count  &&  rowIDs[target] != 0 )
    	    printf( "%s: !!! Expected row @ %d\n", test, target );
    }
    else  if ( rows_act > 0 )
    {
        if ( rows_act > rows_exp )
	{
	    II_INT4 t_row = target + rows_exp;

	    printf( "%s: !!! Expected %d rows, received %d\n", 
	    	    test, rows_exp, rows_act );

	    if ( t_row < 1  ||  t_row > row_count  ||  rowIDs[t_row] == 0 )
	        printf( "%s: !!! Expected no row @ %d\n", test, t_row );
	}

    	target += rows_act - 1;	/* At last returned */

        if ( target < 1  ||  target > row_count  ||  rowIDs[target] == 0 )
	    printf( "%s: !!! Expected no row @ %d\n", test, target );
    }

    if ( target < 1 )  
    	flags |= IIAPI_ROW_BEFORE;
    else  if ( target > row_count )  
    	flags |= IIAPI_ROW_AFTER;
    else
    {
	if ( target == 1 )  flags |= IIAPI_ROW_FIRST;
	if ( target == row_count )  flags |=  IIAPI_ROW_LAST;
    	if ( rowIDs[target] < 0 )  flags |= IIAPI_ROW_UPDATED;
	if ( rowIDs[target] == 0 )  flags |= IIAPI_ROW_DELETED;
    }

    if ( api_gqi( stmtHandle ) == IIAPI_ST_SUCCESS )
    {
	if ( verbose )  printf( "%s: Result count %d row %d status 0x%x %s\n", 
				test, resultRows, cursorPosition, cursorStatus,
				endOfData ? "EOD" : "" );

	if ( target != cursorPosition )
	    printf( "%s: !!! Target %d, row position %d\n", 
	    	    test, target, cursorPosition );

	if ( flags != cursorStatus )
	    printf( "%s: !!! Expected status 0x%x, actual status 0x%x\n",
		    test, flags, cursorStatus );

	if ( target > row_count  &&  ! scrollable )
	{
	    if ( ! endOfData )
	        printf( "%s: !!! Expected END-OF-DATA\n", test );
	}
	else
	{
	    if ( endOfData )
	    	printf( "%s: !!! Unexpected END-OF-DATA @ %d\n", test, target );
	}

	if ( rows_act != resultRows )
	    if ( resultRows >= 0 )
		printf( "%s: !!! Received %d rows, %d rows indicated\n",
			test, rows_act, resultRows );
	    else  
	    	printf( "%s: !!! Received %d rows, no row count received\n",
			test, rows_act );
    }

    return;
}
	
static IIAPI_STATUS
api_init( II_PTR *envHandle )
{
    IIAPI_INITPARM	initParm;

    initParm.in_timeout = -1;
    initParm.in_version = IIAPI_VERSION_6; 

    IIapi_initialize( &initParm );
    *envHandle = initParm.in_envHandle;

    return( initParm.in_status );
}

static IIAPI_STATUS
api_term( II_PTR *envHandle )
{
    IIAPI_RELENVPARM	relParm;
    IIAPI_TERMPARM	termParm;

    relParm.re_envHandle = *envHandle;

    IIapi_releaseEnv( &relParm );
    IIapi_terminate( &termParm );

    *envHandle = NULL;
    return( termParm.tm_status );
}

static IIAPI_STATUS
api_conn( char *dbname, II_PTR *envHandle, II_PTR *connHandle )
{
    IIAPI_CONNPARM    	connParm;
    IIAPI_WAITPARM      waitParm = { -1 };
    
    connParm.co_genParm.gp_callback = NULL;
    connParm.co_genParm.gp_closure = NULL;
    connParm.co_target =  dbname;
    connParm.co_type = IIAPI_CT_SQL;
    connParm.co_connHandle = *envHandle;
    connParm.co_tranHandle = NULL;
    connParm.co_username = NULL;
    connParm.co_password = NULL;
    connParm.co_timeout = -1;

    IIapi_connect( &connParm );
    
    while( connParm.co_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    *connHandle = connParm.co_connHandle;
    return( api_status( &connParm.co_genParm ) );
}


static IIAPI_STATUS
api_disconn( II_PTR *connHandle )
{
    IIAPI_DISCONNPARM	disconnParm;
    IIAPI_WAITPARM	waitParm = { -1 };
    
    disconnParm.dc_genParm.gp_callback = NULL;
    disconnParm.dc_genParm.gp_closure = NULL;
    disconnParm.dc_connHandle = *connHandle;
    
    IIapi_disconnect( &disconnParm );
    
    while( disconnParm.dc_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );
    
    *connHandle = NULL;
    return( api_status( &disconnParm.dc_genParm ) );
}

static IIAPI_STATUS
api_exec( II_PTR *connHandle, II_PTR *tranHandle, char *queryText )
{
    IIAPI_QUERYPARM	queryParm;
    IIAPI_WAITPARM	waitParm = { -1 };
    IIAPI_STATUS	status;

    queryParm.qy_genParm.gp_callback = NULL;
    queryParm.qy_genParm.gp_closure = NULL;
    queryParm.qy_connHandle = *connHandle;
    queryParm.qy_tranHandle = *tranHandle;
    queryParm.qy_stmtHandle = (II_PTR)NULL;
    queryParm.qy_queryType = IIAPI_QT_QUERY;
    queryParm.qy_queryText = (char *)queryText;
    queryParm.qy_parameters = FALSE;
    queryParm.qy_flags = 0;

    IIapi_query( &queryParm );
  
    while( queryParm.qy_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    *tranHandle = queryParm.qy_tranHandle;
    status = api_status( &queryParm.qy_genParm );

    if ( status == IIAPI_ST_SUCCESS )
    	status = api_gqi( &queryParm.qy_stmtHandle );
    else
	api_gqi( &queryParm.qy_stmtHandle );

    api_close( &queryParm.qy_stmtHandle );
    return( status );
}

static IIAPI_STATUS
api_cursor_op( II_PTR *connHandle, II_PTR *tranHandle, II_PTR *stmtHandle, 
	       IIAPI_QUERYTYPE queryType, char *queryText )
{
    IIAPI_QUERYPARM	queryParm;
    IIAPI_SETDESCRPARM	descParm;
    IIAPI_PUTPARMPARM	parmParm;
    IIAPI_DESCRIPTOR	desc[1];
    IIAPI_DATAVALUE	parms[1];
    IIAPI_WAITPARM	waitParm = { -1 };
    IIAPI_STATUS	status;

    queryParm.qy_genParm.gp_callback = NULL;
    queryParm.qy_genParm.gp_closure = NULL;
    queryParm.qy_connHandle = *connHandle;
    queryParm.qy_tranHandle = *tranHandle;
    queryParm.qy_stmtHandle = (II_PTR)NULL;
    queryParm.qy_queryType = queryType;
    queryParm.qy_queryText = (char *)queryText;
    queryParm.qy_parameters = TRUE;
    queryParm.qy_flags = 0;

    IIapi_query( &queryParm );
  
    while( queryParm.qy_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    *tranHandle = queryParm.qy_tranHandle;
    status = api_status( &queryParm.qy_genParm );

    if ( status == IIAPI_ST_SUCCESS )
    {
	descParm.sd_genParm.gp_callback = NULL;
	descParm.sd_genParm.gp_closure = NULL;
	descParm.sd_stmtHandle = queryParm.qy_stmtHandle;
	descParm.sd_descriptorCount = 1;
	descParm.sd_descriptor = desc;
	desc[0].ds_dataType = IIAPI_HNDL_TYPE;
	desc[0].ds_nullable = FALSE;
	desc[0].ds_length = sizeof( *stmtHandle );
	desc[0].ds_precision = 0;
	desc[0].ds_scale = 0;
	desc[0].ds_columnType = IIAPI_COL_SVCPARM;
	desc[0].ds_columnName = NULL;

	IIapi_setDescriptor( &descParm );

	while( descParm.sd_genParm.gp_completed == FALSE );
	    IIapi_wait( &waitParm );

	status = api_status( &descParm.sd_genParm );
    }

    if ( status == IIAPI_ST_SUCCESS )
    {
    	parmParm.pp_genParm.gp_callback = NULL;
	parmParm.pp_genParm.gp_closure = NULL;
	parmParm.pp_stmtHandle = queryParm.qy_stmtHandle;
	parmParm.pp_parmCount = 1;
	parmParm.pp_parmData = parms;
	parmParm.pp_moreSegments = FALSE;
	parms[0].dv_null = FALSE;
	parms[0].dv_length = sizeof( *stmtHandle );
	parms[0].dv_value = (II_PTR)stmtHandle;

	IIapi_putParms( &parmParm );

	while( parmParm.pp_genParm.gp_completed == FALSE );
	    IIapi_wait( &waitParm );

	status = api_status( &parmParm.pp_genParm );
    }

    if ( status == IIAPI_ST_SUCCESS )
    	status = api_gqi( &queryParm.qy_stmtHandle );
    else
	api_gqi( &queryParm.qy_stmtHandle );

    api_close( &queryParm.qy_stmtHandle );
    return( status );
}

static IIAPI_STATUS
api_open( II_PTR *connHandle, II_PTR *tranHandle, II_PTR *stmtHandle,
	  char *queryText, II_BOOL scrollable )
{
    IIAPI_QUERYPARM	queryParm;
    IIAPI_WAITPARM	waitParm = { -1 };
    IIAPI_STATUS	status;

    queryParm.qy_genParm.gp_callback = NULL;
    queryParm.qy_genParm.gp_closure = NULL;
    queryParm.qy_connHandle = *connHandle;
    queryParm.qy_tranHandle = *tranHandle;
    queryParm.qy_stmtHandle = (II_PTR)NULL;
    queryParm.qy_queryType = IIAPI_QT_OPEN;
    queryParm.qy_queryText = (char *)queryText;
    queryParm.qy_flags = scrollable ? IIAPI_QF_SCROLL : 0;
    queryParm.qy_parameters = FALSE;

    IIapi_query( &queryParm );
  
    while( queryParm.qy_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    *tranHandle = queryParm.qy_tranHandle;
    *stmtHandle = queryParm.qy_stmtHandle;
    status = api_status( &queryParm.qy_genParm );
    return( status );
}

static IIAPI_STATUS
api_scroll( II_PTR *stmtHandle, II_UINT2 orientation, II_INT4 offset )
{
    IIAPI_SCROLLPARM	scrollParm;
    IIAPI_WAITPARM	waitParm = { -1 };
    IIAPI_STATUS	status;

    scrollParm.sl_genParm.gp_callback = NULL;
    scrollParm.sl_genParm.gp_closure = NULL;
    scrollParm.sl_stmtHandle = *stmtHandle;
    scrollParm.sl_orientation = orientation;
    scrollParm.sl_offset = offset;

    IIapi_scroll( &scrollParm );

    while( scrollParm.sl_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    status = api_status( &scrollParm.sl_genParm );
    return( status );
}


static IIAPI_STATUS
api_pos( II_PTR *stmtHandle, II_UINT2 reference, II_INT4 offset, II_INT2 rows )
{
    IIAPI_POSPARM	posParm;
    IIAPI_WAITPARM	waitParm = { -1 };
    IIAPI_STATUS	status;

    posParm.po_genParm.gp_callback = NULL;
    posParm.po_genParm.gp_closure = NULL;
    posParm.po_stmtHandle = *stmtHandle;
    posParm.po_reference = reference;
    posParm.po_offset = offset;
    posParm.po_rowCount = rows;

    IIapi_position( &posParm );

    while( posParm.po_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    status = api_status( &posParm.po_genParm );
    return( status );
}


static II_INT2
api_get( char *test, II_PTR *stmtHandle, II_INT2 rows, II_INT4 target )
{
    IIAPI_GETCOLPARM	gcParm;
    IIAPI_WAITPARM	waitParm = { -1 };
    IIAPI_STATUS	status;
    int			i;

    gcParm.gc_genParm.gp_callback = NULL;
    gcParm.gc_genParm.gp_closure = NULL;
    gcParm.gc_stmtHandle = *stmtHandle;
    gcParm.gc_rowCount = rows ? rows : 1;
    gcParm.gc_columnCount = col_count;
    gcParm.gc_columnData = rowDV;

    IIapi_getColumns( &gcParm );

    while( gcParm.gc_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    status = api_status( &gcParm.gc_genParm );

    switch( status )
    {
    case IIAPI_ST_SUCCESS :
	if ( gcParm.gc_rowsReturned <= 0 )
	{
	    printf( "IIapi_getColumns: returned SUCCESS but %d rows!\n",
	    	    gcParm.gc_rowsReturned );
	    gcParm.gc_rowsReturned = 0;
	}
        break;

    case IIAPI_ST_NO_DATA :
	if ( gcParm.gc_rowsReturned != 0 )
	    printf( "IIapi_getColumns: returned %d rows with NO DATA\n",
	    	    gcParm.gc_rowsReturned );
	gcParm.gc_rowsReturned = 0;
        break;

    default : return( 0 );
    }

    if ( veryverbose )  printf( "%s: Requested %d rows, received %d rows\n", 
			    test, rows, gcParm.gc_rowsReturned );

    for( i = 0; i < gcParm.gc_rowsReturned; i++ )
	if ( (target + i) < 1  ||  (target + i) > row_count )
	    printf( "%s: !!! Unexpected row %d, received ID %d\n",
	    	    test, target + i, rowData[i].id );
	else  if ( rowData[i].id != rowIDs[target + i] )
	    printf( "%s: !!! Target row %d, Expected ID %d, received ID %d\n",
	    	    test, target + i, rowIDs[target + i], rowData[i].id );
	else  if ( veryverbose )
	    printf( "%s: Received row %d [%d]\n", 
	            test, target + i, rowData[i].id );

    return( gcParm.gc_rowsReturned );
}

static IIAPI_STATUS
api_gqi( II_PTR *stmtHandle )
{
    IIAPI_GETQINFOPARM	getQInfoParm;
    IIAPI_WAITPARM	waitParm = { -1 };
    IIAPI_STATUS	status;

    endOfData = FALSE;
    resultRows = -1;
    cursorType = 0;
    cursorStatus = 0;
    cursorPosition = -1;

    getQInfoParm.gq_genParm.gp_callback = NULL;
    getQInfoParm.gq_genParm.gp_closure = NULL;
    getQInfoParm.gq_stmtHandle = *stmtHandle;

    IIapi_getQueryInfo( &getQInfoParm );

    while( getQInfoParm.gq_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    status = api_status( &getQInfoParm.gq_genParm );
    
    if ( status < IIAPI_ST_FAILURE )
    {
	if ( getQInfoParm.gq_flags & IIAPI_GQF_END_OF_DATA )
	    endOfData = TRUE;

    	if ( getQInfoParm.gq_mask & IIAPI_GQ_ROW_COUNT )
	{
	    if ( getQInfoParm.gq_rowCount >= 0 )
		resultRows = getQInfoParm.gq_rowCount;
	    else
	    {
		printf( "!!! Negative row count received - forcing 0\n" );
		resultRows = 0;
	    }
	}

        if ( getQInfoParm.gq_mask & IIAPI_GQ_CURSOR )
	    cursorType = getQInfoParm.gq_cursorType;

        if ( getQInfoParm.gq_mask & IIAPI_GQ_ROW_STATUS )
	{
	    cursorStatus = getQInfoParm.gq_rowStatus;
	    cursorPosition = getQInfoParm.gq_rowPosition;
	}
    }

    return( status );
}

static IIAPI_STATUS
api_close( II_PTR *stmtHandle )
{
    IIAPI_CLOSEPARM	closeParm;
    IIAPI_WAITPARM	waitParm = { -1 };
    IIAPI_STATUS	status;

    closeParm.cl_genParm.gp_callback = NULL;
    closeParm.cl_genParm.gp_closure = NULL;
    closeParm.cl_stmtHandle = *stmtHandle;

    IIapi_close( &closeParm );

    while( closeParm.cl_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    status = api_status( &closeParm.cl_genParm );
    return( status );
}

static IIAPI_STATUS
api_commit( II_PTR *tranHandle )
{
    IIAPI_COMMITPARM	commitParm;
    IIAPI_WAITPARM	waitParm = { -1 };

    commitParm.cm_genParm.gp_callback = NULL;
    commitParm.cm_genParm.gp_closure = NULL;
    commitParm.cm_tranHandle = *tranHandle;
    *tranHandle = NULL;

    IIapi_commit( &commitParm );

    while( commitParm.cm_genParm.gp_completed == FALSE )
        IIapi_wait( &waitParm );

    return( api_status( &commitParm.cm_genParm ) );
}

static IIAPI_STATUS
api_rollback( II_PTR *tranHandle )
{
    IIAPI_ROLLBACKPARM	rollParm;
    IIAPI_WAITPARM	waitParm = { -1 };

    rollParm.rb_genParm.gp_callback = NULL;
    rollParm.rb_genParm.gp_closure = NULL;
    rollParm.rb_tranHandle = *tranHandle;
    rollParm.rb_savePointHandle = NULL;
    *tranHandle = NULL;

    IIapi_rollback( &rollParm );

    while( rollParm.rb_genParm.gp_completed == FALSE )
        IIapi_wait( &waitParm );

    return( api_status( &rollParm.rb_genParm ) );
}

static IIAPI_STATUS
api_status( IIAPI_GENPARM *genParm )
{
    switch( genParm->gp_status )
    {
    case IIAPI_ST_SUCCESS :
    case IIAPI_ST_MESSAGE :
    case IIAPI_ST_NO_DATA :
        break;

    case IIAPI_ST_WARNING :
	printf( "API: Warning\n" );
	break;

    case IIAPI_ST_ERROR :
	printf( "API: DBMS error\n" );
    	break;

    case IIAPI_ST_FAILURE :
	printf( "API: request failed\n" );
	break;

    case IIAPI_ST_NOT_INITIALIZED : 
    	printf( "API: no init\n" );
	break;

    case IIAPI_ST_OUT_OF_MEMORY : 
    	printf( "API: Mem alloc failure\n" );
	break;

    case IIAPI_ST_INVALID_HANDLE : 
    	printf( "API: Bad handle\n" );
	break;

    default :	
    	printf( "Unknown API status: %d\n", genParm->gp_status );
	break;
    }

    while( genParm->gp_errorHandle )
    {
    	IIAPI_GETEINFOPARM	getErr;

	getErr.ge_errorHandle = genParm->gp_errorHandle;
	IIapi_getErrorInfo( &getErr );
	if ( getErr.ge_status != IIAPI_ST_SUCCESS )  break;

    	switch( getErr.ge_type )
	{
	case IIAPI_GE_ERROR :
	    printf( "Error: '%s' 0x%x: %s\n", getErr.ge_SQLSTATE,
	    	    getErr.ge_errorCode, getErr.ge_message ? getErr.ge_message 
		    					   : "" );
	    break;

	case IIAPI_GE_WARNING :
	    printf( "Warning: '%s' 0x%x: %s\n", getErr.ge_SQLSTATE,
	    	    getErr.ge_errorCode, getErr.ge_message ? getErr.ge_message 
		    					   : "" );
	    break;

	case IIAPI_GE_MESSAGE :
	    printf( "User msg: 0x%x: %s\n", getErr.ge_errorCode, 
	    	    getErr.ge_message ? getErr.ge_message : "" );
	    break;

	case IIAPI_GE_XAERR :
	    printf( "XA error: %d\n", getErr.ge_errorCode );
	    break;

	default :
	    printf( "Unknown API error message type: %d\n", getErr.ge_type );
	    break;
    	}
    }

    return( (genParm->gp_status >= IIAPI_ST_NO_DATA) ? genParm->gp_status 
    						     : IIAPI_ST_SUCCESS );
}


