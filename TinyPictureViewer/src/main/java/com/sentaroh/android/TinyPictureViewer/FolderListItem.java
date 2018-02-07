package com.sentaroh.android.TinyPictureViewer;

import static com.sentaroh.android.TinyPictureViewer.Constants.*;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class FolderListItem implements Externalizable, Comparable<FolderListItem>{
	private static final long serialVersionUID = SERIALIZABLE_VERSION_CODE;
	private String folder_name="";
	private String parent_directory="";
	private boolean selected=false;
	private boolean enabled=true;
	
	private long file_last_modified=0l;
	public void setFileLastModified(long time) {file_last_modified=time;}
	public long getFileLastModified() {return file_last_modified;}
	
	private int no_of_pictures=0;
	
	private boolean always_top=false;
	private int sort_position=Integer.MAX_VALUE;
	
//	private ArrayList<PictureListItem> pic_list=null;
	
	public FolderListItem() {};
	
	public FolderListItem(int sort_key, int sort_order) {
		this.sort_key=sort_key;
		this.sort_order=sort_order;
	};
	
//	public void setPictureList(ArrayList<PictureListItem>list) {
//		if (pic_list==null) pic_list=new ArrayList<PictureListItem>();
//		pic_list.clear();
//		pic_list.addAll(list);
//	};
	
	private int sort_order=SORT_ORDER_ASCENDANT;
	private int sort_key=SORT_KEY_THUMBNAIL_FILE_NAME;
	
	public void setSortOrder(int order) {sort_order=order;}
	public int getSortOrder() {return sort_order;}

	public void setSortKey(int key) {sort_key=key;}
	public int getSortKey() {return sort_key;}

//	public ArrayList<PictureListItem> getPictureList() {return pic_list;}
//	public void setPictureList(ArrayList<PictureListItem>pl) {pic_list=pl;}
	
	public void setNoOfPictures(int count) {no_of_pictures=count;}
	public int getNoOfPictures() {return no_of_pictures;}
	
	private byte[] thumbnailByteArray=null;
	private String thumbnailFilePath="";
	public byte[] getThumbnailArray() {return thumbnailByteArray;}
	public void setThumbnailArray(byte[] p) {thumbnailByteArray=p;}
	public String getThumbnailFilePath() {return thumbnailFilePath;}
	public void setThumbnailFilePath(String p) {thumbnailFilePath=p;}

	public String getFolderName() {return folder_name;}
	public void setFolderName(String p) {folder_name=p;}

	public String getParentDirectory() {return parent_directory;}
	public void setParentDirectory(String p) {parent_directory=p;}

	public void setSelected(boolean p) {selected=p;};
	public boolean isSelected() {return selected;}

	public void setEnabled(boolean p) {enabled=p;};
	public boolean isEnabled() {return enabled;}
	
	public void setAlwaysTop(boolean top) {always_top=top;}
	public boolean isAlwaysTop() {return always_top;}
	
	public void setSortPosition(int position) {sort_position=position;}
	public int getSortPosition() {return sort_position;}

	@Override
	public int compareTo(FolderListItem another) {
		if (this.getFolderName().equals(another.getFolderName())) {
			return this.getParentDirectory().compareToIgnoreCase(another.getParentDirectory());
		} else {
			return this.getFolderName().compareToIgnoreCase(another.getFolderName());
		}
	};
	
	@Override
	public void readExternal(ObjectInput input) throws IOException,
			ClassNotFoundException {
		if (input.readLong()!=serialVersionUID) 
			throw new IOException("serialVersionUID was not matched by saved UID");
		
		folder_name=input.readUTF();
		parent_directory=input.readUTF();
		thumbnailByteArray=PictureUtil.readArrayByte(input);
		thumbnailFilePath=input.readUTF();
		file_last_modified=input.readLong();
		sort_order=input.readInt();
		sort_key=input.readInt();
		no_of_pictures=input.readInt();
		always_top=input.readBoolean();
		sort_position=input.readInt();
	};
	
	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeLong(serialVersionUID);
		
		output.writeUTF(folder_name);
		output.writeUTF(parent_directory);
		PictureUtil.writeArrayByte(output, thumbnailByteArray);
		output.writeUTF(thumbnailFilePath);
		output.writeLong(file_last_modified);
		output.writeInt(sort_order);
		output.writeInt(sort_key);
		output.writeInt(no_of_pictures);
		output.writeBoolean(always_top);
		output.writeInt(sort_position);
	};
	
};
