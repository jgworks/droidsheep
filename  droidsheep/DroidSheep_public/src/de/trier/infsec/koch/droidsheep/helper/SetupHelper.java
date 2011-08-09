/*    	SetupHelper.java does the initial copy of the binaries
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


package de.trier.infsec.koch.droidsheep.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;
import android.util.Log;
import de.trier.infsec.koch.droidsheep.R;

public class SetupHelper {

	public static void checkPrerequisites(Context c) {

		
		
		InputStream inDroidSheep = c.getResources().openRawResource(R.raw.droidsheep);
		InputStream inARPSpoof = c.getResources().openRawResource(R.raw.arpspoof);
		FileOutputStream out;
		try {
			File fDroidSheep = new File(SystemHelper.getDroidSheepBinaryPath(c));
			if (fDroidSheep.exists()) {
				fDroidSheep.delete();
			}
			out = c.openFileOutput("droidsheep", Context.MODE_PRIVATE);
			byte[] bufferDroidSheep = new byte[64];
			while (inDroidSheep.read(bufferDroidSheep) > -1) {
				out.write(bufferDroidSheep);
			}
			out.flush();
			out.close();
			SystemHelper.execSUCommand("chmod 777 " + c.getFilesDir().toString() + File.separator + "droidsheep");

			File fARPSpoof = new File(SystemHelper.getARPSpoofBinaryPath(c));
			if (fARPSpoof.exists()) {
				fARPSpoof.delete();
			}
			out = c.openFileOutput("arpspoof", Context.MODE_PRIVATE);
			byte[] bufferARPSpoof = new byte[64];
			while (inARPSpoof.read(bufferARPSpoof) > -1) {
				out.write(bufferARPSpoof);
			}
			out.flush();
			out.close();
			SystemHelper.execSUCommand("chmod 777 " + c.getFilesDir().toString() + File.separator + "arpspoof");
			
		} catch (Exception e) {
			Log.e("FS", "", e);
		}

	}

}
