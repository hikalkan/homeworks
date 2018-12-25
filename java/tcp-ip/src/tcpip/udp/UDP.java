package tcpip.udp;

import jpcap.packet.*;
import tcpip.ip.IP;

/**
 * UDP protokol�n� uygulayan s�n�f.
 * 
 * @author Halil �brahim KALKAN 
 */
public class UDP 
{
	// Private de�i�kenler ////////////////////////////////////////////////////
	/** IP protokol�n� kullanabilmek i�in gerekli referans */
	private IP									ip;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	/**
	 * IP protokol�ne olan referans� ayarlanm�� bir UDP nesnesi olu�turur.
	 * @param ip IP protokol�n� uygulanan nesne
	 */
	public UDP(IP ip)
	{
		//IP protokol�ne olan referans al�n�yor
		this.ip = ip;
	}
	
	// Public fonksyonlar /////////////////////////////////////////////////////
	/**
	 * IP katman�ndan UDP'ye pakey g�ndermek i�in kullan�lan fonksyon.
	 * @param yeniPaket A�dan gelen UDP paketi
	 */
	public void paketAl(UDPPacket yeniPaket)
	{
		
	}
}
