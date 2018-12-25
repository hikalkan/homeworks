package tcpip.tcp;

/**
 * Bir TCP baðlantýsý üzerinden veri (byte) akýþý gönderebilmek için
 * gerekli fonksyonlarý saðlayan arayüz.
 * 
 * @author Halil Ýbrahim KALKAN
 */
public interface TCPOutputStream
{
	/**
	 * Tek bir byte yazmak için.
	 * Eðer tamponda yer kalmamýþsa yer açýlana kadar bloklama yapýlýr.
	 * @param b yazýlacak byte
	 * @return iþlemin baþarý durumu
	 */
	public boolean yaz(byte b);
	
	/**
	 * Bir byte dizisi yazmak için.
	 * Eðer tamponda hiç yer kalmamýþsa bloklama yapýlýr, en az bir byte
	 * varsa yazýlabilen kadarý yazýlýr ve bu miktar dönderilir.
	 * @param b yazýlacak byte dizisi
	 * @return kaç byte'ýn yazýlabildiði
	 */
	public int yaz(byte[] b);
	
	/**
	 * Bir byte dizisinin bir kýsmýný yazmak için.
	 * Eðer tamponda hiç yer kalmamýþsa bloklama yapýlýr, en az bir byte
	 * varsa yazýlabilen kadarý yazýlýr ve bu miktar dönderilir.
	 * @param b yazýlacak byte dizisi
	 * @param baslangic b dizisinde yazmanýn baþlanacaðý yer
	 * @param sayi b dizisinde yazýlacak byte sayýsý
	 * @return kaç byte'ýn yazýlabildiði
	 */
	public int yaz(byte[] b, int baslangic, int sayi);
	
	/**
	 * Bloklama yapýlmadan yazýlabilecek maksimum veri miktarýný öðrenmek
	 * için.
	 * @return tamponda kalan boþ yer miktarý (byte sayýsý olarak) 
	 */
	public int bosYer();
}
