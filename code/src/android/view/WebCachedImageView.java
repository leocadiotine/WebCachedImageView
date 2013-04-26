package android.view;


import io.leocad.webcachedimageview.BitmapWorkerTask;
import io.leocad.webcachedimageview.CacheManager;
import io.leocad.webcachedimageview.R;

import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;


public class WebCachedImageView extends ImageView {
	
	private static final int[] ATTRS_DIMEN_ANDROID = {android.R.attr.layout_width, android.R.attr.layout_height};

	public CacheManager mCacheMgr;
	private WeakReference<BitmapWorkerTask> mBitmapWorkerRef;
	
	private int mWidth;
	private int mHeight;
	
	private Drawable mPlaceHolder;

	public WebCachedImageView(Context context) {
		super(context);
		init(context, CacheManager.MODE_MEMORY | CacheManager.MODE_DISK, 12.5f, null);
	}

	public WebCachedImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.WebCachedImageView);
		int mode = styledAttrs.getInt(R.styleable.WebCachedImageView_mode, CacheManager.MODE_MEMORY | CacheManager.MODE_DISK);
		float memoryFractionToUse = styledAttrs.getFloat(R.styleable.WebCachedImageView_memoryPercentToUse, 12.5f);
		mPlaceHolder = styledAttrs.getDrawable(R.styleable.WebCachedImageView_placeHolder);
        
		styledAttrs.recycle();
		
		init(context, mode, memoryFractionToUse, attrs);
	}

	private void init(Context context, int mode, float memoryPercentToUse, AttributeSet attrs) {
		
		if (!isInEditMode()) {
			
			// Get the image dimensions
			if (attrs != null) {
				
				TypedArray styledAttrs = context.obtainStyledAttributes(attrs, ATTRS_DIMEN_ANDROID);
				mWidth = styledAttrs.getDimensionPixelSize(0, ViewGroup.LayoutParams.MATCH_PARENT);
				mHeight = styledAttrs.getDimensionPixelSize(1, ViewGroup.LayoutParams.MATCH_PARENT);
				
				styledAttrs.recycle();
				
				// Use the screen dimensions if the imageview's dimensions aren't set.
				// The image will never de bigger than the screen
				if (mWidth <= 0 || mHeight <= 0) {
					Point screenDimensions = getScreenDimensions(context);
					mWidth = screenDimensions.x;
					mHeight = screenDimensions.y;
				}
			}
			
			mCacheMgr = CacheManager.getInstance(context, mode, memoryPercentToUse);
		} 
		
		if(mPlaceHolder != null) {
			setImageDrawable(mPlaceHolder);
		}
	}

	public void setImageUrl(String url) {

		setImageDrawable(mPlaceHolder);
		
		if (cancelPotentialWork(url)) {
			final BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(this);
			mBitmapWorkerRef = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
			bitmapWorkerTask.execute(url, mCacheMgr, mWidth, mHeight);
		}
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private static Point getScreenDimensions(Context context) {
		Point size = new Point();
		
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		
		if (Build.VERSION.SDK_INT >= 13) {
			display.getSize(size);
		} else {
			size.x = display.getWidth();
			size.y = display.getHeight();
		}
		
		return size;
	}
	
	private boolean cancelPotentialWork(String url) {
		if (url == null) {
			return false;
		}
		
		BitmapWorkerTask task = getBitmapWorkerTask();
	    if (task != null) {
	    	
			if (url != task.url) {
	            // Cancel previous task
	            task.cancel(true);
	        } else {
	            // The same work is already in progress
	            return false;
	        }
	    }
	    // No task associated with the ImageView, or an existing task was cancelled
	    return true;
	}

	public BitmapWorkerTask getBitmapWorkerTask() {

		return mBitmapWorkerRef == null? null: mBitmapWorkerRef.get();
	}
}