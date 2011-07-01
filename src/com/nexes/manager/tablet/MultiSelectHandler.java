package com.nexes.manager.tablet;

import android.content.Context;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.io.File;

public class MultiSelectHandler {
	private static MultiSelectHandler mInstance = null;
	private static Context mContext;
	private static LayoutInflater mInflater;
	private static ArrayList<String> mFileList = null;
	private View view;
	
	public static MultiSelectHandler getInstance(Context context) {
		//make this cleaner
		if(mInstance == null)
			mInstance = new MultiSelectHandler();
		if(mFileList == null)
			mFileList = new ArrayList<String>();
		
		mContext = context;
		mInflater = (LayoutInflater)mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return mInstance;
	}
	
	public View addFile(String file) {	
		if(mFileList.contains(file))
			return null;
		
		view = mInflater.inflate(R.layout.multiselect_layout, null);

		ImageView image = (ImageView)view.findViewById(R.id.multi_icon);
		TextView text = (TextView)view.findViewById(R.id.multi_text);
		String ext = "";
		
		if(new File(file).isDirectory()) {
			text.setText(file.substring(file.lastIndexOf("/") + 1, file.length()));
			ext = "dir";
		} else {
			text.setText(file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf(".")));
			ext = file.substring(file.lastIndexOf(".") + 1, file.length());
		}
	
		setImage(ext, image);
		mFileList.add(file);
		
		return view;
	}
	
	public ArrayList<String> getSelectedFiles() {
		return mFileList;
	}
	
	public int clearFileEntry(String path) {
		int index = mFileList.indexOf(path);
		
		if(index > -1)
			mFileList.remove(index);
		
		return index;
	}
	
	public void cancelMultiSelect() {
		mFileList.clear();
		mFileList = null;
		mInstance = null;		
	}
	
	private void setImage(String extension, ImageView image) {
		if(extension.equalsIgnoreCase("dir")) {
			image.setImageResource(R.drawable.folder_md);
		
		} else if(extension.equalsIgnoreCase("doc") || 
				  extension.equalsIgnoreCase("docx")) {
			image.setImageResource(R.drawable.doc_md);
			
		} else if(extension.equalsIgnoreCase("xls")  || 
				  extension.equalsIgnoreCase("xlsx") ||
				  extension.equalsIgnoreCase("xlsm")) {
			image.setImageResource(R.drawable.excel_md);
			
		} else if(extension.equalsIgnoreCase("ppt") || 
				  extension.equalsIgnoreCase("pptx")) {
			image.setImageResource(R.drawable.powerpoint_md);
			
		} else if(extension.equalsIgnoreCase("zip") || 
				  extension.equalsIgnoreCase("gzip")) {
			image.setImageResource(R.drawable.zip_md);
			
		} else if(extension.equalsIgnoreCase("rar")) {
			image.setImageResource(R.drawable.rar_md);
			
		} else if(extension.equalsIgnoreCase("apk")) {
			image.setImageResource(R.drawable.apk_md);
			
		} else if(extension.equalsIgnoreCase("pdf")) {
			image.setImageResource(R.drawable.pdf_md);
			
		} else if(extension.equalsIgnoreCase("xml") || 
				  extension.equalsIgnoreCase("html")) {
			image.setImageResource(R.drawable.xml_html_md);
			
		} else if(extension.equalsIgnoreCase("mp4") || extension.equalsIgnoreCase("3gp") ||
				extension.equalsIgnoreCase("webm")  || extension.equalsIgnoreCase("m4v")) {
			image.setImageResource(R.drawable.movie_md);
			
		} else if(extension.equalsIgnoreCase("mp3") || extension.equalsIgnoreCase("wav") ||
				extension.equalsIgnoreCase("wma")   || extension.equalsIgnoreCase("m4p") ||
				extension.equalsIgnoreCase("m4a")   || extension.equalsIgnoreCase("ogg")) {
			image.setImageResource(R.drawable.music_md);
			
		} else if(extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("png") ||
				extension.equalsIgnoreCase("jpg")    || extension.equalsIgnoreCase("gif")) {
			image.setImageResource(R.drawable.photo_md);
			
		} else {
			image.setImageResource(R.drawable.unknown_md);
		}
	}
}
