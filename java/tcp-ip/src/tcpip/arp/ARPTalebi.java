package tcpip.arp;

import java.net.Inet4Address;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Bir IP adresinin MAC adresini ��renmek isteyen ARPIstemci nesnelerini,
 * ARP talebinin cevab� gelinceye kadar saklamak i�in kullan�lan s�n�f.
 * E�er ayn� ip i�in daha �nce sonu�lanmam�� bir talep varsa yeni gelen
 * talepler i�in yeniden ARP_REQUEST yap�lmaz, bunun yerine sadece IstemciEkle()
 * fonksyonu ile bu talep de listeye eklenir. Bu sayede trafik ve bekleme
 * azalm�� olur.
 * 
 * @author Halil �brahim KALKAN
 */
public class ARPTalebi
{
	/** MAC adresi talep edilen IP adresi */
	public Inet4Address 						ip;
	/** Bu MAC adresini bekleyen talepler */
	public LinkedBlockingQueue<ARPIstemciKaydi>	istemciler;
	/** ARP talebinin g�nderilme an� (System.System.currentTimeMillis() ile al�n�r) */
	public long									gondermeZamani;
	/** ARP tablebinin takrar deneme say�s�. Cevap gelmezse tekrar denemek i�in */
	public int									tekrarDenemeSayisi = 0;
	/** Bir ARP iste�ine cevap gelmezse maximum tekrar deneme say�s� */
	public static int							maxTekrarDenemeSayisi = 2;
	/**
	 * Bir istemci bilgisini tutmak i�in alt s�n�f
	 * @author Halil �brahim KALKAN 
	 */
	public class ARPIstemciKaydi
	{
		/** istemciye referans */
		public ARPIstemcisi		istemci = null;
		/** istekleri ay�rt etmek i�in talepID */
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
	 * Ayn� IP i�in bir talep daha ekler
	 * @param istemci Bu IP adresinin MAC adresini ��renmek isteyen istemci
	 * @param talepID Bu talep i�in bir ID numaras�
	 */
	public void istemciEkle(ARPIstemcisi istemci, long talepID)
	{
		istemciler.add(new ARPIstemciKaydi(istemci, talepID));
	}
}

