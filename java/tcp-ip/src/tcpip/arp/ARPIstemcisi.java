package tcpip.arp;

import java.net.*;
import tcpip.*;

/** 
 * ARP protokol�ne bir IP->MAC e�le�tirme talebi yap�ld���nda cevab� alabilmek
 * i�in geli�tirilen aray�z. ARP s�n�f�n� kullanarak ARP sorgusu yapacak
 * s�n�f ya bu aray�z� uygulamal�d�r ya da bu aray�z� uygulayan bir s�n�f�
 * kullanmal�d�r.
 * 
 * @author Halil �brahim KALKAN
 * */
public interface ARPIstemcisi 
{
	/** 
	 * ARP protokol� bir ARP_REQUEST paketine cevap al�nca gerekli bilgileri bu
	 * fonksyona g�nderir.
	 * @param talepID Bu talep i�in bir ID numaras�
	 * @param ipAdresi MAC adresi istenen IP adresi
	 * @param macAdresi Bulunan MAC adresi
	 * @param onBellek Bulunan mac adresi �n bellekten al�nd�ysa true aksi halde false
	 */
	public void MACal(long talepID, Inet4Address ipAdresi, MACAdresi macAdresi, boolean onBellek);
	/**
	 * ARP protokol� bu ip adresini a�da bulamad�ysa bu fonksyonu �a��r�r
	 * @param talepID Bu talep i�in bir ID numaras�
	 * @param ipAdresi MAC adresi istenen IP adresi
	 */
	public void MACBulunamadi(long talepID, Inet4Address ipAdresi);
}
