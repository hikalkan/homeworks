package tcpip;

import java.util.Comparator;
import java.net.*;

/**
 * Inet4Address tipinde iki nesneyi karþýlaþtýrmak amacýyla geliþtirilmiþtir.
 * Java'nýn hazýr koleksiyon sýnýflarýnda comparator olarak kullanýlýr.
 * 
 * @author Halil Ýbrahim KALKAN
 */
public class Inet4AddressKarsilastirici implements Comparator<Inet4Address> 
{
	/**
	 * Karþýlaþtýrmayý gerçekleþtiren fonksyon.
	 * @param ipAdresi1 Birinci IP adresi
	 * @param ipAdresi2 Ýkinci IP adresi
	 * @return karþýlaþtýrma sonucu
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
