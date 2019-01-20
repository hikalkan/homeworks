Program YILAN_OYUNU;

{ Bu program Halil ˜brahim Kalkan Tarafndan 16/06/2002 Tarihinde Yapld.

                             WWW.KALKAN.CJB.NET                             }

uses crt, dos, graph;
label tusbekleyelim;
TYPE
NoktaGos=^nokta_tipi;
nokta_tipi=record
x,y:byte;
onceki,sonraki:noktagos;
end;

VAR
bas,son:noktagos;
yon:byte; {Ylann o anki gidiŸ y”n. 1->Sa§a 2->Sola 3->Yukar 4->AŸa§}
tus, tus2:char;
puan:longint; {Oyuncunun puan}
kx,ky:byte; {yiyece§in kordinatlar}
derece:byte; {Oyunun zorluk derecesi}
sesli, yem_yenildi, oyunsonu : boolean;
hiz:integer; {Oyunun hz}

PROCEDURE BEKLE(Zaman:Longint); {Delay yerine kullanyorum}
VAR
SA,DK,SN,SL:WORD;
ZMN1,ZMN2:LONGINT;
BEGIN
GetTime(SA,DK,SN,SL);
ZMN1:=SL+SN*100+DK*6000+SA*360000;
REPEAT
GetTime(SA,DK,SN,SL);
ZMN2:=SL+SN*100+DK*6000+SA*360000;
UNTIL ((ZMN2-ZMN1)>ZAMAN) or ((zmn1-zmn2)>zaman);
END;

Procedure Grafik_ac; {Grafik Moduna Ge‡iŸ}
VAR
GD,GM:INTEGER;
BEGIN
GD:=DETECT;
INITGRAPH(GD,GM,'');
END;

Function doluMu(x,y:byte):boolean;
begin
doluMu:=(getpixel(224+x*6,174+y*6)=lightgray);
end;

Procedure oyun_cercevesi;
begin
setcolor(lightblue);
rectangle(226,176,408,298);
end;

Procedure noktaciz(x,y:byte);
begin
setcolor(15);
setfillstyle(1,lightgray);
fillellipse(224+x*6,174+y*6,2,2);
end;

Procedure noktasil(x,y:byte);
begin
setcolor(0);
setfillstyle(1,0);
fillellipse(224+x*6,174+y*6,2,2);
end;


Procedure Dereceyi_belirle;
var
t:char;
begin clrscr;
writeln('[1..9] aras bir seviye belirleyin...');
writeln('1 -> En YavaŸ, 9 -> En Hzl');
write('Se‡iminiz..: ');
repeat
t:=readkey;
until t in['1'..'9'];
derece:=ord(t)-48;
end;

Procedure hizi_belirle;
begin
case derece of
1:hiz:=18;
2:hiz:=16;
3:hiz:=14;
4:hiz:=12;
5:hiz:=10;
6:hiz:=8;
7:hiz:=6;
8:hiz:=4;
9:hiz:=2;
end;
end;

Procedure basaEkle;
var
p1:noktagos;
begin
new(p1);
bas^.sonraki:=p1;
case yon of
1:begin
if bas^.x<30 then p1^.x:=bas^.x+1 else p1^.x:=1;
p1^.y:=bas^.y;
end; {Sa§a}
2:begin
if bas^.x>1 then p1^.x:=bas^.x-1 else p1^.x:=30;
p1^.y:=bas^.y;
end; {Sola}
3:begin
if bas^.y>1 then p1^.y:=bas^.y-1 else p1^.y:=20;
p1^.x:=bas^.x;end; {Yukar}
4:begin
if bas^.y<20 then p1^.y:=bas^.y+1 else p1^.y:=1;
p1^.x:=bas^.x;
end; {AŸa§}
end;
p1^.sonraki:=nil;
p1^.onceki:=bas;
bas:=p1;
noktaciz(bas^.x,bas^.y);
end;

Procedure kuyrukSil;
var
p1:noktagos;
begin
noktasil(son^.x,son^.y);
p1:=son^.sonraki;
dispose(son);
son:=p1;
son^.onceki:=nil;
end;

Procedure ilk_yilani_tanimla;
var
a:byte;
begin
new(son);
son^.onceki:=nil;
son^.sonraki:=nil;
son^.x:=1;
son^.y:=10;
bas:=son;
for a:=1 to 3 do basaekle;
end;

Procedure ilk_yilani_ciz;
var
a:byte;
p1:noktagos;
begin
p1:=son;
noktaciz(p1^.x,p1^.y);
while p1^.sonraki<>nil do begin
p1:=p1^.sonraki;
noktaciz(p1^.x,p1^.y);
end;
end;

Procedure yem_koy;
begin
repeat
kx:=random(30)+1;
ky:=random(20)+1;
until (not (dolumu(kx,ky)));
setcolor(15);
setfillstyle(1,lightred);
fillellipse(224+kx*6,174+ky*6,2,2);
end;

Procedure puan_yaz;
var
pn:string;
begin
setcolor(0);
setfillstyle(1,0);
bar(348,165,408,174);
str(puan,pn);
setcolor(lightgray);
outtextxy(350,166,pn)
end;

Procedure bellegi_bosalt; {Oyun sonunda belle§i tekrar boŸaltr}
var p1,p2:noktagos;
begin
p1:=son;
p2:=son^.sonraki;
repeat
dispose(p1);
p1:=p2;
p2:=p1^.sonraki;
until p1=nil;
end;

begin
dereceyi_belirle;
randomize;
grafik_ac;
oyun_cercevesi;
yon:=1;
ilk_yilani_tanimla;
ilk_yilani_ciz;
oyunsonu:=false;
yem_yenildi:=true;
sesli:=true;
puan:=0;
hizi_belirle;
puan_yaz;
outtextxy(300,166,'Puan:');
outtextxy(250,366,'www.kalkan.cjb.net');
outtextxy(500,456,'D   -> Durdur');
outtextxy(500,466,'ESC -> Cikis');

{Oyun baŸlyor...}
repeat
if yem_yenildi then begin yem_koy;yem_yenildi:=false;end else kuyruksil;
basaekle;
bekle(hiz);

 if ((yon=1) and (((bas^.x=30) and (dolumu(1,bas^.y))) or dolumu(bas^.x+1,bas^.y)))
 or ((yon=2) and (((bas^.x=1) and (dolumu(30,bas^.y))) or dolumu(bas^.x-1,bas^.y)))
 or ((yon=3) and (((bas^.y=1) and (dolumu(bas^.x,20))) or dolumu(bas^.x,bas^.y-1)))
 or ((yon=4) and (((bas^.y=20) and (dolumu(bas^.x,1))) or dolumu(bas^.x,bas^.y+1)))
 then bekle(7);

if keypressed then begin
tusbekleyelim:;
tus:=readkey;
if tus=#0 then begin
tus:=readkey;
case tus of
#77,^D    : if yon<>2 then yon:=1;
#75,^S    : if yon<>1 then yon:=2;
#72,^W,^E : if yon<>4 then yon:=3;
#80,^Z,^X : if yon<>3 then yon:=4;
end;{case tus of...}
end;{if tus=#0 then...}
if tus in['d','D'] then goto tusbekleyelim;
end;{if keypressed..}
case yon of
1:if (doluMu(bas^.x+1,bas^.y)) or ((bas^.x=30) and (doluMu(1,bas^.y))) then oyunsonu:=true;
2:if (doluMu(bas^.x-1,bas^.y)) or ((bas^.x=1) and (doluMu(30,bas^.y))) then oyunsonu:=true;
3:if (doluMu(bas^.x,bas^.y-1)) or ((bas^.y=1) and (doluMu(bas^.x,20))) then oyunsonu:=true;
4:if (doluMu(bas^.x,bas^.y+1)) or ((bas^.y=20) and (doluMu(bas^.x,1))) then oyunsonu:=true;
end;
if (bas^.x=kx) and (bas^.y=ky) then begin
yem_yenildi:=true;
puan:=puan+derece;
puan_yaz;
if sesli then begin sound(500);delay(1);nosound;end;
end;
until (tus=#27) or (oyunSonu);

{Oyun kapanyor...}
outtextxy(280,350,'Oyun Sonu!');
repeat
tus:=readkey;
until tus in[#27,#13];
bellegi_bosalt;
closegraph;
textcolor(lightred);
gotoxy(27,10);write('Programlayan  : Halil ˜brahim Kalkan');
gotoxy(27,11);write('E-mail adresi : webmaster@kalkan.cjb.net');
gotoxy(27,12);write('Web Sitesi    : http://www.kalkan.cjb.net');
textcolor(lightblue);
gotoxy(43,13);write('ÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄ');
textcolor(lightgray);
end.