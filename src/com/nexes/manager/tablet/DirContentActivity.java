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
import com.nexes.manager.tablet.DialogHandler.OnSearchFileSelected; 

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
import android.graphics.drawable.BitmapDrawable;
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
	private static final int D_MENU_INFO	= 0x0d;
	
	private static final int F_MENU_DELETE	= 0x06;
	private static final int F_MENU_RENAME	= 0x07;
	private static final int F_MENU_COPY	= 0x08;
	private static final int F_MENU_MOVE	= 0x09;
	private static final int F_MENU_SEND	= 0x0a;
	private static final int F_MENU_INFO	= 0x0e;
	private static boolean mMultiSelectOn = false;
	
	private FileManager mFileMang;
	private EventHandler mHandler;
	private MultiSelectHandler mMultiSelect;
	private ThumbnailCreator mThumbnail;
	private static OnBookMarkAddListener mBookmarkList;
	
	private LinearLayout mPathView, mMultiSelectView;
	private GridView mGrid = null;
	private ListView mList = null;
	private boolean mShowGrid;
	
	private ArrayList<String> mData; //the data that is bound to our array adapter.
	private ArrayList<String> mHoldingFileList; //holding files waiting to be pasted(moved)
	private ArrayList<String> mHoldingZipList; //holding zip files waiting to be unzipped.
	private Context mContext;
	private DataAdapter mDelegate;
	private ActionMode mActionMode;
	private boolean mActionModeSelected;
	private boolean mHoldingFile;
	private boolean mHoldingZip;
	private boolean mCutFile;
	private boolean mShowThumbnails;
	private int mBackPathIndex;
	
	public interface OnBookMarkAddListener {
		public void onBookMarkAdd(String path);
	}
	
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
			menu.add(0, D_MENU_INFO, 0, "Info");
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
			ArrayList<String> files = new ArrayList<String>();
			String name = "/" + mode.getTitle().toString();
			String path = null;
			
			path = mFileMang.getCurrentDir() + name;
			
			switch(item.getItemId()) {
				case D_MENU_BOOK:
					mBookmarkList.onBookMarkAdd(path);
					mode.finish();
					return true;
					
				case D_MENU_DELETE:
					files.add(path);
					mHandler.deleteFile(files);
					mode.finish();
					return true;
					
				case D_MENU_RENAME:
					mHandler.renameFile(path, true);
					mode.finish();
					return true;
					
				case D_MENU_COPY:
					if(mHoldingFileList == null)
						mHoldingFileList = new ArrayList<String>();
					
					mHoldingFileList.clear();
					mHoldingFileList.add(path);
					mHoldingFile = true;
					mCutFile = false;
					((MainActivity)getActivity()).changeActionBarTitle("Holding " + name);
					mode.finish();
					return true;
					
				case D_MENU_MOVE:
					if(mHoldingFileList == null)
						mHoldingFileList = new ArrayList<String>();
					
					mHoldingFileList.clear();
					mHoldingFileList.add(path);
					mHoldingFile = true;
					mCutFile = true;
					((MainActivity)getActivity()).changeActionBarTitle("Holding " + name);
					mode.finish();
					return true;
					
				case D_MENU_PASTE:
					if(mHoldingFile && mHoldingFileList.size() > 0)
						if(mCutFile)
							mHandler.cutFile(mHoldingFileList, path);
						else
							mHandler.copyFile(mHoldingFileList, path);
					
					mHoldingFile = false;
					mCutFile = false;
					mHoldingFileList.clear();
					mHoldingFileList = null;
					((MainActivity)getActivity()).changeActionBarTitle("Open Manager");
					mode.finish();
					return true;
					
				case D_MENU_ZIP:
					mHandler.zipFile(path);
					mode.finish();
					return true;
					
				case D_MENU_UNZIP:
					mHandler.unZipFileTo(mHoldingZipList.get(0), path);
					
					mHoldingZip = false;
					mHoldingZipList.clear();
					mHoldingZipList = null;
					((MainActivity)getActivity()).changeActionBarTitle("Open Manager");
					mode.finish();
					return true;
				
				case D_MENU_INFO:
					DialogHandler dialog = DialogHandler.newDialog(DialogHandler.FILEINFO_DIALOG, mContext);
					dialog.setFilePath(path);
					dialog.show(getFragmentManager(), "info");
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
			menu.add(0, F_MENU_INFO, 0, "Info");
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
			ArrayList<String> files = new ArrayList<String>();
			String path = null;
			String name = mode.getTitle().toString();
	
			path = mFileMang.getCurrentDir() + "/" + name;
			
			switch(item.getItemId()) {
				case F_MENU_DELETE:
					files.add(path);
					mHandler.deleteFile(files);
					mode.finish();
					return true;
					
				case F_MENU_RENAME:
					mHandler.renameFile(path, false);
					mode.finish();
					return true;
					
				case F_MENU_COPY:
					if(mHoldingFileList == null)
						mHoldingFileList = new ArrayList<String>();
					
					mHoldingFileList.clear();
					mHoldingFileList.add(path);
					mHoldingFile = true;
					mCutFile = false;
					((MainActivity)getActivity()).changeActionBarTitle("Holding " + name);				
					mode.finish();
					return true;
					
				case F_MENU_MOVE:
					if(mHoldingFileList == null)
						mHoldingFileList = new ArrayList<String>();
					
					mHoldingFileList.clear();
					mHoldingFileList.add(path);
					mHoldingFile = true;
					mCutFile = true;
					((MainActivity)getActivity()).changeActionBarTitle("Holding " + name);		
					mode.finish();
					return true;
					
				case F_MENU_SEND:
					Intent mail = new Intent();
					mail.setType("application/mail");
					
					mail.setAction(android.content.Intent.ACTION_SEND);
					mail.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
					startActivity(mail);
					
					mode.finish();
					return true;

//					this is for bluetooth
//					files.add(path);
//					mHandler.sendFile(files);
//					mode.finish();
//					return true;
					
				case F_MENU_INFO:
					DialogHandler dialog = DialogHandler.newDialog(DialogHandler.FILEINFO_DIALOG, mContext);
					dialog.setFilePath(path);
					dialog.show(getFragmentManager(), "info");
					mode.finish();
					return true;
			}
			mActionModeSelected = false;
			return false;
		}
	};
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = getActivity();
		mFileMang = new FileManager();
		mHandler = new EventHandler(mContext, mFileMang);
		mHandler.setOnWorkerThreadFinishedListener(this);
		
		if (savedInstanceState != null)
			mData = mFileMang.getNextDir(savedInstanceState.getString("location"), true);
		else
			mData = mFileMang.setHomeDir("/sdcard");
		
		mBackPathIndex = 0;
		mHoldingFile = false;
		mHoldingZip = false;
		mActionModeSelected = false;
		mShowGrid = "grid".equals((PreferenceManager
									.getDefaultSharedPreferences(mContext))
										.getString("pref_view", "grid"));
		
		mShowThumbnails = PreferenceManager.getDefaultSharedPreferences(mContext)
							.getBoolean(SettingsActivity.PREF_THUMB_KEY, false);
		
		MainActivity.setOnSetingsChangeListener(this);
		DirListActivity.setOnChangeLocationListener(this);
	}
	
	 @Override
	    public void onSaveInstanceState(Bundle outState) {
	    	super.onSaveInstanceState(outState);
	    	
	    	outState.putString("location", mFileMang.getCurrentDir());
	    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.main_layout, container, false);
		v.setBackgroundResource(R.color.lightgray);
		
		mPathView = (LinearLayout)v.findViewById(R.id.scroll_path);
		mGrid = (GridView)v.findViewById(R.id.grid_gridview);
		mList = (ListView)v.findViewById(R.id.list_listview);
		mMultiSelectView = (LinearLayout)v.findViewById(R.id.multiselect_path);
		
		if (savedInstanceState != null) {
			String location = savedInstanceState.getString("location");
			String[] parts = location.split("/");
			
			for(int i = 2; i < parts.length; i++)
				addBackButton(parts[i], false);
		}
		
		if(mShowGrid) {
			mDelegate = new DataAdapter(mContext, R.layout.grid_content_layout, mData);
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
			mDelegate = new DataAdapter(mContext, R.layout.list_content_layout, mData);
			mList.setVisibility(View.VISIBLE);	
			mList.setAdapter(mDelegate);
			mList.setOnItemClickListener(this);
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
		final String name;
		final File file;
		
		name = mData.get(pos);
		file = new File(mFileMang.getCurrentDir() + "/" + name);
		
		if(mMultiSelectOn) {
			View v;
			
			if (mThumbnail == null)
				v = mMultiSelect.addFile(file.getPath());
			else
				v = mMultiSelect.addFile(file.getPath(), mThumbnail);
			
			if(v == null)
				return;
			
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {					
					int ret = mMultiSelect.clearFileEntry(file.getPath());
					mMultiSelectView.removeViewAt(ret);
				}
			});
			
			mMultiSelectView.addView(v);
			return;
		}
		
		if(file.isDirectory() && !mActionModeSelected ) {
			if (mThumbnail != null) {
				mThumbnail.setCancelThumbnails(true);
				mThumbnail = null;
			}
			
			addBackButton(name, true);

		} else if (!file.isDirectory() && !mActionModeSelected ) {
			
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
	    			apkIntent.setDataAndType(Uri.fromFile(file), 
	    									 "application/vnd.android.package-archive");
	    			startActivity(apkIntent);
	    		}
	    	}
			
			/* HTML XML file */
	    	else if(item_ext.equalsIgnoreCase(".html") || 
	    			item_ext.equalsIgnoreCase(".xml")) {
	    		
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
			
			/* ZIP files */
	    	else if(item_ext.equalsIgnoreCase(".zip")) {
	    		mHandler.unzipFile(file.getPath());
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
	
	/*
	 * (non-Javadoc)
	 * This is a callback function that is called when the user
	 * selects an item (directory) from the dir list on the left.
	 * This will update the contents of the dir on the right
	 * from the path give in the variable name.
	 */
	@Override
	public void onChangeLocation(String name) {
		
		if(mActionModeSelected || mMultiSelectOn)
			return;
		
		if (mThumbnail != null) {
			mThumbnail.setCancelThumbnails(true);
			mThumbnail = null;
		}
		
		mData = mFileMang.setHomeDir(name);
		mDelegate.notifyDataSetChanged();
		
		mPathView.removeAllViews();
		mBackPathIndex = 0;
	}
	
	/*
	 * (non-Javadoc)
	 * this will update the data shown to the user after a change to
	 * the file system has been made from our background thread or EventHandler.
	 */
	@Override
	public void onWorkerThreadComplete(int type, ArrayList<String> results) {
		
		if(type == EventHandler.SEARCH_TYPE) {
			if(results == null || results.size() < 1) {
				Toast.makeText(mContext, "Sorry, zero items found", Toast.LENGTH_LONG).show();
				return;
			}
			
			DialogHandler dialog = DialogHandler.newDialog(DialogHandler.SEARCHRESULT_DIALOG, mContext);
			dialog.setHoldingFileList(results);
			dialog.setOnSearchFileSelected(new OnSearchFileSelected() {
				
				@Override
				public void onFileSelected(String fileName) {
					File f = new File(fileName);
					String name;
					
					if (f.isDirectory()) {
						mData = mFileMang.getNextDir(f.getPath(), true);
						mDelegate.notifyDataSetChanged();
						
					} else {
						name = f.getPath().substring(0, f.getPath().lastIndexOf("/"));
						mData = mFileMang.getNextDir(name, true);
						mDelegate.notifyDataSetChanged();
					}						
				}
			});
			
			dialog.show(getFragmentManager(), "dialog");
			
		} else if(type == EventHandler.UNZIPTO_TYPE && results != null) {
			String name = results.get(0);
			name = name.substring(name.lastIndexOf("/") + 1, name.length());
			
			if(mHoldingZipList == null)
				mHoldingZipList = new ArrayList<String>();
			
			mHoldingZipList.add(results.get(0));
			mHoldingZip = true;
			((MainActivity)getActivity()).changeActionBarTitle("Holding " + name);
			
		} else {
			mData = mFileMang.getNextDir(mFileMang.getCurrentDir(), true);
			mDelegate.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onHiddenFilesChanged(boolean state) {
		mFileMang.setShowHiddenFiles(state);
		
		mData = mFileMang.getNextDir(mFileMang.getCurrentDir(), true);
		mDelegate.notifyDataSetChanged();
	}

	@Override
	public void onThumbnailChanged(boolean state) {
		mShowThumbnails = state;
		mDelegate.notifyDataSetChanged();
	}
	
	@Override
	public void onSortingChanged(String state) {		
		if (state.equals("none"))
			mFileMang.setSortType(0);
		else if (state.equals("alpha"))
			mFileMang.setSortType(1);
		else if (state.equals("type"))
			mFileMang.setSortType(2);
		else if (state.equals("size"))
			mFileMang.setSortType(3);
		
		mData = mFileMang.getNextDir(mFileMang.getCurrentDir(), true);
		mDelegate.notifyDataSetChanged();
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
			
	public void changeMultiSelectState(boolean state, MultiSelectHandler handler) {
		if(state && handler != null) {
			mMultiSelect = handler;
			mMultiSelectOn = state;
			
		} else if (!state && handler != null) {
			mMultiSelect = handler;
			mMultiSelect.cancelMultiSelect();
			mMultiSelectView.removeAllViews();
			mMultiSelectOn = state;
		}
	}
	
	public static void setOnBookMarkAddListener(OnBookMarkAddListener e) {
		mBookmarkList = e;
	}
	
	/*
	 * This is a convience function so our applications main activity
	 * (MainActivity.java) class can have access to our event handler
	 * and perform file operations such as delete, rename etc. 
	 * Event handler sits between our view and modal (FileManager)
	 */
	protected EventHandler getEventHandlerInst() {
		return mHandler;
	}
	
	/*
	 * See comments for getEventHandlerInst(). Same reasoning.
	 */
	protected FileManager getFileManagerInst() {
		return mFileMang;
	}
	
	/*
	 * we need to make a temp arraylist because when the
	 * multiselect actionmode callback is finished our multiselect
	 * object will turn off and clear the data in files
	 */
	protected void setCopiedFiles(ArrayList<String> files, boolean cutFile) {
		ArrayList<String> temp = new ArrayList<String>();
		int len = files.size();
		
		for(int i = 0; i < len; i++)
			temp.add(files.get(i));
		
		mHoldingFile = true;
		mCutFile = cutFile;
		mHoldingFileList = temp;
	}
	
	private void addBackButton(String name, boolean refreshList) {//, int pos) {
		final String bname = name;
		Button button = new Button(mContext);
		
		if (refreshList)
			mData = mFileMang.getNextDir(name, false);
		
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int index = (Integer)v.getTag();
				String path = mFileMang.getCurrentDir();
				String subPath;
				
				if(mActionModeSelected || mMultiSelectOn)
					return;
				
				if (mThumbnail != null) {
					mThumbnail.setCancelThumbnails(true);
					mThumbnail = null;
				}
				
				if(index != (mPathView.getChildCount() - 1)) {
					while(index < mPathView.getChildCount() - 1)
						mPathView.removeViewAt(mPathView.getChildCount() - 1);
					
					mBackPathIndex = index + 1;
					
					subPath = path.substring(0, path.lastIndexOf(bname));					
					mData = mFileMang.getNextDir(subPath + bname, true);
					mDelegate.notifyDataSetChanged();
				}
			}
		});
		
		button.setText(name);
		button.setTag(mBackPathIndex++);
		mPathView.addView(button);
		
		if (refreshList)
			mDelegate.notifyDataSetChanged();
	}
	
	
	/**
	 * 
	 */
	private class DataAdapter extends ArrayAdapter<String> {
		private final int KB = 1024;
    	private final int MG = KB * KB;
    	private final int GB = MG * KB;
    	
		private DataViewHolder mHolder;
		private String mName;
		
		public DataAdapter(Context context, int layout, ArrayList<String> data) {
			super(context, layout, data);			
		}
		
		@Override
		public View getView(int position, View view, ViewGroup parent) {
			String ext;
			File file = null;
			String current = mFileMang.getCurrentDir();
			
			if (mThumbnail == null)
				mThumbnail = new ThumbnailCreator(mContext, 72, 72);

			mName = mData.get(position);
			file = new File(current + "/" + mName);
									
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
			if(file.isDirectory()) {
				String[] lists = file.list();
				
				if(file.canRead() && lists != null && lists.length > 0)
					mHolder.mIcon.setImageResource(R.drawable.folder_large_full);
				else
					mHolder.mIcon.setImageResource(R.drawable.folder_large);
				
			} else if(ext.equalsIgnoreCase("doc") || ext.equalsIgnoreCase("docx")) {
				mHolder.mIcon.setImageResource(R.drawable.doc);
				
			} else if(ext.equalsIgnoreCase("xls")  || 
					  ext.equalsIgnoreCase("xlsx") ||
					  ext.equalsIgnoreCase("xlsm")) {
				mHolder.mIcon.setImageResource(R.drawable.excel);
				
			} else if(ext.equalsIgnoreCase("ppt") || ext.equalsIgnoreCase("pptx")) {
				mHolder.mIcon.setImageResource(R.drawable.powerpoint);
				
			} else if(ext.equalsIgnoreCase("zip") || ext.equalsIgnoreCase("gzip")) {
				mHolder.mIcon.setImageResource(R.drawable.zip);
				
			} else if (ext.equalsIgnoreCase("rar")) {
				mHolder.mIcon.setImageResource(R.drawable.rar);
				
			} else if(ext.equalsIgnoreCase("apk")) {
				mHolder.mIcon.setImageResource(R.drawable.apk);
				
			} else if(ext.equalsIgnoreCase("pdf")) {
				mHolder.mIcon.setImageResource(R.drawable.pdf);
				
			} else if(ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("html")) {
				mHolder.mIcon.setImageResource(R.drawable.xml_html);
				
			} else if(ext.equalsIgnoreCase("mp4") || 
					  ext.equalsIgnoreCase("3gp") ||
					  ext.equalsIgnoreCase("webm") || 
					  ext.equalsIgnoreCase("m4v")) {
				mHolder.mIcon.setImageResource(R.drawable.movie);
				
			} else if(ext.equalsIgnoreCase("mp3") || ext.equalsIgnoreCase("wav") ||
					  ext.equalsIgnoreCase("wma") || ext.equalsIgnoreCase("m4p") ||
					  ext.equalsIgnoreCase("m4a") || ext.equalsIgnoreCase("ogg")) {
				mHolder.mIcon.setImageResource(R.drawable.music);
				
			} else if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png") ||
					  ext.equalsIgnoreCase("jpg")  || ext.equalsIgnoreCase("gif")) {

				if(file.length() > 0 && mShowThumbnails) {
					BitmapDrawable thumb = mThumbnail.isBitmapCached(file.getPath());

					if (thumb == null) {
						final Handler handle = new Handler(new Handler.Callback() {
							public boolean handleMessage(Message msg) {
								notifyDataSetChanged();
								
								return true;
							}
						});
										
						mThumbnail.createNewThumbnail(mData, mFileMang.getCurrentDir(), handle);
						
						if (!mThumbnail.isAlive()) 
							mThumbnail.start();
						
					} else {
						mHolder.mIcon.setImageDrawable(thumb);
					}
					
				} else {
					mHolder.mIcon.setImageResource(R.drawable.photo);
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



