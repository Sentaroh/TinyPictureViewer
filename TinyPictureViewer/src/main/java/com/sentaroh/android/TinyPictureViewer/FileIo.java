package com.sentaroh.android.TinyPictureViewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import com.sentaroh.android.Utilities.SafFile;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.Dialog.ProgressSpinDialogFragment;

public class FileIo {
	private final static int BUFFER_SIZE=1024*1024*4;

	static public void scanMediaFile(GlobalParameters gp, CommonUtilities util, String fp) {
		MediaScannerConnection.scanFile(gp.appContext, new String[]{fp}, null, null);
		util.addDebugMsg(2, "I","Media scanner invoked, name=",fp);
	};

	static public int deleteMediaStoreItem(Context c, String fp) {
		Uri uri=MediaStore.Files.getContentUri("external");
		int dc=c.getContentResolver().delete(
			    uri,
			    MediaStore.Files.FileColumns.DATA + "=?",
			    new String[]{ fp }
		);
		return dc;
	};
	
	static public boolean deleteLocalItem(File del_item) {
		boolean result=false;
		if (del_item.exists()) {
			if (del_item.isDirectory()) {
				File[] del_list=del_item.listFiles();
				if (del_list!=null && del_list.length>0) {
					for(File child_item:del_list) {
						result=deleteLocalItem(child_item);
						if (!result) break;
					}
					if (result) result=del_item.delete(); 
				} else {
					result=del_item.delete(); 
				}
			} else {
				result=del_item.delete();
			}
		}
		return result;
	};
	
	static public boolean moveInternalToInternal(GlobalParameters gp, CommonUtilities util,
			ProgressSpinDialogFragment psdf, ThreadCtrl tc, 
			ArrayList<String>ccl, String to_dir) {
		boolean result=false;
		File in_file=null;
		for(String f_path:ccl) {
			if (!tc.isEnabled()) break;
			in_file=new File(f_path);
			File out_file=new File(to_dir+"/"+in_file.getName());
			out_file.delete();
			in_file.renameTo(out_file);
			String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_move_success), in_file.getName());
			psdf.updateMsgText(msg);
			util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
			scanMediaFile(gp, util, out_file.getAbsolutePath());
			scanMediaFile(gp, util, in_file.getAbsolutePath());
		}
		return result;
	};
	static public boolean copyInternalToInternal(GlobalParameters gp, CommonUtilities util,
			ProgressSpinDialogFragment psdf, ThreadCtrl tc, 
			ArrayList<String>ccl, String to_dir) {
		boolean result=false;
		File in_file=null;
		File out_file_tmp=null;
		for(String f_path:ccl) {
			if (!tc.isEnabled()) break;
			try {
				in_file=new File(f_path);
				out_file_tmp=new File(gp.internalRootDirectory+gp.appSpecificDirectory+"/temp_file.tmp");
				FileInputStream fis=new FileInputStream(in_file);
				FileOutputStream fos=new FileOutputStream(out_file_tmp);
				byte[] buff=new byte[BUFFER_SIZE];
				int rc=fis.read(buff);
				while(rc>0) {
					if (!tc.isEnabled()) break;
					fos.write(buff, 0, rc);
					rc=fis.read(buff);
				}
				fos.flush();
				fos.close();
				fis.close();
				
				if (!tc.isEnabled()) {
					result=false;
					out_file_tmp.delete();
					String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_copy_fail), in_file.getName());
					psdf.updateMsgText(msg);
					util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
					break;
				} else {
					File out_file=new File(to_dir+"/"+in_file.getName());
					out_file.delete();
					out_file_tmp.renameTo(out_file);
					out_file.setLastModified(in_file.lastModified());
					result=true;
					String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_copy_success), in_file.getName());
					psdf.updateMsgText(msg);
					util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
					scanMediaFile(gp, util, out_file.getAbsolutePath());
				}
			} catch (IOException e) {
				out_file_tmp.delete();
				e.printStackTrace();
				String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_copy_fail), in_file.getName());
				psdf.updateMsgText(msg);
				util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
			}
		}
		return result;
	};
	
	static public boolean moveInternalToExternal(GlobalParameters gp, CommonUtilities util,
			ProgressSpinDialogFragment psdf, ThreadCtrl tc, 
			ArrayList<String>ccl, String to_dir) {
		boolean result=false;
		if (copyInternalToExternal(gp, util, psdf, tc, ccl, to_dir)) {
			for(String item:ccl) {
				if (!tc.isEnabled()) {
					result=false;
					break;
				} else {
					File lf=new File(item);
					result=lf.delete();
					if (!result) {
						String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_move_fail), lf.getName());
						psdf.updateMsgText(msg);
						util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
						break;
					} else {
						String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_move_success),lf.getName());
						psdf.updateMsgText(msg);
						util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
						scanMediaFile(gp, util, lf.getAbsolutePath());
					}
				}
			}
		}
		return result;
	};
	
	static public boolean copyInternalToExternal(GlobalParameters gp, CommonUtilities util,
			ProgressSpinDialogFragment psdf, ThreadCtrl tc, 
			ArrayList<String>ccl, String to_dir) {
		boolean result=false;
		File in_file=null;
		SafFile tsf=null;
		for(String f_path:ccl) {
			if (!tc.isEnabled()) break;
			try {
				in_file=new File(f_path);
				tsf=gp.safMgr.getSafFileBySdcardPath(gp.safMgr.getSdcardSafFile(), to_dir+"/"+in_file.getName()+".tmp", false);
				FileInputStream fis=new FileInputStream(in_file);
				OutputStream fos=gp.appContext.getContentResolver().openOutputStream(tsf.getUri());
				byte[] buff=new byte[BUFFER_SIZE];
				int rc=fis.read(buff);
				while(rc>0) {
					if (!tc.isEnabled()) break;
					fos.write(buff, 0, rc);
					rc=fis.read(buff);
				}
				fos.flush();
				fos.close();
				fis.close();
				
				if (!tc.isEnabled()) {
					tsf.delete();
					result=false;
					break;
				} else {
					String to_fn=to_dir+"/"+in_file.getName();
					SafFile dsf=gp.safMgr.getSafFileBySdcardPath(gp.safMgr.getSdcardSafFile(), to_fn, false);
					dsf.delete();
					tsf.renameTo(in_file.getName());
					result=true;
					String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_copy_success),in_file.getName());
					psdf.updateMsgText(msg);
					util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
					scanMediaFile(gp, util, to_fn);
				}
			} catch (IOException e) {
				tsf.delete();
				e.printStackTrace();
				String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_copy_fail),in_file.getName());
				psdf.updateMsgText(msg);
				util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
			}
		}
		return result;
	};
	
	static public boolean moveExternalToExternal(GlobalParameters gp, CommonUtilities util,
			ProgressSpinDialogFragment psdf, ThreadCtrl tc, 
			ArrayList<String>ccl, String to_dir) {
		boolean result=false;
		if (copyInternalToExternal(gp, util, psdf, tc, ccl, to_dir)) {
			for(String item:ccl) {
				if (!tc.isEnabled()) break;
				SafFile sf=gp.safMgr.getSafFileBySdcardPath(gp.safMgr.getSdcardSafFile(), item, false);
				if (sf!=null) {
					result=sf.delete();
					if (!result) {
						String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_move_fail), item.substring(item.lastIndexOf(("/")+1)));
						psdf.updateMsgText(msg);
						util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
						break;
					} else {
						String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_move_success), item.substring(item.lastIndexOf(("/")+1)));
						psdf.updateMsgText(msg);
						util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
						scanMediaFile(gp, util, item);
					}
				} else {
					String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_move_fail), item.substring(item.lastIndexOf(("/")+1)));
					psdf.updateMsgText(msg);
					util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
					break;
				}
			}
		}
		return result;
	};

	static public boolean copyExternalToExternal(GlobalParameters gp, CommonUtilities util,
			ProgressSpinDialogFragment psdf, ThreadCtrl tc, 
			ArrayList<String>ccl, String to_dir) {
		return copyInternalToExternal(gp, util, psdf, tc, ccl, to_dir);
	};

	static public boolean moveExternalToInternal(GlobalParameters gp, CommonUtilities util,
			ProgressSpinDialogFragment psdf, ThreadCtrl tc, 
			ArrayList<String>ccl, String to_dir) {
		boolean result=false;
		if (copyInternalToInternal(gp, util, psdf, tc, ccl, to_dir)) {
			for(String item:ccl) {
				if (!tc.isEnabled()) break;
				SafFile sf=gp.safMgr.getSafFileBySdcardPath(gp.safMgr.getSdcardSafFile(), item, false);
				if (sf!=null) {
					result=sf.delete();
					if (!result) {
						String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_move_fail), item.substring(item.lastIndexOf(("/")+1)));
						psdf.updateMsgText(msg);
						util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
						break;
					} else {
						String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_move_success), item.substring(item.lastIndexOf(("/")+1)));
						psdf.updateMsgText(msg);
						util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
						scanMediaFile(gp, util, item);
					}
				} else {
					String msg=String.format(gp.appContext.getString(R.string.msgs_main_file_move_fail), item.substring(item.lastIndexOf(("/")+1)));
					psdf.updateMsgText(msg);
					util.addLogMsg("I", CommonUtilities.getExecutedMethodName()+" "+msg);
					break;
				}
			}
		}
		return result;
	};

	static public boolean copyExternalToInternal(GlobalParameters gp, CommonUtilities util,
			ProgressSpinDialogFragment psdf, ThreadCtrl tc, 
			ArrayList<String>ccl, String to_dir) {
		return copyInternalToInternal(gp, util, psdf, tc, ccl, to_dir);
	};

}
