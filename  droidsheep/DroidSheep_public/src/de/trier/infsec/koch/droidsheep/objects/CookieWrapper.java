package de.trier.infsec.koch.droidsheep.objects;


public class CookieWrapper {
	
	org.apache.http.cookie.Cookie cookie = null;
	String url = null;
	
	public CookieWrapper(org.apache.http.cookie.Cookie cookie, String url) {
		this.cookie = cookie;
		this.url = url;
	}
	
	public org.apache.http.cookie.Cookie getCookie() {
		return cookie;
	}

	public String getUrl() {
		return url;
	}
}
