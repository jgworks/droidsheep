package de.trier.infsec.koch.droidsheep.auth;

import java.util.ArrayList;

import org.apache.http.impl.cookie.BasicClientCookie;

import de.trier.infsec.koch.droidsheep.objects.CookieWrapper;


public class AuthDefinition {
	
	ArrayList<String>	cookieNames;
	String		url;
	String		domain;
	String		name;
	String 		mobileurl;
	
	public AuthDefinition(ArrayList<String> cookieNames, String url, String mobileurl, String domain, String name) {
		this.cookieNames = cookieNames;
		this.url = url;
		this.domain = domain;
		this.name = name;
		this.mobileurl = mobileurl;
	}	
	
	public Auth getAuthFromCookieString(String cookieListString) {
		ArrayList<CookieWrapper> cookieList = new ArrayList<CookieWrapper>();
		String[] cookies = cookieListString.split(";");
		for (String cookieString : cookies) {
			String[] values = cookieString.split("=");
			if (cookieString.endsWith("=")) {
				values[values.length - 1] = values[values.length - 1] + "="; 
			}
			values[0] = values[0].replaceAll("Cookie:", "");
			values[0] = values[0].replaceAll(" ", "");
			if (cookieNames.contains(values[0])) {
				String val = "";
				for (int i = 1; i < values.length; i++) {
					if (i > 1) val += "=";
					val += values[i];
				}
				BasicClientCookie cookie = new BasicClientCookie(values[0], val);
				cookie.setDomain(domain);
				cookie.setPath("/");
				cookie.setVersion(0);
				
				cookieList.add(new CookieWrapper(cookie, url));
			}
		}
		if (cookieList != null && !cookieList.isEmpty() && cookieList.size() == cookieNames.size()) {
			return new Auth(cookieList, url, mobileurl);
		}
		return null;
	}

}
