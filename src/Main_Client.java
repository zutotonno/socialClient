import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main_Client {
	static String MC_GROUP;
	static int UDP;
	static int MULTICAST_PORT;
	static int Client_Server_Socket_PORT;
	static String Registry_Host;
	static int Registry_Port;
	public static int PORT;
	public static String HOST;
	public static String CLIENT_HOST;
	private static String token,password;
	static String user;
	static final String ok = "OK";
	static final String wr_pass = "Wrong Password!";
	static final String wr_us = "Wrong User!";
	static final String wr_token = "Wrong Token!";
	static final String usr_offline = "User Offline!";
	static final String rem_err = "Wrong User or Token!";
	static final String saved_4_later = "Saved for later: OK";
	static ArrayList<String> wall = new ArrayList<String>();
	static File myFile, backWall;
	static ArrayList<String> pendingRequest;
	static AtomicBoolean startUDP;
	static boolean stop = false;
	static AtomicBoolean save = new AtomicBoolean(false);
	static ExecutorService es;
	static Thread thr;
	static boolean logout = false;

	public static void main(String[] args) {
		System.out.println("MAIN_CLIENT");
		es = Executors.newCachedThreadPool();
		ClientSocial userCB = null;
		try {
			ServerSocket serverClient = new ServerSocket();
			setup();
			serverClient.bind(new InetSocketAddress(CLIENT_HOST, Client_Server_Socket_PORT));
			BufferedReader keyIN = new BufferedReader(new InputStreamReader(System.in));
			if (loadUser() == 0) // load user, if exists
				System.out.println("Your last Login was: " + user);
			myFile = new File("pendingRequest.ser");
			if (!myFile.exists()) {
				myFile.createNewFile();
				System.out.println("Created");
			}
			backWall = new File("backWall.ser");
			if (!backWall.exists()) {
				backWall.createNewFile();
				System.out.println("Created");
			}
			loadRequests();
			loadWall();
			Runnable miniServer = new MiniServer(serverClient, myFile, pendingRequest);
			Runnable udpKA = new M_Client(startUDP);
			Runnable saveWall = new Thread_Saver(backWall, wall);
			thr = new Thread(saveWall);
			thr.start();
			es.submit(miniServer);
			es.submit(udpKA);
			userCB = new ClientSocialImpl();
			ClientSocial userStub = (ClientSocial) UnicastRemoteObject.exportObject(userCB, 0);
			Registry registry = LocateRegistry.getRegistry(Registry_Host, Registry_Port);
			GestSocial network = (GestSocial) registry.lookup("Social");
			while (!stop) {
				System.out.println(
						"Type ... \n'Reg' to register as new User\n'Login' to login\n'Open' to open application\n'show' to show your wall");
				String select = keyIN.readLine().toLowerCase().trim();
				if (select.equals("reg")) {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(HOST, PORT));
					registration(socket, keyIN, select, userCB);
				}
				if (select.equals("login")) {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(HOST, PORT));
					if (login(socket, keyIN, select, userStub, network, userCB) == 0) {
						synchronized (startUDP) {
							if (!startUDP.get()) {
								startUDP.set(true);
								startUDP.notify();
							}
						}
					}
				}
				if (select.equals("open")) {
					if (user != null && Integer.parseInt(token) > 0) {
						Socket socket = new Socket();
						socket.connect(new InetSocketAddress(HOST, PORT));
						openApp(socket, keyIN, select, user, token, userCB, network);
					} else
						System.err.println("You must Login first");
				}
				if (select.equals("show")) {
					System.out.println(wall);
					wall.clear();
					synchronized (Main_Client.save) {
						Main_Client.save.set(true);
						Main_Client.save.notify();
					}
				}
			}

		} catch (IOException e) {
			// serverDown();
			closeClient(userCB);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		System.out.println("Exiting ...");

	}

	private static void openApp(Socket socket, BufferedReader keyIN, String select, String u, String t,
			ClientSocial userCB, GestSocial network) throws IOException {
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out.write(select + "\r");
			out.flush();
			System.out.println(
					"Select:\n'0' Send a Friendship request\n'1' Accept a Friendship request\n'3' Request your friend list"
							+ "\n'4' Search for an User\n'5' Publish a content\n'6' Follow an User\n'8' Logout");
			String action = keyIN.readLine();
			if (action.equals("0")) {
				send_friendship_Request(keyIN, in, out, action, u, t, socket, userCB);
			} else if (action.equals("1")) {
				accept_friendship_Request(keyIN, in, out, action, u, t, socket, userCB);
			} else if (action.equals("3")) {
				get_Friendlist(in, out, action, u, t, socket, userCB);
			} else if (action.equals("4")) {
				search_Users(keyIN, in, out, action, u, t, socket, userCB);
			} else if (action.equals("5")) {
				publish_Content(keyIN, in, out, action, u, t, socket, userCB);
			} else if (action.equals("6")) {
				out.write("6\r");
				out.flush();
				out.close();
				in.close();
				socket.close();
				follow_user(keyIN, u, t, network); // RMI
			} else if (action.equals("8")) {
				logout_Handler(in, out, action, u, t, socket, userCB);
			}
		} catch (SocketException e) {
			closeClient(userCB);
		}
	}

	private static void follow_user(BufferedReader keyIN, String u, String t, GestSocial network) throws IOException {
		System.out.println("Write an user you'd want to follow");
		String us = keyIN.readLine();
		if (!us.equals(user)) {
			if (network.followUser(u, us, t) == true)
				System.out.println(ok);
			else
				System.out.println(rem_err);
		} else {
			System.err.println("You can't follow yourself");
		}
	}

	private static void publish_Content(BufferedReader keyIN, BufferedReader in, BufferedWriter out, String action,
			String u, String t, Socket socket, ClientSocial userCB) throws IOException {
		System.out.println("Write a content:");
		String content = keyIN.readLine();
		out.write(action + "\r");
		out.flush();
		out.write(u + "\r");
		out.flush();
		out.write(t + "\r");
		out.flush();
		out.write(u + ": " + content + "\r");
		out.flush();
		try {
			int cod = Integer.parseInt(in.readLine());
			checkCode(cod);
		} catch (NumberFormatException e) {
			System.err.println("Content lost");
		}
	}

	private static void get_Friendlist(BufferedReader in, BufferedWriter out, String action, String u, String t,
			Socket socket, ClientSocial userCB) throws IOException {
		try {
			System.out.println("Requesting your Friend List...");
			out.write(action + "\r");
			out.flush();
			out.write(u + "\r");
			out.flush();
			out.write(t + "\r");
			out.flush();
			int cod = Integer.parseInt(in.readLine());
			if (checkCode(cod) == 0) {
				int dim = Integer.parseInt(in.readLine());
				if (dim > 0) {
					for (int i = 0; i < dim; i++) {
						System.out.println("USER:" + in.readLine() + " Status:" + in.readLine());
					}
				} else {
					System.out.println("You have still no friends");
				}
			}
			socket.close();
			out.close();
			in.close();
		} catch (SocketException e) {
			closeClient(userCB);
		}

	}

	private static void search_Users(BufferedReader keyIN, BufferedReader in, BufferedWriter out, String action,
			String u, String t, Socket socket, ClientSocial userCB) throws NumberFormatException, IOException {
		try {
			System.out.println("Type a string");
			String user_to_find = keyIN.readLine();
			out.write(action + "\r");
			out.flush();
			out.write(u + "\r");
			out.flush();
			out.write(t + "\r");
			out.flush();
			out.write(user_to_find + "\r");
			out.flush();
			int cod = Integer.parseInt(in.readLine());
			if (checkCode(cod) == 0) {
				System.out.println("User List:");
				int dim = Integer.parseInt(in.readLine());
				for (int i = 0; i < dim; i++) {
					System.out.println(in.readLine());
				}
			}
			socket.close();
			out.close();
			in.close();
		} catch (SocketException e) {
			closeClient(userCB);
		}
	}

	private static void accept_friendship_Request(BufferedReader keyIN, BufferedReader in, BufferedWriter out,
			String action, String u, String t, Socket socket, ClientSocial userCB) throws IOException {
		try {
			System.out.println("Your Pending Request: ..");
			synchronized (pendingRequest) {
				for (String s : pendingRequest) {
					System.out.println(s);
				}
			}
			if (!pendingRequest.isEmpty()) {
				System.out.println("Type user");
				String friend = keyIN.readLine();
				System.out.println("Type 'accept' or 'reject'");
				String decision = keyIN.readLine();
				if (pendingRequest.contains(friend)) {
					if (decision.equals("accept") || decision.equals("reject")) {
						out.write(action + "\r");
						out.flush();
						out.write(u + "\r");
						out.flush();
						out.write(t + "\r");
						out.flush();
						out.write(friend + "\r");
						out.flush();
						out.write(decision + "\r");
						out.flush();
						int cod = Integer.parseInt(in.readLine());
						checkCode(cod);
						synchronized (pendingRequest) {
							pendingRequest.remove(friend);
							FileOutputStream fileOut = new FileOutputStream(myFile);
							ObjectOutputStream outFile = new ObjectOutputStream(fileOut);
							outFile.writeObject(pendingRequest);
							outFile.close();
							fileOut.close();
						}
					} else {
						out.write("null\r");
						out.flush();
						System.err.println("Not a valid selection!");
					}
				} else {
					out.write("null\r");
					out.flush();
					System.err.println("Wrong user!");
				}
			} else {
				out.write("null\r");
				out.flush();
				System.err.println("None pending request!");
			}
			socket.close();
			out.close();
			in.close();
		} catch (SocketException e) {
			closeClient(userCB);
		}

	}

	private static void logout_Handler(BufferedReader in, BufferedWriter out, String action, String u, String t,
			Socket socket, ClientSocial userCB) throws IOException {
		try {
			System.out.println("Logout");
			out.write(action + "\r");
			out.flush();
			// System.out.println(u+t);
			out.write(u + "\r");
			out.flush();
			out.write(t + "\r");
			out.flush();
			int cod = Integer.parseInt(in.readLine());
			if (checkCode(cod) == 0) {
				logout = true;
				closeClient(userCB);
			}
		} catch (SocketException e) {
			closeClient(userCB);
		}
	}

	private static int login(Socket socket, BufferedReader keyIN, String select, ClientSocial userStub,
			GestSocial network, ClientSocial userCB) throws IOException {
		int ok = 1;
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("Type name:");
			String name = keyIN.readLine();
			System.out.println("Type password");
			String pass = keyIN.readLine();
			out.write(select + "\r");
			out.flush();
			out.write(name + "\r");
			out.flush();
			out.write(pass + "\r");
			out.flush();
			int cod = Integer.parseInt(in.readLine());
			if (checkCode(cod) == 0) {
				System.out.println(network.addCallBack(name, userStub)); // RMI
				out.write(CLIENT_HOST + "\r");
				out.flush();
				out.write(Client_Server_Socket_PORT + "\r");
				out.flush();
				token = in.readLine();
				System.out.println(token);
				FileOutputStream fileOut = new FileOutputStream("user.ser");
				ObjectOutputStream outFile = new ObjectOutputStream(fileOut);
				TinyUser usr = new TinyUser(name, token, pass);
				outFile.writeObject(usr);
				outFile.close();
				fileOut.close();
				user = name;
				password = pass;
				ok = 0;
			}
			socket.close();
			in.close();
			out.close();
		} catch (SocketException e) {
			closeClient(userCB);
		}
		return ok;
	}

	private static void registration(Socket socket, BufferedReader keyIN, String select, ClientSocial userCB)
			throws IOException {
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out.write(select + "\r");
			out.flush();
			System.out.println("Type name:");
			String name = keyIN.readLine();
			System.out.println("Type password");
			String pass = keyIN.readLine();
			out.write(name + "\r");
			out.flush();
			out.write(pass + "\r");
			out.flush();
			int cod = Integer.parseInt(in.readLine());
			checkCode(cod);
			socket.close();
			out.close();
			in.close();
		} catch (SocketException e) {
			closeClient(userCB);
		}
	}

	private static void send_friendship_Request(BufferedReader keyIN, BufferedReader in, BufferedWriter out,
			String action, String u, String t, Socket socket, ClientSocial userCB) throws IOException {
		try {
			System.out.println("Type user");
			String friend = keyIN.readLine();
			if (!friend.equals(user)) {
				out.write(action + "\r");
				out.flush();
				out.write(u + "\r");
				out.flush();
				out.write(t + "\r");
				out.flush();
				out.write(friend + "\r");
				out.flush();
				int cod = Integer.parseInt(in.readLine());
				checkCode(cod);
			} else {
				out.write("null\r");
				out.flush();
				System.err.println("Can't send Friendship request to youself!");
			}
			socket.close();
			out.close();
			in.close();
		} catch (SocketException e) {
			closeClient(userCB);
		}
	}

	private static int checkCode(int code) {
		if (code == 200) {
			System.out.println(ok);
			return 0;
		} else if (code == 300)
			System.err.println(wr_pass);
		else if (code == 301)
			System.err.println(wr_us);
		else if (code == 302)
			System.err.println(wr_token);
		else if (code == 303)
			System.err.println(usr_offline);
		else if (code == 304)
			System.err.println(rem_err);
		else if (code == 201)
			System.err.println(saved_4_later);
		return -1;

	}

	private static void loadWall() throws IOException, ClassNotFoundException {
		synchronized (backWall) {

			try {
				FileInputStream fileIn;
				ObjectInputStream in;
				fileIn = new FileInputStream(backWall);
				in = new ObjectInputStream(fileIn);
				wall = (ArrayList<String>) in.readObject();
				in.close();
				fileIn.close();
			} catch (EOFException e) {
			}
		}

	}

	private static void loadRequests() throws IOException, ClassNotFoundException {
		synchronized (myFile) {
			FileInputStream fileIn;
			ObjectInputStream in;
			try {
				fileIn = new FileInputStream(myFile);
				in = new ObjectInputStream(fileIn);
				pendingRequest = (ArrayList<String>) in.readObject();
				in.close();
				fileIn.close();
			} catch (EOFException e) {
			}
		}

	}

	private static int loadUser() {
		try {
			FileInputStream fileIn = new FileInputStream("user.ser");
			ObjectInputStream inObj = new ObjectInputStream(fileIn);
			String u = null;
			String t = null;
			String p = null;
			TinyUser usr = (TinyUser) inObj.readObject();
			u = usr.getUser();
			t = usr.getToken();
			p = usr.getPassword();
			inObj.close();
			fileIn.close();
			if (user == null)
				user = u;
			if (token == null)
				token = t;
			if (password == null)
				password = p;
			return 0;
		} catch (FileNotFoundException e) {
			System.out.println("First Login");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static void closeClient(ClientSocial userCB) {
		if (!logout)
			System.out.println("SERVER DOWN!! ...");
		stop = true;
		try {
			synchronized (startUDP) {
				if (!startUDP.get()) {
					startUDP.set(true);
					startUDP.notify();
				}
			}
			UnicastRemoteObject.unexportObject(userCB, true);
			thr.interrupt();
			String msg = "CLOSE";
			msg += "#";
			ByteBuffer buff = ByteBuffer.allocate((msg.length() * 2) + 4);
			buff.putInt(msg.length());
			for (int i = 0; i < msg.length(); i++)
				buff.putChar(msg.charAt(i));
			buff.flip();
			DatagramChannel closeUDP = DatagramChannel.open();
			closeUDP.connect(new InetSocketAddress(InetAddress.getByName(MC_GROUP), Main_Client.MULTICAST_PORT));
			closeUDP.write(buff);
			Socket so = new Socket();
			so.connect(new InetSocketAddress(CLIENT_HOST, Client_Server_Socket_PORT));
			BufferedWriter close = new BufferedWriter(new OutputStreamWriter(so.getOutputStream()));
			close.write("CLOSE\r");
			close.flush();
			so.close();
			close.close();
		} catch (IOException e1) {
			System.err.println("Please shutdown manually");
		}
		es.shutdown();
	}

	private static void setup() throws IOException {
		Properties prop = new Properties();
		InputStream reader = new FileInputStream("PROP.xml");
		prop.loadFromXML(reader);
		MC_GROUP = prop.getProperty("MC_GROUP");
		UDP = Integer.parseInt(prop.getProperty("UDP"));
		MULTICAST_PORT = Integer.parseInt(prop.getProperty("multicast_port"));
		PORT = Integer.parseInt(prop.getProperty("portServer"));
		HOST = prop.getProperty("host");
		CLIENT_HOST = prop.getProperty("client_host");
		Registry_Host = prop.getProperty("Registry_host");
		Registry_Port = Integer.parseInt(prop.getProperty("Registry_port"));
		Client_Server_Socket_PORT = Integer.parseInt(prop.getProperty("Client_Server_Socket"));
		startUDP = new AtomicBoolean(false);
		pendingRequest = new ArrayList<String>();
	}

}
