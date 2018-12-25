package tcpip;

import tcpip.arp.*;
import tcpip.map.*;
import tcpip.ip.*;
import tcpip.tcp.*;
import tcpip.udp.*;
import tcpip.nal.*;
import java.net.*;

/**
 * T�m protokolleri kullan�ma haz�rlayan ve sarmalayan s�n�f.
 * MyTCPIP protokol k�mesini kullanabilmek i�in bu s�n�ftan bir
 * nesne olu�turulur ve baslat() y�ntemi �a��r�l�r.
 * 
 * @author Halil �brahim KALKAN
 */
public class MyTCPIP
{
	// Protokoller ve katmanlar ///////////////////////////////////////////////
	private NetworkAccessLayer 					nal;
	private ARP 								arp;
	private IP									ip;
	private TCP									tcp;
	private UDP									udp;
	private MAP									map;

	// �ye de�i�kenler ////////////////////////////////////////////////////////
	private Ayarlar								ayarlar;
	
	// Protokolleri ve katmanlar� elde eden fonksyonlar ///////////////////////
	/**
	 * Network Access Layer katman�n� uygulayan nesneye bir referans d�nderir.
	 * @return MyTCPIP i�erisinde �al��an NetworkAccessLayer nesnesi
	 */
	public NetworkAccessLayer getNAL()
	{
		return nal;
	}
	/**
	 * ARP protokol�n� uygulayan nesneye bir referans d�nderir.
	 * @return MyTCPIP i�erisinde �al��an ARP nesnesi
	 */
	public ARP getARP()
	{
		return arp;
	}
	/**
	 * IP protokol�n� uygulayan nesneye bir referans d�nderir.
	 * @return MyTCPIP i�erisinde �al��an IP nesnesi
	 */
	public IP getIP()
	{
		return ip;
	}
	/**
	 * TCP protokol�n� uygulana nesneye bir referans d�nderir.
	 * @return MyTCPIP i�erisinde �al��an TCP nesnesi
	 */
	public TCP getTCP()
	{
		return tcp;
	}
	/**
	 * UDP protokol�n� uygulana nesneye bir referans d�nderir.
	 * @return MyTCPIP i�erisinde �al��an UDP nesnesi
	 */
	public UDP getUDP()
	{
		return udp;
	}
	/**
	 * MAP protokol�n� uygulayan nesneye bir referans d�nderir.
	 * @return MyTCPIP i�erisinde �al��an MAP nesnesi
	 */
	public MAP getMAP()
	{
		return map;
	}
	
	// Sistemi haz�rlayan fonksyon ////////////////////////////////////////////
	/**
	 * MyTCPIP protokol k�mesini �al��maya haz�rlar.
	 * @return i�lemin ba�ar�l� olup olmama durumu
	 */
	public boolean baslat()
	{
		try
		{
			//JpCap'den soyutlamak i�in bir nesne olu�tur
			nal = new NetworkAccessLayer();
			//ARP protokol�n� uygulayan nesneyi olu�tur
			arp = new ARP(nal);
			//IP protokol�n� uygulayan nesneyi olu�tur
			ip = new IP(nal);
			//TCP protokol�n� uygulayan nesneyi olu�tur
			tcp = new TCP(ip);
			//UDP protokol�ne uygulayan nesneyi olu�tur
			udp = new UDP(ip);
			//MAP protokol�n� uygulayan nesneyi olu�tur
			map = new MAP(ip);
			
			//Katmanlar�n birbirine olan referanslar�n� ayarla
			nal.setARP(arp);
			nal.setIP(ip);
			ip.setARP(arp);
			ip.setTCP(tcp);
			ip.setUDP(udp);
			ip.setMAP(map);
			
			//Ayar dosyas�ndan ge�erli ayarlar� al
			if((ayarlar=Ayarlar.yukle("MyTCPIP.ini"))==null)
				return false; //alamazsan ��k

			//Network Access Layer i�in ayalamalar
			nal.setIPAdresi((Inet4Address)ayarlar.getIPAdresi());
			
			//IP protokol� i�in ayarlamalar
			ip.setIPAdresi((Inet4Address)ayarlar.getIPAdresi());
			ip.setAltAgMaskesi((Inet4Address)ayarlar.getAltAgMaskesi());
			ip.setVarsayilanAgGecidi((Inet4Address)ayarlar.getVarsayilanAgGecidi());
			
			//protokolleri �al��t�r
			nal.start();
			arp.start();
			ip.start();
		}
		catch(Exception e)
		{
			//hata var, false d�nder
			return false;
		}
		//hata yok, true d�nder
		return true;
	}
	
	// Sistemi durduran fonksyon //////////////////////////////////////////////
	/**
	 * MyTCPIP protokol k�mesindeki t�m protokollerin �al��mas�n� durdurur. 
	 */
	public void durdur()
	{
		ip.durdur();
		arp.durdur();		
		nal.durdur();
	}	
}
