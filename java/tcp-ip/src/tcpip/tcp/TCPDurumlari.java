package tcpip.tcp;

/**
 * Bir TCP baðlantýsýnýn durumlarýný temsil eder.
 * 
 * @author HALÝL
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
