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
	
	public static String getBinaryPath(Context c) {
		return c.getFilesDir().getAbsolutePath() + File.separator + "droidsheep";
	}

}
