package com.sentaroh.android.TinyPictureViewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomActionBarSpinnerAdapter extends ArrayAdapter<String> {
	
	private Context mContext;

	public CustomActionBarSpinnerAdapter(Context c) {
		super(c, 0);
		mContext=c;
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
        	view=LayoutInflater.from(mContext).inflate(R.layout.action_bar_spinner_item, null);
        } else {
            view=convertView;
        }
//    	final float dp_scale = getContext().getResources().getDisplayMetrics().density;
//    	final float sp_scale = getContext().getResources().getDisplayMetrics().scaledDensity;
        TextView tv=(TextView)view.findViewById(R.id.text);
//        tv.setPadding(0, 0, 50, 0);
        tv.setText(getItem(position));
    	tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        if (getCount()==1) tv.setAlpha(0.3f);
        else tv.setAlpha(1.0f);
        return view;
	}
	
//	@SuppressWarnings("deprecation")
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
        final TextView text=(TextView)super.getDropDownView(position, convertView, parent);
    	// Convert the dips to pixels  
    	final float dp_scale = getContext().getResources().getDisplayMetrics().density;
//    	final float sp_scale = getContext().getResources().getDisplayMetrics().scaledDensity;z
    	text.setMinWidth((int) (100*dp_scale));
//    	text.setTextSize(10*sp_scale);
    	
//		text.setCompoundDrawablesWithIntrinsicBounds(null,null,
//          		mContext.getResources().getDrawable(android.R.drawable.btn_radio), 
//          		null );
		text.post(new Runnable(){
			@Override
			public void run() {
				text.setSingleLine(false);
			}
		});
        return text;
	}
	
}
