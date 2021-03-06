package com.sentaroh.android.TinyPictureViewer;

import static com.sentaroh.android.TinyPictureViewer.Constants.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressLint("DefaultLocale")
public class PictureUtil {
    private static Logger log= LoggerFactory.getLogger(PictureUtil.class);

	final static public String createBitmapCacheFilePath(GlobalParameters gp, String pic_file_path) {
		String cache_name=(pic_file_path).replace("/", "_")+".bmc";
		return gp.internalRootDirectory+gp.appSpecificDirectory+"/bitmap_cache/"+cache_name;
	};
	
	final static public boolean isBitmapCacheFileExists(GlobalParameters gp, String pic_file_path) {
		File lf=new File(createBitmapCacheFilePath(gp, pic_file_path));
		return lf.exists();
	};
	
	final static public void removeBitmapCacheFile(GlobalParameters gp, String pic_file_path) {
		removePictureFileCacheItemFromCache(gp, pic_file_path);
		File cf=new File(PictureUtil.createBitmapCacheFilePath(gp, pic_file_path));
		cf.delete();
	};
	
	public static class PictureFileCacheItem implements Externalizable{
		private static final long serialVersionUID = SERIALIZABLE_VERSION_CODE;
		public PictureFileCacheItem(){};
		public String file_path="";
		public long file_length=0L;
		public long file_last_modified=0L;
		byte[] bitmap_byte_array=null;
		@Override
		public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
			if (input.readLong()!=serialVersionUID) 
				throw new IOException("serialVersionUID was not matched by saved UID");
			file_path=input.readUTF();
			file_length=input.readLong();
			file_last_modified=input.readLong();
			bitmap_byte_array=readArrayByte(input);
		}
		@Override
		public void writeExternal(ObjectOutput output) throws IOException {
			output.writeLong(serialVersionUID);
			output.writeUTF(file_path);
			output.writeLong(file_length);
			output.writeLong(file_last_modified);
			writeArrayByte(output,bitmap_byte_array);
		}
	};
	
	final static public byte[] readArrayByte(ObjectInput input) throws IOException{
		int lsz=input.readInt();
		byte[] result=null;
		if (lsz!=-1) {
			result=new byte[lsz];
			if (lsz>0) input.readFully(result,0,lsz);
		}
		return result;
	};
	
	final static public void writeArrayByte(ObjectOutput output, byte[] al) throws IOException {
		int lsz=-1;
		if (al!=null) {
			if (al.length!=0) lsz=al.length;
			else lsz=0;
		}
		output.writeInt(lsz);
		if (lsz>0) {
			output.write(al,0,lsz);
		}
	};

	final static long MAX_BITMAP_FILE_CACHE_SIZE=1024*1024*512; //512MB
	final static long MAX_BITMAP_FILE_RETENTION_PERIOD=(1000*60*60*24)*30;//30Day
	static public void houseKeepBitmapCacheFile(final GlobalParameters gp, final String cd) {
		Thread th=new Thread(){
			@Override
			public void run() {
//				long b_time=System.currentTimeMillis();
//				Log.v("","non exists file="+(System.currentTimeMillis()-b_time));
				File df=new File(cd);
				File[] cf_list=df.listFiles();
				if (cf_list!=null && cf_list.length>0) {
					long expired_time=System.currentTimeMillis()-MAX_BITMAP_FILE_RETENTION_PERIOD;
					long fs=0;
					for(File ch_file:cf_list) fs+=ch_file.length();
					for(File ch_file:cf_list) {
						if (ch_file.exists()) {
							if (fs>=MAX_BITMAP_FILE_CACHE_SIZE) {
								if (ch_file.lastModified()<=expired_time) {
									ch_file.delete();
									File del_file=new File(ch_file.getPath().substring(0,ch_file.getPath().lastIndexOf(".")));
									del_file.delete();
//									Log.v("","bitmap cache file deleted by housekeep request, name="+del_file.getPath());
								}
							}
						}
					}
				}
//				Log.v("","housekeep="+(System.currentTimeMillis()-b_time));
			};
		};
		th.setName("cacheHousekeep");
		th.start();
	};

	static public void clearCacheFileDirectory(final String cd) {
		Thread th=new Thread() {
			@Override
			public void run() {
				File df=new File(cd);
				File[] cf_list=df.listFiles();
				if (cf_list!=null && cf_list.length>0) {
					for(File ch_file:cf_list) {
						ch_file.delete();
//						File del_file=new File(ch_file.getPath().substring(0,ch_file.getPath().lastIndexOf(".")));
//						del_file.delete();
//						Log.v("","bitmap cache file deleted by clear request, name="+ch_file.getPath());
					}
				}
			}
		};
		th.setName("ClearBitmapCache");
		th.start();
	};

	static public void removePictureFileCacheItemFromCache(GlobalParameters gp, String pic_file_path) {
		PictureFileCacheItem result=null;
		synchronized(gp.pictureFileCacheList) {
			if (gp.pictureFileCacheList.size()>0) {
				for(PictureFileCacheItem pfci:gp.pictureFileCacheList) {
//					Log.v("","fp="+pic_file_path+"\n"+"c="+pfci.file_path);
					if (pfci.file_path.equals(pic_file_path)) {
						result=pfci;
//			        	if (gp.settingDebugLevel>1) Log.v(APPLICATION_TAG,"Cache hit, name="+pic_file_path);
						break;
					}
				}
				if (result!=null) {
					gp.pictureFileCacheList.remove(result);
				}
			}
		}
	};

	static public PictureFileCacheItem getPictureFileCacheItemFromCache(GlobalParameters gp, String pic_file_path) {
		PictureFileCacheItem result=null;
		synchronized(gp.pictureFileCacheList) {
			if (gp.pictureFileCacheList.size()>0) {
				for(PictureFileCacheItem pfci:gp.pictureFileCacheList) {
//					Log.v("","fp="+pic_file_path+"\n"+"c="+pfci.file_path);
					if (pfci.file_path.equals(pic_file_path)) {
						result=pfci;
//			        	if (gp.settingDebugLevel>1) Log.v(APPLICATION_TAG,"Cache hit, name="+pic_file_path);
						break;
					}
				}
				if (result!=null) {
					gp.pictureFileCacheList.remove(result);
					gp.pictureFileCacheList.add(0, result);
//					Log.v("","cache hit");
				}
			}
		}
		return result;
	};
	
	final static private int MAX_CACHE_SIZE=100;
	static private void addPictureFileCacheItemToCache(GlobalParameters gp, PictureFileCacheItem pfci) {
		synchronized(gp.pictureFileCacheList) {
			PictureFileCacheItem c_pfci=getPictureFileCacheItemFromCache(gp, pfci.file_path);
			if (c_pfci==null) {
				gp.pictureFileCacheList.add(0, pfci);
//				if (gp.settingDebugLevel>1) Log.v(APPLICATION_TAG,"Cache added, name="+createBitmapCacheFilePath(gp, pfci.file_path));
				if (gp.pictureFileCacheList.size()>MAX_CACHE_SIZE) {
					gp.pictureFileCacheList.remove(MAX_CACHE_SIZE);
//					if (gp.settingDebugLevel>1) Log.v(APPLICATION_TAG,"Cache remove, pos="+MAX_CACHE_SIZE);
				}
			} else {
				c_pfci.bitmap_byte_array=pfci.bitmap_byte_array;
				c_pfci.file_last_modified=pfci.file_last_modified;
				c_pfci.file_length=pfci.file_length;
			}
		}
	};
	
	static public PictureFileCacheItem loadPictureFileCacheFile(GlobalParameters gp, String pic_file_path) {
		long b_time=System.currentTimeMillis();
		PictureFileCacheItem pfbmci=null;
		try {
			FileInputStream fis=new FileInputStream(new File(createBitmapCacheFilePath(gp, pic_file_path)));
			BufferedInputStream bis=new BufferedInputStream(fis,1024*1024*4);
			ObjectInputStream ois=new ObjectInputStream(bis);
			pfbmci=new PictureFileCacheItem();
			pfbmci.readExternal(ois);
			ois.close();
			bis.close();
			addPictureFileCacheItemToCache(gp, pfbmci) ;
            log.debug("Bitmap cache file loaded"+
                ", elapsed time="+(System.currentTimeMillis()-b_time)+", fp="+pic_file_path);
		} catch (FileNotFoundException e) {
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
		}
		return pfbmci;
	};

	static private void savePictureFileCacheFile(GlobalParameters gp,  
			PictureFileCacheItem pfbmci) {
		try {
			long b_time=System.currentTimeMillis();
			final File bmcf=new File(createBitmapCacheFilePath(gp, pfbmci.file_path));
			FileOutputStream fos=new FileOutputStream(bmcf);
			BufferedOutputStream bos=new BufferedOutputStream(fos,1024*1024*4);
			ObjectOutputStream oos=new ObjectOutputStream(bos);
			pfbmci.writeExternal(oos);
			oos.flush();
			oos.close();
			addPictureFileCacheItemToCache(gp, pfbmci);
            log.debug("savePictureFileCacheFile cache file saved"+
                ", elapsed time="+(System.currentTimeMillis()-b_time)+", fp="+bmcf.getAbsolutePath());
		} catch (IOException e) {
            log.debug("savePictureFileCacheFile cache file save error, error="+e.getMessage()+", fp="+pfbmci.file_path);
		}
	};

	final static public PictureFileCacheItem getPictureFileCacheItem(final GlobalParameters gp,
			final String fp, DisplayMetrics disp_metrics, String orientation) {
    	long b_time=System.currentTimeMillis();
    	boolean recreate_required=false;
    	PictureFileCacheItem pfbmci=getPictureFileCacheItemFromCache(gp, fp);
    	if (pfbmci==null) {
    		pfbmci=loadPictureFileCacheFile(gp, fp);
    	}
		if (pfbmci!=null) {
        	File img=new File(fp);
    		if (img.lastModified()!=pfbmci.file_last_modified || img.length()!=pfbmci.file_length) {
    			recreate_required=true;
    		}
		} else {
			recreate_required=true;
		}
//    	recreate_required=true;
    	
    	if (recreate_required) {
    		pfbmci=createPictureCacheFile(gp, fp, disp_metrics, orientation);
    	}
    	if (gp.settingDebugLevel>1)  
    		gp.cUtil.addDebugMsg(1,"I","getPictureFileCacheItem ended, Elapsed time="+(System.currentTimeMillis()-b_time)+
    			", result="+pfbmci+", fp="+fp);

		return pfbmci;
    };

    final static private PictureFileCacheItem createPictureCacheFile(
    		final GlobalParameters gp, 
			final String fp, DisplayMetrics disp_metrics, String orientation) {
		Bitmap bitmap=createPictureFileBitmap(gp, fp, disp_metrics, orientation);
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		byte[] ba=null;
		if (bitmap!=null) {
			bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
			ba=baos.toByteArray();
			bitmap.recycle();
		}
		
		final File pic_file=new File(fp);
		final PictureFileCacheItem pfbmci=new PictureFileCacheItem();
		pfbmci.file_path=fp;
		pfbmci.file_last_modified=pic_file.lastModified();
		pfbmci.file_length=pic_file.length();
		pfbmci.bitmap_byte_array=ba;
		
		addPictureFileCacheItemToCache(gp, pfbmci) ;
//		Log.v("","cache added, name="+fp);

    	Thread save=new Thread() {
    		@Override
    		public void run() {
    			savePictureFileCacheFile(gp, pfbmci);
    		}
    	};
    	save.start();
    	return pfbmci;
    };
    
  	final static public String EXIF_IMAGE_ORIENTATION_CLOCKWISE_90_AND_FLIP_HORIZONTAL="5";//時計回りに90度回転して水平反転
    final static public String EXIF_IMAGE_ORIENTATION_CLOCKWISE_90="6";//時計回りに90度回転
    final static public String EXIF_IMAGE_ORIENTATION_CLOCKWISE_270_AND_FLIP_HORIZONTAL="7";//時計回りに270度回転して水平反転
    final static public String EXIF_IMAGE_ORIENTATION_CLOCKWISE_270="8";//時計回りに270度回転

	final static private Bitmap createPictureFileBitmap(final GlobalParameters gp,
			final String fp, DisplayMetrics disp_metrics, String orientation) {
    	long b_time=System.currentTimeMillis();
    	byte[] image_file_byte_array=PictureUtil.createImageByteArray(gp, fp);
    	if (image_file_byte_array!=null) {
            BitmapFactory.Options org_opt = new BitmapFactory.Options();
            org_opt.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(image_file_byte_array, 0, image_file_byte_array.length, org_opt);

            BitmapFactory.Options decode_opt = new BitmapFactory.Options();
            if (org_opt.outHeight*org_opt.outHeight>1024*1024*20) {
                decode_opt.inSampleSize=2;
            }
            Bitmap input_bitmap=BitmapFactory.decodeByteArray(image_file_byte_array, 0, image_file_byte_array.length, decode_opt);
            if (input_bitmap!=null) {
                long decoded_time=System.currentTimeMillis()-b_time;

                int bm_width=0, bm_height=0;
                if (orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_CLOCKWISE_270) ||
                        orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_CLOCKWISE_90) ||
                        orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_CLOCKWISE_90_AND_FLIP_HORIZONTAL) ||
                        orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_CLOCKWISE_270_AND_FLIP_HORIZONTAL) ) {
                    bm_width=input_bitmap.getHeight();
                    bm_height=input_bitmap.getWidth();
                } else {
                    bm_height=input_bitmap.getHeight();
                    bm_width=input_bitmap.getWidth();
                }
                bm_height=input_bitmap.getHeight();
                bm_width=input_bitmap.getWidth();

                float scale=getFitImageSize(bm_width, bm_height);

                int o_h=0, o_w=0;
                o_w=(int)((float)input_bitmap.getWidth()/scale);
                o_h=(int)((float)input_bitmap.getHeight()/scale);

//			Log.v("","s_w="+scale_width+", s_h="+scale_height+", scale="+scale+", o_w="+o_w+", o_h="+o_h);
                Bitmap output_bitmap=null;
                if (scale!=1.0f) {
                    output_bitmap=Bitmap.createScaledBitmap(input_bitmap, o_w, o_h, true);
                    input_bitmap.recycle();
                } else output_bitmap=input_bitmap;
                Bitmap rot_bm=rotateBitmapByPictureOrientation(output_bitmap, orientation);

                log.debug("createPictureFileBitmap Picture bit map created"+
                        ", Display height="+disp_metrics.heightPixels+", width="+disp_metrics.widthPixels+
                        ", Density="+disp_metrics.density+
                        ", Original Bitmap height="+org_opt.outHeight+", width="+org_opt.outWidth+
                        ", Size="+input_bitmap.getByteCount()+
                        ", Scale="+scale+
                        ", Resized Bitmap height="+rot_bm.getHeight()+", width="+rot_bm.getWidth()+", size="+rot_bm.getByteCount()+
                        ", Decode time="+decoded_time+
                        ", Elapsed time="+(System.currentTimeMillis()-b_time)+
                        ", fp="+fp);
                return rot_bm;
            } else {
                log.debug("createPictureFileBitmap Picture dummy bit map created"+
                        ", Elapsed time="+(System.currentTimeMillis()-b_time)+
                        ", fp="+fp);
                return null;
            }
        } else {
            log.debug("createPictureFileBitmap null bm_array"+
                    ", Elapsed time="+(System.currentTimeMillis()-b_time)+
                    ", fp="+fp);
    	    return null;
        }
    };

    private static float getFitImageSize(int bm_width, int bm_height) {
    	float base=2048.0f;
		float o_h=0, o_w=0;
		o_w=((float)bm_width/base);
		o_h=((float)bm_height/base);
		if (Math.max(o_h, o_w)>=1.0f) return Math.max(o_h, o_w);
		else return 1.0f;
    };
    
//	final static private Bitmap loadBitmapFromFile(final GlobalParameters gp, boolean pre_fetch,
//			final String fp, DisplayMetrics disp_metrics, String orientation) {
//    	long b_time=System.currentTimeMillis();
//    	byte[] image_file_byte_array=PictureUtil.createImageByteArray(fp);
//		BitmapFactory.Options options = new BitmapFactory.Options();  
//    	if (image_file_byte_array.length>1024*1024*10) {
////        	options.inPreferredConfig = Bitmap.Config.RGB_565;
//    		options.inSampleSize=2;
//    	}
//		Bitmap input_bitmap=BitmapFactory.decodeByteArray(image_file_byte_array, 0, image_file_byte_array.length, options);
//		long decoded_time=System.currentTimeMillis()-b_time;
////		float base_disp=Math.max(disp_metrics.heightPixels, disp_metrics.widthPixels);
//		
////		float scale_width=(float)input_bitmap.getWidth()/(float)base_disp;
////		float scale_height=(float)input_bitmap.getHeight()/(float)base_disp;
////		float init_scale=Math.min(scale_height, scale_width);
//		
//		@SuppressWarnings("unused")
//		int d_h=0, d_w=0;
//		if (disp_metrics.widthPixels>disp_metrics.heightPixels) {
//			//Landscape
//			d_h=disp_metrics.heightPixels;
//			d_w=disp_metrics.widthPixels;
//		} else {
//			//Portrait
//			d_w=disp_metrics.heightPixels;
//			d_h=disp_metrics.widthPixels;
//		}
//		
//		float scale_width=(float)input_bitmap.getWidth()/(float)d_w;
//		if (orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_CLOCKWISE_270) || 
//				orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_CLOCKWISE_90) ||
//				orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_CLOCKWISE_90_AND_FLIP_HORIZONTAL) ||
//				orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_CLOCKWISE_270_AND_FLIP_HORIZONTAL) ) {
//			scale_width=(float)input_bitmap.getHeight()/(float)d_w;
//		}
////		float scale_height=(float)input_bitmap.getHeight()/(float)d_h;
//		float init_scale=1.0f;//scale_width;//Math.min(scale_height, scale_width);
//
//		float scale=getFitImageSize(init_scale, input_bitmap);
//		
//		int o_h=0, o_w=0;
//		o_w=(int)((float)input_bitmap.getWidth()/scale);
//		o_h=(int)((float)input_bitmap.getHeight()/scale);
//		
////		Log.v("","s_w="+scale_width+", s_h="+scale_height+", scale="+scale+", o_w="+o_w+", o_h="+o_h);
//		Bitmap output_bitmap=null;
//		if (scale!=1.0f) output_bitmap=Bitmap.createScaledBitmap(input_bitmap, o_w, o_h, true);
//		else output_bitmap=input_bitmap;
//    	if (gp.settingDebugLevel>1)  
//    		Log.v(APPLICATION_TAG,"Bitmap created from file"+
//    			", Display height="+disp_metrics.heightPixels+", width="+disp_metrics.widthPixels+//", base="+base_disp+
//    			", Original Bitmap height="+options.outHeight*options.inSampleSize+
//    			", width="+options.outWidth*options.inSampleSize+
//    			", size="+input_bitmap.getByteCount()+
//    			", Initial scale="+init_scale+
//    			", Adjusted scale="+scale+
//    			", Resized Bitmap height="+output_bitmap.getHeight()+", width="+output_bitmap.getWidth()+", size="+output_bitmap.getByteCount()+
//    			", Decode time="+decoded_time+
//    			", Elapsed time="+(System.currentTimeMillis()-b_time)+
//    			", fp="+fp);
//		return output_bitmap;
//    };
//
//    private static float getFitImageSize(float scale, Bitmap o_bm) {
//    	if (scale<1.0) return 1.0f;
//		int o_h=0, o_w=0;
//		o_w=(int)((float)o_bm.getWidth()/scale);
//		o_h=(int)((float)o_bm.getHeight()/scale);
//		if (o_h>2048 || o_w>2048) {
//			scale=getFitImageSize(scale+0.1f, o_bm);
//		}
//		return scale;
//    };

//	final static private Bitmap loadBitmapFromFilex(final GlobalParameters gp, final String fp, DisplayMetrics disp_metrics) {
//    	long b_time=System.currentTimeMillis();
//    	Bitmap bitmap=null;
//    	byte[] image_file_byte_array=PictureUtil.createImageByteArray(fp);
//		BitmapFactory.Options options = new BitmapFactory.Options();  
//		options.inJustDecodeBounds = true;
//		BitmapFactory.decodeByteArray(image_file_byte_array, 0, image_file_byte_array.length, options);
//		int d_h=0, d_w=0;
//		d_h=disp_metrics.heightPixels;
//		d_w=disp_metrics.widthPixels;
//		int scale=0;
//			
//		scale=calculateInSampleSize(options, d_w, d_h);
//		
//		BitmapFactory.Options n_options = new BitmapFactory.Options();  
//		n_options.inSampleSize=scale;
//		bitmap = BitmapFactory.decodeByteArray(image_file_byte_array, 0, image_file_byte_array.length, n_options);
//		if (bitmap.getHeight()>=2048 || bitmap.getWidth()>=2048) {
//			if (gp.settingDebugLevel>1) Log.v(APPLICATION_TAG,"Bitmap max size reached, scale value increaed");
//			n_options.inSampleSize=scale+1;
//			bitmap.recycle();
//			bitmap = BitmapFactory.decodeByteArray(image_file_byte_array, 0, image_file_byte_array.length, n_options);
//		}
//    	if (gp.settingDebugLevel>1)  
//    		Log.v(APPLICATION_TAG,"Bitmap created, "+
//    			"Display height="+d_h+", width="+d_w+
//    			", Original Bitmap height="+options.outHeight+", width="+options.outWidth+
//    			", Scale="+n_options.inSampleSize+
//    			", Resized Bitmap height="+bitmap.getHeight()+", width="+bitmap.getWidth()+
//    			", size="+bitmap.getByteCount()+
//    			", elapsed time="+(System.currentTimeMillis()-b_time)+
//    			", fp="+fp);
//		return bitmap;
//    };

//    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {  
//        int inSampleSize = 1;  
//        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {  
//        	float scale_height=((float)options.outHeight / (float)reqHeight);//+0.5f;//+0.99f;
//        	float scale_width=((float)options.outWidth / (float)reqWidth);//+0.5f;//+0.99f;
//        	
//        	if (scale_height>scale_width) inSampleSize=(int)Math.floor(scale_height);
//        	else inSampleSize=(int)Math.floor(scale_width);
//        	
////        	Log.v("","scale h="+scale_height+", w="+scale_width+", scale="+inSampleSize);
//        }  
//        	
//        return inSampleSize;  
//    };
	
	final static public String getFileExtention(String name) {
		int per_pos=name.lastIndexOf(".");
		if (per_pos > 0) {
			return name.substring(per_pos + 1);//.toLowerCase();
		}
		return "";
	};

    final static public String getFileName(String fp) {
        String file_name="";
        if (fp.lastIndexOf("/")>=0) {
            file_name=fp.substring(fp.lastIndexOf("/")+1);
        } else {
            file_name=fp;
        }
        return file_name;
    };

    static private boolean hasContainedNomediaFile(File lf) {
		boolean result=false;
		File nomedia=new File(lf.getAbsolutePath()+"/.nomedia");
		result=nomedia.exists(); 
		return result;
	};
	
	final static public void getAllPictureFileInDirectory(GlobalParameters gp,
			ArrayList<File>fl, File lf, boolean process_sub_directories) {
//		Log.v("","path="+lf.getAbsolutePath());
		if (lf.exists()) {
			if (gp.settingScanHiddenFile || (!lf.isHidden() && !hasContainedNomediaFile(lf))) {
				if (lf.isDirectory()) {
					if (!isDirectoryToBeExcluded(gp, lf)) {
						File[] cfl=lf.listFiles();
						if (cfl!=null && cfl.length>0) {
							for(File cf:cfl) {
								if (gp.settingScanHiddenFile || !cf.isHidden()) {
//									Log.v("","name1="+cf.getPath()+", hidden="+cf.isHidden());
									if (cf.isDirectory()) {
										if (gp.settingScanHiddenFile || !hasContainedNomediaFile(lf)) {
											if (!cf.getName().equals(".thumbnails")) {
												if (process_sub_directories) 
													getAllPictureFileInDirectory(gp, fl, cf, process_sub_directories);
											}
										} else {
											break;
										}
									} else {
										if (isPictureFile(gp, cf.getName())) fl.add(cf);
									}
								}
							}
						}
					}
				} else {
//					Log.v("","name2="+lf.getPath()+", hidden="+lf.isHidden());
					if (isPictureFile(gp, lf.getPath())) fl.add(lf);
				}
			}
		} 
	};

	static public void getAllPictureDirectoryInDirectory(GlobalParameters gp,
			ArrayList<File>fl, File lf, boolean process_sub_directories) {
		
		if (gp.settingScanHiddenFile || (!lf.isHidden() && !hasContainedNomediaFile(lf))) {
			if (lf.isDirectory()) {
				if (!isDirectoryToBeExcluded(gp, lf)) {
					File[] cfl=lf.listFiles();
					if (cfl!=null && cfl.length>0) {
						boolean pic_file_exist=false;
						for(File cf:cfl) {
							if (gp.settingScanHiddenFile || !cf.isHidden()) {
//								Log.v("","name1="+cf.getPath()+", hidden="+cf.isHidden());
								if (cf.isDirectory()) {
									if (gp.settingScanHiddenFile || !hasContainedNomediaFile(lf)) {
										if (process_sub_directories) 
											getAllPictureDirectoryInDirectory(gp, fl, cf, process_sub_directories);
									} else {
										break;
									}
								} else {
									if (!pic_file_exist && isPictureFile(gp, cf.getPath()))
										pic_file_exist=true;
								}
							}
						}
						if (pic_file_exist) {
							fl.add(lf);
						}
					}
				}
			}
		}
	};

	public static boolean isPictureFile(GlobalParameters gp, String file_name) {
	    if (file_name.endsWith("/.nomedia")) return false;
        if (file_name.endsWith("/.android_secure")) return false;
		String ft=getFileExtention(getFileName(file_name));
		if (!ft.equals("")) {
            for(String sel_type:gp.settingScanFileType) {
                if (ft.equalsIgnoreCase(sel_type)) {
                    return true;
                }
            }
        } else {
//            try {
//                ExifInterface ei=new ExifInterface(file_name);
//                if (ei!=null) return true;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
		return false;
	};

	public static boolean isDirectoryToBeExcluded(GlobalParameters gp, File sel_dir) {
		boolean result=true;
		String fp=sel_dir.getAbsolutePath();
		result=isDirectoryToBeExcluded(gp, fp);
		return result;
	};

	public static boolean isDirectoryToBeExcluded(GlobalParameters gp, String fp) {
		boolean result=false;
		for(ScanFolderItem exc_item:gp.settingScanDirectoryList) {
			if (!exc_item.include) {
//				Log.v("","file="+pic_file.getAbsolutePath()+", e="+exc_item.folder_path);
//				Log.v("","exc_item="+exc_item.folder_path+", fp="+fp);
				if (exc_item.process_sub_directories) {
					if (fp.startsWith(exc_item.folder_path)) {
						result=true;
						break;
					}
				} else {
					if (fp.equals(exc_item.folder_path)) {
						result=true;
						break;
					}
				}
			}
		}
//		Log.v("","exc result="+result);
		return result;
	};

	public static boolean isDirectoryToBeProcessed(GlobalParameters gp, String fp) {
		boolean result=false;
		for(ScanFolderItem inc_item:gp.settingScanDirectoryList) {
			if (inc_item.include) {
//				Log.v("","item="+inc_item.folder_path+", fp="+fp);
				if (inc_item.process_sub_directories) {
					if (fp.startsWith(inc_item.folder_path)) {
						result=true;
						break;
					}
				} else {
					if (fp.equals(inc_item.folder_path)) {
						result=true;
						break;
					}
				}
			}
		}
//		Log.v("","result1="+result);
		if (result) {
			if (isDirectoryToBeExcluded(gp, fp)) result=false;
//			Log.v("","result2="+result);
		}
//		Log.v("","result="+result);
		return result;
	};

	static public Bitmap rotateBitmapByPictureOrientation(Bitmap bitmap, String orientation) {
		Bitmap bmp=bitmap;
		if (!orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_NO_ROTATION)) {
			Matrix mat = new Matrix();
			if (orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_CLOCKWISE_90)) {
				mat.postRotate(90);
			} else if (orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_CLOCKWISE_270)) {
				mat.postRotate(270);
			} else if (orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_FLIP_HORIZONTAL)) {
				mat.preScale(-1.0f, 1.0f);//水平反転
			} else if (orientation.equals(PictureListItem.EXIF_IMAGE_ORIENTATION_FLIP_VERTICAL)) {
				mat.preScale(1.0f, -1.0f);//垂直反転
			}
			bmp=Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
		}
		return bmp;
	};

	static public Bitmap rotateBitmap(Bitmap bitmap, float rotation) {
		Bitmap o_bm=bitmap;
		if (rotation!=0f) {
			Matrix mat = new Matrix();
			mat.postRotate(rotation);
			o_bm=Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
		}
		return o_bm;
	};

	
	final static public byte[] createImageByteArrayWithResize(GlobalParameters gp, boolean debug, int max_width, int image_quality,
			String fp, String orientation) {
		byte[] bm_result=null;
		File lf=new File(fp);
		try {
			FileInputStream fis = new FileInputStream(lf);
			BufferedInputStream bis=new BufferedInputStream(fis,1024*1024*2);
			byte[] bm_file=new byte[(int) lf.length()];
			bis.read(bm_file);
			bis.close();
			
			BitmapFactory.Options imageOptions = new BitmapFactory.Options();
			imageOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(bm_file,0,bm_file.length, imageOptions);

			float imageScale = (float)imageOptions.outWidth / max_width;

		    BitmapFactory.Options imageOptions2 = new BitmapFactory.Options();
		    imageOptions2.inSampleSize = (int)imageScale;
		    Bitmap bitmap = BitmapFactory.decodeByteArray(bm_file,0,bm_file.length, imageOptions2);
		    
		    Bitmap bmp=rotateBitmapByPictureOrientation(bitmap, orientation);
		    if (bmp!=null) {
                ByteArrayOutputStream bos=new ByteArrayOutputStream();
                bmp.compress(CompressFormat.JPEG, image_quality, bos);
                bos.flush();
                bos.close();
                bitmap.recycle();
                bmp.recycle();
                bm_result=bos.toByteArray();
            } else {
                log.debug("BitmapFactory.decodeByteArray failed, fp="+fp);
                bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
                Canvas cv = new Canvas(bmp);
                Paint p = new Paint();
                p.setTextSize(30);
                p.setColor(0xffffffff);
                p.setAntiAlias(true);
                cv.drawText("Unknown file",60f, 100f, p);
                cv.drawText("format",90f, 130f, p);

                ByteArrayOutputStream bos=new ByteArrayOutputStream();
                bmp.compress(CompressFormat.JPEG, image_quality, bos);
                bos.flush();
                bos.close();
                bmp.recycle();
                bm_result=bos.toByteArray();
            }
            log.debug("Image file="+fp+
						", Original Image Size: " + imageOptions.outWidth +
						" x " + imageOptions.outHeight+
						", Scale factor="+imageOptions2.inSampleSize+", bitmap array size="+bm_result.length);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
		}
		return bm_result;
	};

	final static private byte[] createImageByteArray(GlobalParameters gp, String fp) {
//		long b_time=System.currentTimeMillis();
		byte[] bm_result=null;
		File lf=new File(fp);
		try {
			FileInputStream fis = new FileInputStream(lf);
			byte[] bm_file=new byte[(int) lf.length()];
			fis.read(bm_file);
			fis.close();
			bm_result=bm_file;
            log.debug("createImageByteArray result="+bm_result+", fp="+fp);
//			Log.v("","elapsed="+(System.currentTimeMillis()-b_time)+", name="+fp);
		} catch (IOException e) {
            log.debug("createImageByteArray error="+e.getMessage()+", fp="+fp);
//			e.printStackTrace();
		}
		return bm_result;
	};
	
	final static public String createPictureInfo(Context c, PictureListItem pfli) {
//       Geocoder gc=new Geocoder(c);
//       String address="";
//       try {
//			List<Address> addr=gc.getFromLocation(pfli.getExifGpsLatitude(), pfli.getExifGpsLongitude(), 1);
//			if (!addr.isEmpty()){
//				address=addr.get(0).getAddressLine(1);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		String exif_image_info="";
		
		if (pfli.getExifImageHeight()!=0) {
			String image_size=String.format(
					c.getString(R.string.msgs_main_exif_image_info_size), pfli.getExifImageHeight(), 
					pfli.getExifImageWidth()).concat(", ");
			
			String aperture=pfli.getExifAperture().equals("")?"":
				String.format(c.getString(R.string.msgs_main_exif_image_info_aperture),pfli.getExifAperture()).concat(", ");
			
			String exposure_time=pfli.getExifExposureTime().equals("")?"":
				String.format(c.getString(R.string.msgs_main_exif_image_info_exposure_time),pfli.getExifExposureTime()).concat(", ");

			String exposure_bias="";//Exposure mode=manual

			String exposure_mode="";
			if (pfli.getExifExposureMode().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_MODE_MANUAL)) exposure_mode=c.getString(R.string.msgs_main_exif_image_info_exposure_mode_manual).concat(", ");
			else if (pfli.getExifExposureMode().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_MODE_BRACKET)) exposure_mode=c.getString(R.string.msgs_main_exif_image_info_exposure_mode_bracket).concat(", ");

			String exposure_program="";
			if (!pfli.getExifExposureMode().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_MODE_MANUAL)) {
				exposure_bias=pfli.getExifExposureBias().equals("")?"":
					String.format(c.getString(R.string.msgs_main_exif_image_info_exposure_bias),pfli.getExifExposureBias()).concat(", ");
				
				if (pfli.getExifExposureProgram().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_PROGRAM_MANUAL)) exposure_program=c.getString(R.string.msgs_main_exif_image_info_exposure_program_manual).concat(", ");
				else if (pfli.getExifExposureProgram().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_PROGRAM_NORMAL_PROGRAM)) exposure_program=c.getString(R.string.msgs_main_exif_image_info_exposure_program_normal).concat(", ");
				else if (pfli.getExifExposureProgram().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_PROGRAM_APERTURE_PRIORITY)) exposure_program=c.getString(R.string.msgs_main_exif_image_info_exposure_program_aperture).concat(", ");
				else if (pfli.getExifExposureProgram().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_PROGRAM_SHUTTER_PRIORITY)) exposure_program=c.getString(R.string.msgs_main_exif_image_info_exposure_program_shutter).concat(", ");
				else if (pfli.getExifExposureProgram().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_PROGRAM_CREATIVE_PROGRAM)) exposure_program=c.getString(R.string.msgs_main_exif_image_info_exposure_program_creative).concat(", ");
				else if (pfli.getExifExposureProgram().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_PROGRAM_ACTION_PROGRAM)) exposure_program=c.getString(R.string.msgs_main_exif_image_info_exposure_program_action).concat(", ");
				else if (pfli.getExifExposureProgram().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_PROGRAM_PORTRAIT_MODE)) exposure_program=c.getString(R.string.msgs_main_exif_image_info_exposure_program_portrait).concat(", ");
				else if (pfli.getExifExposureProgram().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_PROGRAM_LANDSCAPE_MODE)) exposure_program=c.getString(R.string.msgs_main_exif_image_info_exposure_program_landscape).concat(", ");
				else if (pfli.getExifExposureMode().equals(PictureListItem.EXIF_IMAGE_EXPOSURE_MODE_AUTO)) exposure_program=c.getString(R.string.msgs_main_exif_image_info_exposure_mode_auto).concat(", ");				
			}

			String focal_length=pfli.getExifFocalLength().equals("")?"":
				String.format(c.getString(R.string.msgs_main_exif_image_info_focal_length),pfli.getExifFocalLength()).concat(", ");
			
			String iso=pfli.getExifISO().equals("")?"":String.format(
				String.format(c.getString(R.string.msgs_main_exif_image_info_iso), pfli.getExifISO())).concat(", ");
			
			String date_time=pfli.getExifDateTime().equals("0000/00/00 00:00:00")?
				c.getString(R.string.msgs_main_exif_image_info_date_time_unknown):
				String.format(c.getString(R.string.msgs_main_exif_image_info_date_time),pfli.getExifDateTime()).concat(", ");
			
			String dc_desc=pfli.getExifXmpDcDescription().equals("")?"":
				String.format(c.getString(R.string.msgs_main_exif_image_info_dc_description), pfli.getExifXmpDcDescription()).concat(", ");
			
			String dc_subj="";
			if (pfli.getExifXmpDcSubject()!=null) {
				String sep="", subj="";
				for(String item:pfli.getExifXmpDcSubject()) {
					subj+=sep+item;
					sep=", ";
				}
				dc_subj=String.format(c.getString(R.string.msgs_main_exif_image_info_dc_subject), subj).concat(", ");
			}

			String dc_creator=pfli.getExifXmpDcCreator().equals("")?"":
				String.format(c.getString(R.string.msgs_main_exif_image_info_dc_creator), pfli.getExifXmpDcCreator()).concat(", ");
			String xmp_label=pfli.getExifXmpXmpLabel().equals("")?"":
				String.format(c.getString(R.string.msgs_main_exif_image_info_xmp_label), pfli.getExifXmpXmpLabel()).concat(", ");
			String xmp_rating=pfli.getExifXmpXmpRating().equals("")?"":
				String.format(c.getString(R.string.msgs_main_exif_image_info_xmp_rating), pfli.getExifXmpXmpRating()).concat(", ");

//			Log.v("","mk="+pfli.getExifMaker());
			String no_of_shutter_released="";
			no_of_shutter_released=pfli.getExifNumberOfShutterRelased()>0?
					String.format(c.getString(R.string.msgs_main_exif_number_of_shutter_released), pfli.getExifNumberOfShutterRelased()).concat(", "):
						"";
			
			exif_image_info=String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s", image_size, 
					date_time, aperture, exposure_time, exposure_bias,  
					focal_length, iso, exposure_mode, exposure_program, 
					no_of_shutter_released, pfli.getExifModel().concat(", "),
					dc_desc, dc_subj, dc_creator, xmp_label, xmp_rating
					).trim();
			int cnt=0;
			while(exif_image_info.endsWith(",")) {
				exif_image_info=exif_image_info.substring(0, exif_image_info.length()-1).trim();
				cnt++;
				if (cnt>10) break;
			}
		}
		
		return exif_image_info;
	};
	
	public static String invokeWallPaperEditor(Context c, String pic_file_path) {
		File file = new File(pic_file_path);
//		ContentResolver cr = c.getContentResolver();
//		ContentValues cv = new ContentValues();
//		cv.put(MediaStore.Images.Media.TITLE, file.getName());
//		cv.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
//		cv.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
//		cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
//		cv.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
//		Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
		
		String result="";
		Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
//        if (Build.VERSION.SDK_INT>=26) {
//            Uri uri= FileProvider.getUriForFile(c, BuildConfig.APPLICATION_ID + ".provider", file);
//            intent.setDataAndType(uri, "image/*");
//        } else {
//            intent.setDataAndType(Uri.fromFile(file), "image/*");
//        }
        intent.setDataAndType(Uri.fromFile(file), "image/*");
	    try {
		    c.startActivity(intent);
	    } catch(Exception e) {
//	    	e.printStackTrace();
	    	result=e.getMessage();
	    }
//	    Log.v("","msg="+result);
	    return result;
	};

	public static Bitmap sharpen(Bitmap src, double weight, double factor, double offset) {
	    double[][] SharpConfig = new double[][] {
	        { 0 , -2    , 0  },
	        { -2, weight, -2 },
	        { 0 , -2    , 0  }
	    };
	    ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
	    convMatrix.applyConfig(SharpConfig);
	    convMatrix.Factor = factor;//weight - 8;
	    convMatrix.Offset = offset;
	    return ConvolutionMatrix.computeConvolution3x3(src, convMatrix);
	}
}
