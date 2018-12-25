package test;

import tcpip.MyTCPIP;
import tcpip.map.*;
import java.net.*;

public class TestMAP 
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
		
		//Mesaj� g�nder
		try
		{
			Inet4Address karsiAdres = (Inet4Address)Inet4Address.getByAddress(new byte[]{(byte)192,(byte)168,(byte)1,(byte)4});
			MAP map = ornekTCPIP.getMAP();
			
			MAPDinleyici dinleyici = new BirMAPDinleyici();
			map.setMAPDinleyici(dinleyici);
			
			map.mesajYolla(karsiAdres, "Merhaba..!".getBytes());
		}
		catch (Exception e)
		{
			//bo�...
		}

		//paketin g�nderilip al�nmas� i�in zaman ver
		Thread.sleep(20000);

		//Protokol� sonland�r
		ornekTCPIP.durdur();
		
		//Protokol�n durmas� i�in zaman ver
		Thread.sleep(1000);
	}
}
