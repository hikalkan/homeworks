package test;

import tcpip.MyTCPIP;
/**
 * Bu sýnýf IP protokolünü test etmek için yazýlmýþtýr.
 * IP protokolünü kullanarak aðda kendine bir mesaj yollar.
 * Mesaj geldiðinde bu mesaj gösterilir. TCP sýnýfýnýn 
 * deneme() fonksyonu kullanýlmýþtýr.
 * 
 * @author Halil Ýbrahim Kalkan
 */
public class TestOptions
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
			System.out.println("Gönderme Zamaný = "+System.currentTimeMillis());
			ornekTCPIP.getIP().opsiyonluYolla();
		}
		catch (Exception e)
		{
			//boþ...
		}

		//paketin gönderilip alýnmasý için zaman ver
		Thread.sleep(30000);

		//Protokolü sonlandýr
		ornekTCPIP.durdur();
		
		//Protokolün durmasý için zaman ver
		Thread.sleep(1000);
	}
}
