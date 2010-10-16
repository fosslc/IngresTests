/*
 * History:
 *
 * 	03-Nov-2008 (wanfr01) Cleaned up compiler warnings
 *
*/


# include <stdio.h>
# include <stdlib.h>
# include <string.h>
# include <iiapi.h>


/*
** put apixa_prepare.c program into common/aif/acm directory
**
** Add these lines to the common/aif/acm/Jamfile
** (after you jam, the executable will be in $II_SYSTEM/ingres/bin)

> IIBINEXE apixa_prepare : apixa_prepare.c ;
> IINEEDLIBS apixa_prepare : APILIB ADFLIB CUFLIB GCFLIB
>       COMPATLIB ;

**
*/
static IIAPI_STATUS
api_init( II_PTR *envHandle );
static IIAPI_STATUS
api_term( II_PTR *envHandle );
static IIAPI_STATUS
api_conn( char *dbname, II_PTR *envHandle, II_PTR *connHandle );
static IIAPI_STATUS
api_disconn( II_PTR *connHandle );
static IIAPI_STATUS
api_query( II_PTR *connHandle, II_PTR *tranHandle, char *queryText , int printerror);
static IIAPI_STATUS
xa_start( II_ULONG flags, II_PTR *connHandle,
          IIAPI_XA_TRAN_ID *xid, II_PTR *tranHandle );
static IIAPI_STATUS
xa_end( II_ULONG flags, II_PTR *connHandle,
        IIAPI_XA_TRAN_ID *xid, II_PTR *tranHandle );
static IIAPI_STATUS
xa_prepare( II_ULONG flags, II_PTR *connHandle, IIAPI_XA_TRAN_ID *xid );
static IIAPI_STATUS
xa_commit( II_ULONG flags, II_PTR *connHandle, IIAPI_XA_TRAN_ID *xid );
#ifdef xDEBUG
static IIAPI_STATUS
xa_rollback( II_ULONG flags, II_PTR *connHandle, IIAPI_XA_TRAN_ID *xid );
#endif
static IIAPI_STATUS
api_status( IIAPI_GENPARM *genParm );
static IIAPI_STATUS
api_commit( II_PTR *tranHandle );
static void
check_willing_commit(int xn_prepared);



static char queryText[] = "select xa_dis_tran_id from lgmo_xa_dis_tran_ids";
static char insertText[] = "insert into b118356_t1 values(2)";


int
main( int argc, char **argv )
{
    II_PTR		envHndl1 = NULL;
    II_PTR		connHndl1 = NULL;
    II_PTR		tranHndl1 = NULL;
    IIAPI_XA_TRAN_ID	xid1;

    if ( argc != 2 )
    {
	printf( "usage: apitst [vnode::]dbname[/server_class]\n" );
	exit( 0 );
    }

    xid1.xt_formatID = 1;
    xid1.xt_gtridLength = 4;
    xid1.xt_bqualLength = 4;
    xid1.xt_data[0] = 0x01; xid1.xt_data[1] = 0x02; 
    xid1.xt_data[2] = 0x03; xid1.xt_data[3] = 0x04;
    xid1.xt_data[4] = 0x04; xid1.xt_data[5] = 0x03; 
    xid1.xt_data[6] = 0x02; xid1.xt_data[7] = 0x01;

    api_init( &envHndl1 ); 
    printf("conn1 connect\n");
    api_conn( argv[1], &envHndl1, &connHndl1 );
    api_query( &connHndl1, &tranHndl1, "drop table b118356_t1" ,0);
    api_query( &connHndl1, &tranHndl1, "create table b118356_t1 (c1 integer)" ,1);
    api_commit(&tranHndl1);

    printf("conn1 start xid1\n");
    xa_start( 0, &connHndl1, &xid1, &tranHndl1 );
    printf("conn1 %s\n", insertText);
    api_query( &connHndl1, &tranHndl1, insertText ,1);
    printf("conn1 end xid1\n");
    xa_end( 0, &connHndl1, &xid1, &tranHndl1 );
    printf("conn1 end xid1 done\n");

    printf("conn1 prepare xid1\n");
    xa_prepare( 0, &connHndl1, &xid1 );
    printf("conn1 prepare xid1 done\n");
    api_disconn ( &connHndl1);

    check_willing_commit(1);

    api_conn( argv[1], &envHndl1, &connHndl1 );
    printf("conn1 commit xid1\n");
    xa_commit( 0, &connHndl1, &xid1 );
    printf("conn1 commit xid1 DONE\n");

    check_willing_commit(0);

/*
    printf("rollback xid1\n");
    xa_rollback( 0, &connHndl1, &xid1 );
    printf("rollback done\n");
*/

    printf("conn1 disconn\n");
    api_disconn( &connHndl1 );
    api_term(&envHndl1);

    return ( 0 );
}

static IIAPI_STATUS
api_init( II_PTR *envHandle )
{
    IIAPI_INITPARM	initParm;

    initParm.in_timeout = -1;
    initParm.in_version = IIAPI_VERSION_5; 

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
api_query( II_PTR *connHandle, II_PTR *tranHandle, char *queryText , int printerror)
{
    IIAPI_QUERYPARM	queryParm;
    IIAPI_GETQINFOPARM	getQInfoParm;
    IIAPI_CLOSEPARM	closeParm;
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

    IIapi_query( &queryParm );
  
    while( queryParm.qy_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    *tranHandle = queryParm.qy_tranHandle;
    status = api_status( &queryParm.qy_genParm );

    getQInfoParm.gq_genParm.gp_callback = NULL;
    getQInfoParm.gq_genParm.gp_closure = NULL;
    getQInfoParm.gq_stmtHandle = queryParm.qy_stmtHandle;

    IIapi_getQueryInfo( &getQInfoParm );

    while( getQInfoParm.gq_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    if ( status == IIAPI_ST_SUCCESS )
    	status = api_status( &getQInfoParm.gq_genParm );
    else
	api_status( &getQInfoParm.gq_genParm );

    closeParm.cl_genParm.gp_callback = NULL;
    closeParm.cl_genParm.gp_closure = NULL;
    closeParm.cl_stmtHandle = queryParm.qy_stmtHandle;

    IIapi_close( &closeParm );

    while( closeParm.cl_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    api_status( &closeParm.cl_genParm );
    return( status );
}


static IIAPI_STATUS
xa_start( II_ULONG flags, II_PTR *connHandle, 
	  IIAPI_XA_TRAN_ID *xid, II_PTR *tranHandle )
{
    IIAPI_XASTARTPARM	startParm;
    IIAPI_WAITPARM	waitParm = { -1 };

    startParm.xs_genParm.gp_callback = NULL;
    startParm.xs_genParm.gp_closure = NULL;
    startParm.xs_connHandle = *connHandle;
    startParm.xs_flags = flags;
    startParm.xs_tranID.ti_type = IIAPI_TI_XAXID;
    startParm.xs_tranID.ti_value.xaXID.xa_branchSeqnum = 0;
    startParm.xs_tranID.ti_value.xaXID.xa_branchFlag = 
					IIAPI_XA_BRANCH_FLAG_FIRST | 
					IIAPI_XA_BRANCH_FLAG_LAST | 
					IIAPI_XA_BRANCH_FLAG_2PC;

    memcpy( (void *)&startParm.xs_tranID.ti_value.xaXID.xa_tranID,
    	    (void *)xid, sizeof( *xid ) );

    IIapi_xaStart( &startParm );

    while( startParm.xs_genParm.gp_completed == FALSE )
        IIapi_wait( &waitParm );

    *tranHandle = startParm.xs_tranHandle;
    return( api_status( &startParm.xs_genParm ) );
}

static IIAPI_STATUS
xa_end( II_ULONG flags, II_PTR *connHandle, 
	IIAPI_XA_TRAN_ID *xid, II_PTR *tranHandle )
{
    IIAPI_XAENDPARM	endParm;
    IIAPI_WAITPARM	waitParm = { -1 };

    endParm.xe_genParm.gp_callback = NULL;
    endParm.xe_genParm.gp_closure = NULL;
    endParm.xe_connHandle = *connHandle;
    endParm.xe_flags = flags;
    endParm.xe_tranID.ti_type = IIAPI_TI_XAXID;
    endParm.xe_tranID.ti_value.xaXID.xa_branchSeqnum = 0;
    endParm.xe_tranID.ti_value.xaXID.xa_branchFlag = 
					IIAPI_XA_BRANCH_FLAG_FIRST | 
					IIAPI_XA_BRANCH_FLAG_LAST | 
					IIAPI_XA_BRANCH_FLAG_2PC;

    memcpy( (void *)&endParm.xe_tranID.ti_value.xaXID.xa_tranID,
    	    (void *)xid, sizeof( *xid ) );

    IIapi_xaEnd( &endParm );

    while( endParm.xe_genParm.gp_completed == FALSE )
        IIapi_wait( &waitParm );

    *tranHandle = NULL;
    return( api_status( &endParm.xe_genParm ) );
}

static IIAPI_STATUS
xa_prepare( II_ULONG flags, II_PTR *connHandle, IIAPI_XA_TRAN_ID *xid )
{
    IIAPI_XAPREPPARM	prepParm;
    IIAPI_WAITPARM	waitParm = { -1 };

    prepParm.xp_genParm.gp_callback = NULL;
    prepParm.xp_genParm.gp_closure = NULL;
    prepParm.xp_connHandle = *connHandle;
    prepParm.xp_flags = flags;
    prepParm.xp_tranID.ti_type = IIAPI_TI_XAXID;
    prepParm.xp_tranID.ti_value.xaXID.xa_branchSeqnum = 0;
    prepParm.xp_tranID.ti_value.xaXID.xa_branchFlag = 
					IIAPI_XA_BRANCH_FLAG_FIRST | 
					IIAPI_XA_BRANCH_FLAG_LAST | 
					IIAPI_XA_BRANCH_FLAG_2PC;

    memcpy( (void *)&prepParm.xp_tranID.ti_value.xaXID.xa_tranID,
    	    (void *)xid, sizeof( *xid ) );

    IIapi_xaPrepare( &prepParm );

    while( prepParm.xp_genParm.gp_completed == FALSE )
        IIapi_wait( &waitParm );

    return( api_status( &prepParm.xp_genParm ) );
}

static IIAPI_STATUS
xa_commit( II_ULONG flags, II_PTR *connHandle, IIAPI_XA_TRAN_ID *xid )
{
    IIAPI_XACOMMITPARM	commitParm;
    IIAPI_WAITPARM	waitParm = { -1 };

    commitParm.xc_genParm.gp_callback = NULL;
    commitParm.xc_genParm.gp_closure = NULL;
    commitParm.xc_connHandle = *connHandle;
    commitParm.xc_flags = flags;
    commitParm.xc_tranID.ti_type = IIAPI_TI_XAXID;
    commitParm.xc_tranID.ti_value.xaXID.xa_branchSeqnum = 0;
    commitParm.xc_tranID.ti_value.xaXID.xa_branchFlag = 
					IIAPI_XA_BRANCH_FLAG_FIRST | 
					IIAPI_XA_BRANCH_FLAG_LAST | 
					IIAPI_XA_BRANCH_FLAG_2PC;

    memcpy( (void *)&commitParm.xc_tranID.ti_value.xaXID.xa_tranID,
    	    (void *)xid, sizeof( *xid ) );

    IIapi_xaCommit( &commitParm );

    while( commitParm.xc_genParm.gp_completed == FALSE )
        IIapi_wait( &waitParm );

    return( api_status( &commitParm.xc_genParm ) );
}

#ifdef xDEBUG
static IIAPI_STATUS
xa_rollback( II_ULONG flags, II_PTR *connHandle, IIAPI_XA_TRAN_ID *xid )
{
    IIAPI_XAROLLPARM	rollParm;
    IIAPI_WAITPARM	waitParm = { -1 };

    rollParm.xr_genParm.gp_callback = NULL;
    rollParm.xr_genParm.gp_closure = NULL;
    rollParm.xr_connHandle = *connHandle;
    rollParm.xr_flags = flags;
    rollParm.xr_tranID.ti_type = IIAPI_TI_XAXID;
    rollParm.xr_tranID.ti_value.xaXID.xa_branchSeqnum = 0;
    rollParm.xr_tranID.ti_value.xaXID.xa_branchFlag = 
					IIAPI_XA_BRANCH_FLAG_FIRST | 
					IIAPI_XA_BRANCH_FLAG_LAST | 
					IIAPI_XA_BRANCH_FLAG_2PC;

    memcpy( (void *)&rollParm.xr_tranID.ti_value.xaXID.xa_tranID,
    	    (void *)xid, sizeof( *xid ) );

    IIapi_xaRollback( &rollParm );

    while( rollParm.xr_genParm.gp_completed == FALSE )
        IIapi_wait( &waitParm );

    return( api_status( &rollParm.xr_genParm ) );
}
#endif

static IIAPI_STATUS
api_status( IIAPI_GENPARM *genParm )
{
    switch( genParm->gp_status )
    {
    case IIAPI_ST_SUCCESS :
        break;

    case IIAPI_ST_MESSAGE :
    case IIAPI_ST_WARNING :
    	genParm->gp_status = IIAPI_ST_SUCCESS;
	break;

    case IIAPI_ST_NO_DATA :
    	break;

    case IIAPI_ST_ERROR :
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

static void
check_willing_commit(int xn_prepared)
{
    II_PTR		connHandle = (II_PTR)NULL;
    II_PTR		tranHandle = (II_PTR)NULL;
    II_PTR		stmtHandle = (II_PTR)NULL;
    II_PTR		evHandle = NULL;
    IIAPI_QUERYPARM	queryParm;
    IIAPI_GETDESCRPARM	getDescrParm;
    IIAPI_GETCOLPARM	getColParm;
    IIAPI_CLOSEPARM	closeParm;
    IIAPI_CANCELPARM	cancelParm;
    IIAPI_WAITPARM	waitParm = { -1 };
    IIAPI_DATAVALUE	DataBuffer[ 1 ];
    int			i;
    char		var1[256];
    int                 willing_commit = 0;

    api_init( &evHandle ); 
    api_conn("iidbdb" , &evHandle, &connHandle );
    printf( "check_willing_commit: %s\n" , queryText);

    queryParm.qy_genParm.gp_callback = NULL;
    queryParm.qy_genParm.gp_closure = NULL;
    queryParm.qy_connHandle = connHandle;
    queryParm.qy_queryType = IIAPI_QT_QUERY;
    queryParm.qy_queryText = queryText;
    queryParm.qy_parameters = FALSE;
    queryParm.qy_tranHandle = tranHandle;
    queryParm.qy_stmtHandle = NULL;

    IIapi_query( &queryParm );
  
    while( queryParm.qy_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    tranHandle = queryParm.qy_tranHandle;
    stmtHandle = queryParm.qy_stmtHandle;

    /*
    **  Get query result descriptors
    */
/*
    printf( "check_willing_commit: get descriptors\n" );
*/

    getDescrParm.gd_genParm.gp_callback = NULL;
    getDescrParm.gd_genParm.gp_closure = NULL;
    getDescrParm.gd_stmtHandle = stmtHandle;
    getDescrParm.gd_descriptorCount = 0;
    getDescrParm.gd_descriptor = NULL;

    IIapi_getDescriptor( &getDescrParm );
    
    while( getDescrParm.gd_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    /*
    **  Get query results
    */
/*
    printf( "check_willing_commit: get results\n" );
*/

    getColParm.gc_genParm.gp_callback = NULL;
    getColParm.gc_genParm.gp_closure = NULL;
    getColParm.gc_rowCount = 1;
    getColParm.gc_columnCount = 1;
    getColParm.gc_rowsReturned = 0;
    getColParm.gc_columnData =  DataBuffer;
    getColParm.gc_columnData[0].dv_value = var1; 
    getColParm.gc_stmtHandle = stmtHandle;
    getColParm.gc_moreSegments = 0;

    for( i = 0; i < 1; i++ )
    {
	IIapi_getColumns( &getColParm );
        
        while( getColParm.gc_genParm.gp_completed == FALSE )
	    IIapi_wait( &waitParm );

	if ( getColParm.gc_genParm.gp_status >= IIAPI_ST_NO_DATA )
	    break; 

	var1[ DataBuffer[0].dv_length ] = '\0';
	willing_commit = 1;
    };

    if (!willing_commit && xn_prepared)
        printf("---> ERROR!!!!  XA PREPARE did not make xn willing commit\n");

    /*
    **  Cancel query processing.
    */
/*
    printf( "check_willing_commit: cancel query\n" );
*/

    cancelParm.cn_genParm.gp_callback = NULL;
    cancelParm.cn_genParm.gp_closure = NULL;
    cancelParm.cn_stmtHandle = stmtHandle;

    IIapi_cancel(&cancelParm );

    while( cancelParm.cn_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm);

    /*
    **  Free query resources.
    */
/*
    printf( "check_willing_commit: free query resources\n" );
*/

    closeParm.cl_genParm.gp_callback = NULL;
    closeParm.cl_genParm.gp_closure = NULL;
    closeParm.cl_stmtHandle = stmtHandle;

    IIapi_close( &closeParm );

    while( closeParm.cl_genParm.gp_completed == FALSE )
	IIapi_wait( &waitParm );

    /* normal commit on this connection to iidbdb */
    api_commit(&tranHandle); 
/*
    printf("check_willing_commit: disconnect\n");
*/
    api_disconn( &connHandle );
/*
    printf("check_willing_commit: term\n");
*/
    api_term(&evHandle);

}
