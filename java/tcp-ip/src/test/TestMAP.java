package test;

import tcpip.MyTCPIP;
import tcpip.map.*;
import java.net.*;

public class TestMAP 
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
		
		//Mesajý gönder
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
			//boþ...
		}

		//paketin gönderilip alýnmasý için zaman ver
		Thread.sleep(20000);

		//Protokolü sonlandýr
		ornekTCPIP.durdur();
		
		//Protokolün durmasý için zaman ver
		Thread.sleep(1000);
	}
}
