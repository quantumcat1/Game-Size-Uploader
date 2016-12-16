import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class KeyboardListenerThread extends Thread {
	boolean isQuit;
	BufferedReader console;

	public KeyboardListenerThread() {
		console = new BufferedReader(new InputStreamReader(System.in));
		isQuit = false;
	}

	@Override
	public void run() {
		while (!isQuit) {
			String ans = "";
			try {
				ans = console.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (ans.equals("q"))
				isQuit = true;
		}
	}

	public boolean isQuit() {
		return isQuit;
	}
}
