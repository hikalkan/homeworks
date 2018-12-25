package tcpip.mapmessenger;

import java.awt.*;
import java.awt.event.*;
import java.net.Inet4Address;
import java.net.InetAddress;

import tcpip.*;
import tcpip.map.*;
import test.BirMAPDinleyici;

public class MesajPenceresi extends Frame implements MAPDinleyici
{
	public Label lblIPYaz;
	public TextField ipAlani;
	public TextField yaziAlani;
	public TextArea mesajlar;
	
	private InetAddress IPAdresi;
	private String txtIPAdresi = "";
	
	MyTCPIP myTCPIP;
	MAP map;
	
	public MesajPenceresi()
	{
		ekraniHazirla();
		protokolKumesiniBaslat();
	}
	
	public void mesajYolla(byte[] mesaj)
	{
		String ipAdresi = ipAlani.getText(); 
		if(ipAdresi!=null)
		{
			try
			{
				InetAddress karsiAdres = null;
				if(txtIPAdresi.equals(ipAdresi))
				{
					karsiAdres = IPAdresi;
				}
				else
				{
					karsiAdres = Inet4Address.getByName(ipAdresi);
					txtIPAdresi = ipAdresi;
					IPAdresi = karsiAdres;
				}
				//Inet4Address karsiAdres = (Inet4Address)Inet4Address.getByAddress(new byte[]{(byte)192,(byte)168,(byte)1,(byte)4});
				mesajlar.append("Giden mesaj: " + new String(mesaj) + " (" + karsiAdres +")\r\n");
				map.mesajYolla(karsiAdres, mesaj);
			}
			catch (Exception e)
			{
				mesajlar.append("IP adresi hatalý!!!\r\n");
			}
		}
	}

	public void mesajAl(InetAddress ipAdresi, byte[] mesaj) 
	{
		String m = new String(mesaj);
		mesajlar.append("Gelen mesaj: " + m + "\r\n");
	}

	private void protokolKumesiniBaslat()
	{
		myTCPIP = new MyTCPIP();
		if(!myTCPIP.baslat())
		{
			mesajlar.append("MyTCPIP protokolü baþlatýlamadý!!!");
			return;
		}
		try { Thread.sleep(1000); } catch (Exception e) { }
		map = myTCPIP.getMAP();
		map.setMAPDinleyici(this);
	}
	
	public void protokolKumesiniDurdur()
	{
		myTCPIP.durdur();
		try { Thread.sleep(1000); } catch (Exception e) { }
	}
	
	private void ekraniHazirla()
	{
		setLayout(null);
		setTitle("IP Messenger");
		setSize(550,384);
		addWindowListener(new MessengerKapatici(this));
		
		lblIPYaz = new Label("Hedef IP Adresi: ");
		lblIPYaz.setBounds(10, 40, 100, 24);
		
		ipAlani = new TextField(20); 
		ipAlani.setBounds(120, 40, 200, 24);
		ipAlani.setText("192.168.1.4");
		
		yaziAlani = new TextField(50);
		yaziAlani.setBounds(10, 350, 530, 24);
		yaziAlani.addActionListener(new MesajGonderici(this));
		
		mesajlar = new TextArea(10,50);
		mesajlar.setEditable(false);
		mesajlar.setBounds(10, 70, 530, 270);

		add(lblIPYaz);
		add(ipAlani);
		add(mesajlar);
		add(yaziAlani);
	}
}

class MessengerKapatici extends WindowAdapter
{
	MesajPenceresi pencere;

	public MessengerKapatici(MesajPenceresi pencere)
	{
		this.pencere = pencere;
	}
	
	public void windowClosing(WindowEvent we)
	{
		System.exit(0);
	}
}

class MesajGonderici implements ActionListener
{
	MesajPenceresi pencere;
	
	public MesajGonderici(MesajPenceresi pencere)
	{
		this.pencere = pencere;
	}
	public void actionPerformed(ActionEvent arg0) 
	{
		if(pencere.yaziAlani.getText()!=null)
		{
			String mes = pencere.yaziAlani.getText();
			pencere.mesajYolla(mes.getBytes());
			pencere.yaziAlani.setText("");
		}
	}	
}
