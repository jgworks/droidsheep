/*    	AuthDefinition.java defnies one Authentication, read from auth.xml resource
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
		String[] lst = cookieListString.split("\\|\\|\\|");
		if (lst.length < 2) return null;
		cookieListString = lst[0];
		
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
			return new Auth(cookieList, url, mobileurl, false);
		}
		return null;
	}

}
