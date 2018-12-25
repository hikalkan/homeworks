package tcpip.ip;

import java.net.Inet4Address;

import tcpip.MACAdresi;
import tcpip.arp.ARPIstemcisi;

/**
 * IP ARP istemcisi.
 * IP protokolünün ARP protokolünden MAC adreslerini alabilmesi amacýyla
 * geliþtirilen sýnýf.
 * 
 * @author Halil Ýbrahim KALKAN
 * */
public class IPARPIstemcisi implements ARPIstemcisi 
{
	/**
	 * IP protokolüne bir referans. Alýnan ARP cevaplarý bu referans yardýmýyla
	 * IP protokolüne aktarýlýr
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
