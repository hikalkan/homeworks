package tcpip.ip;

import java.net.*;
import java.util.*;

import jpcap.packet.*;

import java.io.*;

/**
 * Fragmentasyon y�ntemleriyle b�l�nm�� olarak gelen paketleri ge�ici
 * olarak saklamak i�in kullan�lan bir s�n�f.
 * 
 * @author Halil �brahim KALKAN
 */
public class ParcaKaydi 
{
	/** Bu kayd� temsil eden kay�t ba�l��� */
	public KayitBasligi							baslik;
	/** orjinal datagram�n par�alar�n� saklayan yap� */
	private TreeSet<ParcaYapisi>				parcalar;
	/** Bu par�alar� (zaman�nda tamamlanmazsa) yok edecek olan g�reve bir referans */
	public TimerTask							yokEtmeGorevi;
	/** Par�alar�n birincisi al�nd�ysa true olur */
	private boolean								birinciParcaAlindi = false;
	/** Par�alar�n sonuncusu al�nd�ysa true olur */
	private boolean								sonuncuParcaAlindi = false;
	/** T�m par�alar al�nd�ysa true olur */
	private boolean								paketTamamlandi = false;
	/** Par�alar�n toplam boyutu */
	private int									toplamBoyut = 0;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	/**
	 * Kay�t ba�l��� verilen kurucu fonksyon. Par�alar� saklamak i�in bir liste
	 * olu�turur.
	 * @param baslik bu kayd� temsil eden kay�t ba�l���
	 */
	public ParcaKaydi(KayitBasligi baslik)
	{
		//ba�l�k nesnesini al
		this.baslik = baslik;
		//orjinal datagram�n par�alar�n� saklayan bir yap� olu�tur
		parcalar = new TreeSet<ParcaYapisi>(new ParcaKarsilastirici());
	}
	
	// Public fonksyonlar /////////////////////////////////////////////////////
	/**
	 * Bu paket i�in yeni bir par�a ekler. Par�a eklendikten sonra t�m par�alar�n
	 * tamamlan�p tamamlanmad���n� kontrol eden fonksyon �a��r�l�r.
	 * @param paket orjinal datagram� olu�turan par�alardan birisi
	 */
	public boolean parcaEkle(IPPacket paket)
	{
		//paketin IHL de�erini al
		byte b = paket.header[14]; // Version + IHL
		byte ihl = (byte)(b & 0x0F); //IHL de�eri
		//paketin Total Length de�erini al
		ByteArrayInputStream bais =
			new ByteArrayInputStream(paket.header,
					16, //16. byte'dan ba�la
					2); //2 byte al
		DataInputStream dis = new DataInputStream(bais);
		short uzunluk = 0;
		try
		{
			uzunluk = dis.readShort();
		}
		catch (Exception e)
		{
			//bo�...
		}
		//hata varsa false d�nder
		if(uzunluk==0)
			return false;		
		//Toplam uzunluktan ba�l�k k�sm�n� ��kar
		uzunluk -= ihl;
		//par�ay� listeye eklemek i�in bir yap� olu�tur
		ParcaYapisi py = new ParcaYapisi(paket, uzunluk);
		//par�ay� listeye ekle, zaten listede varsa ��k
		if(!parcalar.contains(py))
		{
			parcalar.add(py);
			toplamBoyut += uzunluk; //toplam boyutu g�ncelle
		}
		else
			return true;
		
		//birinci par�a olup olmad���n� kontrol et
		if(paket.offset==0)
			birinciParcaAlindi = true;
		//sonuncu par�a olup olmad���n� kontrol et
		if(!paket.more_frag)
			sonuncuParcaAlindi = true;
		//birinci ve sonuncu par�a al�nm��sa paketin tamamlanma durumunu kontrol et
		if(sonuncuParcaAlindi && birinciParcaAlindi)
			paketTamamlandiKontrol();
		return true;
	}
	//-------------------------------------------------------------------------
	/**
	 * E�er bu paketin t�m par�alar� tamamlanm��sa true d�nderir.
	 * @return paketin tamamlanma durumu
	 */
	public boolean paketHazir()
	{
		return paketTamamlandi;
	}
	//-------------------------------------------------------------------------
	/**
	 * E�er paket haz�rsa t�m paketleri tek bir IP paketi olarak birle�tirir.
	 * @return birle�tirilmi� paket
	 */
	public IPPacket birlestirilmisPaketiOlustur()
	{
		//Paket tamamlanmad�ysa null d�nder
		if(this.paketTamamlandi)
			return null;

		//Ba�l�k bilgilerini olu�turmak i�in ilk par�ay� al
		ParcaYapisi ilkParca = parcalar.first();

		//Ba�l�k olarak ilk par�an�n ba�l���n� al
		IPPacket paket = ilkParca.paket;
		//Fragmentasyonla alakal� alanlar� temizle (DF, MF, offset)
		paket.header[20] = 0;
		paket.header[21] = 0;
		
		//Paketin data k�sm�n� olu�turmak i�in yeterli b�y�kl�kte
		//bo� bir buffer olu�tur
		ByteArrayOutputStream baos = new ByteArrayOutputStream(toplamBoyut);
		//buffer'a yazmak i�in output stream olu�tur
		DataOutputStream dos = new DataOutputStream(baos);
		try 
		{
			//t�m par�alar�n data k�sm�n� ekle
			for(ParcaYapisi parca : parcalar)
			{
				dos.write(parca.paket.data);
			}
			
			//Elde edilen byte dizisini paketin data k�sm� olarak belirle
			paket.data = baos.toByteArray();
			//paketi d�nder
			return paket;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	// Private fonksyonlar ////////////////////////////////////////////////////
	/**
	 * T�m datagram par�alar�n�n al�n�p al�nmad���n� kontrol eder.
	 * E�er t�m par�alar tamamlanm��sa paketTamamlandi de�erini true yapar.
	 */
	private void paketTamamlandiKontrol()
	{
		short offset = 0;
		//�imdiye kadar aln�nan t�m par�alar i�in bir d�ng� kur.
		//Not: Par�alar offset de�erlerine g�re k���kten b�y��e s�ral� haldedir.
		for(ParcaYapisi parca : parcalar)
		{
			//paket'in offset de�erini al 8 ile �arp (��nk� offset bilgisinde
			//her say� 8 byte'� temsil eder) ve d�ng�de gelinen offset de�eri ile
			//kar��la�t�r. E�er farkl�ysa arada bir paket EKS�K demektir.
			//Bu durumda fonksyondan ��k.
			if((parca.paket.offset*8)!=offset)
				return;
			//her ad�mda offset de�erini okunan par�an�n uzunlu�u kadar art�r.
			offset += parca.uzunluk;
		}
		
		//E�er �stteki d�ng�de fonksyondan ��kmad�ysa paketler tamamlanm�� demektir.
		paketTamamlandi = true;
	}
	
	// Alt s�n�flar ///////////////////////////////////////////////////////////
	//-------------------------------------------------------------------------
	/**
	 * Bir paketin her bir par�as�n� saklamak i�in kullan�lan s�n�f.
	 * 
	 * @author Halil �brahim KALKAN
	 */
	private class ParcaYapisi
	{
		
		/** Bu par�ay� olu�turan IP paketi */
		IPPacket								paket;
		/**
		 * Bu paketdeki data k�sm�n�n byte olarak uzunlu�u.
		 * Paketin toplam uzunlu�undan (Total Length)
		 * ba�l�k uzunlu�u (IHL=Internet Header Length)
		 * de�eri ��kar�larak elde elilir.
		 */
		short									uzunluk;
		
		// Kurucu fonksyonlar /////////////////////////////////////////////////
		/**
		 * ParcaYapisi nesnesi olu�turan kurucu fonksyon.
		 * @param paket bu par�ay� i�eren IP paketi
		 * @param uzunluk bu paketteki data'n�n byte olarak uzunlu�u
		 */
		public ParcaYapisi(IPPacket paket, short uzunluk)
		{
			this.paket = paket;
			this.uzunluk = uzunluk;
		}
	}
	/**
	 * ParcaYapisi nesnelerini paketin offset de�erlerine g�re 
	 * k���kten b�y��e s�ralayabilmek i�in kullan�lan kar��la�t�r�c� bir s�n�f.
	 * 
	 * @author Halil �brahim KALKAN
	 */
	private class ParcaKarsilastirici implements java.util.Comparator<ParcaYapisi>
	{
		/**
		 * Kar��la�t�rmay� ger�ekle�tiren fonksyon. �ki ParcaYapisi nesnesini
		 * kar��la�t�r�r. Birinci paketin offset de�eri b�y�kse pozitif bir de�er,
		 * ikinci paketin offset de�eri b�y�kse negatif bir de�er d�nderir. E�itlik
		 * halinde 0 d�nderir.
		 * @param o1 Birinci IP pakeri
		 * @param o2 �kinci IP paketi
		 * @return kar��la�t�rma sonucu
		 */
		public int compare(ParcaYapisi o1, ParcaYapisi o2) 
		{
			return (o1.paket.offset-o2.paket.offset);
		}	
	}
}
//-----------------------------------------------------------------------------
/**
 * ParcaKaydi s�n�f�n� javan�n standart container'lar�nda saklamak i�in
 * anahtar de�er olarak kullan�lacak s�n�f.
 * Birbiriyle alakal� olan par�alar i�in tam bir anahtar de�er sa�lar. ��nk�
 * bunlar� di�erlerinden ay�ran �zellik g�nderen makinenin IP adresi ve paket
 * i�erisindeki identification numaras�d�r. 
 * 
 * @author Halil ibrahim KALKAN
 */
class KayitBasligi implements Comparable<KayitBasligi>
{
	/** Paketi yollayan makinan�n IP adresi */
	public Inet4Address						ipAdresi;
	/** Gelen paketteki ident numaras� */
	public short 							ident;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////
	/**
	 * �ye de�i�kenleri verilen bir KayitBasligi nesnesi olu�turur.
	 * @param ipAdresi paketi yollayan makinan�n IP adresi
	 * @param ident gelen paketteki ident numaras�
	 */
	public KayitBasligi(Inet4Address ipAdresi, short ident)
	{
		this.ipAdresi = ipAdresi;
		this.ident = ident;
	}
	
	// kar��la�t�rma fonksyonlar� /////////////////////////////////////////
	/**
	 * Bu KayitBasligi nesnesi ile arg0 nesnesini kar��la�t�r�r. Sonu�ta birinci
	 * b�y�kse pozitif, ikinci b�y�kse negatif bir say�, e�itse 0 d�nderir.
	 * bu s�n�f� javan�n standart konteyner'lar�nda anahtar de�er olarak kullanmak
	 * i�in gereklidir.
	 * @param arg0 kar��la�t�r�lacak KayitBasligi nesnesi
	 */
	public int compareTo(KayitBasligi arg0) 
	{
		if(ipAdresi.equals(arg0.ipAdresi))
		{
			//IP adresleri ayn� ise ident'i b�y�k olan b�y�kt�r.
			return this.ident-arg0.ident;
		}
		else
		{
			//IP adresi b�y�k olan b�y�kt�r
			byte[] adres1 = this.ipAdresi.getAddress();
			byte[] adres2 = arg0.ipAdresi.getAddress();

			for(int i=0;i<4;i++)
				if(adres1[i]!=adres2[i])
					return (adres1[i]-adres2[i]);	
		}
		//hem IP adresi hem de ident de�eri e�itse bu ba�l�klar e�ittir. 
		return 0;
	}
	//---------------------------------------------------------------------
	/**
	 * Bu KayitBasligi nesnesi ile verilen nesne e�itse true aksi halde
	 * false d�nderir.
	 * @param o kar��la�t�r�lacak nesne.
	 */
	public boolean equals(Object o)
	{
		//hem IP adresi hem de ident de�eri e�itse bu ba�l�klar e�ittir. 
		KayitBasligi kb = (KayitBasligi)o;
		if(this.ipAdresi.equals(kb.ipAdresi) && this.ident==kb.ident)
			return true;
		return false;
	}
}
