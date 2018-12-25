package tcpip.tcp;

import tcpip.ip.IP;
import jpcap.packet.*;
import java.net.*;
import java.util.*;

/**
 * TCP protokol�n� uygulayan s�n�f.
 * 
 * @author Halil �brahim KALKAN 
 */
public class TCP 
{
	// Private de�i�kenler ////////////////////////////////////////////////////
	/** IP protokol�n� kullanabilmek i�in gerekli referans */
	private IP									ip;
	/** Kullan�lmayan portlar�n listesi */
	private LinkedList<Integer>					bosPortListesi;
	/** A��k ve aktif olan port numaralar�n� kullanan ba�lant�lar */
	private TreeMap<Integer, TCPDinleyici>		acikBaglantilar;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	/**
	 * IP protokol�ne olan referans� ayarlanm�� bir TCP nesnesi olu�turur.
	 * @param ip IP protokol�n� uygulanan nesne
	 */
	public TCP(IP ip)
	{
		//IP protokol�ne olan referans al�n�yor
		this.ip = ip;
		//Bo� port listesini olu�tur
		bosPortListesi = new LinkedList<Integer>();
		//Sadece 30.000 ile 30.100 aras�ndaki portlar kullan�labilir!
		for(int i=10052; i<=10055; i++)
		{
			bosPortListesi.add(i);
		}
		//A��lan ba�lant�lar� y�netmen i�in a�a� yap�s� olu�turuluyor
		acikBaglantilar = new TreeMap<Integer, TCPDinleyici>();
		//Static TCP de�i�keni ayarlan�yor
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
	 * IP katman�ndan TCP'ye paket g�ndermek i�in kullan�lan fonksyon.
	 * @param yeniPaket A�dan gelen TCP paketi
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
		System.out.println("Al�nma Zaman�   = " + System.currentTimeMillis());
		System.out.println(str);
		*/
	}
	
	/**
	 * TCP mod�l� �zerinden a�a paket yollamak i�in kullan�lan fonksyon.
	 * @param p a�a yollanacak paket
	 */
	public synchronized void paketYolla(TCPPacket p, Inet4Address IPAdresi)
	{
		ip.paketYolla(p, IPAdresi);			
	}
	
	/**
	 * TCP mod�l� �zerinden a�a paket yollamak i�in kullan�lan fonksyon.
	 * @param p a�a yollanacak paket
	 */
	public synchronized void paketYolla(IPPacket p, Inet4Address IPAdresi)
	{
		ip.paketYolla(p, IPAdresi);			
	}
	
	/**
	 * Kullan�labilir port numaralar�ndan birisini d�nderir.
	 * @return kullan�labilir bir port numaras�
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
	 * �zel bir port numaras�n� istemek i�indir.
	 * @param portNo istenen port numaras�
	 * @return istek kar��lanabildiyse true, aksi halde false
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
	 * Kullan�lan bir port numaras�n� port havuzuna iade etmek i�indir.
	 * @param portNo serbest b�rak�lan port numaras�
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
	 * Bu makinenin IP adresini ��renmek i�in kullan�l�r
	 * @return bu makinenin ip adresi
	 */
	public InetAddress getIPAdresi()
	{
		return ip.getIPAdresi();		
	}
	
	// test kodlar� ///////////////////////////////////////////////////////////
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
			//bo�...
		}
	}
}
