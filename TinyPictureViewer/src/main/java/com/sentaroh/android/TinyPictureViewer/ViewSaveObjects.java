package com.sentaroh.android.TinyPictureViewer;

import java.util.ArrayList;

class ViewSaveObjects {
	public int folder_view_pos_x=0;
//	public String folder_selection_filter="";
//	public int folder_selection_visibility=0;
//	public int folder_selection_msg_visibility=0;
	public int image_view_pos_x=0;
	public int picture_view_pos_x=0;
	public ArrayList<PictureListItem> thumbnail_list=null;
	public ArrayList<FolderListItem> folder_list=null;
	public String picture_view_file_name="", picture_view_info="", picture_view_zoom="";
	public int picture_view_show_info=0;

//	public String activity_title="";

	public boolean picture_view_reset_enabled=false;

	public boolean folder_view_adapter_select_mode=false;
	public boolean thumbnail_view_adapter_select_mode=false;

	public int thumbnail_grid_view_visible_status=0, thumbnail_empty_view_visible_status=0;
	public int clip_borad_btn_visiblity=0;
	public String clip_board_btn_text="";

//	public float picture_view_scale=0f;
//	public Matrix picture_view_matrix=null;

	public int picture_map_button_visibility=0;
	public int main_progress=-1;

	public boolean folder_adapter_enabled=true, thumbnail_adapter_enabled=true;

	public int folder_sort_key=0, folder_sort_order=0;
	public int thumbnail_sort_key=0, thumbnail_sort_order=0;

	public int action_bar_display_option=0;
}
