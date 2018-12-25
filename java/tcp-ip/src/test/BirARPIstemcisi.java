package test;

import java.net.Inet4Address;

import tcpip.MACAdresi;
import tcpip.arp.ARPIstemcisi;

public class BirARPIstemcisi implements ARPIstemcisi 
{
	public void MACBulunamadi(long talepID, Inet4Address ipAdresi) 
	{
		System.out.println(ipAdresi+" a�da bulanamad�.");
	}

	public void MACal(long talepID, Inet4Address ipAdresi, MACAdresi macAdresi,
			boolean onBellek) 
	{
		System.out.println("Al�nma zaman�   = "+System.currentTimeMillis());
		System.out.println(ipAdresi+" -> "+macAdresi+"( "+((onBellek)?("�nbellekten al�nd�"):("ger�ek sorgu yolland�"))+" )");
	}
}
