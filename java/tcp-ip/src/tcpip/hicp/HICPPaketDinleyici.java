package tcpip.hicp;

import java.net.*;

public interface HICPPaketDinleyici 
{
	public void paketAl(InetAddress kaynakIP, HICPPaketi p);
}
