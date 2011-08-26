/*
 * DebugActivity.java is intended to help users determine if the phone is ready for DroidSheep 
 * Copyright (C) 2011 Andreas Koch <koch.trier@gmail.com>
 * 
 * This software was supported by the University of Trier
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package de.trier.infsec.koch.droidsheep.activities;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import de.trier.infsec.koch.droidsheep.arpspoof.ArpspoofService;
import de.trier.infsec.koch.droidsheep.thread.ListenService;

public class DebugActivity extends Activity {
	
	public static volatile ArrayList<String> errorList 	= new ArrayList<String>();
	public static volatile ArrayList<String> outputList = new ArrayList<String>();
	public ListAdapter errorAdapter 	= null;
	public ListAdapter outpurAdapter 	= null;

	@Override
	protected void onStart() {
		super.onStart();
		
		Intent intent1 = new Intent(this, ArpspoofService.class);
		stopService(intent1);
		Intent intent2 = new Intent(this, ListenService.class);
		stopService(intent2);
		
		errorAdapter 	= new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, errorList);
		outpurAdapter 	= new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, outputList);
	}
	
	
	
	
}
