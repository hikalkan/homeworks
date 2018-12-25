package tcpip.hicp;

import java.net.InetAddress;

public class HICPSunucuSoketi implements HICPPaketDinleyici
{
	/** Baðlantýnýn durumu */
	private HICPDurumlari						durum;
	/** Sunucunun dinlediði ve baðlantýlarý kabul ettiði port numarasý */
	private int									portNo;
	/** Yeni baðlantý isteklerini deðerlendirecek olan nesne */
	private HICPBaglantiDinleyici				baglantiDinleyici;

	/**
	 * Kurucu fonksyon.
	 * @param portNo Sunucunun dinlediði ve baðlantýlarý kabul ettiði port numarasý
	 * @param baglantiDinleyici Yeni baðlantý isteklerini deðerlendirecek olan nesne
	 */
	public HICPSunucuSoketi(int portNo, HICPBaglantiDinleyici baglantiDinleyici)
	{
		durum = HICPDurumlari.KAPALI;
		this.portNo = portNo;
		this.baglantiDinleyici = baglantiDinleyici;
	}

	/**
	 * Sunucu soketini açar ve dinleme durumuna geçer.
	 * @return sunucu soketi açma iþleminin baþarý durumu
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
