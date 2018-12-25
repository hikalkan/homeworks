package tcpip.nal;

import jpcap.PacketReceiver;
import jpcap.packet.*;
import tcpip.arp.*;
import tcpip.ip.*;

/**
 * Bu sýnýf jpcap kütüphanesi tarafýndan kullanýlýr ve her yeni paket yakalandýðýnda
 * bu sýnýfýn receivePacket fonksyonu çaðýrýlýr. Bu fonksyon paketin tipine bakarak
 * ilgili protokole (katmana) paketi gönderir.
 * @author Halil Ýbrahim KALKAN
 */
public class PaketAlicisi implements PacketReceiver 
{
	/** ARP protokolüne referans */
	private ARP 		arp;
	/** IP protokolüne referans; */
	private IP			ip;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public PaketAlicisi(ARP arp, IP ip)
	{
		this.arp = arp;
		this.ip = ip;
	}
	
	// Her paket geliþinde çalýþýr ////////////////////////////////////////////
	/**
	 * Her paket geliþinde çalýþýr ve paketin tipine göre hangi protokole
	 * göndereceðine karar verir.
	 * @param yeniPaket aðdan yeni gelen paket
	 */
	public void receivePacket(Packet yeniPaket) 
	{		
		if(yeniPaket instanceof ARPPacket) 
		{
			arp.paketAl((ARPPacket)yeniPaket);
		} 
		else if(yeniPaket instanceof IPPacket) 
		{
			ip.paketAl((IPPacket)yeniPaket);
		}
	}
}
