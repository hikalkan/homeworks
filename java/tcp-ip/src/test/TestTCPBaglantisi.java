package test;

import java.io.*;
import java.net.Inet4Address;

import tcpip.MyTCPIP;
import tcpip.tcp.*;

public class TestTCPBaglantisi 
{
	public static void main(String args[]) throws Exception
	{
		//Protokolü sarmalayan sýnýftan bir örnek (nesne) oluþtur
		MyTCPIP ornekTCPIP = new MyTCPIP();
		
		//TCP/IP protokolünün çalýþmasýný baþlat
		if(!ornekTCPIP.baslat())
			return; //hata varsa çýk
		
		//hazýrlanmasý için zaman ver
		Thread.sleep(1000);
		
		//---------------------------------------------------------------------
		//Inet4Address karsiAdres = (Inet4Address)Inet4Address.getByName("www.google.com.tr");
		//Inet4Address karsiAdres = (Inet4Address)Inet4Address.getByName("www.altavista.com");
		Inet4Address karsiAdres = (Inet4Address)Inet4Address.getByAddress(new byte[]{(byte)192,(byte)168,(byte)1,(byte)3});
		
		System.out.println(karsiAdres);
		TCPBaglantisi s = TCPBaglantisi.baglantiOlustur(
				karsiAdres,
				10049
				);
		
		if(s!=null)
		{
			System.out.println("baþarýlý");
			Thread.sleep(3000);
			TCPOutputStream os = s.getOutputStream();
			
			byte b[] = new byte[] {
					0x53,					//83: baþlangýç byte'ý
					0x00, 0x09,				// 9: paket uzunluðu
					0x03,					// 3: çarpma iþlemi
					0x00, 0x00, 0x00, 0x11,	//17: birinci sayý
					0x00, 0x00, 0x00, 0x03, // 3: ikinci sayý
					0x55					//85: sonlandýrma byte'ý
			        };
			os.yaz(b);
			

			Thread.sleep(30000);
		}
		else
		{
			System.out.println("baþarýsýz");
		}
		
		//---------------------------------------------------------------------
		
		//Protokolü sonlandýr
		ornekTCPIP.durdur();
		
		//Protokolün durmasý için zaman ver
		Thread.sleep(1000);
	}
}
