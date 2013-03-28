package android.view;


import io.leocad.webcachedimageview.BitmapWorkerTask;
import io.leocad.webcachedimageview.CacheManager;
import io.leocad.webcachedimageview.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;


public class WebCachedImageView extends ImageView {

	public CacheManager mCacheMgr;

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
		
		new BitmapWorkerTask(this).execute(url, mCacheMgr);
	}
}