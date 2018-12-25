package test;

import tcpip.*;
import tcpip.tcp.*;
import java.net.*;
import java.io.*;

public class TestTCP 
{
	public static void main(String[] args) throws Exception
	{
//		Protokol� sarmalayan s�n�ftan bir �rnek (nesne) olu�tur
		MyTCPIP ornekTCPIP = new MyTCPIP();
		
		//TCP/IP protokol�n�n �al��mas�n� ba�lat
		if(!ornekTCPIP.baslat())
			return; //hata varsa ��k
		
		//haz�rlanmas� i�in zaman ver
		Thread.sleep(1000);
		
		//---------------------------------------------------------------------
		
		TCPBaglantisi s = TCPBaglantisi.baglantiOlustur(
				Inet4Address.getByName("localhost"),
				1928
				);
		
		//OutputStream os = s.getOutputStream();
		//os.write(19);
		
		s.kapat();
		
		//---------------------------------------------------------------------
		
		//Protokol� sonland�r
		ornekTCPIP.durdur();
		
		//Protokol�n durmas� i�in zaman ver
		Thread.sleep(1000);
	}
}
