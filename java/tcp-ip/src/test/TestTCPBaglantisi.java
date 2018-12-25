package test;

import java.io.*;
import java.net.Inet4Address;

import tcpip.MyTCPIP;
import tcpip.tcp.*;

public class TestTCPBaglantisi 
{
	public static void main(String args[]) throws Exception
	{
		//Protokol� sarmalayan s�n�ftan bir �rnek (nesne) olu�tur
		MyTCPIP ornekTCPIP = new MyTCPIP();
		
		//TCP/IP protokol�n�n �al��mas�n� ba�lat
		if(!ornekTCPIP.baslat())
			return; //hata varsa ��k
		
		//haz�rlanmas� i�in zaman ver
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
			System.out.println("ba�ar�l�");
			Thread.sleep(3000);
			TCPOutputStream os = s.getOutputStream();
			
			byte b[] = new byte[] {
					0x53,					//83: ba�lang�� byte'�
					0x00, 0x09,				// 9: paket uzunlu�u
					0x03,					// 3: �arpma i�lemi
					0x00, 0x00, 0x00, 0x11,	//17: birinci say�
					0x00, 0x00, 0x00, 0x03, // 3: ikinci say�
					0x55					//85: sonland�rma byte'�
			        };
			os.yaz(b);
			

			Thread.sleep(30000);
		}
		else
		{
			System.out.println("ba�ar�s�z");
		}
		
		//---------------------------------------------------------------------
		
		//Protokol� sonland�r
		ornekTCPIP.durdur();
		
		//Protokol�n durmas� i�in zaman ver
		Thread.sleep(1000);
	}
}
