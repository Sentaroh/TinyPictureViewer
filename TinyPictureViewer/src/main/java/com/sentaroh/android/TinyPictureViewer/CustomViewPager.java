package com.sentaroh.android.TinyPictureViewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

class CustomViewPager extends ViewPager {//OverScrollEffectViewPager {

	public CustomViewPager(Context context, AttributeSet attrs) 
			throws Exception {
		super(context, attrs);
		init();
	};
	
	public CustomViewPager(Context context) 
			throws Exception {
		super(context);
		init();
	};

	@Override
	protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
		if (v instanceof WebView) {
			return false;
    	} else {
    		return super.canScroll(v, checkV, dx, x, y);
        }
	};
	
	private void init() {
//		setPageTransformer(false, new ViewPager.PageTransformer() {
//		    @Override
//		    public void transformPage(View page, float position) {
//		    	final float normalizedposition = Math.abs(Math.abs(position) - 1);
//
//		    	page.setAlpha(normalizedposition);
//		        
//		        page.setScaleX(normalizedposition / 2 + 0.5f);
//		        page.setScaleY(normalizedposition / 2 + 0.5f);
//		        
//		    	page.setRotationY(position * -30);
//		    } 
//		});
	};
	
	public void enableDefaultPageTransformer(boolean enabled) {
		if (enabled) {
			setPageTransformer(false, new ViewPager.PageTransformer() {
			    @Override
			    public void transformPage(View page, float position) {
//			    	final float normalizedposition = Math.abs(Math.abs(position) - 1);
//		
//			    	page.setAlpha(normalizedposition);
//			        
//			        page.setScaleX(normalizedposition / 2 + 0.5f);
//			        page.setScaleY(normalizedposition / 2 + 0.5f);
//			        
//			    	page.setRotationY(position * -30);
			    	float alpha = 0;
			        @SuppressWarnings("unused")
					int pageWidth = page.getWidth();
			        if (-1 < position && position < 0) {
			            // 左にスワイプしていくにつれ透明にする
			            alpha = position + 1;
			        } else if (0 <= position && position <= 1) {
			            // 右にスワイプしていくにつれ透明にする
			            alpha = 1 - position;
			        }
			        page.setAlpha(alpha);
			        // 逆方向に移動させることで位置を固定する
//			        page.setTranslationX(pageWidth * -position);
			        page.setRotationY(position * -30);
			    } 
			});
		} else {
			setPageTransformer(false, null);
		}
	};
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
	    try {
	        return super.onInterceptTouchEvent(ev);
	    } catch (IllegalArgumentException e) {
	        e.printStackTrace();
	        return false;
	    }
	}

	private int mode = 0;
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    switch (event.getAction() & MotionEvent.ACTION_MASK) {  
	    case MotionEvent.ACTION_DOWN:  
	        mode = 1;  
	        break;  
	    case MotionEvent.ACTION_UP:  
	        mode = 0;  
	        break;  
	    case MotionEvent.ACTION_POINTER_UP:  
	        mode -= 1;  
	        break;  
	    case MotionEvent.ACTION_POINTER_DOWN:  
	        mode += 1;  
	        return false;  
	    case MotionEvent.ACTION_MOVE:  
	        if (mode >= 2) {  
	          return false;
	        }  
	        break;  
	    }  
	    return super.onTouchEvent(event);  
	} 
}
