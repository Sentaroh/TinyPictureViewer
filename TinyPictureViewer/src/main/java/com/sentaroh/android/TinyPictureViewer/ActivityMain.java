package com.sentaroh.android.TinyPictureViewer;


import static com.sentaroh.android.TinyPictureViewer.Constants.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import com.sentaroh.android.TinyPictureViewer.Log.LogFileListDialogFragment;
import com.sentaroh.android.Utilities.ContentProviderUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.SafFile;
import com.sentaroh.android.Utilities.StringUtil;
import com.sentaroh.android.Utilities.ThemeUtil;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.ContextButton.ContextButtonUtil;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.FileSelectDialogFragment;
import com.sentaroh.android.Utilities.Dialog.ProgressSpinDialogFragment;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.Widget.CustomPopupMenu;
import com.sentaroh.android.Utilities.Widget.CustomPopupMenu.OnMenuItemClickListener;
import com.sentaroh.android.Utilities.Widget.CustomTabContentView;
import com.sentaroh.android.Utilities.Widget.CustomViewPagerAdapter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

public class ActivityMain extends AppCompatActivity {
	private GlobalParameters mGp=null;
	
	private boolean mTerminateApplication=false;
	private int mRestartStatus=0;

	private CommonDialog mCommonDlg=null;

	private FragmentManager mFragmentManager=null;
	
	private Context mContext=null;
	private ActivityMain mActivity=null;
	
	private CommonUtilities mUtil=null;

	private ActionBar mActionBar=null;

	private float mDefaultBackLightLevel=0;
	
	private Handler mUiHandler=null;
	
	private String mCurrentThumbnailViewTitle="";
	
	private ThreadCtrl mTcCreatePictureCacheFile=new ThreadCtrl();

	private boolean enableMainUi=true;
	
//	private int mFolderListPosition=0;
	
	private PictureView mPictureView=null;

    private ContentObserver mContentObserver=null;
	private boolean mRefreshFilelistRequired=false;

//	private boolean mShowProgressBar=false;

	@Override  
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mUtil.addDebugMsg(1, "I", "onSaveInstanceState entered");
	};  

	@Override  
	protected void onRestoreInstanceState(Bundle savedState) {  
		super.onRestoreInstanceState(savedState);
		mUtil.addDebugMsg(1, "I", "onRestoreInstanceState entered");
		mRestartStatus=2;
	};

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
	   mContext=getApplicationContext();
	   mActivity=this;
	   requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
       mFragmentManager=getSupportFragmentManager();
       mRestartStatus=0;
       mGp=(GlobalParameters)getApplication();
       mGp.refreshMediaDir(mContext);
//       setTheme(mGp.applicationTheme);
       mGp.themeColorList=ThemeUtil.getThemeColorList(mActivity);
       super.onCreate(savedInstanceState);

       mUtil=new CommonUtilities(mContext, "Main", mGp);
       
       mUtil.addDebugMsg(1, "I", "onCreate entered, bd="+savedInstanceState);

       mTcCreatePictureCacheFile.setDisabled();
       
       resetDeviceOrientation();
       
       mUiHandler=new Handler();
       
       mCommonDlg=new CommonDialog(mActivity, mFragmentManager);
       
       mDefaultBackLightLevel=getWindow().getAttributes().screenBrightness;
       
       mGp.masterFolderList=loadFolderList();
       mGp.showedFolderList.clear();
       mGp.showedFolderList.addAll(mGp.masterFolderList);
	   if (mGp.masterFolderList.size()==0) {
			PictureUtil.clearCacheFileDirectory(mGp.pictureFileCacheDirectory);
			PictureUtil.clearCacheFileDirectory(mGp.pictureBitmapCacheDirectory);
	   }

       mPictureView=new PictureView(mActivity, mGp, mUtil, mCommonDlg);

       checkMapApplicationAvailability();

       initViewWidget();
       
       if (Build.VERSION.SDK_INT>=24) {
    	   resetMultiWindowMode(isInMultiWindowMode());
       }

       setUiActionBar();
       checkRequiredPermissions();
       checkSdcardAccess();
       resetDeviceOrientation();

       mContentObserver=new ContentObserver(new Handler()) {
    	   @Override
    	   public void onChange(boolean selfChange) {
    		   onChange(selfChange, null);
    	   };
	   	   @Override
	   	   public void onChange(boolean selfChange, Uri uri) {
	   	    	mUtil.addDebugMsg(1,"I","onChange entered "+"selfChange="+selfChange+", Uri="+uri);
//  	    	if (mTcBuildFolderList==null) {
//  				if (mGp.settingAutoFileChangeDetection.equals(AUTO_FILE_CHANGE_DETECTION_MEDIA_STORE_CHANGED)) {
//  					buildFolderList();
//  				}
//  	    	}
	   	    	mRefreshFilelistRequired=true;
	   	   };
       };
       getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mContentObserver);
	};
	
	private void checkSdcardAccess() {
       for(ScanFolderItem scan_dir:mGp.settingScanDirectoryList) {
    	   if (scan_dir.folder_path.startsWith(mGp.externalRootDirectory)) {
    	       if (mGp.safMgr.getSdcardSafFile()==null) {
    	    	   NotifyEvent ntfy=new NotifyEvent(mContext);
    	    	   ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						startSdcardPicker();
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						mCommonDlg.showCommonDialog(false, "W", 
							mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg), "", null);
					}
    	    	   });
    	    	   showSelectSdcardMsg(ntfy);
    	       }
    	       break;
    	   }
       }
	};

	private void checkMapApplicationAvailability() {
       Uri gmmIntentUri=Uri.parse("geo:0,0?q=loc:0,0");
       Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
       mapIntent.setPackage("com.google.android.apps.maps");
       if (mapIntent.resolveActivity(getPackageManager())!=null) mGp.mapApplicationAvailable=true;
       else mGp.mapApplicationAvailable=false;
	};
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		mUtil.addDebugMsg(1,"I","onNewIntent entered, "+"resartStatus="+mRestartStatus);
		mGp.refreshMediaDir(mContext);
		if (intent!=null && intent.getData()!=null) showSpecificPicture(intent);
	};

	@Override
	public void onStart() {
		super.onStart();
		mUtil.addDebugMsg(1, "I", "onStart entered");
	};

	@Override
	public void onRestart() {
		super.onStart();
		mUtil.addDebugMsg(1, "I", "onRestart entered");
		if (mRestartStatus==1) {
			if (!mGp.showSinglePicture && mTcBuildFolderList==null && 
					!mGp.settingAutoFileChangeDetection.equals(AUTO_FILE_CHANGE_DETECTION_NONE)) {
				if (mGp.settingAutoFileChangeDetection.equals(AUTO_FILE_CHANGE_DETECTION_ALWAYS)) buildFolderList();
				else {
					if (mRefreshFilelistRequired) buildFolderList();
				}
				mRefreshFilelistRequired=false;
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		mUtil.addDebugMsg(1, "I", "onResume entered, restartStatus="+mRestartStatus);
		if (mRestartStatus==1) {
			mGp.refreshMediaDir(mContext);
			if (mGp.currentView==CURRENT_VIEW_PICTURE) {
				if (mGp.uiMode==UI_MODE_FULL_SCREEN) {
					setUiFullScreen();
				} else if (mGp.uiMode==UI_MODE_FULL_SCREEN_WITH_NAVI) {
					setUiFullScreenWithNaviButton();
//				} else if (mGp.uiMode==UI_MODE_FULL_SCREEN_WITH_SYSTEM_VIEW) {
//					setUiFullScreenWithSystemView();
				}
			}
		} else {
			CommonUtilities.cleanupWorkFile(mGp);
			if (mRestartStatus==0) {
				Intent in=getIntent();
				if (in!=null) {
					if (!showSpecificPicture(in)) {
						mGp.showSinglePicture=true;
						mTerminateApplication=true;
						finish();
					}
				}
			} else if (mRestartStatus==2) {
				if (mGp.activityIsDestroyed) {
					mCommonDlg.showCommonDialog(false, "W",  
					getString(R.string.msgs_main_restart_by_destroyed),"",null);
				} else {
//					mCommonDlg.showCommonDialog(false, "W",  
//					getString(R.string.msgs_main_restart_by_killed),"",null);
				}
			}
	        if (!mGp.showSinglePicture) {
	        	buildFolderList();
				setFolderViewListener();
				setThumbnailViewListener();
	        } else {
	    		mGp.folderView.setVisibility(LinearLayout.GONE);
	    		mGp.thumbnailView.setVisibility(LinearLayout.GONE);
	    		mGp.pictureView.setVisibility(LinearLayout.GONE);
	        }
			mRestartStatus=1;
			mGp.activityIsDestroyed=false;
		}
	};

	@SuppressLint("NewApi")
	@Override
	public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
		super.onMultiWindowModeChanged(isInMultiWindowMode);
		mUtil.addDebugMsg(1, "I", "onMultiWindowModeChanged entered, isInMultiWindowMode="+isInMultiWindowMode);
//		resetMultiWindowMode(isInMultiWindowMode);
	};

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mUtil.addDebugMsg(1, "I", "onLowMemory entered");
       // Application process is follow
		mGp.pictureFileCacheList.clear();
	};

//	@Override
//	public void onTrimMemory(int level) {
//		super.onTrimMemory(level);
//		mUtil.addDebugMsg(1, "I", "onTrimMemory entered, level="+level);
//       // Application process is follow
//		mUiHandler.postDelayed(new Runnable(){
//			@Override
//			public void run() {
//				Runtime.getRuntime().gc();
//			}
//		}, 100);
//
//	};

	@Override
	public void onPause() {
		super.onPause();
		mUtil.addDebugMsg(1, "I", "onPause entered");
       // Application process is follow
	};

	@Override
	public void onStop() {
		super.onStop();
		mUtil.addDebugMsg(1, "I", "onStop entered");
//		System.gc();
//		mGp.pictureFileCacheList.clear();
		Runtime.getRuntime().gc();
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		mUtil.addDebugMsg(1, "I", "onDestroy entered");
       // Application process is follow
		CommonUtilities.cleanupWorkFile(mGp);
		
		mUtil.flushLog();
		
		 this.getContentResolver().unregisterContentObserver(mContentObserver);
		
		if (mTerminateApplication) {
			mGp.settingExitClean=true;
			mTcCreatePictureCacheFile.setDisabled();
			cancelBuildFolderList();
			
			mGp.clearParms();
			
			Handler hndl=new Handler();
			hndl.postDelayed(new Runnable(){
				@Override
				public void run() {
					Runtime.getRuntime().gc();
//					android.os.Process.killProcess(android.os.Process.myPid());
				}
			}, 500);
		} else {
			mGp.activityIsDestroyed=true;
		}
	};
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				handleCloseButtonPressed();
				return true;
				// break;
			default:
				return super.onKeyDown(keyCode, event);
				// break;
		}
	};

	public void handleCloseButtonPressed() {
		mUtil.addDebugMsg(1, "I", "handleCloseButtonPressed entered, currentView="+mGp.currentView+
				", UiEnabled="+isUiEnabled());
		if (isUiEnabled()) {
			if (mGp.currentView==CURRENT_VIEW_FOLDER) {
				if (mGp.adapterFolderView.isSelectMode()) {
					mGp.adapterFolderView.setSelectMode(false);
					mGp.adapterFolderView.notifyDataSetChanged();
					setFolderViewContextButtonVisibility();
				} else {
					if (mGp.spinnerFolderSelector.getSelectedItemPosition()>0) {
						mGp.spinnerFolderSelector.setSelection(0);					
					} else {
						mTerminateApplication=true;
						finish();
//						confirmExit();
					}
				}
			} else if (mGp.currentView==CURRENT_VIEW_THUMBNAIL) {
				if (mGp.adapterThumbnailView.isSelectMode()) {
					mGp.adapterThumbnailView.setSelectMode(false);
					mGp.adapterThumbnailView.notifyDataSetChanged();
					setThumbnailViewContextButtonVisibility();
				} else {
					if (mGp.spinnerPictureSelector.getSelectedItemPosition()>0) {
						mGp.spinnerPictureSelector.setSelection(0);
					} else {
						mTcCreatePictureCacheFile.setDisabled();
						mUiHandler.postDelayed(new Runnable(){
							@Override
							public void run() {
								mGp.adapterThumbnailView.setPictureList(null);
								mGp.adapterThumbnailView.notifyDataSetChanged();
								Runtime.getRuntime().gc();
							}
						},500);
//						mGp.adapterThumbnailView.setPictureList(null);
//						mGp.adapterThumbnailView.notifyDataSetChanged();
						mUiHandler.post(new Runnable(){
							@Override
							public void run() {
//								mGp.mainProgressBar.setVisibility(ProgressBar.VISIBLE);
								showFolderView();
							}
						});
					}
				}
			} else if (mGp.currentView==CURRENT_VIEW_PICTURE) {
				if (mGp.showSinglePicture) {
//					mPictureView.closeView();
					mTerminateApplication=true;
					finish();
				} else {
					int pic_pos=mGp.customViewPager.getCurrentItem();
					mPictureView.closeView();
					setThumbnailViewContextButtonVisibility();
					reshowThumbnailView(pic_pos);
					mGp.mainProgressBar.setVisibility(ProgressBar.VISIBLE);
				}
			}
		}
	};
	
	public void refreshOptionMenu() {
//		Thread.dumpStack();
		invalidateOptionsMenu();
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mUtil.addDebugMsg(1, "I", "onCreateOptionsMenu Entered");
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_top, menu);
		
		createFolderSelectionList();
 	   	return true;
	};
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		mUtil.addDebugMsg(2, "I", "onPrepareOptionsMenu Entered");
		if (mGp.showSinglePicture) return true;

		if (mGp.debuggable) menu.findItem(R.id.menu_top_switch_test_mode).setVisible(true);
		else menu.findItem(R.id.menu_top_switch_test_mode).setVisible(false);
		
		if (mGp.currentView==CURRENT_VIEW_FOLDER) {
			menu.findItem(R.id.menu_top_sort_thumbnail).setVisible(false);
			menu.findItem(R.id.menu_top_edit_scan_folder).setVisible(true);
			if (isUiEnabled()) {
				if (mTcBuildFolderList!=null && mTcBuildFolderList.isEnabled()) {
					menu.findItem(R.id.menu_top_edit_scan_folder).setVisible(false);
					menu.findItem(R.id.menu_top_refresh_file_list).setVisible(false);
					menu.findItem(R.id.menu_top_sort_folder).setVisible(false);
					mGp.spinnerFolderSelector.setVisibility(Spinner.GONE);
					menu.findItem(R.id.menu_top_start_camera).setVisible(false);
				} else {
					menu.findItem(R.id.menu_top_edit_scan_folder).setVisible(true);
					menu.findItem(R.id.menu_top_refresh_file_list).setVisible(true);
					menu.findItem(R.id.menu_top_sort_folder).setVisible(true);
					mGp.spinnerFolderSelector.setVisibility(Spinner.VISIBLE);
					menu.findItem(R.id.menu_top_start_camera).setVisible(true);
				}
			} else {
				mGp.spinnerFolderSelector.setVisibility(Spinner.GONE);
				menu.findItem(R.id.menu_top_sort_folder).setVisible(false);
				menu.findItem(R.id.menu_top_refresh_file_list).setVisible(false);
				menu.findItem(R.id.menu_top_start_camera).setVisible(false);
			}
    	   
			mGp.spinnerPictureSelector.setVisibility(Spinner.GONE);

//    	   	createFolderSelectionList();
		} else if (mGp.currentView==CURRENT_VIEW_THUMBNAIL) {
			menu.findItem(R.id.menu_top_sort_folder).setVisible(false);
			menu.findItem(R.id.menu_top_edit_scan_folder).setVisible(false);
			menu.findItem(R.id.menu_top_show_hide_hidden).setVisible(false);
			mGp.spinnerFolderSelector.setVisibility(Spinner.GONE);
    	   
			if (mTcCreatePictureCacheFile.isEnabled()) {
				mGp.spinnerPictureSelector.setVisibility(Spinner.GONE);
				menu.findItem(R.id.menu_top_sort_thumbnail).setVisible(false);
				menu.findItem(R.id.menu_top_refresh_file_list).setVisible(false);
				menu.findItem(R.id.menu_top_start_camera).setVisible(false);
			} else {
				if (isUiEnabled()) {
					mGp.spinnerPictureSelector.setVisibility(Spinner.VISIBLE);
					menu.findItem(R.id.menu_top_sort_thumbnail).setVisible(true);
					menu.findItem(R.id.menu_top_refresh_file_list).setVisible(true);
					menu.findItem(R.id.menu_top_start_camera).setVisible(true);
				} else {
					mGp.spinnerPictureSelector.setVisibility(Spinner.GONE);
					menu.findItem(R.id.menu_top_sort_thumbnail).setVisible(false);
					menu.findItem(R.id.menu_top_refresh_file_list).setVisible(false);
					menu.findItem(R.id.menu_top_start_camera).setVisible(false);
				}
			}
			createPictureSelectionList();
		} else {
			menu.findItem(R.id.menu_top_sort_thumbnail).setVisible(false);
			menu.findItem(R.id.menu_top_sort_folder).setVisible(false);
			menu.findItem(R.id.menu_top_refresh_file_list).setVisible(false);
			menu.findItem(R.id.menu_top_edit_scan_folder).setVisible(false);
			mGp.spinnerPictureSelector.setVisibility(Spinner.GONE);
			mGp.spinnerPictureSelector.setVisibility(Spinner.GONE);
        }

		if (mGp.adapterFolderView.getSortOrder()==SORT_ORDER_ASCENDANT) menu.findItem(R.id.menu_top_sort_folder).setIcon(R.drawable.ic_128_sort_asc_gray);
		else menu.findItem(R.id.menu_top_sort_folder).setIcon(R.drawable.ic_128_sort_dsc_gray);
		if (mGp.adapterThumbnailView.getSortOrder()==SORT_ORDER_ASCENDANT) menu.findItem(R.id.menu_top_sort_thumbnail).setIcon(R.drawable.ic_128_sort_asc_gray);
		else menu.findItem(R.id.menu_top_sort_thumbnail).setIcon(R.drawable.ic_128_sort_dsc_gray);

		if (mGp.settingScanHiddenFile) menu.findItem(R.id.menu_top_show_hide_hidden).setTitle(mContext.getString(R.string.msgs_menu_show_hidden_file_to_hide));
		else menu.findItem(R.id.menu_top_show_hide_hidden).setTitle(mContext.getString(R.string.msgs_menu_show_hidden_file_to_show));
		
		if (mGp.settingDebugLevel>0 && mGp.settingLogOption) {
			menu.findItem(R.id.menu_top_log_management).setVisible(true);
		} else {
			menu.findItem(R.id.menu_top_log_management).setVisible(false);
		}
        if (isUiEnabled()) {
        	menu.findItem(R.id.menu_top_sort_folder).setEnabled(true);
        	menu.findItem(R.id.menu_top_sort_thumbnail).setEnabled(true);
        	menu.findItem(R.id.menu_top_refresh_file_list).setEnabled(true);
        	menu.findItem(R.id.menu_top_show_hide_hidden).setEnabled(true);
        	menu.findItem(R.id.menu_top_edit_scan_folder).setEnabled(true);
        	menu.findItem(R.id.menu_top_quit).setEnabled(true);

	        menu.findItem(R.id.menu_top_log_management).setEnabled(true);
	        menu.findItem(R.id.menu_top_config_backup_restore).setEnabled(true);
	        menu.findItem(R.id.menu_top_about).setEnabled(true);
	        menu.findItem(R.id.menu_top_settings).setEnabled(true);
        } else {
        	menu.findItem(R.id.menu_top_sort_folder).setEnabled(false);
        	menu.findItem(R.id.menu_top_sort_thumbnail).setEnabled(false);
        	menu.findItem(R.id.menu_top_refresh_file_list).setEnabled(false);
        	menu.findItem(R.id.menu_top_show_hide_hidden).setEnabled(false);
        	menu.findItem(R.id.menu_top_edit_scan_folder).setEnabled(false);
        	menu.findItem(R.id.menu_top_quit).setEnabled(false);

	        menu.findItem(R.id.menu_top_log_management).setEnabled(false);
	        menu.findItem(R.id.menu_top_config_backup_restore).setEnabled(false);
	        menu.findItem(R.id.menu_top_about).setEnabled(false);
	        menu.findItem(R.id.menu_top_settings).setEnabled(false);
        }
    	menu.findItem(R.id.menu_top_quit).setVisible(false);
        
        return true;
	};

	private void refreshFileList() {
		mGp.adapterFolderView.setSelectMode(false);
		mGp.adapterThumbnailView.setSelectMode(false);
		buildFolderList();
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) { 
		mUtil.addDebugMsg(2, "I", "onOptionsItemSelected Entered");
		
		switch (item.getItemId()) {
			case android.R.id.home:
				handleCloseButtonPressed();
				return true;
			case R.id.menu_top_sort_folder:
				showSortFolderMenu(item);
				return true;
			case R.id.menu_top_sort_thumbnail:
				showSortThumbnailMenu();
				return true;
			case R.id.menu_top_refresh_file_list:
				refreshFileList();
				return true;
			case R.id.menu_top_start_camera:
				Intent in=new Intent();
				in.setAction(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
				startActivity(in);
				return true;
			case R.id.menu_top_show_hide_hidden:
				confirmHiddenFile();
				return true;
			case R.id.menu_top_edit_scan_folder:
				editScanFolderList();
				return true;
			case R.id.menu_top_quit:
				handleCloseButtonPressed();
				return true;
			case R.id.menu_top_log_management:
				invokeLogManagement();
				return true;
			case R.id.menu_top_about:
				aboutApplicaion();
				return true;			
			case R.id.menu_top_settings:
				invokeSettingsActivity();
				return true;			
			case R.id.menu_top_kill:
				confirmKill();
				return true;			
			case R.id.menu_top_config_backup_restore:
				backupRestoreConfig();
				return true;			
			case R.id.menu_top_uninstall:
				uninstallApplication();
				return true;			
			case R.id.menu_top_switch_test_mode:
				mGp.pictureShowTestMode=!mGp.pictureShowTestMode;
				return true;			
		}
		return false;
	};
	
	private boolean showSpecificPicture(Intent intent) {
		mUtil.addDebugMsg(1,"I","showSpecificPicture, "+"Uri="+intent.getData()+", type="+intent.getType());
		boolean result=true;
		if (intent.getData()!=null) {
			String cd=getCacheDir()+"/Attached/image/";
    		mGp.folderView.setVisibility(LinearLayout.GONE);
    		mGp.thumbnailView.setVisibility(LinearLayout.GONE);
    		mGp.pictureView.setVisibility(LinearLayout.GONE);

            String file_path=ContentProviderUtil.getFilePath(mContext, cd, intent.getData());
    		if (file_path!=null) {
    			if (PictureUtil.isPictureFile(mGp, file_path)) {
        			mGp.showedPictureList.clear();
        			File tlf=new File(file_path);
        			File df=new File(tlf.getParent());
        			File[] fl=df.listFiles();
        			int pic_pos=0;
        			if (fl!=null && fl.length>0) {
        				for(File cf:fl) {
        					if (PictureUtil.isPictureFile(mGp, cf.getName())) {
        						PictureListItem pli=new PictureListItem();
        						pli.setFileName(cf.getName());
        						pli.setParentDirectory(PictureListItem.createParentDirectory(cf));
        						pli.setFolderName(PictureListItem.createFolderName(cf));
        						pli.setFileLastModified(cf.lastModified());
        						pli.setFileLength(cf.length());

            					if (tlf.getName().equals(cf.getName())) {
            						pli.createFileInfo(cf);
            						pli.createExifInfo(cf.getAbsolutePath());
            						pic_pos=mGp.showedPictureList.size();
            					}
            					mGp.showedPictureList.add(pli);
        					}
        				}
        				
        				mGp.currentPictureList=new ArrayList<PictureListItem>();
        				mGp.currentFolderListItem=new FolderListItem();
        				mGp.currentFolderListItem.setParentDirectory(mGp.showedPictureList.get(pic_pos).getParentDirectory());
        				final int w_pos=pic_pos;
        				mUiHandler.post(new Runnable(){
        					@Override
        					public void run() {
        						mPictureView.showPictureView(w_pos);
        					}
        				});
        			}
        			mGp.settingPictureDisplayLastUiMode=mGp.settingPictureDisplayDefualtUiMode;
// 		           	mGp.mPictureViewShowNavigateButton=false;
    				mGp.showSinglePicture=true;			
    			} else {
    				result=false;
    			}
    		} else {
                result=false;
            }
		};
		return result;
	};
	
	private void confirmHiddenFile() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				mGp.saveSettingOptionHiddenFile(mContext, !mGp.settingScanHiddenFile);
				mGp.settingScanHiddenFile=!mGp.settingScanHiddenFile;
				refreshFileList();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				
			}
		});
		if (mGp.settingScanHiddenFile) {
			mCommonDlg.showCommonDialog(true, "W", 
					mContext.getString(R.string.msgs_main_hidden_directory_confirm_title), "", ntfy);
		} else {
			ntfy.notifyToListener(true, null);
		}
	};
	
	private void createFolderSelectionList() {
 	   	mGp.spinnerFolderSelector.setOnItemSelectedListener(null);
 	   	ArrayList<String>sel_list=new ArrayList<String>();
 	   	if (mGp.masterFolderList!=null) {
 	 	   	for(FolderListItem fli:mGp.masterFolderList) {
 			   String w_folder=
 					   fli.getFolderName().length()>=mGp.settingFolderSelectionCharacterCount?
 							fli.getFolderName().substring(0,mGp.settingFolderSelectionCharacterCount):
 							fli.getFolderName();
 			   boolean found=false;
 			   for(int i=0;i<sel_list.size();i++) {
 				   if (sel_list.get(i).equals(w_folder)) {
 					   found=true;
 					   break;
 				   }
 			   }
 			   if (!found) sel_list.add(w_folder);
	  	   	}
	  	   	Collections.sort(sel_list);
 	   	}
 	   	mGp.adapterFolderSelectorSpinner.clear();
 	   	mGp.adapterFolderSelectorSpinner.addAll(sel_list);
 	   	if (mGp.adapterFolderSelectorSpinner.getCount()==1) {
 	   		mGp.adapterFolderSelectorSpinner.clear();
 	   		mGp.spinnerFolderSelector.setEnabled(false);
 	   	} else mGp.spinnerFolderSelector.setEnabled(true);
 	   	mGp.adapterFolderSelectorSpinner.insert(mContext.getString(R.string.msgs_main_folder_view_folder_selector_all_folder), 0);
 	   	mGp.adapterFolderSelectorSpinner.notifyDataSetChanged();
 	   	mGp.spinnerFolderSelector.setSelection(mGp.selectFolderSpinnerPosition, false);
 	   	mGp.spinnerFolderSelector.setOnItemSelectedListener(mGp.folderSelectorListener);
	};
	
	private void createPictureSelectionList() {
		mGp.spinnerPictureSelector.setOnItemSelectedListener(null);
		ArrayList<String>sel_list=new ArrayList<String>();
 	   	for(PictureListItem pli:mGp.currentPictureList) {
		   if (!pli.getExifDateTime().equals("")) {
			   String w_month=pli.getExifDateTime().substring(0,7);
			   String n_month="";
			   if (!w_month.equals("0000/00")) n_month=w_month;
			   else n_month=mContext.getString(R.string.msgs_main_picture_file_date_time_unknown);
			   boolean found=false;
			   for(int i=0;i<sel_list.size();i++) {
				   if (sel_list.get(i).equals(n_month)) {
					   found=true;
					   break;
				   }
			   }
			   if (!found) sel_list.add(n_month);
		   }
 	   	}
 	   	Collections.sort(sel_list);
// 	   	Collections.sort(tag_list);
 	   	mGp.adapterPictureSelectorSpinner.clear();
 	   	mGp.adapterPictureSelectorSpinner.addAll(sel_list);
// 	   	mGp.adapterPictureSelectorSpinner.addAll(tag_list);
 	   	if (mGp.adapterPictureSelectorSpinner.getCount()==1) {
 	   		mGp.adapterPictureSelectorSpinner.clear();
 	   		mGp.spinnerPictureSelector.setEnabled(false);
 	   	} else mGp.spinnerPictureSelector.setEnabled(true);
 	   	mGp.adapterPictureSelectorSpinner.insert(mContext.getString(R.string.msgs_main_thumbnail_view_date_selector_all_picture), 0);
 	   	mGp.adapterPictureSelectorSpinner.notifyDataSetChanged();
 	   	mGp.spinnerPictureSelector.setSelection(mGp.selectPictureDateSpinnerPosition, false);
 	   	mGp.spinnerPictureSelector.setOnItemSelectedListener(mGp.pictureSelectorListener);
	};
	
	private void performSortFolderList(int key) {
		if (mGp.adapterFolderView.getSortKey()==key) {
			if (mGp.adapterFolderView.getSortOrder()==SORT_ORDER_ASCENDANT) mGp.adapterFolderView.setSortOrder(SORT_ORDER_DESCENDANT);
			else mGp.adapterFolderView.setSortOrder(SORT_ORDER_ASCENDANT);
		} else {
			mGp.adapterFolderView.setSortOrder(SORT_ORDER_ASCENDANT);
		}
		
		mGp.adapterFolderView.setSortKey(key);
		mGp.adapterFolderView.sort();
    	saveFolderList(mGp.masterFolderList);
    	mGp.folderListSortKey=mGp.adapterFolderView.getSortKey();
    	mGp.folderListSortOrder=mGp.adapterFolderView.getSortOrder();
    	mGp.saveFolderSortParm(mContext);
    	
    	refreshOptionMenu();
	};

	private void performSortThumbnailList(int key) {
		if (mGp.adapterThumbnailView.getSortKey()==key) {
			if (mGp.adapterThumbnailView.getSortOrder()==SORT_ORDER_ASCENDANT) mGp.adapterThumbnailView.setSortOrder(SORT_ORDER_DESCENDANT);
			else mGp.adapterThumbnailView.setSortOrder(SORT_ORDER_ASCENDANT);
		} else {
			mGp.adapterThumbnailView.setSortOrder(SORT_ORDER_ASCENDANT);
		}
		
		mGp.adapterThumbnailView.setSortKey(key);
    	mGp.adapterThumbnailView.sort();
    	mGp.currentFolderListItem.setThumbnailArray(mGp.adapterThumbnailView.getItem(0).getThumbnailImageByte());
		mGp.currentFolderListItem.setSortKey(mGp.adapterThumbnailView.getSortKey());
		mGp.currentFolderListItem.setSortOrder(mGp.adapterThumbnailView.getSortOrder());
    	mGp.adapterFolderView.notifyDataSetChanged();
    	saveFolderList(mGp.masterFolderList);
    	
    	refreshOptionMenu();
	};

	private void showSortFolderMenu(MenuItem item) {
		CustomPopupMenu popup = new CustomPopupMenu(this, mGp.mainToolBar, Gravity.END);
	    popup.getMenuInflater().inflate(R.menu.menu_sort_folder, popup.getMenu());

		if (mGp.adapterFolderView.getSortKey()==SORT_KEY_FOLDER_NAME) {
			if (mGp.adapterFolderView.getSortOrder()==SORT_ORDER_ASCENDANT) {
				popup.getMenu().findItem(R.id.menu_sort_folder_name).setIcon(R.drawable.context_button_arrow_up);
			} else {
				popup.getMenu().findItem(R.id.menu_sort_folder_name).setIcon(R.drawable.context_button_arrow_down);
			}
		} else if (mGp.adapterFolderView.getSortKey()==SORT_KEY_FOLDER_DIR_LAST_MODIFIED) {
			if (mGp.adapterFolderView.getSortOrder()==SORT_ORDER_ASCENDANT) {
				popup.getMenu().findItem(R.id.menu_sort_folder_time).setIcon(R.drawable.context_button_arrow_up);
			} else {
				popup.getMenu().findItem(R.id.menu_sort_folder_time).setIcon(R.drawable.context_button_arrow_down);
			}
		} else if (mGp.adapterFolderView.getSortKey()==SORT_KEY_FOLDER_PATH) {
			if (mGp.adapterFolderView.getSortOrder()==SORT_ORDER_ASCENDANT) {
				popup.getMenu().findItem(R.id.menu_sort_folder_path).setIcon(R.drawable.context_button_arrow_up);
			} else {
				popup.getMenu().findItem(R.id.menu_sort_folder_path).setIcon(R.drawable.context_button_arrow_down);
			}
		}

	    popup.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
					case R.id.menu_sort_folder_name:
						performSortFolderList(SORT_KEY_FOLDER_NAME);
						return true;
					case R.id.menu_sort_folder_time:
						performSortFolderList(SORT_KEY_FOLDER_DIR_LAST_MODIFIED);
						return true;
					case R.id.menu_sort_folder_path:
						performSortFolderList(SORT_KEY_FOLDER_PATH);
						return true;
				};
				return true;
			}
	    });
	    popup.show();
	};

	
	private void showSortThumbnailMenu() {
		CustomPopupMenu popup = new CustomPopupMenu(this, mGp.mainToolBar, Gravity.END);
	    popup.getMenuInflater().inflate(R.menu.menu_sort_thumbnail, popup.getMenu());

		if (mGp.adapterThumbnailView.getSortKey()==SORT_KEY_THUMBNAIL_FILE_NAME) {
			if (mGp.adapterThumbnailView.getSortOrder()==SORT_ORDER_ASCENDANT) {
				popup.getMenu().findItem(R.id.menu_sort_thumbnail_name).setIcon(R.drawable.context_button_arrow_up);
			} else {
				popup.getMenu().findItem(R.id.menu_sort_thumbnail_name).setIcon(R.drawable.context_button_arrow_down);
			}
		} else if (mGp.adapterThumbnailView.getSortKey()==SORT_KEY_THUMBNAIL_FILE_LAST_MODIFIED) {
			if (mGp.adapterThumbnailView.getSortOrder()==SORT_ORDER_ASCENDANT) {
				popup.getMenu().findItem(R.id.menu_sort_thumbnail_file_time).setIcon(R.drawable.context_button_arrow_up);
			} else {
				popup.getMenu().findItem(R.id.menu_sort_thumbnail_file_time).setIcon(R.drawable.context_button_arrow_down);
			}
		} else if (mGp.adapterThumbnailView.getSortKey()==SORT_KEY_THUMBNAIL_PICTURE_TIME) {
			if (mGp.adapterThumbnailView.getSortOrder()==SORT_ORDER_ASCENDANT) {
				popup.getMenu().findItem(R.id.menu_sort_thumbnail_picture_time).setIcon(R.drawable.context_button_arrow_up);
			} else {
				popup.getMenu().findItem(R.id.menu_sort_thumbnail_picture_time).setIcon(R.drawable.context_button_arrow_down);
			}
		}
		
	    popup.setOnMenuItemClickListener(new OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
					case R.id.menu_sort_thumbnail_name:
						performSortThumbnailList(SORT_KEY_THUMBNAIL_FILE_NAME);
						return true;
					case R.id.menu_sort_thumbnail_file_time:
						performSortThumbnailList(SORT_KEY_THUMBNAIL_FILE_LAST_MODIFIED);
						return true;
					case R.id.menu_sort_thumbnail_picture_time:
						performSortThumbnailList(SORT_KEY_THUMBNAIL_PICTURE_TIME);
						return true;
					case R.id.menu_sort_thumbnail_set_sort_parm:
						confirmSortParmSet();
						return true;
				};
				return true;
			}
	    });
	    popup.show();
	};

	private void confirmSortParmSet() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				for(FolderListItem fli:mGp.masterFolderList) {
					fli.setSortKey(mGp.adapterThumbnailView.getSortKey());
					fli.setSortOrder(mGp.adapterThumbnailView.getSortOrder());
				}
		    	saveFolderList(mGp.masterFolderList);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
			
		});
		mCommonDlg.showCommonDialog(true, "W", 
				mContext.getString(R.string.msgs_main_sort_thumbnail_set_sort_parm_confirm), "", ntfy);
	};
	
	private final int REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE=1;
	@SuppressLint("NewApi")
	private void checkRequiredPermissions() {
		if (Build.VERSION.SDK_INT>=23) {
			mUtil.addDebugMsg(1, "I", "Prermission WriteExternalStorage="+checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)+
					", WakeLock="+checkSelfPermission(Manifest.permission.WAKE_LOCK));
	        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
	        	NotifyEvent ntfy=new NotifyEvent(mContext);
	        	ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
			            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
			            		REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
			        	NotifyEvent ntfy_term=new NotifyEvent(mContext);
			        	ntfy_term.setListener(new NotifyEventListener(){
							@Override
							public void positiveResponse(Context c, Object[] o) {
					        	finish();
							}
							@Override
							public void negativeResponse(Context c, Object[] o) {
							}
			        	});
			        	mCommonDlg.showCommonDialog(false, "W", 
			        			mContext.getString(R.string.msgs_main_permission_external_storage_title), 
			        			mContext.getString(R.string.msgs_main_permission_external_storage_denied_msg), ntfy_term);
					}
	        	});
	        	mCommonDlg.showCommonDialog(false, "W", 
	        			mContext.getString(R.string.msgs_main_permission_external_storage_title), 
	        			mContext.getString(R.string.msgs_main_permission_external_storage_request_msg), ntfy);
	        }
		}
	};

	private void invokeSettingsActivity() {
		mUtil.addDebugMsg(1,"I","Invoke Settings.");
		Intent intent=null;
		intent = new Intent(this, ActivitySettings.class);
		startActivityForResult(intent,0);
	};

	private void invokeLogManagement() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				mGp.saveSettingOptionLogEnabled(mContext, (Boolean)o[0]);
//				loadSettingParms();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		mUtil.flushLog();
		LogFileListDialogFragment lfm=
				LogFileListDialogFragment.newInstance(false, getString(R.string.msgs_log_management_title));
		lfm.showDialog(getSupportFragmentManager(), lfm, mGp, ntfy);
	};

	public boolean isApplicationTerminating() {return mTerminateApplication;}
	
	@SuppressWarnings("unused")
	private void confirmExit() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				mTerminateApplication=true;
				finish();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				mGp.settingExitClean=false;
			}
		});
		mCommonDlg.showCommonDialog(true, "W",
				mContext.getString(R.string.msgs_main_exit_confirm_msg), "", ntfy);
	};

	private void confirmKill() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				mGp.settingExitClean=false;
			}
		});
		mCommonDlg.showCommonDialog(true, "W",
				mContext.getString(R.string.msgs_main_kill_confirm_msg), "", ntfy);
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mUtil.addDebugMsg(1, "I", "Return from settings, rc="+requestCode);
		if (requestCode==0) applySettingParms();
		else if (requestCode == ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS) {
			mUtil.addDebugMsg(1,"I","Return from Storage Picker. id="+requestCode);
	        if (resultCode == Activity.RESULT_OK) {
	        	mUtil.addDebugMsg(1,"I","Intent="+data.getData().toString());
       		if (mGp.safMgr.isRootTreeUri(data.getData())) {
	        		mGp.safMgr.addSafFileFromUri(data.getData());
//	        		if (mSafSelectActivityNotify!=null) mSafSelectActivityNotify.notifyToListener(true, null);
	        	} else {
	        		NotifyEvent ntfy_retry=new NotifyEvent(mContext);
	        		ntfy_retry.setListener(new NotifyEventListener(){
						@Override
						public void positiveResponse(Context c, Object[] o) {
			        		NotifyEvent ntfy=new NotifyEvent(mContext);
			        		ntfy.setListener(new NotifyEventListener(){
								@Override
								public void positiveResponse(Context c, Object[] o) {
									startSdcardPicker();
								}
								@Override
								public void negativeResponse(Context c, Object[] o) {
									mCommonDlg.showCommonDialog(false, "W", 
											mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
											mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
											null);
								}
			        		});
			        		showSelectSdcardMsg(ntfy);
						}
						@Override
						public void negativeResponse(Context c, Object[] o) {
						}
	        		});
	        		mCommonDlg.showCommonDialog(true, "W", 
	        				mContext.getString(R.string.msgs_main_external_sdcard_select_retry_select_msg), "", ntfy_retry);
	        	}
	        } else {
	        	if (mGp.safMgr.getSdcardSafFile()==null) {
					mCommonDlg.showCommonDialog(false, "W", 
							mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
							mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
							null);
	        	}
	        }
		}
	};
	
	private void applySettingParms() {
		boolean prev_hidden_file=mGp.settingScanHiddenFile;
		boolean prev_always_top=mGp.settingCameraFolderAlwayTop;
		mGp.loadSettingsParms(mContext);
		mGp.setLogParms(mGp);
		mGp.refreshMediaDir(mContext);

		if (!mGp.settingPictureDisplayOptionRestoreWhenStartup) {
			mGp.settingPictureDisplayLastUiMode=mGp.settingPictureDisplayDefualtUiMode;
		}

		if (((prev_hidden_file && !mGp.settingScanHiddenFile) || (!prev_hidden_file && mGp.settingScanHiddenFile)) ||
    		   mGp.masterFolderList.size()==0) {
			mGp.adapterFolderView.setSelectMode(false);
			mGp.adapterThumbnailView.setSelectMode(false);

   		   	cancelBuildFolderList();
   		   	mGp.adapterFolderView.notifyDataSetChanged();
   		   	buildFolderList();
		}

       if ((prev_always_top && !mGp.settingCameraFolderAlwayTop) || (!prev_always_top && mGp.settingCameraFolderAlwayTop)) {
    	   for(FolderListItem fli:mGp.masterFolderList) {
    	       setCameraFolderAlwaysTop(fli);
    	   }
    	   createFolderShowedList((String)mGp.spinnerFolderSelector.getSelectedItem());
    	   createFolderSelectionList();
    	   mGp.adapterFolderView.notifyDataSetChanged();
    	   saveFolderList(mGp.masterFolderList);
       }
       if (mGp.masterFolderList.size()==0) {
    	   showFolderView();
       }

	};

	public void setUiEnabled() {
		enableMainUi=true;
		if (mGp.adapterFolderView!=null) mGp.adapterFolderView.setAdapterEnabled(true);
		if (mGp.adapterThumbnailView!=null) mGp.adapterThumbnailView.setAdapterEnabled(true);
//		mUiHandler.post(new Runnable(){
//			@Override
//			public void run() {
//				mGp.mFolderGridView.setAlpha(1.0f);
//				mGp.mThumbnailGridView.setAlpha(1.0f);
//			}
//		});
		refreshOptionMenu();
	};
	
	public void setUiDisabled() {
//		Thread.dumpStack();
		enableMainUi=false;
		if (mGp.adapterFolderView!=null) mGp.adapterFolderView.setAdapterEnabled(false);
		if (mGp.adapterThumbnailView!=null) mGp.adapterThumbnailView.setAdapterEnabled(false);
//		mUiHandler.post(new Runnable(){
//			@Override
//			public void run() {
//				mGp.mFolderGridView.setAlpha(0.3f);
//				mGp.mThumbnailGridView.setAlpha(0.3f);
//			}
//		});
		refreshOptionMenu();
	};
	
	public boolean isUiEnabled() {
		return enableMainUi;
	};

	private void initViewWidget() {
	       getWindow().setSoftInputMode(
	       WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	       
	       setContentView(R.layout.main_view);
	       
	       createTabView();
	};

	private void resetMultiWindowMode(boolean isInMultiWindowMode) {
//		LinearLayout ll_main=(LinearLayout)findViewById(R.id.main_view);
		LinearLayout ll_spacer_toolbar_top=(LinearLayout)findViewById(R.id.main_view_spacer_view_toolbar_top);
		LinearLayout ll_spacer_toolbar_bot=(LinearLayout)findViewById(R.id.main_view_spacer_view_toolbar_bottom);
		LinearLayout ll_spacer_progress=(LinearLayout)findViewById(R.id.main_view_spacer_view_progress);
		LinearLayout ll_spacer_folder_grid_top=(LinearLayout)findViewById(R.id.main_view_spacer_view_folder_grid_top);
		LinearLayout ll_spacer_folder_grid_bot=(LinearLayout)findViewById(R.id.main_view_spacer_view_folder_grid_bottom);
		LinearLayout ll_spacer_thumbnail_grid_top=(LinearLayout)findViewById(R.id.main_view_spacer_view_thumbnail_grid_top);
		LinearLayout ll_spacer_thumbnail_grid_bot=(LinearLayout)findViewById(R.id.main_view_spacer_view_thumbnail_grid_bottom);
		LinearLayout ll_spacer_pic_top=(LinearLayout)findViewById(R.id.main_view_spacer_view_picture_top);
		LinearLayout ll_spacer_pic_bot=(LinearLayout)findViewById(R.id.main_view_spacer_view_picture_bottom);
		if (isInMultiWindowMode) {
			ll_spacer_toolbar_top.setVisibility(LinearLayout.GONE);
			ll_spacer_toolbar_bot.setVisibility(LinearLayout.GONE);
			ll_spacer_progress.setVisibility(LinearLayout.GONE);
			ll_spacer_folder_grid_top.setVisibility(LinearLayout.GONE);
			ll_spacer_folder_grid_bot.setVisibility(LinearLayout.GONE);
			ll_spacer_thumbnail_grid_top.setVisibility(LinearLayout.GONE);
			ll_spacer_thumbnail_grid_bot.setVisibility(LinearLayout.GONE);
			ll_spacer_pic_top.setVisibility(LinearLayout.GONE);
			ll_spacer_pic_bot.setVisibility(LinearLayout.GONE);
		} else {
			ll_spacer_toolbar_top.setVisibility(LinearLayout.VISIBLE);
			ll_spacer_toolbar_bot.setVisibility(LinearLayout.VISIBLE);
			ll_spacer_progress.setVisibility(LinearLayout.VISIBLE);
			ll_spacer_folder_grid_top.setVisibility(LinearLayout.VISIBLE);
			ll_spacer_folder_grid_bot.setVisibility(LinearLayout.VISIBLE);
			ll_spacer_thumbnail_grid_top.setVisibility(LinearLayout.VISIBLE);
			ll_spacer_thumbnail_grid_bot.setVisibility(LinearLayout.VISIBLE);
			ll_spacer_pic_top.setVisibility(LinearLayout.VISIBLE);
			ll_spacer_pic_bot.setVisibility(LinearLayout.VISIBLE);
		}
//		setUiActionBar();
	};	
		
	@SuppressLint("InflateParams")
	private void createTabView() {
		LinearLayout ll_main=(LinearLayout)findViewById(R.id.main_view);
//		ll_main.setFitsSystemWindows(true);
		ll_main.setBackgroundColor(Color.BLACK);//mGp.themeColorList.window_background_color_content);
		
		mGp.mainToolBar=(Toolbar) findViewById(R.id.tool_bar);
		mGp.mainToolBar.setTitleTextAppearance(mContext, android.R.style.TextAppearance_DeviceDefault_Medium);
		setSupportActionBar(mGp.mainToolBar);
		mActionBar=getSupportActionBar();
		mActionBar.setDisplayShowHomeEnabled(true);
		mActionBar.setHomeButtonEnabled(true);
		
        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout ll_folder=(LinearLayout)vi.inflate(R.layout.main_view_folder,null);
        LinearLayout ll_thumbnail=(LinearLayout)vi.inflate(R.layout.main_view_thumbnail,null);

        LinearLayout container_folder=(LinearLayout)findViewById(R.id.main_view_folder_container);
        container_folder.addView(ll_folder, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        LinearLayout container_thumbnail=(LinearLayout)findViewById(R.id.main_view_thumbnail_container);
        container_thumbnail.addView(ll_thumbnail, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		mGp.folderView=(LinearLayout)ll_folder.findViewById(R.id.main_view_folder_view);
		mGp.folderView.setBackgroundColor(Color.BLACK);//mGp.themeColorList.window_background_color_content);
		
		mGp.mainProgressView=(LinearLayout)findViewById(R.id.main_view_progress_view);
		mGp.mainProgressBar=(ProgressBar)findViewById(R.id.main_view_progress_bar);
		mGp.mainProgressView.setBackgroundColor(Color.TRANSPARENT);
		mGp.mainProgressBar.setBackgroundColor(Color.BLACK);
		mGp.mainProgressView.setVisibility(ProgressBar.GONE);
		
		mGp.thumbnailView=(LinearLayout)ll_thumbnail.findViewById(R.id.main_view_thumbnail_view);
		mGp.thumbnailView.setBackgroundColor(Color.BLACK);//mGp.themeColorList.window_background_color_content);
		mGp.thumbnailView.setVisibility(LinearLayout.GONE);
		
		mGp.folderGridView=(GridView)ll_folder.findViewById(R.id.main_view_folder_grid_view);
		
		mGp.adapterFolderView=new AdapterFolderList(mActivity, mGp.masterFolderList);
		mGp.folderGridView.setAdapter(mGp.adapterFolderView);
//		mGp.mFolderGridView.setFastScrollEnabled(true);
		mGp.adapterFolderView.setSortKey(mGp.folderListSortKey);
		mGp.adapterFolderView.setSortOrder(mGp.folderListSortOrder);

		mGp.thumbnailEmptyView=(TextView)ll_thumbnail.findViewById(R.id.main_view_thumbnail_empty_view);
		
		mGp.thumbnailGridView=(GridView)ll_thumbnail.findViewById(R.id.main_view_thumbnail_grid_view);
		
		mGp.adapterThumbnailView=new AdapterThumbnailList(mActivity, null);
		mGp.thumbnailGridView.setAdapter(mGp.adapterThumbnailView);
//		mThumbnailGridView.setFastScrollEnabled(true);

		mGp.adapterPictureSelectorSpinner=new CustomActionBarSpinnerAdapter(this);
		mGp.adapterPictureSelectorSpinner.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);//simple_spinner_dropdown_item);

		mGp.adapterFolderSelectorSpinner=new CustomActionBarSpinnerAdapter(this);
		mGp.adapterFolderSelectorSpinner.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);//simple_spinner_dropdown_item);

		mGp.pictureSelectorListener=new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		    	mGp.selectPictureDateSpinnerPosition=position;
		   		String sel_month=(String)mGp.spinnerPictureSelector.getItemAtPosition(position);
		   		createPictureShowedList(sel_month);
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
 	   	};
		mGp.folderSelectorListener=new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
//				Thread.dumpStack();
		    	mGp.selectFolderSpinnerPosition=position;
		   		String sel_key=(String)mGp.spinnerFolderSelector.getItemAtPosition(position);
		   		createFolderShowedList(sel_key);
	   			mUiHandler.post(new Runnable(){
					@Override
					public void run() {
						mGp.folderGridView.setSelection(0);
					}
	   			});
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
 	   	};

		mGp.spinnerPictureSelector=(Spinner)findViewById(R.id.main_view_toolbar_picture_spinner);
        mGp.spinnerPictureSelector.setAdapter(mGp.adapterPictureSelectorSpinner);
        mGp.spinnerPictureSelector.setBackground(mActivity.getDrawable(R.drawable.action_bar_spinner_color_background));
 	   	mGp.spinnerPictureSelector.setOnItemSelectedListener(mGp.pictureSelectorListener);
 	   
		mGp.spinnerFolderSelector=(Spinner)findViewById(R.id.main_view_toolbar_folder_spinner);
        mGp.spinnerFolderSelector.setAdapter(mGp.adapterFolderSelectorSpinner);
        mGp.spinnerFolderSelector.setBackground(mActivity.getDrawable(R.drawable.action_bar_spinner_color_background));
 	   	mGp.spinnerFolderSelector.setOnItemSelectedListener(mGp.folderSelectorListener);

		LinearLayout context_folder=(LinearLayout)ll_folder.findViewById(R.id.context_view_folder);
		mGp.contextButtonFolderAdd=(ImageButton)context_folder.findViewById(R.id.context_button_add);
		mGp.contextButtonFolderMediaFileScan=(ImageButton)context_folder.findViewById(R.id.context_button_media_file_scan);
		mGp.contextButtonFolderExclude=(ImageButton)context_folder.findViewById(R.id.context_button_inactivate);
		mGp.contextButtonFolderRename=(ImageButton)context_folder.findViewById(R.id.context_button_rename);
		mGp.contextButtonFolderDelete=(ImageButton)context_folder.findViewById(R.id.context_button_delete);
		mGp.contextButtonFolderSelectAll=(ImageButton)context_folder.findViewById(R.id.context_button_select_all);
		mGp.contextButtonFolderUnselectAll=(ImageButton)context_folder.findViewById(R.id.context_button_unselect_all);
		mGp.contextButtonFolderAddView=(LinearLayout)context_folder.findViewById(R.id.context_button_add_view);
		mGp.contextButtonFolderMediaFileScanView=(LinearLayout)context_folder.findViewById(R.id.context_button_media_file_scan_view);
		mGp.contextButtonFolderExcludeView=(LinearLayout)context_folder.findViewById(R.id.context_button_inactivate_view);
		mGp.contextButtonFolderRenameView=(LinearLayout)context_folder.findViewById(R.id.context_button_rename_view);
		mGp.contextButtonFolderDeleteView=(LinearLayout)context_folder.findViewById(R.id.context_button_delete_view);
		mGp.contextButtonFolderSelectAllView=(LinearLayout)context_folder.findViewById(R.id.context_button_select_all_view);
		mGp.contextButtonFolderUnselectAllView=(LinearLayout)context_folder.findViewById(R.id.context_button_unselect_all_view);
		
		LinearLayout context_thumbnai=(LinearLayout)ll_thumbnail.findViewById(R.id.context_view_thumbnail);
		mGp.contextButtonThumbnailShare=(ImageButton)context_thumbnai.findViewById(R.id.context_button_share);
		mGp.contextButtonThumbnailRename=(ImageButton)context_thumbnai.findViewById(R.id.context_button_rename);
		mGp.contextButtonThumbnailPaste=(ImageButton)context_thumbnai.findViewById(R.id.context_button_paste);
		mGp.contextButtonThumbnailCopy=(ImageButton)context_thumbnai.findViewById(R.id.context_button_copy);
		mGp.contextButtonThumbnailCut=(ImageButton)context_thumbnai.findViewById(R.id.context_button_cut);
		mGp.contextButtonThumbnailDelete=(ImageButton)context_thumbnai.findViewById(R.id.context_button_delete);
		mGp.contextButtonThumbnailSelectAll=(ImageButton)context_thumbnai.findViewById(R.id.context_button_select_all);
		mGp.contextButtonThumbnailUnselectAll=(ImageButton)context_thumbnai.findViewById(R.id.context_button_unselect_all);
		mGp.contextButtonThumbnailShareView=(LinearLayout)context_thumbnai.findViewById(R.id.context_button_share_view);
		mGp.contextButtonThumbnailRenameView=(LinearLayout)context_thumbnai.findViewById(R.id.context_button_rename_view);
		mGp.contextButtonThumbnailPasteView=(LinearLayout)context_thumbnai.findViewById(R.id.context_button_paste_view);
		mGp.contextButtonThumbnailCopyView=(LinearLayout)context_thumbnai.findViewById(R.id.context_button_copy_view);
		mGp.contextButtonThumbnailCutView=(LinearLayout)context_thumbnai.findViewById(R.id.context_button_cut_view);
		mGp.contextButtonThumbnailDeleteView=(LinearLayout)context_thumbnai.findViewById(R.id.context_button_delete_view);
		mGp.contextButtonThumbnailSelectAllView=(LinearLayout)context_thumbnai.findViewById(R.id.context_button_select_all_view);
		mGp.contextButtonThumbnailUnselectAllView=(LinearLayout)context_thumbnai.findViewById(R.id.context_button_unselect_all_view);
		
		mGp.contextClipBoardBtn=(Button)ll_thumbnail.findViewById(R.id.context_view_clipboard_btn);
		
		mPictureView.createView();

	};

	@SuppressWarnings("deprecation")
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    if (mUtil!=null) {
	    	mUtil.addDebugMsg(1,"I",CommonUtilities.getExecutedMethodName()+" Entered, " ,
	    			"New orientation="+newConfig.orientation+
	    			", New language=",newConfig.locale.getLanguage());
	    }
	    SavedViewContents sv=saveViewContents();
	    SavedViewContents picture_sv=mPictureView.saveViewContents();
	    initViewWidget();
	    restoreViewContents(sv);
	    mPictureView.restoreViewContents(picture_sv);
	};

	private SavedViewContents saveViewContents() {
		SavedViewContents sv=new SavedViewContents();
		sv.action_bar_display_option=mActionBar.getDisplayOptions();
//		sv.activity_title=mActivity.getTitle().toString();
		if (mGp.mainProgressView.getVisibility()==ProgressBar.VISIBLE) sv.main_progress=mGp.mainProgressBar.getProgress();
		sv.folder_view_pos_x=mGp.folderGridView.getFirstVisiblePosition();
		sv.image_view_pos_x=mGp.thumbnailGridView.getFirstVisiblePosition();
		sv.thumbnail_list=mGp.adapterThumbnailView.getPictureList();
		sv.thumbnail_view_adapter_select_mode=mGp.adapterThumbnailView.isSelectMode();
		
		sv.folder_list=mGp.adapterFolderView.getFolderList();
		sv.folder_view_adapter_select_mode=mGp.adapterFolderView.isSelectMode();
		
	    sv.folder_adapter_enabled=mGp.adapterFolderView.isAdapterEnabled();
	    sv.thumbnail_adapter_enabled=mGp.adapterThumbnailView.isAdapterEnabled();
	    
	    sv.folder_sort_key=mGp.adapterFolderView.getSortKey();
	    sv.folder_sort_order=mGp.adapterFolderView.getSortOrder();
	    
	    sv.thumbnail_sort_key=mGp.adapterThumbnailView.getSortKey();
	    sv.thumbnail_sort_order=mGp.adapterThumbnailView.getSortOrder();
	    
	    sv.thumbnail_grid_view_visible_status=mGp.thumbnailGridView.getVisibility();
	    sv.thumbnail_empty_view_visible_status=mGp.thumbnailEmptyView.getVisibility();
	    
	    sv.clip_borad_btn_visiblity=mGp.contextClipBoardBtn.getVisibility();
	    sv.clip_board_btn_text=mGp.contextClipBoardBtn.getText().toString();

		return sv;
	};

	private void restoreViewContents(SavedViewContents sv) {
//		mActivity.setTitle(sv.activity_title);
		mActionBar.setDisplayOptions(sv.action_bar_display_option);
		if (mGp.uiMode==UI_MODE_ACTION_BAR) setUiActionBar();
		else if (mGp.uiMode==UI_MODE_FULL_SCREEN) setUiFullScreen();
		else if (mGp.uiMode==UI_MODE_FULL_SCREEN_WITH_NAVI) setUiFullScreenWithNaviButton();
//		else if (mGp.uiMode==UI_MODE_FULL_SCREEN_WITH_SYSTEM_VIEW) setUiFullScreenWithSystemView();
		
		if (sv.main_progress!=-1) {
			mGp.mainProgressBar.setProgress(sv.main_progress);
			mGp.mainProgressView.setVisibility(ProgressBar.VISIBLE);
		} else {
			mGp.mainProgressView.setVisibility(ProgressBar.GONE);
		}
		if (mGp.currentView==CURRENT_VIEW_FOLDER) {
			mGp.folderView.setVisibility(LinearLayout.VISIBLE);
			mGp.thumbnailView.setVisibility(LinearLayout.GONE);
			mGp.pictureView.setVisibility(LinearLayout.GONE);
		} else if (mGp.currentView==CURRENT_VIEW_THUMBNAIL) {
			mGp.folderView.setVisibility(LinearLayout.GONE);
			mGp.thumbnailView.setVisibility(LinearLayout.VISIBLE);
			mGp.pictureView.setVisibility(LinearLayout.GONE);
		} else if (mGp.currentView==CURRENT_VIEW_PICTURE) {
			mGp.folderView.setVisibility(LinearLayout.GONE);
			mGp.thumbnailView.setVisibility(LinearLayout.GONE);
			mGp.pictureView.setVisibility(LinearLayout.VISIBLE);
			mGp.pictureViewFileInfo.setVisibility(sv.picture_view_show_info);
		}
		mGp.folderGridView.setAdapter(mGp.adapterFolderView);
		mGp.folderGridView.setSelection(sv.folder_view_pos_x);
		
		mGp.adapterFolderView.setSortKey(sv.folder_sort_key);
		mGp.adapterFolderView.setSortOrder(sv.folder_sort_order);
		mGp.adapterFolderView.setFolderList(sv.folder_list);
		mGp.adapterFolderView.setSelectMode(sv.folder_view_adapter_select_mode);
		mGp.adapterFolderView.notifyDataSetChanged();
		
		mGp.adapterThumbnailView.setSortKey(sv.thumbnail_sort_key);
		mGp.adapterThumbnailView.setSortOrder(sv.thumbnail_sort_order);
		mGp.adapterThumbnailView.setPictureList(sv.thumbnail_list);
		mGp.adapterThumbnailView.setSelectMode(sv.thumbnail_view_adapter_select_mode);
		mGp.adapterThumbnailView.notifyDataSetChanged();

		mGp.thumbnailGridView.setVisibility(sv.thumbnail_grid_view_visible_status);
		mGp.thumbnailEmptyView.setVisibility(sv.thumbnail_empty_view_visible_status);

		mGp.thumbnailGridView.setAdapter(mGp.adapterThumbnailView);
		mGp.thumbnailGridView.setSelection(sv.image_view_pos_x);
		
	    mGp.adapterFolderView.setAdapterEnabled(sv.folder_adapter_enabled);
	    mGp.adapterThumbnailView.setAdapterEnabled(sv.thumbnail_adapter_enabled);

	    mGp.contextClipBoardBtn.setVisibility(sv.clip_borad_btn_visiblity);
	    mGp.contextClipBoardBtn.setText(sv.clip_board_btn_text);

		setFolderViewListener();
		setThumbnailViewListener();

	};

	public void toggleFullScreenMode() {
		if (!isUiEnabled()) return;
		if (mGp.uiMode==UI_MODE_FULL_SCREEN) {
//			setUiFullScreenWithSystemView();
			setUiFullScreenWithNaviButton();
//		} else if (mGp.uiMode==UI_MODE_FULL_SCREEN_WITH_SYSTEM_VIEW) {
//			setUiFullScreenWithNaviButton();
		} else if (mGp.uiMode==UI_MODE_FULL_SCREEN_WITH_NAVI) {
			setUiFullScreen();
		}
	};

	public void setUiHideSystemView() {
		getWindow().addFlags(
				  WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN 
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//				| WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
				);
		View decor = this.getWindow().getDecorView();
        decor.setSystemUiVisibility(
        		View.SYSTEM_UI_FLAG_FULLSCREEN |
//        		View.SYSTEM_UI_FLAG_LOW_PROFILE |
//        		View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
        		View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | 
        		View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
        		View.SYSTEM_UI_FLAG_HIDE_NAVIGATION  | 
        		View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	};
	private void setUiShowSystemView() {
		getWindow().addFlags(
				  WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN 
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//				| WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
				);
		View decor = this.getWindow().getDecorView();
        decor.setSystemUiVisibility(
//        		View.SYSTEM_UI_FLAG_FULLSCREEN |
//        		View.SYSTEM_UI_FLAG_LOW_PROFILE |
//        		View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
        		View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN  
        		| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION 
//        		| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION  | 
//        		| View.SYSTEM_UI_FLAG_IMMERSIVE
        		| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        		);
	};

	public void setUiFullScreen() {
		mGp.uiMode=UI_MODE_FULL_SCREEN;
		mGp.mainToolBar.setVisibility(android.widget.Toolbar.GONE);
		setUiHideSystemView();
		mGp.pictureViewBottomControl.setVisibility(LinearLayout.INVISIBLE);//GONE);
		mGp.pictureViewTopControl.setVisibility(TextView.INVISIBLE);//GONE);
		if (!mGp.showSinglePicture) mGp.saveSettingPictureDisplayUiMode(mContext);

	};

//	public void setUiFullScreenWithSystemView() {
//		mGp.uiMode=UI_MODE_FULL_SCREEN_WITH_SYSTEM_VIEW;
//		mGp.mainToolBar.setVisibility(android.widget.Toolbar.GONE);
//		setUiShowSystemView();
//		mGp.pictureViewBottomControl.setVisibility(LinearLayout.INVISIBLE);//GONE);
//		mGp.pictureViewTopControl.setVisibility(TextView.INVISIBLE);//GONE);
//		if (!mGp.showSinglePicture) mGp.saveSettingPictureDisplayUiMode(mContext);
//
//	};

	public void setUiFullScreenWithNaviButton() {
		mGp.uiMode=UI_MODE_FULL_SCREEN_WITH_NAVI;
		mGp.mainToolBar.setVisibility(android.widget.Toolbar.GONE);
		setUiShowSystemView();
		mGp.pictureViewBottomControl.setVisibility(LinearLayout.VISIBLE);
		mGp.pictureViewTopControl.setVisibility(TextView.VISIBLE);
		if (!mGp.showSinglePicture) mGp.saveSettingPictureDisplayUiMode(mContext);
	};

	public void setUiActionBar() {
		mGp.uiMode=UI_MODE_ACTION_BAR;
		mGp.mainToolBar.setVisibility(android.widget.Toolbar.VISIBLE);
		setUiShowSystemView();
		mActivity.setTitle(R.string.app_name);
		mGp.pictureViewBottomControl.setVisibility(LinearLayout.VISIBLE);
		mGp.pictureViewTopControl.setVisibility(TextView.INVISIBLE);
	};
	
	public void setBackLightLevelToDefault() {
//		Thread.currentThread().dumpStack();
		LayoutParams lp = new LayoutParams(); 
		lp=getWindow().getAttributes();
		lp.screenBrightness = mDefaultBackLightLevel;
		getWindow().setAttributes(lp); 
	};
	 
	public void setBackLightLevelToMax() {
		if (mGp.settingMaxBrightWhenImageShowed) {
			mDefaultBackLightLevel=getWindow().getAttributes().screenBrightness;
//			Thread.currentThread().dumpStack();
			LayoutParams lp = new LayoutParams(); 
			lp=getWindow().getAttributes();
			lp.screenBrightness = 1.0f;
			getWindow().setAttributes(lp); 
		}
	};

    private void setContextButtonEnabled(final ImageButton btn, boolean enabled) {
    	mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered, enabled="+enabled);
    	if (enabled) {
        	btn.postDelayed(new Runnable(){
    			@Override
    			public void run() {
    				btn.setEnabled(true);
    			}
        	}, 1000);
    	} else {
    		btn.setEnabled(false);
    	}
    };

    private boolean isSelectedFolderIsExternalSdcard() {
    	boolean result=false;
    	for(FolderListItem fli:mGp.adapterFolderView.getFolderList()) {
    		if (fli.isSelected() && !fli.getParentDirectory().startsWith(mGp.internalRootDirectory)) {
    			result=true;
    			break;
    		}
    	}
    	return result;
    };
    
    private void setFolderViewContextButtonVisibility() {
    	boolean sdcard_usable=true;
		if (mGp.safMgr.getSdcardSafFile()==null) sdcard_usable=false;
    	
    	if (mGp.adapterFolderView.getCount()>0) {
        	if (mGp.adapterFolderView.getSelectedItemCount()>0) {
        		mGp.contextButtonFolderMediaFileScanView.setVisibility(LinearLayout.VISIBLE);
        		mGp.contextButtonFolderAddView.setVisibility(LinearLayout.INVISIBLE);
        		if (mGp.adapterFolderView.getSelectedItemCount()==1) {
        			if ((isSelectedFolderIsExternalSdcard() && sdcard_usable) || 
        					!isSelectedFolderIsExternalSdcard()) {
        				mGp.contextButtonFolderRenameView.setVisibility(LinearLayout.VISIBLE);
        				mGp.contextButtonFolderDeleteView.setVisibility(LinearLayout.VISIBLE);
        			} else {
        				mGp.contextButtonFolderRenameView.setVisibility(LinearLayout.INVISIBLE);
        				mGp.contextButtonFolderDeleteView.setVisibility(LinearLayout.INVISIBLE);
        			}
            	} else {
            		mGp.contextButtonFolderRenameView.setVisibility(LinearLayout.INVISIBLE);
        			if ((isSelectedFolderIsExternalSdcard() && sdcard_usable) || 
        					!isSelectedFolderIsExternalSdcard()) {
        				mGp.contextButtonFolderDeleteView.setVisibility(LinearLayout.VISIBLE);
        			} else {
        				mGp.contextButtonFolderDeleteView.setVisibility(LinearLayout.INVISIBLE);
        			}
            	}
        		if (mGp.adapterFolderView.isAllItemSelected()) mGp.contextButtonFolderSelectAllView.setVisibility(ImageButton.INVISIBLE);
        		else mGp.contextButtonFolderSelectAllView.setVisibility(ImageButton.VISIBLE);
        		if (mGp.adapterFolderView.getSelectedItemCount()==0) mGp.contextButtonFolderUnselectAllView.setVisibility(ImageButton.INVISIBLE);
        		else mGp.contextButtonFolderUnselectAllView.setVisibility(ImageButton.VISIBLE);
        		mGp.contextButtonFolderExcludeView.setVisibility(LinearLayout.VISIBLE);
        	} else {
        		mGp.contextButtonFolderMediaFileScanView.setVisibility(LinearLayout.INVISIBLE);
        		if (mGp.adapterFolderView.isSelectMode()) mGp.contextButtonFolderAddView.setVisibility(LinearLayout.INVISIBLE);
        		else mGp.contextButtonFolderAddView.setVisibility(LinearLayout.VISIBLE);
        		mGp.contextButtonFolderExcludeView.setVisibility(LinearLayout.INVISIBLE);
        		mGp.contextButtonFolderRenameView.setVisibility(LinearLayout.INVISIBLE);
        		mGp.contextButtonFolderDeleteView.setVisibility(LinearLayout.INVISIBLE);
        		mGp.contextButtonFolderSelectAllView.setVisibility(ImageButton.VISIBLE);
        		mGp.contextButtonFolderUnselectAllView.setVisibility(ImageButton.INVISIBLE);
        	}
    	} else {
    		mGp.contextButtonFolderAddView.setVisibility(LinearLayout.INVISIBLE);
    		mGp.contextButtonFolderMediaFileScanView.setVisibility(LinearLayout.INVISIBLE);
    		mGp.contextButtonFolderExcludeView.setVisibility(LinearLayout.INVISIBLE);
    		mGp.contextButtonFolderRenameView.setVisibility(LinearLayout.INVISIBLE);
    		mGp.contextButtonFolderDeleteView.setVisibility(LinearLayout.INVISIBLE);
    		mGp.contextButtonFolderSelectAllView.setVisibility(ImageButton.INVISIBLE);
    		mGp.contextButtonFolderUnselectAllView.setVisibility(ImageButton.INVISIBLE);
    	}
    	setFolderViewTitle();
//    	contextButtonFolderAddView.setVisibility(LinearLayout.GONE);
    };

    private void confirmFolderRename() {
		final Dialog dialog = new Dialog(mActivity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.single_item_input_dlg);
		final LinearLayout dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
		dlg_view.setBackgroundResource(R.drawable.dialog_border_dark);
		
		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		final TextView dlg_title = (TextView) dialog.findViewById(R.id.single_item_input_title);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		dlg_title.setText(mContext.getString(R.string.msgs_main_file_rename_title));
		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
		final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
		final Button btnOk = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
		final Button btnCancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
		final EditText etDir=(EditText) dialog.findViewById(R.id.single_item_input_dir);
		dlg_cmp.setVisibility(TextView.GONE);
		
		FolderListItem w_fli=null;
		for(int i=0;i<mGp.adapterFolderView.getCount();i++) {
			if (mGp.adapterFolderView.getItem(i).isSelected()) {
				w_fli=mGp.adapterFolderView.getItem(i);
				break;
			}
		}
		final FolderListItem fli=w_fli;
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		btnOk.setEnabled(false);
		etDir.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length()>0) {
					String n_path=fli.getParentDirectory().replace(fli.getFolderName(), "")+s.toString();
					File lf=new File(n_path);
//					Log.v("","fp="+lf.getPath());
					if (lf.exists()) {
						btnOk.setEnabled(false);
						dlg_msg.setText(mContext.getString(R.string.msgs_single_item_input_dlg_duplicate_dir));
					} else {
						btnOk.setEnabled(true);
						dlg_msg.setText("");
					}
				}
			}
		});
		etDir.setText(fli.getFolderName()); 
		
		//OK button
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final String new_name=fli.getParentDirectory().replace(fli.getFolderName(), "")+etDir.getText().toString();
				final String current_name=fli.getParentDirectory();
//				NotifyEvent 
//				Log.v("","new name="+new_name+", current name="+current_name);
				final ThreadCtrl tc=new ThreadCtrl();
				NotifyEvent ntfy_cancel=new NotifyEvent(mContext);
				ntfy_cancel.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						tc.setDisabled();
					}
				});
				final ProgressSpinDialogFragment psd=ProgressSpinDialogFragment.newInstance(
						mContext.getString(R.string.msgs_main_file_rename_title),
						mContext.getString(R.string.msgs_main_build_file_list_process_msg),
						mContext.getString(R.string.msgs_main_build_file_list_process_cancel),
						mContext.getString(R.string.msgs_main_build_file_list_process_canceling));
				psd.showDialog(mFragmentManager, psd, ntfy_cancel, false);

				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						Thread th=new Thread(){
							@Override
							public void run(){
								boolean rc_rename=false;
								final ArrayList<File>fl=new ArrayList<File>();
								PictureUtil.getAllPictureFileInDirectory(mGp, fl, new File(current_name), true); 
								if (current_name.startsWith(mGp.safMgr.getExternalSdcardPath())) {
									SafFile sf=mGp.safMgr.getSafFileBySdcardPath(mGp.safMgr.getSdcardSafFile(), current_name, false);
									sf.renameTo(etDir.getText().toString());
									rc_rename=sf.exists();
								} else {
									File n_file=new File(new_name);
									File c_file=new File(current_name);
									rc_rename=c_file.renameTo(n_file);
								}
								if (!rc_rename) {
									dlg_msg.setText(String.format(
											mContext.getString(R.string.msgs_main_file_rename_failed),etDir.getText()));
								} else {
									psd.updateMsgText(mContext.getString(R.string.msgs_main_file_update_media_store_information));
									for(File item:fl) {
										FileIo.deleteMediaStoreItem(mContext, item.getAbsolutePath() );
										PictureUtil.removeBitmapCacheFile(mGp, item.getAbsolutePath());										
//										scanMediaFile(item.getAbsolutePath());
//										psd.updateMsgText("MediaStore Item removed :"+item.getAbsolutePath());
									}
									fl.clear();
									PictureUtil.getAllPictureFileInDirectory(mGp, fl, new File(new_name), true);
									for(File item:fl) {
										scanMediaFile(item.getAbsolutePath());
//										psd.updateMsgText("MediaStore scan request issued :"+item.getAbsolutePath());
									}
									mCommonDlg.showCommonDialog(false, "I", 
											String.format(mContext.getString(R.string.msgs_main_file_rename_completed), new_name), "", null);
									addScanFolderItem(new_name, true, true);
								}
								mUiHandler.post(new Runnable(){
									@Override
									public void run() {
										mGp.adapterFolderView.setAllItemsSelected(false);
										mGp.adapterFolderView.setSelectMode(false);
										setFolderViewContextButtonVisibility();
										psd.dismissAllowingStateLoss();
										buildFolderList();
									}
								});
							}
						};
						th.setPriority(Thread.MIN_PRIORITY);
						th.start();
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						psd.dismissAllowingStateLoss();
					}
				});
				mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_main_file_rename_confirm_title), 
						current_name, ntfy);
				dialog.dismiss();
			}
		});
		
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog.show();

    };

    private void confirmFolderDelete() {
    	NotifyEvent ntfy=new NotifyEvent(mContext);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				final ThreadCtrl tc=new ThreadCtrl();
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						tc.setDisabled();
					}
				});
				final ProgressSpinDialogFragment psd=ProgressSpinDialogFragment.newInstance(
						mContext.getString(R.string.msgs_main_file_delete_dialog_title),
						mContext.getString(R.string.msgs_main_build_file_list_process_msg),
						mContext.getString(R.string.msgs_main_build_file_list_process_cancel),
						mContext.getString(R.string.msgs_main_build_file_list_process_canceling));
				psd.showDialog(mFragmentManager, psd, ntfy, false);
				final Handler hndl=new Handler();
				Thread th=new Thread() {
					@Override
					public void run() {
				    	String del_list="", sep="";
						boolean rc_delete=false;
						ArrayList<FolderListItem>folder_del_list=new ArrayList<FolderListItem>();
						for(int i=0;i<mGp.adapterFolderView.getCount();i++) {
							if (!tc.isEnabled()) break;
							if (mGp.adapterFolderView.getItem(i).isSelected()) {
								FolderListItem fli=mGp.adapterFolderView.getItem(i);
								ArrayList<PictureListItem>pic_list=getPictureList(fli);
								for(PictureListItem pli:pic_list) {
									if (!tc.isEnabled()) break;
									psd.updateMsgText(pli.getParentDirectory()+"/"+pli.getFileName());
									if (pli.getParentDirectory().startsWith(mGp.safMgr.getExternalSdcardPath())) {
										SafFile sf=mGp.safMgr.getSafFileBySdcardPath(mGp.safMgr.getSdcardSafFile(), 
												pli.getParentDirectory()+"/"+pli.getFileName(), false);
										rc_delete=sf.delete();
									} else {
										File lf=new File(pli.getParentDirectory()+"/"+pli.getFileName());
										rc_delete=lf.delete();
									}
									PictureUtil.removeBitmapCacheFile(mGp, pli.getParentDirectory()+"/"+pli.getFileName());
//									Log.v("","del="+cf.getAbsolutePath());
									if (!rc_delete) {
										mCommonDlg.showCommonDialog(false, "I", 
												String.format(mContext.getString(R.string.msgs_main_file_delete_file_was_failed),pli.getFileName()),
												"", null);
										break;
									} else {
										FileIo.deleteMediaStoreItem(mContext, pli.getParentDirectory()+"/"+pli.getFileName());
									}
								}
								removePictureList(fli);
								folder_del_list.add(fli);
								del_list+=sep+fli.getParentDirectory();
								sep=", ";
							}
						}
						synchronized(mGp.masterFolderList) {
							mGp.masterFolderList.removeAll(folder_del_list);
							saveFolderList(mGp.masterFolderList);
						}
						psd.dismissAllowingStateLoss();
						hndl.post(new Runnable(){
							@Override
							public void run() {
								mGp.adapterFolderView.setAllItemsSelected(false);
								mGp.adapterFolderView.setSelectMode(false);
								if (mGp.spinnerFolderSelector!=null) createFolderShowedList((String)mGp.spinnerFolderSelector.getSelectedItem());
								else createFolderShowedList(null);
//								mGp.mAdapterFolderView.setFolderList(mGp.masterFolderList);
								mGp.adapterFolderView.notifyDataSetChanged();
								setFolderViewContextButtonVisibility();
//								buildFolderList();
							}
						});
						if (tc.isEnabled()) {
							if (rc_delete) mCommonDlg.showCommonDialog(false, "I", 
									mContext.getString(R.string.msgs_main_file_delete_file_completed), del_list, null);
							
						} else {
							mCommonDlg.showCommonDialog(false, "I", 
									mContext.getString(R.string.msgs_main_file_delete_file_was_cancelled), "", null);
						}
					}
				};
				th.start();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
    	});
    	String del_list="", sep="";
    	for(int i=0;i<mGp.adapterFolderView.getCount();i++) {
    		if (mGp.adapterFolderView.getItem(i).isSelected()) {
    			del_list+=sep+mGp.adapterFolderView.getItem(i).getFolderName();
    			sep=", ";
    		}
    	}
    	mCommonDlg.showCommonDialog(true, "W", 
    			mContext.getString(R.string.msgs_main_file_delete_confirm_delete), del_list, ntfy);
    };

    private void confirmFolderScan() {
    	NotifyEvent ntfy=new NotifyEvent(mContext);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				final ThreadCtrl tc=new ThreadCtrl();
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						tc.setDisabled();
					}
				});
				final ProgressSpinDialogFragment psd=ProgressSpinDialogFragment.newInstance(
						mContext.getString(R.string.msgs_main_file_scan_dialog_title),
						mContext.getString(R.string.msgs_main_build_file_list_process_msg),
						mContext.getString(R.string.msgs_main_build_file_list_process_cancel),
						mContext.getString(R.string.msgs_main_build_file_list_process_canceling));
				psd.showDialog(mFragmentManager, psd, ntfy, false);
				Thread th=new Thread() {
					@Override
					public void run() {
						boolean rc_delete=false;
						for(int i=0;i<mGp.adapterFolderView.getCount();i++) {
							if (!tc.isEnabled()) break;
							if (mGp.adapterFolderView.getItem(i).isSelected()) {
								FolderListItem fli=mGp.adapterFolderView.getItem(i);
								File[] scan_list=(new File(fli.getParentDirectory())).listFiles();
								if (scan_list!=null && scan_list.length>0) {
									for(File sf:scan_list) {
										if (!tc.isEnabled()) break;
										scanMediaFile(sf.getAbsolutePath());
									}
								}
							}
						}
						psd.dismissAllowingStateLoss();
						if (tc.isEnabled()) {
							if (rc_delete) mCommonDlg.showCommonDialog(false, "I", 
									mContext.getString(R.string.msgs_main_file_scan_file_completed), "", null);
							
						} else {
							mCommonDlg.showCommonDialog(false, "I", 
									mContext.getString(R.string.msgs_main_file_scan_file_was_cancelled), "", null);
						}
					}
				};
				th.start();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
    	});
    	String del_list="", sep="";
    	for(int i=0;i<mGp.adapterFolderView.getCount();i++) {
    		if (mGp.adapterFolderView.getItem(i).isSelected()) {
    			del_list+=sep+mGp.adapterFolderView.getItem(i).getFolderName();
    			sep=", ";
    		}
    	}
    	mCommonDlg.showCommonDialog(true, "W", 
    			mContext.getString(R.string.msgs_main_file_scan_confirm_scan), del_list, ntfy);
    };

    
    private ScanFolderItem getScanFolderItem(String dir_path) {
    	ScanFolderItem result=null;
    	for(ScanFolderItem sfi:mGp.settingScanDirectoryList) {
    		if (sfi.folder_path.equals(dir_path)) {
    			result=sfi;
    			break;
    		}
    	}
    	return result;
    };
    
    private void addScanFolderItem(String folder_path, boolean process_sub_directories, boolean select_include) {
		ScanFolderItem sfi=new ScanFolderItem();
		sfi.folder_path=folder_path;
		sfi.process_sub_directories=process_sub_directories;
		sfi.include=select_include;
//		Log.v("","c="+getScanFolderItem(sfi.folder_path));
		if (getScanFolderItem(sfi.folder_path)!=null) 
			mGp.settingScanDirectoryList.remove(getScanFolderItem(sfi.folder_path));
		mGp.settingScanDirectoryList.add(sfi);
		sortScanFolderList();
    };

    private void sortScanFolderList() {
    	Collections.sort(mGp.settingScanDirectoryList, new Comparator<ScanFolderItem>(){
			@Override
			public int compare(ScanFolderItem lhs, ScanFolderItem rhs) {
				return lhs.folder_path.compareToIgnoreCase(rhs.folder_path);
			}
    	});
    };

    private void confirmFolderExclude() {
    	NotifyEvent ntfy=new NotifyEvent(mContext);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				ArrayList<FolderListItem>folder_del_list=new ArrayList<FolderListItem>();
		    	for(int i=0;i<mGp.adapterFolderView.getCount();i++) {
		    		if (mGp.adapterFolderView.getItem(i).isSelected()) {
		    			addScanFolderItem(mGp.adapterFolderView.getItem(i).getParentDirectory(), false, false);
		    			folder_del_list.add(mGp.adapterFolderView.getItem(i));
		    		}
		    	}
		    	mGp.saveScanFolderList(mContext);
		    	
				mGp.adapterFolderView.setAllItemsSelected(false);
				mGp.adapterFolderView.setSelectMode(false);
				setFolderViewContextButtonVisibility();

//		    	buildFolderList();
				synchronized(mGp.masterFolderList) {
					mGp.masterFolderList.removeAll(folder_del_list);
					for(FolderListItem fli:folder_del_list) {
						removePictureList(fli);
					}
					if (mGp.spinnerFolderSelector!=null) createFolderShowedList((String)mGp.spinnerFolderSelector.getSelectedItem());
					else createFolderShowedList(null);
					mGp.adapterFolderView.notifyDataSetChanged();
					saveFolderList(mGp.masterFolderList);
				}
				
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
    	});
    	String exc_list="", sep="";
    	for(int i=0;i<mGp.adapterFolderView.getCount();i++) {
    		if (mGp.adapterFolderView.getItem(i).isSelected()) {
    			exc_list+=sep+mGp.adapterFolderView.getItem(i).getFolderName();
    			sep=", ";
    		}
    	}
    	mCommonDlg.showCommonDialog(true, "W", 
    			mContext.getString(R.string.msgs_main_exclude_folder_confirm), exc_list, ntfy);
    };

    private void addFolder() {
    	NotifyEvent ntfy=new NotifyEvent(mContext);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, final Object[] o) {
				final Dialog dialog=showProgressSpinIndicator();
				Thread th=new Thread(){
					@Override
					public void run(){
						String w_dir=(String)o[0];
						final String dir=w_dir.substring(0, w_dir.length()-1);
						boolean add_required=true, abort=false;
						for(ScanFolderItem sfi:mGp.settingScanDirectoryList) {
				    		if (dir.startsWith(sfi.folder_path)) {
				    			if (sfi.include && sfi.process_sub_directories) {
				    				add_required=false;
//					    			break;
				    			} else if (!sfi.include && sfi.process_sub_directories) {
				    				add_required=false;
				    				abort=true;
				    				mCommonDlg.showCommonDialog(false, "W", 
				    						mContext.getString(R.string.msgs_main_edit_scan_folder_ignored_folder), dir, null);
				    				break;
				    			} else if (!sfi.include && !sfi.process_sub_directories) {
				    				add_required=true;
				    			}
				    		}
				    	}
						if (!abort) {
							if (add_required) {
								addScanFolderItem(dir, false, true);
								mGp.saveScanFolderList(mContext);
							}
							
							final FolderListItem n_fli=new FolderListItem();
							n_fli.setFolderName(dir.substring(dir.lastIndexOf("/")+1));
							n_fli.setFileLastModified(new File(dir).lastModified());
							n_fli.setParentDirectory(dir);
							n_fli.setSortKey(SORT_KEY_THUMBNAIL_PICTURE_TIME);
							n_fli.setSortOrder(SORT_ORDER_DESCENDANT);
							mGp.masterFolderList.add(n_fli);
							AdapterFolderList.sort(mGp.masterFolderList, mGp.folderListSortKey, mGp.folderListSortOrder);
							mUtil.addDebugMsg(1, "I", "addFolder new folder added, Name="+n_fli.getFolderName());
							
							mUiHandler.post(new Runnable(){
								@Override
								public void run() {
									if (mGp.spinnerFolderSelector!=null) createFolderShowedList((String)mGp.spinnerFolderSelector.getSelectedItem());
									else createFolderShowedList(null);
									mUiHandler.post(new Runnable(){
										@Override
										public void run() {
											for(int i=0;i<mGp.showedFolderList.size();i++) {
												FolderListItem fli=mGp.showedFolderList.get(i);
												if (fli.getParentDirectory().equals(dir)) {
													final int pos=i;
													mUiHandler.post(new Runnable(){
														@Override
														public void run() {
															mGp.folderGridView.setSelection(pos);
														}
													});
													break;
												}
											}
											mGp.adapterFolderView.notifyDataSetChanged();
										}
									});
								}
							});
							final ArrayList<PictureListItem>n_pl=new ArrayList<PictureListItem>();
							updateCurrentFolder(n_fli, n_pl);
							mUiHandler.post(new Runnable(){
								@Override
								public void run() {
									mGp.adapterThumbnailView.setPictureList(n_pl);
							   		createPictureShowedList((String)mGp.spinnerPictureSelector.getSelectedItem());
									mGp.adapterFolderView.notifyDataSetChanged();
									dialog.dismiss();
								}
							});
						} else {
							dialog.dismiss();
						}
					}
				};
				th.start();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
    	});
		FileSelectDialogFragment fsdf=FileSelectDialogFragment.newInstance(
//				false, true, false, false, true, false, true, true, 
				mGp.internalRootDirectory, "", "", 
				mContext.getString(R.string.msgs_main_folder_list_add_title));
		fsdf.setOptionDebug(false);
		fsdf.setOptionEnableCreate(true);
		fsdf.setOptionFileOnly(false);
		fsdf.setOptionDirectoryOnly(true);
		fsdf.setOptionHideMountPoint(false);
		fsdf.setOptionIncludeRoot(false);
		fsdf.setOptionSingleSelect(true);
		fsdf.setOptionLimitMountPoint(true);
		fsdf.setOptionSelectedFilePathWithMountPoint(true);
		fsdf.setHideHiddenDirsFiles(!mGp.settingScanHiddenFile);
		fsdf.showDialog(mFragmentManager, fsdf, ntfy);
    };
    
	private void setFolderViewListener() {
		setFolderViewContextButtonVisibility();
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				setFolderViewContextButtonVisibility();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		mGp.adapterFolderView.setCheckedChangeListener(ntfy);

		mGp.contextButtonFolderExclude.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					confirmFolderExclude();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonFolderExclude, mContext.getString(R.string.msgs_main_cont_label_exclude));

        mGp.contextButtonFolderMediaFileScan.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					confirmFolderScan();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonFolderMediaFileScan, mContext.getString(R.string.msgs_main_cont_label_media_file_scan));
        
        mGp.contextButtonFolderAdd.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					addFolder();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonFolderAdd, mContext.getString(R.string.msgs_main_cont_label_add));

        mGp.contextButtonFolderRename.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					confirmFolderRename();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonFolderRename, mContext.getString(R.string.msgs_main_cont_label_rename));

        mGp.contextButtonFolderDelete.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					confirmFolderDelete();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonFolderDelete, mContext.getString(R.string.msgs_main_cont_label_delete));

        mGp.contextButtonFolderSelectAll.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					mGp.adapterFolderView.setAllItemsSelected(true);
					mGp.adapterFolderView.setSelectMode(true);
					mGp.adapterFolderView.notifyDataSetChanged();
					setFolderViewContextButtonVisibility();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonFolderSelectAll, mContext.getString(R.string.msgs_main_cont_label_select_all));

        mGp.contextButtonFolderUnselectAll.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					mGp.adapterFolderView.setAllItemsSelected(false);
					mGp.adapterFolderView.notifyDataSetChanged();
					setFolderViewContextButtonVisibility();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonFolderUnselectAll, mContext.getString(R.string.msgs_main_cont_label_unselect_all));

		mGp.folderGridView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (mGp.adapterFolderView.isSelectMode()) {
					mGp.adapterFolderView.getItem(position).setSelected(!mGp.adapterFolderView.getItem(position).isSelected());
					mGp.adapterFolderView.notifyDataSetChanged();
				} else {
//					mFolderListPosition=position;
					if (mGp.showedFolderList.get(position)!=null
//							&& mGp.selectFolderList.get(position).getThumbnailArray()!=null
							)
						showThumbnailView(mGp.showedFolderList.get(position));
				}
			}
		});
		mGp.folderGridView.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				mGp.adapterFolderView.setSelectMode(true);
				mGp.adapterFolderView.getItem(position).setSelected(true);
				mGp.adapterFolderView.notifyDataSetChanged();
				return true;
			}
		});
		
	};

	private void createPictureShowedList(String sel_month) {
		synchronized(mGp.showedPictureList) {
	    	mGp.showedPictureList.clear();
	    	if (sel_month!=null && !sel_month.equals(mContext.getString(R.string.msgs_main_thumbnail_view_date_selector_all_picture))) {
	    		String n_key=sel_month;
	    		if (sel_month.equals(mContext.getString(R.string.msgs_main_picture_file_date_time_unknown))) n_key="0000/00";
	    		for(PictureListItem pli:mGp.currentPictureList) {
	    			if (pli.getExifDateTime().startsWith(n_key)) mGp.showedPictureList.add(pli);
	    		}
	    		mGp.adapterThumbnailView.setPictureList(mGp.showedPictureList);
	    		mGp.adapterThumbnailView.notifyDataSetChanged();
	        	setFolderViewContextButtonVisibility();
	    	} else {
	    		if (mGp.currentPictureList!=null) mGp.showedPictureList.addAll(mGp.currentPictureList);
	    		mGp.adapterThumbnailView.setPictureList(mGp.showedPictureList);
	    		mGp.adapterThumbnailView.notifyDataSetChanged();
	        	setFolderViewContextButtonVisibility();
	    	}
		}
		if (mGp.showedPictureList.size()>0) setThumbnailViewTitle(mGp.showedPictureList.get(0).getFolderName());
    };

	private void createFolderShowedList(String sel_key) {
    	synchronized(mGp.showedFolderList) {
        	mGp.showedFolderList.clear();
        	if (sel_key!=null && !sel_key.equals(mContext.getString(R.string.msgs_main_folder_view_folder_selector_all_folder))) {
            	for(FolderListItem fli:mGp.masterFolderList) {
            		if (fli.getFolderName().startsWith(sel_key)) {
            			mGp.showedFolderList.add(fli);	
            		}
            	}
            	setFolderViewContextButtonVisibility();
        	} else {
        		if (mGp.masterFolderList!=null) mGp.showedFolderList.addAll(mGp.masterFolderList);
    			setFolderViewContextButtonVisibility();
        	}
        	mGp.adapterFolderView.setFolderList(mGp.showedFolderList);
        	mGp.adapterFolderView.notifyDataSetChanged();
        	setFolderViewTitle();
//        	mGp.mAdapterFolderView.sort();
    	}
    };

	private void showThumbnailView(final FolderListItem fli) {
		final ArrayList<PictureListItem>pl=getPictureList(fli);
		mGp.currentFolderListItem=fli;
		mGp.currentView=CURRENT_VIEW_THUMBNAIL;
		mGp.selectPictureDateSpinnerPosition=0;
		mGp.currentPictureList=pl;
		mActionBar.setDisplayHomeAsUpEnabled(true);
		if (pl.size()>0) {
			
			mGp.adapterThumbnailView.setSortKey(mGp.currentFolderListItem.getSortKey());
			mGp.adapterThumbnailView.setSortOrder(mGp.currentFolderListItem.getSortOrder());
			
			createPictureShowedList(null);
			
			createThumbnailPictureInfo(fli, mGp.showedPictureList);
			
			mGp.folderView.setVisibility(LinearLayout.GONE);
			mGp.thumbnailView.setVisibility(LinearLayout.VISIBLE);
			mGp.thumbnailGridView.setVisibility(GridView.VISIBLE);
			mGp.thumbnailEmptyView.setVisibility(TextView.GONE);
			mGp.pictureView.setVisibility(LinearLayout.GONE);
			setBackLightLevelToDefault();
			refreshOptionMenu();
			setUiActionBar();

			mGp.adapterThumbnailView.setSelectMode(false);
			mGp.thumbnailGridView.postDelayed(new Runnable(){
				@Override
				public void run() {
					setThumbnailViewContextButtonVisibility();
					mGp.thumbnailGridView.setSelection(0);
				}
			},200);

			mCurrentThumbnailViewTitle=fli.getFolderName();
			setThumbnailViewTitle(mCurrentThumbnailViewTitle);
		} else {
			mGp.folderView.setVisibility(LinearLayout.GONE);
			mGp.thumbnailView.setVisibility(LinearLayout.VISIBLE);
			mGp.thumbnailGridView.setVisibility(GridView.GONE);
			mGp.thumbnailEmptyView.setVisibility(TextView.VISIBLE);
			mGp.pictureView.setVisibility(LinearLayout.GONE);
			
			setBackLightLevelToDefault();
			refreshOptionMenu();
			setUiActionBar();
			
			mGp.thumbnailGridView.postDelayed(new Runnable(){
				@Override
				public void run() {
					setThumbnailViewContextButtonVisibility();
				}
			},200);

			mCurrentThumbnailViewTitle=fli.getFolderName();
			setThumbnailViewTitle(mCurrentThumbnailViewTitle);
		}
		resetDeviceOrientation();
	};

	private void createThumbnailPictureInfo(final FolderListItem fli, ArrayList<PictureListItem>pl) {
		boolean create_required=false;
		for(PictureListItem pli:pl) {
			if (pli.getThumbnailImageByte()==null) {
				create_required=true;
				break;
			}
		}
		if (!create_required) return;
		mTcCreatePictureCacheFile.setEnabled();
		mGp.adapterThumbnailView.setAllItemsEnabled(false);
		final ArrayList<PictureListItem> t_pl=new ArrayList<PictureListItem>();
		t_pl.addAll(pl);
		showProgressBar();
		mGp.mainProgressBar.setProgress(0);
		Thread th=new Thread() {
			@Override
			public void run() {
				setName(fli.getFolderName());
//				int cnt=0;
				boolean save_required=false;
				int proc_cnt=0;
				for(PictureListItem pli:t_pl) {
					if (!mTcCreatePictureCacheFile.isEnabled()) {
						break;
					}
					proc_cnt++;
					String fp=pli.getParentDirectory()+"/"+pli.getFileName();
					if (pli.getThumbnailImageByte()==null) {
						pli.createExifInfo(fp);
						save_required=true;
						final int progress=(proc_cnt*100)/t_pl.size();
						mUiHandler.postDelayed(new Runnable(){
							@Override
							public void run() {
								if (mTcCreatePictureCacheFile.isEnabled()) {
									mGp.adapterThumbnailView.notifyDataSetChanged();
									mGp.mainProgressBar.setProgress(progress);
								}
							}
						},100);
//						cnt++;
//						if (cnt>10) {
//							cnt=0;
//						}
					}
					pli.setEnabled(true);
				}
				mGp.adapterThumbnailView.setAllItemsEnabled(true);
				final boolean w_save_required=save_required;
				mTcCreatePictureCacheFile.setDisabled();
				mUiHandler.post(new Runnable(){
					@Override
					public void run() {
						if (w_save_required) {
							mGp.adapterThumbnailView.sort();
							putPictureList(t_pl, fli);
						}
						mGp.adapterThumbnailView.notifyDataSetChanged();
						hideProgressBar();
					}
				});
				refreshOptionMenu();
			}
		};
		th.setPriority(Thread.MAX_PRIORITY);
		th.start();
	};
	
	public void resetDeviceOrientation() {
		mGp.pictureScreenRotationLocked=false;
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);//.SCREEN_ORIENTATION_UNSPECIFIED);
	};

	private void reshowThumbnailView(final int pic_pos) {
		mGp.currentView=CURRENT_VIEW_THUMBNAIL;
		mGp.folderView.setVisibility(LinearLayout.GONE);
		mGp.thumbnailView.setVisibility(LinearLayout.VISIBLE);
		mGp.pictureView.setVisibility(LinearLayout.GONE);
		mGp.adapterThumbnailView.setSelectMode(false);
		mGp.mainProgressBar.setVisibility(ProgressBar.VISIBLE);
		setBackLightLevelToDefault();
		refreshOptionMenu();
		setUiActionBar();
		setThumbnailViewTitle(mCurrentThumbnailViewTitle);
		resetDeviceOrientation();
		mUiHandler.post(new Runnable(){
			@Override
			public void run() {
				mGp.thumbnailGridView.setSelection(pic_pos);
				Runtime.getRuntime().gc();
			}
		});
	};

	private void setFolderViewTitle() {
		if (mGp.currentView==CURRENT_VIEW_FOLDER) {
//			Thread.dumpStack();
//			Log.v("","sel mode="+mGp.mAdapterFolderView.isSelectMode());
			if (mGp.adapterFolderView.isSelectMode()) {
				mActivity.setTitle(mGp.adapterFolderView.getSelectedItemCount()+"/"+mGp.adapterFolderView.getCount());
				mActionBar.setDisplayHomeAsUpEnabled(true);
			} else {
				mActivity.setTitle(String.format(mContext.getString(R.string.msgs_main_folder_view_title),mGp.showedFolderList.size()));
				mActionBar.setDisplayHomeAsUpEnabled(false);
			}
		}
	};
	
	public void showFolderView() {
		mUtil.addDebugMsg(1, "I", "showFolderView entered, mGp.mCurrentView="+mGp.currentView);
		mGp.currentView=CURRENT_VIEW_FOLDER;
		mActionBar.setDisplayHomeAsUpEnabled(false);
		mGp.folderView.setVisibility(LinearLayout.VISIBLE);
		mGp.thumbnailView.setVisibility(LinearLayout.GONE);
		mGp.pictureView.setVisibility(LinearLayout.GONE);
		setBackLightLevelToDefault();
		refreshOptionMenu();
		setUiActionBar();

		createFolderSelectionList();

		if (mGp.spinnerFolderSelector!=null) createFolderShowedList((String)mGp.spinnerFolderSelector.getSelectedItem());
		else createFolderShowedList(null);

		setFolderViewTitle();
		resetDeviceOrientation();
//		setFolderViewFolderSelector();
	};

	private void setThumbnailViewTitle(String folder_name) {
//		Thread.dumpStack();
		if (mGp.currentView==CURRENT_VIEW_THUMBNAIL) {
			if (mGp.adapterThumbnailView.isSelectMode()) {
				mActivity.setTitle(mGp.adapterThumbnailView.getSelectedItemCount()+"/"+mGp.adapterThumbnailView.getCount());
			} else {
				mActivity.setTitle(folder_name);//+"("+mGp.mSelectedPictureList.size()+")");
			}
		}
	};
	
	private boolean isPasteEnabled() {
		boolean result=false;
		if (mGp.copyCutList.size()>0) {
			String f_pd=mGp.copyCutList.get(0).substring(0,mGp.copyCutList.get(0).lastIndexOf("/"));
			if (!mGp.currentFolderListItem.getParentDirectory().equals(f_pd)) {
				result=true;
			}
		}
		return result;
	};
	
	private void pasteItem() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				final ThreadCtrl tc=new ThreadCtrl();
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						tc.setDisabled();
					}
				});
				String title="";
				if (mGp.isCutMode) title=mContext.getString(R.string.msgs_main_file_move_title);
				else title=mContext.getString(R.string.msgs_main_file_copy_title);
				final ProgressSpinDialogFragment psdf=ProgressSpinDialogFragment.newInstance(
						title, title, 
						mContext.getString(R.string.msgs_main_file_cancel_btn),
						mContext.getString(R.string.msgs_main_file_canceling_btn));
				psdf.showDialog(getSupportFragmentManager(), psdf, ntfy, true);
				Thread th=new Thread(){
					@Override
					public void run() {
						mUtil.addLogMsg("I", "PastItem started, mGp.mCutMode="+mGp.isCutMode);
						ArrayList<String>ccl=new ArrayList<String>();
						ccl.addAll(mGp.copyCutList);
						String pd=mGp.currentFolderListItem.getParentDirectory();
						if (pd.startsWith(mGp.externalRootDirectory)) {//SDCARD
							if (ccl.get(0).startsWith(mGp.internalRootDirectory)) {
								//Internal to Internal
								if (mGp.isCutMode) {
									FileIo.moveInternalToExternal(mGp, mUtil, psdf, tc, ccl, pd);
								} else {
									FileIo.copyInternalToExternal(mGp, mUtil, psdf, tc, ccl, pd);
								}
							} else {
								if (mGp.isCutMode) {
									FileIo.moveExternalToExternal(mGp, mUtil, psdf, tc, ccl, pd);
								} else {
									FileIo.copyExternalToExternal(mGp, mUtil, psdf, tc, ccl, pd);
								}
							}
						} else {//To Internal storage
							if (ccl.get(0).startsWith(mGp.internalRootDirectory)) {
								//Internal to Internal
								if (mGp.isCutMode) {
									FileIo.moveInternalToInternal(mGp, mUtil, psdf, tc, ccl, pd);
								} else {
									FileIo.copyInternalToInternal(mGp, mUtil, psdf, tc, ccl, pd);
								}
							} else {
								if (mGp.isCutMode) {
									FileIo.moveExternalToInternal(mGp, mUtil, psdf, tc, ccl, pd);
								} else {
									FileIo.copyExternalToInternal(mGp, mUtil, psdf, tc, ccl, pd);
								}
							}
						}
						
						if (mGp.isCutMode) {
							String f_dir=ccl.get(0).substring(0,ccl.get(0).lastIndexOf("/"));
							FolderListItem f_fli=getFolderListItem(f_dir);
							ArrayList<PictureListItem>pic_list=getPictureList(f_fli);
							updateSpecificFolder(f_fli, pic_list);
						}
						mGp.copyCutList.clear();
						psdf.dismissAllowingStateLoss();
						
						updateCurrentFolder(mGp.currentFolderListItem, mGp.currentPictureList);

						mUiHandler.post(new Runnable(){
							@Override
							public void run() {
								mGp.adapterThumbnailView.setPictureList(mGp.currentPictureList);
								if (mGp.currentPictureList.size()>0) {
									mGp.thumbnailGridView.setVisibility(GridView.VISIBLE);
									mGp.thumbnailEmptyView.setVisibility(TextView.GONE);
								}
								setClipboardBtn("");
								int c_fc=mGp.currentFolderListItem.getNoOfPictures();
								setThumbnailViewContextButtonVisibility();
								createPictureShowedList((String)mGp.spinnerPictureSelector.getSelectedItem());
								mGp.adapterFolderView.notifyDataSetChanged();
								if (c_fc==0) showThumbnailView(mGp.currentFolderListItem); 
//								mShowProgressBar=true;
//								buildFolderList();
							}
						});
					}
				};
				th.start();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				
			}
		});
		String item_list="", sep="";
		for(String item:mGp.copyCutList) {
			item_list+=sep+item.substring(item.lastIndexOf("/")+1);
			sep=", ";
			if (item_list.length()>=1024) {
				item_list+="\n...";
				break;
			}
		}
		if (mGp.isCutMode) {
			mCommonDlg.showCommonDialog(true, "W", 
				mContext.getString(R.string.msgs_main_file_move_file_confirm_title), item_list, ntfy);
		} else {
			mCommonDlg.showCommonDialog(true, "W", 
					mContext.getString(R.string.msgs_main_file_copy_file_confirm_title), item_list, ntfy);
		}
	};
	
	private void setClipboardBtn(String folder_name) {
		String item_list="", sep="";
		for(String item:mGp.copyCutList) {
			item_list+=sep+item.substring(item.lastIndexOf("/")+1);
			sep=", ";
			if (item_list.length()>=256) {
				break;
			}
		}
		if (item_list.length()==0) mGp.contextClipBoardBtn.setText("");
		else {
			if (mGp.isCutMode) mGp.contextClipBoardBtn.setText(mContext.getString(R.string.msgs_main_cont_label_cut)+" "+folder_name+":"+item_list);
			else mGp.contextClipBoardBtn.setText(mContext.getString(R.string.msgs_main_cont_label_copy)+" "+folder_name+":"+item_list);
		}
	};
	
	private void copyItem() {
		mGp.copyCutList.clear();
		mGp.isCutMode=false;
		for(PictureListItem pli:mGp.adapterThumbnailView.getPictureList()) {
			if (pli.isSelected()) {
				mGp.copyCutList.add(pli.getParentDirectory()+"/"+pli.getFileName());
			}
		}
		mGp.adapterThumbnailView.setAllItemsEnabled(true);
		mGp.adapterThumbnailView.setAllItemsSelected(false);
		mGp.adapterThumbnailView.setSelectMode(false);
		mGp.adapterThumbnailView.notifyDataSetChanged();
		setClipboardBtn(mGp.adapterThumbnailView.getPictureList().get(0).getFolderName());
	};

	private void cutItem() {
		mGp.copyCutList.clear();
		mGp.isCutMode=true;
		for(PictureListItem pli:mGp.adapterThumbnailView.getPictureList()) {
			if (pli.isSelected()) {
				mGp.copyCutList.add(pli.getParentDirectory()+"/"+pli.getFileName());
			}
		}
		mGp.adapterThumbnailView.setAllItemsEnabled(true);
		mGp.adapterThumbnailView.setAllItemsSelected(false);
		mGp.adapterThumbnailView.setSelectMode(false);
		mGp.adapterThumbnailView.notifyDataSetChanged();
		setClipboardBtn(mGp.adapterThumbnailView.getPictureList().get(0).getFolderName());
	};
	
    private void setThumbnailViewContextButtonVisibility() {
    	if (isPasteEnabled()) {
    		if (!mGp.adapterThumbnailView.isSelectMode()) mGp.contextButtonThumbnailPasteView.setVisibility(LinearLayout.VISIBLE);
    		else mGp.contextButtonThumbnailPasteView.setVisibility(LinearLayout.INVISIBLE);
    	} else {
    		mGp.contextButtonThumbnailPasteView.setVisibility(LinearLayout.INVISIBLE);
    	}
    	if (mGp.copyCutList.size()==0) mGp.contextClipBoardBtn.setVisibility(Button.GONE);
    	else mGp.contextClipBoardBtn.setVisibility(Button.VISIBLE);
    	
    	boolean enable_file_mod=true;
    	if (mGp.currentFolderListItem!=null && 
    			!mGp.currentFolderListItem.getParentDirectory().startsWith(mGp.internalRootDirectory)) {
    		if (mGp.safMgr.getSdcardSafFile()==null) enable_file_mod=false;
    	}
    	
    	if (mGp.adapterThumbnailView.getCount()>0) {
        	if (mGp.adapterThumbnailView.getSelectedItemCount()>0) {
            	if (mGp.adapterThumbnailView.getSelectedItemCount()==1) {
            		mGp.contextButtonThumbnailShareView.setVisibility(LinearLayout.VISIBLE);
            		if (enable_file_mod)  {
            			mGp.contextButtonThumbnailRenameView.setVisibility(LinearLayout.VISIBLE);
            			mGp.contextButtonThumbnailDeleteView.setVisibility(LinearLayout.VISIBLE);
            			mGp.contextButtonThumbnailCutView.setVisibility(LinearLayout.VISIBLE);
            		} else {
            			mGp.contextButtonThumbnailRenameView.setVisibility(LinearLayout.INVISIBLE);
            			mGp.contextButtonThumbnailDeleteView.setVisibility(LinearLayout.INVISIBLE);
            			mGp.contextButtonThumbnailCutView.setVisibility(LinearLayout.INVISIBLE);
            		}
            		mGp.contextButtonThumbnailCopyView.setVisibility(LinearLayout.VISIBLE);
            	} else {
            		if (enable_file_mod)  {
                        mGp.contextButtonThumbnailRenameView.setVisibility(LinearLayout.VISIBLE);
            			mGp.contextButtonThumbnailDeleteView.setVisibility(LinearLayout.VISIBLE);
            			mGp.contextButtonThumbnailCutView.setVisibility(LinearLayout.VISIBLE);
            		} else {
                        mGp.contextButtonThumbnailRenameView.setVisibility(LinearLayout.INVISIBLE);
            			mGp.contextButtonThumbnailDeleteView.setVisibility(LinearLayout.INVISIBLE);
            			mGp.contextButtonThumbnailCutView.setVisibility(LinearLayout.INVISIBLE);
            		}
            		mGp.contextButtonThumbnailShareView.setVisibility(LinearLayout.VISIBLE);
            		mGp.contextButtonThumbnailCopyView.setVisibility(LinearLayout.VISIBLE);
            	}
        		if (mGp.adapterThumbnailView.isAllItemSelected()) mGp.contextButtonThumbnailSelectAllView.setVisibility(ImageButton.INVISIBLE);
        		else mGp.contextButtonThumbnailSelectAllView.setVisibility(ImageButton.VISIBLE);
        		if (mGp.adapterThumbnailView.getSelectedItemCount()==0) mGp.contextButtonThumbnailUnselectAllView.setVisibility(ImageButton.INVISIBLE);
        		else mGp.contextButtonThumbnailUnselectAllView.setVisibility(ImageButton.VISIBLE);
        	} else {
        		mGp.contextButtonThumbnailShareView.setVisibility(LinearLayout.INVISIBLE);
        		mGp.contextButtonThumbnailRenameView.setVisibility(LinearLayout.INVISIBLE);
        		mGp.contextButtonThumbnailCopyView.setVisibility(LinearLayout.INVISIBLE);
        		mGp.contextButtonThumbnailCutView.setVisibility(LinearLayout.INVISIBLE);
        		mGp.contextButtonThumbnailDeleteView.setVisibility(LinearLayout.INVISIBLE);
        		mGp.contextButtonThumbnailSelectAllView.setVisibility(ImageButton.VISIBLE);
        		mGp.contextButtonThumbnailUnselectAllView.setVisibility(ImageButton.INVISIBLE);
        	}
    	} else {
    		mGp.contextButtonThumbnailShareView.setVisibility(LinearLayout.INVISIBLE);
    		mGp.contextButtonThumbnailRenameView.setVisibility(LinearLayout.INVISIBLE);
    		mGp.contextButtonThumbnailCopyView.setVisibility(LinearLayout.INVISIBLE);
    		mGp.contextButtonThumbnailCutView.setVisibility(LinearLayout.INVISIBLE);
    		mGp.contextButtonThumbnailDeleteView.setVisibility(LinearLayout.INVISIBLE);
    		mGp.contextButtonThumbnailSelectAllView.setVisibility(ImageButton.INVISIBLE);
    		mGp.contextButtonThumbnailUnselectAllView.setVisibility(ImageButton.INVISIBLE);
    	}
    	setThumbnailViewTitle(mCurrentThumbnailViewTitle);
//   		contextButtonThumbnailPasteView.setVisibility(LinearLayout.GONE);
//   		contextButtonThumbnailCopyView.setVisibility(LinearLayout.GONE);
//   		contextButtonThumbnailCutView.setVisibility(LinearLayout.GONE);
//    	mContextClipBoardBtn.setVisibility(Button.GONE);

    };

    private void confirmSingleThumbnailViewRename() {
		final Dialog dialog = new Dialog(mActivity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.single_item_input_dlg);
		final LinearLayout dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
		dlg_view.setBackgroundResource(R.drawable.dialog_border_dark);
		
		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		final TextView dlg_title = (TextView) dialog.findViewById(R.id.single_item_input_title);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		dlg_title.setText(mContext.getString(R.string.msgs_main_file_rename_title));
		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
		final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
		final Button btnOk = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
		final Button btnCancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
		final EditText etDir=(EditText) dialog.findViewById(R.id.single_item_input_dir);
		dlg_cmp.setVisibility(TextView.GONE);
		
		PictureListItem w_pli=null;
		for(int i=0;i<mGp.adapterThumbnailView.getCount();i++) {
			if (mGp.adapterThumbnailView.getItem(i).isSelected()) {
				w_pli=mGp.adapterThumbnailView.getItem(i);
				break;
			}
		}
		final PictureListItem pli=w_pli;

		CommonDialog.setDlgBoxSizeCompact(dialog);
		btnOk.setEnabled(false);
		etDir.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length()>0) {
					String n_path=pli.getParentDirectory()+"/"+s.toString();
					File lf=new File(n_path);
//					Log.v("","fp="+lf.getPath());
					if (lf.exists()) {
						btnOk.setEnabled(false);
						dlg_msg.setText(mContext.getString(R.string.msgs_single_item_input_dlg_duplicate_dir));
					} else {
						btnOk.setEnabled(true);
						dlg_msg.setText("");
					}
				}
			}
		});
		etDir.setText(pli.getFileName().substring(0,pli.getFileName().lastIndexOf("."))); 
		
		//OK button
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final String new_file_name=etDir.getText().toString()+pli.getFileName().substring(pli.getFileName().lastIndexOf("."));
				final String new_name=pli.getParentDirectory()+"/"+new_file_name;
				final String current_name=pli.getParentDirectory()+"/"+pli.getFileName();
//				NotifyEvent 
//				Log.v("","new name="+new_name+", current name="+current_name);
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						boolean rc_create=false;
						if (current_name.startsWith(mGp.safMgr.getExternalSdcardPath())) {
							SafFile sf=mGp.safMgr.getSafFileBySdcardPath(mGp.safMgr.getSdcardSafFile(), current_name, false);
							sf.renameTo(new_file_name);
							rc_create=sf.exists();
						} else {
							File n_file=new File(new_name);
							File c_file=new File(current_name);
							rc_create=c_file.renameTo(n_file);
						}
						mGp.adapterThumbnailView.setAllItemsSelected(false);
						mGp.adapterThumbnailView.setSelectMode(false);
						setThumbnailViewContextButtonVisibility();
						PictureUtil.removeBitmapCacheFile(mGp, current_name);
						if (!rc_create) {
							dlg_msg.setText(String.format(
									mContext.getString(R.string.msgs_main_file_rename_failed),
									etDir.getText()));
							return;
						} else {
							scanMediaFile(new_name);
							scanMediaFile(current_name);
						}
//						buildFolderList();
						mGp.currentPictureList.remove(pli);
						pli.setFileName(new_file_name);
						mGp.currentPictureList.add(pli);
						
						AdapterThumbnailList.sort(mGp.currentPictureList, 
								mGp.adapterThumbnailView.getSortKey(), mGp.adapterThumbnailView.getSortOrder());
						
						putPictureList(mGp.currentPictureList, mGp.currentFolderListItem);

						String fp=mGp.currentPictureList.get(0).getParentDirectory()+"/"+mGp.currentPictureList.get(0).getFileName();
						if (!mGp.currentFolderListItem.getThumbnailFilePath().equals(fp)) {
							mGp.currentFolderListItem.setThumbnailArray(mGp.currentPictureList.get(0).getThumbnailImageByte());
							mGp.currentFolderListItem.setThumbnailFilePath(fp);
							saveFolderList(mGp.masterFolderList);
							mGp.adapterFolderView.notifyDataSetChanged();
						}
						
				   		String sel_month=(String)mGp.spinnerPictureSelector.getSelectedItem();
				   		createPictureShowedList(sel_month);

						setThumbnailViewContextButtonVisibility();
						
						mCommonDlg.showCommonDialog(false, "I", 
								String.format(mContext.getString(R.string.msgs_main_file_rename_completed), new_name), "", null);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_main_file_rename_confirm_title), 
						current_name, ntfy);
				dialog.dismiss();
			}
		});
		// CANCEL�{�^���̎w��
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog.show();

    }

    private void confirmMultiThumbnailViewRename() {
        final Dialog dialog = new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.single_item_input_dlg);
        final LinearLayout dlg_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
        dlg_view.setBackgroundResource(R.drawable.dialog_border_dark);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
        title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
        final TextView dlg_title = (TextView) dialog.findViewById(R.id.single_item_input_title);
        dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
        dlg_title.setText(mContext.getString(R.string.msgs_main_file_rename_title));
        final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
        final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
        final Button btnOk = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
        final Button btnCancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
        final EditText etDir=(EditText) dialog.findViewById(R.id.single_item_input_dir);
        dlg_cmp.setVisibility(TextView.GONE);

        PictureListItem w_pli=null;
        for(int i=0;i<mGp.adapterThumbnailView.getCount();i++) {
            if (mGp.adapterThumbnailView.getItem(i).isSelected()) {
                w_pli=mGp.adapterThumbnailView.getItem(i);
                break;
            }
        }
        final PictureListItem pli=w_pli;

        CommonDialog.setDlgBoxSizeCompact(dialog);
        btnOk.setEnabled(false);
        etDir.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length()>0) {
                    String n_path=pli.getParentDirectory()+"/"+s.toString();
                    File lf=new File(n_path);
//					Log.v("","fp="+lf.getPath());
                    if (lf.exists()) {
                        btnOk.setEnabled(false);
                        dlg_msg.setText(mContext.getString(R.string.msgs_single_item_input_dlg_duplicate_dir));
                    } else {
                        btnOk.setEnabled(true);
                        dlg_msg.setText("");
                    }
                }
            }
        });
        etDir.setText(pli.getFileName().substring(0,pli.getFileName().lastIndexOf(".")));

        //OK button
        btnOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String new_file_name=etDir.getText().toString()+pli.getFileName().substring(pli.getFileName().lastIndexOf("."));
                final String new_name=pli.getParentDirectory()+"/"+new_file_name;
                final String current_name=pli.getParentDirectory()+"/"+pli.getFileName();
//				NotifyEvent
//				Log.v("","new name="+new_name+", current name="+current_name);
                NotifyEvent ntfy=new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener(){
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        boolean rc_create=false;
                        if (current_name.startsWith(mGp.safMgr.getExternalSdcardPath())) {
                            SafFile sf=mGp.safMgr.getSafFileBySdcardPath(mGp.safMgr.getSdcardSafFile(), current_name, false);
                            sf.renameTo(new_file_name);
                            rc_create=sf.exists();
                        } else {
                            File n_file=new File(new_name);
                            File c_file=new File(current_name);
                            rc_create=c_file.renameTo(n_file);
                        }
                        mGp.adapterThumbnailView.setAllItemsSelected(false);
                        mGp.adapterThumbnailView.setSelectMode(false);
                        setThumbnailViewContextButtonVisibility();
                        PictureUtil.removeBitmapCacheFile(mGp, current_name);
                        if (!rc_create) {
                            dlg_msg.setText(String.format(
                                    mContext.getString(R.string.msgs_main_file_rename_failed),
                                    etDir.getText()));
                            return;
                        } else {
                            scanMediaFile(new_name);
                            scanMediaFile(current_name);
                        }
//						buildFolderList();
                        mGp.currentPictureList.remove(pli);
                        pli.setFileName(new_file_name);
                        mGp.currentPictureList.add(pli);

                        AdapterThumbnailList.sort(mGp.currentPictureList,
                                mGp.adapterThumbnailView.getSortKey(), mGp.adapterThumbnailView.getSortOrder());

                        putPictureList(mGp.currentPictureList, mGp.currentFolderListItem);

                        String fp=mGp.currentPictureList.get(0).getParentDirectory()+"/"+mGp.currentPictureList.get(0).getFileName();
                        if (!mGp.currentFolderListItem.getThumbnailFilePath().equals(fp)) {
                            mGp.currentFolderListItem.setThumbnailArray(mGp.currentPictureList.get(0).getThumbnailImageByte());
                            mGp.currentFolderListItem.setThumbnailFilePath(fp);
                            saveFolderList(mGp.masterFolderList);
                            mGp.adapterFolderView.notifyDataSetChanged();
                        }

                        String sel_month=(String)mGp.spinnerPictureSelector.getSelectedItem();
                        createPictureShowedList(sel_month);

                        setThumbnailViewContextButtonVisibility();

                        mCommonDlg.showCommonDialog(false, "I",
                                String.format(mContext.getString(R.string.msgs_main_file_rename_completed), new_name), "", null);
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_main_file_rename_confirm_title),
                        current_name, ntfy);
                dialog.dismiss();
            }
        });
        // CANCEL�{�^���̎w��
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    private void confirmThumbnailViewDelete() {
    	NotifyEvent ntfy=new NotifyEvent(mContext);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				final ThreadCtrl tc=new ThreadCtrl();
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						tc.setDisabled();
					}
				});
				final ProgressSpinDialogFragment psd=ProgressSpinDialogFragment.newInstance(
						mContext.getString(R.string.msgs_main_file_delete_dialog_title),
						mContext.getString(R.string.msgs_main_build_file_list_process_msg),
						mContext.getString(R.string.msgs_main_build_file_list_process_cancel),
						mContext.getString(R.string.msgs_main_build_file_list_process_canceling));
				psd.showDialog(mFragmentManager, psd, ntfy, false);
				Thread th=new Thread() {
					@Override
					public void run() {
				    	String del_list="", sep="";
						boolean rc_delete=false;
						ArrayList<PictureListItem>del_item=new ArrayList<PictureListItem>();
						boolean thumbnail_update_required=false;
						for(int i=0;i<mGp.adapterThumbnailView.getCount();i++) {
							if (!tc.isEnabled()) break;
							PictureListItem pli=mGp.adapterThumbnailView.getItem(i);
							
							if (pli.isSelected()) {
								if (i==0) thumbnail_update_required=true;
								psd.updateMsgText(pli.getParentDirectory()+"/"+pli.getFileName());								
								if (pli.getParentDirectory().startsWith(mGp.safMgr.getExternalSdcardPath())) {
									String to_fn=pli.getParentDirectory()+"/"+pli.getFileName();
									SafFile sf=mGp.safMgr.getSafFileBySdcardPath(mGp.safMgr.getSdcardSafFile(), 
											to_fn, false);
									rc_delete=sf.delete();
									FileIo.deleteMediaStoreItem(mContext, to_fn);
								} else {
									File lf=new File(pli.getParentDirectory()+"/"+pli.getFileName());
									rc_delete=lf.delete();
									FileIo.deleteMediaStoreItem(mContext, lf.getAbsolutePath());
								}
								PictureUtil.removeBitmapCacheFile(mGp, pli.getParentDirectory()+"/"+pli.getFileName());
								if (!rc_delete) {
									mCommonDlg.showCommonDialog(false, "I", 
											String.format(mContext.getString(R.string.msgs_main_file_delete_file_was_failed),pli.getFileName()),
											"", null);
									break;
								} else {
									del_list+=sep+pli.getParentDirectory()+"/"+pli.getFileName();
									sep=", ";
									del_item.add(pli);
								}
							}
						}
						mGp.currentPictureList.removeAll(del_item);
						mGp.showedPictureList.removeAll(del_item);
						if (thumbnail_update_required) {
							if (mGp.currentPictureList.size()>0) 
								mGp.currentFolderListItem.setThumbnailArray(mGp.currentPictureList.get(0).getThumbnailImageByte());
							else {
								mGp.currentFolderListItem.setThumbnailArray(null);
							}
						}
						
						mUiHandler.post(new Runnable(){
							@Override
							public void run() {
								if (mGp.currentPictureList.size()>0) {
									mGp.adapterThumbnailView.setAllItemsSelected(false);
									mGp.adapterThumbnailView.setSelectMode(false);
									mGp.adapterThumbnailView.notifyDataSetChanged();
									
									putPictureList(mGp.currentPictureList, mGp.currentFolderListItem);
									
									PictureListItem pli=mGp.currentPictureList.get(0);
									String fp=pli.getParentDirectory()+"/"+pli.getFileName();
									if (!mGp.currentFolderListItem.getThumbnailFilePath().equals(fp)) {
										mGp.currentFolderListItem.setThumbnailArray(pli.getThumbnailImageByte());
										mGp.currentFolderListItem.setThumbnailFilePath(fp);
										mGp.adapterFolderView.notifyDataSetChanged();
									}
									mGp.currentFolderListItem.setNoOfPictures(mGp.currentPictureList.size());
									saveFolderList(mGp.masterFolderList);
									
									if (mGp.showedPictureList.size()==0) {
										createPictureSelectionList();
										mGp.spinnerPictureSelector.setSelection(0,false);
								   		createPictureShowedList(null);
									} else {
								   		String sel_month=(String)mGp.spinnerPictureSelector.getSelectedItem();
								   		createPictureShowedList(sel_month);
									}

									setThumbnailViewContextButtonVisibility();
									mGp.adapterFolderView.notifyDataSetChanged();
								} else {
									removePictureList(mGp.currentFolderListItem);
									mGp.masterFolderList.remove(mGp.currentFolderListItem);
									if (mGp.spinnerFolderSelector!=null) createFolderShowedList((String)mGp.spinnerFolderSelector.getSelectedItem());
									else createFolderShowedList(null);
									showFolderView();
									saveFolderList(mGp.masterFolderList);
								}
							}
						});

						if (tc.isEnabled()) {
							if (rc_delete) mCommonDlg.showCommonDialog(false, "I", 
									mContext.getString(R.string.msgs_main_file_delete_file_completed), del_list, null);
						} else {
							mCommonDlg.showCommonDialog(false, "I", 
									mContext.getString(R.string.msgs_main_file_delete_file_was_cancelled), "", null);
						}
						psd.dismissAllowingStateLoss();
					}
				};
				th.start();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
    	});
    	String del_list="", sep="";
    	for(int i=0;i<mGp.adapterThumbnailView.getCount();i++) {
    		if (mGp.adapterThumbnailView.getItem(i).isSelected()) {
    			del_list+=sep+mGp.adapterThumbnailView.getItem(i).getFileName();
    			sep=", ";
    		}
    	}
    	
    	mCommonDlg.showCommonDialog(true, "W", 
    			mContext.getString(R.string.msgs_main_file_delete_confirm_delete), del_list, ntfy);
    };

	private void setThumbnailViewListener() {
		setThumbnailViewContextButtonVisibility();
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				setThumbnailViewContextButtonVisibility();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		mGp.adapterThumbnailView.setCheckedChangeListener(ntfy);

		mGp.contextButtonThumbnailShare.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled() && mGp.adapterThumbnailView.getSelectedItemCount()>0) {
					String[] send_fp=new String[mGp.adapterThumbnailView.getSelectedItemCount()];
					int cnt=0;
					for(int i=0;i<mGp.adapterThumbnailView.getCount();i++) {
						if (mGp.adapterThumbnailView.getItem(i).isSelected()) {
							send_fp[cnt]=mGp.adapterThumbnailView.getItem(i).getParentDirectory()+"/"+mGp.adapterThumbnailView.getItem(i).getFileName();
							cnt++;
						}
					}
					if (send_fp.length>100) {
						mCommonDlg.showCommonDialog(false, "E", 
								mContext.getString(R.string.msgs_main_file_share_file_max_file_count_reached), "", null);

					} else {
						CommonUtilities.sharePictures(mContext, send_fp);
					}
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonThumbnailShare, mContext.getString(R.string.msgs_main_cont_label_share));

        mGp.contextButtonThumbnailRename.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);

					if (mGp.adapterThumbnailView.getSelectedItemCount()==1) confirmSingleThumbnailViewRename();
//					else confirmMultiThumbnailViewRename();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonThumbnailRename, mContext.getString(R.string.msgs_main_cont_label_rename));

        mGp.contextButtonThumbnailPaste.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					pasteItem();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonThumbnailPaste, mContext.getString(R.string.msgs_main_cont_label_paste));

        mGp.contextButtonThumbnailCopy.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					copyItem();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonThumbnailCopy, mContext.getString(R.string.msgs_main_cont_label_copy));

        mGp.contextButtonThumbnailCut.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					cutItem();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonThumbnailCut, mContext.getString(R.string.msgs_main_cont_label_cut));

        mGp.contextButtonThumbnailDelete.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					confirmThumbnailViewDelete();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonThumbnailDelete, mContext.getString(R.string.msgs_main_cont_label_delete));

        mGp.contextButtonThumbnailSelectAll.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					mGp.adapterThumbnailView.setAllItemsSelected(true);
					mGp.adapterThumbnailView.setSelectMode(true);
					mGp.adapterThumbnailView.notifyDataSetChanged();
					setThumbnailViewContextButtonVisibility();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonThumbnailSelectAll, mContext.getString(R.string.msgs_main_cont_label_select_all));

        mGp.contextButtonThumbnailUnselectAll.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (isUiEnabled()) {
					setContextButtonEnabled((ImageButton)v, false);
					mGp.adapterThumbnailView.setAllItemsSelected(false);
					mGp.adapterThumbnailView.notifyDataSetChanged();
					setThumbnailViewContextButtonVisibility();
					setContextButtonEnabled((ImageButton)v, true);
				}
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mGp.contextButtonThumbnailUnselectAll, mContext.getString(R.string.msgs_main_cont_label_unselect_all));
		
        mGp.thumbnailGridView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (mGp.adapterThumbnailView.isSelectMode()) {
					mGp.adapterThumbnailView.getItem(position).setSelected(!mGp.adapterThumbnailView.getItem(position).isSelected());
					mGp.adapterThumbnailView.notifyDataSetChanged();
				} else {
					if (mGp.adapterThumbnailView.getItem(position).getThumbnailImageByte()!=null) 
						mPictureView.showPictureView(position);
				}
			}
		});
		
        mGp.thumbnailGridView.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				mGp.adapterThumbnailView.setSelectMode(true);
				mGp.adapterThumbnailView.getItem(position).setSelected(true);
				mGp.adapterThumbnailView.notifyDataSetChanged();
				return true;
			}
		});
		
        mGp.contextClipBoardBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				String item_list="", sep="";
				for(String item:mGp.copyCutList) {
					item_list+=sep+item.substring(item.lastIndexOf("/")+1);
					sep=", ";
					if (item_list.length()>=2048) {
						item_list+="\n.........";
						break;
					}
				}
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						mGp.copyCutList.clear();
						setClipboardBtn("");
						setThumbnailViewContextButtonVisibility();
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				mCommonDlg.showCommonDialog(true, "W", mContext.getString(R.string.msgs_main_file_copy_cut_clear_confirm), 
						item_list, ntfy);
			}
		});
	};
	
	private void createScanPictureDirectoryList(GlobalParameters gp, ArrayList<File>esfl) {
		long b_time=System.currentTimeMillis();
		if (gp.settingScanDirectoryList.size()>0) {
			for(ScanFolderItem item:gp.settingScanDirectoryList) {
				if (item.include) {
					File bf=new File(item.folder_path);
					if (bf.exists()) {
						PictureUtil.getAllPictureDirectoryInDirectory(gp, esfl, 
								bf, item.process_sub_directories);
					}
				}
			}
		}
		for(int i=esfl.size()-1;i>=0;i--) {
			if (esfl.get(i).getName().equals(".thumbnails")) esfl.remove(i);
		};
		
		Collections.sort(esfl, new Comparator<File>(){
			@Override
			public int compare(File lhs, File rhs) {
				return lhs.getAbsolutePath().compareToIgnoreCase(rhs.getAbsolutePath());
			}
		});
		
		mUtil.addDebugMsg(1, "I", "buildFolderList scan directory count="+esfl.size()+
				", Elapsed time="+(System.currentTimeMillis()-b_time));
	};
	
	private void setCameraFolderAlwaysTop(FolderListItem fli) {
		String base_dir="";
		if (fli.getParentDirectory().startsWith(mGp.internalRootDirectory)) base_dir=fli.getParentDirectory().replace(mGp.internalRootDirectory,"");
		else base_dir=fli.getParentDirectory().replace(mGp.externalRootDirectory,"");
		if (base_dir.length()==5 && base_dir.equals("/DCIM") ||
				base_dir.length()>4 && base_dir.startsWith("/DCIM/")) {
			if (mGp.settingCameraFolderAlwayTop) {
				fli.setAlwaysTop(true);
			} else {
				fli.setAlwaysTop(false);
			}
		}
	};
	
	private void updateCurrentFolder(final FolderListItem fli, final ArrayList<PictureListItem>pic_list) {
		File[]fl=new File(fli.getParentDirectory()).listFiles();
		if (fl!=null && fl.length>0) {
			ArrayList<File>file_list=new ArrayList<File>();
			for(File item:fl) {
				if (!item.isDirectory() && PictureUtil.isPictureFile(mGp, item.getName())) file_list.add(item);
			}
			if (file_list.size()>0) {
				ThreadCtrl tc=new ThreadCtrl();
				if (updateSpecificPictureList(tc, fli, pic_list, file_list)) {
					putPictureList(pic_list, fli);
//					Log.v("","pic size="+pic_list.size());
					if (pic_list.size()>0) {
						PictureListItem pli=pic_list.get(0);
						if (fli.getParentDirectory().equals(pli.getParentDirectory()) && 
								fli.getFolderName().equals(pli.getFolderName())) {
							if (mGp.currentView==CURRENT_VIEW_THUMBNAIL) {
								mUiHandler.post(new Runnable(){
									@Override
									public void run() {
										createThumbnailPictureInfo(fli, pic_list);
									}
								});
							}
						}
					}
					
//					mGp.adapterFolderView.notifyDataSetChanged();
					saveFolderList(mGp.masterFolderList);
					
					mGp.currentPictureList=pic_list;

//			   		String sel_month=(String)mGp.pictureSelectSpinner.getSelectedItem();
//			   		createSelectedTumbnailPictureList(sel_month);
				}
			}
		}
	};

	private void updateSpecificFolder(FolderListItem fli, ArrayList<PictureListItem>pic_list) {
		File[]fl=new File(fli.getParentDirectory()).listFiles();
		if (fl!=null && fl.length>0) {
			ArrayList<File>file_list=new ArrayList<File>();
			for(File item:fl) file_list.add(item);
			if (file_list.size()>0) {
				ThreadCtrl tc=new ThreadCtrl();
				if (updateSpecificPictureList(tc, fli, pic_list, file_list)) {
					putPictureList(pic_list, fli);
					fli.setThumbnailArray(pic_list.get(0).getThumbnailImageByte());
					
					mGp.adapterFolderView.notifyDataSetChanged();
					saveFolderList(mGp.masterFolderList);
				}
			}
		} else {
			mGp.masterFolderList.remove(fli);
			removePictureList(fli);
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					createFolderShowedList((String)mGp.spinnerFolderSelector.getSelectedItem());
					createFolderSelectionList();
					mGp.adapterFolderView.notifyDataSetChanged();
					saveFolderList(mGp.masterFolderList);
				}
			});
		}
	};

	private boolean updateSpecificPictureList(ThreadCtrl tc, FolderListItem fli, ArrayList<PictureListItem>pic_list, 
			ArrayList<File>file_list) {
		boolean pic_save_required=false;
		for(PictureListItem pli:pic_list) pli.setSelected(false);
		for(File item:file_list) {
			if (!tc.isEnabled()) break;
			PictureListItem curr_pli=getPictureFileListItem(pic_list, item.getParent(), item.getName());
			if (curr_pli==null) {
				PictureListItem new_pli=new PictureListItem(mGp.settingDebugLevel>1, item);
				new_pli.setSelected(true);
				pic_list.add(new_pli);
				pic_save_required=true;
			} else {
				curr_pli.setSelected(true);
				if (curr_pli.getFileLastModified()!=item.lastModified() ||
						curr_pli.getFileLength()!=item.length()) {
					curr_pli.createFileInfo(item);
					curr_pli.setThumbnailImageByte(null);
					curr_pli.setThumbnailVerified(false);
					pic_save_required=true;
				}
			}
		}
		if (tc.isEnabled()) {
			for(int i=pic_list.size()-1;i>=0;i--) {
				PictureListItem pli=pic_list.get(i);
				if (!pli.isSelected()) {
					pic_list.remove(i);
					pic_save_required=true;
				}
			}
			for(PictureListItem pli:pic_list) pli.setSelected(false);
		}
		if (pic_list.size()>0) {
			if (fli.getSortKey()!=0 || fli.getSortOrder()!=0) {
				AdapterThumbnailList.sort(pic_list, fli.getSortKey(), fli.getSortOrder());
			}
			PictureListItem pli=pic_list.get(0);
			pli.createExifInfo(pli.getParentDirectory()+"/"+pli.getFileName());
			fli.setThumbnailArray(pli.getThumbnailImageByte());
			fli.setNoOfPictures(pic_list.size());
		}
		
		return pic_save_required;
	};
	
	private void showProgressBar() {
		if (mGp.currentView!=CURRENT_VIEW_PICTURE)
			mGp.mainProgressView.setVisibility(ProgressBar.VISIBLE);
	};
	
	private void hideProgressBar() {
		mGp.mainProgressView.setVisibility(ProgressBar.GONE);
	};
	
	private void cancelBuildFolderList() {
		if (mTcBuildFolderList!=null && mTcBuildFolderList.isEnabled()) {
			mTcBuildFolderList.setDisabled();
			for(int i=0;i<500;i++) {
				if (mTcBuildFolderList==null) {
					break;
				}
				SystemClock.sleep(100);
			}
		}
	};
	
	private ThreadCtrl mTcBuildFolderList=null;
	private void buildFolderList() {
		mUtil.addDebugMsg(1, "I", "buildFolderList entered, TC="+mTcBuildFolderList);
		cancelBuildFolderList();
		mTcBuildFolderList=new ThreadCtrl();
		
//		mGp.mainProgressBar.setProgress(0);
//		mGp.mainProgressBar.setVisibility(ProgressBar.VISIBLE);
//		showProgressBar();
		
		final Dialog dialog=showProgressSpinIndicator();
		
		Thread th=new Thread(){
			@Override
			public void run() {
				long b_time=System.currentTimeMillis();
				mUtil.addDebugMsg(1, "I", "buildFolderList Scan started");
//				refreshOptionMenu();
//				setUiDisabled();
				
//				mGp.mainProgressBar.setProgress(1);

				final ArrayList<File>scan_dir_list=new ArrayList<File>();
				
				createScanPictureDirectoryList(mGp, scan_dir_list);

//				mGp.mainProgressBar.setProgress(2);
				
				synchronized(mGp.masterFolderList) {
					boolean folder_updated=false;
					//Add new folder
					for(File add_item:scan_dir_list) {
						boolean add_required=true;
						for(FolderListItem fli:mGp.masterFolderList) {
							if (fli.getParentDirectory().equals(add_item.getAbsolutePath())) {
								add_required=false;
								break;
							}
						}
						if (add_required) {
							FolderListItem new_fli=new FolderListItem(mGp.thumbnailListSortKey, mGp.thumbnailListSortOrder);
							new_fli.setFolderName(add_item.getName());
							new_fli.setParentDirectory(add_item.getAbsolutePath());
							new_fli.setFileLastModified(-1);
							new_fli.setEnabled(false);
							setCameraFolderAlwaysTop(new_fli);
							mGp.masterFolderList.add(new_fli);
							folder_updated=true;
							mUtil.addDebugMsg(1, "I", "buildFolderList folder added="+new_fli.getFolderName());
						}
					}
					
					//Remove folder and mark update required
					ArrayList<FolderListItem> del_folder_list=new ArrayList<FolderListItem>();
					for(FolderListItem fli:mGp.masterFolderList) {
						File s_key=new File(fli.getParentDirectory());
						int idx=Collections.binarySearch(scan_dir_list, s_key, new Comparator<File>(){
							@Override
							public int compare(File o1, File s_key) {
								return o1.getAbsolutePath().compareToIgnoreCase(s_key.getAbsolutePath());
							}
						});
						if (idx>=0) {
							File spfi=scan_dir_list.get(idx);
							if (fli.getFileLastModified()!=spfi.lastModified()) {
								fli.setFileLastModified(-1);
							}
						} else {
							del_folder_list.add(fli);
							folder_updated=true;
						}
					}
					mGp.masterFolderList.removeAll(del_folder_list);
					
					AdapterFolderList.sort(mGp.masterFolderList, mGp.folderListSortKey, mGp.folderListSortOrder);
					if (folder_updated) {
						mUiHandler.post(new Runnable(){
							@Override
							public void run() {
								createFolderShowedList((String)mGp.spinnerFolderSelector.getSelectedItem());
								mGp.adapterFolderView.notifyDataSetChanged();
								setFolderViewContextButtonVisibility();
							}
						});
						saveFolderList(mGp.masterFolderList);
					}

//					setUiEnabled();
					
//					int proc_count=0;
					for(int i=0;i<mGp.masterFolderList.size();i++) {
						if (!mTcBuildFolderList.isEnabled()) break;
						final FolderListItem fli=mGp.masterFolderList.get(i);
//						proc_count++;
						//Update picture list
						if (fli.getFileLastModified()==-1) {
							final ArrayList<PictureListItem>pic_list=getPictureList(fli);
							
							ArrayList<File>scanned_file_list=new ArrayList<File>();
							PictureUtil.getAllPictureFileInDirectory(mGp, scanned_file_list, 
									new File(fli.getParentDirectory()), false);
							
							boolean pic_save_required=
									updateSpecificPictureList(mTcBuildFolderList, fli, pic_list, scanned_file_list);
							
							if (fli.getSortKey()!=0 || fli.getSortOrder()!=0) {
								AdapterThumbnailList.sort(pic_list, fli.getSortKey(), fli.getSortOrder());
							}
							if (pic_list.size()>0) {
								PictureListItem pli=pic_list.get(0);
								if (fli.getThumbnailFilePath().equals(pli.getParentDirectory()+"/"+pli.getFileName())) {
									pli.createExifInfo(pli.getParentDirectory()+"/"+pli.getFileName());
									fli.setThumbnailArray(pli.getThumbnailImageByte());
									fli.setThumbnailFilePath(pli.getParentDirectory()+"/"+pli.getFileName());
									pic_save_required=true;
								}
							}
							if (fli.getNoOfPictures()!=pic_list.size()) {
								fli.setNoOfPictures(pic_list.size());
								pic_save_required=true;
							}
							
							if (pic_save_required) putPictureList(pic_list, fli);
							
//							final int progress=(proc_count*100)/(mGp.masterFolderList.size()+2);
							
							mUiHandler.post(new Runnable(){
								@Override
								public void run() {
//									mGp.mainProgressBar.setProgress(progress);
									createFolderShowedList((String)mGp.spinnerFolderSelector.getSelectedItem());
									mGp.adapterFolderView.notifyDataSetChanged();
									setFolderViewContextButtonVisibility();
									
									if (mGp.currentView==CURRENT_VIEW_THUMBNAIL || mGp.currentView==CURRENT_VIEW_PICTURE) {
//										Log.v("","cfli="+mGp.mCurrentFolderListItem.getParentDirectory()+", fli="+fli.getParentDirectory());
										if (mGp.currentFolderListItem.getParentDirectory().equals(fli.getParentDirectory())) {
											createThumbnailPictureInfo(fli, pic_list);
											mGp.currentPictureList=pic_list;
											createPictureShowedList((String)mGp.spinnerPictureSelector.getSelectedItem());
											mGp.adapterThumbnailView.setPictureList(pic_list);
											mGp.adapterThumbnailView.notifyDataSetChanged();
										}
									}
									if (dialog.isShowing()) dialog.dismiss();
								}
							});
							fli.setEnabled(true);
							if (mTcBuildFolderList.isEnabled()) 
								fli.setFileLastModified(new File(fli.getParentDirectory()).lastModified());
						}
						if (!mTcBuildFolderList.isEnabled()) break;
					};
					for(FolderListItem fli:mGp.masterFolderList) fli.setEnabled(true);
					if (dialog.isShowing()) dialog.dismiss();
				}
				
				mUiHandler.post(new Runnable(){
					@Override
					public void run() {
						createFolderShowedList((String)mGp.spinnerFolderSelector.getSelectedItem());
						mGp.adapterFolderView.notifyDataSetChanged();
						setFolderViewContextButtonVisibility();
//						mGp.mainProgressBar.setProgress(100);
						mUiHandler.post(new Runnable(){
							@Override
							public void run() {
//								hideProgressBar();
							}
						});
					}
				});
				saveFolderList(mGp.masterFolderList);
				PictureUtil.houseKeepBitmapCacheFile(mGp, mGp.pictureBitmapCacheDirectory);

				mTcBuildFolderList=null;
				refreshOptionMenu();
				mUtil.addDebugMsg(1, "I", "Build folder list ended, Elapsed time="+(System.currentTimeMillis()-b_time));
			}
		};
		th.setName("BuildFolderList");
		th.setPriority(Thread.MIN_PRIORITY);
		th.start();
	};
	
	static private PictureListItem getPictureFileListItem(ArrayList<PictureListItem> pfl,
			String pd, String name) {
		for(PictureListItem item:pfl) {
			if (pd.equals(item.getParentDirectory()) && name.equals(item.getFileName())) {
				return item;
			}
		}
//		Log.v("","getPictureFileListItem elapsed="+(System.currentTimeMillis()-b_time));
		return null;
	};

	private String createPictureListCacheFilePath(FolderListItem fli) {
		String cache_name=(fli.getParentDirectory()).replace("/", "_");
		return mGp.pictureFileCacheDirectory+cache_name;
	};
	
	public void removePictureList(FolderListItem fli) {
		File lf=new File(createPictureListCacheFilePath(fli));
		lf.delete();
//		mGp.removePictureListCache(createPictureListCacheFilePath(fli));
		mUtil.addDebugMsg(1, "I", "Picture list deleted, folder="+fli.getFolderName());
	};
	
	private FolderListItem getFolderListItem(String folder_path) {
		FolderListItem fli=null;
		for(FolderListItem w_fli:mGp.masterFolderList) {
//			Log.v("","fli n="+w_fli.getParentDirectory()+", fp="+folder_path);
			if (w_fli.getParentDirectory().equals(folder_path)) {
				fli=w_fli;
				break;
			}
		}
		return fli;
	};
	
	private ArrayList<PictureListItem> getPictureList(FolderListItem fli) {
		ArrayList<PictureListItem> pic_list=loadPictureList(createPictureListCacheFilePath(fli));
		AdapterThumbnailList.sort(pic_list, fli.getSortKey(), fli.getSortOrder());
//		Log.v("","key="+fli.getSortKey()+", order="+fli.getSortOrder());
		return pic_list;
	};

	public void putPictureList(ArrayList<PictureListItem>pic_list, FolderListItem fli) {
		savePictureList(createPictureListCacheFilePath(fli), pic_list);
	};

//	private void removePictureList(FolderListItem fli) {
//		File lf=new File(fp);
//		savePictureList(createPictureListCacheFilePath(fli), pic_list);
//	};

	private ArrayList<PictureListItem> loadPictureList(String fp) {
		ArrayList<PictureListItem>pl=new ArrayList<PictureListItem>();
		boolean loaded=false;
		long b_time=System.currentTimeMillis();
		try {
			File lf=new File(fp);
			FileInputStream fis=new FileInputStream(lf);
			BufferedInputStream bis=new BufferedInputStream(fis,1024*1024*4);
			ObjectInputStream ois=new ObjectInputStream(bis);
			int bm_cnt=ois.readInt();
			for(int i=0;i<bm_cnt;i++) {
				PictureListItem pfi=new PictureListItem(mGp.settingDebugLevel>1);
				pfi=(PictureListItem) ois.readObject();
				pl.add(pfi);
			}
			ois.close();
			bis.close();
			fis.close();
			loaded=true;
		} catch (IOException e) {
//			e.printStackTrace();
		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
		}
		if (loaded) mUtil.addDebugMsg(2, "I", "Picture list loaded"+
				", Elapsed time="+(System.currentTimeMillis()-b_time)+", File="+fp);
		else mUtil.addDebugMsg(2, "I", "Picture list not loaded, File="+fp);
		return pl;
	};

	private void savePictureList(String fp, ArrayList<PictureListItem>pic_list) {
		long b_time=System.currentTimeMillis();
		try {
			FileOutputStream fos=new FileOutputStream(fp);
			BufferedOutputStream bos=new BufferedOutputStream(fos,1024*1024*4);
			ObjectOutputStream oos=new ObjectOutputStream(bos);
			oos.writeInt(pic_list.size());
			for(int i=0;i<pic_list.size();i++) {
				oos.writeObject(pic_list.get(i));
			}
			oos.flush();
			oos.close();
//			bos.flush();
			bos.close();
//			fos.flush();
			fos.close();
			
		} catch (StreamCorruptedException e) {
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		mUtil.addDebugMsg(2, "I", "Picture list saved"+
				", Picture count="+pic_list.size()+", Elapsed time="+(System.currentTimeMillis()-b_time)+
				", Name="+fp);
	};

	private ArrayList<FolderListItem> loadFolderList() {
		long b_time=System.currentTimeMillis();
		ArrayList<FolderListItem>pfl=new ArrayList<FolderListItem>();
		try {
			FileInputStream fis=new FileInputStream(mGp.folderListFilePath);
			BufferedInputStream bis=new BufferedInputStream(fis,1024*1024*4);
			ObjectInputStream ois=new ObjectInputStream(bis);
			int bm_cnt=ois.readInt();
			for(int i=0;i<bm_cnt;i++) {
				FolderListItem pfi=new FolderListItem();
				pfi=(FolderListItem) ois.readObject();
				pfl.add(pfi);
			}
			ois.close();
			fis.close();
			AdapterFolderList.sort(pfl, mGp.folderListSortKey, mGp.folderListSortOrder);
		} catch (IOException e) {
//			e.printStackTrace();
		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
		}
		mUtil.addDebugMsg(2, "I", "Folder list loaded, elapsed time="+(System.currentTimeMillis()-b_time));
		return pfl;
	};

	public void saveFolderList(final ArrayList<FolderListItem>pfl) {
		long b_time=System.currentTimeMillis();
		int tot_pic_cnt=0;
		try {
			FileOutputStream fos=new FileOutputStream(mGp.folderListFilePath);
			BufferedOutputStream bos=new BufferedOutputStream(fos,1024*1024*4);
			ObjectOutputStream oos=new ObjectOutputStream(bos);
			oos.writeInt(pfl.size());
			for(int i=0;i<pfl.size();i++) {
				oos.writeObject(pfl.get(i));
				tot_pic_cnt+=pfl.get(i).getNoOfPictures();
			}
			oos.flush();
			oos.close();
//			bos.flush();
			bos.close();
//			fos.flush();
			fos.close();
			
		} catch (IOException e) {
//			e.printStackTrace();
		}
		mUtil.addDebugMsg(2, "I", "Folder list saved, pic count="+
				tot_pic_cnt+", elapsed time="+(System.currentTimeMillis()-b_time));
	};

	public Dialog showProgressSpinIndicator() {
		Dialog dialog=new Dialog(mActivity, R.style.MainTranslucent);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.progress_spin_indicator_dlg);
//		RelativeLayout rl_view=(RelativeLayout)dialog.findViewById(R.id.progress_spin_indicator_dlg_view);
//		rl_view.setBackgroundColor(Color.TRANSPARENT);
//		ProgressBar pb_view=(ProgressBar)dialog.findViewById(R.id.progress_spin_indicator_dlg_progress_bar);
//		pb_view.setBackgroundColor(Color.TRANSPARENT);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
		return dialog;
	};

	private void editScanFolderList() {
		mGp.refreshMediaDir(mContext);
		final Dialog dialog = new Dialog(mActivity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    dialog.setContentView(R.layout.edit_scan_folder_dlg);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.edit_scan_folder_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.edit_scan_folder_dlg_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		
		@SuppressWarnings("unused")
		final TextView dlg_msg=(TextView)dialog.findViewById(R.id.edit_scan_folder_dlg_msg);
		
		final ListView lv_scan_folder=(ListView)dialog.findViewById(R.id.edit_scan_folder_dlg_folder_list);

		final ArrayList<ScanFolderItem>fl=new ArrayList<ScanFolderItem>();
		fl.addAll(mGp.settingScanDirectoryList);
		final AdapterScanFolderList adapter=new AdapterScanFolderList(mActivity, fl);
		adapter.sort();
		lv_scan_folder.setAdapter(adapter);
		
		final Button add_all_internal=(Button)dialog.findViewById(R.id.edit_scan_folder_dlg_add_internal_folder);
		final Button add_all_sdcard=(Button)dialog.findViewById(R.id.edit_scan_folder_dlg_add_sdcard_folder);
		final Button add_specific_folder=(Button)dialog.findViewById(R.id.edit_scan_folder_dlg_add_specific_folder);
		
		setEditScanFolderViewVisibility(dialog, adapter);
		checkEditScanFolderCombination(dialog, adapter);
		
		final Button btnOk = (Button) dialog.findViewById(R.id.edit_scan_folder_dlg_btn_ok);
		final Button btnCancel = (Button) dialog.findViewById(R.id.edit_scan_folder_dlg_btn_cancel);

		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		
		NotifyEvent ntfy_delete=new NotifyEvent(mContext);
		ntfy_delete.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				setEditScanFolderViewVisibility(dialog, adapter);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
			
		});
		adapter.setNotifyDeleteListener(ntfy_delete);

		NotifyEvent ntfy_change=new NotifyEvent(mContext);
		ntfy_change.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				checkEditScanFolderCombination(dialog, adapter);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
			
		});
		adapter.setNotifyChangeListener(ntfy_change);

		add_all_internal.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (!adapter.contains(mGp.internalRootDirectory)) {
					ScanFolderItem sfi=new ScanFolderItem();
					sfi.folder_path=mGp.internalRootDirectory;
					adapter.addItem(sfi);
				}
				adapter.sort();
				adapter.notifyDataSetChanged();
				setEditScanFolderViewVisibility(dialog, adapter);
			}
		});

		add_all_sdcard.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (!adapter.contains(mGp.externalRootDirectory)) {
					ScanFolderItem sfi=new ScanFolderItem();
					sfi.folder_path=mGp.externalRootDirectory;
					adapter.addItem(sfi);
				}
				adapter.sort();
				adapter.notifyDataSetChanged();
				setEditScanFolderViewVisibility(dialog, adapter);
			}
		});

		
		add_specific_folder.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						String[] dir_list=(String[])o;
						for(String item:dir_list) {
							if (!adapter.contains(item)) {
								ScanFolderItem sfi=new ScanFolderItem();
								sfi.folder_path=item;
								adapter.addItem(sfi);
							}
						}
						adapter.sort();
						adapter.notifyDataSetChanged();
						setEditScanFolderViewVisibility(dialog, adapter);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						
					}
				});
				FileSelectDialogFragment fsdf=FileSelectDialogFragment.newInstance(
//						false, true, false, false, true, false, true, true, 
						mGp.internalRootDirectory, "", "", 
						mContext.getString(R.string.msgs_main_edit_scan_folder_title));
				fsdf.setOptionDebug(false);
				fsdf.setOptionEnableCreate(false);
				fsdf.setOptionFileOnly(false);
				fsdf.setOptionDirectoryOnly(true);
				fsdf.setOptionHideMountPoint(false);
				fsdf.setOptionIncludeRoot(false);
				fsdf.setOptionSingleSelect(false);
				fsdf.setOptionLimitMountPoint(true);
				fsdf.setOptionSelectedFilePathWithMountPoint(true);
				fsdf.setHideHiddenDirsFiles(!mGp.settingScanHiddenFile);
				fsdf.showDialog(mFragmentManager, fsdf, ntfy);
			}
		});
		

		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mGp.settingScanDirectoryList=new ArrayList<ScanFolderItem>();;
				for(ScanFolderItem item:fl) if (!item.deleted) mGp.settingScanDirectoryList.add(item);
				sortScanFolderList();
				mGp.saveScanFolderList(mContext);
				dialog.dismiss();
				checkSdcardAccess();
//				mShowProgressBar=true;
				buildFolderList();
			}
		});
 
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				for(ScanFolderItem item:fl) item.deleted=false;
			}
		});

		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
		dialog.show();
	};
	
	private void setEditScanFolderViewVisibility(Dialog dialog, AdapterScanFolderList adapter) {
		final ListView lv_scan_folder=(ListView)dialog.findViewById(R.id.edit_scan_folder_dlg_folder_list);
		final TextView tv_no_folder=(TextView)dialog.findViewById(R.id.edit_scan_folder_dlg_no_folder);
		final Button add_all_internal=(Button)dialog.findViewById(R.id.edit_scan_folder_dlg_add_internal_folder);
		final Button add_all_sdcard=(Button)dialog.findViewById(R.id.edit_scan_folder_dlg_add_sdcard_folder);
//		final Button add_specific_folder=(Button)dialog.findViewById(R.id.edit_scan_folder_dlg_add_specific_folder);
		if (adapter.contains(mGp.internalRootDirectory)) add_all_internal.setEnabled(false);
		else add_all_internal.setEnabled(true);
		if (adapter.contains(mGp.externalRootDirectory)) add_all_sdcard.setEnabled(false);
		else add_all_sdcard.setEnabled(true);
		if (mGp.externalRootDirectory.equals(GlobalParameters.STORAGE_STATUS_UNMOUNT)) add_all_sdcard.setEnabled(false);
		if (adapter.getCount()==0) {
			tv_no_folder.setVisibility(TextView.VISIBLE);
			lv_scan_folder.setVisibility(ListView.GONE);
		} else {
			tv_no_folder.setVisibility(TextView.GONE);
			lv_scan_folder.setVisibility(ListView.VISIBLE);
		}
	};
	
	private void checkEditScanFolderCombination(Dialog dialog, AdapterScanFolderList adapter) {
		String ignored_filter="", sep="";
		for(int i=0;i<adapter.getCount();i++) {
			ScanFolderItem p_sfi=adapter.getItem(i);
			if (!p_sfi.include && p_sfi.process_sub_directories) {
				for(int j=i+1;j<adapter.getCount();j++) {
					ScanFolderItem t_sfi=adapter.getItem(j);
					if (t_sfi.include) {
						if (t_sfi.folder_path.startsWith(p_sfi.folder_path)) {
							 ignored_filter+=sep+t_sfi.folder_path;
							 sep=",";
						}
					}
				}
			}
		}
		if (!ignored_filter.equals("")) {
			mCommonDlg.showCommonDialog(false, "W", 
					mContext.getString(R.string.msgs_main_edit_scan_folder_ignored_folder), ignored_filter, null);
		}
	};

	public void startSdcardPicker() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
	    startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS);
	};
	
	public void showSelectSdcardMsg(final NotifyEvent ntfy) {
		final Dialog dialog = new Dialog(mActivity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    dialog.setContentView(R.layout.show_select_sdcard_dlg);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.show_select_sdcard_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.show_select_sdcard_dlg_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		
		final TextView dlg_msg=(TextView)dialog.findViewById(R.id.show_select_sdcard_dlg_msg);
		String msg="";
		if (Build.VERSION.SDK_INT>=23) msg=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_api23);
		else if (Build.VERSION.SDK_INT>=21) msg=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_api21);
		else msg=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_api21);
		dlg_msg.setText(msg);
		
		final ImageView func_view=(ImageView)dialog.findViewById(R.id.show_select_sdcard_dlg_image);
		
		
		try {
			String fn="";
			if (Build.VERSION.SDK_INT>=23) fn=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_file_api23);
			else if (Build.VERSION.SDK_INT>=21) fn=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_file_api21);
			else fn=mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_file_api21);
		    InputStream is=mContext.getResources().getAssets().open(fn);
		    Bitmap bm = BitmapFactory.decodeStream(is);
		    func_view.setImageBitmap(bm);
		} catch (IOException e) {
		}
		
		final Button btnOk = (Button) dialog.findViewById(R.id.show_select_sdcard_dlg_btn_ok);
		final Button btnCancel = (Button) dialog.findViewById(R.id.show_select_sdcard_dlg_btn_cancel);

		CommonDialog.setDlgBoxSizeLimit(dialog,true);

		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				if (ntfy!=null) ntfy.notifyToListener(true, null);
			}
		});
 
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				if (ntfy!=null) ntfy.notifyToListener(false, null);
			}
		});

		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});
		dialog.show();
	};
	
	@SuppressLint("InflateParams")
	@SuppressWarnings("deprecation")
	private void aboutApplicaion() {
		final Dialog dialog = new Dialog(mActivity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    dialog.setContentView(R.layout.about_dialog);
	    
		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.about_dialog_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.about_dialog_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		title.setText(getString(R.string.msgs_dlg_title_about)+"(Ver "+getAppVersionName()+")");
		
        // get our tabHost from the xml
		final TabHost tab_host = (TabHost)dialog.findViewById(R.id.about_tab_host);
        tab_host.setup();
        
        final TabWidget tab_widget = (TabWidget)dialog.findViewById(android.R.id.tabs);
		 
	    tab_widget.setStripEnabled(false);  
	    tab_widget.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);  

		CustomTabContentView tabViewProf = new CustomTabContentView(this,getString(R.string.msgs_about_dlg_func_btn));
		tab_host.addTab(tab_host.newTabSpec("func").setIndicator(tabViewProf).setContent(android.R.id.tabcontent));
		
		CustomTabContentView tabViewHist = new CustomTabContentView(this,getString(R.string.msgs_about_dlg_change_btn));
		tab_host.addTab(tab_host.newTabSpec("change").setIndicator(tabViewHist).setContent(android.R.id.tabcontent));

        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout ll_func=(LinearLayout)vi.inflate(R.layout.about_dialog_func,null);
        LinearLayout ll_change=(LinearLayout)vi.inflate(R.layout.about_dialog_change,null);

		final WebView func_view=(WebView)ll_func.findViewById(R.id.about_dialog_function);
		func_view.loadUrl("file:///android_asset/"+getString(R.string.msgs_dlg_title_about_func_desc));
		func_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
//		func_view.getSettings().setBuiltInZoomControls(true);
		
		final WebView change_view=
				(WebView)ll_change.findViewById(R.id.about_dialog_change_history);
		change_view.loadUrl("file:///android_asset/"+getString(R.string.msgs_dlg_title_about_change_desc));
		change_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
//		change_view.getSettings().setBuiltInZoomControls(true);
		
		final CustomViewPagerAdapter adapter=new CustomViewPagerAdapter(this, 
	    		new WebView[]{func_view, change_view});
		final CustomViewPager mAboutViewPager=(CustomViewPager)dialog.findViewById(R.id.about_view_pager);
//	    mMainViewPager.setBackgroundColor(mThemeColorList.window_color_background);
		mAboutViewPager.setAdapter(adapter);
		mAboutViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener(){
		    @Override  
		    public void onPageSelected(int position) {
//		    	util.addDebugMsg(2,"I","onPageSelected entered, pos="+position);
		        tab_widget.setCurrentTab(position);
		        tab_host.setCurrentTab(position);
		    }  
		  
		    @Override  
		    public void onPageScrollStateChanged(int state) {  
//		    	util.addDebugMsg(2,"I","onPageScrollStateChanged entered, state="+state);
		    }  
		  
		    @Override  
		    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//		    	util.addDebugMsg(2,"I","onPageScrolled entered, pos="+position);
		    }  
		}); 

		tab_host.setOnTabChangedListener(new OnTabChangeListener(){
			@Override
			public void onTabChanged(String tabId) {
				mUtil.addDebugMsg(2,"I","onTabchanged entered. tab="+tabId);
				mAboutViewPager.setCurrentItem(tab_host.getCurrentTab());
			}
		});
		
		final Button btnOk = (Button) dialog.findViewById(R.id.about_dialog_btn_ok);

		CommonDialog.setDlgBoxSizeLimit(dialog,true);

		// OKボタンの指定
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnOk.performClick();
			}
		});

		dialog.show();
	};

	public String getAppVersionName() {
		try {
		    String packegeName = getPackageName();
		    PackageInfo packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	};

	final private void scanMediaFile(String fp) {
		FileIo.scanMediaFile(mGp, mUtil, fp);
	};

	final private void uninstallApplication() {
//		NotifyEvent ntfy=new NotifyEvent(mContext);
//		ntfy.setListener(new NotifyEventListener(){
//			@Override
//			public void positiveResponse(Context c, Object[] o) {
//				Uri uri=Uri.fromParts("package",getPackageName(),null);
//				Intent intent=new Intent(Intent.ACTION_DELETE,uri);
//				startActivity(intent);
//			}
//			@Override
//			public void negativeResponse(Context c, Object[] o) {}
//		});
//		mCommonDlg.showCommonDialog(true, "W",getString(R.string.msgs_main_uninstall_confirm_title), 
//				getString(R.string.msgs_main_uninstall_confirm_message), ntfy);
		Uri uri=Uri.fromParts("package",getPackageName(),null);
		Intent intent=new Intent(Intent.ACTION_DELETE,uri);
		startActivity(intent);
	};
	
	private void backupRestoreConfig() {
		final Dialog dialog = new Dialog(mActivity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.config_backup_restore_dlg);
		final LinearLayout dlg_view = (LinearLayout) dialog.findViewById(R.id.config_backup_restore_dlg_view);
		dlg_view.setBackgroundResource(R.drawable.dialog_border_dark);
		
		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.config_backup_restore_dlg_title_view);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		final TextView dlg_title = (TextView) dialog.findViewById(R.id.config_backup_restore_dlg_title);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		final TextView dlg_backup_date = (TextView) dialog.findViewById(R.id.config_backup_restore_dlg_backup_date);
		dlg_backup_date.setTextColor(mGp.themeColorList.text_color_dialog_title);
		final Button btnClose = (Button) dialog.findViewById(R.id.config_backup_restore_dlg_btn_close);
		final Button btnBackup = (Button) dialog.findViewById(R.id.config_backup_restore_dlg_btn_backup);
		final Button btnRestore = (Button) dialog.findViewById(R.id.config_backup_restore_dlg_btn_restore);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		if (isConfigBackupFileExists()) {
			dlg_backup_date.setText(
					String.format(mContext.getString(R.string.msgs_main_backup_restore_config_backup_date),
							StringUtil.convDateTimeTo_YearMonthDayHourMinSec(getConfigBackupFileTime())));
		} else {
			dlg_backup_date.setText(mContext.getString(R.string.msgs_main_backup_restore_config_no_backup_file));
		}
		//OK button
		btnClose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
		btnBackup.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				backupConfig();
				dialog.dismiss();
			}
		});
		btnRestore.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				restoreConfig();
				dialog.dismiss();
			}
		});
		dialog.show();
	};
	private final static String CONFIG_FILE_NAME="config.bkf";
	private void backupConfig() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				CommonUtilities.saveSettingsParmsToFile(mContext, mGp.internalRootDirectory+"/"+APPLICATION_TAG, CONFIG_FILE_NAME);
				mCommonDlg.showCommonDialog(false, "I", 
						mContext.getString(R.string.msgs_main_backup_config_backup_completed), "", null);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		
		if (isConfigBackupFileExists()) {
			mCommonDlg.showCommonDialog(true, "W", 
					String.format(mContext.getString(R.string.msgs_main_backup_config_confirm_override), 
							StringUtil.convDateTimeTo_YearMonthDayHourMinSec(getConfigBackupFileTime())),
					"", ntfy);
		} else {
			ntfy.notifyToListener(true, null);
		}
	};
	
	private boolean isConfigBackupFileExists() {
		boolean result=false;
		File cf=new File(mGp.internalRootDirectory+"/"+APPLICATION_TAG+"/"+CONFIG_FILE_NAME);
		try {
			BufferedReader br=new BufferedReader(new FileReader(cf));
			String pl=br.readLine();
			br.close();
			if (pl.startsWith("Config ")) result=true;
		} catch (Exception e) {
//			e.printStackTrace();
		}
		return result;
	};

	private long getConfigBackupFileTime() {
		long result=0;
		File cf=new File(mGp.internalRootDirectory+"/"+APPLICATION_TAG+"/"+CONFIG_FILE_NAME);
		try {
			BufferedReader br=new BufferedReader(new FileReader(cf));
			String pl=br.readLine();
			br.close();
			if (pl.startsWith("Config ")) {
				String time=pl.substring(pl.indexOf(" ")+1);
				result=Long.parseLong(time);
			}
		} catch (Exception e) {
//			e.printStackTrace();
		}
		return result;
	};

	private void restoreConfig() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				CommonUtilities.restoreSettingsParmFromFile(mContext, mGp.internalRootDirectory+"/"+APPLICATION_TAG, CONFIG_FILE_NAME);
				mCommonDlg.showCommonDialog(false, "I", 
						mContext.getString(R.string.msgs_main_restore_config_restore_completed), "", null);
				applySettingParms();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		
		if (!isConfigBackupFileExists()) {
			mCommonDlg.showCommonDialog(false, "W", 
					mContext.getString(R.string.msgs_main_restore_config_file_not_found),
					"", null);
		} else {
			mCommonDlg.showCommonDialog(true, "W", 
					String.format(mContext.getString(R.string.msgs_main_restore_config_confirm_restore), 
							StringUtil.convDateTimeTo_YearMonthDayHourMinSec(getConfigBackupFileTime())),
					"", ntfy);
		}
	};

}

class SavedViewContents {
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
};
