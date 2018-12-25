package tcpip.nal;

import jpcap.*;
import jpcap.packet.*;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.Inet4Address;
import java.io.*;

import tcpip.MACAdresi;
import tcpip.arp.*;
import tcpip.ip.*;

/**
 * IP ve ARP protokollerinin aða eriþmesini saðlayan sýnýf.
 * Bir thread olarak çalýþýr ve aðdan paket geldikçe bunlarý
 * ARPKuyrugu ve IPKuyrugu nesnelerine aktarýr ve bu kuyruklarda
 * paket bekleyen thread'leri bilgilendirir (notifyAll()).
 * Ayrýca paketleri göndermek için de paketYollayicisi adlý bir
 * thread oluþturur ve bunu NetworkKuyrugu nesnesine baðlar.
 * 
 * @author Halil Ýbrahim KALKAN
 */
public class NetworkAccessLayer extends Thread 
{
	/** iletiþimin üstünden saðlantýðý NIC */
	private NetworkInterface 				agCihazi;
	/** Bu makinanýn geçerli IP adresi */
	private Inet4Address					IPAdresi;
	/** IP protokolüne referans */
	private IP								ip;
	/** ARP protokolüne referans */
	private ARP								arp;
	/** paket yakalamak için kullanýlan nesne */
	private JpcapCaptor 					captor;
	/** paket göndermek için kullanýlan nesne */
	private JpcapSender 					sender;
	/** paket göndermek için kullanýlan Thread */
	private PaketYollayicisi				paketYollayicisi;
	/** paketleri almak için PacketReceiver arayüzünü uygulayan sýnýftan bir nesne */
	private PaketAlicisi					receiver;
	/** üst katmanlardan bu katmana gelen paketlerin girdiði tampon kuyruk */
	private LinkedBlockingQueue<Packet>		NetworkKuyrugu;
	/** protokolü yönetmek için bayrak */
	private volatile boolean				calisiyor = true;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public NetworkAccessLayer() 
	{
		//Bu Thread'e bir isim ver
		setName("NAL");
		//gelen paketler için kuyruðu oluþtur
		NetworkKuyrugu = new LinkedBlockingQueue<Packet>();
	}
	
	// Public Metodlar ////////////////////////////////////////////////////////
	/**
	 * Bu fonksyon üst katmanlardan aða paket yollanmasý amacýyla tasarlandý.
	 * Gelen paketler NetworkKuyrugu adlý bir kuyruða eklenir ve bu kuyrukta
	 * paket bekleyen Thread'ler bilgilendirilir (notifyAll()).
	 * @param p aða gönderilecek paket
	 */
	public void paketAl(Packet p)
	{
		synchronized(NetworkKuyrugu)
		{
			NetworkKuyrugu.add(p);
			NetworkKuyrugu.notifyAll();
		}
	}

	// GET fonksyonlarý ///////////////////////////////////////////////////////
	/**
	 * IP protokolüne olan referansý dönderir.
	 * @return IP sýnýfýndan bir nesne
	 */
	public IP getIP()
	{
		return ip;
	}
	//-------------------------------------------------------------------------
	/**
	 * ARP protokolüne olan referansý dönderir.
	 * @return ARP sýnýfýndan bir nesne
	 */
	public ARP getARP()
	{
		return arp;
	}
	//-------------------------------------------------------------------------
	/**
	 * Kullanýlan að baðdaþtýrýcý cihazýn MAC adresini dönderir.
	 * @return MAC adresi
	 */
	public MACAdresi getMAC()
	{
		return (new MACAdresi(agCihazi.mac_address));
	}
	//-------------------------------------------------------------------------
	/**
	 * Aða eriþmek için kullanýlan bu makinenin IP adresini dönderir.
	 * @return Makinenin IP adresi
	 */
	public Inet4Address getIPAdresi()
	{
		return IPAdresi;
	}

	// SET fonksyonlarý ///////////////////////////////////////////////////////
	/**
	 * IP protokolüne olan referansý deðiþtirir.
	 * @param ip IP sýnýfýndan bir nesne
	 */
	public void setIP(IP ip)
	{
		this.ip = ip;
	}
	//-------------------------------------------------------------------------
	/**
	 * ARP protokolüne olan referansý deðiþtirir.
	 * @param arp ARP sýnýfýndan bir nesne
	 */
	public void setARP(ARP arp)
	{
		this.arp = arp;
	}
	//-------------------------------------------------------------------------
	/**
	 * Aða eriþmek için kullanýlan IP adresini deðiþtirir.
	 * @param IPAdresi IP adresi
	 */
	public void setIPAdresi(Inet4Address IPAdresi)
	{
		this.IPAdresi = IPAdresi;
	}
	
	// Thread fonksyonlarý ////////////////////////////////////////////////////
	/**
	 * Thread'in giriþ noktasý.
	 * Bu fonksyon jpcaptor kütüphanesini çalýþtýrýr ve paketleri almak için
	 * bir döngü tanýmlar.
	 */
	public void run()
	{
		//IP adresime göre hangi Að cihazýný kullandýðým tespit ediliyor... 
		//IP adresimi 4 byte'lýk bir byte dizisine çevir
		byte[] bip = IPAdresi.getAddress();
		//Bu makinedeki tüm að cihazlarýnýn listesini al
		NetworkInterface[] tumCihazlar = JpcapCaptor.getDeviceList();
		//Geçerli IP adresini kullanan cihazý bul
		device_dongusu:	for(NetworkInterface birCihaz:tumCihazlar){ //tüm cihazlar için
			for(NetworkInterfaceAddress addr:birCihaz.addresses){ //bu cihazdaki tüm adresler için
				if(!(addr.address instanceof Inet4Address)) continue; //IP4 deðilse geç..
				byte[] bif = addr.address.getAddress(); //adresi byte dizisine çevir
				if(Arrays.equals(bip,bif)){ //iki dizisi karþýlaþtýr
					agCihazi = birCihaz;//eþitse cihaz bulundu, döngüyü sonlandýr
					break device_dongusu;
				}
			}
		}
		//Bu IP'yi kullanan bir að cihazý bulunamadýysa hata ver
		if(agCihazi==null)
			throw new IllegalArgumentException("ip hatalý");
		
		//tespit edilen cihaz için paket yakalayýcý nesneyi oluþtur
		try
		{
			captor = JpcapCaptor.openDevice(agCihazi,2000,false,3000);
		} 
		catch (IOException e)
		{
			return;
		}

		//göndermek için nesne referansý al
		sender = captor.getJpcapSenderInstance();
		
		//paketleri almak için sýnýftan bir nesne oluþtur
		receiver = new PaketAlicisi(arp, ip);
		
		//peketleri yollamak için sýnýftan bir nesne oluþtur
		paketYollayicisi = new PaketYollayicisi(sender, NetworkKuyrugu);
		//paket yollamak için gerekli Thread'i çalýþtýr
		paketYollayicisi.start();
		
		//paketleri alan döngü
		while(calisiyor)
		{
			captor.loopPacket(-1, receiver);
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu Thread'in çalýþmasýný durdurmak için
	 */
	public void durdur()
	{
		calisiyor = false;
		captor.breakLoop();
		paketYollayicisi.durdur();
		captor.close();
	}
}