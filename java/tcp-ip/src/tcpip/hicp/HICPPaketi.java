package tcpip.hicp;

/**
 * HICP protokol�nde kullan�lan bir segmenti temsil eder.
 * 
 * @author Halil �brahim KALKAN
 */
public class HICPPaketi 
{
	/** Paketi yollayan cihaz�n port numaras� */
	public short kaynakPortNumarasi;
	/** Paketin yolland��� cihaz�n port numaras� */
	public short hedefPortNumarasi;
	
	/** Paket i�erisindeki verinin byte olarak uzunlu�u */
	public short veriUzunlugu;
	/** Paket i�erisinde g�nderilen veri */
	public byte[] veri;
	
	/** En son al�nan paketin s�ra numaras� */
	public int onayNumarasi;
	/** Bu paketin s�ra numaras� */
	public int paketNumarasi;

	/** Onay numaras� ge�erli olup olmad��� */
	public boolean onayBayragi = false;
	/** Paketin veri ta��y�p ta��mad��� */
	public boolean veriBayragi = false;
	/** Ba�lant� a�ma iste�ini g�sterir. true olmas� durumunda paket numaras� ba�lang�� paket numaras� olur */
	public boolean baslangicBayragi = false;
	/** Ba�lant� kapama iste�ini g�sterir. */
	public boolean sonlandirmaBayragi = false;
}
