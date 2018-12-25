package tcpip.map;

import java.net.*;
import jpcap.packet.*;
import tcpip.ip.*;
import java.util.*;

public class MAP 
{
	private IP									ip;
	public static final short 					protokolNumarasi = 253;
	private short								maxPaketBoyutu = 1480;
	private String 								onMesaj = "[MAPM]";
	private byte[] 								onMesajDizisi = onMesaj.getBytes();
	private MAPDinleyici						dinleyici;
	
	public MAP(IP ip)
	{
		this.ip = ip;
	}
	
	public void paketAl(IPPacket p)
	{
		if(dinleyici!=null)
		{
			if(p.data!=null)
			{
				if(p.data.length > onMesajDizisi.length && p.data.length <= maxPaketBoyutu)
				{
					if(onMesajDogru(p.data))
					{
						int mesajBoyutu = p.data.length - onMesajDizisi.length;
						byte[] mesaj = new byte[mesajBoyutu];
						System.arraycopy(p.data, onMesajDizisi.length, mesaj, 0, mesajBoyutu);
						dinleyici.mesajAl(p.src_ip, mesaj);
					}
				}
			}
		}
	}
	
	public void mesajYolla(InetAddress hedefIPAdresi, byte[] mesaj)
	{
		IPPacket p = new IPPacket();
		
		p.data = new byte[onMesajDizisi.length + mesaj.length];
		System.arraycopy(onMesajDizisi, 0, p.data, 0, onMesajDizisi.length);
		System.arraycopy(mesaj, 0, p.data, onMesajDizisi.length, mesaj.length);
		
		p.protocol = MAP.protokolNumarasi;
		
		ip.paketYolla(p, (Inet4Address)hedefIPAdresi);
	}
	
	public void setMAPDinleyici(MAPDinleyici d)
	{
		dinleyici = d;
	}
	
	private boolean onMesajDogru(byte[] mesaj)
	{
		for(int i=0; i<onMesajDizisi.length; i++)
		{
			if(mesaj[i] != onMesajDizisi[i])
				return false;
		}
		return true;
	}
}
