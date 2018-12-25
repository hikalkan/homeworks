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
 * IP ve ARP protokollerinin a�a eri�mesini sa�layan s�n�f.
 * Bir thread olarak �al���r ve a�dan paket geldik�e bunlar�
 * ARPKuyrugu ve IPKuyrugu nesnelerine aktar�r ve bu kuyruklarda
 * paket bekleyen thread'leri bilgilendirir (notifyAll()).
 * Ayr�ca paketleri g�ndermek i�in de paketYollayicisi adl� bir
 * thread olu�turur ve bunu NetworkKuyrugu nesnesine ba�lar.
 * 
 * @author Halil �brahim KALKAN
 */
public class NetworkAccessLayer extends Thread 
{
	/** ileti�imin �st�nden sa�lant��� NIC */
	private NetworkInterface 				agCihazi;
	/** Bu makinan�n ge�erli IP adresi */
	private Inet4Address					IPAdresi;
	/** IP protokol�ne referans */
	private IP								ip;
	/** ARP protokol�ne referans */
	private ARP								arp;
	/** paket yakalamak i�in kullan�lan nesne */
	private JpcapCaptor 					captor;
	/** paket g�ndermek i�in kullan�lan nesne */
	private JpcapSender 					sender;
	/** paket g�ndermek i�in kullan�lan Thread */
	private PaketYollayicisi				paketYollayicisi;
	/** paketleri almak i�in PacketReceiver aray�z�n� uygulayan s�n�ftan bir nesne */
	private PaketAlicisi					receiver;
	/** �st katmanlardan bu katmana gelen paketlerin girdi�i tampon kuyruk */
	private LinkedBlockingQueue<Packet>		NetworkKuyrugu;
	/** protokol� y�netmek i�in bayrak */
	private volatile boolean				calisiyor = true;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public NetworkAccessLayer() 
	{
		//Bu Thread'e bir isim ver
		setName("NAL");
		//gelen paketler i�in kuyru�u olu�tur
		NetworkKuyrugu = new LinkedBlockingQueue<Packet>();
	}
	
	// Public Metodlar ////////////////////////////////////////////////////////
	/**
	 * Bu fonksyon �st katmanlardan a�a paket yollanmas� amac�yla tasarland�.
	 * Gelen paketler NetworkKuyrugu adl� bir kuyru�a eklenir ve bu kuyrukta
	 * paket bekleyen Thread'ler bilgilendirilir (notifyAll()).
	 * @param p a�a g�nderilecek paket
	 */
	public void paketAl(Packet p)
	{
		synchronized(NetworkKuyrugu)
		{
			NetworkKuyrugu.add(p);
			NetworkKuyrugu.notifyAll();
		}
	}

	// GET fonksyonlar� ///////////////////////////////////////////////////////
	/**
	 * IP protokol�ne olan referans� d�nderir.
	 * @return IP s�n�f�ndan bir nesne
	 */
	public IP getIP()
	{
		return ip;
	}
	//-------------------------------------------------------------------------
	/**
	 * ARP protokol�ne olan referans� d�nderir.
	 * @return ARP s�n�f�ndan bir nesne
	 */
	public ARP getARP()
	{
		return arp;
	}
	//-------------------------------------------------------------------------
	/**
	 * Kullan�lan a� ba�da�t�r�c� cihaz�n MAC adresini d�nderir.
	 * @return MAC adresi
	 */
	public MACAdresi getMAC()
	{
		return (new MACAdresi(agCihazi.mac_address));
	}
	//-------------------------------------------------------------------------
	/**
	 * A�a eri�mek i�in kullan�lan bu makinenin IP adresini d�nderir.
	 * @return Makinenin IP adresi
	 */
	public Inet4Address getIPAdresi()
	{
		return IPAdresi;
	}

	// SET fonksyonlar� ///////////////////////////////////////////////////////
	/**
	 * IP protokol�ne olan referans� de�i�tirir.
	 * @param ip IP s�n�f�ndan bir nesne
	 */
	public void setIP(IP ip)
	{
		this.ip = ip;
	}
	//-------------------------------------------------------------------------
	/**
	 * ARP protokol�ne olan referans� de�i�tirir.
	 * @param arp ARP s�n�f�ndan bir nesne
	 */
	public void setARP(ARP arp)
	{
		this.arp = arp;
	}
	//-------------------------------------------------------------------------
	/**
	 * A�a eri�mek i�in kullan�lan IP adresini de�i�tirir.
	 * @param IPAdresi IP adresi
	 */
	public void setIPAdresi(Inet4Address IPAdresi)
	{
		this.IPAdresi = IPAdresi;
	}
	
	// Thread fonksyonlar� ////////////////////////////////////////////////////
	/**
	 * Thread'in giri� noktas�.
	 * Bu fonksyon jpcaptor k�t�phanesini �al��t�r�r ve paketleri almak i�in
	 * bir d�ng� tan�mlar.
	 */
	public void run()
	{
		//IP adresime g�re hangi A� cihaz�n� kulland���m tespit ediliyor... 
		//IP adresimi 4 byte'l�k bir byte dizisine �evir
		byte[] bip = IPAdresi.getAddress();
		//Bu makinedeki t�m a� cihazlar�n�n listesini al
		NetworkInterface[] tumCihazlar = JpcapCaptor.getDeviceList();
		//Ge�erli IP adresini kullanan cihaz� bul
		device_dongusu:	for(NetworkInterface birCihaz:tumCihazlar){ //t�m cihazlar i�in
			for(NetworkInterfaceAddress addr:birCihaz.addresses){ //bu cihazdaki t�m adresler i�in
				if(!(addr.address instanceof Inet4Address)) continue; //IP4 de�ilse ge�..
				byte[] bif = addr.address.getAddress(); //adresi byte dizisine �evir
				if(Arrays.equals(bip,bif)){ //iki dizisi kar��la�t�r
					agCihazi = birCihaz;//e�itse cihaz bulundu, d�ng�y� sonland�r
					break device_dongusu;
				}
			}
		}
		//Bu IP'yi kullanan bir a� cihaz� bulunamad�ysa hata ver
		if(agCihazi==null)
			throw new IllegalArgumentException("ip hatal�");
		
		//tespit edilen cihaz i�in paket yakalay�c� nesneyi olu�tur
		try
		{
			captor = JpcapCaptor.openDevice(agCihazi,2000,false,3000);
		} 
		catch (IOException e)
		{
			return;
		}

		//g�ndermek i�in nesne referans� al
		sender = captor.getJpcapSenderInstance();
		
		//paketleri almak i�in s�n�ftan bir nesne olu�tur
		receiver = new PaketAlicisi(arp, ip);
		
		//peketleri yollamak i�in s�n�ftan bir nesne olu�tur
		paketYollayicisi = new PaketYollayicisi(sender, NetworkKuyrugu);
		//paket yollamak i�in gerekli Thread'i �al��t�r
		paketYollayicisi.start();
		
		//paketleri alan d�ng�
		while(calisiyor)
		{
			captor.loopPacket(-1, receiver);
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu Thread'in �al��mas�n� durdurmak i�in
	 */
	public void durdur()
	{
		calisiyor = false;
		captor.breakLoop();
		paketYollayicisi.durdur();
		captor.close();
	}
}