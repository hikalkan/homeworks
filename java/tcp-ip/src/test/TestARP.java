package test;

import tcpip.MyTCPIP;
import java.net.*;
/**
 * ARP protokolünü test eden kod. Bir ARP sorgusu gönderir ve
 * gelen cevabý ekranda yazar.
 * @author Halil Ýbrahim Kalkan
 */
public class TestARP 
{
	public static void main(String[] args) throws Exception 
	{
		//Protokolü sarmalayan sýnýftan bir örnek (nesne) oluþtur
		MyTCPIP ornekTCPIP = new MyTCPIP();
		
		//TCP/IP protokolünün çalýþmasýný baþlat
		if(!ornekTCPIP.baslat())
			return; //hata varsa çýk
		
		//hazýrlanmasý için zaman ver
		Thread.sleep(1000);
		
		//ARP isteði gönder
		try
		{
			//bir sorgu yolla
			System.out.println("Gönderme Zamaný = "+System.currentTimeMillis());
			ornekTCPIP.getARP().MACIstegi(0,
					(Inet4Address)InetAddress.getByName("192.168.1.4"),
					new BirARPIstemcisi());

			//5 saniye bekle
			//Thread.sleep(5000);
			
			//Ayný adres için bir sorgu daha yolla
			System.out.println("Gönderme Zamaný = "+System.currentTimeMillis());
			ornekTCPIP.getARP().MACIstegi(0,
					(Inet4Address)InetAddress.getByName("192.168.1.1"),
					new BirARPIstemcisi());

			//5 saniye bekle
			//Thread.sleep(5000);
			
			//Ayný adres için bir sorgu daha yolla
			System.out.println("Gönderme Zamaný = "+System.currentTimeMillis());
			ornekTCPIP.getARP().MACIstegi(0,
					(Inet4Address)InetAddress.getByName("192.168.1.3"),
					new BirARPIstemcisi());
		}
		catch (Exception e)
		{
			//boþ...
		}

		//sorgunun gönderilip alýnmasý için zaman ver
		Thread.sleep(20000);

		//Protokolü sonlandýr
		ornekTCPIP.durdur();
		
		//Protokolün durmasý için zaman ver
		Thread.sleep(1000);
	}
}