package test;

import java.net.InetAddress;
import tcpip.map.MAPDinleyici;

public class BirMAPDinleyici implements MAPDinleyici 
{
	public void mesajAl(InetAddress ipAdresi, byte[] mesaj) 
	{
		System.out.println("Mesaj alindi: " + new String(mesaj));
	}
}
