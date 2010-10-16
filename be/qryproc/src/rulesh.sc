/* 
**	rulesh.sc
**
**	header file for rules tests.
**
**	history:
**		jan 16/89 sgp
**			- created.
**		28-Aug-1997  (merja01)
**			Change long to int to make compatable with 64 bit
**			platforms such as axp_osf.
*/

#define	ALL_TYPES		23
#define	BLDG			20
#define	CHAIN_1			14
#define	CHAIN_2			15
#define	CHAIN_3			16
#define	DEPT			21
#define	DULL			13
#define	EMP			22
#define	GRAPH_1			1
#define	GRAPH_2			2
#define	GRAPH_3			19
#define	LOG			12
#define	SMALL_1			3
#define	SMALL_2			4
#define	SMALL_3			5
#define	SMALL_4			6
#define	SMALL_5			7
#define	SMALL_6			8
#define	SMALL_7			9
#define	SMALL_8			10
#define	SMALL_9			11
#define	STD_0100		24

exec sql begin declare section;

struct all_types_ {
	short	i1;
	short	i2;
	int	i4;
	float	f4;
	double	f8;
	char	c10[11];
	char	char10[11];
	char	vchar10[11];
	char	varchar10[11];
	char	d[26];
	double	m;
} all_types;

struct bldg_ {
	char	bldg[3];
	char	st_adr[31];
	char	city[16];
	char	state[3];
	char	zip[6];
} bldg;

struct chain_1_ {
	int	id;
} chain_1;

struct chain_2_ {
	int	id;
} chain_2;

struct chain_3_ {
	int	id;
} chain_3;

struct dept_ {
	char	dname[11];
	char	div[4];
	int	sales;
	char	bldg[2];
	int	floor;
	int	num_emp;
} dept;

struct dull_ {
	int	dull;
} dull;

struct emp_ {
	char	name[11];
	float	salary;
	char	dept[9];
	char	div[4];
	char	mgr[11];
	char	birthdate[26];
	int	num_dep;
} emp;

struct graph_1_ {
	int	from_node;
	int	to_node;
} graph_1;

struct graph_2_ {
	int	from_node;
	int	to_node;
} graph_2;

struct graph_3_ {
	int	from_node;
	int	to_node;
} graph_3;

struct ruleslog_ {
	char	entry[81];
} ruleslog;

struct small_1_ {
	int	ida;
	int	idb;
} small_1;

struct small_2_ {
	int	ida;
} small_2;

struct small_3_ {
	int	ida;
	int	ida_sub;
} small_3;

struct small_4_ {
	int	ida;
	int	ida_sub;
} small_4;

struct small_5_ {
	int	idb;
} small_5;

struct small_6_ {
	int	idb;
	int	idc;
} small_6;

struct small_7_ {
	int	idc;
	int	idc_sub;
} small_7;

struct small_8_ {
	int	idc;
	int	ida;
} small_8;

struct small_9_ {
	int	idc;
} small_9;

struct _std_0100_ {
	char	a[18];
	double	b;
	int	c;
	short	d;
	char	e[24];
	char	f[91];
	char	g[26];
	char	h[26];
	char	i[18];
	double	j;
	short	k;
	short	l;
	char	m[4];
	double	n;
	char	o[57];
	float	p;
	char	q[8];
	short	r;
	char	s[30];
	char	t[43];
	char	u[9];
	int	v;
	short	w;
	char	x[9];
	short	y;
	char	z[26];
	char	aa[18];
	double	bb;
	int	cc;
	short	dd;
	char	ee[24];
	char	ff[91];
	char	gg[26];
	char	hh[26];
	char	ii[18];
	double	jj;
	short	kk;
	short	ll;
	char	mm[4];
	double	nn;
	char	oo[57];
	float	pp;
	char	qq[8];
	short	rr;
	char	ss[30];
	char	tt[43];
	char	uu[9];
	int	vv;
	short	ww;
	char	xx[9];
	short	yy;
	char	zz[26];
	char	aaa[18];
	double	bbb;
	int	ccc;
	short	ddd;
	char	eee[24];
	char	fff[91];
	char	ggg[26];
	char	hhh[26];
	char	iii[18];
	double	jjj;
	short	kkk;
	short	lll;
	char	mmm[4];
	double	nnn;
	char	ooo[57];
	float	ppp;
	char	qqq[8];
	short	rrr;
	char	sss[30];
	char	ttt[43];
	char	uuu[9];
	int	vvv;
	short	www;
	char	xxx[9];
	short	yyy;
	char	zzz[26];
	char	aaaa[18];
	double	bbbb;
	int	cccc;
	short	dddd;
	char	eeee[24];
	char	ffff[91];
	char	gggg[26];
	char	hhhh[26];
	char	iiii[18];
	double	jjjj;
	short	kkkk;
	short	llll;
	char	mmmm[4];
	double	nnnn;
	char	oooo[57];
	float	pppp;
	char	qqqq[8];
	short	rrrr;
	char	ssss[30];
	char	tttt[43];
	char	uuuu[9];
	int	vvvv;
	short	wwww;
	char	xxxx[9];
	short	yyyy;
	char	zzzz[26];
	char	aaaaa[18];
	double	bbbbb;
	int	ccccc;
	short	ddddd;
	char	eeeee[24];
	char	fffff[91];
	char	ggggg[26];
	char	hhhhh[26];
	char	iiiii[18];
	double	jjjjj;
	short	kkkkk;
	short	lllll;
	char	mmmmm[4];
	double	nnnnn;
	char	ooooo[57];
	float	ppppp;
	char	qqqqq[8];
	short	rrrrr;
	char	sssss[30];
	char	ttttt[43];
	char	uuuuu[9];
	int	vvvvv;
	short	wwwww;
} std_0100;

exec sql end declare section;
