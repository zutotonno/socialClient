import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MiniServer implements Runnable {
	ServerSocket s;
	ArrayList<String> pendingRequest;
	File myFile;
	public MiniServer(ServerSocket serverClient, File myFile, ArrayList<String> pendingRequest) {
		s = serverClient;
		this.pendingRequest=pendingRequest;
		this.myFile=myFile;
	}

	@Override
	public void run() {
		Socket client = null;
		while (!Main_Client.stop) {
			try {
				client = s.accept();
				BufferedReader b = new BufferedReader(new InputStreamReader(client.getInputStream()));
				String name = b.readLine();
				if (!name.equals("CLOSE")) {
					System.out.println("Fried Request: " + name);
					if (!pendingRequest.contains(name)) {
						pendingRequest.add(name);
						saveRequests(pendingRequest);
						System.out.println("Request Saved");
					}
					else{
						System.out.println(name+" is stalking you!!");
					}
				}
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Closing MiniServer");
	}

	private void saveRequests(ArrayList<String> pendingRequest) {
		try {
			FileOutputStream fileOut = new FileOutputStream(myFile);
			ObjectOutputStream outFile = new ObjectOutputStream(fileOut);
			synchronized (pendingRequest) {
				outFile.writeObject(pendingRequest);
				outFile.close();
				fileOut.close();
			}
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

	}

}
