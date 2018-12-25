package tcpip.arp;

import java.net.Inet4Address;
import tcpip.*;

/**
 * ARP önbelleðindeki bir kaydý temsil eder.
 * 
 * @author Halil Ýbrahim KALKAN
 */
public class ARPKaydi implements Comparable<ARPKaydi> 
{
	// Cache tablosundaki alanlar
	public short 			ProtokolTipi;
	public Inet4Address 	IPAdresi;
	public MACAdresi 		HardwareAdresi;
	public long				TimeStamp;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public ARPKaydi(short ProtokolTipi, Inet4Address IPAdresi, MACAdresi HardwareAdresilong, long TimeStamp)
	{
		this.ProtokolTipi = ProtokolTipi;
		this.IPAdresi = IPAdresi;
		this.HardwareAdresi = HardwareAdresilong;
		this.TimeStamp = TimeStamp;
	}
	
	//-------------------------------------------------------------------------
	public boolean equals(Object o)
	{
		ARPKaydi k = (ARPKaydi)o;
		if(ProtokolTipi==k.ProtokolTipi &&
		    IPAdresi.equals(k.IPAdresi) &&
		    HardwareAdresi.equals(k.HardwareAdresi) &&
		    TimeStamp==k.TimeStamp)
			return true;
		else
			return false;
	}
	//-------------------------------------------------------------------------
	public int compareTo(ARPKaydi kayit) 
	{
		byte[] adres1 = IPAdresi.getAddress();
		byte[] adres2 = kayit.IPAdresi.getAddress();

		for(int i=0;i<4;i++)
			if(adres1[i]!=adres2[i])
				return (adres1[i]-adres2[i]);
		
		return 0;
	}
}
