import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;

public class ClientSocialImpl extends RemoteObject implements ClientSocial {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void message(String message) throws RemoteException {
		//System.out.println(message);
		Main_Client.wall.add(message);
		synchronized (Main_Client.save) {
			Main_Client.save.set(true);
			Main_Client.save.notify();
		}
	}
}
