package com.sentaroh.android.TinyPictureViewer;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/
import static com.sentaroh.android.TinyPictureViewer.Constants.*;
import static com.sentaroh.android.TinyPictureViewer.Log.LogConstants.*;

import java.io.File;
import java.util.ArrayList;

import com.sentaroh.android.TinyPictureViewer.PictureUtil.PictureFileCacheItem;
import com.sentaroh.android.Utilities.CommonGlobalParms;
import com.sentaroh.android.Utilities.SafFileManager;
import com.sentaroh.android.Utilities.ThemeColorList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class GlobalParameters extends CommonGlobalParms {
	public boolean debugEnabled=true;
	public boolean activityIsDestroyed=false;
	public boolean activityIsBackground=false;
//	public int applicationTheme=R.style.Theme_AppCompat_NoActionBar;
	public ThemeColorList themeColorList=null;
	public boolean themeIsLight=false;

//	public ISvcCallback callbackStub=null;

	public Context appContext=null;
	
	public boolean debuggable=false;

	public boolean externalStorageIsMounted=false;
	public boolean externalStorageAccessIsPermitted=false;
	
	final static public String STORAGE_STATUS_UNMOUNT="/unknown";
	public String internalRootDirectory=STORAGE_STATUS_UNMOUNT;
	public String externalRootDirectory=STORAGE_STATUS_UNMOUNT;
	public String appSpecificDirectory="/Android/data/com.sentaroh.android."+APPLICATION_TAG+"/files";
	public String applicationRootDirectory="/";
	public String applicationCacheDirectory="/";
	
	public String folderListFilePath="", pictureFileCacheDirectory="", pictureBitmapCacheDirectory="";

	public SafFileManager safMgr=null;

	public ArrayList<FolderListItem> showedFolderList=new ArrayList<FolderListItem>();
	public ArrayList<FolderListItem> masterFolderList=null;
	public ArrayList<PictureFileCacheItem>pictureFileCacheList=new ArrayList<PictureFileCacheItem>();

	public FolderListItem currentFolderListItem=null;
	public boolean showSinglePicture=false;

	public int uiMode=UI_MODE_ACTION_BAR;

	public int currentView=CURRENT_VIEW_FOLDER;

	public ArrayList<PictureListItem> currentPictureList=null;

	public ArrayList<PictureListItem>showedPictureList=new ArrayList<PictureListItem>();
	
	public AdapterPictureList adapterPictureView=null;
	
	public ArrayList<String> copyCutList=new ArrayList<String>();
	public boolean isCutMode=false;

	public boolean pictureZoomLocked=false;
	public boolean pictureScreenRotationLocked=false;
	
	public boolean mapApplicationAvailable=false;

	public boolean pictureShowTestDirctionNext=true;
	public boolean pictureShowTestMode=false;

	public LinearLayout folderView=null;
	public LinearLayout thumbnailView=null;
	public AdapterFolderList adapterFolderView=null;
	public GridView folderGridView=null;
	public AdapterThumbnailList adapterThumbnailView=null;
	public GridView thumbnailGridView=null;
	public TextView thumbnailEmptyView=null;
	
	public LinearLayout mainProgressView=null;
	public ProgressBar mainProgressBar=null;
	
	public RelativeLayout pictureView=null;
	public LinearLayout pictureViewTopControl=null;
	public LinearLayout pictureViewBottomControl=null;
	public LinearLayout customViewPagerView=null;
	public CustomViewPager customViewPager=null;
	public TextView pictureViewFileInfo=null;
	public TextView pictureViewFileName=null;
	public TextView pictureViewZoomRatio=null;
	public ImageButton picturePrevBtn=null;
	public ImageButton pictureNextBtn=null;
	public ImageButton pictureZoomOutBtn=null;
	public ImageButton pictureZoomInBtn=null;
	public ImageButton pictureLockScreenRotationBtn=null;
	public ImageButton pictureShowMapBtn=null;
	public ImageButton pictureRotatePictureRightBtn=null;
	public ImageButton pictureRotatePictureLeftBtn=null;
	public ImageButton pictureLockZoomBtn=null;
	public ImageButton pictureResetBtn=null;
	public ImageButton pictureShareBtn=null;
	public ImageButton pictureWallpaperBtn=null;
	public ImageButton pictureDeleteBtn=null;

	public ImageButton contextButtonFolderExclude=null, 
			contextButtonFolderMediaFileScan=null,
			contextButtonFolderRename=null, contextButtonFolderAdd=null, 
			contextButtonFolderDelete=null, contextButtonFolderSelectAll=null, 
			contextButtonFolderUnselectAll=null;
	public LinearLayout contextButtonFolderExcludeView=null, 
			contextButtonFolderMediaFileScanView=null,
			contextButtonFolderRenameView=null, contextButtonFolderAddView=null, 
			contextButtonFolderDeleteView=null, contextButtonFolderSelectAllView=null, 
			contextButtonFolderUnselectAllView=null;

	public ImageButton contextButtonThumbnailShare=null, 
			contextButtonThumbnailRename=null, 
			contextButtonThumbnailPaste=null, contextButtonThumbnailCopy=null, contextButtonThumbnailCut=null,
			contextButtonThumbnailDelete=null, contextButtonThumbnailSelectAll=null, 
			contextButtonThumbnailUnselectAll=null;
	public LinearLayout contextButtonThumbnailShareView=null, 
			contextButtonThumbnailRenameView=null,
			contextButtonThumbnailPasteView=null, contextButtonThumbnailCopyView=null, contextButtonThumbnailCutView=null,
			contextButtonThumbnailDeleteView=null, contextButtonThumbnailSelectAllView=null, 
			contextButtonThumbnailUnselectAllView=null;

	public Button contextClipBoardBtn=null;
	
	public Spinner spinnerPictureSelector=null;
	public CustomActionBarSpinnerAdapter adapterPictureSelectorSpinner=null;
	public int selectPictureDateSpinnerPosition=0;
	
	public Spinner spinnerFolderSelector=null;
	public CustomActionBarSpinnerAdapter adapterFolderSelectorSpinner=null;
	public int selectFolderSpinnerPosition=0;
	
	public OnItemSelectedListener folderSelectorListener=null;
	public OnItemSelectedListener pictureSelectorListener=null;

	public Toolbar mainToolBar=null;
	
	
//	Settings parameter	    	
	public boolean settingExitClean=false;
	public int     settingDebugLevel=0;
	public int     settingLogMaxFileCount=10;		
	public String  settingLogMsgDir="", settingLogMsgFilename=LOG_FILE_NAME;
	public boolean settingLogOption=true;
	public boolean settingPutLogcatOption=false;
	
	public boolean settingMaxBrightWhenImageShowed=true;
//	public boolean settingPictureScreenWithoutNavigateButton=false;
	
	public String settingAutoFileChangeDetection=AUTO_FILE_CHANGE_DETECTION_ALWAYS;
	
	public boolean settingCameraFolderAlwayTop=true;

	public int settingFolderSelectionCharacterCount=4;

	public int 		settingPictureDisplayDefualtUiMode=UI_MODE_FULL_SCREEN_WITH_NAVI;
	public boolean 	settingPictureDisplayOptionRestoreWhenStartup=false;
	public int 		settingPictureDisplayLastUiMode=UI_MODE_FULL_SCREEN_WITH_NAVI;
//	public boolean settingPictureDisplayOptionShowNavigationButton=true;
//	public boolean settingPictureDisplayOptionShowPictureInfo=true;
	
	public boolean settingScanHiddenFile=false;
	public ArrayList<ScanFolderItem> settingScanDirectoryList=null;
	public String[] settingScanFileType=new String[]{"jpg","png"};
	
	public int folderListSortKey=0;
	public int folderListSortOrder=0;

	public int thumbnailListSortKey=SORT_KEY_THUMBNAIL_PICTURE_TIME;
	public int thumbnailListSortOrder=SORT_ORDER_DESCENDANT;

	public int settingImageQuality=30;
	public int settingImagesizeMaxWidth=512;

	public Handler uiHandler=null;


	public GlobalParameters() {
//		Log.v("","constructed");
	};
	
//	@SuppressLint("Wakelock")
//	@Override
//	public void onCreate() {
////		Log.v("","onCreate dir="+getFilesDir().toString());
//		appContext=this.getApplicationContext();
//		uiHandler=new Handler();
//		debuggable=isDebuggable();
//
//		internalRootDirectory=Environment.getExternalStorageDirectory().toString();
//
//		applicationRootDirectory=getFilesDir().toString();
//		applicationCacheDirectory=getCacheDir().toString();
//
//	    folderListFilePath=internalRootDirectory+appSpecificDirectory+"/folder_list_cache";
//	    pictureFileCacheDirectory=internalRootDirectory+appSpecificDirectory+"/pic_cache/";
//	    pictureBitmapCacheDirectory=internalRootDirectory+appSpecificDirectory+"/bitmap_cache/";
//	    File lf=new File(pictureFileCacheDirectory);
//	    if (!lf.exists()) lf.mkdirs();
//	    lf=new File(pictureBitmapCacheDirectory);
//	    if (!lf.exists()) lf.mkdirs();
//
//		initStorageStatus(this);
//
//		initSettingsParms(this);
//		loadSettingsParms(this);
//		setLogParms(this);
//		loadFolderSortParm(this);
//	};
//
    public void initGlobalParameter(Context c) {
//		Log.v("","onCreate dir="+getFilesDir().toString());
        appContext=c;
        uiHandler=new Handler();
        debuggable=isDebuggable();

        internalRootDirectory=Environment.getExternalStorageDirectory().toString();

        applicationRootDirectory=c.getFilesDir().toString();
        applicationCacheDirectory=c.getCacheDir().toString();

        folderListFilePath=internalRootDirectory+appSpecificDirectory+"/folder_list_cache";
        pictureFileCacheDirectory=internalRootDirectory+appSpecificDirectory+"/pic_cache/";
        pictureBitmapCacheDirectory=internalRootDirectory+appSpecificDirectory+"/bitmap_cache/";
        File lf=new File(pictureFileCacheDirectory);
        if (!lf.exists()) lf.mkdirs();
        lf=new File(pictureBitmapCacheDirectory);
        if (!lf.exists()) lf.mkdirs();

        initStorageStatus(c);

        initSettingsParms(c);
        loadSettingsParms(c);
        setLogParms(this);
        loadFolderSortParm(c);
    };

    public void clearParms() {
		showedFolderList=new ArrayList<FolderListItem>();
		masterFolderList=null;
		pictureFileCacheList=new ArrayList<PictureFileCacheItem>();

		currentFolderListItem=null;
		showSinglePicture=false;

		uiMode=UI_MODE_ACTION_BAR;
		currentView=CURRENT_VIEW_FOLDER;
		currentPictureList=null;
		showedPictureList=new ArrayList<PictureListItem>();
		adapterPictureView=null;
		copyCutList=new ArrayList<String>();
		isCutMode=false;

	};
	
	@SuppressLint("NewApi")
	public void initStorageStatus(Context c) {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {  
    		externalStorageIsMounted=false;
    	} else  {  
    		externalStorageIsMounted=true;
    	}
		
		if (Build.VERSION.SDK_INT>=23) {
			externalStorageAccessIsPermitted=
					(c.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED);
		} else {
			externalStorageAccessIsPermitted=true;
		}
		
		refreshMediaDir(c);
	};
	
	public void refreshMediaDir(Context c) {
		File[] fl=ContextCompat.getExternalFilesDirs(c, null);
		if (fl!=null) {
			for(File item:fl) {
				if (item!=null && !item.getAbsolutePath().startsWith(internalRootDirectory)) {
					externalRootDirectory=item.getAbsolutePath().substring(0,item.getAbsolutePath().indexOf("/Android"));
					break;
				}
			}
		}
		if (safMgr==null) {
			safMgr=new SafFileManager(c, settingDebugLevel>0);
		} else {
			safMgr.loadSafFileList();
		}
	};
	
	public void setLogParms(GlobalParameters gp) {
		setDebugLevel(gp.settingDebugLevel);
		setLogcatEnabled(gp.settingPutLogcatOption);
		setLogLimitSize(2*1024*1024);
		setLogMaxFileCount(gp.settingLogMaxFileCount);
		setLogEnabled(gp.settingLogOption);
		setLogDirName(gp.settingLogMsgDir);
		setLogFileName(gp.settingLogMsgFilename);
		setApplicationTag(APPLICATION_TAG);
		setLogIntent(BROADCAST_LOG_RESET,
				BROADCAST_LOG_DELETE,
				BROADCAST_LOG_FLUSH,
				BROADCAST_LOG_ROTATE,
				BROADCAST_LOG_SEND,
				BROADCAST_LOG_CLOSE);

	}
	
//	private int mTextColorForeground=0;
//	private int mTextColorBackground=0;
//	public void initTextColor(Context c) {
//    	TypedValue outValue = new TypedValue();
//    	c.getTheme().resolveAttribute(android.R.attr.textColorPrimary, outValue, true);
//    	mTextColorForeground=c.getResources().getColor(outValue.resourceId);
//    	c.getTheme().resolveAttribute(android.R.attr.colorBackground, outValue, true);
//    	mTextColorBackground=c.getResources().getColor(outValue.resourceId);
//    	Log.v("","f="+String.format("0x%08x", mTextColorForeground));
//    	Log.v("","b="+String.format("0x%08x", mTextColorBackground));
//	};

	public void initSettingsParms(Context c) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		
		if (prefs.getString(c.getString(R.string.settings_log_dir), "-1").equals("-1")) {
			Editor pe=prefs.edit();
			
			pe.putBoolean(c.getString(R.string.settings_max_screen_brightness_when_image_showed), true);
			pe.putString(c.getString(R.string.settings_log_dir), internalRootDirectory+"/"+APPLICATION_TAG+"/");
			
			pe.commit();
		}
		
		if (!prefs.contains(c.getString(R.string.settings_picture_display_default_ui_mode))) {
			prefs.edit().putString(c.getString(R.string.settings_picture_display_default_ui_mode), String.valueOf(UI_MODE_FULL_SCREEN_WITH_NAVI)).commit();
		}
		
		if (!prefs.contains(c.getString(R.string.settings_folder_filter_character_count))) {
			prefs.edit().putString(c.getString(R.string.settings_folder_filter_character_count), "4").commit();
		}

		if (!prefs.contains(c.getString(R.string.settings_file_changed_auto_detect))) {
			prefs.edit().putString(c.getString(R.string.settings_file_changed_auto_detect), 
					AUTO_FILE_CHANGE_DETECTION_MEDIA_STORE_CHANGED).commit();
		}

		if (!prefs.contains(c.getString(R.string.settings_camera_folder_always_top))) {
			prefs.edit().putBoolean(c.getString(R.string.settings_camera_folder_always_top), true).commit();
		}
	};

	static private final String DISPLAY_OPTION_LAST_UIMODE="settings_picture_display_last_ui_mode";
	public void saveSettingPictureDisplayUiMode(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		prefs.edit().putInt(DISPLAY_OPTION_LAST_UIMODE, uiMode).commit();
		settingPictureDisplayLastUiMode=uiMode;
//		Log.v("","gp nav="+enabled);
	};

	public void saveSettingOptionLogEnabled(Context c, boolean enabled) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		prefs.edit().putBoolean(c.getString(R.string.settings_log_option), enabled).commit(); 
	};

	public void saveSettingOptionHiddenFile(Context c, boolean enable_hidden_file) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		prefs.edit().putBoolean(c.getString(R.string.settings_process_hidden_files), enable_hidden_file).commit(); 
	};

	public void loadSettingsParms(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

		settingDebugLevel=Integer.parseInt(prefs.getString(c.getString(R.string.settings_log_level), "0")); 
		settingLogMaxFileCount=Integer.valueOf(prefs.getString(c.getString(R.string.settings_log_file_max_count), "10"));
		settingLogMsgDir=prefs.getString(c.getString(R.string.settings_log_dir),internalRootDirectory+"/"+APPLICATION_TAG+"/");
		settingLogOption=prefs.getBoolean(c.getString(R.string.settings_log_option), false); 
		settingPutLogcatOption=prefs.getBoolean(c.getString(R.string.settings_put_logcat_option), false);

		settingMaxBrightWhenImageShowed=prefs.getBoolean(c.getString(R.string.settings_max_screen_brightness_when_image_showed),true);
		settingPictureDisplayDefualtUiMode=
				Integer.parseInt(prefs.getString(c.getString(R.string.settings_picture_display_default_ui_mode), String.valueOf(UI_MODE_FULL_SCREEN_WITH_NAVI)));
		settingPictureDisplayLastUiMode=prefs.getInt(DISPLAY_OPTION_LAST_UIMODE, UI_MODE_FULL_SCREEN_WITH_NAVI);
		
		settingScanHiddenFile=prefs.getBoolean(c.getString(R.string.settings_process_hidden_files),false);
		
		settingFolderSelectionCharacterCount=
				Integer.parseInt(prefs.getString(c.getString(R.string.settings_folder_filter_character_count), "4"));
		
		settingAutoFileChangeDetection=prefs.getString(c.getString(R.string.settings_file_changed_auto_detect), 
																		AUTO_FILE_CHANGE_DETECTION_MEDIA_STORE_CHANGED);
		
		settingCameraFolderAlwayTop=prefs.getBoolean(c.getString(R.string.settings_camera_folder_always_top), false);

		settingPictureDisplayOptionRestoreWhenStartup=
				prefs.getBoolean(c.getString(R.string.settings_picture_display_option_restore_when_startup), false);
//		Log.v("","gp init pi="+settingPictureDisplayOptionShowPictureInfo);

		loadScanFolderList(c);
	};

	final static private String FOLDER_LIST_SORT_KEY="settings_folder_list_sort_key";
	final static private String FOLDER_LIST_SORT_ORDER="settings_folder_list_sort_order";
	public void saveFolderSortParm(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		prefs.edit().putInt(FOLDER_LIST_SORT_KEY, folderListSortKey).commit(); 
		prefs.edit().putInt(FOLDER_LIST_SORT_ORDER, folderListSortOrder).commit();
	};
	public void loadFolderSortParm(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		folderListSortKey=prefs.getInt(FOLDER_LIST_SORT_KEY, 0); 
		folderListSortOrder=prefs.getInt(FOLDER_LIST_SORT_ORDER, 0);
	};

	public void saveScanFolderList(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String scan_list_value="", sep="";
		for(ScanFolderItem sfi:settingScanDirectoryList) {
			String sub=sfi.process_sub_directories?"1":"0";
			String inc=sfi.include?"1":"0";
			scan_list_value+=sep+(sfi.folder_path+"\t"+sub+"\t"+inc);
			sep="\n";
		}
		prefs.edit().putString(SCAN_FOLDER_LIST_KEY, scan_list_value).commit();
//		Log.v("","saved sv="+scan_list_value);
	};

	final static private String OLD_INCLUDE_DIR_KEY="settings_scan_directory_list";
	final static private String OLD_EXCLUDE_DIR_KEY="settings_scan_exclude_directory_list";
	final static public String SCAN_FOLDER_LIST_KEY="settings_scan_list";
	
	public void loadScanFolderList(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String[] settingSelectPictureDirectory=null;
		String[] settingExcludePictureDirectory=null;
		ArrayList<ScanFolderItem>scan_list=new ArrayList<ScanFolderItem>();

		if (prefs.contains(OLD_INCLUDE_DIR_KEY)) {
//			Log.v("","old");
			String sv=prefs.getString(OLD_INCLUDE_DIR_KEY, "");
			String[] result=null;
			if (!sv.equals("")) {
				result=sv.split(",");
			}
			settingSelectPictureDirectory=result;
			
			String ev=prefs.getString(OLD_EXCLUDE_DIR_KEY, "");
			String[] e_result=null;
			if (!ev.equals("")) {
				e_result=ev.split(",");
			}
			settingExcludePictureDirectory=e_result;
			
			prefs.edit().remove(OLD_INCLUDE_DIR_KEY).commit();
			prefs.edit().remove(OLD_EXCLUDE_DIR_KEY).commit();
			
			if (settingSelectPictureDirectory!=null) {
				for(String item:settingSelectPictureDirectory) {
					ScanFolderItem sfi=new ScanFolderItem();
					sfi.folder_path=item;
					sfi.include=true;
					scan_list.add(sfi);
				}
			}
			if (settingExcludePictureDirectory!=null) {
				for(String item:settingExcludePictureDirectory) {
					ScanFolderItem sfi=new ScanFolderItem();
					sfi.folder_path=item;
					sfi.include=false;
					scan_list.add(sfi);
				}
			}
			settingScanDirectoryList=scan_list;
			saveScanFolderList(c);
		} else {
			String scan_list_value=prefs.getString(SCAN_FOLDER_LIST_KEY, "default");
//			Log.v("","load sv="+scan_list_value);
			if (scan_list_value.equals("default")) {
				ScanFolderItem sfi=new ScanFolderItem();
				sfi.folder_path=this.internalRootDirectory+"/DCIM";
				sfi.process_sub_directories=true;
				sfi.include=true;
				scan_list.add(sfi);

				sfi=new ScanFolderItem();
				sfi.folder_path=this.internalRootDirectory+"/Pictures";
				sfi.process_sub_directories=true;
				sfi.include=true;
				scan_list.add(sfi);
				
				if (!this.externalRootDirectory.equals(STORAGE_STATUS_UNMOUNT)) {
					sfi=new ScanFolderItem();
					sfi.folder_path=this.externalRootDirectory;
					sfi.process_sub_directories=true;
					sfi.include=true;
					scan_list.add(sfi);

					sfi=new ScanFolderItem();
					sfi.folder_path=this.externalRootDirectory+"/Android";
					sfi.process_sub_directories=true;
					sfi.include=false;
					scan_list.add(sfi);

					sfi=new ScanFolderItem();
					sfi.folder_path=this.externalRootDirectory+"/LOST.DIR";
					sfi.process_sub_directories=true;
					sfi.include=false;
					scan_list.add(sfi);

				}
			} else {
				if (!scan_list_value.equals("")) {
					String[] items=scan_list_value.split("\n");
					for(int i=0;i<items.length;i++) {
						String[] list=items[i].split("\t");
						ScanFolderItem sfi=new ScanFolderItem();
						sfi.folder_path=list[0];
						sfi.process_sub_directories=list[1].equals("1")?true:false;
						sfi.include=list[2].equals("1")?true:false;
						scan_list.add(sfi);
					}
				}
			}
			settingScanDirectoryList=scan_list;
		}
	};
	
	private boolean isDebuggable() {
		boolean result=false;
        PackageManager manager = appContext.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = manager.getApplicationInfo(appContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
        	result=false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
        	result=true;
//        Log.v("","debuggable="+result);
        return result;
    };
	
}
