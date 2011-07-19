package de.trier.infsec.koch.droidsheep.auth;

import java.util.ArrayList;

import de.trier.infsec.koch.droidsheep.objects.CookieWrapper;


public class Auth {

	ArrayList<CookieWrapper> cookieList = null;
	String url = null;
	String mobileurl = null;
	String id = null;
	
	public Auth(ArrayList<CookieWrapper> cookieList, String url, String mobileUrl) {
		this.cookieList = cookieList;
		this.mobileurl = mobileUrl;
		this.url = url;
		
		int id = 0;
		for (CookieWrapper c : cookieList) {
			id += c.getCookie().getValue().hashCode();
		}
		this.id = "DroidSheep ID: " + Integer.toString(id);
	}

	public String getId() {
		return id;
	}

	public ArrayList<CookieWrapper> getCookies() {
		return cookieList;
	}

	public String getName() {
		return url;
	}

	public String getUrl() {
		return url;
	}

	public String getMobileUrl() {
		return mobileurl;
	}
	
	
	
	

}
