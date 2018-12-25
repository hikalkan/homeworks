package tcpip.tcp;

/**
 * Dairesel bir tampon kullanarak TCP protokolünde kullanýlan
 * kayan pencere mekanizmasýný gerçekleþtirmek için tasarlanan sýnýf.
 * 
 * Bu sýnýftan oluþturulan bir nesnedeki veriler ya gönderilmiþtir
 * ve onay bekliyordur ya da hiç gönderilmemiþtir. Yani karþýdan ulaþtýðýna
 * dair onay alýnan veriler derhal silinmelidir (bu sýnýfý kullanan
 * baþka sýnýflar tarafýndan). 
 * 
 * @author Halil Ýbrahim KALKAN
 */
public class KayanPencere
{
	/** Verileri saklamak için tampon (dairesel dizi) */
	private byte[]								tampon;
	/** dizi içerisindeki verinin ilk byte'ýnýn indisi */
	private int									bas;
	/** dizi içerisindeki verinin son byte'ýndan bir sonraki byte'ýn indisi (yani ilk boþ yerin indisi) */
	private int									son;
	/** dizi içerisinde bulunan geçerli verinin toplam uzunluðu */
	private int									veriBoyutu;
	/** tamponda maksimum saklanabilecek byte sayýsý */
	private int									tamponBoyutu;
	/** tampona veri eklendiðinde bilgilendirilecek nesne */
	private VeriDinleyicisi						veriDinleyicisi;
	
	/**
	 * Kurucu fonksyon.
	 * @param tamponBoyutu tamponda maksimum saklanabilecek byte sayýsý
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
	 * Tampona bir byte eklemek için fonksyon
	 * @param b eklenecek byte
	 * @return ekleme iþleminin baþarý durumu
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
	 * Tampona bir byte eklemek için fonksyon.
	 * Tamponda yer yoksa, boþalana kadar bloklama yapar.
	 * @param b eklenecek byte
	 * @return ekleme iþleminin baþarý durumu
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
	 * Tampona bir byte dizisi eklemek için fonksyon.
	 * @param b eklenecek byte dizisi
	 * @return dizinin kaç adet elemanýnýn eklenebildiði
	 */
	public synchronized int ekle(byte[] b)
	{
		return ekle(b, 0, b.length);
	}
	
	/**
	 * Tampona bir byte dizisi eklemek için fonksyon.
	 * Tamponda hiç boþ yer yoksa bloklama yapar deðilse
	 * yazabildiði kadarýný yazar ve bu miktarý dönderir.
	 * @param b eklenecek byte dizisi
	 * @return dizinin kaç adet elemanýnýn eklenebildiði
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
	 * Tampona bir byte dizisinin bir parçasýnýn eklenmesi için bir fonksyon.
	 * @param b eklenecek byte dizisi
	 * @param baslangic b dizisinin ilk eklenecek elemaný
	 * @param sayi kaç adet eleman ekleneceði
	 * @return kaç adet elemanýn eklenebildiði
	 */
	public synchronized int ekle(byte[] b, int baslangic, int sayi)
	{
		//tampon doluysa hiçbiþey yapmadan çýk
		if(doluMu())
		{
			return 0;
		}
		int eklenen = 0;
		//tampondaki boþ yeri bul
		int bosYer = tamponBoyutu - veriBoyutu;
		//eðer eklenmek istenen byte sayýsý tampondaki toplam
		//boþ yerden büyükse sadece boþ olan yer kadarýný eklemek için
		//toplam eklenecek byte sayýsýný bul
		int eklenecek = (sayi > bosYer) ? bosYer : sayi;
		//tamponun sonunda kalan boþ yer eklenmek istenen sayýdan küçükse
		//önce bu boþluk kadarýný ekle
		int sondakiBosYer = tamponBoyutu - son;
		if(eklenecek > sondakiBosYer)
		{			
			System.arraycopy(b, baslangic, tampon, son, sondakiBosYer);
			eklenen += sondakiBosYer;
			son = 0;
		}
		//yukarýda eklenemeyen, yani kalan byte sayýsýný bul
		//ve tamponun baþýndan itibaren ekle
		int kalan = eklenecek - eklenen;
		if(kalan>0)
		{
			System.arraycopy(b, baslangic + eklenen, tampon, son, kalan);
			eklenen += kalan;
			son += kalan;
		}
		//veri boyutu artýþýný yansýt
		veriBoyutu += eklenen;
		//veri eklenmesini bekleyen nesneyi bilgilendir
		if(eklenen>0)
		{
			veriDinleyicisi.veriEklendi();
		}
		//eklenen byte sayýsýný dönder
		return eklenen;
	}	
	
	/**
	 * Tampona bir byte dizisinin bir parçasýnýn eklenmesi için bir fonksyon.
	 * Tamponda hiç boþ yer yoksa bloklama yapar deðilse
	 * yazabildiði kadarýný yazar ve bu miktarý dönderir.
	 * @param b eklenecek byte dizisi
	 * @param baslangic b dizisinin ilk eklenecek elemaný
	 * @param sayi kaç adet eleman ekleneceði
	 * @return kaç adet elemanýn eklenebildiði
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
	 * Tamponun doluluk durumunu öðrenmek içindir.
	 * @return tampon doluysa true, deðilse false
	 */
	public synchronized boolean doluMu()
	{
		return (veriBoyutu == tamponBoyutu);
	}
	
	/**
	 * Kayan pencere mekanizmasýnda kullanýlabilir durumda olan boþ yer
	 * miktarýný dönderir
	 * @return boþ yer miktarý
	 */
	public synchronized int bosYer()
	{
		return (tamponBoyutu - veriBoyutu);
	}
	
	/**
	 * Tamponda bulunan toplam geçerli byte sayýsýný verir.
	 * @return tamponda bulunan toplam geçerli byte sayýsý
	 */
	public synchronized int getVeriBoyutu()
	{
		return veriBoyutu;
	}
	
	/**
	 * Tamponun boyutunu dönderir. 
	 * @return tamponun boyutu
	 */
	public synchronized int getTamponBoyutu()
	{
		return tamponBoyutu;
	}
	
	/**
	 * Tamponun baþýndan istenilen kadar byte'ý siler
	 * @param miktar silinmek istenen byte sayýsý
	 */
	public synchronized void sil(int miktar)
	{
		//silinmek istenen veri toplam veriden büyükse hataya yol açmamak
		//için sadece olan kadarýný sil
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
		//modunu al ve bas>=tamponBoyutu ise bas'i yeniden ayarlamýþ ol
		bas %= tamponBoyutu;
	}
	
	/**
	 * Tek bir byte almak için fonksyon.
	 * Alýnan byte tampondan silinmez.
	 * @return bir byte veri
	 */
	public synchronized byte al()
	{
		//veri yoksa derhal çýk
		if(veriBoyutu==0)
		{
			return 0;
		}
		//ilk byte'ý dönder
		return tampon[bas];		
	}
	
	/**
	 * Tampondaki verinin istenilen tek bir byte'ýný almak için fonksyon.
	 * @param atlama verinin baþýndan itibaren kaçýncý byte'ýnýn istendiði (0'dan baþlayarak)
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
	 * Bir byte dizisi almak için.
	 * b dizisinin tamamýný doldurur. Eðer yeterli eleman
	 * yoksa olduðu kadarýný doldurur.
	 * Alýnan elemanlar tampondan silinmez.
	 * @param b byte dizisi almak için kullanýlan boþ dizi
	 * @return kaç adet elemanýn alýnabildiði
	 */
	public synchronized int al(byte[] b)
	{
		return al(b, 0, b.length);
	}
	
	/**
	 * Tampondaki verilerin herhangi bir yerinden bir byte dizisi almak için.
	 * b dizisinin tamamýný doldurur. Eðer yeterli eleman
	 * yoksa olduðu kadarýný doldurur.
	 * Alýnan elemanlar tampondan silinmez.
	 * @param atlama verinin baþýndan baþlayarak kaç byte'ýn atlanacaðý (ilk byte 0 olacak þekilde)
	 * @param b byte dizisi almak için kullanýlan boþ dizi
	 * @return kaç adet elemanýn alýnabildiði
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
	 * Bir byte dizisi almak için.
	 * b dizisine baslangic deðerinden baþlayarak sayi kadar byte
	 * doldurur.
	 * Alýnan elemanlar tampondan silinmez.
	 * @param b byte dizisi almak için kullanýlan boþ dizi
	 * @param baslangic elemanlarýn b dizisinde yazýlacaðý yerin baþlangýç indisi
	 * @param sayi kaç adet byte alýnmak istendiði
	 * @return kaç adet elemanýn alýnabildiði
	 */
	public synchronized int al(byte[] b, int baslangic, int sayi)
	{
		//hiç veri yoksa derhal çýk
		if(veriBoyutu==0)
		{
			return 0;
		}
		int alinan = 0;
		//istenen byte sayýsý mevcut sayýdan fazlaysa sadece
		//olan kadarýný almasýný saðla		
		int alinacak = (sayi > veriBoyutu) ? veriBoyutu : sayi;
		//bas indisinden itibaren tamponun sonuna kadar olan
		//yer sayýsýný hesapla
		int sondakiVeri = tamponBoyutu - bas;
		//eðer bu deðer alinmak istenen toplam veriden daha küçükse
		//veri 2 parça halinde, önce sondan sonra baþtan alýnýr,
		//deðilse tek parça halinde alýnýr.
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
	 * Tampondaki verilerin herhangi bir yerinden bir byte dizisi almak için.
	 * b dizisinin tamamýný doldurur. Eðer yeterli eleman
	 * yoksa olduðu kadarýný doldurur.
	 * Alýnan elemanlar tampondan silinmez.
 	 * @param atlama verinin baþýndan baþlayarak kaç byte'ýn atlanacaðý (ilk byte 0 olacak þekilde)
	 * @param b byte dizisi almak için kullanýlan boþ dizi
	 * @param baslangic elemanlarýn b dizisinde yazýlacaðý yerin baþlangýç indisi
	 * @param sayi kaç adet byte alýnmak istendiði
	 * @return kaç adet elemanýn alýnabildiði
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
	 * Tek bir byte almak için fonksyon.
	 * Alýnan byte tampondan silinir.
	 * @return bir byte veri
	 */
	public synchronized byte cikar()
	{
		//veri yoksa derhal çýk
		if(veriBoyutu==0)
		{
			return 0;
		}
		//ilk byte'ý al
		byte b = tampon[bas++];
		//bas'i yeniden ayarla
		bas %= tamponBoyutu;
		//veri boyutunu 1 azalt
		veriBoyutu--;
		//alýnan byte'ý dönder
		return b;		
	}
	
	/**
	 * Bir byte dizisi almak için.
	 * b dizisinin tamamýný doldurur. Eðer yeterli eleman
	 * yoksa olduðu kadarýný doldurur.
	 * Alýnan elemanlar tampondan silinir.
	 * @param b byte dizisi almak için kullanýlan boþ dizi
	 * @return kaç adet elemanýn alýnabildiði
	 */
	public synchronized int cikar(byte[] b)
	{
		return cikar(b, 0, b.length);
	}
	
	/**
	 * Bir byte dizisi almak için.
	 * b dizisine baslangic deðerinden baþlayarak sayi kadar byte
	 * doldurur.
	 * Alýnan elemanlar tampondan silinir.
	 * @param b byte dizisi almak için kullanýlan boþ dizi
	 * @param baslangic elemanlarýn b dizisinde yazýlacaðý yerin baþlangýç indisi
	 * @param sayi kaç adet byte alýnmak istendiði
	 * @return kaç adet elemanýn alýnabildiði
	 */
	public synchronized int cikar(byte[] b, int baslangic, int sayi)
	{
		//al fonksyonunu kullanarak istenen veriler ile diziyi doldur
		int alinan = al(b, baslangic, sayi);
		//kaç adet byte alýndýysa o kadar byte'ý tampondan sil
		sil(alinan);
		//kaç adet byte alýndýðýný dönder
		return alinan;
	}
}
