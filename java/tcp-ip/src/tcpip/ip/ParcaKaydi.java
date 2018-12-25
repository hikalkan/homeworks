package tcpip.ip;

import java.net.*;
import java.util.*;

import jpcap.packet.*;

import java.io.*;

/**
 * Fragmentasyon yöntemleriyle bölünmüþ olarak gelen paketleri geçici
 * olarak saklamak için kullanýlan bir sýnýf.
 * 
 * @author Halil Ýbrahim KALKAN
 */
public class ParcaKaydi 
{
	/** Bu kaydý temsil eden kayýt baþlýðý */
	public KayitBasligi							baslik;
	/** orjinal datagramýn parçalarýný saklayan yapý */
	private TreeSet<ParcaYapisi>				parcalar;
	/** Bu parçalarý (zamanýnda tamamlanmazsa) yok edecek olan göreve bir referans */
	public TimerTask							yokEtmeGorevi;
	/** Parçalarýn birincisi alýndýysa true olur */
	private boolean								birinciParcaAlindi = false;
	/** Parçalarýn sonuncusu alýndýysa true olur */
	private boolean								sonuncuParcaAlindi = false;
	/** Tüm parçalar alýndýysa true olur */
	private boolean								paketTamamlandi = false;
	/** Parçalarýn toplam boyutu */
	private int									toplamBoyut = 0;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	/**
	 * Kayýt baþlýðý verilen kurucu fonksyon. Parçalarý saklamak için bir liste
	 * oluþturur.
	 * @param baslik bu kaydý temsil eden kayýt baþlýðý
	 */
	public ParcaKaydi(KayitBasligi baslik)
	{
		//baþlýk nesnesini al
		this.baslik = baslik;
		//orjinal datagramýn parçalarýný saklayan bir yapý oluþtur
		parcalar = new TreeSet<ParcaYapisi>(new ParcaKarsilastirici());
	}
	
	// Public fonksyonlar /////////////////////////////////////////////////////
	/**
	 * Bu paket için yeni bir parça ekler. Parça eklendikten sonra tüm parçalarýn
	 * tamamlanýp tamamlanmadýðýný kontrol eden fonksyon çaðýrýlýr.
	 * @param paket orjinal datagramý oluþturan parçalardan birisi
	 */
	public boolean parcaEkle(IPPacket paket)
	{
		//paketin IHL deðerini al
		byte b = paket.header[14]; // Version + IHL
		byte ihl = (byte)(b & 0x0F); //IHL deðeri
		//paketin Total Length deðerini al
		ByteArrayInputStream bais =
			new ByteArrayInputStream(paket.header,
					16, //16. byte'dan baþla
					2); //2 byte al
		DataInputStream dis = new DataInputStream(bais);
		short uzunluk = 0;
		try
		{
			uzunluk = dis.readShort();
		}
		catch (Exception e)
		{
			//boþ...
		}
		//hata varsa false dönder
		if(uzunluk==0)
			return false;		
		//Toplam uzunluktan baþlýk kýsmýný çýkar
		uzunluk -= ihl;
		//parçayý listeye eklemek için bir yapý oluþtur
		ParcaYapisi py = new ParcaYapisi(paket, uzunluk);
		//parçayý listeye ekle, zaten listede varsa çýk
		if(!parcalar.contains(py))
		{
			parcalar.add(py);
			toplamBoyut += uzunluk; //toplam boyutu güncelle
		}
		else
			return true;
		
		//birinci parça olup olmadýðýný kontrol et
		if(paket.offset==0)
			birinciParcaAlindi = true;
		//sonuncu parça olup olmadýðýný kontrol et
		if(!paket.more_frag)
			sonuncuParcaAlindi = true;
		//birinci ve sonuncu parça alýnmýþsa paketin tamamlanma durumunu kontrol et
		if(sonuncuParcaAlindi && birinciParcaAlindi)
			paketTamamlandiKontrol();
		return true;
	}
	//-------------------------------------------------------------------------
	/**
	 * Eðer bu paketin tüm parçalarý tamamlanmýþsa true dönderir.
	 * @return paketin tamamlanma durumu
	 */
	public boolean paketHazir()
	{
		return paketTamamlandi;
	}
	//-------------------------------------------------------------------------
	/**
	 * Eðer paket hazýrsa tüm paketleri tek bir IP paketi olarak birleþtirir.
	 * @return birleþtirilmiþ paket
	 */
	public IPPacket birlestirilmisPaketiOlustur()
	{
		//Paket tamamlanmadýysa null dönder
		if(this.paketTamamlandi)
			return null;

		//Baþlýk bilgilerini oluþturmak için ilk parçayý al
		ParcaYapisi ilkParca = parcalar.first();

		//Baþlýk olarak ilk parçanýn baþlýðýný al
		IPPacket paket = ilkParca.paket;
		//Fragmentasyonla alakalý alanlarý temizle (DF, MF, offset)
		paket.header[20] = 0;
		paket.header[21] = 0;
		
		//Paketin data kýsmýný oluþturmak için yeterli büyüklükte
		//boþ bir buffer oluþtur
		ByteArrayOutputStream baos = new ByteArrayOutputStream(toplamBoyut);
		//buffer'a yazmak için output stream oluþtur
		DataOutputStream dos = new DataOutputStream(baos);
		try 
		{
			//tüm parçalarýn data kýsmýný ekle
			for(ParcaYapisi parca : parcalar)
			{
				dos.write(parca.paket.data);
			}
			
			//Elde edilen byte dizisini paketin data kýsmý olarak belirle
			paket.data = baos.toByteArray();
			//paketi dönder
			return paket;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	// Private fonksyonlar ////////////////////////////////////////////////////
	/**
	 * Tüm datagram parçalarýnýn alýnýp alýnmadýðýný kontrol eder.
	 * Eðer tüm parçalar tamamlanmýþsa paketTamamlandi deðerini true yapar.
	 */
	private void paketTamamlandiKontrol()
	{
		short offset = 0;
		//Þimdiye kadar alnýnan tüm parçalar için bir döngü kur.
		//Not: Parçalar offset deðerlerine göre küçükten büyüðe sýralý haldedir.
		for(ParcaYapisi parca : parcalar)
		{
			//paket'in offset deðerini al 8 ile çarp (çünkü offset bilgisinde
			//her sayý 8 byte'ý temsil eder) ve döngüde gelinen offset deðeri ile
			//karþýlaþtýr. Eðer farklýysa arada bir paket EKSÝK demektir.
			//Bu durumda fonksyondan çýk.
			if((parca.paket.offset*8)!=offset)
				return;
			//her adýmda offset deðerini okunan parçanýn uzunluðu kadar artýr.
			offset += parca.uzunluk;
		}
		
		//Eðer üstteki döngüde fonksyondan çýkmadýysa paketler tamamlanmýþ demektir.
		paketTamamlandi = true;
	}
	
	// Alt sýnýflar ///////////////////////////////////////////////////////////
	//-------------------------------------------------------------------------
	/**
	 * Bir paketin her bir parçasýný saklamak için kullanýlan sýnýf.
	 * 
	 * @author Halil Ýbrahim KALKAN
	 */
	private class ParcaYapisi
	{
		
		/** Bu parçayý oluþturan IP paketi */
		IPPacket								paket;
		/**
		 * Bu paketdeki data kýsmýnýn byte olarak uzunluðu.
		 * Paketin toplam uzunluðundan (Total Length)
		 * baþlýk uzunluðu (IHL=Internet Header Length)
		 * deðeri çýkarýlarak elde elilir.
		 */
		short									uzunluk;
		
		// Kurucu fonksyonlar /////////////////////////////////////////////////
		/**
		 * ParcaYapisi nesnesi oluþturan kurucu fonksyon.
		 * @param paket bu parçayý içeren IP paketi
		 * @param uzunluk bu paketteki data'nýn byte olarak uzunluðu
		 */
		public ParcaYapisi(IPPacket paket, short uzunluk)
		{
			this.paket = paket;
			this.uzunluk = uzunluk;
		}
	}
	/**
	 * ParcaYapisi nesnelerini paketin offset deðerlerine göre 
	 * küçükten büyüðe sýralayabilmek için kullanýlan karþýlaþtýrýcý bir sýnýf.
	 * 
	 * @author Halil Ýbrahim KALKAN
	 */
	private class ParcaKarsilastirici implements java.util.Comparator<ParcaYapisi>
	{
		/**
		 * Karþýlaþtýrmayý gerçekleþtiren fonksyon. Ýki ParcaYapisi nesnesini
		 * karþýlaþtýrýr. Birinci paketin offset deðeri büyükse pozitif bir deðer,
		 * ikinci paketin offset deðeri büyükse negatif bir deðer dönderir. Eþitlik
		 * halinde 0 dönderir.
		 * @param o1 Birinci IP pakeri
		 * @param o2 Ýkinci IP paketi
		 * @return karþýlaþtýrma sonucu
		 */
		public int compare(ParcaYapisi o1, ParcaYapisi o2) 
		{
			return (o1.paket.offset-o2.paket.offset);
		}	
	}
}
//-----------------------------------------------------------------------------
/**
 * ParcaKaydi sýnýfýný javanýn standart container'larýnda saklamak için
 * anahtar deðer olarak kullanýlacak sýnýf.
 * Birbiriyle alakalý olan parçalar için tam bir anahtar deðer saðlar. Çünkü
 * bunlarý diðerlerinden ayýran özellik gönderen makinenin IP adresi ve paket
 * içerisindeki identification numarasýdýr. 
 * 
 * @author Halil ibrahim KALKAN
 */
class KayitBasligi implements Comparable<KayitBasligi>
{
	/** Paketi yollayan makinanýn IP adresi */
	public Inet4Address						ipAdresi;
	/** Gelen paketteki ident numarasý */
	public short 							ident;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////
	/**
	 * Üye deðiþkenleri verilen bir KayitBasligi nesnesi oluþturur.
	 * @param ipAdresi paketi yollayan makinanýn IP adresi
	 * @param ident gelen paketteki ident numarasý
	 */
	public KayitBasligi(Inet4Address ipAdresi, short ident)
	{
		this.ipAdresi = ipAdresi;
		this.ident = ident;
	}
	
	// karþýlaþtýrma fonksyonlarý /////////////////////////////////////////
	/**
	 * Bu KayitBasligi nesnesi ile arg0 nesnesini karþýlaþtýrýr. Sonuçta birinci
	 * büyükse pozitif, ikinci büyükse negatif bir sayý, eþitse 0 dönderir.
	 * bu sýnýfý javanýn standart konteyner'larýnda anahtar deðer olarak kullanmak
	 * için gereklidir.
	 * @param arg0 karþýlaþtýrýlacak KayitBasligi nesnesi
	 */
	public int compareTo(KayitBasligi arg0) 
	{
		if(ipAdresi.equals(arg0.ipAdresi))
		{
			//IP adresleri ayný ise ident'i büyük olan büyüktür.
			return this.ident-arg0.ident;
		}
		else
		{
			//IP adresi büyük olan büyüktür
			byte[] adres1 = this.ipAdresi.getAddress();
			byte[] adres2 = arg0.ipAdresi.getAddress();

			for(int i=0;i<4;i++)
				if(adres1[i]!=adres2[i])
					return (adres1[i]-adres2[i]);	
		}
		//hem IP adresi hem de ident deðeri eþitse bu baþlýklar eþittir. 
		return 0;
	}
	//---------------------------------------------------------------------
	/**
	 * Bu KayitBasligi nesnesi ile verilen nesne eþitse true aksi halde
	 * false dönderir.
	 * @param o karþýlaþtýrýlacak nesne.
	 */
	public boolean equals(Object o)
	{
		//hem IP adresi hem de ident deðeri eþitse bu baþlýklar eþittir. 
		KayitBasligi kb = (KayitBasligi)o;
		if(this.ipAdresi.equals(kb.ipAdresi) && this.ident==kb.ident)
			return true;
		return false;
	}
}
