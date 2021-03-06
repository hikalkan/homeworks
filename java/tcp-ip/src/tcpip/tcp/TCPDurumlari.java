package tcpip.tcp;

/**
 * Bir TCP bağlantısının durumlarını temsil eder.
 * 
 * @author HALİL
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
