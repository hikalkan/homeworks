package tcpip.hicp;

import java.net.*;

public class HICPIstemciSoketi implements HICPPaketDinleyici 
{
	/** Baðlantýnýn durumu */
	private HICPDurumlari						durum;
	
	private InetAddress							hedefIPAdresi;
	
	private short								kaynakPortNo;
	private short								hedefPortNo;
	
	public HICPIstemciSoketi(InetAddress hedefIPAdresi, short hedefPortNo)
	{
		durum = HICPDurumlari.KAPALI;
	}
	
	public boolean ac()
	{
		kaynakPortNo = HICP.getHICP().portNumarasiTahsisEt(this);
		if(kaynakPortNo<=0)
			return false;
		
		return true;
	}
	
	public boolean ac(HICPPaketi p)
	{
		return true;
	}
	
	private void ikinciMesajiYolla()
	{
		HICPPaketi p = new HICPPaketi();
		p.kaynakPortNumarasi = 0;
	}
	
	public void paketAl(InetAddress kaynakIP, HICPPaketi p) 
	{
		
	}
}
