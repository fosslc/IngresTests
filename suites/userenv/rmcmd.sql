grant select,insert,update,delete on remotecmdinview to testenv; 
grant select,insert,update,delete on remotecmdoutview to testenv; 
grant select,insert,update,delete on remotecmdview to testenv;
grant execute on procedure launchremotecmd to testenv; 
grant execute on procedure sendrmcmdinput to testenv; 
grant register,raise on dbevent rmcmdcmdend to testenv; 
grant register,raise on dbevent rmcmdnewcmd to testenv; 
grant register,raise on dbevent rmcmdnewinputline to testenv; 
grant register,raise on dbevent rmcmdnewoutputline to testenv; 
grant register,raise on dbevent rmcmdstp to testenv;\g\q 


