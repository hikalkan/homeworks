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
 * IP protokolünü uygulayan temel sýnýf.
 * 
 * @author Halil Ýbrahim KALKAN 
 */
public class IP extends Thread
{
	// Private deðiþkenler ////////////////////////////////////////////////////
	/** NetworkAccessLayer nesnesine referans */
	private NetworkAccessLayer 					nal;
	/** TCP protokolüne bir referans */
	private TCP									tcp;
	/** UDP protokolüne bir referans */
	private UDP									udp;
	/** ARP protokolüne bir referans */
	private ARP									arp;
	/** MAP protokolüne bir referans */
	private MAP									map;
	/** ARP'den MAC talep etmek için bir istemci */
	private IPARPIstemcisi						ARPIstemci;
	/** ARP sorgusu gönderilip cevap beklenen IP paketleri */
	private TreeMap<Long, IPPacket> 			MACBekleyenPaketler;
	/** ARP taleplerine bir ID vermek için kullanýlan deðiþken */
	private volatile long						sonTalepID = 0;
	/** Son gönderilen paketin IDENTIFICATION numarasý */
	private short								sonIdent = 0;
	/** Fragmantasyona uðramýþ paketler */
	private TreeMap<KayitBasligi, ParcaKaydi>	parcaliPaketler;
	/** Fragmantasyona uðramýþ bir paketin tüm parçalarýnýn gelmesi için maksimum bekleme süresi */
	private static long							parcaliPaketZamanAsimi = 60000; //1 dakika
	/** Periyodik bazý görevleri gerçekleþtirmak için bir timer */
	private Timer								zamanlayici;
	/** varsayýlan TTL deðeri */
	private short								defaultTTL = 64;
	/** varsayýlan MTU deðeri (ethernet çerçevesinin max büyüklüðü */
	private short								MTU = 1500; //ethernet:1500 byte
	/** network katmanýndan buraya gelen paketler için bir tampon olarak kuyruk */
	private LinkedBlockingQueue<IPPacket> 		IPKuyrugu;
	/** Bu makinanýn IP adresi */
	private Inet4Address						IPAdresi;
	/** Bu makinanýn baðlý olduðu LAN için alt að maskesi */
	private Inet4Address						altAgMaskesi;
	/** Varsayýlan að geçidi (rooter'ýn IP adresi) */
	private Inet4Address						varsayilanAgGecidi;
	/** protokolü yönetmek için bayrak */
	private volatile boolean					calisiyor = true;

	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	/**
	 * IP protokolünü gerçekleþtiren bir IP nesnesi oluþturur ve çalýþmaya
	 * hazýr hale getirir.
	 * @param nal NAL katmanýna referans
	 */
	public IP(NetworkAccessLayer nal)
	{
		//Bu Thread'e bir isim ver
		setName("IP");
		//NetworkAccessLayer referansýný sakla
		this.nal = nal;
		//IP datagramlarýný (paketlerini) alabilmek için kuyruðu oluþtur
		IPKuyrugu = new LinkedBlockingQueue<IPPacket>();
		//MAC talep edilen paketleri geçici olarak saklayan nesne
		MACBekleyenPaketler = new TreeMap<Long, IPPacket>();
		//ARP talebi yapabilmek için ARP istemcisini oluþtur
		ARPIstemci = new IPARPIstemcisi(this);
		//Parçalanmýþ paketleri saklayan yapýyý oluþtur
		parcaliPaketler = new TreeMap<KayitBasligi, ParcaKaydi>();
		//timer nesnesini oluþtur
		zamanlayici = new Timer(true);
	}
	
	// Public fonksyonlar /////////////////////////////////////////////////////
	/**
	 * Bu fonksyon bu katmana alt katmandan yeni bir paket göndermek için kullanýlýr.
	 * Gelen paket IPKuyrugu adlý kuyruða aktarýlýr ve bu paketleri bekleyen
	 * Threadler notifyAll() metoduyla bilgilendirilir (çalýþmalarýna devam edilir).
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
	 * Üst katmanlardan aða paket yollamak için bu fonksyon çaðýrýlýr.
	 * Gelen paket MACBekleyenPaketler nesnesine atýlýr ve gönderilecek
	 * IP adresinin MAC adresini bulmak için ARP protokolü kullanýlýr.
	 * @param p üst katmandan gelen paket. Genelde TCP ya da UDP paketidir.
	 * @param ipAdresi Bu paketin gönderileceði IP adresi
	 */
	public void paketYolla(IPPacket paket, Inet4Address ipAdresi)
	{
		//son talepID deðerini bir artýr ve bu gönderim için kullan
		long talepID = ++sonTalepID;
		//paketin hedef IP adresi deðerini ayarla
		paket.dst_ip = ipAdresi;
		//Paketi ARP sorgusu cevabý bekleyen paketlerin listesine at
		synchronized(MACBekleyenPaketler)
		{
			MACBekleyenPaketler.put(talepID, paket);
		}
		//Paketin LAN üzerindeki bir düðüme gidiyorsa bu düðümün MAC adresini,
		//LAN dýþýnda bir düðüme gidiyorsa Að geçidi cihazýnýn (rooter) MAC
		//adresini iste.
		if(yerelAgAdresi(ipAdresi))
			arp.MACIstegi(talepID, ipAdresi, ARPIstemci);
		else
			arp.MACIstegi(talepID, varsayilanAgGecidi, ARPIstemci);
		/* ARP protokolü iþlemlerini tamamlayýnca zaten MACal() ya da
		 * MACBulunamadi() fonksyonunu çaðýracaðý için bu noktada
		 * baþka biþey yapmaya gerek yoktur.
		 */
	}
	//-------------------------------------------------------------------------
	/**
	 * ARP protokolü üzerinden talep edilen MAC adreslerini almak için
	 * fonksyon. IPARPIstemcisi bir MAC sorgusu cevabý aldýðýnda bilgileri
	 * bu fonksyona gönderir. Bu fonksyon da MACBekleyenPaketler listesinden
	 * ilgili paketi bulup IP header ve Ethernet frame alanlarýný ayarlayýp
	 * NAL katmanýna gönderir.
 	 * @param talepID Daha önce yollanan talebin ID numarasý
	 * @param ipAdresi MAC adresi istenen IP adresi
	 * @param macAdresi Bulunan MAC adresi
	 * @param onBellek Bulunan mac adresi ön bellekten alýndýysa true aksi halde false
	 */
	public void MACal(long talepID, Inet4Address ipAdresi, MACAdresi macAdresi, boolean onBellek)
	{
		IPPacket paket = null;
		//Gelen MAC adresini bekleyen paketi kuyruktan (aðaçtan) al
		synchronized(MACBekleyenPaketler)
		{
			paket = MACBekleyenPaketler.remove(talepID);
		}
		//Böyle bir talep yoksa biþey yapmadan fonksyondan çýk
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
		//Paketin IP protokolü ile ilgili olan kýsýmlarýný ayarla 
		paket.setIPv4Parameter(
				0, // Priority
				false,false,false, // Delay, Troughput, Reliability
				0, // Type of Service
				false,false,false, // 0, DF, MF
				0, //offset
				++sonIdent, // Identification
				defaultTTL, // TTL
				paket.protocol, // Protocol (üst katmandan geliyor zaten)
				this.IPAdresi, // Source Address
				hedefAdres // Destination Address
				);
		//Ethernet çerçevesinin alanlarýný ayarla
		EthernetPacket ether	= new EthernetPacket();
		ether.frametype			= EthernetPacket.ETHERTYPE_IP;
		ether.src_mac			= nal.getMAC().getAdres();
		ether.dst_mac			= macAdresi.getAdres();
		//IP paketini ethernet çerçevesinin içerisine göm
		paket.datalink = ether;
		//Paketi NAL üzerinden aða yolla
		nal.paketAl(paket);
	}
	//-------------------------------------------------------------------------
	/**
	 * ARP protokolü üzerinden sorgulanan bir IP adresi için MAC adresi
	 * bulunamazsa bu durum bu fonksyon ile bildirilir.
	 * Bu fonksyon da bu MAC adresini bekleyen paketi MACBekleyenPaketler listesinden
	 * çýkarýr.
 	 * @param talepID Daha önce yollanan talebin ID numarasý
	 * @param ipAdresi MAC adresi istenen IP adresi
	 */
	public void MACBulunamadi(long talepID, Inet4Address ipAdresi)
	{
		//Böyle bir talep yoksa biþey yapmadan fonksyondan çýk
		if(!MACBekleyenPaketler.containsKey(talepID))
			return;
		//Paketi MAC bekleyenler listesinden çýkar
		/* IPPacket paket = */ MACBekleyenPaketler.remove(talepID);		
		//Üst katmana bu IP'nin bulunamadýðýný bildir ve paketi üst katmana geri ver
		/*
		 *  UYGULANACAK
		 */
	}
	
	// Private fonksyonlar ////////////////////////////////////////////////////
	/**
	 * Alt katmandan gelen IP paketlerini IP adresine göre deðerlendiren fonksyon.
	 * Eðer paket bu bilgisayara gelmiþse paketDegerlendir() fonksyonu, deðilse
	 * paketYonlendir() fonksyonu çaðýrýlýr.
	 * @param p alt katmandan gelen IP paketi
	 */
	private void paketIsle(IPPacket p)
	{
		//gelen paket'in hedef IP adresi ile bu makinanýn IP adresini karþýlaþtýr.
		if(p.dst_ip.equals(nal.getIPAdresi()))
		{
			paketDegerlendir(p); //paket bana gelmiþ
		}
		else
		{
			paketYonlendir(p); //paketin yönlendirilmesi gerekiyor
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu bilgisayara gelen paketleri deðerlendiren fonksyon.
	 * Fragmantasyon bayraklarýna göre ya ustProtokolePaketYolla() fonksyonuna
	 * ya da parcalanmisPaketDegerlendir() fonksyonuna paketi devreder.
	 * @param p alt katmandan gelen IP paketi
	 */
	private void paketDegerlendir(IPPacket p)
	{
		//Fragmentasyon durumuna paketi göre ilgili fonksyona yolla
		if((p.dont_frag) //DF=1 ise frag yok
				|| ((!p.more_frag) && p.offset==0)) //MF=0 ve offset=0 ise frag yok 
		{ //frag yok, paketi protokol numarasýna göre ilgili protokole yolla
			ustProtokolePaketYolla(p);
		}
		else
		{ //frag var, o halde buna göre deðerlendirme yap
			parcalanmisPaketDegerlendir(p);
		}
	}
	/**
	 * Gelen paketi baþlýk yapýsýndaki protokol numarasýna bakarak
	 * ilgili üst katman protokoline gönderir.
	 * @param p framgmantasyon olmayan bütün bir paket
	 */
	private void ustProtokolePaketYolla(IPPacket p)
	{
		//Protokol'e göre ilgili nesneye paketi gönder
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
	 * Fragmentasyon yöntemiyle parçalanmýþ paketleri birleþtiren fonksyon.
	 * Datagramýn parçalarý saklanýr ve bütün parçalar tamamlandýðýnda
	 * hepsi birleþtirilerek üst katmana gönderilir.
	 * @param p framgmentasyona uðrayan datagramýn bir parçasý
	 */
	private void parcalanmisPaketDegerlendir(IPPacket p)
	{
		//Paketi parçalanmýþ kayýtlarý saklayan parcaliPaketler
		//nesnesine atabilmek için bir Kayýt baþlýðý nesnesi oluþturuluyor
		KayitBasligi kb =
			new KayitBasligi((Inet4Address)p.src_ip, (short)p.ident);
		//Tüm parçalarý tamamlanmýþsa synchronized bloðu sonunda
		//bu deðiþkenin deðeri null olmaz
		ParcaKaydi tamamlanmisPaket = null;
		synchronized(parcaliPaketler)
		{
			//Eðer bu IP'den gelen ayný ident'li baþka parçalar varsa bu
			//parçayý da aralarýna ilave et, yoksa yeni bir parça kaydý
			//oluþtur ve ilk olarak bu parçayý ekle
			if(parcaliPaketler.containsKey(kb))
			{
				//bu parçalarý saklayan nesneyi al
				ParcaKaydi pk = parcaliPaketler.get(kb);
				//yeni parçayý ekle
				pk.parcaEkle(p);
				//Paket tamamlanmýþsa bu paketi listeden çýkar.
				if(pk.paketHazir())
					tamamlanmisPaket = parcaliPaketler.remove(kb);
			}
			else
			{
				//Bir parça kaydý oluþtur
				ParcaKaydi pk = new ParcaKaydi(kb);
				//Ýlk olarak bu parçayý ekle
				pk.parcaEkle(p);
				//Belirli süre içerisinde paketin tüm parçalarý gelmezse
				//gelen parçalarýný silen bir görev oluþtur
				pk.yokEtmeGorevi = new ParcaYoketmeGorevi(this,kb);
				//Bu görevi zaman çizelgesine ekle
				zamanlayici.schedule(pk.yokEtmeGorevi, IP.parcaliPaketZamanAsimi);
			}
		}
		
		//Eðer tamamlanan bir paket varsa bunu birleþtir ve üst katmana ilet
		if(tamamlanmisPaket!=null)
		{
			//daha önce ayarlanmýþ yok etme görevini iptal et çünkü paket tamamlandý 
			tamamlanmisPaket.yokEtmeGorevi.cancel();
			ustProtokolePaketYolla(tamamlanmisPaket.birlestirilmisPaketiOlustur());
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Baþka bilgisayara giden paketleri yönlendirmek için fonksyon. Daha çok
	 * router'larda çalýþýr. Ýçeriði boþ
	 * @param p alt katmandan gelen IP paketi
	 */
	private void paketYonlendir(IPPacket p)
	{
		/*
		 * PAKET YÖNLENDÝRME ÝLE ALAKALI KISIM. BU PROJEDE YAPILMAYACAK.
		 * 
		 */
	}
	//-------------------------------------------------------------------------
	/**
	 * Bir IP adresinin LAN üzerindeki bir makinanýn IP adresi
	 * olup olmadýðýný söyler.
	 * @return LAN üzerinde bir IP ise true, aksi halde false
	 */
	private boolean yerelAgAdresi(Inet4Address ipAdresi)
	{
		//IP adreslerini byte dizisine çevir
		byte[] benimIP = this.IPAdresi.getAddress();
		byte[] hedefIP = ipAdresi.getAddress();
		byte[] agMaskesi = altAgMaskesi.getAddress();
		//að maskesinde 255 olan alanlarda benim ip adresim ile
		//hedef ip adresinin deðerleri farklýysa hedef IP LAN'da deðildir.
		for(int i=0;i<4;i++)
		{
			if((agMaskesi[i]==((byte)255)) && 
				(benimIP[i]!=hedefIP[i]))
					return false;
		}
		return true;
	}
	// SET fonksyonlarý ///////////////////////////////////////////////////////
	/**
	 * ARP protokolüne olan referansý ayarlar.
	 * @param arp ARP sýnýfýndan bir nesne
	 */
	public void setARP(ARP arp)
	{
		this.arp = arp;
	}
	//-------------------------------------------------------------------------
	/**
	 * TCP protokolüne olan referansý ayarlar.
	 * @param tcp TCP sýnýfýndan bir nesne 
	 */
	public void setTCP(TCP tcp)
	{
		this.tcp = tcp;
	}
	//-------------------------------------------------------------------------
	/**
	 * UDP protokolüne olan referansý ayarlar.
	 * @param udp UDP sýnýfýndan bir nesne 
	 */
	public void setUDP(UDP udp)
	{
		this.udp = udp;
	}
	/**
	 * MAP protokolüne referansý ayarlar.
	 * @param map MAP sýnýfýndan bir nesne
	 */
	public void setMAP(MAP map)
	{
		this.map = map;
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu makinanýn IP adresini deðiþtirmek için.
	 * @param iPAdresi IP adresi
	 */
	public void setIPAdresi(Inet4Address iPAdresi) 
	{
		IPAdresi = iPAdresi;
	}
	//-------------------------------------------------------------------------
	/**
	 * Yerel að için alt að maskesi deðerini deðiþtirir.
	 * @param altAgMaskesi alt að maskesi
	 */
	public void setAltAgMaskesi(Inet4Address altAgMaskesi)
	{
		this.altAgMaskesi = altAgMaskesi;
	}
	//-------------------------------------------------------------------------
	/**
	 * Varsayýlan að geçidini deðiþtirmek içindir.
	 * @param varsayilanAgGecidi varsayýlan að geçidi
	 */
	public void setVarsayilanAgGecidi(Inet4Address varsayilanAgGecidi)
	{
		this.varsayilanAgGecidi = varsayilanAgGecidi;
	}

	// Get fonksyonlarý ///////////////////////////////////////////////////////
	/**
	 * Bu makinanýn IP adresini dönderir.
	 * @return Bu makinanýn IP adresi
	 */
	public Inet4Address getIPAdresi() 
	{
		return IPAdresi;
	}
	//-------------------------------------------------------------------------
	/**
	 * Yerel aðýn að maskesini dönderir.
	 * @return Alt að maskesi 
	 */
	public Inet4Address getAltAgMaskesi()
	{
		return altAgMaskesi;
	}
	//-------------------------------------------------------------------------
	/**
	 * Varsayýlan að geçidini dönderir.
	 * @return varsayýlan að geçidi
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

	// Thread fonksyonlarý ////////////////////////////////////////////////////
	/**
	 * Thread'in giriþ noktasý.
	 * Bu fonksyon alt katmandan (NAL) gelen paketleri saklamak için kullanýlan
	 * IPKuyrugu adlý kuyruða bakar. Eðer kuyrukta hiç paket yoksa yeni bir paket
	 * gelene kadar bekleme konumunda kalýr. Yeni bir paket geldiðinde paketi
	 * paketIsle fonksyonuna gönderir ve tekrar bekleme konumuna geçer.
	 */
	public void run()
	{
		IPPacket yeniPaket;
		while(calisiyor)
		{
			//alt katmandan gelen paketlerin alýnmasý
			yeniPaket = null;
			synchronized(IPKuyrugu)
			{
				//kuyrukta paket var mý?
				if(IPKuyrugu.size()<=0)
				{
					// yeni paket gelene kadar bekle
					try { IPKuyrugu.wait(); }
					catch (InterruptedException e) { /* boþ */ }
				}
				else
				{
					//paketi kuyruktan al
					yeniPaket = IPKuyrugu.poll();
				}
			}
			//paket alýndýysa bu paketi iþleyen fonksyona yolla
			if(yeniPaket!=null)
			{
				paketIsle(yeniPaket);
			}
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu Thread'in çalýþmasýný durdurmak için.
	 * Bu fonksyon çaðýrýldýktan sonra artýk IP paketi dinleme ve dolayýsýyla
	 * deðerlendirme iþine son verilir.
	 */
	public void durdur()
	{
		calisiyor = false;
		synchronized(IPKuyrugu)
		{
			IPKuyrugu.notifyAll();
		}
	}
	
	// GEÇÝÇÝ KOD /////////////////////////////////////////////////////////////
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
		
		//Paketin IP protokolü ile ilgili olan kýsýmlarýný ayarla 
		paket.setIPv4Parameter(0, // Priority
				false,false,false, // Delay, Troughput, Reliability
				0, // Type of Service
				false,false,false, // 0, DF, MF
				0, //offset
				++sonIdent, // Identification
				defaultTTL, // TTL
				0x0800, // Protocol (üst katmandan geliyor zaten)
				this.IPAdresi, // Source Address
				ipAdresi); // Destination Address
		paket.data = "DENEME".getBytes();
		//Ethernet çerçevesinin alanlarýný ayarla
		EthernetPacket ether	= new EthernetPacket();
		ether.frametype			= EthernetPacket.ETHERTYPE_IP;
		ether.src_mac			= nal.getMAC().getAdres();
		ether.dst_mac			= nal.getMAC().getAdres();
		//IP paketini ethernet çerçevesinin içerisine göm
		paket.datalink = ether;
		//Paketi NAL üzerinden aða yolla
		nal.paketAl(paket);
	}
	// GEÇÝÇÝ KOD /////////////////////////////////////////////////////////////

	// Alt sýnýflar ///////////////////////////////////////////////////////////
	/**
	 * Belirli bir süre içinde tüm parçalarý tamamlanmayan IP paketlerini
	 * iptal etmek için bir görevi temsil eden sýnýf.
	 * 
	 * @author Halil Ýbrahim Kalkan
	 */
	private class ParcaYoketmeGorevi extends TimerTask 
	{
		/** IP protokolünü uygulayan nesneye referans */
		private IP									ip;
		/** Kontrol edilecek kayýtý bulmak için baþlýk bilgisi */
		private KayitBasligi						kb;
		
		// Kurucu fonksyonlar /////////////////////////////////////////////////
		/**
		 * Yeni bir ParcaYoketmeGorevi nesnesi oluþturur.
		 * @param ip IP protokolünü uygulayan nesneye referans
		 * @param kb Kontrol edilecek kayýtý bulmak için baþlýk bilgisi
		 */
		public ParcaYoketmeGorevi(IP ip, KayitBasligi kb)
		{
			this.ip = ip;
			this.kb = kb;
		}

		// Thread fonksyonlarý ////////////////////////////////////////////////
		/**
		 * Bu fonksyon çalýþtýðý zaman kb ile baþlýk bilgisi saklanan kaydý
		 * listeden çýkarýr. Belirli bir süre içerisinde tüm parçalarý tamamlanmayan
		 * paketleri iptal etmek içindir.
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