package android.view;


import io.leocad.webcachedimageview.BitmapWorkerTask;
import io.leocad.webcachedimageview.CacheManager;
import io.leocad.webcachedimageview.R;

import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;


public class WebCachedImageView extends ImageView {
	
	private static final int[] ATTRS_DIMEN_ANDROID = {android.R.attr.layout_width, android.R.attr.layout_height};

	public CacheManager mCacheMgr;
	private WeakReference<BitmapWorkerTask> mBitmapWorkerRef;
	
	private int mWidth;
	private int mHeight;
	
	private Animation mAppearAnimation;

	public WebCachedImageView(Context context) {
		super(context);
		init(context, CacheManager.MODE_MEMORY | CacheManager.MODE_DISK, 12.5f, null);
	}

	public WebCachedImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.WebCachedImageView);
		int mode = styledAttrs.getInt(R.styleable.WebCachedImageView_mode, CacheManager.MODE_MEMORY | CacheManager.MODE_DISK);
		float memoryFractionToUse = styledAttrs.getFloat(R.styleable.WebCachedImageView_memoryPercentToUse, 12.5f);
		styledAttrs.recycle();
		
		init(context, mode, memoryFractionToUse, attrs);
	}

	private void init(Context context, int mode, float memoryPercentToUse, AttributeSet attrs) {

		if (!isInEditMode()) {
			
			// Get the image dimensions
			if (attrs != null) {
				
				TypedArray styledAttrs = context.obtainStyledAttributes(attrs, ATTRS_DIMEN_ANDROID);
				
				try {
					mWidth = styledAttrs.getDimensionPixelSize(0, ViewGroup.LayoutParams.MATCH_PARENT);
				} catch (UnsupportedOperationException e) {
					// This dimension is either "match_parent" or "wrap_content"
					mWidth = -1;
				}
				
				try {
					mHeight = styledAttrs.getDimensionPixelSize(1, ViewGroup.LayoutParams.MATCH_PARENT);
				} catch (UnsupportedOperationException e) {
					mHeight = -1;
				}
				
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
			
			mAppearAnimation = new AlphaAnimation(0.f, 1.f);
			mAppearAnimation.setDuration(300);
			mAppearAnimation.setRepeatCount(0);
			mAppearAnimation.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {
					setAlphaCompat(1.f);
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
				}
			});
			
		} // TODO else show placeholder
	}

	public void setImageUrl(String url) {

		setAlphaCompat(0.f); // TODO Add placeholder
		
		if (url != null && cancelPotentialWork(url)) {
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
	
	@SuppressLint("NewApi")
	private void setAlphaCompat(float alpha) {
		
		if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 16) {
			setAlpha(alpha);
		} else if (Build.VERSION.SDK_INT >= 16) {
			setImageAlpha((int) (alpha * 255));
		} else {
			//Compatibility version: no alpha
			if (alpha == 0.f) {
				setVisibility(View.INVISIBLE);
			} else {
				setVisibility(View.VISIBLE);
			}
		}
	}
	
	private boolean cancelPotentialWork(String url) {

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
	
	@Override
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);
		startAnimation(mAppearAnimation);
	}
}