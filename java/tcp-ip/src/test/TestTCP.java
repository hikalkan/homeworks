package test;

import tcpip.*;
import tcpip.tcp.*;
import java.net.*;
import java.io.*;

public class TestTCP 
{
	public static void main(String[] args) throws Exception
	{
//		Protokolü sarmalayan sýnýftan bir örnek (nesne) oluþtur
		MyTCPIP ornekTCPIP = new MyTCPIP();
		
		//TCP/IP protokolünün çalýþmasýný baþlat
		if(!ornekTCPIP.baslat())
			return; //hata varsa çýk
		
		//hazýrlanmasý için zaman ver
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
		
		//Protokolü sonlandýr
		ornekTCPIP.durdur();
		
		//Protokolün durmasý için zaman ver
		Thread.sleep(1000);
	}
}
