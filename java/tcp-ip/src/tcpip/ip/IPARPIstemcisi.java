package tcpip.ip;

import java.net.Inet4Address;

import tcpip.MACAdresi;
import tcpip.arp.ARPIstemcisi;

/**
 * IP ARP istemcisi.
 * IP protokol�n�n ARP protokol�nden MAC adreslerini alabilmesi amac�yla
 * geli�tirilen s�n�f.
 * 
 * @author Halil �brahim KALKAN
 * */
public class IPARPIstemcisi implements ARPIstemcisi 
{
	/**
	 * IP protokol�ne bir referans. Al�nan ARP cevaplar� bu referans yard�m�yla
	 * IP protokol�ne aktar�l�r
	 */
	private IP ip = null;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public IPARPIstemcisi(IP ip)
	{
		this.ip = ip;
	}
	
	// ARPIstemcisi ///////////////////////////////////////////////////////////
	/**
	 * {@inheritDoc}
	 */
	public void MACal(long talepID, Inet4Address ipAdresi, MACAdresi macAdresi, boolean onBellek) 
	{
		ip.MACal(talepID, ipAdresi, macAdresi, onBellek);
	}
	/**
	 * {@inheritDoc}
	 */
	public void MACBulunamadi(long talepID, Inet4Address ipAdresi)
	{
		ip.MACBulunamadi(talepID, ipAdresi);
	}
}
