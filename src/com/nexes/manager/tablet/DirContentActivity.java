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

import com.nexes.manager.tablet.DirListActivity.OnChangeLocationListener;
import com.nexes.manager.tablet.EventHandler.OnWorkerThreadFinishedListener;
import com.nexes.manager.tablet.MainActivity.OnSetingsChangeListener;
import java.io.File;
import java.util.ArrayList;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.preference.PreferenceManager;
import android.graphics.Bitmap;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.net.Uri;
import android.util.Log;

public class DirContentActivity extends Fragment implements OnItemClickListener,
															OnSetingsChangeListener,
															OnChangeLocationListener,
															OnWorkerThreadFinishedListener{
	private static final int D_MENU_DELETE 	= 0x00;
	private static final int D_MENU_RENAME 	= 0x01;
	private static final int D_MENU_COPY	= 0x02;
	private static final int D_MENU_MOVE	= 0x03;
	private static final int D_MENU_ZIP		= 0x04;
	private static final int D_MENU_PASTE	= 0x05;
	private static final int D_MENU_UNZIP	= 0x0b;
	private static final int D_MENU_BOOK	= 0x0c;
	
	private static final int F_MENU_DELETE	= 0X06;
	private static final int F_MENU_RENAME	= 0X07;
	private static final int F_MENU_COPY	= 0X08;
	private static final int F_MENU_MOVE	= 0X09;
	private static final int F_MENU_SEND	= 0x0a;
	private static boolean mMultiSelectOn = false;
	
	private FileManager mFileMang;
	private EventHandler mHandler;
	private static OnBookMarkAddListener mBookmarkList;
	
	private LinearLayout mPathView;
	private GridView mGrid = null;
	private ListView mList = null;
	private boolean mShowGrid;
	
	private ArrayList<String> mData;
	private Context mContext;
	private DataAdapter mDelegate;
	private ActionMode mActionMode;
	private boolean mActionModeSelected;
	private boolean mHoldingFile;
	private boolean mHoldingZip;
	private int mBackPathIndex;
	
	private ActionMode.Callback mFolderOptActionMode = new ActionMode.Callback() {
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			mActionModeSelected = false;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			menu.add(0, D_MENU_BOOK, 0, "Bookmark");
			menu.add(0, D_MENU_DELETE, 0, "Delete");
			menu.add(0, D_MENU_RENAME, 0, "Rename");
        	menu.add(0, D_MENU_COPY, 0, "Copy");
        	menu.add(0, D_MENU_MOVE, 0, "Cut");
        	menu.add(0, D_MENU_ZIP, 0, "Zip");
        	menu.add(0, D_MENU_PASTE, 0, "Paste into folder").setEnabled(mHoldingFile);
        	menu.add(0, D_MENU_UNZIP, 0, "Unzip here").setEnabled(mHoldingZip);
        	
        	mActionModeSelected = true;
			
        	return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			String path = mFileMang.getCurrentDir() + "/";
			String name = mode.getTitle().toString();
			
			switch(item.getItemId()) {
				case D_MENU_BOOK:
					mBookmarkList.onBookMarkAdd(path + name);
					mode.finish();
					
					return true;
					
				case D_MENU_DELETE:
					mHandler.deleteFile(path + name);
					mode.finish();
					
					return true;
					
				case D_MENU_RENAME:
					mHandler.renameFile(path + name, true);
					mode.finish();
					
					return true;
					
				case D_MENU_COPY:
					mode.finish();
					return true;
					
				case D_MENU_MOVE:
					mode.finish();
					return true;
					
				case D_MENU_ZIP:
					mode.finish();
					return true;
					
				case D_MENU_PASTE:
					mode.finish();
					return true;
					
				case D_MENU_UNZIP:
					mode.finish();
					return true;
			}
			
			return false;
		}
	};	

	private ActionMode.Callback mFileOptActionMode = new ActionMode.Callback() {
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			mActionModeSelected = false;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {			
			menu.add(0, F_MENU_DELETE, 0, "Delete");
    		menu.add(0, F_MENU_RENAME, 0, "Rename");
    		menu.add(0, F_MENU_COPY, 0, "Copy");
    		menu.add(0, F_MENU_MOVE, 0, "Cut");
    		menu.add(0, F_MENU_SEND, 0, "Send");
    		
    		mActionModeSelected = true;
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			String path = mFileMang.getCurrentDir() + "/";
			String name = mode.getTitle().toString();
			
			switch(item.getItemId()) {
				case F_MENU_DELETE:
					mHandler.deleteFile(path + name);
					mode.finish();
					
					return true;
					
				case F_MENU_RENAME:
					mHandler.renameFile(path + name, false);
					mode.finish();
					
					return true;
					
				case F_MENU_COPY:
					mode.finish();
					
					return true;
					
				case F_MENU_MOVE:
					mode.finish();
					
					return true;
					
				case F_MENU_SEND:
					mHandler.sendFile(path + name);
					mode.finish();
					
					return true;
			}
			
			mActionModeSelected = false;
			return false;
		}
	};
	
	public interface OnBookMarkAddListener {
		public void onBookMarkAdd(String path);
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = getActivity();
		mFileMang = new FileManager();
		mData = mFileMang.setHomeDir("/sdcard");
		mHandler = new EventHandler(mContext, mFileMang);
		mHandler.setOnWorkerThreadFinishedListener(this);
		
		mBackPathIndex = 0;
		mHoldingFile = false;
		mHoldingZip = false;
		mActionModeSelected = false;
		mShowGrid = "grid".equals((PreferenceManager
									.getDefaultSharedPreferences(mContext))
										.getString("pref_view", "grid"));
		
		MainActivity.setOnSetingsChangeListener(this);
		DirListActivity.setOnChangeLocationListener(this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.grid_layout, container, false);
		v.setBackgroundResource(R.color.lightgray);
		
		mPathView = (LinearLayout)v.findViewById(R.id.scroll_path);
		mDelegate = new DataAdapter(mContext, R.layout.grid_content_layout, mData);
		mGrid = (GridView)v.findViewById(R.id.grid_gridview);
		mList = (ListView)v.findViewById(R.id.list_listview);
		
		if(mShowGrid) {
			mGrid.setVisibility(View.VISIBLE);	
			mGrid.setOnItemClickListener(this);
			mGrid.setAdapter(mDelegate);
			mGrid.setOnItemLongClickListener(new OnItemLongClickListener() {
				
				@Override
				public boolean onItemLongClick(AdapterView<?> list, View view ,int pos, long id) {
					String name = mData.get(pos);
					
					if(!mFileMang.isDirectory(name) && mActionMode == null && !mMultiSelectOn) {
						mActionMode = getActivity().startActionMode(mFileOptActionMode);
						mActionMode.setTitle(mData.get(pos));
						
						return true;
					}
					
					if(mFileMang.isDirectory(name) && mActionMode == null && !mMultiSelectOn) {
						mActionMode = getActivity().startActionMode(mFolderOptActionMode);
						mActionMode.setTitle(mData.get(pos));
						
						return true;
					}
					
					return false;
				}
			});
			
		} else if(!mShowGrid) {			
			mList.setVisibility(View.VISIBLE);	
			mList.setOnItemClickListener(this);
			mList.setAdapter(mDelegate);
			mList.setOnItemLongClickListener(new OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> list, View view ,int pos, long id) {
					String name = mData.get(pos);
					
					if(!mFileMang.isDirectory(name) && mActionMode == null && !mMultiSelectOn) {
						mActionMode = getActivity().startActionMode(mFileOptActionMode);
						mActionMode.setTitle(mData.get(pos));
						
						return true;
					}
					
					if(mFileMang.isDirectory(name) && mActionMode == null && !mMultiSelectOn) {
						mActionMode = getActivity().startActionMode(mFolderOptActionMode);
						mActionMode.setTitle(mData.get(pos));
						
						return true;
					}
					
					return false;
				}
			});
		}

		return v;
	}
	
	@Override
	public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
		String item_ext = "";
		final String name = mData.get(pos);
		
		if(mMultiSelectOn) {
			view.setBackgroundColor(0xff0091dc);
		}
		
		if(mFileMang.isDirectory(name) && !mActionModeSelected && !mMultiSelectOn) {
			Button button = new Button(mContext);
			mData = mFileMang.getNextDir(name, false);
			
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int index = (Integer)v.getTag();
					String path = mFileMang.getCurrentDir();
					String subPath;
					
					if(mActionModeSelected || mMultiSelectOn)
						return;
					
					if(index != (mPathView.getChildCount() - 1)) {					
						while(index < mPathView.getChildCount() - 1) 
							mPathView.removeViewAt(mPathView.getChildCount() - 1);
						
						mBackPathIndex = index + 1;
						subPath = path.substring(0, path.lastIndexOf(name));
						mData = mFileMang.getNextDir(subPath + name, true);
						
						mDelegate.notifyDataSetChanged();
					}
				}
			});
			
			button.setText(name);
			button.setTag(mBackPathIndex++);
			mPathView.addView(button);
						
			mDelegate.notifyDataSetChanged();
			
		} else if (!mFileMang.isDirectory(name) && !mActionModeSelected && !mMultiSelectOn) {
			File file = new File(mFileMang.getCurrentDir() + "/" + name);
			
			try {
				item_ext = name.substring(name.lastIndexOf("."), name.length());
				
			} catch (StringIndexOutOfBoundsException e) {
				item_ext = "";
			}
			
			/*audio files*/
			if (item_ext.equalsIgnoreCase(".mp3") || 
				item_ext.equalsIgnoreCase(".m4a") ) {
	    		
	    		Intent i = new Intent();
   				i.setAction(android.content.Intent.ACTION_VIEW);
   				i.setDataAndType(Uri.fromFile(file), "audio/*");
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
		    		picIntent.setDataAndType(Uri.fromFile(file), "image/*");
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
		    		movieIntent.setDataAndType(Uri.fromFile(file), "video/*");
		    		startActivity(movieIntent);	
	    	}
			
			/*pdf file selected*/
	    	else if(item_ext.equalsIgnoreCase(".pdf")) {
	    		
	    		if(file.exists()) {
		    		Intent pdfIntent = new Intent();
		    		pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
		    		pdfIntent.setDataAndType(Uri.fromFile(file), "application/pdf");
			    		
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
	    		
	    		if(file.exists()) {
	    			Intent apkIntent = new Intent();
	    			apkIntent.setAction(android.content.Intent.ACTION_VIEW);
	    			apkIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
	    			startActivity(apkIntent);
	    		}
	    	}
			
			/* HTML file */
	    	else if(item_ext.equalsIgnoreCase(".html")) {
	    		
	    		if(file.exists()) {
	    			Intent htmlIntent = new Intent();
	    			htmlIntent.setAction(android.content.Intent.ACTION_VIEW);
	    			htmlIntent.setDataAndType(Uri.fromFile(file), "text/html");
	    			
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
    			txtIntent.setDataAndType(Uri.fromFile(file), "text/plain");
    			
    			try {
    				startActivity(txtIntent);
    			} catch(ActivityNotFoundException e) {
    				txtIntent.setType("text/*");
    				startActivity(txtIntent);
    			}
	    	}
			
			/* generic intent */
	    	else {
	    		if(file.exists()) {
		    		Intent generic = new Intent();
		    		generic.setAction(android.content.Intent.ACTION_VIEW);
		    		generic.setDataAndType(Uri.fromFile(file), "application/*");
		    		
		    		try {
		    			startActivity(generic);
		    		} catch(ActivityNotFoundException e) {
		    			Toast.makeText(mContext, "Sorry, couldn't find anything " +
		    						   "to open " + file.getName(), 
		    						   Toast.LENGTH_SHORT).show();
			    	}
	    		}
	    	}
		}
	}
	
	@Override
	public void onChangeLocation(String name) {
		
		if(mActionModeSelected || mMultiSelectOn)
			return;
		
		mData = mFileMang.setHomeDir(name);
		mDelegate.notifyDataSetChanged();
		
		mPathView.removeAllViews();
		mBackPathIndex = 0;
	}
	
	@Override
	public void onWorkerThreadComplete(int type) {
		//check type
		mData = mFileMang.getNextDir(mFileMang.getCurrentDir(), true);
		mDelegate.notifyDataSetChanged();
	}
	
	@Override
	public void onHiddenFilesChanged(boolean state) {
		
	}

	@Override
	public void onThumbnailChanged(boolean state) {
		
	}

	@Override
	public void onViewChanged(String state) {
		//think of a better way.
		if(state.equals("list") && mShowGrid) {						
			mList.setVisibility(View.VISIBLE);	
			mList.setOnItemClickListener(this);
			mList.setAdapter(mDelegate);
			mList.setOnItemLongClickListener(new OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> list, View view ,int pos, long id) {
					String name = mData.get(pos);
					
					if(!mFileMang.isDirectory(name) && mActionMode == null && !mMultiSelectOn) {
						mActionMode = getActivity().startActionMode(mFileOptActionMode);
						mActionMode.setTitle(mData.get(pos));
						
						return true;
					}
					
					if(mFileMang.isDirectory(name) && mActionMode == null && !mMultiSelectOn) {
						mActionMode = getActivity().startActionMode(mFolderOptActionMode);
						mActionMode.setTitle(mData.get(pos));
						
						return true;
					}
					
					return false;
				}
			});	
			mGrid.setVisibility(View.GONE);
			mShowGrid = false;
			
		} else if (state.equals("grid") && !mShowGrid) {
			mGrid.setVisibility(View.VISIBLE);	
			mGrid.setOnItemClickListener(this);
			mGrid.setAdapter(mDelegate);
			mGrid.setOnItemLongClickListener(new OnItemLongClickListener() {
				
				@Override
				public boolean onItemLongClick(AdapterView<?> list, View view ,int pos, long id) {
					String name = mData.get(pos);
					
					if(!mFileMang.isDirectory(name) && mActionMode == null && !mMultiSelectOn) {
						mActionMode = getActivity().startActionMode(mFileOptActionMode);
						mActionMode.setTitle(mData.get(pos));
						
						return true;
					}
					
					if(mFileMang.isDirectory(name) && mActionMode == null && !mMultiSelectOn) {
						mActionMode = getActivity().startActionMode(mFolderOptActionMode);
						mActionMode.setTitle(mData.get(pos));
						
						return true;
					}
					
					return false;
				}
			});
			mList.setVisibility(View.GONE);
			mShowGrid = true;
		}
	}

	@Override
	public void onSortingChanged(String state) {
		
	}
	
	public void newFolder() {
		mHandler.createNewFolder(mFileMang.getCurrentDir());
	}
	
	public static void isMultiSelectOn(boolean state) {
		mMultiSelectOn = state;
	}
	
	public static void setOnBookMarkAddListener(OnBookMarkAddListener e) {
		mBookmarkList = e;
	}
	
	
	
	/**
	 * 
	 */
	private class DataAdapter extends ArrayAdapter<String> {
		private final int KB = 1024;
    	private final int MG = KB * KB;
    	private final int GB = MG * KB;
    	
		private DataViewHolder mHolder;
		private ThumbnailCreator mThumbnail;
		private String mTempDir;
		private String mName;
		
		public DataAdapter(Context context, int layout, ArrayList<String> data) {
			super(context, layout, data);
			
			mThumbnail = new ThumbnailCreator(72, 72);
			mTempDir = mFileMang.getCurrentDir();
		}
		
		@Override
		public View getView(int position, View view, ViewGroup parent) {
			mName = mData.get(position);
			String current = mFileMang.getCurrentDir();
			String ext;
			File file = new File(current + "/" + mName);
			
			if(!mTempDir.equals(current)) {
				mThumbnail.clearBitmapCache();
				mTempDir = current;
			}
			
			try {
				ext = mName.substring(mName.lastIndexOf('.') + 1, mName.length());
				
			} catch (StringIndexOutOfBoundsException e) { ext = ""; }
			
			if(view == null) {
				LayoutInflater in = (LayoutInflater)mContext
										.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				if(mShowGrid)
					view = in.inflate(R.layout.grid_content_layout, parent, false);
				else
					view = in.inflate(R.layout.list_content_layout, parent, false);
				
				mHolder = new DataViewHolder();
				mHolder.mIcon = (ImageView)view.findViewById(R.id.content_icon);
				mHolder.mMainText = (TextView)view.findViewById(R.id.content_text);
				
				if(!mShowGrid) 
					mHolder.mSubText = (TextView)view.findViewById(R.id.content_subtext);
				
				view.setTag(mHolder);
				
			} else {
				mHolder = (DataViewHolder)view.getTag();
			}
			
			if(!mShowGrid) 
				mHolder.mSubText.setText(getFileDetails());
			
			mHolder.mMainText.setText(mName);
			
			/* assign custom icons based on file type */
			if(mFileMang.isDirectory(mName)) {
				if(file.canRead() && file.list().length > 0)
					mHolder.mIcon.setImageResource(R.drawable.folder_large_full);
				else
					mHolder.mIcon.setImageResource(R.drawable.folder_large);
				
			} else if(ext.equalsIgnoreCase("doc") || ext.equalsIgnoreCase("docx")) {
				mHolder.mIcon.setImageResource(R.drawable.doc);
				
			} else if(ext.equalsIgnoreCase("xls")) {
				mHolder.mIcon.setImageResource(R.drawable.xls);
				
			} else if(ext.equalsIgnoreCase("apk")) {
				mHolder.mIcon.setImageResource(R.drawable.apk);
				
			} else if(ext.equalsIgnoreCase("pdf")) {
				mHolder.mIcon.setImageResource(R.drawable.pdf);
				
			} else if(ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("html")) {
				mHolder.mIcon.setImageResource(R.drawable.xml_html);
				
			} else if(ext.equalsIgnoreCase("mp4") || ext.equalsIgnoreCase("3gp") ||
					  ext.equalsIgnoreCase("webm") || ext.equalsIgnoreCase("m4v")) {
				mHolder.mIcon.setImageResource(R.drawable.movie);
				
			} else if(ext.equalsIgnoreCase("mp3") || ext.equalsIgnoreCase("wav") ||
					  ext.equalsIgnoreCase("wma") || ext.equalsIgnoreCase("m4p") ||
					  ext.equalsIgnoreCase("m4a") || ext.equalsIgnoreCase("ogg")) {
				mHolder.mIcon.setImageResource(R.drawable.music);
				
			} else if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png") ||
					  ext.equalsIgnoreCase("jpg")  || ext.equalsIgnoreCase("gif")) {
				final int pos = position;
				//FIX THIS, THIS DOESN'T WORK RIGHT
				if(file.length() > 0) {
					Bitmap thumb = mThumbnail.hasBitmapCached(pos);
					
					if(thumb == null) {
						Handler handle = new Handler(new Handler.Callback() {
							@Override
							public boolean handleMessage(Message msg) {
								mHolder.mIcon.setImageBitmap((Bitmap)msg.obj);
								notifyDataSetChanged();
								return true;
							}
						});
						
						mThumbnail.setBitmapToImageView(file.getPath(), 
														handle,
														pos);
					} else {
						mHolder.mIcon.setImageBitmap(thumb);
					}
				}
				
			} else {
				mHolder.mIcon.setImageResource(R.drawable.unknown);
			}
			
			return view;
		}
		
		private String getFileDetails() {
			File file = new File(mFileMang.getCurrentDir() + "/" + mName);
			String t = file.getPath() + "\t\t";
			double bytes;
			String size = "";
			String atrs = " | - ";
			
			if(file.isDirectory()) {
				if(file.canRead())
					size =  file.list().length + " items";
				atrs += " d";
				
			} else {
				bytes = file.length();
				
				if (bytes > GB)
    				size = String.format("%.2f Gb ", (double)bytes / GB);
    			else if (bytes < GB && bytes > MG)
    				size = String.format("%.2f Mb ", (double)bytes / MG);
    			else if (bytes < MG && bytes > KB)
    				size = String.format("%.2f Kb ", (double)bytes/ KB);
    			else
    				size = String.format("%.2f bytes ", (double)bytes);
			}
			
			if(file.canRead())
				atrs += "r";
			if(file.canWrite())
				atrs += "w";
			
			return t + size + atrs;
		}
	}
}



