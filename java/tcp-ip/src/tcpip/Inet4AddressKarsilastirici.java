package tcpip;

import java.util.Comparator;
import java.net.*;

/**
 * Inet4Address tipinde iki nesneyi kar��la�t�rmak amac�yla geli�tirilmi�tir.
 * Java'n�n haz�r koleksiyon s�n�flar�nda comparator olarak kullan�l�r.
 * 
 * @author Halil �brahim KALKAN
 */
public class Inet4AddressKarsilastirici implements Comparator<Inet4Address> 
{
	/**
	 * Kar��la�t�rmay� ger�ekle�tiren fonksyon.
	 * @param ipAdresi1 Birinci IP adresi
	 * @param ipAdresi2 �kinci IP adresi
	 * @return kar��la�t�rma sonucu
	 */
	public int compare(Inet4Address ipAdresi1, Inet4Address ipAdresi2) 
	{
		byte[] adres1 = ipAdresi1.getAddress();
		byte[] adres2 = ipAdresi2.getAddress();

		for(int i=0;i<4;i++)
			if(adres1[i]!=adres2[i])
				return (adres1[i]-adres2[i]);
		
		return 0;
	}
}
