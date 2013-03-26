package io.leocad.cachedimagefetcher;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;


public class CachedImageFetcher {

	public static final int MODE_MEMORY = 1;
	public static final int MODE_DISK = 2;
	
	private Context mContext;
	private int mMode;
	private Map<String, SoftReference<Drawable>> mMemoryMap;
	
	public CachedImageFetcher(Context context) {
		this(context, MODE_MEMORY | MODE_DISK);
	}
	
	public CachedImageFetcher(Context context, int mode) {
		
		mContext = context;
		mMode = mode;
		
		if ((mode & MODE_MEMORY) == MODE_MEMORY) {
			mMemoryMap = new HashMap<String, SoftReference<Drawable>>();
		}
	}
	
	public void asyncSetImageDrawable(final ImageView iv, final String url) {
		
		// First, check in memory
		if ((mMode & MODE_MEMORY) == MODE_MEMORY && mMemoryMap.containsKey(url)) {
			
			// Check if the cached version was garbage collected
			Drawable cachedDrawable = mMemoryMap.get(url).get();
			if (cachedDrawable != null) {
				
				// Use the cached version and don't spawn a thread
				iv.setImageDrawable(cachedDrawable);
				return;
				
			} else {
				// Clear it from the memory map
				mMemoryMap.remove(url);
			}
		}
		
		// TODO check in disk
		
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
					Log.e("CachedImageFetcher", "Can't downloat image at " + url, e);
				}
			
			}
		}.start();
	}
	
	private static Drawable fetch(String urlString) throws IllegalStateException, IOException {
		
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		InputStream is = response.getEntity().getContent();
		
		return Drawable.createFromStream(is, "CachedImageFetcher");
	}
}