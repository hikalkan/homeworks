package tcpip.tcp;

/**
 * KayanPencere mekanizmas�na veri eklendi�inde, bu verileri
 * hedefe g�ndermek i�in bekleyen nesneyi bilgilendirmek
 * i�in kullan�lan aray�z.
 * 
 * @author Halil �brahim KALKAN
 */
public interface VeriDinleyicisi 
{
	/**
	 * Tampona en az 1 byte veri eklendi�inde bu metod �a��r�l�r.
	 */
	public void veriEklendi();
}
