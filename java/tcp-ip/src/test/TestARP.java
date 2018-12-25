package test;

import tcpip.MyTCPIP;
import java.net.*;
/**
 * ARP protokol�n� test eden kod. Bir ARP sorgusu g�nderir ve
 * gelen cevab� ekranda yazar.
 * @author Halil �brahim Kalkan
 */
public class TestARP 
{
	public static void main(String[] args) throws Exception 
	{
		//Protokol� sarmalayan s�n�ftan bir �rnek (nesne) olu�tur
		MyTCPIP ornekTCPIP = new MyTCPIP();
		
		//TCP/IP protokol�n�n �al��mas�n� ba�lat
		if(!ornekTCPIP.baslat())
			return; //hata varsa ��k
		
		//haz�rlanmas� i�in zaman ver
		Thread.sleep(1000);
		
		//ARP iste�i g�nder
		try
		{
			//bir sorgu yolla
			System.out.println("G�nderme Zaman� = "+System.currentTimeMillis());
			ornekTCPIP.getARP().MACIstegi(0,
					(Inet4Address)InetAddress.getByName("192.168.1.4"),
					new BirARPIstemcisi());

			//5 saniye bekle
			//Thread.sleep(5000);
			
			//Ayn� adres i�in bir sorgu daha yolla
			System.out.println("G�nderme Zaman� = "+System.currentTimeMillis());
			ornekTCPIP.getARP().MACIstegi(0,
					(Inet4Address)InetAddress.getByName("192.168.1.1"),
					new BirARPIstemcisi());

			//5 saniye bekle
			//Thread.sleep(5000);
			
			//Ayn� adres i�in bir sorgu daha yolla
			System.out.println("G�nderme Zaman� = "+System.currentTimeMillis());
			ornekTCPIP.getARP().MACIstegi(0,
					(Inet4Address)InetAddress.getByName("192.168.1.3"),
					new BirARPIstemcisi());
		}
		catch (Exception e)
		{
			//bo�...
		}

		//sorgunun g�nderilip al�nmas� i�in zaman ver
		Thread.sleep(20000);

		//Protokol� sonland�r
		ornekTCPIP.durdur();
		
		//Protokol�n durmas� i�in zaman ver
		Thread.sleep(1000);
	}
}