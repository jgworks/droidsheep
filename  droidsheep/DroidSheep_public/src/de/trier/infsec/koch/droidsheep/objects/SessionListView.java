package de.trier.infsec.koch.droidsheep.objects;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;
import de.trier.infsec.koch.droidsheep.activities.ListenActivity;

public class SessionListView extends ListView {

	public static final int ID_MOBILE = 1;
	public static final int ID_NORMAL = 2;
	public static final int ID_DELETE = 3;
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
