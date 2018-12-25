package tcpip.arp;

import java.net.Inet4Address;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Bir IP adresinin MAC adresini öðrenmek isteyen ARPIstemci nesnelerini,
 * ARP talebinin cevabý gelinceye kadar saklamak için kullanýlan sýnýf.
 * Eðer ayný ip için daha önce sonuçlanmamýþ bir talep varsa yeni gelen
 * talepler için yeniden ARP_REQUEST yapýlmaz, bunun yerine sadece IstemciEkle()
 * fonksyonu ile bu talep de listeye eklenir. Bu sayede trafik ve bekleme
 * azalmýþ olur.
 * 
 * @author Halil Ýbrahim KALKAN
 */
public class ARPTalebi
{
	/** MAC adresi talep edilen IP adresi */
	public Inet4Address 						ip;
	/** Bu MAC adresini bekleyen talepler */
	public LinkedBlockingQueue<ARPIstemciKaydi>	istemciler;
	/** ARP talebinin gönderilme aný (System.System.currentTimeMillis() ile alýnýr) */
	public long									gondermeZamani;
	/** ARP tablebinin takrar deneme sayýsý. Cevap gelmezse tekrar denemek için */
	public int									tekrarDenemeSayisi = 0;
	/** Bir ARP isteðine cevap gelmezse maximum tekrar deneme sayýsý */
	public static int							maxTekrarDenemeSayisi = 2;
	/**
	 * Bir istemci bilgisini tutmak için alt sýnýf
	 * @author Halil Ýbrahim KALKAN 
	 */
	public class ARPIstemciKaydi
	{
		/** istemciye referans */
		public ARPIstemcisi		istemci = null;
		/** istekleri ayýrt etmek için talepID */
		public long				talepID = 0;
		//---------------------------------------------------------------------
		public ARPIstemciKaydi(ARPIstemcisi istemci, long talepID)
		{
			this.istemci = istemci;
			this.talepID = talepID;
		}
	}
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public ARPTalebi(Inet4Address ip)
	{
		this.ip = ip;
		istemciler = new LinkedBlockingQueue<ARPIstemciKaydi>();
	}
	
	// public fonksyonlar ////////////////////////////////////////////////////
	/**
	 * Ayný IP için bir talep daha ekler
	 * @param istemci Bu IP adresinin MAC adresini öðrenmek isteyen istemci
	 * @param talepID Bu talep için bir ID numarasý
	 */
	public void istemciEkle(ARPIstemcisi istemci, long talepID)
	{
		istemciler.add(new ARPIstemciKaydi(istemci, talepID));
	}
}

