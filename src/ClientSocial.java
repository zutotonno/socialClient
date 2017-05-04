import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientSocial extends Remote {
	public void message(String message) throws RemoteException;
}