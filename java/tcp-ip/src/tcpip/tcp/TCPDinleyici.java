package tcpip.tcp;

import jpcap.packet.*;

/**
 * TCP sýnýfý ile Application Socket Layer'ýn iletiþimini standart hale
 * getirmek için geliþtirilen ve ASL tarafýndan kullanýlmasý öngörülen
 * arayüz. TCP sýnýfý isteklere cevaplarý bu arayüz üzerinden verir ve yine
 * gelen ilgili segmentleri ASL'ye bu arayüz üzerinden aktarýr.
 * 
 * @author HALÝL
 */
public interface TCPDinleyici 
{
	/**
	 * Bu baðlantýyla iliþkili gelen paketleri alabilmek için
	 * @param yeniPaket Aðdan gelen yeni TCP paketi
	 */
	public void paketAl(TCPPacket yeniPaket);	
}
