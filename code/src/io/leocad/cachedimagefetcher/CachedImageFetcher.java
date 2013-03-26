package io.leocad.cachedimagefetcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;


public class CachedImageFetcher {

	public static final int MODE_NO_CACHE = 0;
	public static final int MODE_MEMORY = 1;
	public static final int MODE_DISK = 2;

	private int mMode;
	private Map<String, SoftReference<Drawable>> mMemoryMap;
	private File mCacheDir;

	public CachedImageFetcher(Context context) {
		this(context, MODE_MEMORY | MODE_DISK);
	}

	public CachedImageFetcher(Context context, int mode) {

		mMode = mode;

		if ((mode & MODE_MEMORY) == MODE_MEMORY) {
			mMemoryMap = new HashMap<String, SoftReference<Drawable>>();
		}

		if ((mode & MODE_DISK) == MODE_DISK) {
			mCacheDir = getCacheDir(context);
		}
	}

	public void asyncSetImageDrawable(final ImageView iv, final String url) {

		Drawable cachedDrawable;
		// First, check in memory
		if ((mMode & MODE_MEMORY) == MODE_MEMORY && (cachedDrawable = getMemoryCachedDrawable(url)) != null) {
			// Use the cached version and don't spawn a thread
			iv.setImageDrawable(cachedDrawable);
			return;
		}

		// Then, check in disk
		if ((mMode & MODE_DISK) == MODE_DISK && (cachedDrawable = getDiskCachedDrawable(url)) != null) {
			iv.setImageDrawable(cachedDrawable);
			return;
		}

		// No cached version
		// Clear the ImageView
		iv.setImageBitmap(null);

		// And download the new image asynchronously
		new Thread() {

			@Override
			public void run() {

				try {
					final Drawable drawable = fetch(url);

					// Display it
					iv.post(new Runnable() {

						@Override
						public void run() {
							iv.setImageDrawable(drawable);
						}
					});

					// And cache it
					if ((mMode & MODE_MEMORY) == MODE_MEMORY) {
						mMemoryMap.put(url, new SoftReference<Drawable>(drawable));
					}

					// TODO Cache in disk

				} catch (IOException e) {
					Log.e("CachedImageFetcher", "Can't download image at " + url, e);
				}

			}
		}.start();
	}

	private Drawable getMemoryCachedDrawable(String url) {

		if (mMemoryMap.containsKey(url)) {	

			// Check if the cached version was garbage collected
			Drawable cachedDrawable = mMemoryMap.get(url).get();
			if (cachedDrawable != null) {
				return cachedDrawable;

			} else {
				// Clear it from the memory map
				mMemoryMap.remove(url);
			}
		}

		return null;
	}

	private Drawable getDiskCachedDrawable(String url) {

		try {
			File cachedFile = new File(mCacheDir, getFileName(url));
			FileInputStream fis = new FileInputStream(cachedFile);
			fis.close();
			
			return null; //TODO
			

		} catch (Exception e) {
			// No cached version or error reading file system
			return null;
		}

	}

	private static String getFileName(String url) {
		return String.valueOf( url.hashCode() );
	}

	@SuppressLint("NewApi")
	private File getCacheDir(Context context) {
		String externalStorageState = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {

			if (Build.VERSION.SDK_INT > 7) {
				return context.getExternalCacheDir();

			} else {
				File externalStorageDir = Environment.getExternalStorageDirectory();
				String cacheDirPath = String.format("/Android/data/%s/cache/", externalStorageDir.getPath());

				//Create the directory (if it doesn't exist)
				File cacheDir = new File(cacheDirPath);
				if (!cacheDir.exists()) {
					cacheDir.mkdirs();
				}

				return cacheDir;
			}

		} else {
			return context.getCacheDir();
		}
	}

	private static Drawable fetch(String urlString) throws IllegalStateException, IOException {

		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		InputStream is = response.getEntity().getContent();

		return Drawable.createFromStream(is, "CachedImageFetcher");
	}
}