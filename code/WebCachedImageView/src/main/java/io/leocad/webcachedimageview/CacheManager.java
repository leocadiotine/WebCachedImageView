package io.leocad.webcachedimageview;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.jakewharton.DiskLruCache;
import com.jakewharton.DiskLruCache.Snapshot;


public class CacheManager {

	public static final int MODE_NO_CACHE = 0;
	public static final int MODE_MEMORY = 1;
	public static final int MODE_DISK = 2;

	private static final long DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
	private static final String DISK_CACHE_SUBDIR = "images";
	private static final int IO_BUFFER_SIZE = 8 * 1024;

	private static final CompressFormat BITMAP_COMPRESS_FORMAT = CompressFormat.JPEG;
	private static final int BITMAP_COMPRESS_QUALITY = 80;

	private static CacheManager INSTANCE = null;

	public static CacheManager getInstance(Context context, int mode, float memoryPercentToUse) {

		if (INSTANCE == null || INSTANCE.mMode != mode || INSTANCE.mMemoryPercentToUse != memoryPercentToUse) {
			INSTANCE = new CacheManager(context, mode, memoryPercentToUse);
		}

		return INSTANCE;
	}

	private int mMode;
	private float mMemoryPercentToUse;
	private LruCache<String, Bitmap> mMemoryCache;
	private DiskLruCache mDiskCache;
	private final Object mDiskCacheLock = new Object();
	private boolean mDiskCacheStarting = true;

	private CacheManager(Context context, int mode, float memoryPercentToUse) {

		mMode = mode;
		mMemoryPercentToUse = memoryPercentToUse;

		if ((mode & MODE_MEMORY) == MODE_MEMORY) {

			if (memoryPercentToUse >= 100.f) {
				throw new RuntimeException("WebCachedImageView can't use more than 99% of the device's memory! Please specify a smaller memoryFractionToUse, like 1/8.");
			}
			
			memoryPercentToUse = memoryPercentToUse / 100;

			final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
			final int cacheSize = (int) (maxMemory * memoryPercentToUse);

			mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
				@Override
				protected int sizeOf(String key, Bitmap bitmap) {
					return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
				}
			};
		}

		if ((mMode & MODE_DISK) == MODE_DISK) {
			// We should initialize the disk cache on a separate thread,
			// because it's a time-consuming operation. Because of that,
			// we should lock the file operations to wait the disk to be
			// initialized. This is the purpose of mDiskCacheLock
			File cacheDir = getCacheDir(context);
			new AsyncTask<File, Void, Void>() {
				@Override
				protected Void doInBackground(File... params) {
					synchronized (mDiskCacheLock) {
						File cacheDir = params[0];

						try {
							mDiskCache = DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE);
						} catch (IOException e) {
							Log.e("WebCachedImageView", "Couldn't init the disk cache.", e);
							mMode -= MODE_DISK;
						}

						mDiskCacheStarting = false; // Finished initialization
						mDiskCacheLock.notifyAll(); // Wake any waiting threads
					}
					return null;
				}
			}.execute(cacheDir);
		}
	}

	@SuppressLint("NewApi")
	private File getCacheDir(Context context) {

		String cacheDirPath;
		String externalStorageState = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {

			if (Build.VERSION.SDK_INT > 7) {
				cacheDirPath = context.getExternalCacheDir().getPath();

			} else {
				File externalStorageDir = Environment.getExternalStorageDirectory();
				cacheDirPath = String.format("/Android/data/%s/cache/", externalStorageDir.getPath());
			}

		} else {
			cacheDirPath = context.getCacheDir().getPath();
		}

		return new File(cacheDirPath + File.separator + DISK_CACHE_SUBDIR);
	}

	public Bitmap getCachedOnMemory(String url, int width, int height) {

		if ((mMode & MODE_MEMORY) == MODE_MEMORY) {
			return mMemoryCache.get( getFileName(url, width, height) );
		}

		return null;
	}

	public Bitmap getCachedOnDisk(String url, int width, int height) {

		if ((mMode & MODE_DISK) == MODE_DISK) {

			synchronized (mDiskCacheLock) {
				// Wait while disk cache is started from background thread
				while (mDiskCacheStarting) {
					try {
						mDiskCacheLock.wait();
					} catch (InterruptedException e) {}
				}

				if (mDiskCache != null) {
					try {
						Snapshot snapshot = mDiskCache.get( getFileName(url, width, height) );
						
						if (snapshot != null) {
							InputStream is = snapshot.getInputStream(0);
							Bitmap bitmap = BitmapFactory.decodeStream(is); // TODO Add sampling
	
							snapshot.close();
							return bitmap;
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return null;
	}

	public void cacheOnMemory(String url, Bitmap bitmap, int width, int height) {

		if ((mMode & MODE_MEMORY) == MODE_MEMORY) {
			mMemoryCache.put(getFileName(url, width, height), bitmap);
		}
	}

	public void cacheOnDisk(String url, Bitmap bitmap, int width, int height) {

		if ((mMode & MODE_DISK) == MODE_DISK) {

			synchronized (mDiskCacheLock) {
				Snapshot snapshot;
				try {
					snapshot = mDiskCache.get( getFileName(url, width, height) );
				} catch (IOException e) {
					Log.e("WebCachedImageView", "Couldn't init the disk cache.", e);
					return;
				}
				
				if (mDiskCache != null && snapshot == null) {

					DiskLruCache.Editor editor = null;
					try {
						editor = mDiskCache.edit( getFileName(url, width, height) );
						if (editor == null) {
							return;
						}

						if (writeBitmapToFile(bitmap, editor)) {               
							mDiskCache.flush();
							editor.commit();
							
						} else {
							editor.abort();
						}
						
					} catch (IOException e) {
						try {
							if ( editor != null ) {
								editor.abort();
							}
						} catch (IOException ignored) {
						}           
					}
				}
			}
		}
	}

	private boolean writeBitmapToFile( Bitmap bitmap, DiskLruCache.Editor editor ) throws IOException, FileNotFoundException {
		OutputStream out = null;
		try {
			out = new BufferedOutputStream( editor.newOutputStream(0), IO_BUFFER_SIZE );
			return bitmap.compress(BITMAP_COMPRESS_FORMAT, BITMAP_COMPRESS_QUALITY, out);
		} finally {
			if ( out != null ) {
				out.close();
			}
		}
	}
	
	private static String getFileName(String url, int width, int height) {
		
		return new StringBuffer()
		.append(url.hashCode())
		.append("_")
		.append(width)
		.append("_")
		.append(height)
		.toString();
	}
}
