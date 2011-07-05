/*
	Open Manager For Tablets, an open source file manager for the Android system
    Copyright (C) 2011  Joe Berria <nexesdevelopment@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.nexes.manager.tablet;

import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.io.File;

public class DialogHandler extends DialogFragment {
	public static final int HOLDINGFILE_DIALOG = 	0X01;
	public static final int SEARCHRESULT_DIALOG = 	0x02;
	public static final int FILEINFO_DIALOG =		0x03;
	
	private static DialogHandler instance = null;
	private static int mDialogType;
	private static Context mContext;
	
	private OnSearchFileSelected mSearchListener;
	private ArrayList<String> mFiles;
	private String mPath;
	
	
	public interface OnSearchFileSelected {
		public void onFileSelected(String fileName);
	}
	
	public static DialogHandler newDialog(int type, Context context) {
		instance = new DialogHandler();
		mDialogType = type;
		mContext = context;
		
		return instance;
	}
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
			
		switch(mDialogType) {
		case HOLDINGFILE_DIALOG:
			setStyle(DialogFragment.STYLE_NORMAL,
					 android.R.style.Theme_Holo_Dialog);
			break;
		case SEARCHRESULT_DIALOG:
			setStyle(DialogFragment.STYLE_NO_TITLE, 
					 android.R.style.Theme_Holo_Panel);
			break;
			
		case FILEINFO_DIALOG:
			setStyle(DialogFragment.STYLE_NO_FRAME, 
					 android.R.style.Theme_Holo_Panel);
			break;
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		switch(mDialogType) {
		case HOLDINGFILE_DIALOG:  	return createHoldingFileDialog();
		case SEARCHRESULT_DIALOG: 	return createSearchResultDialog(inflater);
		case FILEINFO_DIALOG:		return createFileInfoDialog(inflater);
		}

		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	public void setHoldingFileList(ArrayList<String> list) {
		mFiles = list;
	}
	
	public void setFilePath(String path) {
		mPath = path;
	}
	
	public void setOnSearchFileSelected(OnSearchFileSelected s) {
		mSearchListener = s;
	}
	
	private View createHoldingFileDialog() {
		getDialog().getWindow().setGravity(Gravity.LEFT | Gravity.TOP);
		getDialog().setTitle("Holding " + mFiles.size() + " files");
		
		ListView list = new ListView(mContext);
		list.setAdapter(new DialogListAdapter(mContext, R.layout.dir_list_layout, mFiles));

		return list;
	}
	
	private View createSearchResultDialog(LayoutInflater inflater) {
		getDialog().getWindow().setGravity(Gravity.RIGHT);
		
		final View v = inflater.inflate(R.layout.search_grid, null);
		final Button launch_button = (Button)v.findViewById(R.id.search_button_open);
		final Button goto_button = (Button)v.findViewById(R.id.search_button_go);
		final LinearLayout layout = (LinearLayout)v.findViewById(R.id.search_button_view);
		layout.setBackgroundColor(0xee444444);

		ListView list = (ListView)v.findViewById(R.id.search_listview);
		list.setBackgroundColor(0xcc000000);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				final File selected = new File(mFiles.get(position));
				
				if (layout.getVisibility() == View.GONE)
					layout.setVisibility(View.VISIBLE);
				
				goto_button.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						mSearchListener.onFileSelected(selected.getPath());
						dismiss();
					}
				});
				
				if (!selected.isDirectory()) {
					launch_button.setVisibility(View.VISIBLE);
					launch_button.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							String item_ext = "";
															
							try {
								item_ext = selected.getName().substring(selected.getName().lastIndexOf("."));
								
							} catch (StringIndexOutOfBoundsException e) {
								item_ext = "";
							}
							
							/*audio files*/
							if (item_ext.equalsIgnoreCase(".mp3") || 
								item_ext.equalsIgnoreCase(".m4a") ) {
					    		
					    		Intent i = new Intent();
				   				i.setAction(android.content.Intent.ACTION_VIEW);
				   				i.setDataAndType(Uri.fromFile(selected), "audio/*");
				   				startActivity(i);
							}
							
							/* image files*/
							else if(item_ext.equalsIgnoreCase(".jpeg") || 
					    			item_ext.equalsIgnoreCase(".jpg")  ||
					    			item_ext.equalsIgnoreCase(".png")  ||
					    			item_ext.equalsIgnoreCase(".gif")  || 
					    			item_ext.equalsIgnoreCase(".tiff")) {

								Intent picIntent = new Intent();
						    		picIntent.setAction(android.content.Intent.ACTION_VIEW);
						    		picIntent.setDataAndType(Uri.fromFile(selected), "image/*");
						    		startActivity(picIntent);
					    	}
							
							/*video file selected--add more video formats*/
					    	else if(item_ext.equalsIgnoreCase(".m4v") ||
					    			item_ext.equalsIgnoreCase(".mp4") ||
					    			item_ext.equalsIgnoreCase(".3gp") ||
					    			item_ext.equalsIgnoreCase(".wmv") || 
					    			item_ext.equalsIgnoreCase(".mp4") || 
					    			item_ext.equalsIgnoreCase(".ogg") ||
					    			item_ext.equalsIgnoreCase(".wav")) {
					    		
				    				Intent movieIntent = new Intent();
						    		movieIntent.setAction(android.content.Intent.ACTION_VIEW);
						    		movieIntent.setDataAndType(Uri.fromFile(selected), "video/*");
						    		startActivity(movieIntent);	
					    	}
							
							/*pdf file selected*/
					    	else if(item_ext.equalsIgnoreCase(".pdf")) {
					    		
					    		if(selected.exists()) {
						    		Intent pdfIntent = new Intent();
						    		pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
						    		pdfIntent.setDataAndType(Uri.fromFile(selected), "application/pdf");
							    		
						    		try {
						    			startActivity(pdfIntent);
						    		} catch (ActivityNotFoundException e) {
						    			Toast.makeText(mContext, "Sorry, couldn't find a pdf viewer", 
												Toast.LENGTH_SHORT).show();
						    		}
						    	}
					    	}
							
							/*Android application file*/
					    	else if(item_ext.equalsIgnoreCase(".apk")){
					    		
					    		if(selected.exists()) {
					    			Intent apkIntent = new Intent();
					    			apkIntent.setAction(android.content.Intent.ACTION_VIEW);
					    			apkIntent.setDataAndType(Uri.fromFile(selected), 
					    									 "application/vnd.android.package-archive");
					    			startActivity(apkIntent);
					    		}
					    	}
							
							/* HTML XML file */
					    	else if(item_ext.equalsIgnoreCase(".html") || 
					    			item_ext.equalsIgnoreCase(".xml")) {
					    		
					    		if(selected.exists()) {
					    			Intent htmlIntent = new Intent();
					    			htmlIntent.setAction(android.content.Intent.ACTION_VIEW);
					    			htmlIntent.setDataAndType(Uri.fromFile(selected), "text/html");
					    			
					    			try {
					    				startActivity(htmlIntent);
					    			} catch(ActivityNotFoundException e) {
					    				Toast.makeText(mContext, "Sorry, couldn't find a HTML viewer", 
					    									Toast.LENGTH_SHORT).show();
						    			
					    			}
					    		}
					    	}
														
							/* text file*/
					    	else if(item_ext.equalsIgnoreCase(".txt")) {
				    			Intent txtIntent = new Intent();
				    			txtIntent.setAction(android.content.Intent.ACTION_VIEW);
				    			txtIntent.setDataAndType(Uri.fromFile(selected), "text/plain");
				    			
				    			try {
				    				startActivity(txtIntent);
				    			} catch(ActivityNotFoundException e) {
				    				txtIntent.setType("text/*");
				    				startActivity(txtIntent);
				    			}
					    	}
							
							/* generic intent */
					    	else {
					    		if(selected.exists()) {
						    		Intent generic = new Intent();
						    		generic.setAction(android.content.Intent.ACTION_VIEW);
						    		generic.setDataAndType(Uri.fromFile(selected), "application/*");
						    		
						    		try {
						    			startActivity(generic);
						    		} catch(ActivityNotFoundException e) {
						    			Toast.makeText(mContext, "Sorry, couldn't find anything " +
						    						   "to open " + selected.getName(), 
						    						   Toast.LENGTH_SHORT).show();
							    	}
					    		}
					    	}
							
							dismiss();
						}
					});
					
				} else {
					launch_button.setVisibility(View.INVISIBLE);
				}
				
				populateFileInfoViews(v, selected);
			}
		});
		list.setAdapter(new DialogListAdapter(mContext, R.layout.dir_list_layout, mFiles));
		
		return v;
	}
	
	private View createFileInfoDialog(LayoutInflater inflater) {
		File file = new File(mPath);
		View v = inflater.inflate(R.layout.info_layout, null);
		v.setBackgroundColor(0xcc000000);
		
		populateFileInfoViews(v, file);
		
		return v;
	}
	
	private String formatSize(long size) {
		int kb = 1024;
		int mb = kb * 1024;
		int gb = mb * 1024;
		String ssize = "";
		
		if (size < kb)
			ssize = String.format("%.2f bytes", (double)size);
		else if (size > kb && size < mb)
			ssize = String.format("%.2f Kb", (double)size / kb);
		else if (size > mb && size < gb)
			ssize = String.format("%.2f Mb", (double)size / mb);
		else if(size > gb)
			ssize = String.format("%.2f Gb", (double)size / gb);
		
		return ssize;
	}
	
	private void populateFileInfoViews(View v, File file) {
		int dirCount = 0;
		int fileCount = 0;
		String apath = file.getPath();
		File files[] = file.listFiles();
		Date date = new Date(file.lastModified());
		
		TextView numDir = (TextView)v.findViewById(R.id.info_dirs_label);
		TextView numFile = (TextView)v.findViewById(R.id.info_files_label);
		
		if (file.isDirectory()) {
			files = file.listFiles();
			
			if (files != null) {
				for(File f : files)
					if (f.isDirectory())
						dirCount++;
					else
						fileCount++;
			}
			
			if (fileCount == 0)
				numFile.setText("-");
			else
				numFile.setText("" + fileCount);
			
			if(dirCount == 0)
				numDir.setText("-");
			else
				numDir.setText("" + dirCount);
			
		} else {
			numFile.setText("-");
			numDir.setText("-");
		}
		
		((TextView)v.findViewById(R.id.info_name_label)).setText(file.getName());
		((TextView)v.findViewById(R.id.info_time_stamp)).setText(date.toString());
		((TextView)v.findViewById(R.id.info_path_label)).setText(apath.substring(0, apath.lastIndexOf("/") + 1));
		((TextView)v.findViewById(R.id.info_total_size)).setText(formatSize(file.length()));		
		((TextView)v.findViewById(R.id.info_read_perm)).setText(file.canRead() + "");
		((TextView)v.findViewById(R.id.info_write_perm)).setText(file.canWrite() + "");
		((TextView)v.findViewById(R.id.info_execute_perm)).setText(file.canExecute() + "");
		
		if (file.isDirectory())
			((ImageView)v.findViewById(R.id.info_icon)).setImageResource(R.drawable.folder_md);
		else
			((ImageView)v.findViewById(R.id.info_icon)).setImageResource(getFileIcon(file.getName(), false));
	}
	
	private int getFileIcon(String fileName, boolean largeSize) {
		int res;
		String ext = "";
		
		try {
			ext = fileName.substring(fileName.lastIndexOf(".") + 1);
			
		} catch (StringIndexOutOfBoundsException e) {
			ext = "dir";
		}
		
		if(ext.equalsIgnoreCase("doc") || ext.equalsIgnoreCase("docx")) {
			res = largeSize ? R.drawable.doc : R.drawable.doc_md;
			
		} else if(ext.equalsIgnoreCase("xls")  || 
				  ext.equalsIgnoreCase("xlsx") ||
				  ext.equalsIgnoreCase("xlsm")) {
			res = largeSize ? R.drawable.excel : R.drawable.excel_md;
			
		} else if(ext.equalsIgnoreCase("ppt") || ext.equalsIgnoreCase("pptx")) {
			res = largeSize ? R.drawable.powerpoint : R.drawable.powerpoint_md;
			
		} else if(ext.equalsIgnoreCase("zip") || ext.equalsIgnoreCase("gzip")) {
			res = largeSize ? R.drawable.zip : R.drawable.zip_md;
			
		} else if(ext.equalsIgnoreCase("rar")) {
			res = largeSize ? R.drawable.rar : R.drawable.rar_md;
			
		} else if(ext.equalsIgnoreCase("apk")) {
			res = largeSize ? R.drawable.apk : R.drawable.apk_md;
			
		} else if(ext.equalsIgnoreCase("pdf")) {
			res = largeSize ? R.drawable.pdf : R.drawable.pdf_md;
			
		} else if(ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("html")) {
			res = largeSize ? R.drawable.xml_html : R.drawable.xml_html_md;
			
		} else if(ext.equalsIgnoreCase("mp4") || 
				  ext.equalsIgnoreCase("3gp") ||
				  ext.equalsIgnoreCase("webm") || 
				  ext.equalsIgnoreCase("m4v")) {
			res = largeSize ? R.drawable.movie : R.drawable.movie_md;
			
		} else if(ext.equalsIgnoreCase("mp3") || ext.equalsIgnoreCase("wav") ||
				  ext.equalsIgnoreCase("wma") || ext.equalsIgnoreCase("m4p") ||
				  ext.equalsIgnoreCase("m4a") || ext.equalsIgnoreCase("ogg")) {
			res = largeSize ? R.drawable.music : R.drawable.music_md;
			
		} else if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png") ||
				  ext.equalsIgnoreCase("jpg")  || ext.equalsIgnoreCase("gif")) {
			res = largeSize ? R.drawable.photo : R.drawable.photo_md;
			
		} else {
			res = largeSize ? R.drawable.unknown : R.drawable.unknown_md;
		}
		
		return res;
	}
	
	/*
	 * 
	 */
	private class DialogListAdapter extends ArrayAdapter<String> {
		private DataViewHolder mHolder;
		
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
				
				mHolder = new DataViewHolder();				
				view = inflater.inflate(R.layout.dir_list_layout, parent, false);
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
