/*
History:
        15-Oct-2008 - removed cusername for VMS compatibility
        03-Nov-2008 (wanfr01) Cleaned up compiler warnings
        13-Feb-2009 (sarjo01) Cleanup 
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

EXEC sql include sqlca;

EXEC sql begin declare section;
    char *dbname;
    char emsg[1024];
EXEC sql end declare section;

int NonFatalSQLErrorHandler()
{
   EXEC sql begin declare section;
      char errbuff[2000];
   EXEC sql end declare section;

   EXEC sql inquire_sql(: errbuff=errortext);
   sprintf (emsg,"Ingres error: %s\n",errbuff);
   return(0);
}

int SQLErrorHandler()
{
   EXEC sql begin declare section;
      char errbuff[2000];
   EXEC sql end declare section;


   EXEC sql inquire_sql(:errbuff=errortext);
   EXEC sql rollback;
   sprintf (emsg,"Ingres error: %s\n",errbuff);

   EXEC sql disconnect;
   exit(1);
   return(0);
}

void execute_dbproc(int in_param, int idx)
{
   EXEC sql begin declare section;
      int divisor=2;
      int result;
      short n[1] = {0};
      int  r_stat = -1;
   EXEC sql end declare section;
        
   divisor = in_param;

   printf ("Exec %d start\n", idx);
   EXEC SQL WHENEVER SQLERROR CALL NonFatalSQLErrorHandler;
    
   EXEC SQL execute procedure b119483_p1(divisor=:divisor)
             result row (:result:n[0]) into :r_stat;
   EXEC SQL BEGIN;
      if (sqlca.sqlcode != 0 )
      {
         printf ("*** in loop; sqlca.sqlcode = %d\n", sqlca.sqlcode );
      }
      printf("row value: %d\n", (n[0] == 0)?result:-999);
   EXEC SQL END;
   if (sqlca.sqlcode != 0 )
   {
      printf ("*** sqlca.sqlcode = %d\n", sqlca.sqlcode );
   }

   printf("row count: %d\n",r_stat);
   EXEC SQL WHENEVER SQLERROR CALL SQLErrorHandler;
   printf ("Exec %d end\n\n", idx);
}

void esql_func()
{
   EXEC SQL WHENEVER SQLERROR CONTINUE;
   EXEC SQL DROP PROCEDURE b119483_p1;
   EXEC SQL WHENEVER SQLERROR CALL SQLErrorHandler;
    
   EXEC SQL COMMIT;

   EXEC SQL create procedure b119483_p1 ( divisor integer4 )
            result row ( integer4 ) as
         declare
             result  integer4;
             row_count int not null;
         begin
             row_count = 1;
    
             result = 2 / divisor;
             return row(:result);
       
             row_count = 2; 

             result = 3 / divisor;
             return row(:result);
       
             return row_count;
         end;

   execute_dbproc(1, 1);
   execute_dbproc(2, 2);
   execute_dbproc(0, 3);
/*
   printf("%s\n", emsg);
*/
   EXEC SQL commit;
   printf ("Done\n");
}

int main (int argc, char *argv[])
{
   dbname = argv[1];

   EXEC sql whenever sqlerror call SQLErrorHandler;

   EXEC sql connect :dbname session 1;

   esql_func();

   EXEC sql disconnect session 1;

   exit(0);
}

