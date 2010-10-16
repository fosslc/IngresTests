set autocommit on;
\p\g
drop table gca00;
\p\g
create table gca00(c1i1 i1    with null
                  ,c2i2 i2    with null
                  ,c3i4 i4    with null
                  ,c4f4 f4    with null
                  ,c5f8 f8    with null
                  ,c6d  date  with null
                  ,c7m  money with null
                  ,shud_be char(20) with null) with duplicates;
\p\g
insert into gca00(c1i1,shud_be) values (-128,'-128');\p\g
insert into gca00(c1i1,shud_be) values (127,'127');\p\g
insert into gca00(c2i2,shud_be) values (-32768,'-32768');\p\g
insert into gca00(c2i2,shud_be) values (32767,'32767');\p\g
insert into gca00(c3i4,shud_be) values (-2147483648,'-2147483648');\p\g
insert into gca00(c3i4,shud_be) values (2147483647,'2147483647');\p\g
insert into gca00(c4f4,shud_be) values (-10.000e+37,'-10.000e+37');\p\g
insert into gca00(c4f4,shud_be) values (10.000e+37,'10.000e+37');\p\g
insert into gca00(c5f8,shud_be) values (-10.000e+37,'-10.000e+37');\p\g
insert into gca00(c5f8,shud_be) values (10.000e+37,'10.000e+37');\p\g
insert into gca00(c6d ,shud_be) values ('01-jan-1582','01-jan-1582');\p\g
insert into gca00(c6d ,shud_be) values ('31-dec-2382','31-dec-2382');\p\g
insert into gca00(c7m ,shud_be) values ('$-999999999999.99','$-999999999999.99');\p\g
insert into gca00(c7m ,shud_be) values ('$999999999999.99','$ 999999999999.99');
\p\g
select c1i1,c2i2,c3i4,shud_be from gca00;\p\g 
select c4f4,c5f8     ,shud_be from gca00;\p\g
select c6d ,c7m      ,shud_be from gca00;
\p\g
copy gca00(c1i1   =c0tab       with null ('NULL')
          ,c2i2   =c0tab       with null ('NULL')
          ,c3i4   =c0tab       with null ('NULL')
          ,c4f4   =c0tab       with null ('NULL')
          ,c5f8   =c0tab       with null ('NULL')
          ,c6d    =c0tab       with null ('NULL')
          ,c7m    =c0tab       with null ('NULL')
          ,shud_be=varchar(20) with null ('NULL')
          ,nl=d1) into 'gca00.res';
\p\g
drop table gca00;
\p\g
create table gca00(c1i1 i1    with null
                  ,c2i2 i2    with null
                  ,c3i4 i4    with null
                  ,c4f4 f4    with null
                  ,c5f8 f8    with null
                  ,c6d  date  with null
                  ,c7m  money with null
                  ,shud_be char(20) with null) with duplicates;
\p\g
copy gca00(c1i1   =c0tab       with null ('NULL')
          ,c2i2   =c0tab       with null ('NULL')
          ,c3i4   =c0tab       with null ('NULL')
          ,c4f4   =c0tab       with null ('NULL')
          ,c5f8   =c0tab       with null ('NULL')
          ,c6d    =c0tab       with null ('NULL')
          ,c7m    =c0tab       with null ('NULL')
          ,shud_be=varchar(20) with null ('NULL')
          ,nl=d1) from 'gca00.res';
\p\g
select c1i1,c2i2,c3i4,shud_be from gca00;\p\g 
select c4f4,c5f8     ,shud_be from gca00;\p\g
select c6d ,c7m      ,shud_be from gca00;
\p\g
drop table gca00;
\p\g
\q 
