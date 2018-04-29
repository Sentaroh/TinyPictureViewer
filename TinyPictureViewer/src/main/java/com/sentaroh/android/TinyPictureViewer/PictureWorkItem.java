package com.sentaroh.android.TinyPictureViewer;

import android.widget.LinearLayout;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

class PictureWorkItem {
	public LinearLayout view=null;
	public CustomImageView image_view=null;
//	public Bitmap image_bitmap=null;
	public byte[] image_thumbnail=null;
	public double image_gps_longitude=0D, image_gps_latitude=0D;
	public String image_file_info="";
	public String image_file_name="";
	public String image_folder_name="";
	public float image_scale=1.0f;
	public String image_orientation="";
	public float image_rotation=0.0f;
	public String image_file_path="";
	public String image_file_parent_directory="";
	public PictureListItem pictureItem=null;

	public ImageViewTouch.OnImageViewTouchSingleTapListener single_tap_listener=null;
}
