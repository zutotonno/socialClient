import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Thread_Saver implements Runnable {
	File backWall;
	ArrayList<String> wall;

	public Thread_Saver(File backWall, ArrayList<String> wall) {
		this.backWall = backWall;
		this.wall = wall;
	}

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
				synchronized (Main_Client.save) {
					while (Main_Client.save.get() == false) {
						Main_Client.save.wait();
					}
					FileOutputStream fileOut = new FileOutputStream("backWall.ser");
					ObjectOutputStream outFile = new ObjectOutputStream(fileOut);
					outFile.writeObject(wall);
					outFile.close();
					fileOut.close();
					Main_Client.save.set(false);
				}
			}
		} catch (InterruptedException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Bye");
	}

}
