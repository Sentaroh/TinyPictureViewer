package com.sentaroh.android.TinyPictureViewer;

import static com.sentaroh.android.TinyPictureViewer.Constants.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Log;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.properties.XMPPropertyInfo;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.makernotes.NikonType1MakernoteDirectory;
import com.drew.metadata.exif.makernotes.NikonType2MakernoteDirectory;
import com.drew.metadata.xmp.XmpDescriptor;
import com.drew.metadata.xmp.XmpDirectory;
import com.sentaroh.android.Utilities.SerializeUtil;
import com.sentaroh.android.Utilities.StringUtil;

public class PictureListItem implements Externalizable, Comparable<PictureListItem>{
	private static final long serialVersionUID = SERIALIZABLE_VERSION_CODE;
	private String pictureFileParentDirectory=null;
	private String pictureFolderName=null;
	private String pictureFileName=null;
	private long pictureFileLastModified=0;
	private long pictureFileLength=0;

	private boolean thumbnailVerified=false;
	
	private byte[] thumbnailByteArray=null;
//	private byte[] resizedByteArray=null;
	
	private int exif_image_height=-1, exif_image_width=-1;
	private String exif_image_aperture="";
	private String exif_image_date_time="";
	private String exif_image_exposure_time="";
	private String exif_image_exposure_bias="";
	private String exif_image_exposure_mode="";
	final static public String EXIF_IMAGE_EXPOSURE_MODE_AUTO="0";
	final static public String EXIF_IMAGE_EXPOSURE_MODE_MANUAL="1";
	final static public String EXIF_IMAGE_EXPOSURE_MODE_BRACKET="2";
	
	private String exif_image_exposure_program="";
//			0 = Not defined
//			1 = Manual
//			2 = Normal program
//			3 = Aperture priority
//			4 = Shutter priority
//			5 = Creative program (biased toward depth of field)
//			6 = Action program (biased toward fast shutter speed)
//			7 = Portrait mode (for closeup photos with the background out of focus)
//			8 = Landscape mode (for landscape photos with the background in focus) 
	final static public String EXIF_IMAGE_EXPOSURE_PROGRAM_MANUAL="1";
	final static public String EXIF_IMAGE_EXPOSURE_PROGRAM_NORMAL_PROGRAM="2";
	final static public String EXIF_IMAGE_EXPOSURE_PROGRAM_APERTURE_PRIORITY="3";
	final static public String EXIF_IMAGE_EXPOSURE_PROGRAM_SHUTTER_PRIORITY="4";
	final static public String EXIF_IMAGE_EXPOSURE_PROGRAM_CREATIVE_PROGRAM="5";
	final static public String EXIF_IMAGE_EXPOSURE_PROGRAM_ACTION_PROGRAM="6";
	final static public String EXIF_IMAGE_EXPOSURE_PROGRAM_PORTRAIT_MODE="7";
	final static public String EXIF_IMAGE_EXPOSURE_PROGRAM_LANDSCAPE_MODE="8";
	
	private String exif_image_focal_length="";
	private String exif_image_iso="";
	private String exif_image_make="";
	private String exif_image_model="";
	
	private int exif_number_of_shutter_released=-1;

	public int getExifNumberOfShutterRelased() {return exif_number_of_shutter_released;}
	public void setExifExifNumberOfShutterRelased(int no) {exif_number_of_shutter_released=no;}
	
	private String exif_image_orientation="";
    final static public String EXIF_IMAGE_ORIENTATION_NO_ROTATION="1";//そのまま
    final static public String EXIF_IMAGE_ORIENTATION_FLIP_HORIZONTAL="2";//水平反転
  	final static public String EXIF_IMAGE_ORIENTATION_FLIP_VERTICAL="3";//180度回転
  	final static public String EXIF_IMAGE_ORIENTATION_FLIP_VERTICAL_AND_HORIZONTAL="4";//180度回転て水平反転
  	final static public String EXIF_IMAGE_ORIENTATION_CLOCKWISE_90_AND_FLIP_HORIZONTAL="5";//時計回りに90度回転して水平反転
    final static public String EXIF_IMAGE_ORIENTATION_CLOCKWISE_90="6";//時計回りに90度回転
    final static public String EXIF_IMAGE_ORIENTATION_CLOCKWISE_270_AND_FLIP_HORIZONTAL="7";//時計回りに270度回転して水平反転
    final static public String EXIF_IMAGE_ORIENTATION_CLOCKWISE_270="8";//時計回りに270度回転
    
    private String[] exif_xmp_dc_subject=null;
    private String exif_xmp_dc_description="";
    private String exif_xmp_dc_creator="";
    private String exif_xmp_xmp_label="";
    private String exif_xmp_xmp_rating="";

	public String[] getExifXmpDcSubject() {return exif_xmp_dc_subject;}
	public void setExifXmpDcSubject(String[] tags) {exif_xmp_dc_subject=tags;}

	public String getExifXmpDcDescription() {return exif_xmp_dc_description;}
	public void setExifXmpDcDescription(String desc) {exif_xmp_dc_description=desc==null?"":desc;}

	public String getExifXmpDcCreator() {return exif_xmp_dc_creator;}
	public void setExifXmpDcCreator(String desc) {exif_xmp_dc_creator=desc==null?"":desc;}

	public String getExifXmpXmpLabel() {return exif_xmp_xmp_label;}
	public void setExifXmpXmpLabel(String label) {exif_xmp_xmp_label=label==null?"":label;}

	public String getExifXmpXmpRating() {return exif_xmp_xmp_rating;}
	public void setExifXmpXmpRating(String rating) {exif_xmp_xmp_rating=rating==null?"":rating;}

	public String getExifExposureMode() {return exif_image_exposure_mode;}
	public void setExifExposureMode(String mode) {exif_image_exposure_mode=mode==null?"":mode;}

	public String getExifExposureProgram() {return exif_image_exposure_program;}
	public void setExifExposureProgram(String program) {exif_image_exposure_program=program==null?"":program;}

	public String getExifImageOrientation() {return exif_image_orientation;}
	public void setExifImageOrientation(String orientation) {exif_image_orientation=orientation==null?"":orientation;}
	
	private double exif_image_longitude=0L, exif_image_latitude=0L;

	public double getExifGpsLongitude() {return exif_image_longitude;}
	public void setExifGpsLongitude(double longitude) {exif_image_longitude=longitude;}
	
	public double getExifGpsLatitude() {return exif_image_latitude;}
	public void setExifGpsLatitude(double altitude) {exif_image_latitude=altitude;}
	
	public boolean isThumbnailVerified() {return thumbnailVerified;}
	public void setThumbnailVerified(boolean p) {thumbnailVerified=p;}
	
	public byte[] getThumbnailImageByte() {return thumbnailByteArray;}
	public void setThumbnailImageByte(byte[] p) {thumbnailByteArray=p;}
	
//	public byte[] getResizedImageByte() {return resizedByteArray;}
//	public void setResizedImageByte(byte[] p) {resizedByteArray=p;}
	
	public String getParentDirectory() {return pictureFileParentDirectory;}
	public void setParentDirectory(String p) {pictureFileParentDirectory=p;}

	public String getFolderName() {return pictureFolderName;}
	public void setFolderName(String p) {pictureFolderName=p;}
	
	public String getFileName() {return pictureFileName;}
	public void setFileName(String p) {pictureFileName=p;}

	public long getFileLastModified() {return pictureFileLastModified;}
	public void setFileLastModified(long p) {pictureFileLastModified=p;}
	
	public long getFileLength() {return pictureFileLength;}
	public void setFileLength(long p) {pictureFileLength=p;}

	public int getExifImageHeight() {return exif_image_height;}
	public void setExifImageHeight(int p) {exif_image_height=p;}
	public int getExifImageWidth() {return exif_image_width;}
	public void setExifImageWidth(int p) {exif_image_width=p;}

	public String getExifAperture() {return exif_image_aperture;}
	public void setExifAperture(String p) {exif_image_aperture=(p==null)?"":p;}

	public String getExifExposureTime() {return exif_image_exposure_time;}
	public void setExifExposureTime(String p) {exif_image_exposure_time=(p==null)?"":p;}

	public String getExifExposureBias() {return exif_image_exposure_bias;}
	public void setExifExposureBias(String p) {exif_image_exposure_bias=(p==null)?"":p;}

	public String getExifFocalLength() {return exif_image_focal_length;}
	public void setExifFocalLength(String p) {exif_image_focal_length=(p==null)?"":p;}

	public String getExifISO() {return exif_image_iso;}
	public void setExifISO(String p) {exif_image_iso=(p==null)?"":p;}

	public String getExifMaker() {return exif_image_make;}
	public void setExifMaker(String p) {exif_image_make=(p==null)?"":p;}

	public String getExifModel() {return exif_image_model;}
	public void setExifModel(String p) {exif_image_model=(p==null)?"":p;}

	public String getExifDateTime() {return exif_image_date_time;}
	public void setExifDateTime(String p) {exif_image_date_time=(p==null)?"":p;}

	private boolean selected=false;
	public boolean isSelected() {return selected;}
	public void setSelected(boolean p) {selected=p;}
	
	private boolean enabled=true;
	public void setEnabled(boolean p) {enabled=p;};
	public boolean isEnabled() {return enabled;}

	
	private boolean debug_enabled=false;

	private GlobalParameters mGp=null;

	public PictureListItem(boolean debug, File pic_file) {
        mGp=null;
		debug_enabled=debug;
		createFileInfo(pic_file);
	};

	public PictureListItem(boolean debug) {
        mGp=null;
		debug_enabled=debug;
	};

	public PictureListItem() {
        mGp=GlobalWorkArea.getGlobalParameters(null);
	};

	@SuppressLint("SimpleDateFormat")
	public void createFileInfo(File pic_file) {
		
//		long b_time=System.currentTimeMillis();
		
		setFileName(pic_file.getName());
		setParentDirectory(createParentDirectory(pic_file));
		setFolderName(createFolderName(pic_file));
		setFileLastModified(pic_file.lastModified());
		setFileLength(pic_file.length());
		
		try {
			setSpecificExifData(pic_file);
		} catch(Exception e) {
			e.printStackTrace();
		}

		if (getExifDateTime().equals("")) {
			try {
				ExifInterface ei=new ExifInterface(pic_file.getAbsolutePath());
				if (ei!=null) {
					String dt=ei.getAttribute(ExifInterface.TAG_DATETIME);
					if (dt!=null) {
						if (dt.indexOf("+")>=0) {
					       SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
							try {
								Date date = df.parse(dt);
								String strDate = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(date);
								String strTime = new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(date);
								setExifDateTime(strDate+" "+strTime);
							} catch (ParseException e) {
								e.printStackTrace();
							}
						} else {
							try {
								String[] date_time=dt.split(" ");
								setExifDateTime(date_time[0].replaceAll(":","/")+" "+date_time[1]);
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (getExifDateTime().equals("")) {
				setExifDateTime(StringUtil.convDateTimeTo_YearMonthDayHourMinSec(pic_file.lastModified()));
			}
		}

//		try {
//			ExifInterface ei;
//			ei = new ExifInterface(pic_file.getAbsolutePath());
//			if (ei!=null) {
//				setExifDateTime(ei.getAttribute(ExifInterface.TAG_DATETIME));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

//		try {
//			Metadata metaData;
//			metaData = ImageMetadataReader.readMetadata(pic_file);
//			ExifSubIFDDirectory directory=null;
//			if (metaData!=null) {
//				directory=metaData.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
//				if (directory!=null) {
//					String date = directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
////					setExifDateTime(date==null?"":StringUtil.convDateTimeTo_YearMonthDayHourMinSec(date.getTime()));
//					setExifDateTime(date);
//				}
//			}
//		} catch (ImageProcessingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

//		createExifInfo(pic_file.getAbsolutePath());
		
//		if (debug_enabled) Log.v("","PictureFileListItem "+"elapsed="+(System.currentTimeMillis()-b_time)+
//				", created name="+getFileName()+
//				", folder name="+getFolderName()+", parentDirectory="+getParentDirectory());
	};

    private byte[] readExifData(BufferedInputStream bis, int read_size) throws IOException {
        byte[] buff=new byte[read_size];
        int rc=bis.read(buff,0,read_size);
        if (rc>0) return buff;
        else return null;
    }

    private void setSpecificExifData(File pic_file) {
        String[] result=null;
        try {
            FileInputStream fis=new FileInputStream(pic_file);
            BufferedInputStream bis=new BufferedInputStream(fis, 1024*32);
            byte[] buff=readExifData(bis, 2);
            if (buff!=null && buff[0]==(byte)0xff && buff[1]==(byte)0xd8) {//if jpeg header
                while(buff!=null) {// find dde1 jpeg segemnt
                    buff=readExifData(bis, 4);
                    if (buff!=null) {
                        if (buff[0]==(byte)0xff && buff[1]==(byte)0xe1) {
                            int seg_size=getIntFrom2Byte(false, buff[2], buff[3]);
                            buff=readExifData(bis, 14);
                            boolean little_endian=false;
                            if (buff[6]==(byte)0x49 && buff[7]==(byte)0x49) little_endian=true;
                            int ifd_offset=getIntFrom4Byte(little_endian, buff[10], buff[11], buff[12], buff[13]);

                            byte[] ifd_buff=new byte[seg_size+ifd_offset];
                            System.arraycopy(buff,6,ifd_buff,0,8);
                            buff=readExifData(bis, seg_size);
                            System.arraycopy(buff,0,ifd_buff,8,seg_size);
                            process0thIfdTag(little_endian, ifd_buff, ifd_offset);
                            break;
                        } else {
                            int offset=((int)buff[2]&0xff)*256+((int)buff[3]&0xff)-2;
                            buff=readExifData(bis, offset);
                        }
                    } else {
                        return ;
                    }
                }

            }
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

//    private void setSpecificExifData(File pic_file) {
//		try {
//			FileInputStream fis=new FileInputStream(pic_file);
//			int buf_sz=1024*4;
//			byte[] buff=new byte[buf_sz];
//			fis.read(buff);
//			fis.close();
////			Log.v("",StringUtil.getDumpFormatHexString(buff, 0, 512));
//			if (buff[0]==(byte)0xff && buff[1]==(byte)0xd8) {//if jpeg header
////				Log.v("","foud jpeg header");
//				int i=2;
//				while(i<buf_sz-3) {// find dde1 jpeg segemnt
////					Log.v("",StringUtil.getDumpFormatHexString(buff, i, 2));
//					if (buff[i]==(byte)0xff && buff[i+1]==(byte)0xe1) {
//						//found jpeg segment
////						Log.v("","foud jpeg segement");
//
//						int seg_size=getIntFrom2Byte(false, buff[i+2], buff[i+3]);
////						String exif_id=new String(buff,i+4,6);
//						boolean little_endian=false;
////						Log.v("",StringUtil.getDumpFormatHexString(buff, i+10, 2));
//						if (buff[i+10]==(byte)0x49 && buff[i+11]==(byte)0x49) little_endian=true;
////						int tiff_ver=getIntFrom2Byte(false, buff[i+12], buff[i+13]);
//						int ifd_offset=getIntFrom4Byte(little_endian, buff[i+14], buff[i+15], buff[i+16], buff[i+17]);
//
////						Log.v("","seg="+seg_size+", id="+exif_id+",　little="+little_endian+", tiff="+tiff_ver+", ifd offset="+ifd_offset);
////						Log.v("",StringUtil.getDumpFormatHexString(buff, i+10+ifd_offset, 256));
//
//						byte[] ifd_buff=Arrays.copyOfRange(buff, i+10, seg_size+ifd_offset);
//						process0thIfdTag(little_endian, ifd_buff, ifd_offset);
//						break;
//					} else {
//						int offset=((int)buff[i+2]&0xff)*256+((int)buff[i+3]&0xff);
//						i=offset+i+2;
//					}
//				}
//
//			}
//		} catch (IOException e) {
////			e.printStackTrace();
//		}
//	};
	
	static private int getIntFrom2Byte(boolean little_endian, byte b1, byte b2) {
		int result=0;
		if (little_endian) result=((int)b2&0xff)*256+((int)b1&0xff);
		else result=((int)b1&0xff)*256+((int)b2&0xff);
		return result;
	};

	static private int getIntFrom4Byte(boolean little_endian, byte b1, byte b2, byte b3, byte b4) {
		int result=0;
		if (little_endian) result=((int)b4&0xff)*65536+((int)b3&0xff)*4096+((int)b2&0xff)*256+((int)b1&0xff);
		else result=((int)b1&0xff)*65536+((int)b2&0xff)*4096+((int)b3&0xff)*256+((int)b4&0xff);
		return result;
	};

	private void process0thIfdTag(boolean little_endian, byte[]ifd_buff, int ifd_offset) {
		int count=getIntFrom2Byte(little_endian, ifd_buff[ifd_offset+0], ifd_buff[ifd_offset+1]);
		int i=0;
		int ba=ifd_offset+2;
		while(i<count) {
//			Log.v("",StringUtil.getDumpFormatHexString(ifd_buff, ba, 12));
			int tag_number=getIntFrom2Byte(little_endian, ifd_buff[ba+0], ifd_buff[ba+1]);
//			int tag_type=getIntFrom2Byte(little_endian, ifd_buff[ba+2], ifd_buff[ba+3]);
//			int tag_count=getIntFrom4Byte(little_endian, ifd_buff[ba+4], ifd_buff[ba+5], ifd_buff[ba+6], ifd_buff[ba+7]);
			int tag_offset=getIntFrom4Byte(little_endian, ifd_buff[ba+8], ifd_buff[ba+9], ifd_buff[ba+10], ifd_buff[ba+11]);
			
//			Log.v("",String.format("tag_number=%x, type=%x, count=%d, offset=%x",
//					tag_number, tag_type, tag_count, tag_offset));
			
			if (tag_number==(0x8769&0xffff)) {
				processExifIfdTag(little_endian, ifd_buff, tag_offset);
				break;
			}
			
//			Log.v("",StringUtil.getDumpFormatHexString(ifd_buff, tag_offset, 512));
			ba+=12;
			i++;
		}
	}

	private void processExifIfdTag(boolean little_endian, byte[]ifd_buff, int ifd_offset) {
		int count=getIntFrom2Byte(little_endian, ifd_buff[ifd_offset+0], ifd_buff[ifd_offset+1]);
		int i=0;
		int ba=ifd_offset+2;
		while(i<count) {
//			Log.v("",StringUtil.getDumpFormatHexString(ifd_buff, ba, 12));
			int tag_number=getIntFrom2Byte(little_endian, ifd_buff[ba+0], ifd_buff[ba+1]);
//			int tag_type=getIntFrom2Byte(little_endian, ifd_buff[ba+2], ifd_buff[ba+3]);
//			int tag_count=getIntFrom4Byte(little_endian, ifd_buff[ba+4], ifd_buff[ba+5], ifd_buff[ba+6], ifd_buff[ba+7]);
			int tag_offset=getIntFrom4Byte(little_endian, ifd_buff[ba+8], ifd_buff[ba+9], ifd_buff[ba+10], ifd_buff[ba+11]);
			
//			Log.v("",String.format("tag_number=%x, type=%x, count=%d, offset=%x",
//					tag_number, tag_type, tag_count, tag_offset));
			if (tag_number==(0x9003&0xffff)) {
				String[] date = new String(ifd_buff, tag_offset, 19).split(" ");
				if (date.length==2) {
					setExifDateTime(date[0].replaceAll(":", "/")+" "+date[1]);
					break;
				}
//				Log.v("",StringUtil.getDumpFormatHexString(ifd_buff, tag_offset, 20));
			}

//			Log.v("",StringUtil.getDumpFormatHexString(ifd_buff, tag_offset, 512));
			ba+=12;
			i++;
		}
	}

	static public String createParentDirectory(File pic_file) {
		return pic_file.getParent();
	};
	static public String createFolderName(File pic_file) {
		return pic_file.getParent().lastIndexOf("/")<1?"":pic_file.getParent().substring(pic_file.getParent().lastIndexOf("/")+1);
	};
	
	public byte[] verifyAndCorrectThumbnail(byte[] thumbnail_array, String fp, String orientation) {
		byte[] result=null;
		if (thumbnail_array!=null) {
			result=thumbnail_array;
//			Log.v("","o="+orientation);
			if (!orientation.equals(EXIF_IMAGE_ORIENTATION_NO_ROTATION)) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				Bitmap org_bitmap=BitmapFactory.decodeByteArray(thumbnail_array, 0, thumbnail_array.length, options);
				Bitmap out_bitmap=PictureUtil.rotateBitmapByPictureOrientation(org_bitmap, orientation);
				try {
					ByteArrayOutputStream baos=new ByteArrayOutputStream();
					out_bitmap.compress(CompressFormat.JPEG, 20, baos);
					baos.flush();
					baos.close();
					result=baos.toByteArray();
				} catch (IOException e) {
//					e.printStackTrace();
				}
				out_bitmap.recycle();
				org_bitmap.recycle();
			}
		} else {
			result=PictureUtil.createImageByteArrayWithResize(mGp, debug_enabled, 512, 20, fp, orientation);
		}
		setThumbnailVerified(true);
		return result;
	};

	final public void createExifInfo(String fp) {
		try {
			File lf=new File(fp);
			Metadata metaData=ImageMetadataReader.readMetadata(lf);
			ExifSubIFDDirectory directory=null;
			if (metaData!=null) directory=metaData.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			if (directory!=null) {
				editExifInfoByMetaDataExtractor(true, metaData, directory, fp);
			} else {
				editExifInfoByExifInterface(true, fp);
			}
			if (this.getExifDateTime().equals("")) {
				setExifDateTime(StringUtil.convDateTimeTo_YearMonthDayHourMinSec(lf.lastModified()));
//				setExifDateTime("0000/00/00 00:00:00");
			}
		} catch (IOException e) {
//			e.printStackTrace();
		} catch (ImageProcessingException e) {
//			e.printStackTrace();
		}
	};

	final public void createExifInfoWithoutThumbnail(String fp) {
		File lf=new File(fp);
		createExifInfoWithoutThumbnail(lf);
	};

	final public void createExifInfoWithoutThumbnail(File lf) {
		try {
			Metadata metaData=ImageMetadataReader.readMetadata(lf);
			ExifSubIFDDirectory directory=null;
			if (metaData!=null) directory=metaData.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			if (directory!=null) {
				editExifInfoByMetaDataExtractor(false, metaData, directory, lf.getAbsolutePath());
			} else {
				editExifInfoByExifInterface(false, lf.getAbsolutePath());
			}
//			if (this.getExifDateTime().equals("")) {
////				setExifDateTime(StringUtil.convDateTimeTo_YearMonthDayHourMinSec(0));
//				setExifDateTime("0000/00/00 00:00:00");
//			}
		} catch (IOException e) {
//			e.printStackTrace();
		} catch (ImageProcessingException e) {
//			e.printStackTrace();
		}
	};

	@SuppressLint("SimpleDateFormat")
	@SuppressWarnings("deprecation")
	final private void editExifInfoByExifInterface(boolean createThumbnail, String fp) throws IOException {
//		Log.v("","fp="+fp);
		ExifInterface ei=new ExifInterface(fp);
		if (ei!=null) {
			setExifImageHeight(ei.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1));
			setExifImageWidth(ei.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1));
			
			setExifAperture(ei.getAttribute(ExifInterface.TAG_APERTURE));
			if (getExifDateTime().equals("") || getExifDateTime().startsWith("0000")) {
				String dt=ei.getAttribute(ExifInterface.TAG_DATETIME);
				if (dt!=null) {
					if (dt.indexOf("+")>=0) {
				       SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
						try {
							Date date = df.parse(dt);
							String strDate = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(date);
							String strTime = new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(date);
							setExifDateTime(strDate+" "+strTime);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else {
						try {
							String[] date_time=dt.split(" ");
							setExifDateTime(date_time[0].replaceAll(":","/")+" "+date_time[1]);
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
//			Log.v("","ss="+ei.getAttribute(ExifInterface.TAG_EXPOSURE_TIME));
			if (ei.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)!=null) {
				float ss=Float.parseFloat(ei.getAttribute(ExifInterface.TAG_EXPOSURE_TIME));
				setExifExposureTime("1/"+String.format("%.0f", 1.0f/ss));
			}
			
			if (ei.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)!=null) {
				String[]fl=ei.getAttribute(ExifInterface.TAG_FOCAL_LENGTH).split("/");
				setExifFocalLength(fl[0]);
			}
			setExifISO(ei.getAttribute(ExifInterface.TAG_ISO));
			setExifMaker(ei.getAttribute(ExifInterface.TAG_MAKE));
			setExifModel(ei.getAttribute(ExifInterface.TAG_MODEL));
			if (ei.getAttribute(ExifInterface.TAG_GPS_LATITUDE)!=null) {
				String longitude_ref=ei.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
				String latitude_ref=ei.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
				String longitude_val=ei.getAttribute(ExifInterface.TAG_GPS_LONGITUDE).replace(",", " ");
				String latitude_val=ei.getAttribute(ExifInterface.TAG_GPS_LATITUDE).replace(",", " ");
				setGpsInfo(latitude_val, latitude_ref, longitude_val, longitude_ref);
			}
			setExifImageOrientation(ei.getAttribute(ExifInterface.TAG_ORIENTATION));
			if (createThumbnail) setThumbnailImageByte(verifyAndCorrectThumbnail(ei.getThumbnail(),fp, getExifImageOrientation()));
		}
	};

	private void setGpsInfo(String latitude_val, String latitude_ref, String longitude_val, String longitude_ref) {
		String[]longitude_part=longitude_val.split(" ");
		if (longitude_part.length>=3) {
            String[]longitude_d=longitude_part[0].split("/");
            String[]longitude_m=longitude_part[1].split("/");
            String[]longitude_s=longitude_part[2].split("/");
            String[]latitude_part=latitude_val.split(" ");
            String[]latitude_d=latitude_part[0].split("/");
            String[]latitude_m=latitude_part[1].split("/");
            String[]latitude_s=latitude_part[2].split("/");
            if (longitude_d!=null && longitude_m!=null && longitude_s!=null &&
                    latitude_d!=null && latitude_m!=null && latitude_s!=null && longitude_ref!=null && latitude_ref!=null) {
                Double longitude=Double.parseDouble(longitude_d[0])/Double.parseDouble(longitude_d[1])+
                        Double.parseDouble(longitude_m[0])/Double.parseDouble(longitude_m[1])/60d+
                        Double.parseDouble(longitude_s[0])/Double.parseDouble(longitude_s[1])/3600d;
                Double latitude=Double.parseDouble(latitude_d[0])/Double.parseDouble(latitude_d[1])+
                        Double.parseDouble(latitude_m[0])/Double.parseDouble(latitude_m[1])/60d+
                        Double.parseDouble(latitude_s[0])/Double.parseDouble(latitude_s[1])/3600d;
                if (longitude_ref.equals("W")) setExifGpsLongitude(-1d*longitude);
                else setExifGpsLongitude(longitude);
                if (latitude_ref.equals("S")) setExifGpsLatitude(-1d*latitude);
                else setExifGpsLatitude(latitude);
            }
        }
	}
	
	final private void editExifInfoByMetaDataExtractor(boolean createThumbnail, 
			Metadata metaData, ExifSubIFDDirectory directory, String fp) {

		ExifIFD0Directory ifdDirectory = metaData.getFirstDirectoryOfType(ExifIFD0Directory.class);
		ArrayList<ExifThumbnailDirectory> exif_thumbnail = (ArrayList<ExifThumbnailDirectory>) metaData.getDirectoriesOfType(ExifThumbnailDirectory.class);
		ArrayList<GpsDirectory> gps = (ArrayList<GpsDirectory>) metaData.getDirectoriesOfType(GpsDirectory.class);
		ArrayList<XmpDirectory> xmp = (ArrayList<XmpDirectory>) metaData.getDirectoriesOfType(XmpDirectory.class);

		if (xmp.size()>0) {
			XMPMeta xmpMeta = xmp.get(0).getXMPMeta();
			ArrayList<String>subject_list=new ArrayList<String>();
			try {
			    XMPIterator itr=xmpMeta.iterator();
			    while (itr.hasNext()) {
			        XMPPropertyInfo pi = (XMPPropertyInfo) itr.next();
//			        Log.v("","pi="+pi.getPath()+", v="+pi.getValue());
			        if (pi!=null && pi.getPath()!=null && pi.getValue()!=null) {
				        if (pi.getPath().startsWith("dc:subject") && !pi.getValue().equals("")) {
//					        Log.v("","pi="+pi.getPath()+", v="+pi.getValue());
				        	subject_list.add(pi.getValue());
				        }
				        if (pi.getPath().startsWith("dc:description") && pi.getPath().indexOf("/")==-1 && !pi.getValue().equals("")) {
//					        Log.v("","pi="+pi.getPath()+", v="+pi.getValue());
				        	setExifXmpDcDescription(pi.getValue());
				        }
				        if (pi.getPath().startsWith("dc:creator") && !pi.getValue().equals("")) {
//					        Log.v("","pi="+pi.getPath()+", v="+pi.getValue());
				        	setExifXmpDcCreator(pi.getValue());
				        }
				        if (pi.getPath().startsWith("xmp:Label") && !pi.getValue().equals("")) {
//					        Log.v("","pi="+pi.getPath()+", v="+pi.getValue());
				        	setExifXmpXmpLabel(pi.getValue());
				        }
				        if (pi.getPath().startsWith("xmp:Rating") && !pi.getValue().equals("")) {
//					        Log.v("","pi="+pi.getPath()+", v="+pi.getValue());
				        	setExifXmpXmpRating(pi.getValue());
				        }
			        }
			    }
			} catch (XMPException e1) {
				e1.printStackTrace();
			}
			if (subject_list.size()>0) {
				String[] subj_array=new String[subject_list.size()];
				int cnt=0;
				for(String item:subject_list) {
					subj_array[cnt]=item;
					cnt++;
//					Log.v("","item="+tag_array[cnt]);
				}
				setExifXmpDcSubject(subj_array);
			}
		}
//		Collection<Tag> xmp_tags=xmp.get(0).getTags();
//		Iterator iterator = xmp_tags.iterator();
//		while(iterator.hasNext()) {
//		    Tag tag=(Tag) iterator.next();
//		    Log.v("","tag="+tag.toString()+", desc="+tag.getDescription());
//		}
		
//		if (xmp.size()>0) Log.v("","xmp="+xmp.get(0).getTags().toString());
//		if (xmp.size()>0) Log.v("","exif="+exif_thumbnail.get(0).getTags().toString());
//		if (xmp.size()>0) Log.v("","directory="+directory.getTags().toString());
		
//		ArrayList<NikonType2MakernoteDirectory> mkn=(ArrayList<NikonType2MakernoteDirectory>) metaData.getDirectoriesOfType(NikonType2MakernoteDirectory.class);
//		if (mkn.size()>0) Log.v("","mkn="+mkn.get(0).getTags().toString());
//		ArrayList<CanonMakernoteDirectory> mkc=(ArrayList<CanonMakernoteDirectory>) metaData.getDirectoriesOfType(CanonMakernoteDirectory.class);
//		if (mkc.size()>0) Log.v("","mkn="+mkc.get(0).getTags().toString());
		
		if (gps!=null && gps.size()>0) {
			if (gps.get(0).getString(GpsDirectory.TAG_LONGITUDE)!=null) {
				String longitude_ref=gps.get(0).getString(GpsDirectory.TAG_LONGITUDE_REF);
				String latitude_ref=gps.get(0).getString(GpsDirectory.TAG_LATITUDE_REF);
				String longitude_val=gps.get(0).getString(GpsDirectory.TAG_LONGITUDE);
				String latitude_val=gps.get(0).getString(GpsDirectory.TAG_LATITUDE);
				setGpsInfo(latitude_val, latitude_ref, longitude_val, longitude_ref);
			}
		}

		setExifAperture(directory.getString(ExifSubIFDDirectory.TAG_FNUMBER));
		if (getExifDateTime().equals("") || getExifDateTime().startsWith("0000")) {
			Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
			setExifDateTime(date==null?"":StringUtil.convDateTimeTo_YearMonthDayHourMinSec(date.getTime()));
		}
		try {
			setExifExposureTime("1/"+(int)Math.floor(1/directory.getDouble(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)));
		} catch (MetadataException e) {
//			e.printStackTrace();
		}
		try {
			setExifExposureBias(String.format("%1$.1f",directory.getDouble(ExifSubIFDDirectory.TAG_EXPOSURE_BIAS)));
		} catch (MetadataException e) {
//			e.printStackTrace();
		}

		
		setExifExposureMode(directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_MODE));
		setExifExposureProgram(directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_PROGRAM));
		
		setExifFocalLength(directory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
		setExifISO(directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
		if (ifdDirectory!=null) {
			setExifMaker(ifdDirectory.getString(ExifIFD0Directory.TAG_MAKE));
			setExifModel(ifdDirectory.getString(ExifIFD0Directory.TAG_MODEL));
			setExifImageOrientation(ifdDirectory.getString(ExifIFD0Directory.TAG_ORIENTATION));
		}

//        for (Tag tag : directory.getTags()) {
//            System.out.format("[%s] %s = %s", tag.getDirectoryName(), tag.getTagName(), tag.getDescription());
//            System.out.println();
//            if (tag.getTagName().startsWith("Lens")) {
//                Log.v("","lens="+tag.getDescription());
//            }
//        }

//        for (Directory dir : metaData.getDirectories()) {
//            for (Tag tag : directory.getTags()) {
//                System.out.format("[%s] %s = %s",
//                        tag.getDirectoryName(), tag.getTagName(), tag.getDescription());
//                System.out.println();
//            }
//        }

//		if (xmp.size()>0) {
//		    for(XmpDirectory item:xmp) {
//                XmpDescriptor descriptor3 = new XmpDescriptor(item);
//                XMPMeta xmpMeta = item.getXMPMeta();
//                XMPIterator itr = null;
//                try {
//                    itr = xmpMeta.iterator();
//                } catch (XMPException e) {
//                    e.printStackTrace();
//                }
//
//                // Iterate XMP properties
//                while (itr.hasNext()) {
//
//                    XMPPropertyInfo property = (XMPPropertyInfo) itr.next();
//
//                    // Print details of the property
//                    System.out.println(property.getPath() + ": " + property.getValue());
//                }            }
//        }
        try {
			setExifImageHeight(directory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT));
			setExifImageWidth(directory.getInt(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH));
		} catch (MetadataException e) {
			e.printStackTrace();
			getImageSizeFromBitmap(fp);
//			return;
		}

		if (createThumbnail) {
//            long b_time=System.currentTimeMillis();

            byte[] bm_me=(exif_thumbnail.size()>0)?exif_thumbnail.get(0).getThumbnailData():null;
            setThumbnailImageByte(verifyAndCorrectThumbnail(bm_me,fp, getExifImageOrientation()));

//            byte[] bm_me=null;
//            if (exif_thumbnail.size()>0) {
//                try {
//                    ExifInterface ei=new ExifInterface(fp);
//                    setThumbnailImageByte(verifyAndCorrectThumbnail(ei.getThumbnail(),fp, getExifImageOrientation()));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            setThumbnailImageByte(verifyAndCorrectThumbnail(bm_me,fp, getExifImageOrientation()));
//             Log.v("","elapsed="+(System.currentTimeMillis()-b_time));
		}
		
		if (getExifMaker().toUpperCase().startsWith("NIKON")) {
			ArrayList<NikonType2MakernoteDirectory> nikon_mn_t2 = (ArrayList<NikonType2MakernoteDirectory>) metaData.getDirectoriesOfType(NikonType2MakernoteDirectory.class);
			if (nikon_mn_t2!=null) {
//				Log.v("","tag="+nikon_mn_t2.get(0).getTags().toString());
				for(int i=0;i<nikon_mn_t2.size();i++) {
					try {
						int nsr=nikon_mn_t2.get(i).getInt(NikonType2MakernoteDirectory.TAG_EXPOSURE_SEQUENCE_NUMBER);
//                        String lens=nikon_mn_t2.get(i).getString(NikonType2MakernoteDirectory.TAG_LENS);
//                        String l_date=nikon_mn_t2.get(i).getString(130);//NikonType2MakernoteDirectory.TAG_LENS_DATA);
//                        String l_type=nikon_mn_t2.get(i).getString(NikonType2MakernoteDirectory.TAG_LENS_TYPE);
						if (nsr!=0) setExifExifNumberOfShutterRelased(nsr);
					} catch (MetadataException e) {
						e.printStackTrace();
					}
				}
			}
		}

//		Log.v("","maker="+getExifMaker());
//		if (getExifMaker().toUpperCase().startsWith("CANON")) {
//			ArrayList<CanonMakernoteDirectory> mn = (ArrayList<CanonMakernoteDirectory>) metaData.getDirectoriesOfType(CanonMakernoteDirectory.class);
//			if (mn!=null) {
//				Log.v("","tag="+mn.get(0).getTags().toString());
//				for(int i=0;i<mn.size();i++) {
//					try {
//						int nsr=mn.get(i).getInt(CanonMakernoteDirectory.TAG_CANON_IMAGE_NUMBER);
//						if (nsr!=0) setExifExifNumberOfShutterRelased(nsr);
//					} catch (MetadataException e) {
////						e.printStackTrace();
//					}
//				}
//			}
//		}

	};

	final private void getImageSizeFromBitmap(String fp) {
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

			setExifImageHeight(imageOptions.outHeight);
			setExifImageWidth(imageOptions.outWidth);


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

	@Override
	final public void readExternal(ObjectInput objin) throws IOException,
			ClassNotFoundException {
		if (objin.readLong()!=serialVersionUID) 
			throw new IOException("serialVersionUID was not matched by saved UID");
		pictureFileParentDirectory=objin.readUTF();
		pictureFolderName=objin.readUTF();
		pictureFileName=objin.readUTF();
		pictureFileLastModified=objin.readLong();
		pictureFileLength=objin.readLong();

		thumbnailVerified=objin.readBoolean();
		
		thumbnailByteArray=PictureUtil.readArrayByte(objin);
//		resizedByteArray=readArrayByte(objin);

		exif_image_height=objin.readInt();
		exif_image_width=objin.readInt();
		exif_image_aperture=objin.readUTF();
		exif_image_date_time=objin.readUTF();
		exif_image_exposure_time=objin.readUTF();
		exif_image_exposure_mode=objin.readUTF();
		exif_image_exposure_program=objin.readUTF();
		exif_image_focal_length=objin.readUTF();
		exif_image_iso=objin.readUTF();
		exif_image_make=objin.readUTF();
		exif_image_model=objin.readUTF();
		exif_image_exposure_bias=objin.readUTF();
		exif_image_longitude=objin.readDouble();
		exif_image_latitude=objin.readDouble();
		exif_image_orientation=objin.readUTF();
		
		exif_xmp_dc_subject=SerializeUtil.readArrayString(objin);
		exif_xmp_dc_description=objin.readUTF();
		
	    exif_xmp_dc_creator=objin.readUTF();
	    exif_xmp_xmp_label=objin.readUTF();
	    exif_xmp_xmp_rating=objin.readUTF();
	    
	    exif_number_of_shutter_released=objin.readInt();

	}
	@Override
	final public void writeExternal(ObjectOutput objout) throws IOException {
		objout.writeLong(serialVersionUID);
		objout.writeUTF(pictureFileParentDirectory);
		objout.writeUTF(pictureFolderName);
		objout.writeUTF(pictureFileName);
		objout.writeLong(pictureFileLastModified);
		objout.writeLong(pictureFileLength);
		
		objout.writeBoolean(thumbnailVerified);
		
		PictureUtil.writeArrayByte(objout, thumbnailByteArray);
//		writeArrayByte(objout, resizedByteArray);
		
		objout.writeInt(exif_image_height);
		objout.writeInt(exif_image_width);
		objout.writeUTF(exif_image_aperture);
		objout.writeUTF(exif_image_date_time);
		objout.writeUTF(exif_image_exposure_time);
		objout.writeUTF(exif_image_exposure_mode);
		objout.writeUTF(exif_image_exposure_program);
		objout.writeUTF(exif_image_focal_length);
		objout.writeUTF(exif_image_iso);
		objout.writeUTF(exif_image_make);
		objout.writeUTF(exif_image_model);
		objout.writeUTF(exif_image_exposure_bias);
		objout.writeDouble(exif_image_longitude);
		objout.writeDouble(exif_image_latitude);
		objout.writeUTF(exif_image_orientation);
		
		SerializeUtil.writeArrayString(objout, exif_xmp_dc_subject);
		objout.writeUTF(exif_xmp_dc_description);
		
		objout.writeUTF(exif_xmp_dc_creator);
		objout.writeUTF(exif_xmp_xmp_label);
		objout.writeUTF(exif_xmp_xmp_rating);

		objout.writeInt(exif_number_of_shutter_released);
	}
	
	@Override
	public int compareTo(PictureListItem another) {
		if (this.getParentDirectory().equals(another.getParentDirectory())) {
			return this.getFileName().compareToIgnoreCase(another.getFileName());
		} else {
			return this.getParentDirectory().compareToIgnoreCase(another.getParentDirectory());
		}
	}
}

