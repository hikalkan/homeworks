package tcpip.hicp;

import java.net.InetAddress;

public class HICPSunucuSoketi implements HICPPaketDinleyici
{
	/** Ba�lant�n�n durumu */
	private HICPDurumlari						durum;
	/** Sunucunun dinledi�i ve ba�lant�lar� kabul etti�i port numaras� */
	private int									portNo;
	/** Yeni ba�lant� isteklerini de�erlendirecek olan nesne */
	private HICPBaglantiDinleyici				baglantiDinleyici;

	/**
	 * Kurucu fonksyon.
	 * @param portNo Sunucunun dinledi�i ve ba�lant�lar� kabul etti�i port numaras�
	 * @param baglantiDinleyici Yeni ba�lant� isteklerini de�erlendirecek olan nesne
	 */
	public HICPSunucuSoketi(int portNo, HICPBaglantiDinleyici baglantiDinleyici)
	{
		durum = HICPDurumlari.KAPALI;
		this.portNo = portNo;
		this.baglantiDinleyici = baglantiDinleyici;
	}

	/**
	 * Sunucu soketini a�ar ve dinleme durumuna ge�er.
	 * @return sunucu soketi a�ma i�leminin ba�ar� durumu
	 */
	public boolean ac()
	{
		return true;
	}
	
	/**
	 * 
	 */
	public void paketAl(InetAddress kaynakIP, HICPPaketi s)
	{
		if(durum == HICPDurumlari.DINLEMEDE)
		{
			if(s.baslangicBayragi)
			{
				HICPIstemciSoketi is = new HICPIstemciSoketi(kaynakIP, s.kaynakPortNumarasi);
			}
		}
	}
}
