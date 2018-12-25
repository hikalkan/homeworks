package tcpip.tcp;

import java.io.*;
import java.net.*;

import jpcap.packet.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * �ki bilgisayar aras�nda kurulan bir TCP ba�lant�s�n� temsil eder.
 * 
 * @author Halil �brahim KALKAN
 */
public class TCPBaglantisi implements TCPDinleyici
{
	/** FIN bayra�� */
	private static final byte					FIN_bayragi = 0x01;
	/** SYN bayra�� */
	private static final byte					SYN_bayragi = 0x02;
	/** RST bayra�� */
	private static final byte					RST_bayragi = 0x04;
	/** PSH bayra�� */
	private static final byte					PSH_bayragi = 0x08;
	/** ACK bayra�� */
	private static final byte					ACK_bayragi = 0x10;
	/** URG bayra�� */
	private static final byte					URG_bayragi = 0x20;
	
	/** Ba�lant� a�ma i�leminde ba�lant�n�n ger�ekle�memesi durumunda ne kadar zaman sonra vazge�ilece�i */
	private static final int					baglantiTimeoutSuresi = 30000; //30 saniye
	/** Varsay�lan olarak kullan�lacak benim pencere boyutum */
	private static final int					baslangicPencereBoyutu = 5000; //5000 byte
	/**
	 * Kar�� tarafa veri yollarken kullan�lacak kayan pencere
	 * mekanizmas�ndaki toplam tampon b�y�kl���. 
	 */
	private static final int					hedefTamponBoyutu = 10000;
	/**
	 * Bir TCP segmentinde g�nderilecek maksimum veri boyutunu tan�mlar.
	 * Daha fazla veri g�nderilecekse birden �ok segment kullan�l�r.
	 * (Varsay�lan de�er)
	 */
	private static final int					maksimumSegmentBoyutu = 1460;
	/** Bu ba�lant� i�in kullan�lan maksimum segment boyutunu saklar */
	private volatile int						maxSegmentBoyutu = maksimumSegmentBoyutu;
	
	/** TCP protokol�n� uygulayan nesneye referans */
	private TCP									tcp;
	
	/** TCP ba�lant�s�n�n durumunu temsil eder */
	private volatile TCPDurumlari				durum;
	
	/** Ba�lant� kurulan hedef cihaz�n IP adresi */
	private Inet4Address						hedefIPAdresi;
	/** Hedef cihaz�n port numaras� */
	private int									hedefPortNo;
	/** Bu bilgisayarda bu ba�lant� i�in kullan�lan port numaras� */
	private int 								benimPortNo;
	
	/** Kar�� cihaza ACK almadan g�nderilebilecek maximum byte say�s� */
	private volatile int						hedefPencereBoyutu = 5000;
	/**
	 * Benim SEQUENCE numaram.
	 * En son g�nderdi�im byte'�n numaras�n� saklar.
	 */
	private volatile long						benimSEQ;
	/**
	 * Benim yollad���m veriler i�in kar�� taraftan en son gelen
	 * ACK numaras�. Ba�ka bir deyi�le kar�� taraftan beklenen ilk byte'�n
	 * s�ra numaras�. 
	 */
	private volatile long						gidenACK;
	/**
	 * Kar�� taraf�n SEQUENCE numaras�.
	 * Kar�� taraftan al�nan SEQ numaras� ile beraber al�nan byte say�s�n�n
	 * toplam� ile g�ncellenir. ACK yollanaca�� zaman bir fazlas� ile yollan�r.
	 */
	private volatile long						hedefSEQ;
	/** Ortalama olarak bir TCP segmenti i�in ACK al�nma s�resi */
	private volatile long						ortalamaACKSuresi = 9000; //9 saniye
	/** Ba�lant� a�ma olay�nda ilk mesaj�n maksimum ka� defa tekrar yollanaca�� */
	private static int							maxTekrarYollamaSayisi = 7;
	/** Kayan pencerede kullan�lacak pencere boyutu */
	private volatile int 						benimPencereBoyutum = baslangicPencereBoyutu;
	/** Kayan pencere mekanizmas�n� uygulayan nesne */
	private KayanPencere						kayanPencere;
	
	/** Uygulama katman�n�n kar�� cihaza byte yollayabilmesi i�in */
	private TCPOutputStream						gidenByteOutputStream;
	/** Kayan pencereden verileri al�p a�a yollamak i�in nesne */
	private VeriYollayici						veriYollayici;
	
	/** Baz� g�revleri zaman �izelgesine oturtmak i�in kullan�lan nesne */
	private Timer								zamanlayici;
	/**
	 * Ba�lant� iste�ini max say�ya ula��ncaya dek periyodik olarak
	 * tekrarlamak i�in kullan�lan nesne. 
	 */
	private SegmentTekrarYollamaGorevi			baglantiTekrarDenemesi = null;
	/**
	 * 3 yollu els�k��ma i�leminde 1. g�nderilen mesaja kar��dan cevap
	 * gelinceye dek threat'in beklemesini sa�layan nesne
	 */
	private CountDownLatch						cevapMesajiBekleticisi;
	/** Kar�� tarafa ba�ar�yla ba�lan�l�p ba�lan�lmad���n� saklar */
	private boolean								bagliMi = false;
	
	/**
	 * Kurucu fonksyon
	 *
	 */
	private TCPBaglantisi()
	{
		//ba�lang�� durumu: kapal�
		durum = TCPDurumlari.CLOSED;
		//gerekli nesneleri olu�tur
		zamanlayici = new Timer(true);
		cevapMesajiBekleticisi = new CountDownLatch(1);
		veriYollayici = new VeriYollayici();
		kayanPencere = new KayanPencere(TCPBaglantisi.hedefTamponBoyutu, veriYollayici);
		gidenByteOutputStream = new MyTCPOutputStream();
	}
	
	/**
	 * Verilen arg�manlar do�rultusunda bir TCPBaglantisi nesnesi
	 * olu�turur.
	 * @param IPAdresi Hedef bilgisayar�n IP adresi
	 * @param PortNo Hedef bilgisayar�n port numaras�
	 * @return E�er olu�turulabildiyse bir TCPBaglantisi nesnesi, de�ilse null
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
	 * Verilen arg�manlar do�rultusunda bir TCPBaglantisi nesnesi
	 * olu�turur.
	 * @param IPAdresi Hedef bilgisayar�n IP adresi
	 * @param PortNo Hedef bilgisayar�n port numaras�
	 * @param benimPortNo Bu bilgisayarda bu ba�lant� i�in kullan�lacak port numaras�
	 * @return E�er olu�turulabildiyse bir TCPBaglantisi nesnesi, de�ilse null
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
	 * TCPBaglantisi nesnesini �al��ma i�in haz�r duruma getirir.
	 * @param baglanti bir TCPBaglantisi nesnesi
	 * @param IPAdresi Hedef bilgisayar�n IP adresi
	 * @param PortNo Hedef bilgisayar�n port numaras�
	 * @param benimPortNo Bu bilgisayarda bu ba�lant� i�in kullan�lacak port numaras�
	 * @return E�er sorun yoksa baglanti adl� nesne, de�ilse null
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
	 * Ba�lant� sa�lanmas�nda ilk mesaj�n yollanmas�n� ger�ekle�tiren
	 * motod. Bu metod CLOSED durumunda ba�lant� a�ma iste�ini
	 * ger�ekle�tirmek i�in kullan�l�r ve hedefe syn bilgisini yollay�p
	 * SYN_SENT durumuna ge�er.
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
		//Maksimum Segment b�y�kl��� opsiyonu
		p.option = new byte[] {
				0x02, 0x04, 0x05, (byte)0xB4, 0x01, 0x01, 0x04, 0x02
				};
		segmentYolla(p);
		return p;
	}
	/**
	 * SYN_SENT durumundayken kar��dan bir syn mesaj� geldi�inde
	 * �a��r�lan fonksyon. Kar�� tarafa syn ve ack bilgilerini yollar
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
	 * Herhangi bir durumdayken kar��daki cihaza ack mesaj� yollamak
	 * i�in kullan�lan fonksyon.
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
	 * Bir TCP segmentinin ba�l�k ve veri k�sm�ndaki bilgileri de�erlendiren
	 * fonksyon
	 * @param p verileri ta��yan TCP segmenti
	 */
	private synchronized void segmenttenVeriAl(TCPPacket p)
	{
		//Paket ba�l���ndan laz�m olan bilgileri al
		hedefSEQ = p.sequence;
		if(p.ack) //ACK bayra�� i�aretliyse ACK de�erini al
		{
			gidenACK = p.ack_num;
		}
		hedefPencereBoyutu = p.window;
		//Paket i�erisindeki verileri al
		/*
		 * BURADA GELEN PAKETTEK� VER�LER� ALIP UYGULAMA KATMANINA
		 * YOLLAYACAK VE hedefSEQ DE�ER�N� G�NCELLEYECEK MANTIK OLACAK! 
		 */
	}
	
	/**
	 * {@inheritDoc}
	 */
	public synchronized void paketAl(TCPPacket yeniPaket)
	{
		//Hedef pencere boyutu bilgisini g�ncelle
		hedefPencereBoyutu = yeniPaket.window;
		//Durum mankinas�na g�re durum ge�i�lerini yap
		if(durum == TCPDurumlari.ESTABLISHED)
		{
			/*                   fin / ack
			 * ESTABLISHED ----------------------> CLOSE_WAIT
			 */
			if(yeniPaket.fin)
			{
				
			}
			/*
			 * Veri al�m durumu. E�er buras� �al���rsa kar�� taraf veri
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
				//durum ge�i�ini sa�la
				durum = TCPDurumlari.SYN_RECEIVED;
				segmenttenVeriAl(yeniPaket);
				syn_ack_yolla();
			}
			/*                syn+ack / ack
			 * SYN_SENT -------------------------> ESTABLISHED  
			 **/ 
			else if(yeniPaket.syn && yeniPaket.ack)
			{
				//durum ge�i�ini sa�la
				durum = TCPDurumlari.ESTABLISHED;
				//kar�� taraf�n yollad��� bilgileri al
				segmenttenVeriAl(yeniPaket);
				//ACK mesaj� yolla
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
				//kar�� taraf�n yollad��� bilgileri al
				segmenttenVeriAl(yeniPaket);
				//durum ge�i�ini sa�la
				durum = TCPDurumlari.ESTABLISHED;
				baglanti_saglandi();
			}
		}
	}

	/**
	 * Ba�lant�n�n ger�ekle�tirilmesi durumunda ac() fonksyonunu
	 * �a��ran threat'in devam etmesini sa�lar.
	 */
	private void baglanti_saglandi()
	{
		//�lk mesaj� tekrar yollamak i�in kurulan mekanizmay� durdur
		if(baglantiTekrarDenemesi != null)
		{
			baglantiTekrarDenemesi.cancel();
			baglantiTekrarDenemesi = null;
		}
		//ac fonksyonunda await ile beklemede olan
		//threat'i uyand�r ve ba�lant�n�n a��ld���n� bildir.
		this.cevapMesajiBekleticisi.countDown();
	}

	/**
	 * Kar�� sunucuya ilgili mesajlar� g�nderip alarak ba�lant�y� a�ar.
	 * Ba�lant� ger�ekle�tirilirse true aksi halde false d�nderir.
	 * (AKT�F A�MA)
	 * (3 yollu el s�k��ma ile)
	 * @return ba�lant�n�n ba�ar� durumu
	 */
	private boolean ac()
	{
		//ba�lang�� SEQ numaram� belirle
		benimSEQ = baslangicSEQ();
		/*             active open / syn
		 * CLOSED --------------------------------> SYN_SENT
		 */
		//durum ge�i�ini sa�la
		durum = TCPDurumlari.SYN_SENT;
		TCPPacket p = syn_yolla();
		//Cevap al�n�ncaya kadar mesaj� tekrar tekrar yollamak
		//i�in bir zamanlay�c� mekanizmas� olu�tur		
		baglantiTekrarDenemesi = new SegmentTekrarYollamaGorevi(p);
		zamanlayici.schedule(
				baglantiTekrarDenemesi,
				ortalamaACKSuresi,
				ortalamaACKSuresi
				);
				
		//cevap mesaj� gelene kadar ya da timeout s�resi dolana kadar bekler.
		try { cevapMesajiBekleticisi.await(TCPBaglantisi.baglantiTimeoutSuresi, TimeUnit.MILLISECONDS); } catch (Exception e) { }
		//timeout durumunda durumu tekrar CLOSED yap
		if(durum != TCPDurumlari.ESTABLISHED)
		{
			durum = TCPDurumlari.CLOSED;
			return false;
		}
		else
		{
			//durum ESTABLISHED, true d�nder
			return true;
		}
	}

	/**
	 * Ba�lang�� i�in bir sekans numaras� olu�turur.
	 * @return bir SEQ numaras�
	 */
	private long baslangicSEQ()
	{
		
		return 1245;
	}
	
	/**
	 * Arg�man olarak verilen paketi tcp mod�l� �st�nden a�a yollar.
	 * @param p g�nderilecek TCP segmenti
	 */
	private synchronized void segmentYolla(TCPPacket p)
	{
		tcp.paketYolla(IPPaketineCevir(p), hedefIPAdresi);
	}
	
	/**
	 * Verilen TCPPacket nesnesini bir IPPacket nesnesine �evirir.
	 * Bunu yaparken TCP ba�l�k ve option alanlar�n� IP paketinde
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
			//TCP ba�l���n� yaz
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
			//Bayraklar� yaz
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
			//TCP protokol� oldu�unu belirt
			ipPaketi.protocol = IPPacket.IPPROTO_TCP;
			//Checksum de�erini hesapla
			checksumHesapla(ipPaketi);
			//Olu�turulan peketi d�nder
			return ipPaketi;
		}
		catch (Exception e) 
		{
			return null;
		}
	}
	
	/**
	 * Verilen segment i�in TCP header checksum de�erini hesaplay�p
	 * segment i�erisinde uygun alana yazar.
	 * @param p Checksum hesaplanacak segment
	 */
	private synchronized void checksumHesapla(IPPacket p)
	{
		byte[] cb = null;
		/* Checksum de�erini hesaplamak i�in gerekli olan veriler
		 * bir byte dizisi haline getiriliyor
		 */
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try 
		{
			//Pseudo Header'� yaz
			dos.write(tcp.getIPAdresi().getAddress());
			dos.write(hedefIPAdresi.getAddress());
			dos.writeByte(0);
			dos.writeByte(IPPacket.IPPROTO_TCP);
			dos.writeShort(p.data.length);
			//TCP ba�l��� + TCP data k�sm�n� yaz
			dos.write(p.data);
			//Gerekiyorsa 1 byte 0 ekle 16 bitin kat�na tamamlamak i�in
			if((p.data.length % 2) == 1)
			{
				dos.writeByte(0);
				System.out.print("--> 1 byte 0 eklendi.");
			}
			//olu�turulan byte dizisini al
			cb = baos.toByteArray();
			dos.close();
		}
		catch (Exception e) 
		{
			return;
		}
		/* Elde edilen byte dizindeki her 16 bitlik say�y�
		 * al�p topla ve toplam�n 1'e t�mleyenini alarak checksum'� bul
		 */
		ByteArrayInputStream bais = new ByteArrayInputStream(cb);
		DataInputStream dis = new DataInputStream(bais);
		try 
		{
			int toplam = 0;
			//Ka� adet 16 bitlik say� oldu�u bulunuyor
			int k16 = cb.length / 2;
			//Her 16 bitlik say� toplama ekleniyor
			for(int i=0; i<k16; i++)
			{
				//toplama ekle
				toplam += dis.readUnsignedShort();
				//toplam &= 0x0000FFFF;
			}
			//toplam�n t�mleyenini al
			toplam = (toplam & 0xffff) + (toplam >> 16);
			short s = (short)(~(short)((toplam >> 16) + (toplam & 0xffff)));
			//TCP ba�l���na checksum de�erini yaz
			checkSumYaz(p, s);
		}
		catch (Exception e) 
		{
			return;
		}
	}
	
	/**
	 * Verilen segmente yine verilen checksum de�erini uygun konumda yerle�tirir.
	 * @param p TCP segmenti
	 * @param checksum bu segmente ait checksum de�eri
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
			//checksum alan� 16. byte'dan itibaren 2 byte olarak yaz�l�yor
			System.arraycopy(b, 0, p.data, 16, 2);
		}
		catch (Exception e) 
		{
			//...
		}
	}
	
	/**
	 * Kar�� tarafa bir byte dizisini yollamak i�in kullan�l�r.
	 * @param veri yollanacak byte dizisi
	 */
	private synchronized void veriYolla(byte[] veri)
	{
		
	}

	/**
	 * Cevap i�in bekleyen threat'i uyand�r ve ba�lant�n�n ba�ar�s�z oldu�unu
	 * bildirir.
	 * @param p cevap gelmeyen paket
	 */
	private synchronized void cevapYok(TCPPacket p)
	{
		bagliMi = false;
		cevapMesajiBekleticisi.countDown();
	}
	
	/**
	 * Kar�� tarafa fin bayra��n� 1 olarak yollayarak ba�lant�y� 
	 * sonland�rmak i�in kullan�l�r
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
	 * Ba�lant�y� kapat�r ve kar�� cihazla ileti�imi sonland�r�r.
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
	 * Kar�� cihaza veri yollamak i�in gerekli
	 * fonksyonlar� sa�layan bir nesne d�nderir.
	 * 
	 * @return TCPOutputStream'i uygulayan nesne
	 */
	public TCPOutputStream getOutputStream() 
	{
		return gidenByteOutputStream;
	}
	
	/**
	 * Uygulama katman�ndan ba�lant�daki hedefe veri yollamak i�in
	 * kullan�lan ve TCPOutputStream aray�z�n� uygulayan s�n�f.
	 * 
	 * @author Halil �brahim KALKAN
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
	 * Uygulama katman�ndan gelen verilerin uygun veri ak�� mant���
	 * kullan�lacak hedef cihaza TCP segmentleri halinde
	 * yollanabilmesi i�in geli�tirilen s�n�f.
	 * 
	 * @author Halil �brahim KALKAN
	 */
	private class VeriYollayici extends Thread implements VeriDinleyicisi
	{
		/** Thread'in �al��ma durumunu g�steren bayrak */
		private volatile boolean 				calisiyor = true;
	
		/**
		 * Kurucu fonksyon
		 */
		public VeriYollayici()
		{
			
		}

		/**
		 * Thread'in giri� noktas�.
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
		 * Kayan pencerede g�nderilmeyi bekleyen verilerden
		 * hedef pencere boyutunun izin verdi�i kadar�n� g�nderir.
		 * E�er birden �ok segment olacak kadar veri varsa birden
		 * �ok segment yollayabilir.
		 */
		public synchronized void birSegmentYolla()
		{
			synchronized(kayanPencere)
			{
				//hedefin tamponunda g�nderilen verileri alabilecek yer
				//oldu�u s�rece kayan pencerede bekleyen verileri
				//segmentler halinde g�nder.
				int hedefTampon = hedefPencereBoyutu;
				//g�nderilen ancak ACK al�nmayan byte say�s�n� hesapla
				long ackBekleyen = benimSEQ - gidenACK;
				//tamponda olan ancak hen�z g�nderilmeyen byte say�s�n� hesapla
				long gonderilmeyiBekleyen = kayanPencere.getVeriBoyutu() - ackBekleyen;
				while((hedefTampon > 0) && (gonderilmeyiBekleyen > 0))
				{
					//bu segment ile g�nderilecek byte say�s� hesaplan�yor
					long gonderilecek = gonderilmeyiBekleyen;
					//E�er bu say� kar�� taraf�n alabilece�inden fazlaysa
					//alabilece�i kadar�n� g�nderiyoruz.
					if(gonderilecek > hedefTampon)
					{
						gonderilecek = hedefPencereBoyutu;						
					}
					//E�er bu say� maksimum segment boyutundan fazlaysa
					//bir segmentte g�nderilebilecek maksimum byte
					//say�s� kadar byte yolluyoruz.
					if(gonderilecek > maxSegmentBoyutu)
					{
						gonderilecek = maxSegmentBoyutu;
					}
					byte b[] = new byte[(int)gonderilecek];
					kayanPencere.al((int)ackBekleyen, b);
					//g�nderilecek veri segmentini olu�tur
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
					//SEQ de�erimi ayarla
					benimSEQ += gonderilecek;
					//Hedef tampon de�i�kenini ayarla (d�ng�n�n sonlanmas� i�in)
					hedefTampon -= gonderilecek;
					//gonderilmeyiBekleyen de�i�kenini ayarla (d�ng�n�n sonlanmas� i�in)
					gonderilmeyiBekleyen -= gonderilecek;
				}
			}
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void veriEklendi()
		{
			//E�er ACK bekleyen veri varsa..
			long ackBekleyen = benimSEQ - gidenACK; 
			if((ackBekleyen) > 0)
			{
				//E�er en az bir segment b�y�kl���nde veri g�nderilmeyi
				//bekliyorsa bir segment yollamaya �al��
				long gonderilmeyiBekleyen = kayanPencere.getVeriBoyutu() - ackBekleyen;
				if(gonderilmeyiBekleyen >= (maxSegmentBoyutu * 0.8))
				{
					birSegmentYolla();
				}
				return;
			}
			//ACK bekleyen yok, bu durumda eklenen verileri kar��
			//taraf�n pencere boyutunun izin verdi�i miktarda derhal yolla
			birSegmentYolla();
		}
	}
	
	/**
	 * Bu s�n�f belli bir s�re sonra bir TCP segmentini tekrar yollamak
	 * i�in tasarlanm��t�r. Timer nesnesi ile beraber kullan�l�r.
	 * TCP ba�lant� a�ma olay�nca 1. paketin tekrar yollanmas�nda
	 * kullan�l�r.
	 * 
	 * @author Halil �brahim KALKAN
	 */
	private class SegmentTekrarYollamaGorevi extends TimerTask
	{
		/** Tekrar yollanacak tcp paketi (segmenti) */
		private TCPPacket 						p;
		/** Bu paketin ka� defa tekrar yolland���n� saklamak i�in */
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
		 * Paketi segmentYolla fonksyonu kullanarak g�nderir.
		 * E�er tekrar yollama say�s� maximum de�ere ula�m��sa yollama
		 * i�lemi kesilir ve pakete cevap gelmedi�i bildirilir.
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
