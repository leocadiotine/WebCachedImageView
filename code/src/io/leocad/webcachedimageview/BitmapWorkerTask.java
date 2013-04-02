package io.leocad.webcachedimageview;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
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
		int width = (Integer) params[2];
		int height = (Integer) params[3];
		Bitmap bitmap = null;

		// Check for cached versions
		if (
				// First, check in memory
				((bitmap = cacheMgr.getCachedOnMemory(url, width, height)) != null)
				// Then, check in disk
				|| ((bitmap = cacheMgr.getCachedOnDisk(url, width, height)) != null)

			) {
			return bitmap;
		}

		// No cached versions. Download it
		try {
			bitmap = decodeSampledBitmapFromUrl(url, width, height);

		} catch (IOException e) {
			Log.e("WebCachedImageView", "Can't download image at " + url, e);
		}

		// And cache it
		if (bitmap != null) {
			cacheMgr.cacheOnMemory(url, bitmap, width, height);
			cacheMgr.cacheOnDisk(url, bitmap, width, height);
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
	
	private static Bitmap decodeSampledBitmapFromUrl(String url, int reqWidth, int reqHeight) throws IOException {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final Options options = new Options();
	    options.inJustDecodeBounds = true;
	    
	    InputStream stream = fetchStream(url);
	    BitmapFactory.decodeStream(stream, null, options);
	    stream.close();

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    
	    stream = fetchStream(url);
	    Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
	    stream.close();
	    
	    return bitmap;
	}
	
	private static InputStream fetchStream(String urlString) throws IllegalStateException, IOException {
		
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}
	
	private static int calculateInSampleSize(Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and width
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}
}
