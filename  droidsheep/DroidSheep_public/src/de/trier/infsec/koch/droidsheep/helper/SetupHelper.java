package de.trier.infsec.koch.droidsheep.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;
import android.util.Log;
import de.trier.infsec.koch.droidsheep.R;

public class SetupHelper {

	public static void checkPrerequisites(Context c) {

		InputStream in = c.getResources().openRawResource(R.raw.droidsheep);
		FileOutputStream out;
		try {
			File f = new File(SystemHelper.getBinaryPath(c));
			if (f.exists()) {
				f.delete();
			}
			out = c.openFileOutput("droidsheep", Context.MODE_PRIVATE);
			byte[] buffer = new byte[64];
			while (in.read(buffer) > -1) {
				out.write(buffer);
			}
			out.flush();
			out.close();
			SystemHelper.execSUCommand("chmod 777 " + c.getFilesDir().toString() + File.separator + "droidsheep");
		} catch (Exception e) {
			Log.e("FS", "", e);
		}

	}

}
