package tcpip.tcp;

import java.io.*;
import java.net.*;

import jpcap.packet.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Ýki bilgisayar arasýnda kurulan bir TCP baðlantýsýný temsil eder.
 * 
 * @author Halil Ýbrahim KALKAN
 */
public class TCPBaglantisi implements TCPDinleyici
{
	/** FIN bayraðý */
	private static final byte					FIN_bayragi = 0x01;
	/** SYN bayraðý */
	private static final byte					SYN_bayragi = 0x02;
	/** RST bayraðý */
	private static final byte					RST_bayragi = 0x04;
	/** PSH bayraðý */
	private static final byte					PSH_bayragi = 0x08;
	/** ACK bayraðý */
	private static final byte					ACK_bayragi = 0x10;
	/** URG bayraðý */
	private static final byte					URG_bayragi = 0x20;
	
	/** Baðlantý açma iþleminde baðlantýnýn gerçekleþmemesi durumunda ne kadar zaman sonra vazgeçileceði */
	private static final int					baglantiTimeoutSuresi = 30000; //30 saniye
	/** Varsayýlan olarak kullanýlacak benim pencere boyutum */
	private static final int					baslangicPencereBoyutu = 5000; //5000 byte
	/**
	 * Karþý tarafa veri yollarken kullanýlacak kayan pencere
	 * mekanizmasýndaki toplam tampon büyüklüðü. 
	 */
	private static final int					hedefTamponBoyutu = 10000;
	/**
	 * Bir TCP segmentinde gönderilecek maksimum veri boyutunu tanýmlar.
	 * Daha fazla veri gönderilecekse birden çok segment kullanýlýr.
	 * (Varsayýlan deðer)
	 */
	private static final int					maksimumSegmentBoyutu = 1460;
	/** Bu baðlantý için kullanýlan maksimum segment boyutunu saklar */
	private volatile int						maxSegmentBoyutu = maksimumSegmentBoyutu;
	
	/** TCP protokolünü uygulayan nesneye referans */
	private TCP									tcp;
	
	/** TCP baðlantýsýnýn durumunu temsil eder */
	private volatile TCPDurumlari				durum;
	
	/** Baðlantý kurulan hedef cihazýn IP adresi */
	private Inet4Address						hedefIPAdresi;
	/** Hedef cihazýn port numarasý */
	private int									hedefPortNo;
	/** Bu bilgisayarda bu baðlantý için kullanýlan port numarasý */
	private int 								benimPortNo;
	
	/** Karþý cihaza ACK almadan gönderilebilecek maximum byte sayýsý */
	private volatile int						hedefPencereBoyutu = 5000;
	/**
	 * Benim SEQUENCE numaram.
	 * En son gönderdiðim byte'ýn numarasýný saklar.
	 */
	private volatile long						benimSEQ;
	/**
	 * Benim yolladýðým veriler için karþý taraftan en son gelen
	 * ACK numarasý. Baþka bir deyiþle karþý taraftan beklenen ilk byte'ýn
	 * sýra numarasý. 
	 */
	private volatile long						gidenACK;
	/**
	 * Karþý tarafýn SEQUENCE numarasý.
	 * Karþý taraftan alýnan SEQ numarasý ile beraber alýnan byte sayýsýnýn
	 * toplamý ile güncellenir. ACK yollanacaðý zaman bir fazlasý ile yollanýr.
	 */
	private volatile long						hedefSEQ;
	/** Ortalama olarak bir TCP segmenti için ACK alýnma süresi */
	private volatile long						ortalamaACKSuresi = 9000; //9 saniye
	/** Baðlantý açma olayýnda ilk mesajýn maksimum kaç defa tekrar yollanacaðý */
	private static int							maxTekrarYollamaSayisi = 7;
	/** Kayan pencerede kullanýlacak pencere boyutu */
	private volatile int 						benimPencereBoyutum = baslangicPencereBoyutu;
	/** Kayan pencere mekanizmasýný uygulayan nesne */
	private KayanPencere						kayanPencere;
	
	/** Uygulama katmanýnýn karþý cihaza byte yollayabilmesi için */
	private TCPOutputStream						gidenByteOutputStream;
	/** Kayan pencereden verileri alýp aða yollamak için nesne */
	private VeriYollayici						veriYollayici;
	
	/** Bazý görevleri zaman çizelgesine oturtmak için kullanýlan nesne */
	private Timer								zamanlayici;
	/**
	 * Baðlantý isteðini max sayýya ulaþýncaya dek periyodik olarak
	 * tekrarlamak için kullanýlan nesne. 
	 */
	private SegmentTekrarYollamaGorevi			baglantiTekrarDenemesi = null;
	/**
	 * 3 yollu elsýkýþma iþleminde 1. gönderilen mesaja karþýdan cevap
	 * gelinceye dek threat'in beklemesini saðlayan nesne
	 */
	private CountDownLatch						cevapMesajiBekleticisi;
	/** Karþý tarafa baþarýyla baðlanýlýp baðlanýlmadýðýný saklar */
	private boolean								bagliMi = false;
	
	/**
	 * Kurucu fonksyon
	 *
	 */
	private TCPBaglantisi()
	{
		//baþlangýç durumu: kapalý
		durum = TCPDurumlari.CLOSED;
		//gerekli nesneleri oluþtur
		zamanlayici = new Timer(true);
		cevapMesajiBekleticisi = new CountDownLatch(1);
		veriYollayici = new VeriYollayici();
		kayanPencere = new KayanPencere(TCPBaglantisi.hedefTamponBoyutu, veriYollayici);
		gidenByteOutputStream = new MyTCPOutputStream();
	}
	
	/**
	 * Verilen argümanlar doðrultusunda bir TCPBaglantisi nesnesi
	 * oluþturur.
	 * @param IPAdresi Hedef bilgisayarýn IP adresi
	 * @param PortNo Hedef bilgisayarýn port numarasý
	 * @return Eðer oluþturulabildiyse bir TCPBaglantisi nesnesi, deðilse null
	 */
	public static TCPBaglantisi baglantiOlustur(InetAddress IPAdresi, int PortNo)
	{
		TCPBaglantisi baglanti = new TCPBaglantisi();
		int benimPortNo = TCP.getTCP().portNumarasiUret(baglanti);
		if(benimPortNo>=0)
		{
			return TCPBaglantisi.baglantiUret(baglanti, IPAdresi, PortNo, benimPortNo);
		}
		return null;
	}

	/**
	 * Verilen argümanlar doðrultusunda bir TCPBaglantisi nesnesi
	 * oluþturur.
	 * @param IPAdresi Hedef bilgisayarýn IP adresi
	 * @param PortNo Hedef bilgisayarýn port numarasý
	 * @param benimPortNo Bu bilgisayarda bu baðlantý için kullanýlacak port numarasý
	 * @return Eðer oluþturulabildiyse bir TCPBaglantisi nesnesi, deðilse null
	 */
	public static TCPBaglantisi baglantiOlustur(InetAddress IPAdresi, int portNo, int benimPortNo)
	{
		TCPBaglantisi baglanti = new TCPBaglantisi();
		if(TCP.getTCP().portNumarasiTahsisEt(baglanti, benimPortNo))
		{
			return TCPBaglantisi.baglantiUret(baglanti, IPAdresi, portNo, benimPortNo);
		}
		return null;
	}
	
	/**
	 * TCPBaglantisi nesnesini çalýþma için hazýr duruma getirir.
	 * @param baglanti bir TCPBaglantisi nesnesi
	 * @param IPAdresi Hedef bilgisayarýn IP adresi
	 * @param PortNo Hedef bilgisayarýn port numarasý
	 * @param benimPortNo Bu bilgisayarda bu baðlantý için kullanýlacak port numarasý
	 * @return Eðer sorun yoksa baglanti adlý nesne, deðilse null
	 */
	private static TCPBaglantisi baglantiUret(TCPBaglantisi baglanti, InetAddress IPAdresi, int PortNo, int benimPortNo)
	{	
		TCP tcp = TCP.getTCP();
		try 
		{
			baglanti.tcp = tcp;
			
			baglanti.benimPortNo = benimPortNo;
			baglanti.hedefPortNo = PortNo;
			baglanti.hedefIPAdresi = (Inet4Address)IPAdresi;
			
			if(baglanti.ac())
			{
				return baglanti;
			}
			else
			{
				tcp.portNumarasiBirak(baglanti.benimPortNo);
				return null;
			}
		}
		catch (Exception e)
		{
			tcp.portNumarasiBirak(baglanti.benimPortNo);
			return null;
		}
	}

	/**
	 * Baðlantý saðlanmasýnda ilk mesajýn yollanmasýný gerçekleþtiren
	 * motod. Bu metod CLOSED durumunda baðlantý açma isteðini
	 * gerçekleþtirmek için kullanýlýr ve hedefe syn bilgisini yollayýp
	 * SYN_SENT durumuna geçer.
	 * @return yollanan segment
	 */
	private synchronized TCPPacket syn_yolla()
	{
		TCPPacket p = new TCPPacket(
				benimPortNo,							//source port
				hedefPortNo,							//destination port
				benimSEQ,								//sequence number
				0,										//ack number
				false,									//urg flag
				false,									//ack flag
				false,									//psh flag
				false,									//rst flag
				true,									//syn flag
				false,									//fin flag
				false,									//rsv1 flag
				false,									//rsv2 flag
				TCPBaglantisi.baslangicPencereBoyutu,	//my window size
				0										//urgent pointer
				);
		p.data = new byte[]{ }; //data yok!
		//Maksimum Segment büyüklüðü opsiyonu
		p.option = new byte[] {
				0x02, 0x04, 0x05, (byte)0xB4, 0x01, 0x01, 0x04, 0x02
				};
		segmentYolla(p);
		return p;
	}
	/**
	 * SYN_SENT durumundayken karþýdan bir syn mesajý geldiðinde
	 * çaðýrýlan fonksyon. Karþý tarafa syn ve ack bilgilerini yollar
	 * @return yollanan segment
	 */
	private synchronized TCPPacket syn_ack_yolla()
	{
		TCPPacket p = new TCPPacket(
				benimPortNo,							//source port
				hedefPortNo,							//destination port
				benimSEQ,								//sequence number
				hedefSEQ + 1,							//ack number
				false,									//urg flag
				true,									//ack flag
				false,									//psh flag
				false,									//rst flag
				true,									//syn flag
				false,									//fin flag
				false,									//rsv1 flag
				false,									//rsv2 flag
				TCPBaglantisi.baslangicPencereBoyutu,	//my window size
				0										//urgent pointer
				);
		p.data = new byte[]{ }; //data yok!
		p.option = new byte[] {
				0x02, 0x04, 0x05, (byte)0xB4, 0x01, 0x01, 0x04, 0x02
		};
		segmentYolla(p);
		return p;
	}
	
	/**
	 * Herhangi bir durumdayken karþýdaki cihaza ack mesajý yollamak
	 * için kullanýlan fonksyon.
	 * @return yollanan paket
	 */
	private synchronized TCPPacket ack_yolla()
	{
		TCPPacket p = new TCPPacket(
				benimPortNo,							//source port
				hedefPortNo,							//destination port
				benimSEQ,								//sequence number
				hedefSEQ + 1,							//ack number
				false,									//urg flag
				true,									//ack flag
				false,									//psh flag
				false,									//rst flag
				false,									//syn flag
				false,									//fin flag
				false,									//rsv1 flag
				false,									//rsv2 flag
				TCPBaglantisi.baslangicPencereBoyutu,	//my window size
				0										//urgent pointer
				);
		p.data = new byte[]{ }; //data yok!
		segmentYolla(p);
		return p;
	}
	
	/**
	 * Bir TCP segmentinin baþlýk ve veri kýsmýndaki bilgileri deðerlendiren
	 * fonksyon
	 * @param p verileri taþýyan TCP segmenti
	 */
	private synchronized void segmenttenVeriAl(TCPPacket p)
	{
		//Paket baþlýðýndan lazým olan bilgileri al
		hedefSEQ = p.sequence;
		if(p.ack) //ACK bayraðý iþaretliyse ACK deðerini al
		{
			gidenACK = p.ack_num;
		}
		hedefPencereBoyutu = p.window;
		//Paket içerisindeki verileri al
		/*
		 * BURADA GELEN PAKETTEKÝ VERÝLERÝ ALIP UYGULAMA KATMANINA
		 * YOLLAYACAK VE hedefSEQ DEÐERÝNÝ GÜNCELLEYECEK MANTIK OLACAK! 
		 */
	}
	
	/**
	 * {@inheritDoc}
	 */
	public synchronized void paketAl(TCPPacket yeniPaket)
	{
		//Hedef pencere boyutu bilgisini güncelle
		hedefPencereBoyutu = yeniPaket.window;
		//Durum mankinasýna göre durum geçiþlerini yap
		if(durum == TCPDurumlari.ESTABLISHED)
		{
			/*                   fin / ack
			 * ESTABLISHED ----------------------> CLOSE_WAIT
			 */
			if(yeniPaket.fin)
			{
				
			}
			/*
			 * Veri alým durumu. Eðer burasý çalýþýrsa karþý taraf veri
			 * yolluyor demektir.
			 */
			else
			{
				
			}
		}
		else if(durum == TCPDurumlari.SYN_SENT)
		{
			/*                syn / syn+ack
			 * SYN_SENT -------------------------> SYN_RECEIVED  
			 **/ 
			if(yeniPaket.syn && !yeniPaket.ack)
			{
				//durum geçiþini saðla
				durum = TCPDurumlari.SYN_RECEIVED;
				segmenttenVeriAl(yeniPaket);
				syn_ack_yolla();
			}
			/*                syn+ack / ack
			 * SYN_SENT -------------------------> ESTABLISHED  
			 **/ 
			else if(yeniPaket.syn && yeniPaket.ack)
			{
				//durum geçiþini saðla
				durum = TCPDurumlari.ESTABLISHED;
				//karþý tarafýn yolladýðý bilgileri al
				segmenttenVeriAl(yeniPaket);
				//ACK mesajý yolla
				benimSEQ++;
				ack_yolla();
				baglanti_saglandi();
			}
		}
		else if(durum == TCPDurumlari.SYN_RECEIVED)
		{
			/*                        ack
			 * SYN_RECEIVED -------------------------> ESTABLISHED  
			 **/ 
			if(yeniPaket.ack)
			{
				//karþý tarafýn yolladýðý bilgileri al
				segmenttenVeriAl(yeniPaket);
				//durum geçiþini saðla
				durum = TCPDurumlari.ESTABLISHED;
				baglanti_saglandi();
			}
		}
	}

	/**
	 * Baðlantýnýn gerçekleþtirilmesi durumunda ac() fonksyonunu
	 * çaðýran threat'in devam etmesini saðlar.
	 */
	private void baglanti_saglandi()
	{
		//Ýlk mesajý tekrar yollamak için kurulan mekanizmayý durdur
		if(baglantiTekrarDenemesi != null)
		{
			baglantiTekrarDenemesi.cancel();
			baglantiTekrarDenemesi = null;
		}
		//ac fonksyonunda await ile beklemede olan
		//threat'i uyandýr ve baðlantýnýn açýldýðýný bildir.
		this.cevapMesajiBekleticisi.countDown();
	}

	/**
	 * Karþý sunucuya ilgili mesajlarý gönderip alarak baðlantýyý açar.
	 * Baðlantý gerçekleþtirilirse true aksi halde false dönderir.
	 * (AKTÝF AÇMA)
	 * (3 yollu el sýkýþma ile)
	 * @return baðlantýnýn baþarý durumu
	 */
	private boolean ac()
	{
		//baþlangýç SEQ numaramý belirle
		benimSEQ = baslangicSEQ();
		/*             active open / syn
		 * CLOSED --------------------------------> SYN_SENT
		 */
		//durum geçiþini saðla
		durum = TCPDurumlari.SYN_SENT;
		TCPPacket p = syn_yolla();
		//Cevap alýnýncaya kadar mesajý tekrar tekrar yollamak
		//için bir zamanlayýcý mekanizmasý oluþtur		
		baglantiTekrarDenemesi = new SegmentTekrarYollamaGorevi(p);
		zamanlayici.schedule(
				baglantiTekrarDenemesi,
				ortalamaACKSuresi,
				ortalamaACKSuresi
				);
				
		//cevap mesajý gelene kadar ya da timeout süresi dolana kadar bekler.
		try { cevapMesajiBekleticisi.await(TCPBaglantisi.baglantiTimeoutSuresi, TimeUnit.MILLISECONDS); } catch (Exception e) { }
		//timeout durumunda durumu tekrar CLOSED yap
		if(durum != TCPDurumlari.ESTABLISHED)
		{
			durum = TCPDurumlari.CLOSED;
			return false;
		}
		else
		{
			//durum ESTABLISHED, true dönder
			return true;
		}
	}

	/**
	 * Baþlangýç için bir sekans numarasý oluþturur.
	 * @return bir SEQ numarasý
	 */
	private long baslangicSEQ()
	{
		
		return 1245;
	}
	
	/**
	 * Argüman olarak verilen paketi tcp modülü üstünden aða yollar.
	 * @param p gönderilecek TCP segmenti
	 */
	private synchronized void segmentYolla(TCPPacket p)
	{
		tcp.paketYolla(IPPaketineCevir(p), hedefIPAdresi);
	}
	
	/**
	 * Verilen TCPPacket nesnesini bir IPPacket nesnesine çevirir.
	 * Bunu yaparken TCP baþlýk ve option alanlarýný IP paketinde
	 * data olarak yazar.
	 * @param p TCP paketi
	 * @return IP paketi
	 */
	private synchronized IPPacket IPPaketineCevir(TCPPacket p)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try 
		{
			IPPacket ipPaketi = new IPPacket();
			//TCP baþlýðýný yaz
			dos.writeShort(p.src_port);
			dos.writeShort(p.dst_port);
			dos.writeInt((int)p.sequence);
			dos.writeInt((int)p.ack_num);
			//TCP Header Length yaz
			byte TCPHL = 5; 
			if(p.option!=null)
			{
				TCPHL += (p.option.length / 4);
			}
			TCPHL = (byte)(TCPHL << 4);
			dos.writeByte(TCPHL); //TCP Header Length = 20 byte
			//Bayraklarý yaz
			byte bayraklar = 0;
			if(p.fin)
				bayraklar |= FIN_bayragi;
			if(p.syn)
				bayraklar |= SYN_bayragi;
			if(p.rst)
				bayraklar |= RST_bayragi;
			if(p.psh)
				bayraklar |= PSH_bayragi;
			if(p.ack)
				bayraklar |= ACK_bayragi;
			if(p.urg)
				bayraklar |= URG_bayragi;
			dos.writeByte(bayraklar);
			dos.writeShort(p.window);
			dos.writeShort(0); //checksum sonra hesaplanacak
			dos.writeShort(p.urgent_pointer);
			if(p.option!=null)
			{
				dos.write(p.option);
			}
			if(p.data!=null)
			{
				if(p.data.length>0)
				{
					dos.write(p.data);
				}
			}
			ipPaketi.data = baos.toByteArray();
			dos.close();
			//TCP protokolü olduðunu belirt
			ipPaketi.protocol = IPPacket.IPPROTO_TCP;
			//Checksum deðerini hesapla
			checksumHesapla(ipPaketi);
			//Oluþturulan peketi dönder
			return ipPaketi;
		}
		catch (Exception e) 
		{
			return null;
		}
	}
	
	/**
	 * Verilen segment için TCP header checksum deðerini hesaplayýp
	 * segment içerisinde uygun alana yazar.
	 * @param p Checksum hesaplanacak segment
	 */
	private synchronized void checksumHesapla(IPPacket p)
	{
		byte[] cb = null;
		/* Checksum deðerini hesaplamak için gerekli olan veriler
		 * bir byte dizisi haline getiriliyor
		 */
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try 
		{
			//Pseudo Header'ý yaz
			dos.write(tcp.getIPAdresi().getAddress());
			dos.write(hedefIPAdresi.getAddress());
			dos.writeByte(0);
			dos.writeByte(IPPacket.IPPROTO_TCP);
			dos.writeShort(p.data.length);
			//TCP baþlýðý + TCP data kýsmýný yaz
			dos.write(p.data);
			//Gerekiyorsa 1 byte 0 ekle 16 bitin katýna tamamlamak için
			if((p.data.length % 2) == 1)
			{
				dos.writeByte(0);
				System.out.print("--> 1 byte 0 eklendi.");
			}
			//oluþturulan byte dizisini al
			cb = baos.toByteArray();
			dos.close();
		}
		catch (Exception e) 
		{
			return;
		}
		/* Elde edilen byte dizindeki her 16 bitlik sayýyý
		 * alýp topla ve toplamýn 1'e tümleyenini alarak checksum'ý bul
		 */
		ByteArrayInputStream bais = new ByteArrayInputStream(cb);
		DataInputStream dis = new DataInputStream(bais);
		try 
		{
			int toplam = 0;
			//Kaç adet 16 bitlik sayý olduðu bulunuyor
			int k16 = cb.length / 2;
			//Her 16 bitlik sayý toplama ekleniyor
			for(int i=0; i<k16; i++)
			{
				//toplama ekle
				toplam += dis.readUnsignedShort();
				//toplam &= 0x0000FFFF;
			}
			//toplamýn tümleyenini al
			toplam = (toplam & 0xffff) + (toplam >> 16);
			short s = (short)(~(short)((toplam >> 16) + (toplam & 0xffff)));
			//TCP baþlýðýna checksum deðerini yaz
			checkSumYaz(p, s);
		}
		catch (Exception e) 
		{
			return;
		}
	}
	
	/**
	 * Verilen segmente yine verilen checksum deðerini uygun konumda yerleþtirir.
	 * @param p TCP segmenti
	 * @param checksum bu segmente ait checksum deðeri
	 */
	private synchronized void checkSumYaz(IPPacket p, short checksum)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try 
		{
			dos.writeShort(checksum);
			byte b[] = baos.toByteArray();
			dos.close();
			//checksum alaný 16. byte'dan itibaren 2 byte olarak yazýlýyor
			System.arraycopy(b, 0, p.data, 16, 2);
		}
		catch (Exception e) 
		{
			//...
		}
	}
	
	/**
	 * Karþý tarafa bir byte dizisini yollamak için kullanýlýr.
	 * @param veri yollanacak byte dizisi
	 */
	private synchronized void veriYolla(byte[] veri)
	{
		
	}

	/**
	 * Cevap için bekleyen threat'i uyandýr ve baðlantýnýn baþarýsýz olduðunu
	 * bildirir.
	 * @param p cevap gelmeyen paket
	 */
	private synchronized void cevapYok(TCPPacket p)
	{
		bagliMi = false;
		cevapMesajiBekleticisi.countDown();
	}
	
	/**
	 * Karþý tarafa fin bayraðýný 1 olarak yollayarak baðlantýyý 
	 * sonlandýrmak için kullanýlýr
	 */
	private synchronized TCPPacket fin_yolla()
	{
		TCPPacket p = new TCPPacket(
				benimPortNo,							//source port
				hedefPortNo,							//destination port
				benimSEQ + 1,							//sequence number
				hedefSEQ + 1,							//ack number
				false,									//urg flag
				true,									//ack flag
				false,									//psh flag
				false,									//rst flag
				false,									//syn flag
				true,									//fin flag
				false,									//rsv1 flag
				false,									//rsv2 flag
				benimPencereBoyutum,					//my window size
				0										//urgent pointer
				);
		p.data = new byte[]{ }; //data yok!
		segmentYolla(p);
		return p;
	}
	
	/**
	 * Baðlantýyý kapatýr ve karþý cihazla iletiþimi sonlandýrýr.
	 */
	public synchronized boolean kapat()
	{
		if(durum == TCPDurumlari.ESTABLISHED)
		{
			fin_yolla();
			durum = TCPDurumlari.FIN_WAIT_1;
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Karþý cihaza veri yollamak için gerekli
	 * fonksyonlarý saðlayan bir nesne dönderir.
	 * 
	 * @return TCPOutputStream'i uygulayan nesne
	 */
	public TCPOutputStream getOutputStream() 
	{
		return gidenByteOutputStream;
	}
	
	/**
	 * Uygulama katmanýndan baðlantýdaki hedefe veri yollamak için
	 * kullanýlan ve TCPOutputStream arayüzünü uygulayan sýnýf.
	 * 
	 * @author Halil Ýbrahim KALKAN
	 */
	private class MyTCPOutputStream implements TCPOutputStream
	{
		/**
		 * {@inheritDoc}
		 */
		public int bosYer() 
		{
			return kayanPencere.bosYer();
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean yaz(byte b) 
		{
			return kayanPencere.ekleBloklu(b);
		}

		/**
		 * {@inheritDoc}
		 */
		public int yaz(byte[] b, int baslangic, int sayi) 
		{
			return kayanPencere.ekleBloklu(b, baslangic, sayi);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public int yaz(byte[] b) 
		{
			return kayanPencere.ekleBloklu(b);
		}
	}
	
	/**
	 * Uygulama katmanýndan gelen verilerin uygun veri akýþ mantýðý
	 * kullanýlacak hedef cihaza TCP segmentleri halinde
	 * yollanabilmesi için geliþtirilen sýnýf.
	 * 
	 * @author Halil Ýbrahim KALKAN
	 */
	private class VeriYollayici extends Thread implements VeriDinleyicisi
	{
		/** Thread'in çalýþma durumunu gösteren bayrak */
		private volatile boolean 				calisiyor = true;
	
		/**
		 * Kurucu fonksyon
		 */
		public VeriYollayici()
		{
			
		}

		/**
		 * Thread'in giriþ noktasý.
		 */
		public void run()
		{
			while(calisiyor)
			{
				
			}
		}
		
		public void durdur()
		{
			calisiyor = false;
		}
		
		/**
		 * Kayan pencerede gönderilmeyi bekleyen verilerden
		 * hedef pencere boyutunun izin verdiði kadarýný gönderir.
		 * Eðer birden çok segment olacak kadar veri varsa birden
		 * çok segment yollayabilir.
		 */
		public synchronized void birSegmentYolla()
		{
			synchronized(kayanPencere)
			{
				//hedefin tamponunda gönderilen verileri alabilecek yer
				//olduðu sürece kayan pencerede bekleyen verileri
				//segmentler halinde gönder.
				int hedefTampon = hedefPencereBoyutu;
				//gönderilen ancak ACK alýnmayan byte sayýsýný hesapla
				long ackBekleyen = benimSEQ - gidenACK;
				//tamponda olan ancak henüz gönderilmeyen byte sayýsýný hesapla
				long gonderilmeyiBekleyen = kayanPencere.getVeriBoyutu() - ackBekleyen;
				while((hedefTampon > 0) && (gonderilmeyiBekleyen > 0))
				{
					//bu segment ile gönderilecek byte sayýsý hesaplanýyor
					long gonderilecek = gonderilmeyiBekleyen;
					//Eðer bu sayý karþý tarafýn alabileceðinden fazlaysa
					//alabileceði kadarýný gönderiyoruz.
					if(gonderilecek > hedefTampon)
					{
						gonderilecek = hedefPencereBoyutu;						
					}
					//Eðer bu sayý maksimum segment boyutundan fazlaysa
					//bir segmentte gönderilebilecek maksimum byte
					//sayýsý kadar byte yolluyoruz.
					if(gonderilecek > maxSegmentBoyutu)
					{
						gonderilecek = maxSegmentBoyutu;
					}
					byte b[] = new byte[(int)gonderilecek];
					kayanPencere.al((int)ackBekleyen, b);
					//gönderilecek veri segmentini oluþtur
					benimSEQ += 1;
					TCPPacket veriSegmenti = new TCPPacket(
							benimPortNo,							//source port
							hedefPortNo,							//destination port
							benimSEQ-1,								//sequence number
							hedefSEQ+1,								//ack number
							false,									//urg flag
							true,									//ack flag
							true,									//psh flag
							false,									//rst flag
							false,									//syn flag
							false,									//fin flag
							false,									//rsv1 flag
							false,									//rsv2 flag
							benimPencereBoyutum,					//my window size
							0										//urgent pointer
							);
					//veri olarak byte dizisini ata
					veriSegmenti.data = b;
					//segmenti yolla
					segmentYolla(veriSegmenti);
					//SEQ deðerimi ayarla
					benimSEQ += gonderilecek;
					//Hedef tampon deðiþkenini ayarla (döngünün sonlanmasý için)
					hedefTampon -= gonderilecek;
					//gonderilmeyiBekleyen deðiþkenini ayarla (döngünün sonlanmasý için)
					gonderilmeyiBekleyen -= gonderilecek;
				}
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void veriEklendi()
		{
			//Eðer ACK bekleyen veri varsa..
			long ackBekleyen = benimSEQ - gidenACK; 
			if((ackBekleyen) > 0)
			{
				//Eðer en az bir segment büyüklüðünde veri gönderilmeyi
				//bekliyorsa bir segment yollamaya çalýþ
				long gonderilmeyiBekleyen = kayanPencere.getVeriBoyutu() - ackBekleyen;
				if(gonderilmeyiBekleyen >= (maxSegmentBoyutu * 0.8))
				{
					birSegmentYolla();
				}
				return;
			}
			//ACK bekleyen yok, bu durumda eklenen verileri karþý
			//tarafýn pencere boyutunun izin verdiði miktarda derhal yolla
			birSegmentYolla();
		}
	}
	
	/**
	 * Bu sýnýf belli bir süre sonra bir TCP segmentini tekrar yollamak
	 * için tasarlanmýþtýr. Timer nesnesi ile beraber kullanýlýr.
	 * TCP baðlantý açma olayýnca 1. paketin tekrar yollanmasýnda
	 * kullanýlýr.
	 * 
	 * @author Halil Ýbrahim KALKAN
	 */
	private class SegmentTekrarYollamaGorevi extends TimerTask
	{
		/** Tekrar yollanacak tcp paketi (segmenti) */
		private TCPPacket 						p;
		/** Bu paketin kaç defa tekrar yollandýðýný saklamak için */
		private int								tekrarDenemeSayisi = 0;
		
		/**
		 * Kurucu fonksyon.
		 * @param p tekrar yollanacak tcp paketi (segmenti)
		 */
		public SegmentTekrarYollamaGorevi(TCPPacket p)
		{
			this.p = p;
		}
		
		/**
		 * Paketi segmentYolla fonksyonu kullanarak gönderir.
		 * Eðer tekrar yollama sayýsý maximum deðere ulaþmýþsa yollama
		 * iþlemi kesilir ve pakete cevap gelmediði bildirilir.
		 */
		public void run() 
		{
			if(tekrarDenemeSayisi < TCPBaglantisi.maxTekrarYollamaSayisi)
			{
				segmentYolla(p);
				tekrarDenemeSayisi++;
			}
			else
			{
				cancel();
				cevapYok(p);
			}
		}
	}
}
