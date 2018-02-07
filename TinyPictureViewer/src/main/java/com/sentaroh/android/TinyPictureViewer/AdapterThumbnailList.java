package com.sentaroh.android.TinyPictureViewer;

import static com.sentaroh.android.TinyPictureViewer.Constants.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.sentaroh.android.Utilities.NotifyEvent;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;


public class AdapterThumbnailList extends BaseAdapter {

	@SuppressWarnings("unused")
	private ActivityMain mActivity;
	private LayoutInflater mLayoutInflater;
	private ArrayList<PictureListItem> mItems=new ArrayList<PictureListItem>();
	private int mViewHeight=0, mViewWidth=0;
	  
	private static class ViewHolder {
	    public ImageView image_view;
	    public TextView file_name, date_time;
	    public CheckBox checkbox;
	}

	public AdapterThumbnailList(ActivityMain a, ArrayList<PictureListItem> fl) {
		mActivity = a;
	    mLayoutInflater = LayoutInflater.from(a);
	    if (fl!=null) mItems=fl;
	    a.getResources().getDrawable(R.drawable.ic_128_tiny_picture_viewer, null);
	    mViewHeight=(int) a.getResources().getDimension(R.dimen.thumbnail_list_image_height);
		mViewWidth=(int) a.getResources().getDimension(R.dimen.thumbnail_list_image_width);
	}

	public void setPictureList(ArrayList<PictureListItem> p) {
		if (p!=null) {
			mItems=p;
			sort();
		} else {
			mItems.clear();
			notifyDataSetChanged();
		}
	};
	  
	public ArrayList<PictureListItem> getPictureList() {return mItems;}
	  
	private int mSortOrder=SORT_ORDER_ASCENDANT;
	public void setSortOrder(int order) {mSortOrder=order;}
	public int getSortOrder() {return mSortOrder;}

	private int mSortKey=SORT_KEY_THUMBNAIL_FILE_NAME;
	public void setSortKey(int key) {mSortKey=key;}
	public int getSortKey() {return mSortKey;}
	  
	public void sort() {
		sort(mItems, mSortKey, mSortOrder);
		notifyDataSetChanged();
	};
	
	static public void sort(ArrayList<PictureListItem>pl, final int key, final int order) {
		Collections.sort(pl, new Comparator<PictureListItem>(){
			@Override
			public int compare(PictureListItem lhs, PictureListItem rhs) {
				if (key==SORT_KEY_THUMBNAIL_FILE_NAME) {
					if (order==SORT_ORDER_ASCENDANT) {
						return lhs.getFileName().compareToIgnoreCase(rhs.getFileName());
					} else {
						return rhs.getFileName().compareToIgnoreCase(lhs.getFileName());
					}
				} else if (key==SORT_KEY_THUMBNAIL_PICTURE_TIME) {
					if (order==SORT_ORDER_ASCENDANT) {
						return lhs.getExifDateTime().compareToIgnoreCase(rhs.getExifDateTime());
					} else {
						return rhs.getExifDateTime().compareToIgnoreCase(lhs.getExifDateTime());
					}
				} else if (key==SORT_KEY_THUMBNAIL_FILE_LAST_MODIFIED) {
					if (order==SORT_ORDER_ASCENDANT) {
						return Long.valueOf(lhs.getFileLastModified()).compareTo(Long.valueOf(rhs.getFileLastModified()));
					} else {
						return Long.valueOf(rhs.getFileLastModified()).compareTo(Long.valueOf(lhs.getFileLastModified()));
					}
				}
				return 0;
			}
		});
	};
	
	public int getCount() {
	    return mItems.size();
	}
	
	public PictureListItem getItem(int position) {
	    return mItems.get(position);
	}
	
	public long getItemId(int position) {
	    return position;
	};
	
	private boolean select_mode=false;
	public void setSelectMode(boolean p) {
		select_mode=p;
		if (!p) {
			setAllItemsSelected(false);
		}
	};
	public boolean isSelectMode() {return select_mode;}

	public void setAllItemsSelected(boolean p) {
		for(PictureListItem pli:mItems) pli.setSelected(p);
	};
	
	public boolean isAnyItemSelected() {
		for(PictureListItem pli:mItems) if (pli.isSelected()) return true;
		return false;
	};
	
	public boolean isAllItemSelected() {
		for(PictureListItem pli:mItems) 
			if (!pli.isSelected()) {
				  return false;
			}
		return true;
	};

	@Override
	public boolean isEnabled(int pos) {
		if (adapter_enabled && getItem(pos).isEnabled() && getItem(pos).getThumbnailImageByte()!=null) return true;
		else return false;
	};

	public void setEnabled(int pos, boolean enabled) {
		getItem(pos).setEnabled(enabled);
	};
	public void setAllItemsEnabled(boolean enabled) {
		for(PictureListItem pli:mItems) pli.setEnabled(enabled);
	};

	private boolean adapter_enabled=true;
	public void setAdapterEnabled(boolean enabled) {
		adapter_enabled=enabled;
	};
	public boolean isAdapterEnabled() {
		return adapter_enabled;
	};
	
	public int getSelectedItemCount() {
		int result=0;
		for(PictureListItem pli:mItems) if (pli.isSelected()) result++;
		return result;
	};

	private NotifyEvent mCheckedChangeNotify=null;
	public void setCheckedChangeListener(NotifyEvent ntfy) {
		  mCheckedChangeNotify=ntfy;
	};

	@SuppressLint("InflateParams")
	public View getView(int position, View convertView, ViewGroup parent) {

	    ViewHolder holder;
	    if (convertView == null) {
	      convertView = mLayoutInflater.inflate(R.layout.cell_thumbnail_view, null);
	      holder = new ViewHolder();
	      holder.image_view = (ImageView)convertView.findViewById(R.id.cell_thumbnail_view_image);
	      holder.file_name = (TextView)convertView.findViewById(R.id.cell_thumbnail_view_file_name);
	      holder.date_time = (TextView)convertView.findViewById(R.id.cell_thumbnail_view_date_time);
	      holder.checkbox=(CheckBox)convertView.findViewById(R.id.cell_thumbnail_view_checkbox);
	      holder.file_name.setBackgroundColor(Color.argb(160,0,0,0));
	      holder.date_time.setBackgroundColor(Color.argb(160,0,0,0));
	      holder.checkbox.setBackgroundColor(Color.argb(200,0,0,0));
	      convertView.setTag(holder);
	    } else {
	      holder = (ViewHolder)convertView.getTag();
	    }

	    if (isSelectMode()) {
			holder.checkbox.setVisibility(CheckBox.VISIBLE);
		} else {
			holder.checkbox.setVisibility(CheckBox.GONE);
		}

	    byte[] thumb_array=getItem(position).getThumbnailImageByte();
	    if (getItem(position).getThumbnailImageByte()!=null) {
	    	thumb_array=getItem(position).getThumbnailImageByte();
	    	
			Bitmap bm=BitmapFactory.decodeByteArray(thumb_array, 0, thumb_array.length);
			int b_w=bm.getWidth();
			int b_h=bm.getHeight();
			
		    Matrix matrix=new Matrix();

		    float scale_w=(float)mViewWidth/(float)b_w;
		    float scale_h=(float)mViewHeight/(float)b_h;
		    float scale=Math.min(scale_w, scale_h);
		    matrix.postScale(scale, scale);
		    
		    int s_w=Math.round((float)b_w*scale);
		    if (s_w<mViewWidth) {
		    	int translate_val=Math.abs(mViewWidth-s_w);
		    	if (translate_val>1) {
//		    		Log.v("","name="+getItem(position).getFileName()+", translate="+(float)(translate_val/2));
		    		matrix.postTranslate((float)(translate_val/2), 0.0f);		
		    	}
		    }
		    holder.image_view.setScaleType(ScaleType.MATRIX);
		    holder.image_view.setImageMatrix(matrix);
		    holder.image_view.setImageBitmap(bm);
		    holder.image_view.setBackgroundColor(Color.BLACK);
	    } else {
	    	holder.image_view.setScaleType(ScaleType.FIT_CENTER);
		    holder.image_view.setImageBitmap(null);
		    holder.image_view.setBackgroundColor(Color.DKGRAY);
//		    holder.image_view.setImageDrawable(mDummyThumbnail);
	    }

	    holder.file_name.setText(getItem(position).getFileName());
	    holder.date_time.setText(getItem(position).getExifDateTime());

		final int p=position;
		holder.checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//				Log.v("","p="+p+", ch="+isChecked);
				if (p<getCount()) {
					getItem(p).setSelected(isChecked);
					if (mCheckedChangeNotify!=null) mCheckedChangeNotify.notifyToListener(true, new Object[]{isChecked});
				}

			}
		});
		holder.checkbox.setChecked(getItem(position).isSelected());

	    return convertView;
	  }
	}