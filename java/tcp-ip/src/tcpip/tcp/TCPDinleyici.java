package tcpip.tcp;

import jpcap.packet.*;

/**
 * TCP s�n�f� ile Application Socket Layer'�n ileti�imini standart hale
 * getirmek i�in geli�tirilen ve ASL taraf�ndan kullan�lmas� �ng�r�len
 * aray�z. TCP s�n�f� isteklere cevaplar� bu aray�z �zerinden verir ve yine
 * gelen ilgili segmentleri ASL'ye bu aray�z �zerinden aktar�r.
 * 
 * @author HAL�L
 */
public interface TCPDinleyici 
{
	/**
	 * Bu ba�lant�yla ili�kili gelen paketleri alabilmek i�in
	 * @param yeniPaket A�dan gelen yeni TCP paketi
	 */
	public void paketAl(TCPPacket yeniPaket);	
}
