/*    	AuthListAdapter.java shows the captured authentications within a list
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
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.trier.infsec.koch.droidsheep.R;
import de.trier.infsec.koch.droidsheep.activities.ListenActivity;
import de.trier.infsec.koch.droidsheep.auth.Auth;

public class AuthListAdapter extends BaseAdapter {
	
	public static final String FACEBOOK = "http://www.facebook.com";
	public static final String FLICKR	= "http://www.flickr.com";
	public static final String AMAZON	= "http://www.amazon.de";
	public static final String TWITTER 	= "http://www.twitter.com";
    
    private Context context;
 
    public AuthListAdapter(Context context) {
        this.context = context;
    }
 
    public int getCount() {
        return ListenActivity.authList.size();
    }
 
    @Override
    public Auth getItem(int position) {
        return ListenActivity.authList.get(position);
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

    	LinearLayout itemLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.listelement, parent, false);
        
    	if (position >= ListenActivity.authList.size()) return itemLayout;
    	
    	if (ListenActivity.authList == null || ListenActivity.authList.get(position) == null) {
    		return itemLayout;
    	}
    	Auth auth = ListenActivity.authList.get(position);
 
        TextView 	tv1 	= (TextView) 	itemLayout.findViewById(R.id.listtext1);
        TextView 	tv2 	= (TextView) 	itemLayout.findViewById(R.id.listtext2);
        ImageView 	imgView = (ImageView) 	itemLayout.findViewById(R.id.image);
        
        tv1.setText(auth.getName());

        if (auth.isGeneric()) {
        	tv1.setTextColor(Color.YELLOW);
        } else {
        	tv1.setTextColor(Color.GREEN);
        }

        tv2.setText("ID: " + auth.getId() + (auth.isSaved()?" << SAVED >>":""));
        
        if (auth.getName().equals(FACEBOOK)) {
        	imgView.setImageDrawable(context.getResources().getDrawable(R.drawable.droidsheep_square));
        } else if (auth.getName().equals(FLICKR)) {
        	imgView.setImageDrawable(context.getResources().getDrawable(R.drawable.droidsheep_square));
        } else if (auth.getName().equals(AMAZON)) {
        	imgView.setImageDrawable(context.getResources().getDrawable(R.drawable.droidsheep_square));
        } else if (auth.getName().equals(TWITTER)) {
        	imgView.setImageDrawable(context.getResources().getDrawable(R.drawable.droidsheep_square));
        } else {
        	imgView.setImageDrawable(context.getResources().getDrawable(R.drawable.droidsheep_square));
        }

        return itemLayout;
    }

	@Override
	public long getItemId(int position) {
		return position;
	}
 
}
