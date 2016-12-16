import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import de.mas.jnustool.NUSTitle;
import de.mas.jnustool.util.Settings;

public class main {
	private static final String DLC = "C";
	private static final String GAME = "G";
	private static final String UPDATE = "U";
	private static final String DEMO = "D";

	private static boolean overwriteTitles;
	private static boolean overwriteIds;
	private static boolean noFiles;
	private static boolean updatesOnly;
	private static boolean DLCOnly;
	private static boolean demosOnly;
	private static String updatesAddress;

	public static boolean sendJson(JSONObject json) {
		CloseableHttpClient httpClient = null;

		try {
			httpClient = HttpClientBuilder.create().build();
			// HttpPost post = new HttpPost("http://quantumc.at/update.php");
			HttpPost post = new HttpPost(updatesAddress);
			HttpResponse response = null;
			post.addHeader("Content-type", "application/json");

			ArrayList<BasicNameValuePair> list = new ArrayList<BasicNameValuePair>();
			list.add(new BasicNameValuePair("value", json.toString()));
			String titles = "f";
			String ids = "f";
			if (overwriteTitles)
				titles = "t";
			if (overwriteIds)
				ids = "t";
			list.add(new BasicNameValuePair("titles", titles));
			list.add(new BasicNameValuePair("ids", ids));
			post.setEntity(new UrlEncodedFormEntity(list));

			int max_retry = 5;
			int tries = 0;
			boolean good = false;
			while (!good && tries < max_retry) {
				tries++;
				try {
					response = httpClient.execute(post);
					good = true;
				} catch (NoHttpResponseException h) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("No response. Try number: " + tries);
				}
			}
			if (tries > 1 && tries < 5) {
				BufferedWriter bw = new BufferedWriter(new FileWriter("./games-that-took-a-few-tries.txt", true));
				bw.write("Name: " + json.getString("name") + ", " + " Type: " + json.getString("type") + " Region : "
						+ json.getString("region") + "\n");
				bw.close();
			} else if (tries == 5) {
				BufferedWriter bw = new BufferedWriter(new FileWriter("./failed-games.txt", true));
				bw.write("Name: " + json.getString("name") + ", " + " Type: " + json.getString("type") + " Region : "
						+ json.getString("region") + "\n");
				bw.close();
			}
			String responseString;
			if (good) {
				responseString = EntityUtils.toString(response.getEntity());
				httpClient.close();
				return true;
			}
			httpClient.close();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	private static JSONObject getJson(Game game) {
		String hex = game.getId();
		hex = hex.replaceAll("^0+", "");
		long id = Long.parseLong(hex, 16);

		JSONObject jsonFiles = null;
		if (!noFiles) {
			NUSTitle nus = new NUSTitle(id, -1, null);
			jsonFiles = nus.makeFilesJSON();
		}
		JSONObject obj = new JSONObject();
		obj.put("titleid", game.getId());
		obj.put("name", game.getName());
		obj.put("type", game.getType());
		obj.put("region", game.getRegion());
		if (jsonFiles != null) {
			obj.put("files", jsonFiles);
		} else {
			obj.put("files", "{}");
		}

		return obj;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		overwriteTitles = false;
		overwriteIds = false;
		noFiles = false;
		updatesOnly = false;
		DLCOnly = false;
		demosOnly = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-titles")) {
				overwriteTitles = true;
			}
			if (args[i].equals("-ids")) {
				overwriteIds = true;
			}
			if (args[i].equals("-nofiles")) {
				noFiles = true;
			}
			if (args[i].equals("-updatesonly")) {
				updatesOnly = true;
			}
			if (args[i].equals("-dlconly")) {
				DLCOnly = true;
			}
			if (args[i].equals("-demosonly")) {
				demosOnly = true;
			}
		}
		String current = new File(".").getCanonicalPath();
		System.out.println("Current dir: " + current);
		InputStream f = null;

		try {
			f = new FileInputStream(new File("./config.txt"));
			BufferedReader br = new BufferedReader(new InputStreamReader(f));
			String line;
			while ((line = br.readLine()) != null) {
				updatesAddress = line;
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Settings.readConfig();
		} catch (IOException e) {
			System.err.println("Error while reading config! Needs to be:");
			System.err.println("DOWNLOAD URL BASE");
			System.err.println("COMMONKEY");
			System.err.println("updateinfos.csv");
			System.err.println("UPDATELIST VERSION URL");
			System.err.println("UPDATELIST URL PATTERN");
			return;
		}
		ArrayList<Game> gameList = new ArrayList<Game>();

		f = new FileInputStream(new File("./games.txt"));

		try (BufferedReader br = new BufferedReader(new InputStreamReader(f, "UTF-16LE"))) {
			String line;
			Game game = null;
			while ((line = br.readLine()) != null) {
				line = line.trim().replaceAll("[\\p{Zl}\\p{Zp}\\p{C}]+", "");
				int i = 0;
				for (String str : line.split("\\|")) {
					if (i == 0) {
						game = new Game();
						game.setName(str);
					} else if (i == 1) {
						game.setId(str);
					} else if (i == 2) {
						game.setType(str);
					} else {
						game.setRegion(str);
						gameList.add(game);
					}
					i++;
				}

			}
		}
		KeyboardListenerThread t = new KeyboardListenerThread();
		t.start();
		for (Game game : gameList) {
			if (t.isQuit()) {
				break;
			}
			if (updatesOnly && !game.type.equals(UPDATE))
				continue;
			if (DLCOnly && !game.type.equals(DLC))
				continue;
			if (demosOnly && !game.type.equals(DEMO))
				continue;
			JSONObject obj = getJson(game);

			if (obj != null) {
				if (sendJson(obj)) {
					System.out.println(
							"Sent: " + game.getName() + " Type: " + game.getType() + " Region: " + game.getRegion());
				} else {
					System.out.println("Failed to send: " + game.getName() + " Type: " + game.getType() + " Region: "
							+ game.getRegion());
				}
			}
		}
	}
}
