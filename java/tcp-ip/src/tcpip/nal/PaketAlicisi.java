package tcpip.nal;

import jpcap.PacketReceiver;
import jpcap.packet.*;
import tcpip.arp.*;
import tcpip.ip.*;

/**
 * Bu s�n�f jpcap k�t�phanesi taraf�ndan kullan�l�r ve her yeni paket yakaland���nda
 * bu s�n�f�n receivePacket fonksyonu �a��r�l�r. Bu fonksyon paketin tipine bakarak
 * ilgili protokole (katmana) paketi g�nderir.
 * @author Halil �brahim KALKAN
 */
public class PaketAlicisi implements PacketReceiver 
{
	/** ARP protokol�ne referans */
	private ARP 		arp;
	/** IP protokol�ne referans; */
	private IP			ip;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public PaketAlicisi(ARP arp, IP ip)
	{
		this.arp = arp;
		this.ip = ip;
	}
	
	// Her paket geli�inde �al���r ////////////////////////////////////////////
	/**
	 * Her paket geli�inde �al���r ve paketin tipine g�re hangi protokole
	 * g�nderece�ine karar verir.
	 * @param yeniPaket a�dan yeni gelen paket
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
