package tcpip.udp;

import jpcap.packet.*;
import tcpip.ip.IP;

/**
 * UDP protokolünü uygulayan sýnýf.
 * 
 * @author Halil Ýbrahim KALKAN 
 */
public class UDP 
{
	// Private deðiþkenler ////////////////////////////////////////////////////
	/** IP protokolünü kullanabilmek için gerekli referans */
	private IP									ip;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	/**
	 * IP protokolüne olan referansý ayarlanmýþ bir UDP nesnesi oluþturur.
	 * @param ip IP protokolünü uygulanan nesne
	 */
	public UDP(IP ip)
	{
		//IP protokolüne olan referans alýnýyor
		this.ip = ip;
	}
	
	// Public fonksyonlar /////////////////////////////////////////////////////
	/**
	 * IP katmanýndan UDP'ye pakey göndermek için kullanýlan fonksyon.
	 * @param yeniPaket Aðdan gelen UDP paketi
	 */
	public void paketAl(UDPPacket yeniPaket)
	{
		
	}
}
