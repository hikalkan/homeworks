Program Agac_Yapisindan_Liste_Olusturma_Algoritmalari;
{
   Bu Program kullanici tarafindan tanimlanan (verileri girilen) bir agac
   uzerinde PreOrder, PostOrder ve InOrder dolasim yaparak PreFix, PostFix
   ve InFix listeler hazirlar ve ekranda goruntuler.

   Programlayan       : Halil ibrahim KALKAN
   Baslangic          : 25.10.2004
   Son degisiklik     : 27.11.2004

                        iletisim Adresleri
   E-posta            : halilibrahimkalkan@yahoo.com
   Web                : http://www.kalkan.cjb.net
}
Uses
    Crt,Dos,Graph;

Type
    DugumGos  = ^DugumTipi;
    DugumTipi = Record
        Deger     : Char;
        Sol       : DugumGos;
        Sag       : DugumGos;
        Ana       : DugumGos;
    End;

Const
    HATA1         = 'Hata: Koke deger girmeden cocuklari giremezsiniz.';
    HATA2         = 'Hata: En azindan ilk koke bir deger girmelisiniz.';

Var
    AnaKok        : DugumGos;

{- Grafik Fonksyonlari ------------------------------------------------------}
Procedure GrafikAc;
Var
    gd,gm:Integer;
Begin
    gd:=Detect;
    Initgraph(gd,gm,'');
End;
{----------------------------------------------------------------------------}
Procedure GrafikKapat;
Begin
    CloseGraph;
End;
{- Belirli bir sre beklemek icin (Delay gibi) ------------------------------}
Procedure Bekle(Zaman:Longint);
Var
    SA,DK,SN,SL:WORD;
    ZMN1,ZMN2:LONGINT;
Begin
    GetTime(SA,DK,SN,SL);
    ZMN1:=SL+SN*100+DK*6000+SA*360000;
    Repeat
        GetTime(SA,DK,SN,SL);
        ZMN2:=SL+SN*100+DK*6000+SA*360000;
    Until ((ZMN2-ZMN1)>Zaman) or ((ZMN1-ZMN2)>Zaman);
End;
{- Bir tusa basilmasini beklemek icin ---------------------------------------}
Procedure TusBekle(t:Char);
Var
    Tus         : Char;
Begin
    Repeat
        Tus:=Readkey;
    Until Tus=t;
End;
{- Kullaniciya hata Bildirmek icin ------------------------------------------}
Procedure UyariMesaji(Mesaj:String);
Var
    T           : Char;
Begin
    Repeat
    setColor(Red);
    OutTextXy(5,450,Mesaj);
    OutTextXy(5,465,'Devam etmek icin bir tusa basiniz..');
    if keypressed then break;
    Bekle(25);
    SetColor(15);
    OutTextXy(5,450,Mesaj);
    OutTextXy(5,465,'Devam etmek icin bir tusa basiniz..');
    if keypressed then break;
    Bekle(25);
    Until KeyPressed;
    T:=Readkey;if T=#0 then T:=Readkey;
    setColor(0);
    OutTextXy(5,450,Mesaj);
    OutTextXy(5,465,'Devam etmek icin bir tusa basiniz..');
    SetColor(15);
End;
{- Bir Deger Alan Fonksyon --------------------------------------------------}
Function KarakterAl(x,y:Integer;Var S:Char):Byte;
Label
    Son;
Var
    Tus           : Char;
    Tamamlandi    : Boolean;
    Giris         : String;
    Cikis         : Byte;
Procedure AltCizgiYaz;
Begin
    OutTextXY(x+1,y+3,'_');
End;
Procedure AltCizgiSil;
Begin
    SetColor(1);
    OutTextXY(x+1,y+3,'_');
    SetColor(15);
End;
Begin
    SetFillStyle(1,1);
    Tamamlandi:=False;
    if S=' ' Then Giris:='' Else Giris:=S;
    Repeat
        Rectangle(x-1,y-1,x+10,y+12);
        Bar(x,y,x+9,Y+11);
        If Giris<>'' Then OutTextXY(x+1,y+2,Giris);
        AltCizgiYaz;
        Tus:=Readkey;
        Case Tus Of
            #0 {™zel tuslar}
                : Begin
                      Tus:=Readkey;
                      Case Tus Of
                          #83 : Giris:=''; {Delete}
                          #72,#75,#77,#80
                              : Begin
                                    Tamamlandi:=True;
                                    If Giris='' Then S:=' ' Else S:=Giris[1];
                                    AltCizgiSil;
                                    Cikis:=Ord(Tus);
                                End;
                      End;
                      Goto Son;
                  End;
            #37,#42,#43,#45..#57,#65..#90,#97..#122,#246
                : if Giris='' Then Giris:=Tus; {Giris karakterleri}
            #8  : Giris:=''; {Delete}
            #27 : Begin {Escape}
                      If Giris='' Then S:=' ' Else S:=Giris[1];
                      Tamamlandi:=True;
                      AltCizgiSil;
                      Cikis:=27;
                  End;
        End;
        Son:;
    Until Tamamlandi;
    KarakterAl:=Cikis;
End;
{- Verilen Sayinin istenilen dereceden ustunu hesaplar ----------------------}
Function Us(S:Byte;U:Byte):LongInt;
Begin
    If U=0 Then Us:=1 Else Us:=S*Us(S,U-1);
End;
{- Agac icin bir seviyedeki tum kokleri ekrana cizer-------------------------}
Procedure AgacSeviyesiOlustur(Seviye:Byte);
Var
    i             : Byte;
    x,y           : Integer;
    xu,yu         : Integer;
Begin
    SetFillStyle(1,darkgray);
    For i:=1 to Us(2,Seviye-1) Do Begin
        x:=Round((600/Us(2,Seviye))+(600/Us(2,Seviye-1))*(i-1));
        y:=Seviye*80-70;
        Rectangle(x-1,y-1,x+10,y+12);
        Bar(x,y,x+9,Y+11);
        if Seviye>1 Then Begin
            if Odd(i) Then
                xu:=Round((600/Us(2,Seviye-1))+(600/Us(2,Seviye-2))*(i-1)/2)
            Else
                xu:=Round((600/Us(2,Seviye-1))+(600/Us(2,Seviye-2))*(i-2)/2);
            yu:=y-68;
            line(xu+5,yu,x+5,y-2);
        End;
    End;
End;
{- Bir dugumun ekrandaki koordinatlarini verir ------------------------------}
Procedure DugumXY(Seviye,Sira:Byte;Var X,Y:Integer);
Begin
    X:=Round((600/Us(2,Seviye))+(600/Us(2,Seviye-1))*(Sira-1));
    Y:=Seviye*80-70;
End;
{- Agaci EKRANDA olustur ----------------------------------------------------}
Procedure BosAgaciOlustur;
Begin
    AgacSeviyesiOlustur(1);
    AgacSeviyesiOlustur(2);
    AgacSeviyesiOlustur(3);
    AgacSeviyesiOlustur(4);
    AgacSeviyesiOlustur(5);
End;
{- Agaci BELLEKTE olusturur -------------------------------------------------}
Procedure BellekteAgacOlustur;
Procedure Ekle(AnaDugum:DugumGos;RL:Char;Seviye:Byte);
Var
    YeniDugum           : DugumGos;
Begin
    if Seviye>0 Then Begin
        New(YeniDugum);
        YeniDugum^.Deger:=' ';
        YeniDugum^.Sol:=NIL;
        YeniDugum^.Sag:=NIL;
        YeniDugum^.Ana:=AnaDugum;
        if RL='L' Then {L:Left, yani sola ekle}
            AnaDugum^.Sol:=YeniDugum
        Else
            AnaDugum^.Sag:=YeniDugum;
        Ekle(YeniDugum,'L',Seviye-1);
        Ekle(YeniDugum,'R',Seviye-1);
    End;
End;
Begin
    New(AnaKok);
    AnaKok^.Deger:=' ';
    AnaKok^.Ana:=NIL;
    Ekle(AnaKok,'L',4);
    Ekle(AnaKok,'R',4);
End;
{- Bos Dugumleri Siler ------------------------------------------------------}
Procedure BosDugumleriSil;
Procedure BossaKopar(Kok:DugumGos);
Begin
    If Kok^.Sol<>NIL Then
        If Kok^.Sol^.Deger=' ' Then
           Kok^.Sol:=NIL
        Else
           BossaKopar(Kok^.Sol);

    If Kok^.Sag<>NIL Then
        If Kok^.Sag^.Deger=' ' Then
            Kok^.Sag:=NIL
        Else
            BossaKopar(Kok^.Sag);
End;
Begin
BossaKopar(AnaKok);
End;
{- Ekrani siyaha boyar ------------------------------------------------------}
Procedure EkraniSil;
Begin
    SetFillStyle(1,0);
    Bar(0,0,639,479);
End;
{- Agaci, bellekteki agac yapisina gore ekranda yeniden olusturur -----------}
Procedure AgaciYenidenOlustur;
Var
    x,y,xu,yu      : Integer;
Procedure DugumOlustur(Dugum:DugumGos;Seviye,Sira:Byte);
Begin
    Bekle(10);
    DugumXY(Seviye,Sira,X,Y);        {Koordinatlari bul}
    Rectangle(x-1,y-1,x+10,y+12);    {Kutuyu olustur ve degeri yaz}
    Bar(x,y,x+9,y+11);
    OutTextXy(X+1,Y+2,Dugum^.Deger);
    if seviye>1 Then Begin
        if Odd(Sira) Then
            xu:=Round((600/Us(2,Seviye-1))+(600/Us(2,Seviye-2))*(Sira-1)/2)
        Else
           xu:=Round((600/Us(2,Seviye-1))+(600/Us(2,Seviye-2))*(Sira-2)/2);
        yu:=y-68;
        line(xu+5,yu,x+5,y-2);
    End;
    if Dugum^.Sol<>NIL Then DugumOlustur(Dugum^.Sol,Seviye+1,Sira*2-1);
    if Dugum^.Sag<>NIL Then DugumOlustur(Dugum^.Sag,Seviye+1,Sira*2);
End;
Begin
    SetFillStyle(1,Blue);
    DugumOlustur(AnaKok,1,1);
End;
{- Istenilen dugumu gosteren gostericiyi verir ------------------------------}
Function DugumeGit(Kok:DugumGos;Seviye,Sira:Byte):DugumGos;
Begin
if (Seviye=1) Then Begin
    DugumeGit:=Kok
End Else Begin
    If Sira>Us(2,Seviye-2) Then
        DugumeGit:=DugumeGit(Kok^.Sag,Seviye-1,Sira-Us(2,Seviye-2))
    Else
        DugumeGit:=DugumeGit(Kok^.Sol,Seviye-1,Sira);
End;
End;
{- Ekrandan agacin verilerini alir ------------------------------------------}
Procedure VerileriAl;
Var
    CK            : Byte;       { Cikis Kodu }
    Girilen       : Char;       { Girilen Karakter }
    Seviye        : Byte;       { Agacin Gezilen Seviyesi }
    Sira          : Byte;       { Simdiki seviyedeki soldan sira }
    x,y           : Integer;    { Girilen Dugumun koordinatlari }
    Ara           : DugumGos;   { ..pointerler }
    SimdikiDugum  : DugumGos;   { Verisi girilen dugum }
Begin
    Seviye:=1;
    Sira:=1;
    SimdikiDugum:=AnaKok;
    OutTextXy(5,360,'Ok tuslariyla agac uzerinde dolasabilir ve bir karakterlik veri girebilirsiniz.');
    OutTextXy(5,370,'Giris islemini bitirmek icin ESC''ye basin.');
    Repeat
        DugumXY(Seviye,Sira,x,y);
        Girilen:=SimdikiDugum^.Deger;
        CK:=KarakterAl(x,y,Girilen);
        Case CK of
            72,75,77,80,27 { Ok tuslariyla veya ESC ile cikis yapilmissa.. }
            : Begin
                  SimdikiDugum^.Deger:=Girilen;
                  Case CK of
       {Yukari}       72 : if seviye>1 Then Begin
                               SimdikiDugum:=SimdikiDugum^.Ana;
                               seviye:=seviye-1;
                               if Odd(Sira) Then
                                   Sira:=Round((Sira+1)/2)
                               Else
                                   Sira:=Round(Sira/2);
                           End;
       {Asagi}        80 : if seviye<5 Then Begin
                               if SimdikiDugum^.Deger<>' ' Then Begin
                                   SimdikiDugum:=SimdikiDugum^.Sol;
                                   seviye:=seviye+1;
                                   sira:=sira*2-1;
                               End Else Begin
                                   UyariMesaji(HATA1);
                               End;
                           End;
       {Sol}          75 : if (Sira>1) Then Begin
                               Ara:=DugumeGit(AnaKok,Seviye,Sira-1);
                               if Ara^.Ana^.Deger<>' ' Then Begin
                                   SimdikiDugum:=DugumeGit(AnaKok,Seviye,Sira-1);
                                   Sira:=Sira-1;
                               End Else Begin
                                   UyariMesaji(HATA1);
                               End;
                           End;
       {Sag}          77 : if (Sira<Us(2,Seviye-1)) Then Begin
                               Ara:=DugumeGit(AnaKok,Seviye,Sira+1);
                               if Ara^.Ana^.Deger<>' ' Then Begin
                                   SimdikiDugum:=DugumeGit(AnaKok,Seviye,Sira+1);
                                   Sira:=Sira+1;
                               End Else Begin
                                   UyariMesaji(HATA1);
                               End;
                           End;
                  End;
              End;
        End;{Case}
    {En azindan ana koke deger girilmis olmali..}
    If (CK=27) And (AnaKok^.Deger=' ') Then Begin
        UyariMesaji(HATA2);
        CK:=0;
    End;
    Until CK=27;
    SetColor(0);
    OutTextXy(5,360,'Ok tuslariyla agac uzerinde dolasabilir ve bir karakterlik veri girebilirsiniz.');
    OutTextXy(5,370,'Giris islemini bitirmek icin ESC''ye basin.');
    SetColor(15);
End;
{----------------------------------------------------------------------------}
Procedure KokuYesilYap(Seviye,Sira:Byte;Kok:DugumGos);
Var
    x,y         : Integer;
Begin
    DugumXY(Seviye,Sira,x,y);
    SetFillStyle(1,Green);
    SetColor(15);
    Rectangle(x-1,y-1,x+10,y+12);
    Bar(x,y,x+9,Y+11);
    OutTextXy(x+1,y+2,Kok^.Deger);
    Bekle(10);
End;
{----------------------------------------------------------------------------}
Procedure KokuMaviYap(Seviye,Sira:Byte;Kok:DugumGos);
Var
    x,y         : Integer;
Begin
    DugumXY(Seviye,Sira,x,y);
    SetFillStyle(1,Blue);
    SetColor(15);
    Rectangle(x-1,y-1,x+10,y+12);
    Bar(x,y,x+9,Y+11);
    OutTextXy(x+1,y+2,Kok^.Deger);
    Bekle(10);
End;
{- Bir degeri dugum uzerinden listeye tasir (ekranda) -----------------------}
Procedure ListeyeYaz(Seviye,Sira:Byte;Dugum:DugumGos;es:Integer);
Const
    BeklemeSuresi=25;
Var
    x,y         : Integer;
    i           : Byte;
Begin
    DugumXY(Seviye,Sira,x,y);
    For i:=1 to 3 do Begin
        SetFillStyle(1,Red);
        SetColor(14);
        Rectangle(x-1,y-1,x+10,y+12);
        Bar(x,y,x+9,Y+11);
        SetColor(15);
        OutTextXy(x+1,y+2,Dugum^.Deger);
        SetColor(4);
        OutTextXy(es*10-5,380,Dugum^.Deger);
        SetColor(15);
        Bekle(BeklemeSuresi);
        SetFillStyle(1,Blue);
        SetColor(15);
        Rectangle(x-1,y-1,x+10,y+12);
        Bar(x,y,x+9,Y+11);
        OutTextXy(x+1,y+2,Dugum^.Deger);
        SetColor(14);
        OutTextXy(es*10-5,380,Dugum^.Deger);
        SetColor(15);
        Bekle(BeklemeSuresi);
    End;
End;
{- ANiMASYONLAR -------------------------------------------------------------}
{
Not: Isin icine animasyon girince PreOrderDolas fonksyonuna birkac parametre
daha eklemek gerekti. Bu parametreler dugumun ekrandaki koordinatlarini
tespit etmekten baska bir ise yaramiyor. Ayrica listeye yazma disindaki
islemler de ekrandaki animasyonla ilgili.
}
Procedure PreOrderAnimasyonu;
Var
    es          : Integer; {Bir anda Listedeki eleman sayisi}
Procedure PreOrderDolas(Seviye,Sira:Byte;Kok:DugumGos); {Dolasim Algoritmasi}
Begin
    es:=es+1;
    {Adim 1 : Koku ziyaret et (yani listeye ekle)}
    ListeyeYaz(Seviye,Sira,Kok,es);
    KokuYesilYap(Seviye,Sira,Kok);
    {Adim 2 : Sol cocuk varsa onun icin de PreOrder dolasim yap}
    If Kok^.Sol<>NIL Then PreOrderDolas(Seviye+1,Sira*2-1,Kok^.Sol);
    {Adim 3 : Sag cocuk varsa onun icin de PreOrder dolasim yap}
    If Kok^.Sag<>NIL Then PreOrderDolas(Seviye+1,Sira*2,Kok^.Sag);
    KokuMaviYap(Seviye,Sira,Kok);
End;
Begin
    es:=0;
    OutTextXy(5,365,'PreFix Liste:');
    PreOrderDolas(1,1,AnaKok);
    OutTextXy(10+es*10,380,'[tamamlandi]');
    OutTextXy(5,395,'Devam etmek icin Enter''a basiniz..');
    TusBekle(#13);
    SetFillStyle(1,0);SetColor(0);
    Bar(0,360,640,420);
    SetColor(15);
End;
{----------------------------------------------------------------------------}
Procedure PostOrderAnimasyon;
Var
    es          : Integer; {Bir anda Listedeki eleman sayisi}
Procedure PostOrderDolas(Seviye,Sira:Byte;Kok:DugumGos); {Dolasim Algoritmasi}
Begin
    {Adim 1 : Koke git.}
    {Bu ifade ile kok yesil renkte olur ve oradan gecildigi anlasilir.
    Halihazirda koke gidilmis durumdadir.}
    KokuYesilYap(Seviye,Sira,Kok);
    {Adim 2 : Sol cocuk varsa onun icin de PostOrder dolasim yap}
    If Kok^.Sol<>NIL Then PostOrderDolas(Seviye+1,Sira*2-1,Kok^.Sol);
    {Adim 3 : Sag cocuk varsa onun icin de PostOrder dolasim yap}
    If Kok^.Sag<>NIL Then PostOrderDolas(Seviye+1,Sira*2,Kok^.Sag);
    {Adim 4 : Koku ziyaret et (yani listeye ekle)}
    es:=es+1;
    ListeyeYaz(Seviye,Sira,Kok,es);
End;
Begin
    es:=0;
    OutTextXy(5,365,'PostFix Liste:');
    PostOrderDolas(1,1,AnaKok);
    OutTextXy(10+es*10,380,'[tamamlandi]');
    OutTextXy(5,395,'Devam etmek icin Enter''a basiniz..');
    TusBekle(#13);
    SetFillStyle(1,0);SetColor(0);
    Bar(0,360,640,420);
    SetColor(15);
End;
{----------------------------------------------------------------------------}
Procedure InOrderAnimasyon;
Var
    es          : Integer; {Bir anda Listedeki eleman sayisi}
Procedure InOrderDolas(Seviye,Sira:Byte;Kok:DugumGos); {Dolasim Algoritmasi}
Begin
    {Adim 1 : Koke git.}
    {Burada 'kok' adli arguman ile zaten kok aktif durumda oldugundan
    'koke git' diye birsey yapmaya gerek yok.}
    KokuYesilYap(Seviye,Sira,Kok);
    {Adim 2 : Sol cocuk varsa onun icin de InOrder dolasim yap}
    If Kok^.Sol<>NIL Then InOrderDolas(Seviye+1,Sira*2-1,Kok^.Sol);
    {Adim 3 : Koku ziyaret et (yani listeye ekle)}
    es:=es+1;
    ListeyeYaz(Seviye,Sira,Kok,es);
    KokuYesilYap(Seviye,Sira,Kok);
    {Adim 4 : Sag cocuk varsa onun icin de InOrder dolasim yap}
    If Kok^.Sag<>NIL Then InOrderDolas(Seviye+1,Sira*2,Kok^.Sag);
    KokuMaviYap(Seviye,Sira,Kok);
End;
Begin
    es:=0;
    OutTextXy(5,365,'InFix Liste:');
    InOrderDolas(1,1,AnaKok);
    OutTextXy(10+es*10,380,'[tamamlandi]');
    OutTextXy(5,395,'Devam etmek icin Enter''a basiniz..');
    TusBekle(#13);
    SetFillStyle(1,0);SetColor(0);
    Bar(0,360,640,420);
    SetColor(15);
End;
{----------------------------------------------------------------------------}
{
*****************************************************************************
                                     ™NEMLi
 Yukarida da Dolasma algoritmalari var ama buradakiler en sade hali.
 Cunku yukaridaki algoritmalarda hem dolasim yapiliyor hem de ekranda
 bunun animasyonu gosteriliyordu.
*****************************************************************************
}
Procedure TumListeleriYaz;
Var
    InFixList     : String;
    PreFixList    : String;
    PostFixList   : String;
Procedure PostOrder(Kok:DugumGos);
Begin
    {Not: Koke git islemi arguman aktariminda yapiliyor!}
    {Adim 1: Varsa Sol Kok icin PostOrder Dolasim yapiliyor}
    If Kok^.Sol<>NIL Then PostOrder(Kok^.Sol);
    {Adim 2: Varsa Sag Kok icin PostOrder Dolasim yapiliyor}
    If Kok^.Sag<>NIL Then PostOrder(Kok^.Sag);
    {Adim 3: Kok Listeye Ekleniyor}
    PostFixList:=PostFixList+Kok^.Deger;
End;
Procedure InOrder(Kok:DugumGos);
Begin
    {Not: Koke git islemi arguman aktariminda yapiliyor!}
    {Adim 1: Varsa Sol Kok icin InOrder Dolasim yapiliyor}
    If Kok^.Sol<>NIL Then InOrder(Kok^.Sol);
    {Adim 2: Kok Listeye Ekleniyor}
    InFixList:=InFixList+Kok^.Deger;
    {Adim 3: Varsa Sag Kok icin InOrder Dolasim yapiliyor}
    If Kok^.Sag<>NIL Then InOrder(Kok^.Sag);
End;
Procedure PreOrder(Kok:DugumGos);
Begin
    {Adim 1: Kok Listeye Ekleniyor}
    PreFixList:=PreFixList+Kok^.Deger;
    {Adim 2: Varsa Sol Kok icin PreOrder Dolasim yapiliyor}
    If Kok^.Sol<>NIL Then PreOrder(Kok^.Sol);
    {Adim 3: Varsa Sag Kok icin PreOrder Dolasim yapiliyor}
    If Kok^.Sag<>NIL Then PreOrder(Kok^.Sag);
End;
Begin
    InFixList:='';      {Listeler sifirlaniyor}
    PreFixList:='';
    PostFixList:='';
    PreOrder(AnaKok);
    PostOrder(AnaKok);
    InOrder(AnaKok);
    SetColor(Yellow);
    OutTextXy(5,365,'PreFix Liste  : ' + PreFixList);
    SetColor(Red);
    OutTextXy(5,380,'PostFix Liste : ' + PostFixList);
    SetColor(Green);
    OutTextXy(5,395,'InFix Liste   : ' + InFixList);
    SetColor(7);
    OutTextXy(5,410,'Devam etmek icin Enter''a basiniz..');
    TusBekle(#13);
    SetFillStyle(1,0);SetColor(0);
    Bar(0,360,640,430);
    SetColor(15);
End;
{----------------------------------------------------------------------------}
Procedure AnimasyonSec;
Var
    Tus         : Char;
Procedure ListeyiYaz;
Begin
    OutTextXy(5,360,'Hangi animasyonu gormek istiyorsaniz onundeki tusa basin:');
    OutTextXy(5,375,'[1] PreOrder');
    OutTextXy(5,385,'[2] PostOrder');
    OutTextXy(5,395,'[3] InOrder');
    OutTextXy(5,405,'[4] Sadece Listeleri Gor');
    OutTextXy(5,415,'[5] Cikis');
End;
Begin
    Repeat
        ListeyiYaz;
        Tus:=Readkey;
        SetColor(0);
        ListeyiYaz; {Liste Siliniyor}
        SetColor(15);
        Case Tus Of
            '1' : PreOrderAnimasyonu;
            '2' : PostOrderAnimasyon;
            '3' : InOrderAnimasyon;
            '4' : TumListeleriYaz;
        End;
    Until Tus='5';
End;
{- ANA PROGRAM --------------------------------------------------------------}
Begin
    Clrscr;
    GrafikAc;             { Grafik ortama gecis                              }
    BosAgaciOlustur;      { Ekranda bos bir agac sekli olusturur             }
    BellekteAgacOlustur;  { Bellekte agac yapisini pointerlar ile olusturur  }
    VerileriAl;           { Kullanicidan agacin verilerini alir              }
    BosDugumleriSil;      { Agacin veri girilmeyen dugumlerini bellekten sil }
    EkraniSil;            { Ekrani siyaha boya (sil)                         }
    AgaciYenidenOlustur;  { Bellekteki agac yapisini ekranda yeniden olustur }
    AnimasyonSec;         { Animasyonlari sec ve oynat                       }
    GrafikKapat;          { Grafik ortamindan cikis                          }
End.