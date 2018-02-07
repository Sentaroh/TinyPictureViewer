package com.sentaroh.android.TinyPictureViewer;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.Preference.CustomDialogPreference;
import com.sentaroh.android.Utilities.Preference.CustomDialogPreference.CustomDialogPreferenceButtonListener;

public class ActivitySettings extends PreferenceActivity{
	private static Context mContext=null;
	private static PreferenceFragment mPrefFrag=null;
	
	@SuppressWarnings("unused")
	private static ActivitySettings mPrefActivity=null;
	
	private static GlobalParameters mGp=null;
	
	private CommonUtilities mUtil=null;
	
//	private GlobalParameters mGp=null;
	
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mContext=this;
		mGp=(GlobalParameters)getApplication();
//		setTheme(mGp.applicationTheme);
		super.onCreate(savedInstanceState);
		mPrefActivity=this;
		if (mUtil==null) mUtil=new CommonUtilities(this, "SettingsActivity", mGp);
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
	}

    @Override
    public void onStart(){
        super.onStart();
        if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
    };
 
    @Override
    public void onResume(){
        super.onResume();
        if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
//		setTitle(R.string.settings_main_title);
    };
 
    @Override
    public void onBuildHeaders(List<Header> target) {
    	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
        loadHeadersFromResource(R.xml.settings_frag, target);
    };

    @Override
    public boolean onIsMultiPane () {
    	mContext=this;
    	mGp=(GlobalParameters)getApplication();
//    	mPrefActivity=this;
    	mUtil=new CommonUtilities(this, "SettingsActivity", mGp);
    	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
        return true;
    };

	@Override  
	protected void onPause() {  
	    super.onPause();  
	    if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
	};

	@Override
	final public void onStop() {
		super.onStop();
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
	};

	private static void checkSettingValue(CommonUtilities ut, SharedPreferences shared_pref, String key_string) {
		if (!checkFileSettings(ut, mPrefFrag.findPreference(key_string),shared_pref, key_string,mContext))
		if (!checkUiSettings(ut, mPrefFrag.findPreference(key_string),shared_pref, key_string,mContext))
	    if (!checkLogSettings(ut, mPrefFrag.findPreference(key_string),shared_pref, key_string,mContext))
		if (!checkMiscSettings(ut, mPrefFrag.findPreference(key_string),shared_pref, key_string,mContext))				    	
		   	checkOtherSettings(ut, mPrefFrag.findPreference(key_string),shared_pref, key_string,mContext);
	};

	private static boolean checkFileSettings(CommonUtilities ut, final Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		if (key_string.equals(c.getString(R.string.settings_process_hidden_files))) {
			isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_folder_filter_character_count))) {
			isChecked=true;
			pref_key.setSummary(String.format(c.getString(R.string.settings_folder_filter_character_count_summary), 
						shared_pref.getString(key_string, "4")));
    	} else if (key_string.equals(c.getString(R.string.settings_file_changed_auto_detect))) {
			isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_camera_folder_always_top))) {
			isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_clear_cache))) {
			isChecked=true;
			CustomDialogPreference cdp=(CustomDialogPreference)pref_key;
			cdp.setButtonListener(new CustomDialogPreferenceButtonListener(){
				@Override
				public void onButtonClick(DialogInterface dialog, int which) {
					if (which==DialogInterface.BUTTON_POSITIVE) {
						File lf=new File(mGp.folderListFilePath);
						lf.delete();

						PictureUtil.clearCacheFileDirectory(mGp.pictureFileCacheDirectory);
						PictureUtil.clearCacheFileDirectory(mGp.pictureBitmapCacheDirectory);
						
						mGp.masterFolderList.clear();
						mGp.showedFolderList.clear();
						mGp.pictureFileCacheList.clear();
						
						pref_key.setEnabled(false);
					}
				}
			});
    	}

    	return isChecked;
	};
	
	private static boolean checkUiSettings(CommonUtilities ut, Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		if (key_string.equals(c.getString(R.string.settings_picture_display_default_ui_mode))) {
			isChecked=true;
		} else if (key_string.equals(c.getString(R.string.settings_max_screen_brightness_when_image_showed))) {
			isChecked=true;
		} else if (key_string.equals(c.getString(R.string.settings_picture_display_option_restore_when_startup))) {
			isChecked=true;
//			if (shared_pref.getBoolean(key_string, false)) {
//				mPrefFrag.findPreference(c.getString(R.string.settings_picture_screen_witout_navigate_button_when_startup)).setEnabled(false);
//			} else {
//				mPrefFrag.findPreference(c.getString(R.string.settings_picture_screen_witout_navigate_button_when_startup)).setEnabled(true);
//			}
    	}
		
    	return isChecked;
	};

	private static boolean checkMiscSettings(CommonUtilities ut, 
			Preference pref_key, SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_exit_clean))) {
			isChecked=true;
			if (shared_pref.getBoolean(key_string, true)) {
				pref_key
					.setSummary(c.getString(R.string.settings_exit_clean_summary_ena));
			} else {
				pref_key
					.setSummary(c.getString(R.string.settings_exit_clean_summary_dis));
			}
    	}

    	return isChecked;
	};

	private static boolean checkLogSettings(CommonUtilities ut, 
			Preference pref_key, SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
    	if (key_string.equals(c.getString(R.string.settings_log_option))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_put_logcat_option))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_log_level))) {
    		isChecked=true;
    		String[] wl_label= c.getResources().getStringArray(R.array.settings_log_level_list_entries);
    		String sum_msg=wl_label[Integer.parseInt(shared_pref.getString(key_string, "0"))];
			pref_key.setSummary(sum_msg);
    	} else if (key_string.equals(c.getString(R.string.settings_log_file_max_count))) {
    		isChecked=true;
			pref_key.setSummary(String.format(c.getString(R.string.settings_log_file_max_count_summary),
							shared_pref.getString(key_string, "10")));
    	}

    	return isChecked;
	};

	private static boolean checkOtherSettings(CommonUtilities ut, 
			Preference pref_key, SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = true;
    	if (pref_key!=null) {
    		pref_key.setSummary(
	    		c.getString(R.string.settings_default_current_setting)+
	    		shared_pref.getString(key_string, "0"));
    	} else {
    		if (mGp.settingDebugLevel>0) ut.addDebugMsg(1, "I", "checkOtherSettings Key not found key="+key_string);
    	}
    	return isChecked;
	};

    public static class SettingsLog extends PreferenceFragment {
    	private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =   
    		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
    	    public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
    	    	checkSettingValue(mUtil, shared_pref, key_string);
    	    }
    	};
    	private CommonUtilities mUtil=null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	mPrefFrag=this;
        	mUtil=new CommonUtilities(mContext, "SettingsLog", mGp);
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_log);

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
        	if (!LocalMountPoint.isExternalStorageAvailable()) {
        		findPreference(getString(R.string.settings_log_dir).toString())
        			.setEnabled(false);
        	}
    		
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_log_option));
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_put_logcat_option));
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_log_file_max_count));
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_log_dir));
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_log_level));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
    		getActivity().setTitle(R.string.settings_log_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
    
    public static class SettingsMisc extends PreferenceFragment {
    	private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =   
    		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
    	    public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
    	    	checkSettingValue(mUtil, shared_pref, key_string);
    	    }
    	};
    	private CommonUtilities mUtil=null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	mPrefFrag=this;
        	mUtil=new CommonUtilities(mContext, "SettingsMisc", mGp);
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_misc);

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
        	shared_pref.edit().putBoolean(getString(R.string.settings_exit_clean),true).commit();
    		findPreference(getString(R.string.settings_exit_clean).toString()).setEnabled(false);
        	checkSettingValue(mUtil, shared_pref,getString(R.string.settings_exit_clean));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
    		getActivity().setTitle(R.string.settings_misc_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

    public static class SettingsUi extends PreferenceFragment {
    	private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =   
    		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
    	    public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
    	    	checkSettingValue(mUtil, shared_pref, key_string);
    	    }
    	};
    	private CommonUtilities mUtil=null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	mPrefFrag=this;
        	mUtil=new CommonUtilities(mContext, "SettingsUi", mGp);
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
           
    		addPreferencesFromResource(R.xml.settings_frag_ui);

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_picture_display_default_ui_mode));
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_max_screen_brightness_when_image_showed));
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_picture_display_option_restore_when_startup));
    		
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
    		getActivity().setTitle(R.string.settings_ui_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
    
    public static class SettingsFile extends PreferenceFragment {
    	private SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =   
    		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
    	    public void onSharedPreferenceChanged(SharedPreferences shared_pref, String key_string) {
    	    	checkSettingValue(mUtil, shared_pref, key_string);
    	    }
    	};
    	private CommonUtilities mUtil=null;
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	mPrefFrag=this;
        	mUtil=new CommonUtilities(mContext, "SettingsFile", mGp);
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
           
    		addPreferencesFromResource(R.xml.settings_frag_file);

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
    		if (mGp.masterFolderList.size()==0) {
    			mPrefFrag.findPreference(getString(R.string.settings_clear_cache)).setEnabled(false);
    		}
    		
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_process_hidden_files));
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_folder_filter_character_count));
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_file_changed_auto_detect));
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_camera_folder_always_top));
    		checkSettingValue(mUtil, shared_pref,getString(R.string.settings_clear_cache));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
    		getActivity().setTitle(R.string.settings_file_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (mGp.settingDebugLevel>0) mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName()+" entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

}