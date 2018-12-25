package test;

import tcpip.MyTCPIP;
/**
 * Bu s�n�f IP protokol�n� test etmek i�in yaz�lm��t�r.
 * IP protokol�n� kullanarak a�da kendine bir mesaj yollar.
 * Mesaj geldi�inde bu mesaj g�sterilir. TCP s�n�f�n�n 
 * deneme() fonksyonu kullan�lm��t�r.
 * 
 * @author Halil �brahim Kalkan
 */
public class TestOptions
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
			System.out.println("G�nderme Zaman� = "+System.currentTimeMillis());
			ornekTCPIP.getIP().opsiyonluYolla();
		}
		catch (Exception e)
		{
			//bo�...
		}

		//paketin g�nderilip al�nmas� i�in zaman ver
		Thread.sleep(30000);

		//Protokol� sonland�r
		ornekTCPIP.durdur();
		
		//Protokol�n durmas� i�in zaman ver
		Thread.sleep(1000);
	}
}
