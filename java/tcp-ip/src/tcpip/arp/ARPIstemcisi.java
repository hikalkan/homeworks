package tcpip.arp;

import java.net.*;
import tcpip.*;

/** 
 * ARP protokolüne bir IP->MAC eþleþtirme talebi yapýldýðýnda cevabý alabilmek
 * için geliþtirilen arayüz. ARP sýnýfýný kullanarak ARP sorgusu yapacak
 * sýnýf ya bu arayüzü uygulamalýdýr ya da bu arayüzü uygulayan bir sýnýfý
 * kullanmalýdýr.
 * 
 * @author Halil Ýbrahim KALKAN
 * */
public interface ARPIstemcisi 
{
	/** 
	 * ARP protokolü bir ARP_REQUEST paketine cevap alýnca gerekli bilgileri bu
	 * fonksyona gönderir.
	 * @param talepID Bu talep için bir ID numarasý
	 * @param ipAdresi MAC adresi istenen IP adresi
	 * @param macAdresi Bulunan MAC adresi
	 * @param onBellek Bulunan mac adresi ön bellekten alýndýysa true aksi halde false
	 */
	public void MACal(long talepID, Inet4Address ipAdresi, MACAdresi macAdresi, boolean onBellek);
	/**
	 * ARP protokolü bu ip adresini aðda bulamadýysa bu fonksyonu çaðýrýr
	 * @param talepID Bu talep için bir ID numarasý
	 * @param ipAdresi MAC adresi istenen IP adresi
	 */
	public void MACBulunamadi(long talepID, Inet4Address ipAdresi);
}
