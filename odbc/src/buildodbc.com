$ cc /float=ieee/nodeb/opt/warning=disable=(questcompare,intrinsicint,implicitfunc)/include='P1' 'P2' odbctest.c
$ lin/noinfo/nodeb/exe=odbctest.exe odbctest.obj, 'P3' /opt
