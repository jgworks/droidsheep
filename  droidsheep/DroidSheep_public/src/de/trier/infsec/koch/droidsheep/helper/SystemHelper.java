/*    	SystemHelper.java executed superuser commands
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

import android.content.Context;
import android.util.Log;

public class SystemHelper {

	public static void execSUCommand(String command) {
		try {
			Process process = new ProcessBuilder().command("su").start();
			process.getOutputStream().write((command + "\n").getBytes("ASCII"));
			process.getOutputStream().flush();
		} catch (Exception e) {
			Log.e("ANDROSHEEP", "Error executing: " + command, e);
		}
	}
	
	public static String getDroidSheepBinaryPath(Context c) {
		return c.getFilesDir().getAbsolutePath() + File.separator + "droidsheep";
	}
	
	public static String getARPSpoofBinaryPath(Context c) {
		return c.getFilesDir().getAbsolutePath() + File.separator + "arpspoof";
	}

}
