package tcpip.nal;

import java.util.concurrent.LinkedBlockingQueue;
import jpcap.*;
import jpcap.packet.*;

/**
 * Bu s�n�f jpcap arac�l���yla network'a paket g�ndermek i�in kullan�l�r.
 * Bir thread olarak �al���r ve e�er NetworkKuyrugu bo�sa yeni bir paket
 * gelene kadar beklemede kal�r (wait()). Yeni bir paket gelince bu paketi g�nderen
 * thread haber verir (notifyAll()) ve bu Thread de paketi a�a g�nderir
 * ve e�er NetworkKuyrugu'nda paket kalmam��sa yeniden bekleme konumuna ge�er.
 * @author Halil �brahim KALKAN
 */
public class PaketYollayicisi extends Thread
{
	/** a�a paket g�nderebilmek i�in JpcapSender nesnesine bir referans */
	private JpcapSender 					sender;
	/** �st katmanlardan gelen paketleri tutan kuyru�a bir referans */
	private LinkedBlockingQueue<Packet>		NetworkKuyrugu;
	/** protokol� y�netmek i�in bayrak */
	private volatile boolean				calisiyor = true;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public PaketYollayicisi(JpcapSender sender, LinkedBlockingQueue<Packet> NetworkKuyrugu)
	{
		//Bu Thread'e bir isim ver
		setName("Paket Yollayicisi");
		//Paket g�ndermek i�in referans al
		this.sender = sender;
		//�st katmanlardan gelen paketlerin sakland��� kuyru�a bir referans
		this.NetworkKuyrugu = NetworkKuyrugu;
	}
	
	// private fonksyonlar ////////////////////////////////////////////////////
	/**
	 * Bu fonksyon JpcapSender s�n�f�ndan �rneklenen sender nesnesini kullanarak
	 * a�a bir paket yollar.
	 * @param p A�a yoolanacak paket
	 */
	private void paketYolla(Packet p)
	{	
		sender.sendPacket(p);
	}
	
	// Thread fonksyonlar� ////////////////////////////////////////////////////
	/**
	 * Thread'in giri� noktas�.
	 * Bu s�n�f e�er �st katmandan gelen paketleri tutan NetworkKuyrugu adl�
	 * kuyrukta hi� paket yoksa bekleme konumuna ge�er (wait()). E�er paket
	 * varsa ilk paketi paketYolla fonksyonu ile a�a yollar.
	 */
	public void run()
	{
		Packet yeniPaket;
		while(calisiyor)
		{
			//network kuyru�unda paket varsa g�nderiliyor yoksa bekleniyor
			yeniPaket = null;
			synchronized(NetworkKuyrugu)
			{
				if(NetworkKuyrugu.size()<=0)
				{
					try 
					{
						NetworkKuyrugu.wait();
					} catch (InterruptedException e) {
						//...
					}
				}
				else
				{
					yeniPaket = NetworkKuyrugu.poll();
				}
			}
			if(yeniPaket!=null)
				paketYolla(yeniPaket);
		}
	}
	//-------------------------------------------------------------------------
	/**
	 * Bu Thread'in �al��mas�n� durdurmak i�in
	 */
	public void durdur()
	{
		calisiyor = false;
		synchronized(NetworkKuyrugu)
		{
			NetworkKuyrugu.notifyAll();
		}
	}
}
