/*    	SessionListView.java is the UI element for displaying the cookies
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


package de.trier.infsec.koch.droidsheep.objects;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;
import de.trier.infsec.koch.droidsheep.activities.ListenActivity;

public class SessionListView extends ListView {

	public static final int ID_MOBILE = 1;
	public static final int ID_NORMAL = 2;
	public static final int ID_DELETE = 3;
	public static final int ID_BLACKLIST = 4;
	public AuthListAdapter adapter = null;
	
	public SessionListView(Context context) {
		super(context);
		adapter = new AuthListAdapter(ListenActivity.authList, context);
		this.setAdapter(adapter);
		this.setLongClickable(false);
	}
	
	public SessionListView(Context c, AttributeSet attrset) {
		super(c, attrset);
		adapter = new AuthListAdapter(ListenActivity.authList, c);
		this.setAdapter(adapter);
		this.setLongClickable(false);
	}
	
	public void refresh() {
		adapter.notifyDataSetChanged(); 
		this.setAdapter(adapter);
	}
	
}
