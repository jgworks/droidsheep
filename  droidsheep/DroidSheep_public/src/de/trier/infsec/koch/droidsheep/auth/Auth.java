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

import java.io.Serializable;
import java.util.ArrayList;

import de.trier.infsec.koch.droidsheep.objects.CookieWrapper;


public class Auth implements Serializable {
	private static final long serialVersionUID = 7124255590593980755L;
	
	
	ArrayList <CookieWrapper> cookieList = null;
	String url = null;
	String mobileurl = null;
	int id = 0; // Id contains a hash sum of all cookies in the object. 
	boolean generic = true;
	boolean saved = false;
	
	public Auth(ArrayList<CookieWrapper> cookieList, String url, String mobileUrl, boolean generic) {
		this.cookieList = cookieList;
		this.mobileurl = mobileUrl;
		this.generic = generic;
		this.url = url;
		
		for (CookieWrapper c : cookieList) {
			this.id += c.getCookie().getValue().hashCode();
		}
	}
	
	// Two authentications are supposed to be identical, in case their hashes are the same.
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Auth)) return false;
		Auth a = (Auth) o;
		return (a.getId() == this.id);
	}
	
	@Override
	public int hashCode() {
		return id;
	}
	

	public int getId() {
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
	
	
	public boolean isGeneric() {
		return generic;
	}
	
	public boolean isSaved() {
		return saved;
	}
	
	public void setSaved(boolean saved) {
		this.saved = saved;
	}

}
