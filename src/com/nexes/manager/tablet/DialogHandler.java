package com.nexes.manager.tablet;

import android.app.DialogFragment;
import android.os.Bundle;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import java.util.ArrayList;
import java.io.File;

public class DialogHandler extends DialogFragment {
	public static final int HOLDINGFILE_DIALOG = 	0X01;
	public static final int SEARCHRESULT_DIALOG = 	0x02;
	public static final int FILEINFO_DIALOG =		0x03;
	
	private static DialogHandler instance = null;
	private static int mDialogType;
	private static Context mContext;
	
	private ArrayList<String> mFiles;
	
	
	public static DialogHandler newDialog(int type, Context context) {
		instance = new DialogHandler();
		mDialogType = type;
		mContext = context;
		
		return instance;
	}
	
	public void setHoldingFileList(ArrayList<String> list) {
		mFiles = list;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		switch(mDialogType) {
		case HOLDINGFILE_DIALOG:
			setStyle(DialogFragment.STYLE_NO_TITLE, 
					 android.R.style.Theme_Holo_Dialog);
			break;
		case SEARCHRESULT_DIALOG:
			setStyle(DialogFragment.STYLE_NORMAL, 
					 android.R.style.Theme_Holo_Dialog);
			break;
			
		case FILEINFO_DIALOG:
			setStyle(DialogFragment.STYLE_NORMAL, 
					 android.R.style.Theme_Holo_Dialog);
			break;
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		switch(mDialogType) {
		case HOLDINGFILE_DIALOG:
			return createHoldingFileDialog();
			
		case SEARCHRESULT_DIALOG:
			return createSearchResultDialog();
			
		case FILEINFO_DIALOG:
			return createFileInfoDialog();
			
		}
				
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	
	private View createHoldingFileDialog() {
		getDialog().getWindow().setGravity(Gravity.LEFT | Gravity.TOP);

		ListView list = new ListView(mContext);
		list.setAdapter(new DialogListAdapter(mContext, R.layout.dir_list_layout, mFiles));
		
		return list;
	}
	
	private View createSearchResultDialog() {
		return null;
	}
	
	private View createFileInfoDialog() {
		View v = null;
		
		return v;
	}
	
	/*
	 * 
	 */
	private class DialogListAdapter extends ArrayAdapter<String> {
		DataViewHolder mHolder;
		
		public DialogListAdapter(Context context, int layout, ArrayList<String> data) {
			super(context, layout, data);
		}
		
		@Override
		public View getView(int position, View view, ViewGroup parent) {
			String ext;
			String file = mFiles.get(position);
			String name = file.substring(file.lastIndexOf("/") + 1, file.length());
			
			
			if (view == null) {
				LayoutInflater inflater = (LayoutInflater)mContext
											.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.dir_list_layout, parent, false);
				
				mHolder = new DataViewHolder();
				mHolder.mIcon = (ImageView)view.findViewById(R.id.list_icon);
				mHolder.mMainText = (TextView)view.findViewById(R.id.list_name);
				
				view.setTag(mHolder);
				
			} else {
				mHolder = (DataViewHolder)view.getTag();
			}
			
			if (new File(file).isDirectory())
				ext = "dir";
			else
				ext = file.substring(file.lastIndexOf(".") + 1);
			
			mHolder.mMainText.setText(name);
			
			if(ext.equalsIgnoreCase("dir")) {	
				mHolder.mIcon.setImageResource(R.drawable.folder_md);
				
			} else if(ext.equalsIgnoreCase("doc") || ext.equalsIgnoreCase("docx")) {
				mHolder.mIcon.setImageResource(R.drawable.doc_md);
				
			} else if(ext.equalsIgnoreCase("xls")  || 
					  ext.equalsIgnoreCase("xlsx") ||
					  ext.equalsIgnoreCase("xlsm")) {
				mHolder.mIcon.setImageResource(R.drawable.excel_md);
				
			} else if(ext.equalsIgnoreCase("ppt") || ext.equalsIgnoreCase("pptx")) {
				mHolder.mIcon.setImageResource(R.drawable.powerpoint_md);
				
			} else if(ext.equalsIgnoreCase("zip") || ext.equalsIgnoreCase("gzip")) {
				mHolder.mIcon.setImageResource(R.drawable.zip_md);
				
			} else if(ext.equalsIgnoreCase("apk")) {
				mHolder.mIcon.setImageResource(R.drawable.apk_md);
				
			} else if(ext.equalsIgnoreCase("pdf")) {
				mHolder.mIcon.setImageResource(R.drawable.pdf_md);
				
			} else if(ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("html")) {
				mHolder.mIcon.setImageResource(R.drawable.xml_html_md);
				
			} else if(ext.equalsIgnoreCase("mp4") || 
					  ext.equalsIgnoreCase("3gp") ||
					  ext.equalsIgnoreCase("webm") || 
					  ext.equalsIgnoreCase("m4v")) {
				mHolder.mIcon.setImageResource(R.drawable.movie_md);
				
			} else if(ext.equalsIgnoreCase("mp3") || ext.equalsIgnoreCase("wav") ||
					  ext.equalsIgnoreCase("wma") || ext.equalsIgnoreCase("m4p") ||
					  ext.equalsIgnoreCase("m4a") || ext.equalsIgnoreCase("ogg")) {
				mHolder.mIcon.setImageResource(R.drawable.music_md);
				
			} else if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png") ||
					  ext.equalsIgnoreCase("jpg")  || ext.equalsIgnoreCase("gif")) {
				mHolder.mIcon.setImageResource(R.drawable.photo_md);
				
			} else {
				mHolder.mIcon.setImageResource(R.drawable.unknown_md);
			}
			
			return view;
		}
	}
}
