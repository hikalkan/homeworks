package tcpip.tcp;

/**
 * KayanPencere mekanizmasýna veri eklendiðinde, bu verileri
 * hedefe göndermek için bekleyen nesneyi bilgilendirmek
 * için kullanýlan arayüz.
 * 
 * @author Halil Ýbrahim KALKAN
 */
public interface VeriDinleyicisi 
{
	/**
	 * Tampona en az 1 byte veri eklendiðinde bu metod çaðýrýlýr.
	 */
	public void veriEklendi();
}
