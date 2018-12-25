package tcpip.hicp;

import java.util.*;

import tcpip.tcp.TCPDinleyici;

public class HICP 
{
	private static HICP hicp;
	public static HICP getHICP() { return HICP.hicp; }
	
	/** Kullan�lmayan portlar�n listesi */
	private LinkedList<Short>					bosPortListesi;
	/** A��k ve aktif olan port numaralar�n� kullanan ba�lant�lar */
	private TreeMap<Short, HICPPaketDinleyici> acikBaglantilar;
	
	public HICP()
	{
		bosPortListesi = new LinkedList<Short>();
		for(short i=50; i<=100; i++)
		{
			bosPortListesi.add(i);
		}
		acikBaglantilar = new TreeMap<Short, HICPPaketDinleyici>();
		HICP.hicp = this;
	}
	
	public void paketYolla(HICPPaketi p)
	{
		
	}
	
	public int baslangicPaketNumarasiUret()
	{
		return 1000;
	}
	
	public short portNumarasiTahsisEt(HICPPaketDinleyici dinleyici)
	{
		if(bosPortListesi.size()<=0)
		{
			return -1;
		}
		else
		{
			short portNo = bosPortListesi.poll();
			acikBaglantilar.put(portNo, dinleyici);
			return portNo;
		}
	}
}
