package com.mygdx.game;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.support.v4.app.ActivityCompat;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import com.mygdx.game.MyGdxGame;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		SSIM androidSSIM = new AndroidSSIM(this.getResources());
		TakeScreenshot screenshot = new Screenshot();

		verifyStoragePermissions(this);

		initialize(new MyGdxGame(androidSSIM, screenshot), config);

	}

	// Storage Permissions
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};

	/**
	 * Checks if the app has permission to write to device storage
	 *
	 * If the app does not has permission then the user will be prompted to grant permissions
	 *
	 * @param activity
	 */
	public static void verifyStoragePermissions(Activity activity) {
		// Check if we have write permission
		int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if (permission != PackageManager.PERMISSION_GRANTED) {
			// We don't have permission so prompt the user
			ActivityCompat.requestPermissions(
					activity,
					PERMISSIONS_STORAGE,
					REQUEST_EXTERNAL_STORAGE
			);
		}
	}

	class Screenshot implements TakeScreenshot{
		public void takeScreenshot() {
			Date now = new Date();
			android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

			try {
				// image naming and path  to include sd card  appending name you choose for file
				String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

				// create bitmap screen capture
				View v1 = getWindow().getDecorView().getRootView();
				v1.setDrawingCacheEnabled(true);
				Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
				v1.setDrawingCacheEnabled(false);

				File imageFile = new File(mPath);

				FileOutputStream outputStream = new FileOutputStream(imageFile);
				int quality = 100;
				bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
				outputStream.flush();
				outputStream.close();

				openScreenshot(imageFile);
			} catch (Throwable e) {
				// Several error may come out with file handling or OOM
				e.printStackTrace();
			}
		}

		public void openScreenshot(File imageFile) {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			Uri uri = Uri.fromFile(imageFile);
			intent.setDataAndType(uri, "image/*");
			startActivity(intent);
		}
	}

}
