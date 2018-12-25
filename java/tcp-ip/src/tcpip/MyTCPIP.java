package tcpip;

import tcpip.arp.*;
import tcpip.map.*;
import tcpip.ip.*;
import tcpip.tcp.*;
import tcpip.udp.*;
import tcpip.nal.*;
import java.net.*;

/**
 * Tüm protokolleri kullanýma hazýrlayan ve sarmalayan sýnýf.
 * MyTCPIP protokol kümesini kullanabilmek için bu sýnýftan bir
 * nesne oluþturulur ve baslat() yöntemi çaðýrýlýr.
 * 
 * @author Halil Ýbrahim KALKAN
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

	// üye deðiþkenler ////////////////////////////////////////////////////////
	private Ayarlar								ayarlar;
	
	// Protokolleri ve katmanlarý elde eden fonksyonlar ///////////////////////
	/**
	 * Network Access Layer katmanýný uygulayan nesneye bir referans dönderir.
	 * @return MyTCPIP içerisinde çalýþan NetworkAccessLayer nesnesi
	 */
	public NetworkAccessLayer getNAL()
	{
		return nal;
	}
	/**
	 * ARP protokolünü uygulayan nesneye bir referans dönderir.
	 * @return MyTCPIP içerisinde çalýþan ARP nesnesi
	 */
	public ARP getARP()
	{
		return arp;
	}
	/**
	 * IP protokolünü uygulayan nesneye bir referans dönderir.
	 * @return MyTCPIP içerisinde çalýþan IP nesnesi
	 */
	public IP getIP()
	{
		return ip;
	}
	/**
	 * TCP protokolünü uygulana nesneye bir referans dönderir.
	 * @return MyTCPIP içerisinde çalýþan TCP nesnesi
	 */
	public TCP getTCP()
	{
		return tcp;
	}
	/**
	 * UDP protokolünü uygulana nesneye bir referans dönderir.
	 * @return MyTCPIP içerisinde çalýþan UDP nesnesi
	 */
	public UDP getUDP()
	{
		return udp;
	}
	/**
	 * MAP protokolünü uygulayan nesneye bir referans dönderir.
	 * @return MyTCPIP içerisinde çalýþan MAP nesnesi
	 */
	public MAP getMAP()
	{
		return map;
	}
	
	// Sistemi hazýrlayan fonksyon ////////////////////////////////////////////
	/**
	 * MyTCPIP protokol kümesini çalýþmaya hazýrlar.
	 * @return iþlemin baþarýlý olup olmama durumu
	 */
	public boolean baslat()
	{
		try
		{
			//JpCap'den soyutlamak için bir nesne oluþtur
			nal = new NetworkAccessLayer();
			//ARP protokolünü uygulayan nesneyi oluþtur
			arp = new ARP(nal);
			//IP protokolünü uygulayan nesneyi oluþtur
			ip = new IP(nal);
			//TCP protokolünü uygulayan nesneyi oluþtur
			tcp = new TCP(ip);
			//UDP protokolüne uygulayan nesneyi oluþtur
			udp = new UDP(ip);
			//MAP protokolünü uygulayan nesneyi oluþtur
			map = new MAP(ip);
			
			//Katmanlarýn birbirine olan referanslarýný ayarla
			nal.setARP(arp);
			nal.setIP(ip);
			ip.setARP(arp);
			ip.setTCP(tcp);
			ip.setUDP(udp);
			ip.setMAP(map);
			
			//Ayar dosyasýndan geçerli ayarlarý al
			if((ayarlar=Ayarlar.yukle("MyTCPIP.ini"))==null)
				return false; //alamazsan çýk

			//Network Access Layer için ayalamalar
			nal.setIPAdresi((Inet4Address)ayarlar.getIPAdresi());
			
			//IP protokolü için ayarlamalar
			ip.setIPAdresi((Inet4Address)ayarlar.getIPAdresi());
			ip.setAltAgMaskesi((Inet4Address)ayarlar.getAltAgMaskesi());
			ip.setVarsayilanAgGecidi((Inet4Address)ayarlar.getVarsayilanAgGecidi());
			
			//protokolleri çalýþtýr
			nal.start();
			arp.start();
			ip.start();
		}
		catch(Exception e)
		{
			//hata var, false dönder
			return false;
		}
		//hata yok, true dönder
		return true;
	}
	
	// Sistemi durduran fonksyon //////////////////////////////////////////////
	/**
	 * MyTCPIP protokol kümesindeki tüm protokollerin çalýþmasýný durdurur. 
	 */
	public void durdur()
	{
		ip.durdur();
		arp.durdur();		
		nal.durdur();
	}	
}
