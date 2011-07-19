package de.trier.infsec.koch.droidsheep.activities;

import java.util.Hashtable;

import android.app.Activity;
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
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import de.trier.infsec.koch.droidsheep.auth.Auth;
import de.trier.infsec.koch.droidsheep.auth.AuthHelper;
import de.trier.infsec.koch.droidsheep.helper.SetupHelper;
import de.trier.infsec.koch.droidsheep.objects.SessionListView;
import de.trier.infsec.koch.droidsheep.objects.WifiChangeChecker;
import de.trier.infsec.koch.droidsheep.thread.ListenThread;

public class ListenActivity extends Activity implements OnClickListener, OnItemClickListener, OnCreateContextMenuListener {

	public static Hashtable<String, Auth> authList = new Hashtable<String, Auth>();
	public static boolean disclaimerAccepted = false;

	private SessionListView 	sessionListView;
	private TextView 			tstatus;
	private TextView 			tnetworkConnected;
	private TextView 			tnetworkName;
	private TextView 			tnetworkEncryption;
	private ProgressBar 		pbrunning;
	
	private int 				sessionListViewSelected;
	private String 				sessionListViewSelectedKey;
	
	private boolean 			networkConnected = false;
	private boolean				networkEncryptionWPA = false;
	private String	 			networkName = "";

	public static final String 	BUNDLE_KEY_TYPE 			= "TYPE";
	public static final String 	BUNDLE_TYPE_WIFICHANGE 		= "WIFICHANGE";
	public static final String	BUNDLE_TYPE_NEWAUTH 		= "NEWAUTH";
	public static final String 	BUNDLE_KEY_ID 				= "ID";
	public static final String 	BUNDLE_KEY_MOBILE 			= "MOBILE";
	public static final String 	BUNDLE_KEY_AUTH				= "AUTH";


	public static final int		MENU_WIFILIST_ID  			= 0;
	public static final int		MENU_CLEAR_SESSIONLIST_ID  	= 1;
	public static final int		MENU_EXIT_ID				= 2;
	
	private static final int 	NOTIFICATION_ID 			= 4711;

	
	private int lastNotification = 0;
	private NotificationManager mNotificationManager = null;
	
	private Handler handler = new Handler() {
		@Override
		public synchronized void handleMessage(Message msg) {
			String type = msg.getData().getString(BUNDLE_KEY_TYPE);
			if (type != null && type.equals(BUNDLE_TYPE_WIFICHANGE)) {
				if (!ListenThread.running) return;
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
		SetupHelper.checkPrerequisites(this.getApplicationContext());
		AuthHelper.init(this.getApplicationContext());
		WifiChangeChecker wi = new WifiChangeChecker(handler);
		this.getApplicationContext().registerReceiver(wi, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
	}

	@Override
	protected void onStart() {
		setContentView(R.layout.listen);
		Button button = (Button) findViewById(R.id.bstartstop);
		button.setOnClickListener(this);
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
		super.onStart();
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		showDisclaimer();
	}
	
	
	private void showDisclaimer() {
		if (disclaimerAccepted) return;
		
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
		refresh();
		mNotificationManager.cancelAll();
	}

	@Override
	protected void onDestroy() {
		cleanup();
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
			authList.remove(authList.keySet().toArray(new String[]{})[sessionListViewSelected]);
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
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(getString(R.string.menu_choose_page_title));
		if (authList.get(sessionListViewSelectedKey).getMobileUrl() != null) {
			menu.add(ContextMenu.NONE, SessionListView.ID_MOBILE, ContextMenu.NONE, getString(R.string.menu_open_mobile));
			menu.add(ContextMenu.NONE, SessionListView.ID_NORMAL, ContextMenu.NONE, getString(R.string.menu_open_normal));
			menu.add(ContextMenu.NONE, SessionListView.ID_DELETE, ContextMenu.NONE, getString(R.string.menu_remove_from_list));
		} else {
			menu.add(ContextMenu.NONE, SessionListView.ID_NORMAL, ContextMenu.NONE, getString(R.string.menu_open_normal));
			menu.add(ContextMenu.NONE, SessionListView.ID_DELETE, ContextMenu.NONE, getString(R.string.menu_remove_from_list));
		}
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_WIFILIST_ID, 0, getString(R.string.menu_wifilist));
		menu.add(0, MENU_CLEAR_SESSIONLIST_ID, 0, getString(R.string.menu_clear_sessionlist));
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
		if (!networkSniffable()) {	
			Toast.makeText(this.getApplicationContext(), "This network is WPA/WPA2 encrypted or you are disconnected. " +
					"With this version of DroidSheep you will not see any sessions within this network!", Toast.LENGTH_LONG).show();
		}
		
		Button bstartstop = (Button) findViewById(R.id.bstartstop);
	
		if (!ListenThread.running) {
			ListenThread.reset();
			Thread t = ListenThread.getInstance(this.getApplicationContext(), handler);
			t.start();
			bstartstop.setText("Stop");
		} else {
			Toast t = Toast.makeText(this.getApplicationContext(), getString(R.string.toast_process_running_text), Toast.LENGTH_SHORT);
			t.show();
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		if (ListenThread.running && ListenThread.getInstance(this.getApplicationContext(), handler).isAlive()) {
			tstatus.setText(getString(R.string.label_running));
			tstatus.setTextColor(Color.GREEN);
			pbrunning.setVisibility(ProgressBar.VISIBLE);
		} else {
			tstatus.setText(getString(R.string.label_not_running));
			tstatus.setTextColor(Color.YELLOW);
			pbrunning.setVisibility(ProgressBar.INVISIBLE);
		}
	}

	private void stop() {
		ListenThread.running = false;
		ListenThread.cleanUp();

		Button bstartstop = ((Button) findViewById(R.id.bstartstop));
		bstartstop.setText("Start");
		if (ListenThread.running && ListenThread.getInstance(this.getApplicationContext(), handler).isAlive()) {
			tstatus.setText(getString(R.string.label_running));
			tstatus.setTextColor(Color.GREEN);
			pbrunning.setVisibility(ProgressBar.VISIBLE);
		} else {
			tstatus.setText(getString(R.string.label_not_running));
			tstatus.setTextColor(Color.YELLOW);
			pbrunning.setVisibility(ProgressBar.INVISIBLE);
		}
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
		if (ListenThread.running && ListenThread.getInstance(this.getApplicationContext(), handler).isAlive()) {
			tstatus.setText(getString(R.string.label_running));
			tstatus.setTextColor(Color.GREEN);
			pbrunning.setVisibility(ProgressBar.VISIBLE);
		} else {
			tstatus.setText(getString(R.string.label_not_running));
			tstatus.setTextColor(Color.YELLOW);
			pbrunning.setVisibility(ProgressBar.INVISIBLE);
		}
		updateNetworkSettings();
		sessionListView.refresh();
	}

	private boolean networkSniffable() { 
		return (!networkEncryptionWPA && networkConnected);
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
			if (!(wc.status == Status.CURRENT)) continue;
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
	
		notification.setLatestEventInfo(context, getString(R.string.notification_title), getString(R.string.notification_text), contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

}