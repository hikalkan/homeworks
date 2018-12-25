package tcpip.arp;

import java.net.Inet4Address;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.*;

import jpcap.packet.*;

import tcpip.*;
import tcpip.nal.NetworkAccessLayer;

/* **************************************************
 * �NEML� NOT:
 * AKT�F L�STES� TEM�ZLEME ���N
 * TIMER VE TIMERTASK NESNELER�N� KULLAN!!! (mutlaka, yoksa �al��m�yor)
 * ************************************************** 
 */


/**
 * ARP protokol�n� uygulayan s�n�f.
 * 
 * @author Halil �brahim KALKAN
 */
public class ARP extends Thread
{
	// �ye de�i�kenler ////////////////////////////////////////////////////////
	/** NetworkAccessLayer nesnesine referans */
	private NetworkAccessLayer 					nal = null;
	/** network katman�ndan buraya gelen paketler i�in bir tampon olarak kuyruk */
	private LinkedBlockingQueue<ARPPacket> 		ARPKuyrugu = null;
	/** ARP cache tablosu */
	private TreeMap<Inet4Address, ARPKaydi> 	onBellek = null;
	/** ARP cache (�n bellek) tablosundaki zaman a��m� de�eri (ms) */
	private long								onBellekZamanAsimi = 120000; // 2 dakika
	/** ARP cache (�n bellek) tablosunda son yap�lan temizli�in zaman� */
	private long								onBellekSonTemizlikZamani;
	/** ARP cache (�n bellek) tablosunda ne kadar s�rede bir temizlik yap�ls�n (ms) */
	private long								onBellekTemizlikPeriyodu = 60000; // 1 dakika
	/** Aktif (cevaplanmay� bekleyen) ARP taleplerini saklamak i�in */
	private TreeMap<Inet4Address, ARPTalebi>	aktifTalepler = null;
	/** ARP talebi i�in ortalama olarak hesaplanan bekleme s�resi. */
	private long								ortCevapBeklemeSuresi = 10000; // 10 saniye
	/** Sistem ba�lad���ndan bu yana yap�lan ve cevap al�nan toplam ARP talep say�s� */
	private long								toplamCevaplananARPSayisi = 1;
	/** ARP istekleri i�in son yap�lan "cevap gelmeyenleri de�erlendirme" zaman� */
	private long								aktifSonKontrolZamani;
	/** ARP istekleri i�in yap�lan "cevap gelmeyenleri de�erlendirme" periyodu */
	private long								aktifKontrolPeriyodu = 15000; //15 saniye
	/** protokol� y�netmek i�in bayrak */
	private volatile boolean					calisiyor = true;
	/** Broadcast bir adres */
	private static byte[] 						broadcastMAC = new byte[]{
		(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	/**
	 * NetworkAccessLayer �zerinden a�la ileti�im kuran bir ARP protokol�
	 * olu�turur.
	 * @param nal Network Access Layer'i uygulayan bir nesne
	 */
	public ARP(NetworkAccessLayer nal)
	{
		//Bu Thread'e bir isim ver
		setName("ARP");
		//NetworkAccessLayer referans�n� sakla
		this.nal = nal;
		//bo� bir kuyruk olu�tur
		ARPKuyrugu = new LinkedBlockingQueue<ARPPacket>();
		//IP-MAC e�le�melerini saklamak i�in bir �nbellek (cache) olu�tur
		onBellek = new TreeMap<Inet4Address, ARPKaydi>(new Inet4AddressKarsilastirici());
		//Aktif ARP taleplerini saklamak i�in TreeMap olu�tur
		aktifTalepler = new TreeMap<Inet4Address, ARPTalebi>(new Inet4AddressKarsilastirici());
		//�n bellek temizlik zaman�n� �imdiye ayarla
		onBellekSonTemizlikZamani = System.currentTimeMillis();
		//aktif kontrol zaman�n� �imdiye ayarla
		aktifSonKontrolZamani = System.currentTimeMillis();
	}
	
	// public fonksyonlar /////////////////////////////////////////////////////
	/**
	 * Bu fonksyon bu katmana yeni bir paket g�ndermek i�in kullan�l�r.
	 * Gelen paket ARPKuyrugu adl� kuyru�a aktar�l�r ve bu paketleri bekleyen
	 * Threadler notifyAll() metoduyla bilgilendirilir (�al��malar�na devam edilir).
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
	 * Bu fonksyon IP adresi bilinen bir d���m�n MAC adresini elde etmek i�in
	 * kullan�l�r. Genelde IP protokol� taraf�ndan �a��r�l�r ancak ARPIstemcisi
	 * aray�z�n� uygulayan bir s�n�f yard�m�yla herhangi bir yerden de �a��r�labilir.
	 * Yap�lan MAC iste�inin cevab� daha sonra g�nderilir. Bu �ekilde istek yapan
	 * kod beklememi� olur. Cevap al�n�nca istemci vas�tas�yla cevap g�nderilir.
	 * �ste�i ger�ekle�tiren kod daha sonra cevab� ald���nda durumu takip edebilmek
	 * i�in her iste�ine kar��l�k bir talepID numaras� atayabilir. Cevap g�nderilirken
	 * ya da bulunamad� mesaj� g�nderilirken bu talepID kullan�l�r.
	 * @param talepID Bu talep i�in bir ID numaras�
	 * @param ipAdresi MAC adresi istenen IP adresi
	 * @param istemci Bu iste�in cevab�n� g�ndermek i�in bir ARPIstemcisi nesnesi 
	 */
	public void MACIstegi(long talepID, Inet4Address ipAdresi, ARPIstemcisi istemci)
	{
		//istenen IP adresinin �nbellekte olup olmad���n� saklayan de�i�ken
		boolean onBellekteVar = false;
		//�nBellekte varsa kayd� saklamak i�in
		ARPKaydi kayit = null;
		//ayn� anda �nbelle�er yaln�zca bir eri�im olmas�n� garantilemek i�in
		//synchronized ifadesi kullan�l�r.
		synchronized(onBellek)
		{
			//onBellek'i kontrol et
			if(onBellek.containsKey(ipAdresi))
			{
				//ilgili kayd� �nbellekten al
				kayit = onBellek.get(ipAdresi);
				//TimeStamp de�erini kontrol et. E�er X ms'den fazla ge�memi�se
				//bu de�eri kullan. X ms'den fazla olmu�sa kayd� sil. Burdaki
				//X, onBellekZamanAsimi de�i�keninin de�eridir.
				if((System.currentTimeMillis()-kayit.TimeStamp)<onBellekZamanAsimi)
				{ //Zaman a��m� olmam��
					onBellekteVar = true;
				}
				else
				{ //Zaman a��m� olmu�, �nbellekten sil
					onBellek.remove(ipAdresi);
				}
			}
		}
		
		if(onBellekteVar)
		{ //E�er �nbellekte varsa MAC adresini istemciye g�nder ve i�lemi bitir
			istemci.MACal(talepID, ipAdresi, kayit.HardwareAdresi, true);
			/*
			 * NOT: BURADA ZAMANI G�NCELLEYEB�L�RS�N...!
			 * 
			 */
			return;
		}
		else 
		{ //onBellekte bulamad�ysan iste�i MACIstegiOnBelleksiz fonksyonuna devret
			MACIstegiOnBelleksiz(talepID, ipAdresi, istemci);
		}
	}
	
	//-------------------------------------------------------------------------
	/**
	 * Bu fonksyon IP adresi bilinen bir d���m�n MAC adresini elde etmek i�in
	 * kullan�l�r. Ancak bu �a�r�da �n belle�e bak�lmaks�z�n do�rudan ARP talebi
	 * a�a g�nderilir. Di�er i�lemler MACIstegi ile ayn�d�r.
	 * @param talepID bu talep i�in bir ID numaras�
	 * @param ipAdresi MAC adresi istenen IP adresi
	 * @param istemci Bu iste�in cevab�n� g�ndermek i�in bir ARPIstemcisi nesnesi
	 */
	public void MACIstegiOnBelleksiz(long talepID, Inet4Address ipAdresi, ARPIstemcisi istemci)
	{
		//istenen ip i�in daha �nceden bir talep yollan�p yollanmad���n� tutmak i�in
		boolean aktifTalepVar = false;
		//Aktif taleplerden istenen talebi almak i�in kullan�l�r
		ARPTalebi talep = null;
		//IP'ye ula��lam�yorsa �nBellekten silmek i�in
		boolean onBellektenSil = false;
		//Aktif ARP taleplerine bak, e�er bu talep zaten beklemedeyse listeye ekle,
		//de�ilse yeni talep girdisi olu�tur
		synchronized(aktifTalepler)
		{
			if(aktifTalepler.containsKey(ipAdresi))
			{ //aktifTalepler listesinde talep var
				//talep'i kolaksiyondan al
				talep = aktifTalepler.get(ipAdresi);
				//istemcilere yeni iste�i ekle
				talep.istemciEkle(istemci, talepID);
				//talebin bekleme s�resini kontrol et
				if((System.currentTimeMillis()-talep.gondermeZamani)<(ortCevapBeklemeSuresi*2))
				{ //cevap beklemek i�in max zaman� a�mam��, sorun yok
					aktifTalepVar = true; //yeni talep �retmemek i�in
				}
				else
				{ //max s�reyi a�m��
					//deneme say�s�na bak...
					if(talep.tekrarDenemeSayisi<ARPTalebi.maxTekrarDenemeSayisi)
					{ //deneme say�s� max de�ere ula�mam��sa talebi tekrar g�nder
						talep.gondermeZamani = System.currentTimeMillis();
						talep.tekrarDenemeSayisi++;
						//aktifTalepVar zaten false oldu�undan talep a�a��da g�nderilir
					}
					else
					{ //max say�da talep g�nderilmi�
						//ilgili talebi listeden kald�r
						aktifTalepler.remove(ipAdresi);
						//ip adresini (varsa) �nbellekten sil
						onBellektenSil = true;
					}
				}
			} 
			else
			{ //aktifTalepler listesinde bu talep yok
				//yeni bir talep olu�tur ve listeye ekle
				ARPTalebi yeniTalep = new ARPTalebi(ipAdresi);
				yeniTalep.istemciEkle(istemci, talepID);
				yeniTalep.gondermeZamani = System.currentTimeMillis();
				aktifTalepler.put(ipAdresi, yeniTalep);
			}
		}
		
		//E�er maximum deneme (tekrar g�nderme) say�s�na ula�m��sa onBellektenSil
		//de�i�keni true olacakt�r. Bu durumda bu ip'yi �nBellekten kald�r ve
		//bu talebi bekleyen istemcilere olumsuz yan�t ver
		if(onBellektenSil)
		{
			synchronized(onBellek)
			{
				onBellek.remove(ipAdresi);
			}
			for(ARPTalebi.ARPIstemciKaydi ik : talep.istemciler)
				ik.istemci.MACBulunamadi(ik.talepID, ipAdresi);
		}

		//E�er aktif talep listesinde yoksa bir ARP_REQUEST olu�tur
		if(!aktifTalepVar)
			ARPTalepYolla(ipAdresi);
	}
	
	// Private fonksyonlar ////////////////////////////////////////////////////
	/**
	 * Bu fonksyon alt katmandan gelen paketleri ARP protokol kurallar�na
	 * uygun olarak de�erlendirir. 
	 * Bkz: INSIDE TCP/IP Third Edition adl� kitab�n PART II Chapter 5
	 * Address Resolution Protocol b�l�m�ndeki algoritma temel al�nm��t�r
	 * @param p de�erlendirilecek paket
	 */
	private void paketIsle(ARPPacket p)
	{
		//Hardware tipi kontrol�
		if(p.hardtype!=ARPPacket.HARDTYPE_ETHER)
			return;
		//MAC adresi uzunlu�u uygun de�il
		if(p.hlen!=6)
			return;
		//Protokol tipi kontrol�
		if(p.prototype!=ARPPacket.PROTOTYPE_IP)
			return;
		//IP adresi uzunlu�u uygun de�il
		if(p.plen!=4)
			return;
		
		boolean onBellekteVar = false;
		boolean paketBana = false;
		
		synchronized(onBellek)
		{
			//�nbellekte bu IP ile ilgili kay�t olup olmad���na bak
			Inet4Address gonderenIP = null;
			
			try { gonderenIP = (Inet4Address)Inet4Address.getByAddress(p.sender_protoaddr);	}
			catch (Exception e) { /* bo� */ }
			
			onBellekteVar = onBellek.containsKey(gonderenIP);
			
			//�ncellekte varsa kayd� g�ncelle
			if(onBellekteVar)
			{
				ARPKaydi kayit = onBellek.get(gonderenIP);
				if(kayit.ProtokolTipi==p.prototype)
				{
					kayit.HardwareAdresi.setAdres(p.sender_hardaddr);
					kayit.TimeStamp = System.currentTimeMillis();
				}
			}
			
			//G�nderilen ARP paketi bana m� gelmi� bak
			paketBana = Arrays.equals(p.target_protoaddr, nal.getIPAdresi().getAddress());
			
			// E�er paket bana gelmi�se ve �nbellekte yoksa ekle
			if(paketBana && (!onBellekteVar))
			{
				//kayd� olu�tur
				ARPKaydi kayit = new ARPKaydi(
					p.prototype,
					gonderenIP,
					new MACAdresi(p.sender_hardaddr),
					System.currentTimeMillis());
				//�nbelle�e ekle
				onBellek.put(kayit.IPAdresi, kayit);
			}
		}
		
		//Paket bana gelmemi�se ��k
		if(!paketBana)
			return;
		
		//operation tipine g�re paketi de�erlendir
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
	 * olu�turur ve t�m de�i�kenlerinin de�erlerini ayarlar.
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
	 * Bu fonksyon a�a do�rudan bir ARP talebi yollar. ARPPacket nesnesini
	 * olu�turur ve t�m de�i�kenlerinin de�erlerini ayarlar.
	 * @param ipAdresi MAC adresi ��renilmek istenen IP adresi
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
	 * Bu fonksyon daha �nce yap�lan bir ARP_REQUEST i�lemine kar��l�k olarak
	 * gelen bir ARP_REPLY paketini de�erlendirir.
	 * Gelen paketdeki MAC adresini, bunu talep eden ARPIstemcisi tipinde
	 * nesnelere g�nderir.
	 * @param p gelen ARP paketi
	 */
	private void ARPCevapDegerlendir(ARPPacket p)
	{
		//Paketdeki ip adresinden bir Inet4Address nesnesi olu�tur
		Inet4Address ipAdresi = null;
		try { ipAdresi = (Inet4Address)(Inet4Address.getByAddress(p.sender_protoaddr));	}
		catch (Exception e) { return; }
		
		//aktifTalepler listesine bak, e�er varsa t�m isteyenlere MAC adresini yolla
		//ve bu talebi listeden ��kar
		synchronized(aktifTalepler)
		{
			if(aktifTalepler.containsKey(ipAdresi))
			{
				//talebi listeden al
				ARPTalebi talep = aktifTalepler.get(ipAdresi);
				//t�m istemcilere MAC adresini g�nder
				for(ARPTalebi.ARPIstemciKaydi ik : talep.istemciler)
					ik.istemci.MACal(ik.talepID, ipAdresi, new MACAdresi(p.sender_hardaddr), false);
				//talebi listeden sil
				aktifTalepler.remove(ipAdresi);
				//ortalama cevap zaman�n� yeniden hesapla
				ortCevapBeklemeSuresi = (long)
					(((ortCevapBeklemeSuresi*toplamCevaplananARPSayisi)+
					(System.currentTimeMillis()-talep.gondermeZamani))/
					(toplamCevaplananARPSayisi+1));
				//en son cevap al�nan s�renin etkisini art�rmak i�in
				//daha �nceki cevaplanan paket say�s� 100'den fazlaysa
				//100 olarak kabul ediliyor.
				if(toplamCevaplananARPSayisi<100)
					toplamCevaplananARPSayisi++;
			}
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu fonksyon NetworkAccessLayer nesnesi yard�m�yla alt katmana bir paket
	 * g�ndermek i�in kullan�l�r.
	 * @param p a�a yollanacak paket
	 */
	private void paketYolla(ARPPacket p)
	{
		nal.paketAl(p);
	}
	
	//-------------------------------------------------------------------------
	/**
	 * �nbellekteki (cache) zaman a��m�na u�ram�� kay�tlar� silmek i�indir.
	 * E�er son temizlikten bu yana onBellekTemizlikPeriyodu kadar zaman ge�mi�se
	 * temizli�e ba�lar ve onBellekZamanAsimi kadar s�redir pasif olan kay�tlar�
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
	 * Bu fonksyon aktif talepleri y�netmek i�in kullan�l�r. ��yle ki;
	 * aktifTalepler listesine bak�l�r ve ortCevapBeklemeSuresi'nin 2 kat�
	 * kadar zaman ge�mesine ra�men hen�z cevap al�namayan talepler 2 �ekilde
	 * de�erlendirilir:
	 * 1) maximum deneme say�s�na ula�mayanlar i�in yeniden talep g�nderilir.
	 * 2) maximum deneme say�s�na ula�anlar i�in istemcilere "bulunamad�" bilgisi
	 *    g�nderilir ve bu ip listeden ��kar�l�r.
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
							//maximum deneme say�s�na ula�mayanlar i�in yeniden talep g�nderilir.
							kayit.gondermeZamani = simdikiZaman;
							kayit.tekrarDenemeSayisi++;
							ARPTalepYolla(kayit.ip);
						}
						else
						{
							//maximum deneme say�s�na ula�anlar i�in istemcilere "bulunamad�"
							//bilgisi g�nderilirve listeden ��kar�l�r
							aktifTalepler.remove(kayit);
							for(ARPTalebi.ARPIstemciKaydi kist : kayit.istemciler)
								kist.istemci.MACBulunamadi(kist.talepID, kayit.ip);
						}
					}
			}
		}
	}
	
	// Thread fonksyonlar� ////////////////////////////////////////////////////
	/**
	 * Thread'in giri� noktas�.
	 * Bu fonksyonda alt katmandan gelen paketlerin bulundu�u ARPKuyrugu
	 * adl� kuyru�a bakar. E�er hi� paket yoksa Thread wait() konumuna ge�er.
	 * E�er kuyrukta paket varsa ilk paket al�n�r ve paketIsle fonksyonuna
	 * g�nderilir.
	 */
	public void run()
	{
		ARPPacket yeniPaket;
		while(calisiyor)
		{
			//alt katmandan gelen paketlerin al�nmas�
			yeniPaket = null;
			synchronized(ARPKuyrugu)
			{
				if(ARPKuyrugu.size()<=0)
				{
					//haz�r bo� kalm��ken �nbelle�i temizle ve aktifleri kontrol et
					//belki bu 2 sat�r daha uygun bir yere yaz�labilir ama �imdilik iyi
					onBellekTemizle();
					aktifTalepleriKontrolEt();
					// yeni paket gelene kadar bekle
					try { ARPKuyrugu.wait(); }
					catch (InterruptedException e) { /* bo� */ }
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
	 * Bu Thread'in �al��mas�n� durdurmak i�in.
	 * Bu fonksyon �a��r�ld�ktan sonra art�k ARP paketi dinleme ve dolay�s�yla
	 * cevaplama ve de�erlendirme i�ine son verilir.
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