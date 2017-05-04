import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class M_Client implements Runnable {
	String name;
	AtomicBoolean startUDP;

	public M_Client(AtomicBoolean startUDP) {
		this.startUDP = startUDP;
	}

	// AGGIUSTARE IL CLIENT
	@Override
	public void run() {
		synchronized (startUDP) {
			while (!startUDP.get()) {
				try {
					startUDP.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		try (DatagramChannel multicastClient = DatagramChannel.open(StandardProtocolFamily.INET);
				DatagramChannel clientUDP = DatagramChannel.open()) {
			multicastClient.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			multicastClient.bind(new InetSocketAddress(Main_Client.MULTICAST_PORT));
			System.out.println(multicastClient.join(InetAddress.getByName(Main_Client.MC_GROUP),
					NetworkInterface.getByInetAddress(InetAddress.getByName(Main_Client.CLIENT_HOST))));
			clientUDP.connect(new InetSocketAddress(InetAddress.getByName(Main_Client.HOST), Main_Client.UDP));
			while (!Main_Client.stop) {
				name=Main_Client.user;
				ByteBuffer rcv = ByteBuffer.allocate(64);
				rcv.clear();
				multicastClient.receive(rcv);
				rcv.flip();
				rcv.getInt();
				StringBuilder sb = new StringBuilder();
				String msg;
				char c;
				while ((c = rcv.getChar()) != '#') {
					sb.append(c);
				}
				msg = sb.toString();
				if (!msg.equals("CLOSE")) {
					msg = name + "#";
					rcv.clear();
					rcv.putInt(msg.length());
					for (int i = 0; i < msg.length(); i++) {
						rcv.putChar(msg.charAt(i));
					}
					rcv.flip();
					clientUDP.write(rcv);
				}
			}
			System.out.println("Closing M_Client");
		} catch (IOException e) {
			// selector.wakeup();
			System.err.println("ERR - connecting Server");
			e.printStackTrace();
		}
	}

	/*private String loadUser() {
		try {
			FileInputStream fileIn = new FileInputStream("user.ser");
			ObjectInputStream inObj = new ObjectInputStream(fileIn);
			TinyUser usr = (TinyUser) inObj.readObject();
			String u = usr.getUser();
			inObj.close();
			fileIn.close();
			return u;
		} catch (FileNotFoundException e) {
			System.out.println("First Login");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}*/

}
