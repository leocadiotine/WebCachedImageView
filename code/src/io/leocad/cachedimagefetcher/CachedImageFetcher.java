package io.leocad.cachedimagefetcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;


public class CachedImageFetcher {

	public static final int MODE_NO_CACHE = 0;
	public static final int MODE_MEMORY = 1;
	public static final int MODE_DISK = 2;

	private int mMode;
	private LruCache<String, Bitmap> mMemoryCache;
	private File mCacheDir;

	public CachedImageFetcher(Context context) {
		this(context, MODE_MEMORY | MODE_DISK, 1/8);
	}

	public CachedImageFetcher(Context context, int mode, float memoryFractionToUse) {

		mMode = mode;

		if ((mode & MODE_MEMORY) == MODE_MEMORY) {
			
			if (memoryFractionToUse >= 1) {
				throw new RuntimeException("CachedImageFetcher can't use more than 99% of the device's memory! Please specify a smaller memoryFractionToUse, like 1/8.");
			}
			
		    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		    final int cacheSize = (int) (maxMemory * memoryFractionToUse);

		    mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
		        @Override
		        protected int sizeOf(String key, Bitmap bitmap) {
		            return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
		        }
		    };

		}

		if ((mode & MODE_DISK) == MODE_DISK) {
			mCacheDir = getCacheDir(context);
		}
	}

	public void asyncSetImageBitmap(ImageView iv, String url) {

		iv.setImageBitmap(null);

		// And download the new image asynchronously
		final WeakReference<ImageView> imageViewReference = new WeakReference<ImageView>(iv);
		
		new AsyncTask<String, Void, Bitmap>() {
			
			@Override
			protected Bitmap doInBackground(String... params) {
				
				String url = params[0];
				Bitmap bitmap = null;
				
				// Check for cached versions
				if (
						// First, check in memory
						((mMode & MODE_MEMORY) == MODE_MEMORY && (bitmap = mMemoryCache.get(url)) != null)
						// Then, check in disk
						|| ((mMode & MODE_DISK) == MODE_DISK && (bitmap = getDiskCachedBitmap(url)) != null)
						
					) {
					return bitmap;
				}
				
				// No cached versions. Download it
				try {
					InputStream is = fetchStream(url);
					
					is.close();
					
					
				} catch (IOException e) {
					Log.e("CachedImageFetcher", "Can't download image at " + url, e);
				}
				
				// And cache it
				if (bitmap != null) {
					// And cache it
					if ((mMode & MODE_MEMORY) == MODE_MEMORY) {
						mMemoryCache.put(url, bitmap);
					}

					// TODO Cache in disk
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Bitmap bitmap) {
			
				ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
				imageViewReference.clear();
			}
		}.execute(url);
	}

	private Bitmap getDiskCachedBitmap(String url) {

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

	private static InputStream fetchStream(String urlString) throws IllegalStateException, IOException {

		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}
}