package tcpip.arp;

import java.net.Inet4Address;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.*;

import jpcap.packet.*;

import tcpip.*;
import tcpip.nal.NetworkAccessLayer;

/* **************************************************
 * ÖNEMLÝ NOT:
 * AKTÝF LÝSTESÝ TEMÝZLEME ÝÇÝN
 * TIMER VE TIMERTASK NESNELERÝNÝ KULLAN!!! (mutlaka, yoksa çalýþmýyor)
 * ************************************************** 
 */


/**
 * ARP protokolünü uygulayan sýnýf.
 * 
 * @author Halil Ýbrahim KALKAN
 */
public class ARP extends Thread
{
	// Üye deðiþkenler ////////////////////////////////////////////////////////
	/** NetworkAccessLayer nesnesine referans */
	private NetworkAccessLayer 					nal = null;
	/** network katmanýndan buraya gelen paketler için bir tampon olarak kuyruk */
	private LinkedBlockingQueue<ARPPacket> 		ARPKuyrugu = null;
	/** ARP cache tablosu */
	private TreeMap<Inet4Address, ARPKaydi> 	onBellek = null;
	/** ARP cache (ön bellek) tablosundaki zaman aþýmý deðeri (ms) */
	private long								onBellekZamanAsimi = 120000; // 2 dakika
	/** ARP cache (ön bellek) tablosunda son yapýlan temizliðin zamaný */
	private long								onBellekSonTemizlikZamani;
	/** ARP cache (ön bellek) tablosunda ne kadar sürede bir temizlik yapýlsýn (ms) */
	private long								onBellekTemizlikPeriyodu = 60000; // 1 dakika
	/** Aktif (cevaplanmayý bekleyen) ARP taleplerini saklamak için */
	private TreeMap<Inet4Address, ARPTalebi>	aktifTalepler = null;
	/** ARP talebi için ortalama olarak hesaplanan bekleme süresi. */
	private long								ortCevapBeklemeSuresi = 10000; // 10 saniye
	/** Sistem baþladýðýndan bu yana yapýlan ve cevap alýnan toplam ARP talep sayýsý */
	private long								toplamCevaplananARPSayisi = 1;
	/** ARP istekleri için son yapýlan "cevap gelmeyenleri deðerlendirme" zamaný */
	private long								aktifSonKontrolZamani;
	/** ARP istekleri için yapýlan "cevap gelmeyenleri deðerlendirme" periyodu */
	private long								aktifKontrolPeriyodu = 15000; //15 saniye
	/** protokolü yönetmek için bayrak */
	private volatile boolean					calisiyor = true;
	/** Broadcast bir adres */
	private static byte[] 						broadcastMAC = new byte[]{
		(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	/**
	 * NetworkAccessLayer üzerinden aðla iletiþim kuran bir ARP protokolü
	 * oluþturur.
	 * @param nal Network Access Layer'i uygulayan bir nesne
	 */
	public ARP(NetworkAccessLayer nal)
	{
		//Bu Thread'e bir isim ver
		setName("ARP");
		//NetworkAccessLayer referansýný sakla
		this.nal = nal;
		//boþ bir kuyruk oluþtur
		ARPKuyrugu = new LinkedBlockingQueue<ARPPacket>();
		//IP-MAC eþleþmelerini saklamak için bir önbellek (cache) oluþtur
		onBellek = new TreeMap<Inet4Address, ARPKaydi>(new Inet4AddressKarsilastirici());
		//Aktif ARP taleplerini saklamak için TreeMap oluþtur
		aktifTalepler = new TreeMap<Inet4Address, ARPTalebi>(new Inet4AddressKarsilastirici());
		//ön bellek temizlik zamanýný þimdiye ayarla
		onBellekSonTemizlikZamani = System.currentTimeMillis();
		//aktif kontrol zamanýný þimdiye ayarla
		aktifSonKontrolZamani = System.currentTimeMillis();
	}
	
	// public fonksyonlar /////////////////////////////////////////////////////
	/**
	 * Bu fonksyon bu katmana yeni bir paket göndermek için kullanýlýr.
	 * Gelen paket ARPKuyrugu adlý kuyruða aktarýlýr ve bu paketleri bekleyen
	 * Threadler notifyAll() metoduyla bilgilendirilir (çalýþmalarýna devam edilir).
	 * @param p gelen paket
	 */
	public void paketAl(ARPPacket p)
	{
		synchronized(ARPKuyrugu)
		{
			ARPKuyrugu.add(p);
			ARPKuyrugu.notifyAll();
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu fonksyon IP adresi bilinen bir düðümün MAC adresini elde etmek için
	 * kullanýlýr. Genelde IP protokolü tarafýndan çaðýrýlýr ancak ARPIstemcisi
	 * arayüzünü uygulayan bir sýnýf yardýmýyla herhangi bir yerden de çaðýrýlabilir.
	 * Yapýlan MAC isteðinin cevabý daha sonra gönderilir. Bu þekilde istek yapan
	 * kod beklememiþ olur. Cevap alýnýnca istemci vasýtasýyla cevap gönderilir.
	 * Ýsteði gerçekleþtiren kod daha sonra cevabý aldýðýnda durumu takip edebilmek
	 * için her isteðine karþýlýk bir talepID numarasý atayabilir. Cevap gönderilirken
	 * ya da bulunamadý mesajý gönderilirken bu talepID kullanýlýr.
	 * @param talepID Bu talep için bir ID numarasý
	 * @param ipAdresi MAC adresi istenen IP adresi
	 * @param istemci Bu isteðin cevabýný göndermek için bir ARPIstemcisi nesnesi 
	 */
	public void MACIstegi(long talepID, Inet4Address ipAdresi, ARPIstemcisi istemci)
	{
		//istenen IP adresinin önbellekte olup olmadýðýný saklayan deðiþken
		boolean onBellekteVar = false;
		//önBellekte varsa kaydý saklamak için
		ARPKaydi kayit = null;
		//ayný anda önbelleðer yalnýzca bir eriþim olmasýný garantilemek için
		//synchronized ifadesi kullanýlýr.
		synchronized(onBellek)
		{
			//onBellek'i kontrol et
			if(onBellek.containsKey(ipAdresi))
			{
				//ilgili kaydý önbellekten al
				kayit = onBellek.get(ipAdresi);
				//TimeStamp deðerini kontrol et. Eðer X ms'den fazla geçmemiþse
				//bu deðeri kullan. X ms'den fazla olmuþsa kaydý sil. Burdaki
				//X, onBellekZamanAsimi deðiþkeninin deðeridir.
				if((System.currentTimeMillis()-kayit.TimeStamp)<onBellekZamanAsimi)
				{ //Zaman aþýmý olmamýþ
					onBellekteVar = true;
				}
				else
				{ //Zaman aþýmý olmuþ, önbellekten sil
					onBellek.remove(ipAdresi);
				}
			}
		}
		
		if(onBellekteVar)
		{ //Eðer önbellekte varsa MAC adresini istemciye gönder ve iþlemi bitir
			istemci.MACal(talepID, ipAdresi, kayit.HardwareAdresi, true);
			/*
			 * NOT: BURADA ZAMANI GÜNCELLEYEBÝLÝRSÝN...!
			 * 
			 */
			return;
		}
		else 
		{ //onBellekte bulamadýysan isteði MACIstegiOnBelleksiz fonksyonuna devret
			MACIstegiOnBelleksiz(talepID, ipAdresi, istemci);
		}
	}
	
	//-------------------------------------------------------------------------
	/**
	 * Bu fonksyon IP adresi bilinen bir düðümün MAC adresini elde etmek için
	 * kullanýlýr. Ancak bu çaðrýda ön belleðe bakýlmaksýzýn doðrudan ARP talebi
	 * aða gönderilir. Diðer iþlemler MACIstegi ile aynýdýr.
	 * @param talepID bu talep için bir ID numarasý
	 * @param ipAdresi MAC adresi istenen IP adresi
	 * @param istemci Bu isteðin cevabýný göndermek için bir ARPIstemcisi nesnesi
	 */
	public void MACIstegiOnBelleksiz(long talepID, Inet4Address ipAdresi, ARPIstemcisi istemci)
	{
		//istenen ip için daha önceden bir talep yollanýp yollanmadýðýný tutmak için
		boolean aktifTalepVar = false;
		//Aktif taleplerden istenen talebi almak için kullanýlýr
		ARPTalebi talep = null;
		//IP'ye ulaþýlamýyorsa önBellekten silmek için
		boolean onBellektenSil = false;
		//Aktif ARP taleplerine bak, eðer bu talep zaten beklemedeyse listeye ekle,
		//deðilse yeni talep girdisi oluþtur
		synchronized(aktifTalepler)
		{
			if(aktifTalepler.containsKey(ipAdresi))
			{ //aktifTalepler listesinde talep var
				//talep'i kolaksiyondan al
				talep = aktifTalepler.get(ipAdresi);
				//istemcilere yeni isteði ekle
				talep.istemciEkle(istemci, talepID);
				//talebin bekleme süresini kontrol et
				if((System.currentTimeMillis()-talep.gondermeZamani)<(ortCevapBeklemeSuresi*2))
				{ //cevap beklemek için max zamaný aþmamýþ, sorun yok
					aktifTalepVar = true; //yeni talep üretmemek için
				}
				else
				{ //max süreyi aþmýþ
					//deneme sayýsýna bak...
					if(talep.tekrarDenemeSayisi<ARPTalebi.maxTekrarDenemeSayisi)
					{ //deneme sayýsý max deðere ulaþmamýþsa talebi tekrar gönder
						talep.gondermeZamani = System.currentTimeMillis();
						talep.tekrarDenemeSayisi++;
						//aktifTalepVar zaten false olduðundan talep aþaðýda gönderilir
					}
					else
					{ //max sayýda talep gönderilmiþ
						//ilgili talebi listeden kaldýr
						aktifTalepler.remove(ipAdresi);
						//ip adresini (varsa) önbellekten sil
						onBellektenSil = true;
					}
				}
			} 
			else
			{ //aktifTalepler listesinde bu talep yok
				//yeni bir talep oluþtur ve listeye ekle
				ARPTalebi yeniTalep = new ARPTalebi(ipAdresi);
				yeniTalep.istemciEkle(istemci, talepID);
				yeniTalep.gondermeZamani = System.currentTimeMillis();
				aktifTalepler.put(ipAdresi, yeniTalep);
			}
		}
		
		//Eðer maximum deneme (tekrar gönderme) sayýsýna ulaþmýþsa onBellektenSil
		//deðiþkeni true olacaktýr. Bu durumda bu ip'yi önBellekten kaldýr ve
		//bu talebi bekleyen istemcilere olumsuz yanýt ver
		if(onBellektenSil)
		{
			synchronized(onBellek)
			{
				onBellek.remove(ipAdresi);
			}
			for(ARPTalebi.ARPIstemciKaydi ik : talep.istemciler)
				ik.istemci.MACBulunamadi(ik.talepID, ipAdresi);
		}

		//Eðer aktif talep listesinde yoksa bir ARP_REQUEST oluþtur
		if(!aktifTalepVar)
			ARPTalepYolla(ipAdresi);
	}
	
	// Private fonksyonlar ////////////////////////////////////////////////////
	/**
	 * Bu fonksyon alt katmandan gelen paketleri ARP protokol kurallarýna
	 * uygun olarak deðerlendirir. 
	 * Bkz: INSIDE TCP/IP Third Edition adlý kitabýn PART II Chapter 5
	 * Address Resolution Protocol bölümündeki algoritma temel alýnmýþtýr
	 * @param p deðerlendirilecek paket
	 */
	private void paketIsle(ARPPacket p)
	{
		//Hardware tipi kontrolü
		if(p.hardtype!=ARPPacket.HARDTYPE_ETHER)
			return;
		//MAC adresi uzunluðu uygun deðil
		if(p.hlen!=6)
			return;
		//Protokol tipi kontrolü
		if(p.prototype!=ARPPacket.PROTOTYPE_IP)
			return;
		//IP adresi uzunluðu uygun deðil
		if(p.plen!=4)
			return;
		
		boolean onBellekteVar = false;
		boolean paketBana = false;
		
		synchronized(onBellek)
		{
			//Önbellekte bu IP ile ilgili kayýt olup olmadýðýna bak
			Inet4Address gonderenIP = null;
			
			try { gonderenIP = (Inet4Address)Inet4Address.getByAddress(p.sender_protoaddr);	}
			catch (Exception e) { /* boþ */ }
			
			onBellekteVar = onBellek.containsKey(gonderenIP);
			
			//Öncellekte varsa kaydý güncelle
			if(onBellekteVar)
			{
				ARPKaydi kayit = onBellek.get(gonderenIP);
				if(kayit.ProtokolTipi==p.prototype)
				{
					kayit.HardwareAdresi.setAdres(p.sender_hardaddr);
					kayit.TimeStamp = System.currentTimeMillis();
				}
			}
			
			//Gönderilen ARP paketi bana mý gelmiþ bak
			paketBana = Arrays.equals(p.target_protoaddr, nal.getIPAdresi().getAddress());
			
			// Eðer paket bana gelmiþse ve önbellekte yoksa ekle
			if(paketBana && (!onBellekteVar))
			{
				//kaydý oluþtur
				ARPKaydi kayit = new ARPKaydi(
					p.prototype,
					gonderenIP,
					new MACAdresi(p.sender_hardaddr),
					System.currentTimeMillis());
				//önbelleðe ekle
				onBellek.put(kayit.IPAdresi, kayit);
			}
		}
		
		//Paket bana gelmemiþse çýk
		if(!paketBana)
			return;
		
		//operation tipine göre paketi deðerlendir
		switch(p.operation)
		{
			case ARPPacket.ARP_REQUEST:
				ARPCevapYolla(p);
				break;
			case ARPPacket.ARP_REPLY:
				ARPCevapDegerlendir(p);
				break;
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu fonksyon gelen bir ARP talebine cevap verir. ARPPacket nesnesini
	 * oluþturur ve tüm deðiþkenlerinin deðerlerini ayarlar.
	 * @param ARPIstegi gelen ARP_REQUEST paketi
	 */
	private void ARPCevapYolla(ARPPacket ARPIstegi)
	{
		ARPPacket ARPcevabi			= new ARPPacket();
		ARPcevabi.hardtype			= ARPPacket.HARDTYPE_ETHER;
		ARPcevabi.prototype			= ARPPacket.PROTOTYPE_IP;
		ARPcevabi.operation			= ARPPacket.ARP_REPLY;
		ARPcevabi.hlen				= 6;
		ARPcevabi.plen				= 4;
		ARPcevabi.sender_hardaddr	= nal.getMAC().getAdres();
		ARPcevabi.sender_protoaddr	= nal.getIPAdresi().getAddress();
		ARPcevabi.target_hardaddr	= ARPIstegi.sender_hardaddr;
		ARPcevabi.target_protoaddr	= ARPIstegi.sender_protoaddr;
		
		EthernetPacket ether 		= new EthernetPacket();
		ether.dst_mac				= ARPIstegi.sender_hardaddr;
		ether.src_mac				= ARPcevabi.sender_hardaddr;
		ether.frametype				= EthernetPacket.ETHERTYPE_ARP;
		ARPcevabi.datalink			= ether;
		
		paketYolla(ARPcevabi);
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu fonksyon aða doðrudan bir ARP talebi yollar. ARPPacket nesnesini
	 * oluþturur ve tüm deðiþkenlerinin deðerlerini ayarlar.
	 * @param ipAdresi MAC adresi öðrenilmek istenen IP adresi
	 */
	private void ARPTalepYolla(Inet4Address ipAdresi)
	{
		ARPPacket ARPistemi			= new ARPPacket();
		ARPistemi.hardtype			= ARPPacket.HARDTYPE_ETHER;
		ARPistemi.prototype			= ARPPacket.PROTOTYPE_IP;
		ARPistemi.operation			= ARPPacket.ARP_REQUEST;
		ARPistemi.hlen				= 6;
		ARPistemi.plen				= 4;
		ARPistemi.sender_hardaddr	= nal.getMAC().getAdres();
		ARPistemi.sender_protoaddr	= nal.getIPAdresi().getAddress();
		ARPistemi.target_hardaddr	= ARP.broadcastMAC;
		ARPistemi.target_protoaddr	= ipAdresi.getAddress();
		
		EthernetPacket ether 		= new EthernetPacket();
		ether.dst_mac				= ARPistemi.target_hardaddr;
		ether.src_mac				= ARPistemi.sender_hardaddr;
		ether.frametype				= EthernetPacket.ETHERTYPE_ARP;
		ARPistemi.datalink			= ether;
		
		paketYolla(ARPistemi);	
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu fonksyon daha önce yapýlan bir ARP_REQUEST iþlemine karþýlýk olarak
	 * gelen bir ARP_REPLY paketini deðerlendirir.
	 * Gelen paketdeki MAC adresini, bunu talep eden ARPIstemcisi tipinde
	 * nesnelere gönderir.
	 * @param p gelen ARP paketi
	 */
	private void ARPCevapDegerlendir(ARPPacket p)
	{
		//Paketdeki ip adresinden bir Inet4Address nesnesi oluþtur
		Inet4Address ipAdresi = null;
		try { ipAdresi = (Inet4Address)(Inet4Address.getByAddress(p.sender_protoaddr));	}
		catch (Exception e) { return; }
		
		//aktifTalepler listesine bak, eðer varsa tüm isteyenlere MAC adresini yolla
		//ve bu talebi listeden çýkar
		synchronized(aktifTalepler)
		{
			if(aktifTalepler.containsKey(ipAdresi))
			{
				//talebi listeden al
				ARPTalebi talep = aktifTalepler.get(ipAdresi);
				//tüm istemcilere MAC adresini gönder
				for(ARPTalebi.ARPIstemciKaydi ik : talep.istemciler)
					ik.istemci.MACal(ik.talepID, ipAdresi, new MACAdresi(p.sender_hardaddr), false);
				//talebi listeden sil
				aktifTalepler.remove(ipAdresi);
				//ortalama cevap zamanýný yeniden hesapla
				ortCevapBeklemeSuresi = (long)
					(((ortCevapBeklemeSuresi*toplamCevaplananARPSayisi)+
					(System.currentTimeMillis()-talep.gondermeZamani))/
					(toplamCevaplananARPSayisi+1));
				//en son cevap alýnan sürenin etkisini artýrmak için
				//daha önceki cevaplanan paket sayýsý 100'den fazlaysa
				//100 olarak kabul ediliyor.
				if(toplamCevaplananARPSayisi<100)
					toplamCevaplananARPSayisi++;
			}
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu fonksyon NetworkAccessLayer nesnesi yardýmýyla alt katmana bir paket
	 * göndermek için kullanýlýr.
	 * @param p aða yollanacak paket
	 */
	private void paketYolla(ARPPacket p)
	{
		nal.paketAl(p);
	}
	
	//-------------------------------------------------------------------------
	/**
	 * Önbellekteki (cache) zaman aþýmýna uðramýþ kayýtlarý silmek içindir.
	 * Eðer son temizlikten bu yana onBellekTemizlikPeriyodu kadar zaman geçmiþse
	 * temizliðe baþlar ve onBellekZamanAsimi kadar süredir pasif olan kayýtlarý
	 * siler.
	 */
	private void onBellekTemizle()
	{
		long simdikiZaman = System.currentTimeMillis();
		if((simdikiZaman-onBellekSonTemizlikZamani)>=onBellekTemizlikPeriyodu)
		{
			synchronized(onBellek)
			{
				Collection<ARPKaydi> kayitlar = onBellek.values();
				for(ARPKaydi kayit : kayitlar)
					if((simdikiZaman-kayit.TimeStamp)>=onBellekZamanAsimi)
						onBellek.remove(kayit.IPAdresi);
			}
		}
	}
	
	//-------------------------------------------------------------------------
	/**
	 * Bu fonksyon aktif talepleri yönetmek için kullanýlýr. Þöyle ki;
	 * aktifTalepler listesine bakýlýr ve ortCevapBeklemeSuresi'nin 2 katý
	 * kadar zaman geçmesine raðmen henüz cevap alýnamayan talepler 2 þekilde
	 * deðerlendirilir:
	 * 1) maximum deneme sayýsýna ulaþmayanlar için yeniden talep gönderilir.
	 * 2) maximum deneme sayýsýna ulaþanlar için istemcilere "bulunamadý" bilgisi
	 *    gönderilir ve bu ip listeden çýkarýlýr.
	 */
	private void aktifTalepleriKontrolEt()
	{
		long simdikiZaman = System.currentTimeMillis();
		if((simdikiZaman-aktifSonKontrolZamani)>=aktifKontrolPeriyodu)
		{
			synchronized(aktifTalepler)
			{
				Collection<ARPTalebi> kayitlar = aktifTalepler.values();
				for(ARPTalebi kayit : kayitlar)
					if(((simdikiZaman-kayit.gondermeZamani))>=(ortCevapBeklemeSuresi*2))
					{
						if(kayit.tekrarDenemeSayisi<ARPTalebi.maxTekrarDenemeSayisi)
						{ 
							//maximum deneme sayýsýna ulaþmayanlar için yeniden talep gönderilir.
							kayit.gondermeZamani = simdikiZaman;
							kayit.tekrarDenemeSayisi++;
							ARPTalepYolla(kayit.ip);
						}
						else
						{
							//maximum deneme sayýsýna ulaþanlar için istemcilere "bulunamadý"
							//bilgisi gönderilirve listeden çýkarýlýr
							aktifTalepler.remove(kayit);
							for(ARPTalebi.ARPIstemciKaydi kist : kayit.istemciler)
								kist.istemci.MACBulunamadi(kist.talepID, kayit.ip);
						}
					}
			}
		}
	}
	
	// Thread fonksyonlarý ////////////////////////////////////////////////////
	/**
	 * Thread'in giriþ noktasý.
	 * Bu fonksyonda alt katmandan gelen paketlerin bulunduðu ARPKuyrugu
	 * adlý kuyruða bakar. Eðer hiç paket yoksa Thread wait() konumuna geçer.
	 * Eðer kuyrukta paket varsa ilk paket alýnýr ve paketIsle fonksyonuna
	 * gönderilir.
	 */
	public void run()
	{
		ARPPacket yeniPaket;
		while(calisiyor)
		{
			//alt katmandan gelen paketlerin alýnmasý
			yeniPaket = null;
			synchronized(ARPKuyrugu)
			{
				if(ARPKuyrugu.size()<=0)
				{
					//hazýr boþ kalmýþken önbelleði temizle ve aktifleri kontrol et
					//belki bu 2 satýr daha uygun bir yere yazýlabilir ama þimdilik iyi
					onBellekTemizle();
					aktifTalepleriKontrolEt();
					// yeni paket gelene kadar bekle
					try { ARPKuyrugu.wait(); }
					catch (InterruptedException e) { /* boþ */ }
				}
				else
				{
					yeniPaket = ARPKuyrugu.poll();
				}
			}
			if(yeniPaket!=null)
				paketIsle(yeniPaket);
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu Thread'in çalýþmasýný durdurmak için.
	 * Bu fonksyon çaðýrýldýktan sonra artýk ARP paketi dinleme ve dolayýsýyla
	 * cevaplama ve deðerlendirme iþine son verilir.
	 */
	public void durdur()
	{
		calisiyor = false;
		synchronized(ARPKuyrugu)
		{
			ARPKuyrugu.notifyAll();
		}
	}
}