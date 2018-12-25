package tcpip.hicp;

/**
 * HICP protokolünde kullanýlan bir segmenti temsil eder.
 * 
 * @author Halil Ýbrahim KALKAN
 */
public class HICPPaketi 
{
	/** Paketi yollayan cihazýn port numarasý */
	public short kaynakPortNumarasi;
	/** Paketin yollandýðý cihazýn port numarasý */
	public short hedefPortNumarasi;
	
	/** Paket içerisindeki verinin byte olarak uzunluðu */
	public short veriUzunlugu;
	/** Paket içerisinde gönderilen veri */
	public byte[] veri;
	
	/** En son alýnan paketin sýra numarasý */
	public int onayNumarasi;
	/** Bu paketin sýra numarasý */
	public int paketNumarasi;

	/** Onay numarasý geçerli olup olmadýðý */
	public boolean onayBayragi = false;
	/** Paketin veri taþýyýp taþýmadýðý */
	public boolean veriBayragi = false;
	/** Baðlantý açma isteðini gösterir. true olmasý durumunda paket numarasý baþlangýç paket numarasý olur */
	public boolean baslangicBayragi = false;
	/** Baðlantý kapama isteðini gösterir. */
	public boolean sonlandirmaBayragi = false;
}
