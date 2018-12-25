package tcpip.nal;

import java.util.concurrent.LinkedBlockingQueue;
import jpcap.*;
import jpcap.packet.*;

/**
 * Bu sýnýf jpcap aracýlýðýyla network'a paket göndermek için kullanýlýr.
 * Bir thread olarak çalýþýr ve eðer NetworkKuyrugu boþsa yeni bir paket
 * gelene kadar beklemede kalýr (wait()). Yeni bir paket gelince bu paketi gönderen
 * thread haber verir (notifyAll()) ve bu Thread de paketi aða gönderir
 * ve eðer NetworkKuyrugu'nda paket kalmamýþsa yeniden bekleme konumuna geçer.
 * @author Halil Ýbrahim KALKAN
 */
public class PaketYollayicisi extends Thread
{
	/** aða paket gönderebilmek için JpcapSender nesnesine bir referans */
	private JpcapSender 					sender;
	/** üst katmanlardan gelen paketleri tutan kuyruða bir referans */
	private LinkedBlockingQueue<Packet>		NetworkKuyrugu;
	/** protokolü yönetmek için bayrak */
	private volatile boolean				calisiyor = true;
	
	// Kurucu fonksyonlar /////////////////////////////////////////////////////
	public PaketYollayicisi(JpcapSender sender, LinkedBlockingQueue<Packet> NetworkKuyrugu)
	{
		//Bu Thread'e bir isim ver
		setName("Paket Yollayicisi");
		//Paket göndermek için referans al
		this.sender = sender;
		//Üst katmanlardan gelen paketlerin saklandýðý kuyruða bir referans
		this.NetworkKuyrugu = NetworkKuyrugu;
	}
	
	// private fonksyonlar ////////////////////////////////////////////////////
	/**
	 * Bu fonksyon JpcapSender sýnýfýndan örneklenen sender nesnesini kullanarak
	 * aða bir paket yollar.
	 * @param p Aða yoolanacak paket
	 */
	private void paketYolla(Packet p)
	{	
		sender.sendPacket(p);
	}
	
	// Thread fonksyonlarý ////////////////////////////////////////////////////
	/**
	 * Thread'in giriþ noktasý.
	 * Bu sýnýf eðer üst katmandan gelen paketleri tutan NetworkKuyrugu adlý
	 * kuyrukta hiç paket yoksa bekleme konumuna geçer (wait()). Eðer paket
	 * varsa ilk paketi paketYolla fonksyonu ile aða yollar.
	 */
	public void run()
	{
		Packet yeniPaket;
		while(calisiyor)
		{
			//network kuyruðunda paket varsa gönderiliyor yoksa bekleniyor
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
	 * Bu Thread'in çalýþmasýný durdurmak için
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
