package de.trier.infsec.koch.droidsheep.objects;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class WifiChangeChecker extends BroadcastReceiver {

	Handler handler = null;

	public WifiChangeChecker(Handler handler) {
		this.handler = handler;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Message m = handler.obtainMessage();
		Bundle b = new Bundle();
		b.putString("TYPE", "WIFICHANGE");
		m.setData(b);
		handler.sendMessage(m);
	}

}
