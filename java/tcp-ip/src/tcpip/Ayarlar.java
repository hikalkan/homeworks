package tcpip;

import java.io.*;
import java.net.*;

/**
 * MyTCPIP protokol k�mesinin ba�lang�� ayarlar�n� saklayan dosyayla
 * ili�kili s�n�f. Ayarlar bu s�n�f �zerinden ayar dosyas�ndan al�n�r
 * ve yine bu s�n�f arac�l���yla de�i�tirilip kaydedilebilir.
 * 
 * @author Halil �brahim Kalkan
 */
public class Ayarlar 
{
	// �ye de�i�kenler ////////////////////////////////////////////////////////
	private InetAddress IPAdresi;
	private InetAddress altAgMaskesi;
	private InetAddress varsayilanAgGecidi;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public Ayarlar()
	{
		//bo�
	}
	
	// Set ve Get Fonksyonlar� ////////////////////////////////////////////////
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
