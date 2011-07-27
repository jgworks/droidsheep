/*    	Auth.java is a wrapper for a requires cookie list of one Authentication
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
