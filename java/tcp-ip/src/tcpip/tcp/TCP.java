package tcpip.tcp;

import tcpip.ip.IP;
import jpcap.packet.*;
import java.net.*;
import java.util.*;

/**
 * TCP protokolünü uygulayan sýnýf.
 * 
 * @author Halil Ýbrahim KALKAN 
 */
public class TCP 
{
	// Private deðiþkenler ////////////////////////////////////////////////////
	/** IP protokolünü kullanabilmek için gerekli referans */
	private IP									ip;
	/** Kullanýlmayan portlarýn listesi */
	private LinkedList<Integer>					bosPortListesi;
	/** Açýk ve aktif olan port numaralarýný kullanan baðlantýlar */
	private TreeMap<Integer, TCPDinleyici>		acikBaglantilar;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	/**
	 * IP protokolüne olan referansý ayarlanmýþ bir TCP nesnesi oluþturur.
	 * @param ip IP protokolünü uygulanan nesne
	 */
	public TCP(IP ip)
	{
		//IP protokolüne olan referans alýnýyor
		this.ip = ip;
		//Boþ port listesini oluþtur
		bosPortListesi = new LinkedList<Integer>();
		//Sadece 30.000 ile 30.100 arasýndaki portlar kullanýlabilir!
		for(int i=10052; i<=10055; i++)
		{
			bosPortListesi.add(i);
		}
		//Açýlan baðlantýlarý yönetmen için aðaç yapýsý oluþturuluyor
		acikBaglantilar = new TreeMap<Integer, TCPDinleyici>();
		//Static TCP deðiþkeni ayarlanýyor
		TCP.TCPNesnesi = this;
	}
	
	// Static fonksyonlar /////////////////////////////////////////////////////
	public static TCP getTCP()
	{
		return TCPNesnesi;
	}
	private static TCP TCPNesnesi = null;
	
	// Public fonksyonlar /////////////////////////////////////////////////////
	/**
	 * IP katmanýndan TCP'ye paket göndermek için kullanýlan fonksyon.
	 * @param yeniPaket Aðdan gelen TCP paketi
	 */
	public synchronized void paketAl(TCPPacket yeniPaket)
	{
		
		Integer portNo = yeniPaket.dst_port;
		TCPDinleyici dinleyici = acikBaglantilar.get(portNo);
		if(dinleyici != null)
		{
			dinleyici.paketAl(yeniPaket);
		}
		
		/*
		String str = new String(yeniPaket.data);
		System.out.println("Alýnma Zamaný   = " + System.currentTimeMillis());
		System.out.println(str);
		*/
	}
	
	/**
	 * TCP modülü üzerinden aða paket yollamak için kullanýlan fonksyon.
	 * @param p aða yollanacak paket
	 */
	public synchronized void paketYolla(TCPPacket p, Inet4Address IPAdresi)
	{
		ip.paketYolla(p, IPAdresi);			
	}
	
	/**
	 * TCP modülü üzerinden aða paket yollamak için kullanýlan fonksyon.
	 * @param p aða yollanacak paket
	 */
	public synchronized void paketYolla(IPPacket p, Inet4Address IPAdresi)
	{
		ip.paketYolla(p, IPAdresi);			
	}
	
	/**
	 * Kullanýlabilir port numaralarýndan birisini dönderir.
	 * @return kullanýlabilir bir port numarasý
	 */
	public synchronized int portNumarasiUret(TCPDinleyici dinleyici)
	{
		if(bosPortListesi.size()<=0)
		{
			return -1;
		}
		else
		{
			Integer portNo = bosPortListesi.poll();
			acikBaglantilar.put(portNo, dinleyici);
			return portNo;
		}
	}
	
	/**
	 * Özel bir port numarasýný istemek içindir.
	 * @param portNo istenen port numarasý
	 * @return istek karþýlanabildiyse true, aksi halde false
	 */
	public synchronized boolean portNumarasiTahsisEt(TCPDinleyici dinleyici, int portNo)
	{	
		if(bosPortListesi.contains(new Integer(portNo)))
		{
			bosPortListesi.remove(bosPortListesi.indexOf(new Integer(portNo)));
			acikBaglantilar.put(portNo, dinleyici);
			return true;
		}
		return false;
	}
	
	/**
	 * Kullanýlan bir port numarasýný port havuzuna iade etmek içindir.
	 * @param portNo serbest býrakýlan port numarasý
	 */
	public synchronized void portNumarasiBirak(int portNo)
	{
		if(!bosPortListesi.contains(new Integer(portNo)))
		{
			bosPortListesi.add(portNo);
			acikBaglantilar.remove(portNo);
		}
	}
	
	/**
	 * Bu makinenin IP adresini öðrenmek için kullanýlýr
	 * @return bu makinenin ip adresi
	 */
	public InetAddress getIPAdresi()
	{
		return ip.getIPAdresi();		
	}
	
	// test kodlarý ///////////////////////////////////////////////////////////
	public void deneme(String s)
	{
		TCPPacket p = new TCPPacket(10049,10049,0,0,
					false,false,false,false,
					true,true,true,true,
					0,0);
		
		p.data = s.getBytes();
		
		try
		{
			ip.paketYolla(p, (Inet4Address)Inet4Address.getByAddress(new byte[]{(byte)192,(byte)168,(byte)1,(byte)3}));
		}
		catch(Exception e)
		{
			//boþ...
		}
	}
}
