package de.trier.infsec.koch.droidsheep.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;
import de.trier.infsec.koch.droidsheep.R;

public class AuthHelper {

	static HashMap<String, AuthDefinition> authDefList = new HashMap<String, AuthDefinition>();
	static String binaryPath = null;

	public static void init(Context c) {
		try {
			readConfig(c);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void readConfig(Context c) throws XmlPullParserException, IOException {
		XmlResourceParser xpp = c.getResources().getXml(R.xml.auth);
		
		xpp.next();
		int eventType = xpp.getEventType();

		String mobileurl = null;
		String name = null;
		String url = null;
		String domain = null;
		ArrayList<String> cookieNames = new ArrayList<String>();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("auth")) {				
				name = null;
				url = null;
				mobileurl = null;
				domain = null;
				cookieNames = new ArrayList<String>();
			}
			while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("auth")) && eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					if (xpp.getName().equals("name")) {
						xpp.next();
						name = xpp.getText();
					} else if (xpp.getName().equals("url")) {
						xpp.next();
						url = xpp.getText();
					} else if (xpp.getName().equals("domain")) {
						xpp.next();
						domain = xpp.getText();
					} else if (xpp.getName().equals("cookiename")) {
						xpp.next();
						cookieNames.add(xpp.getText());
					} else if (xpp.getName().equals("mobileurl")) {
						xpp.next();
						mobileurl = xpp.getText();
					}
				}
				eventType = xpp.next();
			}
			if (name!= null && url != null && domain != null && cookieNames != null && !cookieNames.isEmpty()) {
				authDefList.put(name, new AuthDefinition(cookieNames, url, mobileurl, domain, name));
			}
			eventType = xpp.next();
		}
	}

	public static Auth match(String line) {
		for (String key : authDefList.keySet()) {
			AuthDefinition ad = authDefList.get(key);
			Auth a = ad.getAuthFromCookieString(line);
			if (a != null) {
				Log.d("FS", "MATCH:" + a.getName());
				return a;
			}
		}
		return null;
	}
	
}