package tcpip.tcp;

/**
 * Bir TCP ba�lant�s� �zerinden veri (byte) ak��� g�nderebilmek i�in
 * gerekli fonksyonlar� sa�layan aray�z.
 * 
 * @author Halil �brahim KALKAN
 */
public interface TCPOutputStream
{
	/**
	 * Tek bir byte yazmak i�in.
	 * E�er tamponda yer kalmam��sa yer a��lana kadar bloklama yap�l�r.
	 * @param b yaz�lacak byte
	 * @return i�lemin ba�ar� durumu
	 */
	public boolean yaz(byte b);
	
	/**
	 * Bir byte dizisi yazmak i�in.
	 * E�er tamponda hi� yer kalmam��sa bloklama yap�l�r, en az bir byte
	 * varsa yaz�labilen kadar� yaz�l�r ve bu miktar d�nderilir.
	 * @param b yaz�lacak byte dizisi
	 * @return ka� byte'�n yaz�labildi�i
	 */
	public int yaz(byte[] b);
	
	/**
	 * Bir byte dizisinin bir k�sm�n� yazmak i�in.
	 * E�er tamponda hi� yer kalmam��sa bloklama yap�l�r, en az bir byte
	 * varsa yaz�labilen kadar� yaz�l�r ve bu miktar d�nderilir.
	 * @param b yaz�lacak byte dizisi
	 * @param baslangic b dizisinde yazman�n ba�lanaca�� yer
	 * @param sayi b dizisinde yaz�lacak byte say�s�
	 * @return ka� byte'�n yaz�labildi�i
	 */
	public int yaz(byte[] b, int baslangic, int sayi);
	
	/**
	 * Bloklama yap�lmadan yaz�labilecek maksimum veri miktar�n� ��renmek
	 * i�in.
	 * @return tamponda kalan bo� yer miktar� (byte say�s� olarak) 
	 */
	public int bosYer();
}
