import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

public interface GestSocial extends Remote {

	public static final String REMOTE_OBJECT_NAME = "Social";
	public int addCallBack(String user,ClientSocial c) throws RemoteException;
	public boolean followUser(String user1,String user2,String token) throws RemoteException;

}
