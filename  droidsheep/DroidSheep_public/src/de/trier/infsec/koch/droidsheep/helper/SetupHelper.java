/*
 * SetupHelper.java does the initial copy of the binaries Copyright (C) 2011
 * Andreas Koch <koch.trier@gmail.com>
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

package de.trier.infsec.koch.droidsheep.helper;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.util.Log;
import de.trier.infsec.koch.droidsheep.R;

public class SetupHelper {

	public static void checkPrerequisites(Context c) {
		if (Constants.DEBUG) Log.d(Constants.APPLICATION_TAG, "CHECKPREREQUISITES");
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

	public static boolean checkCommands() {
		Process process = null;
		DataOutputStream os = null;
		InputStreamReader osRes = null;
		boolean hasBusybox = false;

		try {
			process = Runtime.getRuntime().exec("busybox");
			os = new DataOutputStream(process.getOutputStream());
			osRes = new InputStreamReader(process.getInputStream());
			BufferedReader reader = new BufferedReader(osRes);

			os.writeBytes("id" + "\n");
			os.flush();

			os.writeBytes("exit \n");
			os.flush();

			String line = reader.readLine();
			while (line != null) {
				if (line.contains("killall")) {
					hasBusybox = true;
				}
				line = reader.readLine();
			}
			process.waitFor();
		} catch (InterruptedException e) {
			Log.e(Constants.APPLICATION_TAG, "error checking root access", e);
		} catch (IOException e) {
			Log.e(Constants.APPLICATION_TAG, "error checking root access", e);
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (osRes != null) {
					osRes.close();
				}
			} catch (IOException e) {
				// swallow error
			} finally {
				if (process != null)
					process.destroy();
			}
		}
		return hasBusybox;
	}
	
	public static boolean checkSu() {
		Process process = null;
		DataOutputStream os = null;
		InputStreamReader osRes = null;
		boolean hasBusybox = false;

		try {
			process = Runtime.getRuntime().exec("ls /system/bin");
			os = new DataOutputStream(process.getOutputStream());
			osRes = new InputStreamReader(process.getInputStream());
			BufferedReader reader = new BufferedReader(osRes);

			os.writeBytes("exit \n");
			os.flush();

			String line = reader.readLine();
			while (line != null) {
				if (line.contains("su")) {
					hasBusybox = true;
				}
				line = reader.readLine();
			}
			process.waitFor();
			
			process = Runtime.getRuntime().exec("ls /system/xbin");
			os = new DataOutputStream(process.getOutputStream());
			osRes = new InputStreamReader(process.getInputStream());
			reader = new BufferedReader(osRes);

			os.writeBytes("exit \n");
			os.flush();

			line = reader.readLine();
			while (line != null) {
				if (line.contains("su")) {
					hasBusybox = true;
				}
				line = reader.readLine();
			}
			process.waitFor();
		} catch (InterruptedException e) {
			Log.e(Constants.APPLICATION_TAG, "error checking root access", e);
		} catch (IOException e) {
			Log.e(Constants.APPLICATION_TAG, "error checking root access", e);
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (osRes != null) {
					osRes.close();
				}
			} catch (IOException e) {
				// swallow error
			} finally {
				if (process != null)
					process.destroy();
			}
		}
		return hasBusybox;
	}

}
