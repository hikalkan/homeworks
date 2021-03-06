package tcpip;


import java.util.Arrays;

/**
 * Bu sınıf bir MAC adresini temsil eder. MAC adreslerinin aktarımının
 * ve saklanmasının kolaylaştırılması amacıyla tasarlanmıştır.
 * 
 * @author Halil İbrahim KALKAN
 */
public class MACAdresi implements Comparable 
{
	/** mac adresini saklayan byte dizisi */
	private byte[] adres = null;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public MACAdresi()
	{
		 adres = new byte[6];
	}
	//-------------------------------------------------------------------------
	public MACAdresi(byte[] adres)
	{
		this.adres = adres;
	}
	//-------------------------------------------------------------------------
	public MACAdresi(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6)
	{
		adres = new byte[]{b1, b2, b3, b4, b5, b6};
	}
	
	// GET fonksyonları ///////////////////////////////////////////////////////
	public byte[] getAdres()
	{
		return adres;
	}
	
	// SET fonksyonları ///////////////////////////////////////////////////////
	public void setAdres(byte[] adres)
	{
		this.adres = adres;
	}
	//-------------------------------------------------------------------------
	public void setAdres(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6)
	{
		adres = new byte[]{b1, b2, b3, b4, b5, b6};
	}
	
	// Karşılaştırma fonksyonları /////////////////////////////////////////////
	public boolean equals(Object o)
	{
		return (Arrays.equals(((MACAdresi)o).adres, this.adres));
	}
	//-------------------------------------------------------------------------
	public int compareTo(Object o)
	{
		MACAdresi mac2 = (MACAdresi)o;
		for(int i=0;i<6;i++)
		{
			if(adres[i]!=mac2.adres[i])
				return adres[i]-mac2.adres[i];
		}
		return 0;
	}
	
	// Diğer  fonksyonlar /////////////////////////////////////////////////////
	public String toString()
	{
		return (Integer.toHexString(adres[0]&0xff)+":"+
				Integer.toHexString(adres[1]&0xff)+":"+
				Integer.toHexString(adres[2]&0xff)+":"+
				Integer.toHexString(adres[3]&0xff)+":"+
				Integer.toHexString(adres[4]&0xff)+":"+
				Integer.toHexString(adres[5]&0xff));
	}
}