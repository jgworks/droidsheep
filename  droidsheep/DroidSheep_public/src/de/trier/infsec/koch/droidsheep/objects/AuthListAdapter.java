package de.trier.infsec.koch.droidsheep.objects;

import java.util.Hashtable;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.trier.infsec.koch.droidsheep.R;
import de.trier.infsec.koch.droidsheep.auth.Auth;

public class AuthListAdapter extends BaseAdapter {
	
	public static final String FACEBOOK = "http://www.facebook.com";
	public static final String FLICKR	= "http://www.flickr.com";
	public static final String AMAZON	= "http://www.amazon.de";
	public static final String TWITTER 	= "http://www.twitter.com";
    
    private Hashtable<String, Auth> authList;
 
    private Context context;
 
    public AuthListAdapter(Hashtable<String, Auth> authList, Context context) {
        this.authList = authList;
        this.context = context;
    }
 
    public int getCount() {
        return authList.size();
    }
 
    @Override
    public Auth getItem(int position) {
    	String key = authList.keySet().toArray(new String[]{})[position];
        return authList.get(key);
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

    	LinearLayout itemLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.listelement, parent, false);
        
    	if (position >= authList.keySet().toArray(new String[]{}).length) return itemLayout;
    	
    	String key = authList.keySet().toArray(new String[]{})[position];
    	if (authList == null || authList.get(key) == null) {
    		return itemLayout;
    	}
    	Auth auth = authList.get(key);
 
        TextView 	tv1 	= (TextView) 	itemLayout.findViewById(R.id.listtext1);
        TextView 	tv2 	= (TextView) 	itemLayout.findViewById(R.id.listtext2);
        ImageView 	imgView = (ImageView) 	itemLayout.findViewById(R.id.image);
        
        tv1.setText(auth.getName());
        tv2.setText(auth.getId());
        
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
		return authList.keySet().toArray(new String[]{})[position].hashCode();
	}
 
}
