package de.trier.infsec.koch.droidsheep.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import de.trier.infsec.koch.droidsheep.activities.ListenActivity;
import de.trier.infsec.koch.droidsheep.auth.Auth;
import de.trier.infsec.koch.droidsheep.auth.AuthHelper;
import de.trier.infsec.koch.droidsheep.helper.SystemHelper;

public class ListenThread extends Thread {

	public static volatile boolean running = false;
	
	public static final String CLEANUP_COMMAND = "killall droidsheep\n";
	
	static Process tcpDumpProcess = null;
	static ListenThread singleton = null;
	static InputStream inputStream = null;
	static OutputStream outputStream = null;

	public static String actualOutput = "";

	private Context context = null;
	private Handler handler = null;
	
	private ListenThread (Context c, Handler handler) {
		this.context  = c;
		this.handler = handler;
	}
	
	public static ListenThread getInstance(Context c, Handler handler) {
		if (singleton == null) {
			singleton = new ListenThread(c, handler);
		}
		return singleton;
	}
	
	
	@Override
	public void run() {
		running = true;
		startProcess();
		AuthHelper.init(context);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line = null;
//		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		while (running) {
			try {
				if (reader.ready()) {
					line = reader.readLine();
					if (line == null) continue;
				} else {
					sleep(200);
					continue;
				}
				
				Log.d("FS", line);
				String[] lst = line.split(";");
				for (String cookieString : lst) {
					Log.d("FS", cookieString);
				}
				actualOutput = line;
				Auth a = AuthHelper.match(line);
				if (a != null) {
					ListenActivity.authList.put(a.getId(), a);
					Message m = handler.obtainMessage();
					Bundle bundle = new Bundle();
					bundle.putString(ListenActivity.BUNDLE_KEY_AUTH, a.getId());
					bundle.putString(ListenActivity.BUNDLE_KEY_TYPE, ListenActivity.BUNDLE_TYPE_NEWAUTH);
					m.setData(bundle);
					handler.sendMessage(m);
				}
			} catch (Exception e) {
				Log.e("FS", "FS", e);
				cleanUp();
				break;
			}
		}
		reset();
	}

	private void startProcess() {
		try {
			tcpDumpProcess = new ProcessBuilder().command("su").redirectErrorStream(true).start();
			inputStream = tcpDumpProcess.getInputStream();
			outputStream = tcpDumpProcess.getOutputStream();
			System.out.println((SystemHelper.getBinaryPath(context) + " \n").getBytes("ASCII"));
			outputStream.write((SystemHelper.getBinaryPath(context) + " \n").getBytes("ASCII"));
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void cleanUp() {
		running = false;
		try {
			Process killProcess = new ProcessBuilder().command("su").start();
			killProcess.getOutputStream().write(CLEANUP_COMMAND.getBytes("ASCII"));
			killProcess.getOutputStream().flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void reset() {
		cleanUp();
		running = false;
		try {
			Thread.sleep(500); // In case thread is running, we could be reading...
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (singleton != null) {
			singleton.interrupt();
		}
		singleton = null;
	}
	
}
