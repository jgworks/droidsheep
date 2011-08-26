/*
 * The arpspoof package containts Software, initially developed by Robboe
 * Clemons, It has been used, changed and published in DroidSheep according the
 * GNU GPL
 */

/*
 * ArpspoofService.java implements the background service that controls running
 * the native binary Copyright (C) 2011 Robbie Clemons <robclemons@gmail.com>
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.trier.infsec.koch.droidsheep.arpspoof;

import java.io.IOException;

import de.trier.infsec.koch.droidsheep.helper.Constants;
import de.trier.infsec.koch.droidsheep.helper.SystemHelper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public class ArpspoofService extends IntentService {

	private final String IPV4_FILEPATH = "/proc/sys/net/ipv4/ip_forward";
	private static final String TAG = "ArpspoofService";
	//	private static final int SHOW_SPOOFING = 1;
	private volatile Thread myThread;
	private static volatile WifiManager.WifiLock wifiLock;
	private static volatile PowerManager.WakeLock wakeLock;

	public ArpspoofService() {
		super("ArpspoofService");
	}

	@Override
	public void onHandleIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		String localBin = bundle.getString("localBin");
		String gateway = bundle.getString("gateway");
		String wifiInterface = bundle.getString("interface");
		final String command = localBin + " -i " + wifiInterface + " " + gateway;
		
		SystemHelper.execSUCommand("chmod 777 " + SystemHelper.getARPSpoofBinaryPath(this));
		SystemHelper.execSUCommand("echo 1 > " + IPV4_FILEPATH);

		//		Notification notification = new Notification(R.drawable.ic_stat_spoofing, "now spoofing: " + gateway, System.currentTimeMillis());
		//		Intent launchActivity = new Intent(this, SpoofingActivity.class);
		//		launchActivity.putExtras(bundle);
		//		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchActivity, 0);
		//		notification.setLatestEventInfo(this, "spoofing: " + gateway, "tap to open Arpspoof", pendingIntent);
		//		startForeground(SHOW_SPOOFING, notification);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "wifiLock");
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeLock");
		wifiLock.acquire();
		wakeLock.acquire();
		try {
			myThread = new ExecuteCommand(command, false);
			myThread.setDaemon(true);
			myThread.start();
			myThread.join();
		} catch (IOException e) {
			Log.e(TAG, "error initializing arpspoof command", e);
		} catch (InterruptedException e) {
			Log.i(TAG, "Spoofing was interrupted", e);
		} finally {
			if (myThread != null)
				myThread = null;
			if (wifiLock.isHeld()) {
				wifiLock.release();
			}
			if (wakeLock.isHeld()) {
				wakeLock.release();
			}
			stopForeground(true);
		}
	}

	@Override
	public void onDestroy() {
		//at the suggestion of the internet
		if (myThread != null) {
			Thread tmpThread = myThread;
			myThread = null;
			tmpThread.interrupt();
		}
		SystemHelper.execSUCommand(Constants.CLEANUP_COMMAND_ARPSPOOF);
		SystemHelper.execSUCommand("echo 0 > " + IPV4_FILEPATH);
	}
}