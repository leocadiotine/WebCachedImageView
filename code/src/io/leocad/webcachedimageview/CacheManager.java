package io.leocad.webcachedimageview;


import java.io.File;
import java.io.FileInputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;


public class CacheManager {

	public static final int MODE_NO_CACHE = 0;
	public static final int MODE_MEMORY = 1;
	public static final int MODE_DISK = 2;

	private static CacheManager INSTANCE = null;

	public static CacheManager getInstance(Context context, int mode, float memoryFractionToUse) {

		if (INSTANCE == null || INSTANCE.mMode != mode || INSTANCE.mMemoryFractionToUse != memoryFractionToUse) {
			INSTANCE = new CacheManager(context, mode, memoryFractionToUse);
		}

		return INSTANCE;
	}

	private int mMode;
	private float mMemoryFractionToUse;
	private LruCache<String, Bitmap> mMemoryCache;
	private File mCacheDir;

	private CacheManager(Context context, int mode, float memoryFractionToUse) {

		if ((mode & MODE_MEMORY) == MODE_MEMORY) {

			if (memoryFractionToUse >= 1.f) {
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
	
	public Bitmap getCachedOnMemory(String url) {
		
		if ((mMode & MODE_MEMORY) == MODE_MEMORY) {
			return mMemoryCache.get(url);
		}
		
		return null;
	}
	
	public Bitmap getCachedOnDisk(String url) {

		if ((mMode & MODE_DISK) == MODE_DISK) {
			
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
		
		return null;
	}
	
	private static String getFileName(String url) {
		return String.valueOf( url.hashCode() );
	}

	public void cacheonMemory(String url, Bitmap bitmap) {

		if ((mMode & MODE_MEMORY) == MODE_MEMORY) {
			mMemoryCache.put(url, bitmap);
		}
	}
}
