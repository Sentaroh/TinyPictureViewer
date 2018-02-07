package com.sentaroh.android.TinyPictureViewer;

import static com.sentaroh.android.TinyPictureViewer.Constants.*;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import com.sentaroh.android.Utilities.Widget.ExtendImageViewTouch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;


@SuppressWarnings("unused")
public class CustomImageView extends ExtendImageViewTouch{

	private Bitmap mBitmap=null;
	
	public interface OnZoomChangedListener {
		/**
		 * Callback invoked when the Zoom changed 
		 * @param scale
		 */
		void onZoomChanged(float scale);
	};

	public CustomImageView(Context context, AttributeSet attrs,
	        int defStyle) {
	    super(context, attrs, defStyle); 
//	    LOG_ENABLED=true;
	}

	public CustomImageView(Context context, AttributeSet attrs) {
	    super(context, attrs);
//	    LOG_ENABLED=true;
	}

	public CustomImageView(Context context) {
	    super(context);
//	    LOG_ENABLED=true;
	}

	@Override
	public void setImageBitmap( final Bitmap bitmap) {
		super.setImageBitmap(bitmap);
		if (mBitmap!=null && !mBitmap.equals(bitmap)) {
			mBitmap.recycle(); 
		}
		mBitmap=bitmap;
	}

	@Override
	public void setImageBitmap( final Bitmap bitmap, Matrix matrix, float min_zoom, float max_zoom ) {
		super.setImageBitmap(bitmap, matrix, min_zoom, max_zoom);
		if (mBitmap!=null && !mBitmap.equals(bitmap)) mBitmap.recycle();
		mBitmap=bitmap;
	}
	
	public Bitmap getBitMap() {
		return mBitmap;
	}
	
	private float mRotation=0f;
	
	@Override
	public float getRotation() {
		return mRotation;
	}
	
//	@Override
//	public void setRotation(float rotate) {
//		mRotation=rotate;
//	}

	public void postRotation(float rotation) {
		PointF center = getCenter();
//		mSuppMatrix.postScale( 2.0f, 2.0f, center.x, center.y );
		mSuppMatrix.postRotate( 90.0f, center.x, center.y );
		setImageMatrix( getImageViewMatrix() );
	}
	
	@Override
	protected float computeMaxZoom() {
		return PICTURE_VIEW_MAX_SCALE;
	}

	@Override
	protected float computeMinZoom() {
		return PICTURE_VIEW_MIN_SCALE;
	}

	
	private OnZoomChangedListener mOnZoomChangedListener=null;
	public void setOnZoomChangedListener(OnZoomChangedListener listener) {
		mOnZoomChangedListener=listener;
	}
	
	@Override
	protected void _setImageDrawable( final Drawable drawable, final Matrix initial_matrix, float min_zoom, float max_zoom ) {
		super._setImageDrawable( drawable, initial_matrix, min_zoom, max_zoom );
//		mScaleFactor = getMaxScale() / 3;
		mScaleFactor=4.0f;
	}
	
	@Override
	protected float onDoubleTapPost( float scale, float maxZoom ) {
		if ( (scale*2) <= PICTURE_VIEW_MAX_SCALE ) return scale*2;
		else if (scale>=PICTURE_VIEW_MAX_SCALE ) return 1f;
		else return PICTURE_VIEW_MAX_SCALE;
	};

	private boolean mZoomEnabled=false;
	public void setZoomEnabled(boolean enabled) {
		mZoomEnabled=enabled;
	};
	public boolean isZoomEnabled() {return mZoomEnabled;}
	
	@Override
	protected void zoomTo(float scale ) {
		if (isZoomEnabled()) {
			super.zoomTo(scale);
//			Log.v("","setted scale="+scale);
		} else {
//			Log.v("","ignored scale="+scale);
		}
	};
	
	@Override
	protected void onZoom( float scale ) {
		if (mOnZoomChangedListener!=null) mOnZoomChangedListener.onZoomChanged(scale);
	}
//	
//	@Override
//	protected void onZoomAnimationCompleted( float scale ) {
//		Log.v("","zoomAnimation="+scale);
//	}
	
}
