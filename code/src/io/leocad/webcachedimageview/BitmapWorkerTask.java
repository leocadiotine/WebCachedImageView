package io.leocad.webcachedimageview;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.WebCachedImageView;


public class BitmapWorkerTask extends AsyncTask<Object, Void, Bitmap> {

	private WeakReference<WebCachedImageView> mImageViewReference;
	public String url;

	public BitmapWorkerTask(WebCachedImageView iv) {
		mImageViewReference = new WeakReference<WebCachedImageView>(iv);
	}

	@Override
	protected Bitmap doInBackground(Object... params) {

		url = (String) params[0];
		CacheManager cacheMgr = (CacheManager) params[1];
		Bitmap bitmap = null;

		// Check for cached versions
		if (
				// First, check in memory
				((bitmap = cacheMgr.getCachedOnMemory(url)) != null)
				// Then, check in disk
				|| ((bitmap = cacheMgr.getCachedOnDisk(url)) != null)

			) {
			return bitmap;
		}

		// No cached versions. Download it
		try {
			InputStream is = fetchStream(url);
			bitmap = BitmapFactory.decodeStream(is); // TODO Add sampling
			is.close();

		} catch (IOException e) {
			Log.e("WebCachedImageView", "Can't download image at " + url, e);
		}

		// And cache it
		if (bitmap != null) {
			cacheMgr.cacheOnMemory(url, bitmap);
			cacheMgr.cacheOnDisk(url, bitmap);
		}

		return bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		
		if (isCancelled()) {
            bitmap = null;
        }
		
		if (mImageViewReference != null && bitmap != null) {
            final WebCachedImageView imageView = mImageViewReference.get();
            
            if (imageView != null) {
            	final BitmapWorkerTask imageViewTask = imageView.getBitmapWorkerTask();
            	
            	if (imageViewTask == this) {
            		imageView.setImageBitmap(bitmap);
            	}
            }
        }
	}

	private static InputStream fetchStream(String urlString) throws IllegalStateException, IOException {

		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}
}
