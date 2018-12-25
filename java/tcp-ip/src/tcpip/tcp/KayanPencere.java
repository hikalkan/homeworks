package tcpip.tcp;

/**
 * Dairesel bir tampon kullanarak TCP protokol�nde kullan�lan
 * kayan pencere mekanizmas�n� ger�ekle�tirmek i�in tasarlanan s�n�f.
 * 
 * Bu s�n�ftan olu�turulan bir nesnedeki veriler ya g�nderilmi�tir
 * ve onay bekliyordur ya da hi� g�nderilmemi�tir. Yani kar��dan ula�t���na
 * dair onay al�nan veriler derhal silinmelidir (bu s�n�f� kullanan
 * ba�ka s�n�flar taraf�ndan). 
 * 
 * @author Halil �brahim KALKAN
 */
public class KayanPencere
{
	/** Verileri saklamak i�in tampon (dairesel dizi) */
	private byte[]								tampon;
	/** dizi i�erisindeki verinin ilk byte'�n�n indisi */
	private int									bas;
	/** dizi i�erisindeki verinin son byte'�ndan bir sonraki byte'�n indisi (yani ilk bo� yerin indisi) */
	private int									son;
	/** dizi i�erisinde bulunan ge�erli verinin toplam uzunlu�u */
	private int									veriBoyutu;
	/** tamponda maksimum saklanabilecek byte say�s� */
	private int									tamponBoyutu;
	/** tampona veri eklendi�inde bilgilendirilecek nesne */
	private VeriDinleyicisi						veriDinleyicisi;
	
	/**
	 * Kurucu fonksyon.
	 * @param tamponBoyutu tamponda maksimum saklanabilecek byte say�s�
	 */
	public KayanPencere(int tamponBoyutu, VeriDinleyicisi veriDinleyicisi)
	{
		bas = 0;
		son = 0;
		veriBoyutu = 0;
		this.tamponBoyutu = tamponBoyutu;
		tampon = new byte[tamponBoyutu];
		this.veriDinleyicisi = veriDinleyicisi;
	}
	
	/**
	 * Tampona bir byte eklemek i�in fonksyon
	 * @param b eklenecek byte
	 * @return ekleme i�leminin ba�ar� durumu
	 */
	public synchronized boolean ekle(byte b)
	{
		if(!doluMu())
		{
			if(son >= tamponBoyutu)
			{
				son = 0;
			}
			tampon[son++] = b;
			veriBoyutu++;
			veriDinleyicisi.veriEklendi();
			return true;
		}
		return false;
	}
	
	/**
	 * Tampona bir byte eklemek i�in fonksyon.
	 * Tamponda yer yoksa, bo�alana kadar bloklama yapar.
	 * @param b eklenecek byte
	 * @return ekleme i�leminin ba�ar� durumu
	 */
	public synchronized boolean ekleBloklu(byte b)
	{
		while(!ekle(b))
		{
			try { wait(); } catch (Exception e) { }
		}
		return true;
	}
	
	/**
	 * Tampona bir byte dizisi eklemek i�in fonksyon.
	 * @param b eklenecek byte dizisi
	 * @return dizinin ka� adet eleman�n�n eklenebildi�i
	 */
	public synchronized int ekle(byte[] b)
	{
		return ekle(b, 0, b.length);
	}
	
	/**
	 * Tampona bir byte dizisi eklemek i�in fonksyon.
	 * Tamponda hi� bo� yer yoksa bloklama yapar de�ilse
	 * yazabildi�i kadar�n� yazar ve bu miktar� d�nderir.
	 * @param b eklenecek byte dizisi
	 * @return dizinin ka� adet eleman�n�n eklenebildi�i
	 */
	public synchronized int ekleBloklu(byte[] b)
	{
		while(doluMu())
		{
			try { wait(); } catch (Exception e) { }
		}
		return ekle(b);
	}
	
	/**
	 * Tampona bir byte dizisinin bir par�as�n�n eklenmesi i�in bir fonksyon.
	 * @param b eklenecek byte dizisi
	 * @param baslangic b dizisinin ilk eklenecek eleman�
	 * @param sayi ka� adet eleman eklenece�i
	 * @return ka� adet eleman�n eklenebildi�i
	 */
	public synchronized int ekle(byte[] b, int baslangic, int sayi)
	{
		//tampon doluysa hi�bi�ey yapmadan ��k
		if(doluMu())
		{
			return 0;
		}
		int eklenen = 0;
		//tampondaki bo� yeri bul
		int bosYer = tamponBoyutu - veriBoyutu;
		//e�er eklenmek istenen byte say�s� tampondaki toplam
		//bo� yerden b�y�kse sadece bo� olan yer kadar�n� eklemek i�in
		//toplam eklenecek byte say�s�n� bul
		int eklenecek = (sayi > bosYer) ? bosYer : sayi;
		//tamponun sonunda kalan bo� yer eklenmek istenen say�dan k���kse
		//�nce bu bo�luk kadar�n� ekle
		int sondakiBosYer = tamponBoyutu - son;
		if(eklenecek > sondakiBosYer)
		{			
			System.arraycopy(b, baslangic, tampon, son, sondakiBosYer);
			eklenen += sondakiBosYer;
			son = 0;
		}
		//yukar�da eklenemeyen, yani kalan byte say�s�n� bul
		//ve tamponun ba��ndan itibaren ekle
		int kalan = eklenecek - eklenen;
		if(kalan>0)
		{
			System.arraycopy(b, baslangic + eklenen, tampon, son, kalan);
			eklenen += kalan;
			son += kalan;
		}
		//veri boyutu art���n� yans�t
		veriBoyutu += eklenen;
		//veri eklenmesini bekleyen nesneyi bilgilendir
		if(eklenen>0)
		{
			veriDinleyicisi.veriEklendi();
		}
		//eklenen byte say�s�n� d�nder
		return eklenen;
	}	
	
	/**
	 * Tampona bir byte dizisinin bir par�as�n�n eklenmesi i�in bir fonksyon.
	 * Tamponda hi� bo� yer yoksa bloklama yapar de�ilse
	 * yazabildi�i kadar�n� yazar ve bu miktar� d�nderir.
	 * @param b eklenecek byte dizisi
	 * @param baslangic b dizisinin ilk eklenecek eleman�
	 * @param sayi ka� adet eleman eklenece�i
	 * @return ka� adet eleman�n eklenebildi�i
	 */
	public synchronized int ekleBloklu(byte[] b, int baslangic, int sayi)
	{
		while(doluMu())
		{
			try { wait(); } catch (Exception e) { }
		}
		return ekle(b, baslangic, sayi);
	}
	
	/**
	 * Tamponun doluluk durumunu ��renmek i�indir.
	 * @return tampon doluysa true, de�ilse false
	 */
	public synchronized boolean doluMu()
	{
		return (veriBoyutu == tamponBoyutu);
	}
	
	/**
	 * Kayan pencere mekanizmas�nda kullan�labilir durumda olan bo� yer
	 * miktar�n� d�nderir
	 * @return bo� yer miktar�
	 */
	public synchronized int bosYer()
	{
		return (tamponBoyutu - veriBoyutu);
	}
	
	/**
	 * Tamponda bulunan toplam ge�erli byte say�s�n� verir.
	 * @return tamponda bulunan toplam ge�erli byte say�s�
	 */
	public synchronized int getVeriBoyutu()
	{
		return veriBoyutu;
	}
	
	/**
	 * Tamponun boyutunu d�nderir. 
	 * @return tamponun boyutu
	 */
	public synchronized int getTamponBoyutu()
	{
		return tamponBoyutu;
	}
	
	/**
	 * Tamponun ba��ndan istenilen kadar byte'� siler
	 * @param miktar silinmek istenen byte say�s�
	 */
	public synchronized void sil(int miktar)
	{
		//silinmek istenen veri toplam veriden b�y�kse hataya yol a�mamak
		//i�in sadece olan kadar�n� sil
		if(miktar > veriBoyutu)
		{
			bas += veriBoyutu;
			veriBoyutu = 0;
		}
		else
		{
			bas += miktar;
			veriBoyutu -= miktar;
		}
		//modunu al ve bas>=tamponBoyutu ise bas'i yeniden ayarlam�� ol
		bas %= tamponBoyutu;
	}
	
	/**
	 * Tek bir byte almak i�in fonksyon.
	 * Al�nan byte tampondan silinmez.
	 * @return bir byte veri
	 */
	public synchronized byte al()
	{
		//veri yoksa derhal ��k
		if(veriBoyutu==0)
		{
			return 0;
		}
		//ilk byte'� d�nder
		return tampon[bas];		
	}
	
	/**
	 * Tampondaki verinin istenilen tek bir byte'�n� almak i�in fonksyon.
	 * @param atlama verinin ba��ndan itibaren ka��nc� byte'�n�n istendi�i (0'dan ba�layarak)
	 * @return bir byte veri
	 */
	public synchronized byte al(int atlama)
	{
		int eskiBas = bas;
		bas = (bas + atlama) % tamponBoyutu;
		byte b = al();
		bas = eskiBas;
		return b;
	}
	
	/**
	 * Bir byte dizisi almak i�in.
	 * b dizisinin tamam�n� doldurur. E�er yeterli eleman
	 * yoksa oldu�u kadar�n� doldurur.
	 * Al�nan elemanlar tampondan silinmez.
	 * @param b byte dizisi almak i�in kullan�lan bo� dizi
	 * @return ka� adet eleman�n al�nabildi�i
	 */
	public synchronized int al(byte[] b)
	{
		return al(b, 0, b.length);
	}
	
	/**
	 * Tampondaki verilerin herhangi bir yerinden bir byte dizisi almak i�in.
	 * b dizisinin tamam�n� doldurur. E�er yeterli eleman
	 * yoksa oldu�u kadar�n� doldurur.
	 * Al�nan elemanlar tampondan silinmez.
	 * @param atlama verinin ba��ndan ba�layarak ka� byte'�n atlanaca�� (ilk byte 0 olacak �ekilde)
	 * @param b byte dizisi almak i�in kullan�lan bo� dizi
	 * @return ka� adet eleman�n al�nabildi�i
	 */
	public synchronized int al(int atlama, byte[] b)
	{
		int eskiBas = bas;
		bas = (bas + atlama) % tamponBoyutu;
		int alinan = al(b);
		bas = eskiBas;
		return alinan;
	}
	
	/**
	 * Bir byte dizisi almak i�in.
	 * b dizisine baslangic de�erinden ba�layarak sayi kadar byte
	 * doldurur.
	 * Al�nan elemanlar tampondan silinmez.
	 * @param b byte dizisi almak i�in kullan�lan bo� dizi
	 * @param baslangic elemanlar�n b dizisinde yaz�laca�� yerin ba�lang�� indisi
	 * @param sayi ka� adet byte al�nmak istendi�i
	 * @return ka� adet eleman�n al�nabildi�i
	 */
	public synchronized int al(byte[] b, int baslangic, int sayi)
	{
		//hi� veri yoksa derhal ��k
		if(veriBoyutu==0)
		{
			return 0;
		}
		int alinan = 0;
		//istenen byte say�s� mevcut say�dan fazlaysa sadece
		//olan kadar�n� almas�n� sa�la		
		int alinacak = (sayi > veriBoyutu) ? veriBoyutu : sayi;
		//bas indisinden itibaren tamponun sonuna kadar olan
		//yer say�s�n� hesapla
		int sondakiVeri = tamponBoyutu - bas;
		//e�er bu de�er alinmak istenen toplam veriden daha k���kse
		//veri 2 par�a halinde, �nce sondan sonra ba�tan al�n�r,
		//de�ilse tek par�a halinde al�n�r.
		if(sondakiVeri < alinacak)
		{
			System.arraycopy(tampon, bas, b, baslangic, sondakiVeri);
			alinan += sondakiVeri;
			int kalan = alinacak - alinan;
			System.arraycopy(tampon, 0, b, baslangic + alinan, kalan);
			alinan += kalan;
		}
		else
		{
			System.arraycopy(tampon, bas, b, baslangic, alinacak);
			alinan += alinacak;
		}		
		return alinan;
	}
	
	/**
	 * Tampondaki verilerin herhangi bir yerinden bir byte dizisi almak i�in.
	 * b dizisinin tamam�n� doldurur. E�er yeterli eleman
	 * yoksa oldu�u kadar�n� doldurur.
	 * Al�nan elemanlar tampondan silinmez.
 	 * @param atlama verinin ba��ndan ba�layarak ka� byte'�n atlanaca�� (ilk byte 0 olacak �ekilde)
	 * @param b byte dizisi almak i�in kullan�lan bo� dizi
	 * @param baslangic elemanlar�n b dizisinde yaz�laca�� yerin ba�lang�� indisi
	 * @param sayi ka� adet byte al�nmak istendi�i
	 * @return ka� adet eleman�n al�nabildi�i
	 */
	public synchronized int al(int atlama, byte[] b, int baslangic, int sayi)
	{
		int eskiBas = bas;
		bas = (bas + atlama) % tamponBoyutu;
		int alinan = al(b, baslangic, sayi);
		bas = eskiBas;
		return alinan;
	}
	
	/**
	 * Tek bir byte almak i�in fonksyon.
	 * Al�nan byte tampondan silinir.
	 * @return bir byte veri
	 */
	public synchronized byte cikar()
	{
		//veri yoksa derhal ��k
		if(veriBoyutu==0)
		{
			return 0;
		}
		//ilk byte'� al
		byte b = tampon[bas++];
		//bas'i yeniden ayarla
		bas %= tamponBoyutu;
		//veri boyutunu 1 azalt
		veriBoyutu--;
		//al�nan byte'� d�nder
		return b;		
	}
	
	/**
	 * Bir byte dizisi almak i�in.
	 * b dizisinin tamam�n� doldurur. E�er yeterli eleman
	 * yoksa oldu�u kadar�n� doldurur.
	 * Al�nan elemanlar tampondan silinir.
	 * @param b byte dizisi almak i�in kullan�lan bo� dizi
	 * @return ka� adet eleman�n al�nabildi�i
	 */
	public synchronized int cikar(byte[] b)
	{
		return cikar(b, 0, b.length);
	}
	
	/**
	 * Bir byte dizisi almak i�in.
	 * b dizisine baslangic de�erinden ba�layarak sayi kadar byte
	 * doldurur.
	 * Al�nan elemanlar tampondan silinir.
	 * @param b byte dizisi almak i�in kullan�lan bo� dizi
	 * @param baslangic elemanlar�n b dizisinde yaz�laca�� yerin ba�lang�� indisi
	 * @param sayi ka� adet byte al�nmak istendi�i
	 * @return ka� adet eleman�n al�nabildi�i
	 */
	public synchronized int cikar(byte[] b, int baslangic, int sayi)
	{
		//al fonksyonunu kullanarak istenen veriler ile diziyi doldur
		int alinan = al(b, baslangic, sayi);
		//ka� adet byte al�nd�ysa o kadar byte'� tampondan sil
		sil(alinan);
		//ka� adet byte al�nd���n� d�nder
		return alinan;
	}
}
