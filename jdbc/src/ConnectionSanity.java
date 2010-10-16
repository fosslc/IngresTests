// Copyright (c) 2004 Ingres Corporation
//
// ****************************************************************************
// File: ConnectionSanity.java
//
// This Java file contains most of the behaviors from the JDBC Connection
// interface for creating a database communication connection.
//
// ****************************************************************************
// History: 	15-Dec-2003 (legru01) Created
//		24-Sep-2004 (legru01) Added the ORDER BY clause to all
//                          select statements. This change
//                          keeps the expected results static
//                          across all platforms.
//		02-Feb-2006 (boija02) Updated copyright for Ingres Corp
//		31-Mar-2008 (boija02) Suppressed unchecked warnings for messy
//			    generics method.
// ****************************************************************************

import java.lang.*;
import java.util.Vector;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Savepoint;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.Date;
import java.sql.SQLWarning;


public class ConnectionSanity {



    // -------------------- ESTABLISH A CONNECTION
    // ========================================================================
    // Creates an Ingres r3 JDBC connection object
    // ========================================================================
	public static Connection makeConnection
        (String connectionURL, String username, String password)
	    throws SQLException {
            Connection conn = null;
            try {
            Class.forName("ca.ingres.jdbc.IngresDriver").newInstance();
            System.out.println("Driver Loaded ...");
            conn = DriverManager.getConnection(connectionURL,username,password);
            System.out.println("Connection made ...");
            } catch (ClassNotFoundException e)  {
                System.out.println(e.getMessage());
            } catch (InstantiationException ie) {
                System.out.println(ie.getMessage());
            } catch (IllegalAccessException iae){
                System.out.println(iae.getMessage());
            } catch (SQLException sqlex)        {
                System.out.println(sqlex.getMessage());}
            return conn;
	}

    // -------------------- TRANSACTION ISOLATION
    // ========================================================================
    // Sets the transaction Isolation level
    // Attempts to change the transaction Isolation level for the
    // connection object
    // input: Connection conn
    //        TRANSACTION_NONE or
    //        TRANSACTION_COMMITED or
    //        TRANSACTION_READ_UNCOMMENTED or
    //        TRANSACTION_REPEATABLE_READ or
    //        TRANSACTION_SERIALIZABLE
    // return: void
    // ========================================================================
    public void setTransctionIsolation (Connection conn, int transLevel)
    throws SQLException {
        conn.setTransactionIsolation(transLevel);
    }

    // ========================================================================
    // Gets the transaction Insolation level
    // Retreives connection object's current transaction isolation level
    // input: NONE
    // return:TRANSACTION_NONE or
    //        TRANSACTION_COMMITED or
    //        TRANSACTION_READ_UNCOMMENTED or
    //        TRANSACTION_REPEATABLE_READ or
    //        TRANSACTION_SERIALIZABLE
    // ========================================================================
    public int getTransactionIsolation (Connection conn)
    throws SQLException {
            return conn.getTransactionIsolation();
    }

    // ========================================================================
    // Method that prints the TransAction Isolation level
    // of the Connection object
    // ========================================================================
    private static void printIsolationLevel (int transActionLevel)
    throws SQLException {

        switch  (transActionLevel) {
        case    Connection.TRANSACTION_NONE:
                System.out.println
                ("Transaction Level is set to TRANSACTION_NONE");
                break;
        case    Connection.TRANSACTION_READ_COMMITTED:
                System.out.println
                ("Transaction Level is set to TRANSACTION_READ_COMMITTED");
                break;
        case    Connection.TRANSACTION_READ_UNCOMMITTED:
                System.out.println
                ("Transaction Level is set to TRANSACTION_READ_UNCOMMITTED");
                break;
        case    Connection.TRANSACTION_REPEATABLE_READ:
                System.out.println
                ("Transaction Level is set to TRANSACTION_REPEATABLE_READ");
                break;
        case    Connection.TRANSACTION_SERIALIZABLE:
                System.out.println
                ("Transaction Level is set to TRANSACTION_SERIALIZABLE");
		break;
        default:
                System.out.println("INVALID OPTION");
	   } // EOF SWITCH

    }

    // ========================================================================
    // Method to test the setting of Transaction Isolation levels
    // ========================================================================
    private void testTransActionIsolation  (Connection conn)
    throws SQLException {
    System.out.println ("");
    System.out.println
    ("TEST TRANSACTION ISOLATION ===========================================");
    System.out.println ("");

    try {
        this.setTransctionIsolation(conn,Connection.TRANSACTION_NONE);
        this.printIsolationLevel (this.getTransactionIsolation (conn));
        } catch (SQLException sqlex) { System.out.println
        ("TRANSACTION_NONE: " + sqlex.getMessage());}
    try {
        this.setTransctionIsolation
        (conn,Connection.TRANSACTION_READ_COMMITTED);
        this.printIsolationLevel (this.getTransactionIsolation (conn));
        } catch (SQLException sqlex) { System.out.println
        ("TRANSACTION_READ_COMMITTED: " + sqlex.getMessage());}
    try {
        this.setTransctionIsolation
        (conn,Connection.TRANSACTION_READ_UNCOMMITTED);
	this.printIsolationLevel (this.getTransactionIsolation (conn));
	} catch (SQLException sqlex) { System.out.println
        ("TRANSACTION_READ_UNCOMMITTED: " + sqlex.getMessage());}
    try {
        this.setTransctionIsolation
        (conn,Connection.TRANSACTION_REPEATABLE_READ);
        this.printIsolationLevel (this.getTransactionIsolation (conn));
        } catch (SQLException sqlex) { System.out.println
        ("TRANSACTION_REPEATABLE_READ: " + sqlex.getMessage());}
    try {
        this.setTransctionIsolation(conn,Connection.TRANSACTION_SERIALIZABLE);
        this.printIsolationLevel (this.getTransactionIsolation (conn));
        } catch (SQLException sqlex) { System.out.println
        ("TRANSACTION_SERIALIZABLE: " + sqlex.getMessage());}
    }

    // -------------------- AUTOCOMMIT
    // ========================================================================
    // Sets the connection's auto commit mode
    // input: Connection conn
    //        Boolean autocommit_mode (true or false)
    // return: void
    // ========================================================================
    public void setAutoCommit (Connection conn, boolean autoCommit)
    throws SQLException {
        conn.setAutoCommit(autoCommit);
    }

    // =======================================================================
    // Gets the connection's auto commit mode
    // input: Connection conn
    // return: boolean value (true or false)
    // =======================================================================
    public boolean getAutoCommit (Connection conn)
     throws SQLException {
        return conn.getAutoCommit();
    }

    // =======================================================================
    // Method that prints the auto commit mode
    // of the Connection object
    // =======================================================================
     private static void printAutoCommit (boolean autocommit)
    throws SQLException {
        System.out.println( "Autocommit mode  is: " + autocommit);
    }

    // =======================================================================
    // Method to test the setting of auto commit mode
    // =======================================================================
    private void testAutoCommit  (Connection conn)
    throws SQLException {
    System.out.println ("");
    System.out.println
    ("TEST AUTOCOMMIT =====================================================");
    System.out.println ("");

    try {
        this.setAutoCommit (conn,true);
        this.printAutoCommit (this.getAutoCommit(conn));
        } catch (SQLException sqlex) { System.out.println
        ("Auto Commit set to true: " + sqlex.getMessage());}
    try {
        this.setAutoCommit (conn, false);
        this.printAutoCommit (this.getAutoCommit(conn));
        } catch (SQLException sqlex) { System.out.println
        ("Auto Commit set to false: " + sqlex.getMessage());}
    }

    // -------------------- READONLY
    // =======================================================================
    // Put the connection in read-only mode
    // input: Connection conn
    //        Boolean readOnly (true or false)
    // return: void
    // =======================================================================
    public void setReadOnly (Connection conn, boolean readOnly)
    throws SQLException {
        conn.setReadOnly(readOnly);
    }

    // =======================================================================
    // Retrives whether the connection's is in read-only mode
    // input: Connection conn
    // return: boolean value (true or false)
    // =======================================================================
    public boolean isReadOnly (Connection conn)
     throws SQLException {
        return conn.isReadOnly();
    }

    // =======================================================================
    // Method that prints the Read-only mode
    // of the Connection object
    // =======================================================================
    private static void printReadOnly (boolean readOnly)
    throws SQLException {
        System.out.println( "Read-only mode  is: " + readOnly);
    }

    // =======================================================================
    // Method to test the setting of read-only mode
    // =======================================================================
    private void testReadOnly  (Connection conn)
    throws SQLException {
    System.out.println ("");
    System.out.println
    ("TEST READ-ONLY ======================================================");
    System.out.println ("");

    try {
        this.setReadOnly (conn,true);
        this.printReadOnly (this.isReadOnly(conn));
        } catch (SQLException sqlex) { System.out.println
        ("Read-only set to true: " + sqlex.getMessage());}
    try {
        this.setReadOnly (conn, false);
        this.printReadOnly (this.isReadOnly(conn));
        } catch (SQLException sqlex) { System.out.println
        ("Read-only set to false: " + sqlex.getMessage());}
    }

    // -------------------- Holdability
    // =======================================================================
    // Change the holdability of ResultSet objects created using the
    // object
    // input: ResultSet.HOLD_CURSORS_OVER_COMMIT
    //        ResultSet.CLOSE_CURSORS_AT_COMMIT
    // return: void
    // =======================================================================
    public void setHoldability (Connection conn, int holdability)
    throws SQLException {
        conn.setHoldability (holdability);
    }

    // =======================================================================
    // Retreives the current holdability of ResultSet objects created by
    // the connection object
    // input: NONE
    // return:ResultSet.HOLD_CURSORS_OVER_COMMIT
    //        ResultSet.CLOSE_CURSORS_AT_COMMIT
    // =======================================================================
    public int getHoldability (Connection conn)
    throws SQLException {
            return conn.getHoldability ();
    }

    // =======================================================================
    // Method that prints the holdability
    // of the Connection object
    // =======================================================================
    private static void printHoldability (int holdability)
    throws SQLException {

        switch  (holdability) {
        case    ResultSet.HOLD_CURSORS_OVER_COMMIT:
                System.out.println
                ("ResultSet holdability is set to " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT");
                break;
        case    ResultSet.CLOSE_CURSORS_AT_COMMIT:
                System.out.println
                ("ResultSet holdability is set to " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT");
                break;
        default:
                System.out.println("INVALID OPTION");
	   } // EOF SWITCH

    }

    // =======================================================================
    // Method to test the setting of Transaction Isolation levels
    // =======================================================================
    private void testHoldability  (Connection conn)
    throws SQLException {
    System.out.println ("");
    System.out.println
    ("TEST HOLDABILITY ====================================================");
    System.out.println ("");

    try {
        this.setHoldability (conn, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        this.printHoldability (this.getHoldability (conn));
        } catch (SQLException sqlex) { System.out.println
        ("ResultSet.HOLD_CURSORS_OVER_COMMIT: " + sqlex.getMessage());}
   try {
        this.setHoldability (conn, ResultSet.CLOSE_CURSORS_AT_COMMIT);
        this.printHoldability (this.getHoldability (conn));
        } catch (SQLException sqlex) { System.out.println
        ("ResultSet.CLOSE_CURSORS_AT_COMMIT: " + sqlex.getMessage());}
    }
    // -------------------- METADATA
    // =======================================================================
    // Retrives a DatabaseMetaData object that contains metadata about
    // the database connection
    // input:  Connection conn
    // return: DatabaseMetaData
    // =======================================================================
    public DatabaseMetaData getMetaData (Connection conn)
    throws SQLException {
        return conn.getMetaData();
    }
    // =======================================================================
    // Method that prints the database Metadata
    // of the Connection object
    // =======================================================================
    private static void printMetaData (DatabaseMetaData metadata)
    throws SQLException {

           System.out.println(
           "supportsANSI92EntryLevelSQL: " +
           metadata.supportsANSI92EntryLevelSQL());

           System.out.println("supportsANSI92FullSQL: " +
           metadata.supportsANSI92FullSQL());

           System.out.println("supportsANSI92IntermediateSQL: " +
           metadata.supportsANSI92IntermediateSQL());

           System.out.println("supportsAlterTableWithAddColumn: " +
           metadata.supportsAlterTableWithAddColumn());

           System.out.println("supportsAlterTableWithDropColumn: " +
           metadata.supportsAlterTableWithDropColumn());

           System.out.println("supportsBatchUpdates: " +
           metadata.supportsBatchUpdates());

           System.out.println("supportsCatalogsInDataManipulation: " +
           metadata.supportsCatalogsInDataManipulation());

           System.out.println("supportsCatalogsInIndexDefinitions: " +
           metadata.supportsCatalogsInIndexDefinitions());

           System.out.println("supportsCatalogsInPrivilegeDefinitions: " +
           metadata.supportsCatalogsInPrivilegeDefinitions());

           System.out.println("supportsCatalogsInProcedureCalls: " +
           metadata.supportsCatalogsInProcedureCalls());

           System.out.println("supportsCatalogsInTableDefinitions: " +
           metadata.supportsCatalogsInTableDefinitions());

           System.out.println("supportsColumnAliasing: " +
           metadata.supportsColumnAliasing());

           System.out.println("supportsConvert: " +
           metadata.supportsConvert());

           System.out.println("supportsCoreSQLGrammar: " +
           metadata.supportsCoreSQLGrammar());

           System.out.println("supportsCorrelatedSubqueries: " +
           metadata.supportsCorrelatedSubqueries());

           System.out.println
           ("supportsDataDefinitionAndDataManipulationTransactions: "
           + metadata.supportsDataDefinitionAndDataManipulationTransactions());

           System.out.println("supportsDataManipulationTransactionsOnly: " +
           metadata.supportsDataManipulationTransactionsOnly());

           System.out.println("supportsDifferentTableCorrelationNames: " +
           metadata.supportsDifferentTableCorrelationNames());

           System.out.println("supportsExpressionsInOrderBy: " +
           metadata.supportsExpressionsInOrderBy());

           System.out.println("supportsExtendedSQLGrammar: " +
           metadata.supportsExtendedSQLGrammar());

           System.out.println("supportsFullOuterJoins: " +
           metadata.supportsFullOuterJoins());

           System.out.println("supportsGroupBy: " +
           metadata.supportsGroupBy());

           System.out.println("supportsGroupByBeyondSelect: " +
           metadata.supportsGroupByBeyondSelect());

           System.out.println("supportsGroupByUnrelated: " +
           metadata.supportsGroupByUnrelated());

           System.out.println("supportsGetGeneratedKeys: " +
           metadata.supportsGetGeneratedKeys());

           System.out.println("supportsIntegrityEnhancementFacility: " +
           metadata.supportsIntegrityEnhancementFacility());

           System.out.println("supportsLikeEscapeClause: " +
           metadata.supportsLikeEscapeClause());

           System.out.println("supportsLimitedOuterJoins: " +
           metadata.supportsLimitedOuterJoins());

           System.out.println("supportsMinimumSQLGrammar: " +
           metadata.supportsMinimumSQLGrammar());

           System.out.println("supportsMixedCaseIdentifiers: " +
           metadata.supportsMixedCaseIdentifiers());

           System.out.println("supportsMixedCaseQuotedIdentifiers: " +
           metadata.supportsMixedCaseQuotedIdentifiers());

           System.out.println("supportsMultipleOpenResults: " +
           metadata.supportsMultipleOpenResults());

           System.out.println("supportsMultipleResultSets: " +
           metadata.supportsMultipleResultSets());

           System.out.println("supportsMultipleTransactions: " +
           metadata.supportsMultipleTransactions());

           System.out.println("supportsNamedParameters: " +
           metadata.supportsNamedParameters());

           System.out.println("supportsNonNullableColumns: " +
           metadata.supportsNonNullableColumns());

           System.out.println("supportsOpenCursorsAcrossCommit: " +
           metadata.supportsOpenCursorsAcrossCommit());

           System.out.println("supportsOpenCursorsAcrossRollback: " +
           metadata.supportsOpenCursorsAcrossRollback());

           System.out.println("supportsOpenStatementsAcrossCommit: " +
           metadata.supportsOpenStatementsAcrossCommit());

           System.out.println("supportsOpenStatementsAcrossRollback: " +
           metadata.supportsOpenStatementsAcrossRollback());

           System.out.println("supportsOrderByUnrelated: " +
           metadata.supportsOrderByUnrelated());

           System.out.println("supportsOuterJoins: " +
           metadata.supportsOuterJoins());

           System.out.println("supportsPositionedDelete: " +
           metadata.supportsPositionedDelete());

           System.out.println("supportsPositionedUpdate: " +
           metadata.supportsPositionedUpdate());

           System.out.println
           ("supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY): " +
           metadata.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY));

           System.out.println
           ("supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE): " +
           metadata.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE));

           System.out.println
           ("supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE): " +
           metadata.supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE));

           System.out.println
           ("supportsResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT): "
           + metadata.supportsResultSetHoldability(
           ResultSet.HOLD_CURSORS_OVER_COMMIT));

           System.out.println
           ("supportsResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT): "
           + metadata.supportsResultSetHoldability(
           ResultSet.CLOSE_CURSORS_AT_COMMIT));

           System.out.println
           ("supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, " +
           "ResultSet.CONCUR_READ_ONLY): " +
           metadata.supportsResultSetConcurrency
           (ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY));

           System.out.println("supportsResultSetConcurrency( " +
           "ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE): " +
           metadata.supportsResultSetConcurrency
           (ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE));

           System.out.println("supportsResultSetConcurrency( " +
           "ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY): " +
           metadata.supportsResultSetConcurrency
           (ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY));

           System.out.println
           ("supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, " +
           "ResultSet.CONCUR_UPDATABLE): " +
           metadata.supportsResultSetConcurrency
           (ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE));

           System.out.println
           ("supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_SENSITIVE, " +
           "ResultSet.CONCUR_READ_ONLY): " +
           metadata.supportsResultSetConcurrency
           (ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY));

           System.out.println
           ("supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_SENSITIVE, " +
           "ResultSet.CONCUR_UPDATABLE): " +
           metadata.supportsResultSetConcurrency
           (ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE));

           System.out.println("supportsSavepoints: " +
           metadata.supportsSavepoints());

           System.out.println("supportsSchemasInDataManipulation: " +
           metadata.supportsSchemasInDataManipulation());

           System.out.println("supportsSchemasInIndexDefinitions: " +
           metadata.supportsSchemasInIndexDefinitions());

           System.out.println("supportsSchemasInPrivilegeDefinitions: " +
           metadata.supportsSchemasInPrivilegeDefinitions());

           System.out.println("supportsSchemasInProcedureCalls: " +
           metadata.supportsSchemasInProcedureCalls());

           System.out.println("supportsSchemasInTableDefinitions: " +
           metadata.supportsSchemasInTableDefinitions());

           System.out.println("supportsSelectForUpdate: " +
           metadata.supportsSelectForUpdate());

           System.out.println("supportsStatementPooling: " +
           metadata.supportsStatementPooling());

           System.out.println("supportsStoredProcedures: " +
           metadata.supportsStoredProcedures());

           System.out.println("supportsSubqueriesInComparisons: " +
           metadata.supportsSubqueriesInComparisons());

           System.out.println("supportsSubqueriesInExists: " +
           metadata.supportsSubqueriesInExists());

           System.out.println("supportsSubqueriesInIns: " +
           metadata.supportsSubqueriesInIns());

           System.out.println("supportsSubqueriesInQuantifieds: " +
           metadata.supportsSubqueriesInQuantifieds());

           System.out.println("supportsTableCorrelationNames: " +
           metadata.supportsTableCorrelationNames());

           System.out.println("supportsTransactions: " +
           metadata.supportsTransactions());

           System.out.println
         ("supportsTransactionIsolationLevel( Connection.TRANSACTION_NONE): " +
           metadata.supportsTransactionIsolationLevel
           (Connection.TRANSACTION_NONE));

           System.out.println
           ("supportsTransactionIsolationLevel( " +
           "Connection.TRANSACTION_READ_UNCOMMITTED): " +
           metadata.supportsTransactionIsolationLevel
           (Connection.TRANSACTION_READ_UNCOMMITTED));

           System.out.println("supportsTransactionIsolationLevel( " +
           "Connection.TRANSACTION_READ_COMMITTED): " +
           metadata.supportsTransactionIsolationLevel
           (Connection.TRANSACTION_READ_COMMITTED));

           System.out.println
           ("supportsTransactionIsolationLevel( " +
           "Connection.TRANSACTION_REPEATABLE_READ): " +
           metadata.supportsTransactionIsolationLevel
           (Connection.TRANSACTION_REPEATABLE_READ));

           System.out.println
           ("supportsTransactionIsolationLevel( " +
           "Connection.TRANSACTION_SERIALIZABLE): " +
           metadata.supportsTransactionIsolationLevel
           (Connection.TRANSACTION_SERIALIZABLE));

           System.out.println("supportsUnion: " +
           metadata.supportsUnion());

           System.out.println("supportsUnionAll: " +
           metadata.supportsUnionAll());

    }

    // =======================================================================
    // Method to test a database Metadata object
    // =======================================================================
    private void testMetaData  (Connection conn)
    throws SQLException {
    System.out.println ("");
    System.out.println
    ("TEST METADATA =======================================================");
    System.out.println ("");
    try {
          this.printMetaData (this.getMetaData (conn));
        } catch (SQLException sqlex) { System.out.println
        ("Database Metadata: " + sqlex.getMessage());}

    } // END OF METADATA

    // -------------------- CREATE STATEMENT
    //  ======================================================================
    //  CreateStatement a statment object for sending SQL statements
    //  to the database
    //  input:  Connection conn
    //  return: Statement
    //  ======================================================================
    public Statement createStatement (Connection conn)
    throws SQLException { return conn.createStatement();}

    //  ======================================================================
    //  CreateStatement a statment object that generate  ResultSet
    //  objects with the given type and concurrency
    //  input:  Connection conn
    //          int rsType
    //          int rsConncurr
    //  return: Statement
    //  ======================================================================
    public Statement createStatement (Connection conn, int rsType,
    int rsConcurr)
    throws SQLException { return conn.createStatement(rsType,rsConcurr); }

    //  ======================================================================
    //  CreateStatement a statment object that generate  ResultSet
    //  objects with the given type and concurrency, and holdability
    //  input:  Connection conn
    //          int rsType
    //          int rsConncurr
    //          int rsHoldability
    //  return: Statement
    //  ======================================================================
    public Statement createStatement
    (Connection conn, int rsType, int rsConcurr, int rsHoldability)
    throws SQLException {
        return conn.createStatement(rsType,rsConcurr,rsHoldability);
    }

    // =======================================================================
    // Method to test a Statement object
    // =======================================================================
    private void testCreateStatement  (Connection conn)
    throws SQLException {
    System.out.println ("");
    System.out.println
    ("TEST CREATE STATEMENT ===============================================");
    System.out.println ("");

    Statement stmt  = null;
    ResultSet dbRS  = null;
    SQLWarning sqlw = null;

    try {

          stmt = this.createStatement(conn);
          if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
             	System.out.println("createStatement(): PASS " );
             }
             else {
             	System.out.println("createStatement(): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }

          }
    }catch (SQLException sqlex) { System.out.println ("createStatement(): "
    + sqlex.getMessage());}

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn,ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_READ_ONLY);
         if (conn == stmt.getConnection()) {

		sqlw = conn.getWarnings();
           	if  (sqlw == null) {
                    System.out.println
                    ("createStatement(ResultSet.TYPE_FORWARD_ONLY, " +
                    "ResultSet.CONCUR_READ_ONLY): PASS");
                }
                else {
                    System.out.println
                    ("createStatement(ResultSet.TYPE_FORWARD_ONLY," +
                    "ResultSet.CONCUR_READ_ONLY): " + sqlw.getMessage());
                    sqlw = null;
                    conn.clearWarnings();
                }
       }
   }catch (SQLException sqlex) {
         System.out.println ("createStatement(ResultSet.TYPE_FORWARD_ONLY, " +
         "ResultSet.CONCUR_READ_ONLY): " + sqlex.getMessage());}

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn,ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_UPDATABLE);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                 System.out.println ("createStatement " +
                 "(ResultSet.TYPE_FORWARD_ONLY, " +
                 "ResultSet.CONCUR_UPDATABLE): PASS");
             }
             else {
                 System.out.println ("createStatement " +
                 "(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE): "
                 + sqlw.getMessage());
                 sqlw = null;
                 conn.clearWarnings();
             }
         }

    }catch (SQLException sqlex) {
         System.out.println ("createStatement " +
         "(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE): "
         + sqlex.getMessage());}

    // -----------------------------------------------------------------------

   try {
         stmt = this.createStatement(conn,ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_READ_ONLY);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                 System.out.println("createStatement " +
                 "(ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                 "ResultSet.CONCUR_READ_ONLY): PASS");
             }
             else {
                System.out.println("createStatement " +
                "(ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
        }

    }catch (SQLException sqlex) {
          System.out.println("createStatement " +
          "(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR__READ_ONLY): "
          + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn,ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_UPDATABLE);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): PASS");
             }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
        }
    }catch (SQLException sqlex) {
            System.out.println("createStatement( " +
            "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
            "ResultSet.CONCUR_UPDATABLE): "
            + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn,ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_READ_ONLY);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): PASS");
             }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): " + sqlw.getMessage());
                 sqlw = null;
                 conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println
         ("createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_READ_ONLY): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {

         stmt = this.createStatement(conn,
         ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): PASS");
             }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): " + sqlw.getMessage());
                 sqlw = null;
                 conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println
         ("createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_UPDATABLE): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn,ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
            }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("createStatement( ResultSet.TYPE_FORWARD_ONLY, " +
         "ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn,
         ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE,
         ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
             }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_FORWARD_ONLY, " +
         "ResultSet.CONCUR_UPDATABLE, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn, ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
            }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_FORWARD_ONLY, " +
         "ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn, ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
             }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
   }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn,ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);

         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
            }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
   }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
         "ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn,ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
             }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
   }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
         "ResultSet.CONCUR_UPDATABLE, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn, ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
            }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
   }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
         "ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn,ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
             }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
         "ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
            }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
             }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
            }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         stmt = this.createStatement(conn, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == stmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
             }
             else {
                System.out.println("createStatement( " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
   }catch (SQLException sqlex) {
         System.out.println("createStatement( " +
         "ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_UPDATABLE, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): "
         + sqlex.getMessage());
       }
    } // END OF CREATE STATEMENT

    // -------------------- PREPARE CALL
    //  ======================================================================
    //  Creates a CallableStatement object for sending a SQL statement
    //  to the database
    //  input:  Connection conn
    //          String sql
    //  return: CallableStatement
    //  ======================================================================
    public CallableStatement prepareCall (Connection conn, String sql)
    throws SQLException { return conn.prepareCall(sql);}

    //  ======================================================================
    //  Creates a  CallableStatement object for generating ResultSet
    //  objects with the given type and concurrency
    //  input:  Connection conn
    //          String sql
    //          int rsType
    //          int rsConcurr
    //  return: CallableStatement
    //  ======================================================================
    public CallableStatement prepareCall (Connection conn, String sql,
    int rsType, int rsConcurr)
    throws SQLException { return conn.prepareCall(sql,rsType,rsConcurr); }

    //  ======================================================================
    //  Creates a  CallableStatement object for generating ResultSet
    //  objects with the given type and concurrency, and holdability
    //  input:  Connection conn
    //          String sql
    //          int rsType
    //          int rsConcurr
    //          int rsHoldability
    //  return: CallableStatement
    //  ======================================================================
    public CallableStatement prepareCall (Connection conn, String sql,
    int rsType, int rsConcurr, int rsHoldability)
    throws SQLException {
        return conn.prepareCall (sql,rsType,rsConcurr,rsHoldability);
    }

    // =======================================================================
    // Method to test a CallableStatement object
    // =======================================================================
    private void testPrepareCall  (Connection conn)
    throws SQLException {
    System.out.println ("");
    System.out.println
    ("TEST PREPARE CALL ===================================================");
    System.out.println ("");

    CallableStatement cstmt = null;
    String sql              = null;
    Statement stmt          = null;
    ResultSet dbRS          = null;
    SQLWarning sqlw         = null;

    try {
        stmt = this.createStatement(conn);
        stmt.executeUpdate("DROP TABLE t_random");
    } catch (SQLException sqlex) { /* DO NOTHING */ }

    // -----------------------------------------------------------------------

    try {
        stmt.executeUpdate("DROP PROCEDURE insert_values");
    } catch (SQLException sqlex) { /* DO NOTHING */ }

    // -----------------------------------------------------------------------

    try {
        stmt.executeUpdate("CREATE TABLE t_random (random_number integer)");
    } catch (SQLException sqlex) { System.out.println (sqlex.getMessage()); }

    // -----------------------------------------------------------------------

    sql =   "create procedure insert_values as " +
            "declare " +
                "i integer;" +
            "begin " +
                "i = 0; " +
                "while ( i < 10000 ) do " +
                    "insert into t_random values (random()); " +
                    "i = i + 1; " +
                "endwhile; " +
               "commit; " +
            "end ";

    // -----------------------------------------------------------------------

    try {
        stmt.execute(sql);
    } catch (SQLException sqlex) {System.out.println (sqlex.getMessage());}

    sql = "{call insert_values }";

    try {

        cstmt = this.prepareCall(conn,sql );
        if (conn == cstmt.getConnection()) {

            sqlw = conn.getWarnings();
            if (sqlw == null) {
             	System.out.println("prepareCall(String sql): PASS " );
             }
             else {
             	System.out.println("prepareCall(String sql): " +
                sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }

          }
    }catch (SQLException sqlex) { System.out.println ("prepareCall( " +
        "String sql): " + sqlex.getMessage());
    }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_READ_ONLY);
         if (conn == cstmt.getConnection()) {

		sqlw = conn.getWarnings();
           	if  (sqlw == null) {
                    System.out.println ("prepareCall( " +
                    "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
                    "ResultSet.CONCUR_READ_ONLY): PASS");
                }
                else {
                    System.out.println ("prepareCall( " +
                    "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
                    "ResultSet.CONCUR_READ_ONLY): " + sqlw.getMessage());
                    sqlw = null;
                    conn.clearWarnings();
             }

       }
    }catch (SQLException sqlex) {
         System.out.println ("prepareCall( " +
         "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
         "ResultSet.CONCUR_READ_ONLY): " + sqlex.getMessage());
    }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_UPDATABLE);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println ("prepareCall( " +
                "string sql, ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_UPDATABLE): PASS");
             }
             else {
                 System.out.println ("prepareCall( " +
                 "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
                 "ResultSet.CONCUR_UPDATABLE): " + sqlw.getMessage());
                 sqlw = null;
                 conn.clearWarnings();
             }
         }

    }catch (SQLException sqlex) {
         System.out.println ("prepareCall( " +
         "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
         "ResultSet.CONCUR_UPDATABLE): " + sqlex.getMessage());
    }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql,
         ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): PASS");
             }
             else {
                System.out.println("prepareCall(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
        }

    }catch (SQLException sqlex) {
          System.out.println("prepareCall( " +
          "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
          "ResultSet.CONCUR__READ_ONLY): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_UPDATABLE);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): PASS");
             }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
        }
    }catch (SQLException sqlex) {
            System.out.println("prepareCall( " +
            "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
            "ResultSet.CONCUR_UPDATABLE): "
            + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_READ_ONLY);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): PASS");
             }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
            System.out.println ( "prepareCall( " +
            "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
            "ResultSet.CONCUR_READ_ONLY): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {

         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_UPDATABLE);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): PASS");
             }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println ("prepareCall( " +
         "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_UPDATABLE): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall(String sql, " +
                "ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
            }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
   	}catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
         "ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------


    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall(String sql,  " +
                "ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
             }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
         "ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
            }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
   }catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
         "ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
             }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
   }catch (SQLException sqlex) {
            System.out.println("prepareCall( " +
            "String sql, ResultSet.TYPE_FORWARD_ONLY, " +
            "ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT): "
            + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
            }
             else {
                System.out.println("prepareCall(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
         "ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlex.getMessage());
       }


    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
             }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
         "ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
            }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
         "ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
             }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_SCROLL_INSENSITIVE, " +
         "ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
            }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
   	}catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

   // -----------------------------------------------------------------------

   try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
             }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
   }catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
            }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT): " +
         sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         cstmt = this.prepareCall(conn, sql, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == cstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE,  " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
             }
             else {
                System.out.println("prepareCall( " +
                "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
   }catch (SQLException sqlex) {
         System.out.println("prepareCall( " +
         "String sql, ResultSet.TYPE_SCROLL_SENSITIVE, " +
         "ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT): "
         + sqlex.getMessage());
       }
    } // END OF PREPARE CALL

    // -------------------- PREPARED STATEMENT
    //  ======================================================================
    //  Creates a PreparedStatement object for sending parameterized SQL
    //  statements to the database
    //  input:  Connection conn
    //          String sql
    //  return: PreparedStatement
    //  ======================================================================
    public PreparedStatement prepareStatement (Connection conn, String sql)
    throws SQLException { return conn.prepareStatement (conn.nativeSQL(sql));}

    //  ======================================================================
    //  Creates a PreparedStatement object that will generating ResultSet
    //  objects with the given type and concurrency
    //  input:  Connection conn
    //          String sql
    //          int rsType
    //          int rsConcurr
    //  return: PreparedStatement
    //  ======================================================================
    public PreparedStatement prepareStatement (Connection conn, String sql,
    int rsType, int rsConcurr)
    throws SQLException { return conn.prepareStatement( conn.nativeSQL(sql),
    rsType,rsConcurr); }

    //  ======================================================================
    //  Creates a PreparedStatement object for generating ResultSet
    //  objects with the given type and concurrency, and holdability
    //  input:  Connection conn
    //          String sql
    //          int rsType
    //          int rsConcurr
    //          int rsHoldability
    //  return: PreparedStatement
    //  ======================================================================
    public PreparedStatement prepareStatement (Connection conn, String sql,
    int rsType, int rsConcurr, int rsHoldability)
    throws SQLException {
        return conn.prepareStatement (conn.nativeSQL(sql),rsType,rsConcurr,
        rsHoldability);
    }
    //  ======================================================================
    //  Creates a default PreparedStatement object capable of returning
    //  the autogenerated keys
    //  input:  Connection conn
    //          String sql
    //          int autoGenerateKeys
    //  return: PreparedStatement
    //  ======================================================================
    public PreparedStatement prepareStatement (Connection conn, String sql,
    int autoGeneratedKeys)
    throws SQLException { return conn.prepareStatement (conn.nativeSQL(sql),
    autoGeneratedKeys);}

    //  ======================================================================
    //  Creates a default PreparedStatement object capable of returning
    //  the autogenerated keys designated by the given array
    //  input:  Connection conn
    //          String sql
    //          int [] columnIndexes
    //  return: PreparedStatement
    //  ======================================================================
    public PreparedStatement prepareStatement (Connection conn, String sql,
    int[] columnIndexes )
    throws SQLException { return conn.prepareStatement (conn.nativeSQL(sql),
    columnIndexes);}

    //  ======================================================================
    //  Creates a default PreparedStatement object capable of returning
    //  the autogenerated keys designated by the given array
    //  input:  Connection conn
    //          String sql
    //          String [] columnNames
    //  return: PreparedStatement
    //  ======================================================================
    public PreparedStatement prepareStatement (Connection conn, String sql,
    String[] columnNames )
    throws SQLException { return conn.prepareStatement (conn.nativeSQL(sql),
    columnNames);}

    // =======================================================================
    // Method to test a PreparedStatement object
    // =======================================================================
    public void testPrepareStatement  (Connection conn)
    throws SQLException {
    System.out.println ("");
    System.out.println
    ("TEST PREPARED STATEMENT =============================================");
    System.out.println ("");

    PreparedStatement pstmt = null;
    String sql              = null;
    Statement stmt          = null;
    ResultSet dbRS          = null;
    SQLWarning sqlw         = null;

    try {
        stmt = this.createStatement(conn);
        stmt.executeUpdate("DROP TABLE t_random");
    } catch (SQLException sqlex) { /* DO NOTHING */ }

    // -----------------------------------------------------------------------

    try {
        stmt.executeUpdate("CREATE TABLE t_random (random_number integer)");
    } catch (SQLException sqlex) { System.out.println (sqlex.getMessage()); }
       sql = "INSERT INTO t_random values (?)";

    // -----------------------------------------------------------------------

    try {

        pstmt = this.prepareStatement(conn,sql );
        if (conn == pstmt.getConnection()) {

            sqlw = conn.getWarnings();
            if (sqlw == null) {
                System.out.println("prepareStatement(String sql): PASS " );
            }
            else {
             	System.out.println("prepareStatement(String sql): " +
                sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
          }
    }catch (SQLException sqlex) { System.out.println
    ("prepareStatement(String sql): " + sqlex.getMessage());
    }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement
         (conn, sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
         if (conn == pstmt.getConnection()) {

		sqlw = conn.getWarnings();
           	if  (sqlw == null) {
                    System.out.println
                    ("prepareStatement(String sql, " +
                    "ResultSet.TYPE_FORWARD_ONLY, " +
                    "ResultSet.CONCUR_READ_ONLY): PASS");
                }
                else {
                    System.out.println ("prepareStatement(String sql, " +
                    "ResultSet.TYPE_FORWARD_ONLY, " +
                    "ResultSet.CONCUR_READ_ONLY): " + sqlw.getMessage());
                    sqlw = null;
                    conn.clearWarnings();
             }

       }
    }catch (SQLException sqlex) {
         System.out.println ("prepareStatement(String sql, " +
         "ResultSet.TYPE_FORWARD_ONLY, " +
         "ResultSet.CONCUR_READ_ONLY): " + sqlex.getMessage());
    }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
             System.out.println ("prepareStatement(string sql, " +
             "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE): PASS");
             }
             else {
                 System.out.println ("prepareStatement(String sql, " +
                 "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE): " +
                 sqlw.getMessage());
                 sqlw = null;
                 conn.clearWarnings();
             }
         }

    }catch (SQLException sqlex) {
         System.out.println ("prepareStatement(String sql, " +
         "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE): " +
         sqlex.getMessage());
    }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): PASS");
             }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
        }

    }catch (SQLException sqlex) {
          System.out.println("prepareStatement(String sql, " +
          "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
          "ResultSet.CONCUR__READ_ONLY): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {

         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): PASS");
             }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
        }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE): "
         + sqlex.getMessage());
       }
    // -----------------------------------------------------------------------
    try {
         pstmt = this.prepareStatement
         (conn, sql, ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_READ_ONLY);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): PASS");
             }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println ("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY): " +
         sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {

         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE): PASS");
             }
             else {
                System.out.println("prepareStatement(String sql, " +
              "ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE): "
                + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println
         ("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY,
         ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
            }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE,
         ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
             }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_FORWARD_ONLY,
         ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
            }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn,sql,
         ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE,
         ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
             }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
            }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
             }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }
    // -----------------------------------------------------------------------
    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
            }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_INSENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
             }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_INSENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
            }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_SENSITIVE,
         ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE,  " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): PASS");
             }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.HOLD_CURSORS_OVER_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, " +
         "ResultSet.HOLD_CURSORS_OVER_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY,
         ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
            }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_READ_ONLY, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
         }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {
         pstmt = this.prepareStatement(conn, sql,
         ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE,
         ResultSet.CLOSE_CURSORS_AT_COMMIT);
         if (conn == pstmt.getConnection()) {

             sqlw = conn.getWarnings();
             if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): PASS");
             }
             else {
                System.out.println("prepareStatement(String sql, " +
                "ResultSet.TYPE_SCROLL_SENSITIVE, " +
                "ResultSet.CONCUR_UPDATABLE, " +
                "ResultSet.CLOSE_CURSORS_AT_COMMIT): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
       }
    }catch (SQLException sqlex) {
         System.out.println("prepareStatement(String sql, " +
         "ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, " +
         "ResultSet.CLOSE_CURSORS_AT_COMMIT): "
         + sqlex.getMessage());
       }

    // -----------------------------------------------------------------------

    try {

        pstmt = this.prepareStatement(conn, sql,
        Statement.RETURN_GENERATED_KEYS);
        if (conn == pstmt.getConnection()) {

            sqlw = conn.getWarnings();
            if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "Statement.RETURN_GENERATED_KEYS): PASS " );
            }
            else {
             	System.out.println("prepareStatement(String sql, " +
                "Statement.RETURN_GENERATED_KEYS): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
          }
    }catch (SQLException sqlex) {
        System.out.println("prepareStatement(String sql, " +
        "Statement.RETURN_GENERATED_KEYS): " + sqlex.getMessage());
    }

    // -----------------------------------------------------------------------

    try {

        pstmt = this.prepareStatement(conn, sql, Statement.NO_GENERATED_KEYS);
        if (conn == pstmt.getConnection()) {

            sqlw = conn.getWarnings();
            if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "Statement.NO_GENERATED_KEYS): PASS " );
            }
            else {
             	System.out.println("prepareStatement(String sql, " +
                "Statement.NO_GENERATED_KEYS): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
          }
    }catch (SQLException sqlex) {
        System.out.println("prepareStatement(String sql, " +
        "Statement.NO_GENERATED_KEYS): " + sqlex.getMessage());
    }

    // -----------------------------------------------------------------------

    try {

        int [] columnIndex = new int[1];
        columnIndex [0] = 1;

        pstmt = this.prepareStatement(conn, sql, columnIndex);
        if (conn == pstmt.getConnection()) {

            sqlw = conn.getWarnings();
            if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "int[] columnIndex): PASS " );
            }
            else {
             	System.out.println("prepareStatement(String sql, " +
                "int[] columnIndex): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
          }
    }catch (SQLException sqlex) {
        System.out.println("prepareStatement(String sql, " +
        "int[] columnIndex): " + sqlex.getMessage());
    }

    // -----------------------------------------------------------------------

    try {

        String [] columnNames = new String[1];
        columnNames [0] = new String("random_number");

        pstmt = this.prepareStatement(conn, sql, columnNames);
        if (conn == pstmt.getConnection()) {

            sqlw = conn.getWarnings();
            if (sqlw == null) {
                System.out.println("prepareStatement(String sql, " +
                "String[] columnNames): PASS " );
            }
            else {
             	System.out.println("prepareStatement(String sql, " +
                "String[] columnNames): " + sqlw.getMessage());
                sqlw = null;
                conn.clearWarnings();
             }
          }
    }catch (SQLException sqlex) {
        System.out.println("prepareStatement(String sql, " +
        "String[] columnNames): " + sqlex.getMessage());
        }
    } // END OF PREPARED STATEMENT

    //  -------------------- SAVE POINT
    //  ======================================================================
    //  Creates an unnamed savepoint in the current transaction and returns
    //  the new Savepoint object that represent it
    //  input:  Connection conn
    //  return: Savepoint
    //  ======================================================================
    public Savepoint setSavepoint (Connection conn)
    throws SQLException { return conn.setSavepoint();}

    //  ======================================================================
    //  Creates a savepoint witht the given name in the current
    //  transaction and returns the new Savepoint object that represent it
    //  input:  Connection conn
    //          String name
    //  return: Savepoint
    //  ======================================================================
    public Savepoint setSavepoint (Connection conn, String name)
    throws SQLException { return conn.setSavepoint (name);}

    //  ======================================================================
    //  Removes the given Savepoint object from the current tranactions
    //  input:  Connection conn
    //          Savepoint savepoint
    //  return: void
    //  ======================================================================
    public void releaseSavepoint (Connection conn, Savepoint savepoint)
    throws SQLException { conn.releaseSavepoint (savepoint);}


    //  ======================================================================
    //  Undoes all changes made made in the current transaction and releases
    //  any database locks currently held by this Connection object
    //  input:  Connection conn
    //  return: void
    //  ======================================================================
    public void rollback (Connection conn)
    throws SQLException { conn.rollback ();}

    //  ======================================================================
    //  Undoes all changes made after the given Savepoint object was set
    //  input:  Connection conn
    //          Savepoint savepoint
    //  return: void
    //  ======================================================================
    public void rollback (Connection conn, Savepoint savepoint)
    throws SQLException { conn.rollback (savepoint);}

    // =======================================================================
    // Method to test a Savepoint object
    // =======================================================================
    private void testSavepoint  (Connection conn)
    throws SQLException {

    System.out.println ("");
    System.out.println
    ("TEST SAVEPOINT AND ROLLBACK =========================================");
    System.out.println ("");

    Savepoint savepoint     = null;
    ResultSet resultSet     = null;
    String sql              = null;
    Statement stmt          = null;
    SQLWarning sqlw         = null;


    // DATABASE TABLE DATA
    String sql_1 =
        "INSERT INTO BUILDINGS VALUES (10,'NEW YORK, NEW YORK')";
    String sql_2 =
        "INSERT INTO BUILDINGS VALUES (22,'CHICAGO, ILLINOIS')";
    String sql_3 =
        "INSERT INTO BUILDINGS VALUES (31,'LOS ANGELES, CALIFORNIA')";
    String sql_4 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('000-11-2222','PETER','PETERSON','SALES MANAGER',22)";
    String sql_5 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('111-22-3333','CARL','CARLSON','PROGRAMMER',10)";
    String sql_6 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('222-33-4444','WILLIAM','WILLIAMSON','PROGRAMMER',10)";
    String sql_7 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('333-44-5555','DAVID','DAVIDSON','SALES REP',22)";
    String sql_8 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('444-55-6666','SMITH','SMITHSON','PROGRAMMER',10)";
    String sql_9 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('555-66-7777','ALLEN','ALISON','MANAGER',10)";
    String sql_10 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('666-77-8888','HARRY','HARRISON','PROGRAMMER',10)";
    String sql_11 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('777-88-9999','HENRY','HENDERSON','SALES REP',31)";
    String sql_12 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('888-99-0000','JOHN','JOHNSON','SALES REP',22)";
    String sql_13 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('999-00-1111','JACK','JACKSON','SALES REP',31)";
    String sql_14 =
        "INSERT INTO EMPLOYEES VALUES " +
        "('122-13-1444','JAMES','JAMESON','PROGRAMMER',31)";

    // TEST SAVEPOINT---------------------------------------------------------

    try {
        this.setAutoCommit(conn,false);
        stmt = this.createStatement(conn);
        stmt.executeUpdate("DROP TABLE BUILDINGS");
    } catch (SQLException sqlex) { /* DO NOTHING */ }

    // -----------------------------------------------------------------------

    try {
        stmt.executeUpdate("DROP table EMPLOYEES");
    } catch (SQLException sqlex) { /* DO NOTHING */ }

    // -----------------------------------------------------------------------

    try {
        stmt.executeUpdate("CREATE TABLE BUILDINGS " +
        "(bldg_id integer not null primary key, " +
        " location varchar(30))");

        stmt.executeUpdate("CREATE TABLE EMPLOYEES " +
        "(SSN varchar(11) not null primary key, " +
        " f_name varchar (20), " +
        " l_name varchar (20), " +
        " position varchar (20), " +
        " bldg_id integer, foreign key (bldg_id) references BUILDINGS)") ;

        conn.commit();

        stmt.executeUpdate(sql_1);
        stmt.executeUpdate(sql_2);
        stmt.executeUpdate(sql_4);
        stmt.executeUpdate(sql_5);
        stmt.executeUpdate(sql_6);
        stmt.executeUpdate(sql_7);
        stmt.executeUpdate(sql_8);



    } catch (SQLException sqlex) { System.out.println (sqlex.getMessage()); }

    // -----------------------------------------------------------------------

    try {

        System.out.println ("TABLE VIEW BEFORE SAVEPOINT ( )");

        resultSet = stmt.executeQuery
       ("SELECT F_NAME, L_NAME, LOCATION FROM EMPLOYEES E,BUILDINGS B " +
        "WHERE B.BLDG_ID = E.BLDG_ID ORDER BY F_NAME");
        this.printRows(resultSet);
        savepoint = this.setSavepoint(conn);
        this.closeRS(resultSet);

        System.out.println ("TABLE VIEW AFTER SAVEPOINT ( )");

        stmt.executeUpdate(sql_3);
        stmt.executeUpdate(sql_11);
        stmt.executeUpdate(sql_13);
        stmt.executeUpdate(sql_14);

        resultSet = stmt.executeQuery
        ("SELECT F_NAME, L_NAME, LOCATION FROM EMPLOYEES E,BUILDINGS B " +
        "WHERE B.BLDG_ID = E.BLDG_ID ORDER BY F_NAME");
        this.printRows(resultSet);
        this.rollback(conn,savepoint);
        this.closeRS(resultSet);

        System.out.println ("TABLE VIEW AFTER ROLLING BACK TO SAVEPOINT( )");

        resultSet = stmt.executeQuery
        ("SELECT F_NAME, L_NAME, LOCATION FROM EMPLOYEES E,BUILDINGS B " +
        "WHERE B.BLDG_ID = E.BLDG_ID ORDER BY F_NAME");
        this.printRows(resultSet);
        this.closeRS(resultSet);

       } catch (SQLException sqlex) {
         System.out.println ( sqlex.getMessage ());

      }

    // -----------------------------------------------------------------------

    try {

        System.out.println ("TABLE VIEW BEFORE SAVEPOINT (String savepoint)");

        resultSet = stmt.executeQuery
       ("SELECT F_NAME, L_NAME, LOCATION FROM EMPLOYEES E,BUILDINGS B " +
        "WHERE B.BLDG_ID = E.BLDG_ID ORDER BY F_NAME");
        this.printRows(resultSet);
        this.closeRS(resultSet);

        savepoint = this.setSavepoint(conn,"SAVEPOINT");

        System.out.println ("TABLE VIEW AFTER SAVEPOINT (String savepoint )");

        stmt.executeUpdate(sql_9);
        stmt.executeUpdate(sql_10);
        stmt.executeUpdate(sql_12);


        resultSet = stmt.executeQuery
        ("SELECT F_NAME, L_NAME, LOCATION FROM EMPLOYEES E,BUILDINGS B " +
        "WHERE B.BLDG_ID = E.BLDG_ID ORDER BY F_NAME");
        this.printRows(resultSet);
        this.rollback(conn,savepoint);
        this.closeRS(resultSet);

        System.out.println
        ("TABLE VIEW AFTER ROLLING BACK TO SAVEPOINT(String savepoint )");

        resultSet = stmt.executeQuery
        ("SELECT F_NAME, L_NAME, LOCATION FROM EMPLOYEES E,BUILDINGS B " +
        "WHERE B.BLDG_ID = E.BLDG_ID ORDER BY F_NAME");
        this.printRows(resultSet);
        this.closeRS(resultSet);

    } catch (SQLException sqlex) {System.out.println(sqlex.getMessage());}


    // TEST ROLLBACK () -----------------------------------------------------

    try {
      stmt.executeUpdate("DROP TABLE BUILDINGS");
    } catch (SQLException sqlex) { /* DO NOTHING */ }

    // -----------------------------------------------------------------------

    try {
        stmt.executeUpdate("DROP TABLE EMPLOYEES");
    } catch (SQLException sqlex) { /* DO NOTHING */ }

    // -----------------------------------------------------------------------

    try {
        stmt.executeUpdate("CREATE TABLE BUILDINGS " +
        "(bldg_id integer not null primary key, " +
        " location varchar(20))");

        stmt.executeUpdate("CREATE TABLE EMPLOYEES " +
        "(SSN varchar(11) not null primary key, " +
        " f_name varchar (20), " +
        " l_name varchar (20), " +
        " position varchar (20), " +
        " bldg_id integer, foreign key (bldg_id) references BUILDINGS)") ;

        conn.commit();

    }catch (SQLException sqlex) {System.out.println (sqlex.getMessage ());}




    try {

     System.out.println ("TABLE VIEW BEFORE INSERTS");
     resultSet = stmt.executeQuery
     ("SELECT SSN, F_NAME, L_NAME, POSITION FROM EMPLOYEES E,BUILDINGS B " +
      "WHERE B.BLDG_ID = E.BLDG_ID ORDER BY SSN");
      this.printRows(resultSet);
      this.closeRS(resultSet);

        stmt.executeUpdate(sql_1);
        stmt.executeUpdate(sql_2);
        stmt.executeUpdate(sql_3);
        stmt.executeUpdate(sql_4);
        stmt.executeUpdate(sql_5);
        stmt.executeUpdate(sql_6);
        stmt.executeUpdate(sql_7);
        stmt.executeUpdate(sql_8);
        stmt.executeUpdate(sql_9);
        stmt.executeUpdate(sql_10);
        stmt.executeUpdate(sql_11);
        stmt.executeUpdate(sql_12);
        stmt.executeUpdate(sql_13);
        stmt.executeUpdate(sql_14);

       System.out.println ("TABLE VIEW AFTER INSERTS");
       resultSet = stmt.executeQuery
       ("SELECT SSN, F_NAME, L_NAME, POSITION FROM EMPLOYEES E,BUILDINGS B " +
       "WHERE B.BLDG_ID = E.BLDG_ID ORDER BY SSN");
       this.printRows(resultSet);

       this.rollback(conn);
       this.closeRS(resultSet);

       System.out.println ("TABLE VIEW AFTER ROLLBACK ()");
       resultSet = stmt.executeQuery
       ("SELECT SSN, F_NAME, L_NAME, POSITION FROM EMPLOYEES E,BUILDINGS B " +
       "WHERE B.BLDG_ID = E.BLDG_ID ORDER BY SSN");
       this.printRows(resultSet);
       this.closeRS(resultSet);

    } catch (SQLException sqlex) {System.out.println(sqlex.getMessage());}

    // -----------------------------------------------------------------------

    try {
        savepoint = this.setSavepoint(conn,"SAVEPOINT");
        this.releaseSavepoint(conn, savepoint);
        System.out.println ("\nCREATE A SAVEPOINT THEN RELEASE IT: PASS");
     } catch (SQLException sqlex) {
       System.out.println(sqlex.getMessage());}

    }

    // =======================================================================
    // Method to print the rows of a ResultSet object
    // =======================================================================

@SuppressWarnings("unchecked")
    private static void printRows(ResultSet dbRS)
    throws SQLException {

        ResultSetMetaData rsMD = dbRS.getMetaData();
        int totalcols          = rsMD.getColumnCount();
        Vector tableHeading    = new Vector(totalcols);
        Vector tableRows       = new Vector();
        Vector rowData         = null;

        // Get columns names

        for (int i = 0; i < totalcols; i++) {
             tableHeading.addElement( rsMD.getColumnName(i + 1) );
        }


        // Get row data

        while (dbRS.next()) {

            rowData = new Vector(totalcols);

            for (int i = 0; i < totalcols; i++) {
                switch (rsMD.getColumnType(i + 1)) {
                   case java.sql.Types.BIGINT:
                       rowData.addElement(new Long(dbRS.getLong(i + 1)));
                                break;
                   case java.sql.Types.LONGVARBINARY:
                   case java.sql.Types.VARBINARY:
                   case java.sql.Types.BINARY:
                       rowData.addElement (dbRS.getBytes(i + 1));
                                break;
                    case java.sql.Types.BIT:
                    case java.sql.Types.BOOLEAN:
                    rowData.addElement (new Boolean(dbRS.getBoolean(i + 1)));
                                break;
                    case java.sql.Types.LONGVARCHAR:
                    case java.sql.Types.VARCHAR:
                    case java.sql.Types.CHAR:
                        rowData.addElement (dbRS.getString(i + 1));
                                break;
                    case java.sql.Types.DATE:
                        rowData.addElement (dbRS.getDate(i + 1));
                                break;
                    case java.sql.Types.NUMERIC:
                    case java.sql.Types.DECIMAL:
                        rowData.addElement (dbRS.getBigDecimal(i + 1));
                                break;
                    case java.sql.Types.DOUBLE:
                    case java.sql.Types.FLOAT:
                        rowData.addElement(new Double(dbRS.getDouble(i + 1)));
                                break;
                    case java.sql.Types.INTEGER:
                        rowData.addElement(new Integer(dbRS.getInt(i + 1)));
                                break;
                    case java.sql.Types.REAL:
                        rowData.addElement(new Float(dbRS.getFloat(i + 1)));
                                break;
                    case java.sql.Types.SMALLINT:
                        rowData.addElement(new Short(dbRS.getShort(i + 1)));
                                break;
                   case java.sql.Types.TIME:
                       rowData.addElement(dbRS.getTime(i + 1));
                                break;
                    case java.sql.Types.TIMESTAMP:
                        rowData.addElement(dbRS.getTimestamp(i + 1));
                                break;
                    case java.sql.Types.TINYINT:
                        rowData.addElement(new Byte(dbRS.getByte(i + 1)));
                                break;
                   default:
                        System.out.println ("UPDATE ME TO HANDLE DATATYPE");
                            break;
                }

            }
            tableRows.addElement(rowData);
         }
        System.out.println("\nCOLS : " + tableHeading);
        for (int i = 0; i < tableRows.size(); i++) {
            System.out.println("ROW " + (i+1) + ": " + tableRows.get(i));
        }
        System.out.println("");
    }




    // =======================================================================
    // Close a ResultSet object
    // =======================================================================
    public static void closeRS(ResultSet r)
    throws SQLException {
 	try {
        if (r != null) {
            r.close();
            System.out.println("ResultSet Closed ...");
            }
        } catch (SQLException sqlex) {
            System.out.println(sqlex.getMessage());
        }
    }

    // =======================================================================
    // Close a Connection object
    // =======================================================================
    public static void closeCN(Connection c)
    throws SQLException {
        try {
        if (!c.isClosed()) {
            c.close();
            System.out.println("");
            System.out.println("Connection Closed ...");
            }
	} catch (SQLException sqlex) { System.out.println(sqlex.getMessage()); }
    }

    // =======================================================================
    // Command Line Connections
    // Modify types of Connections
    // =======================================================================
    public static void main(String args[]) {

        ConnectionSanity jdbcConn = new ConnectionSanity();
        Connection dbConnection         = null;


            try {

                dbConnection = jdbcConn.makeConnection(args[0],args[1],args[2]);
                jdbcConn.testMetaData (dbConnection);
                jdbcConn.testTransActionIsolation (dbConnection);
                jdbcConn.testAutoCommit(dbConnection);
                jdbcConn.testReadOnly(dbConnection);
                jdbcConn.testHoldability(dbConnection);
                jdbcConn.testCreateStatement(dbConnection);
                jdbcConn.testPrepareCall(dbConnection);
                jdbcConn.testPrepareStatement(dbConnection);
                jdbcConn.testSavepoint(dbConnection);
                jdbcConn.closeCN(dbConnection);

            }catch (SQLException sqlex) {
	     System.out.print(sqlex.getMessage());
	    }

     } //END OF MAIN

 }
