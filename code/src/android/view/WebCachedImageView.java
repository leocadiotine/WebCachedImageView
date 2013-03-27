package android.view;


import io.leocad.webcachedimageview.CacheManager;
import io.leocad.webcachedimageview.R;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;


public class WebCachedImageView extends ImageView {

	private CacheManager mCacheMgr;

	public WebCachedImageView(Context context) {
		super(context);
		init(context, CacheManager.MODE_MEMORY | CacheManager.MODE_DISK, 0.125f);
	}

	public WebCachedImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.WebCachedImageView);
		int mode = styledAttrs.getInt(R.styleable.WebCachedImageView_mode, CacheManager.MODE_MEMORY | CacheManager.MODE_DISK);
		float memoryFractionToUse = styledAttrs.getFraction(R.styleable.WebCachedImageView_memoryPercentToUse, 1, 1, 0.125f);
		styledAttrs.recycle();
		
		init(context, mode, memoryFractionToUse);
	}

	private void init(Context context, int mode, float memoryFractionToUse) {

		if (!isInEditMode()) {
			mCacheMgr = CacheManager.getInstance(context, mode, memoryFractionToUse);
		} // TODO else show placeholder
	}

	public void setImageUrl(String url) {

		setImageBitmap(null); // TODO Add placeholder

		new AsyncTask<String, Void, Bitmap>() {
			
			@Override
			protected Bitmap doInBackground(String... params) {
				
				String url = params[0];
				Bitmap bitmap = null;
				
				// Check for cached versions
				if (
						// First, check in memory
						((bitmap = mCacheMgr.getCachedOnMemory(url)) != null)
						// Then, check in disk
						|| ((bitmap = mCacheMgr.getCachedOnDisk(url)) != null)
						
					) {
					return bitmap;
				}
				
				// No cached versions. Download it
				try {
					InputStream is = fetchStream(url);
					bitmap = BitmapFactory.decodeStream(is); // TODO Add sampling
					is.close();
					
				} catch (IOException e) {
					Log.e("CachedImageFetcher", "Can't download image at " + url, e);
				}
				
				// And cache it
				if (bitmap != null) {
					mCacheMgr.cacheOnMemory(url, bitmap);
					mCacheMgr.cacheOnDisk(url, bitmap);
				}
				
				return bitmap;
			}
			
			@Override
			protected void onPostExecute(Bitmap bitmap) {
			
				setImageBitmap(bitmap);
			}
		}.execute(url);
	}

	private static InputStream fetchStream(String urlString) throws IllegalStateException, IOException {

		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}
}