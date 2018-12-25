package tcpip.ip;

import jpcap.packet.*;
import tcpip.*;
import tcpip.arp.*;
import tcpip.tcp.*;
import tcpip.map.*;
import tcpip.udp.*;
import tcpip.nal.NetworkAccessLayer;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * IP protokol�n� uygulayan temel s�n�f.
 * 
 * @author Halil �brahim KALKAN 
 */
public class IP extends Thread
{
	// Private de�i�kenler ////////////////////////////////////////////////////
	/** NetworkAccessLayer nesnesine referans */
	private NetworkAccessLayer 					nal;
	/** TCP protokol�ne bir referans */
	private TCP									tcp;
	/** UDP protokol�ne bir referans */
	private UDP									udp;
	/** ARP protokol�ne bir referans */
	private ARP									arp;
	/** MAP protokol�ne bir referans */
	private MAP									map;
	/** ARP'den MAC talep etmek i�in bir istemci */
	private IPARPIstemcisi						ARPIstemci;
	/** ARP sorgusu g�nderilip cevap beklenen IP paketleri */
	private TreeMap<Long, IPPacket> 			MACBekleyenPaketler;
	/** ARP taleplerine bir ID vermek i�in kullan�lan de�i�ken */
	private volatile long						sonTalepID = 0;
	/** Son g�nderilen paketin IDENTIFICATION numaras� */
	private short								sonIdent = 0;
	/** Fragmantasyona u�ram�� paketler */
	private TreeMap<KayitBasligi, ParcaKaydi>	parcaliPaketler;
	/** Fragmantasyona u�ram�� bir paketin t�m par�alar�n�n gelmesi i�in maksimum bekleme s�resi */
	private static long							parcaliPaketZamanAsimi = 60000; //1 dakika
	/** Periyodik baz� g�revleri ger�ekle�tirmak i�in bir timer */
	private Timer								zamanlayici;
	/** varsay�lan TTL de�eri */
	private short								defaultTTL = 64;
	/** varsay�lan MTU de�eri (ethernet �er�evesinin max b�y�kl��� */
	private short								MTU = 1500; //ethernet:1500 byte
	/** network katman�ndan buraya gelen paketler i�in bir tampon olarak kuyruk */
	private LinkedBlockingQueue<IPPacket> 		IPKuyrugu;
	/** Bu makinan�n IP adresi */
	private Inet4Address						IPAdresi;
	/** Bu makinan�n ba�l� oldu�u LAN i�in alt a� maskesi */
	private Inet4Address						altAgMaskesi;
	/** Varsay�lan a� ge�idi (rooter'�n IP adresi) */
	private Inet4Address						varsayilanAgGecidi;
	/** protokol� y�netmek i�in bayrak */
	private volatile boolean					calisiyor = true;

	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	/**
	 * IP protokol�n� ger�ekle�tiren bir IP nesnesi olu�turur ve �al��maya
	 * haz�r hale getirir.
	 * @param nal NAL katman�na referans
	 */
	public IP(NetworkAccessLayer nal)
	{
		//Bu Thread'e bir isim ver
		setName("IP");
		//NetworkAccessLayer referans�n� sakla
		this.nal = nal;
		//IP datagramlar�n� (paketlerini) alabilmek i�in kuyru�u olu�tur
		IPKuyrugu = new LinkedBlockingQueue<IPPacket>();
		//MAC talep edilen paketleri ge�ici olarak saklayan nesne
		MACBekleyenPaketler = new TreeMap<Long, IPPacket>();
		//ARP talebi yapabilmek i�in ARP istemcisini olu�tur
		ARPIstemci = new IPARPIstemcisi(this);
		//Par�alanm�� paketleri saklayan yap�y� olu�tur
		parcaliPaketler = new TreeMap<KayitBasligi, ParcaKaydi>();
		//timer nesnesini olu�tur
		zamanlayici = new Timer(true);
	}
	
	// Public fonksyonlar /////////////////////////////////////////////////////
	/**
	 * Bu fonksyon bu katmana alt katmandan yeni bir paket g�ndermek i�in kullan�l�r.
	 * Gelen paket IPKuyrugu adl� kuyru�a aktar�l�r ve bu paketleri bekleyen
	 * Threadler notifyAll() metoduyla bilgilendirilir (�al��malar�na devam edilir).
	 * @param p gelen paket
	 */
	public void paketAl(IPPacket p)
	{
		synchronized(IPKuyrugu)
		{
			IPKuyrugu.add(p);
			IPKuyrugu.notifyAll();
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * �st katmanlardan a�a paket yollamak i�in bu fonksyon �a��r�l�r.
	 * Gelen paket MACBekleyenPaketler nesnesine at�l�r ve g�nderilecek
	 * IP adresinin MAC adresini bulmak i�in ARP protokol� kullan�l�r.
	 * @param p �st katmandan gelen paket. Genelde TCP ya da UDP paketidir.
	 * @param ipAdresi Bu paketin g�nderilece�i IP adresi
	 */
	public void paketYolla(IPPacket paket, Inet4Address ipAdresi)
	{
		//son talepID de�erini bir art�r ve bu g�nderim i�in kullan
		long talepID = ++sonTalepID;
		//paketin hedef IP adresi de�erini ayarla
		paket.dst_ip = ipAdresi;
		//Paketi ARP sorgusu cevab� bekleyen paketlerin listesine at
		synchronized(MACBekleyenPaketler)
		{
			MACBekleyenPaketler.put(talepID, paket);
		}
		//Paketin LAN �zerindeki bir d���me gidiyorsa bu d���m�n MAC adresini,
		//LAN d���nda bir d���me gidiyorsa A� ge�idi cihaz�n�n (rooter) MAC
		//adresini iste.
		if(yerelAgAdresi(ipAdresi))
			arp.MACIstegi(talepID, ipAdresi, ARPIstemci);
		else
			arp.MACIstegi(talepID, varsayilanAgGecidi, ARPIstemci);
		/* ARP protokol� i�lemlerini tamamlay�nca zaten MACal() ya da
		 * MACBulunamadi() fonksyonunu �a��raca�� i�in bu noktada
		 * ba�ka bi�ey yapmaya gerek yoktur.
		 */
	}
	//-------------------------------------------------------------------------
	/**
	 * ARP protokol� �zerinden talep edilen MAC adreslerini almak i�in
	 * fonksyon. IPARPIstemcisi bir MAC sorgusu cevab� ald���nda bilgileri
	 * bu fonksyona g�nderir. Bu fonksyon da MACBekleyenPaketler listesinden
	 * ilgili paketi bulup IP header ve Ethernet frame alanlar�n� ayarlay�p
	 * NAL katman�na g�nderir.
 	 * @param talepID Daha �nce yollanan talebin ID numaras�
	 * @param ipAdresi MAC adresi istenen IP adresi
	 * @param macAdresi Bulunan MAC adresi
	 * @param onBellek Bulunan mac adresi �n bellekten al�nd�ysa true aksi halde false
	 */
	public void MACal(long talepID, Inet4Address ipAdresi, MACAdresi macAdresi, boolean onBellek)
	{
		IPPacket paket = null;
		//Gelen MAC adresini bekleyen paketi kuyruktan (a�a�tan) al
		synchronized(MACBekleyenPaketler)
		{
			paket = MACBekleyenPaketler.remove(talepID);
		}
		//B�yle bir talep yoksa bi�ey yapmadan fonksyondan ��k
		if(paket==null)
			return;
		//Hedef IP adresini al
		Inet4Address hedefAdres = null;
		try 
		{
			hedefAdres = (Inet4Address)Inet4Address.getByAddress(paket.dst_ip.getAddress());
		} catch (Exception e) {
			// TODO: handle exception
		}
		if(paket==null)
			return;
		//Paketin IP protokol� ile ilgili olan k�s�mlar�n� ayarla 
		paket.setIPv4Parameter(
				0, // Priority
				false,false,false, // Delay, Troughput, Reliability
				0, // Type of Service
				false,false,false, // 0, DF, MF
				0, //offset
				++sonIdent, // Identification
				defaultTTL, // TTL
				paket.protocol, // Protocol (�st katmandan geliyor zaten)
				this.IPAdresi, // Source Address
				hedefAdres // Destination Address
				);
		//Ethernet �er�evesinin alanlar�n� ayarla
		EthernetPacket ether	= new EthernetPacket();
		ether.frametype			= EthernetPacket.ETHERTYPE_IP;
		ether.src_mac			= nal.getMAC().getAdres();
		ether.dst_mac			= macAdresi.getAdres();
		//IP paketini ethernet �er�evesinin i�erisine g�m
		paket.datalink = ether;
		//Paketi NAL �zerinden a�a yolla
		nal.paketAl(paket);
	}
	//-------------------------------------------------------------------------
	/**
	 * ARP protokol� �zerinden sorgulanan bir IP adresi i�in MAC adresi
	 * bulunamazsa bu durum bu fonksyon ile bildirilir.
	 * Bu fonksyon da bu MAC adresini bekleyen paketi MACBekleyenPaketler listesinden
	 * ��kar�r.
 	 * @param talepID Daha �nce yollanan talebin ID numaras�
	 * @param ipAdresi MAC adresi istenen IP adresi
	 */
	public void MACBulunamadi(long talepID, Inet4Address ipAdresi)
	{
		//B�yle bir talep yoksa bi�ey yapmadan fonksyondan ��k
		if(!MACBekleyenPaketler.containsKey(talepID))
			return;
		//Paketi MAC bekleyenler listesinden ��kar
		/* IPPacket paket = */ MACBekleyenPaketler.remove(talepID);		
		//�st katmana bu IP'nin bulunamad���n� bildir ve paketi �st katmana geri ver
		/*
		 *  UYGULANACAK
		 */
	}
	
	// Private fonksyonlar ////////////////////////////////////////////////////
	/**
	 * Alt katmandan gelen IP paketlerini IP adresine g�re de�erlendiren fonksyon.
	 * E�er paket bu bilgisayara gelmi�se paketDegerlendir() fonksyonu, de�ilse
	 * paketYonlendir() fonksyonu �a��r�l�r.
	 * @param p alt katmandan gelen IP paketi
	 */
	private void paketIsle(IPPacket p)
	{
		//gelen paket'in hedef IP adresi ile bu makinan�n IP adresini kar��la�t�r.
		if(p.dst_ip.equals(nal.getIPAdresi()))
		{
			paketDegerlendir(p); //paket bana gelmi�
		}
		else
		{
			paketYonlendir(p); //paketin y�nlendirilmesi gerekiyor
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu bilgisayara gelen paketleri de�erlendiren fonksyon.
	 * Fragmantasyon bayraklar�na g�re ya ustProtokolePaketYolla() fonksyonuna
	 * ya da parcalanmisPaketDegerlendir() fonksyonuna paketi devreder.
	 * @param p alt katmandan gelen IP paketi
	 */
	private void paketDegerlendir(IPPacket p)
	{
		//Fragmentasyon durumuna paketi g�re ilgili fonksyona yolla
		if((p.dont_frag) //DF=1 ise frag yok
				|| ((!p.more_frag) && p.offset==0)) //MF=0 ve offset=0 ise frag yok 
		{ //frag yok, paketi protokol numaras�na g�re ilgili protokole yolla
			ustProtokolePaketYolla(p);
		}
		else
		{ //frag var, o halde buna g�re de�erlendirme yap
			parcalanmisPaketDegerlendir(p);
		}
	}
	/**
	 * Gelen paketi ba�l�k yap�s�ndaki protokol numaras�na bakarak
	 * ilgili �st katman protokoline g�nderir.
	 * @param p framgmantasyon olmayan b�t�n bir paket
	 */
	private void ustProtokolePaketYolla(IPPacket p)
	{
		//Protokol'e g�re ilgili nesneye paketi g�nder
		switch(p.protocol)
		{
			case IPPacket.IPPROTO_TCP: // TCP paketi
				tcp.paketAl((TCPPacket)p);
				break;
			case IPPacket.IPPROTO_UDP: // UDP paketi
				udp.paketAl((UDPPacket)p);
				break;
			case MAP.protokolNumarasi: // MAP paketi
				if(map!=null)
					map.paketAl(p);
				break;
		}
	}
	/**
	 * Fragmentasyon y�ntemiyle par�alanm�� paketleri birle�tiren fonksyon.
	 * Datagram�n par�alar� saklan�r ve b�t�n par�alar tamamland���nda
	 * hepsi birle�tirilerek �st katmana g�nderilir.
	 * @param p framgmentasyona u�rayan datagram�n bir par�as�
	 */
	private void parcalanmisPaketDegerlendir(IPPacket p)
	{
		//Paketi par�alanm�� kay�tlar� saklayan parcaliPaketler
		//nesnesine atabilmek i�in bir Kay�t ba�l��� nesnesi olu�turuluyor
		KayitBasligi kb =
			new KayitBasligi((Inet4Address)p.src_ip, (short)p.ident);
		//T�m par�alar� tamamlanm��sa synchronized blo�u sonunda
		//bu de�i�kenin de�eri null olmaz
		ParcaKaydi tamamlanmisPaket = null;
		synchronized(parcaliPaketler)
		{
			//E�er bu IP'den gelen ayn� ident'li ba�ka par�alar varsa bu
			//par�ay� da aralar�na ilave et, yoksa yeni bir par�a kayd�
			//olu�tur ve ilk olarak bu par�ay� ekle
			if(parcaliPaketler.containsKey(kb))
			{
				//bu par�alar� saklayan nesneyi al
				ParcaKaydi pk = parcaliPaketler.get(kb);
				//yeni par�ay� ekle
				pk.parcaEkle(p);
				//Paket tamamlanm��sa bu paketi listeden ��kar.
				if(pk.paketHazir())
					tamamlanmisPaket = parcaliPaketler.remove(kb);
			}
			else
			{
				//Bir par�a kayd� olu�tur
				ParcaKaydi pk = new ParcaKaydi(kb);
				//�lk olarak bu par�ay� ekle
				pk.parcaEkle(p);
				//Belirli s�re i�erisinde paketin t�m par�alar� gelmezse
				//gelen par�alar�n� silen bir g�rev olu�tur
				pk.yokEtmeGorevi = new ParcaYoketmeGorevi(this,kb);
				//Bu g�revi zaman �izelgesine ekle
				zamanlayici.schedule(pk.yokEtmeGorevi, IP.parcaliPaketZamanAsimi);
			}
		}
		
		//E�er tamamlanan bir paket varsa bunu birle�tir ve �st katmana ilet
		if(tamamlanmisPaket!=null)
		{
			//daha �nce ayarlanm�� yok etme g�revini iptal et ��nk� paket tamamland� 
			tamamlanmisPaket.yokEtmeGorevi.cancel();
			ustProtokolePaketYolla(tamamlanmisPaket.birlestirilmisPaketiOlustur());
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Ba�ka bilgisayara giden paketleri y�nlendirmek i�in fonksyon. Daha �ok
	 * router'larda �al���r. ��eri�i bo�
	 * @param p alt katmandan gelen IP paketi
	 */
	private void paketYonlendir(IPPacket p)
	{
		/*
		 * PAKET Y�NLEND�RME �LE ALAKALI KISIM. BU PROJEDE YAPILMAYACAK.
		 * 
		 */
	}
	//-------------------------------------------------------------------------
	/**
	 * Bir IP adresinin LAN �zerindeki bir makinan�n IP adresi
	 * olup olmad���n� s�yler.
	 * @return LAN �zerinde bir IP ise true, aksi halde false
	 */
	private boolean yerelAgAdresi(Inet4Address ipAdresi)
	{
		//IP adreslerini byte dizisine �evir
		byte[] benimIP = this.IPAdresi.getAddress();
		byte[] hedefIP = ipAdresi.getAddress();
		byte[] agMaskesi = altAgMaskesi.getAddress();
		//a� maskesinde 255 olan alanlarda benim ip adresim ile
		//hedef ip adresinin de�erleri farkl�ysa hedef IP LAN'da de�ildir.
		for(int i=0;i<4;i++)
		{
			if((agMaskesi[i]==((byte)255)) && 
				(benimIP[i]!=hedefIP[i]))
					return false;
		}
		return true;
	}
	// SET fonksyonlar� ///////////////////////////////////////////////////////
	/**
	 * ARP protokol�ne olan referans� ayarlar.
	 * @param arp ARP s�n�f�ndan bir nesne
	 */
	public void setARP(ARP arp)
	{
		this.arp = arp;
	}
	//-------------------------------------------------------------------------
	/**
	 * TCP protokol�ne olan referans� ayarlar.
	 * @param tcp TCP s�n�f�ndan bir nesne 
	 */
	public void setTCP(TCP tcp)
	{
		this.tcp = tcp;
	}
	//-------------------------------------------------------------------------
	/**
	 * UDP protokol�ne olan referans� ayarlar.
	 * @param udp UDP s�n�f�ndan bir nesne 
	 */
	public void setUDP(UDP udp)
	{
		this.udp = udp;
	}
	/**
	 * MAP protokol�ne referans� ayarlar.
	 * @param map MAP s�n�f�ndan bir nesne
	 */
	public void setMAP(MAP map)
	{
		this.map = map;
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu makinan�n IP adresini de�i�tirmek i�in.
	 * @param iPAdresi IP adresi
	 */
	public void setIPAdresi(Inet4Address iPAdresi) 
	{
		IPAdresi = iPAdresi;
	}
	//-------------------------------------------------------------------------
	/**
	 * Yerel a� i�in alt a� maskesi de�erini de�i�tirir.
	 * @param altAgMaskesi alt a� maskesi
	 */
	public void setAltAgMaskesi(Inet4Address altAgMaskesi)
	{
		this.altAgMaskesi = altAgMaskesi;
	}
	//-------------------------------------------------------------------------
	/**
	 * Varsay�lan a� ge�idini de�i�tirmek i�indir.
	 * @param varsayilanAgGecidi varsay�lan a� ge�idi
	 */
	public void setVarsayilanAgGecidi(Inet4Address varsayilanAgGecidi)
	{
		this.varsayilanAgGecidi = varsayilanAgGecidi;
	}

	// Get fonksyonlar� ///////////////////////////////////////////////////////
	/**
	 * Bu makinan�n IP adresini d�nderir.
	 * @return Bu makinan�n IP adresi
	 */
	public Inet4Address getIPAdresi() 
	{
		return IPAdresi;
	}
	//-------------------------------------------------------------------------
	/**
	 * Yerel a��n a� maskesini d�nderir.
	 * @return Alt a� maskesi 
	 */
	public Inet4Address getAltAgMaskesi()
	{
		return altAgMaskesi;
	}
	//-------------------------------------------------------------------------
	/**
	 * Varsay�lan a� ge�idini d�nderir.
	 * @return varsay�lan a� ge�idi
	 */
	public Inet4Address getVarsayilanAgGecidi()
	{
		return varsayilanAgGecidi;
	}
	//-------------------------------------------------------------------------
	public short getMTU()
	{
		return MTU;
	}

	// Thread fonksyonlar� ////////////////////////////////////////////////////
	/**
	 * Thread'in giri� noktas�.
	 * Bu fonksyon alt katmandan (NAL) gelen paketleri saklamak i�in kullan�lan
	 * IPKuyrugu adl� kuyru�a bakar. E�er kuyrukta hi� paket yoksa yeni bir paket
	 * gelene kadar bekleme konumunda kal�r. Yeni bir paket geldi�inde paketi
	 * paketIsle fonksyonuna g�nderir ve tekrar bekleme konumuna ge�er.
	 */
	public void run()
	{
		IPPacket yeniPaket;
		while(calisiyor)
		{
			//alt katmandan gelen paketlerin al�nmas�
			yeniPaket = null;
			synchronized(IPKuyrugu)
			{
				//kuyrukta paket var m�?
				if(IPKuyrugu.size()<=0)
				{
					// yeni paket gelene kadar bekle
					try { IPKuyrugu.wait(); }
					catch (InterruptedException e) { /* bo� */ }
				}
				else
				{
					//paketi kuyruktan al
					yeniPaket = IPKuyrugu.poll();
				}
			}
			//paket al�nd�ysa bu paketi i�leyen fonksyona yolla
			if(yeniPaket!=null)
			{
				paketIsle(yeniPaket);
			}
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu Thread'in �al��mas�n� durdurmak i�in.
	 * Bu fonksyon �a��r�ld�ktan sonra art�k IP paketi dinleme ve dolay�s�yla
	 * de�erlendirme i�ine son verilir.
	 */
	public void durdur()
	{
		calisiyor = false;
		synchronized(IPKuyrugu)
		{
			IPKuyrugu.notifyAll();
		}
	}
	
	// GE���� KOD /////////////////////////////////////////////////////////////
	public void opsiyonluYolla()
	{
		IPPacket paket = new IPPacket();
		InetAddress ipAdresi = null;
		try
		{
			ipAdresi = Inet4Address.getByAddress(new byte[]{(byte)192,(byte)168,(byte)1,(byte)13});
		}
		catch(Exception e)
		{
			
		}
		//paket.option = "ABCD".getBytes();
		
		//Paketin IP protokol� ile ilgili olan k�s�mlar�n� ayarla 
		paket.setIPv4Parameter(0, // Priority
				false,false,false, // Delay, Troughput, Reliability
				0, // Type of Service
				false,false,false, // 0, DF, MF
				0, //offset
				++sonIdent, // Identification
				defaultTTL, // TTL
				0x0800, // Protocol (�st katmandan geliyor zaten)
				this.IPAdresi, // Source Address
				ipAdresi); // Destination Address
		paket.data = "DENEME".getBytes();
		//Ethernet �er�evesinin alanlar�n� ayarla
		EthernetPacket ether	= new EthernetPacket();
		ether.frametype			= EthernetPacket.ETHERTYPE_IP;
		ether.src_mac			= nal.getMAC().getAdres();
		ether.dst_mac			= nal.getMAC().getAdres();
		//IP paketini ethernet �er�evesinin i�erisine g�m
		paket.datalink = ether;
		//Paketi NAL �zerinden a�a yolla
		nal.paketAl(paket);
	}
	// GE���� KOD /////////////////////////////////////////////////////////////

	// Alt s�n�flar ///////////////////////////////////////////////////////////
	/**
	 * Belirli bir s�re i�inde t�m par�alar� tamamlanmayan IP paketlerini
	 * iptal etmek i�in bir g�revi temsil eden s�n�f.
	 * 
	 * @author Halil �brahim Kalkan
	 */
	private class ParcaYoketmeGorevi extends TimerTask 
	{
		/** IP protokol�n� uygulayan nesneye referans */
		private IP									ip;
		/** Kontrol edilecek kay�t� bulmak i�in ba�l�k bilgisi */
		private KayitBasligi						kb;
		
		// Kurucu fonksyonlar /////////////////////////////////////////////////
		/**
		 * Yeni bir ParcaYoketmeGorevi nesnesi olu�turur.
		 * @param ip IP protokol�n� uygulayan nesneye referans
		 * @param kb Kontrol edilecek kay�t� bulmak i�in ba�l�k bilgisi
		 */
		public ParcaYoketmeGorevi(IP ip, KayitBasligi kb)
		{
			this.ip = ip;
			this.kb = kb;
		}

		// Thread fonksyonlar� ////////////////////////////////////////////////
		/**
		 * Bu fonksyon �al��t��� zaman kb ile ba�l�k bilgisi saklanan kayd�
		 * listeden ��kar�r. Belirli bir s�re i�erisinde t�m par�alar� tamamlanmayan
		 * paketleri iptal etmek i�indir.
		 */
		public void run() 
		{
			synchronized(ip.parcaliPaketler)
			{
				if(ip.parcaliPaketler.containsKey(kb))
				{
					ip.parcaliPaketler.remove(kb);
				}
			}
		}
	}
}