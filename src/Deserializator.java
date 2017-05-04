import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class Deserializator {
	static TinyUser u;
	static ArrayList<String> pr;
	static ArrayList<String> bw;
	public static void main(String[] args) {
		try {
			FileInputStream fileIn = new FileInputStream("user.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			u = (TinyUser) in.readObject();
			in.close();
			fileIn.close();
			System.out.println(u.getUser()+u.getPassword()+u.getToken());
			FileInputStream fileIn3= new FileInputStream("backWall.ser");
			ObjectInputStream in3 = new ObjectInputStream(fileIn3);
			bw = (ArrayList<String>) in3.readObject();
			System.out.println("Not seen messages:");
			System.out.println(bw);
			in3.close();
			fileIn3.close();
			FileInputStream fileIn2 = new FileInputStream("pendingRequest.ser");
			ObjectInputStream in2 = new ObjectInputStream(fileIn2);
			pr = (ArrayList<String>) in2.readObject();
			in2.close();
			fileIn2.close();
			System.out.println("Pending Request:");
			for(String s:pr){
				System.out.println(s);
			}
		} catch (IOException i) {
			//i.printStackTrace();
			return;
		} catch (ClassNotFoundException c) {
			System.out.println("TinyUser class not found");
			c.printStackTrace();
			return;
		}
		//System.out.println(u.getToken());

	}

}
