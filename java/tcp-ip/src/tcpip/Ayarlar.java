package tcpip;

import java.io.*;
import java.net.*;

/**
 * MyTCPIP protokol kümesinin baþlangýç ayarlarýný saklayan dosyayla
 * iliþkili sýnýf. Ayarlar bu sýnýf üzerinden ayar dosyasýndan alýnýr
 * ve yine bu sýnýf aracýlýðýyla deðiþtirilip kaydedilebilir.
 * 
 * @author Halil Ýbrahim Kalkan
 */
public class Ayarlar 
{
	// Üye deðiþkenler ////////////////////////////////////////////////////////
	private InetAddress IPAdresi;
	private InetAddress altAgMaskesi;
	private InetAddress varsayilanAgGecidi;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public Ayarlar()
	{
		//boþ
	}
	
	// Set ve Get Fonksyonlarý ////////////////////////////////////////////////
	/**
	 * @param iPAdresi the iPAdresi to set
	 */
	public void setIPAdresi(InetAddress iPAdresi) 
	{
		IPAdresi = iPAdresi;
	}
	/**
	 * @return the iPAdresi
	 */
	public InetAddress getIPAdresi() 
	{
		return IPAdresi;
	}
	//-------------------------------------------------------------------------
	/**
	 * @param altAgMaskesi the altAgMaskesi to set
	 */
	public void setAltAgMaskesi(InetAddress altAgMaskesi) 
	{
		this.altAgMaskesi = altAgMaskesi;
	}
	/**
	 * @return the altAgMaskesi
	 */
	public InetAddress getAltAgMaskesi() 
	{
		return altAgMaskesi;
	}
	//-------------------------------------------------------------------------
	/**
	 * @param varsayilanAgGecidi the varsayilanAgGecidi to set
	 */
	public void setVarsayilanAgGecidi(InetAddress varsayilanAgGecidi) 
	{
		this.varsayilanAgGecidi = varsayilanAgGecidi;
	}	
	/**
	 * @return the varsayilanAgGecidi
	 */
	public InetAddress getVarsayilanAgGecidi() 
	{
		return varsayilanAgGecidi;
	}
	
	// Static fonksyonlar /////////////////////////////////////////////////////
	public static Ayarlar yukle(String dosyaAdi)
	{
		try 
		{
			Ayarlar ayarlar = new Ayarlar();
			File dosya = new File(dosyaAdi);
			FileReader fr = new FileReader(dosya);
			BufferedReader br = new BufferedReader(fr);
			String satir;
			String[] ayrilmis;
			while((satir=br.readLine())!=null)
			{
				ayrilmis = satir.split("=");
				if(ayrilmis.length==2)
				{
					if(ayrilmis[0].trim().equals("IPAdresi"))
						ayarlar.setIPAdresi(Inet4Address.getByName(ayrilmis[1].trim()));
					else if(ayrilmis[0].trim().equals("AltAgMaskesi"))
						ayarlar.setAltAgMaskesi(Inet4Address.getByName(ayrilmis[1].trim()));
					else if(ayrilmis[0].trim().equals("VarsayilanAgGecidi"))
						ayarlar.setVarsayilanAgGecidi(Inet4Address.getByName(ayrilmis[1].trim()));
				}
			}
			fr.close();

			return ayarlar;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
