package tcpip.tcp;

/**
 * Bir TCP ba�lant�s�n�n durumlar�n� temsil eder.
 * 
 * @author HAL�L
 */
public enum TCPDurumlari 
{
	CLOSED,
	LISTEN,
	SYN_RECEIVED,
	SYN_SENT,
	ESTABLISHED,
	CLOSE_WAIT,
	CLOSING,
	FIN_WAIT_1,
	FIN_WAIT_2,
	LAST_ACK,
	TIMED_WAIT
}
