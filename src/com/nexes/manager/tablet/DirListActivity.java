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

import com.nexes.manager.tablet.DirContentActivity.OnBookMarkAddListener;

import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.app.ListFragment;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.io.File;

public class DirListActivity extends ListFragment implements OnBookMarkAddListener,
															 OnItemLongClickListener{
	private static final int BOOKMARK_POS = 6;
	
	private static OnChangeLocationListener mChangeLocList;
	private ArrayList<String> mDirList;
	private ArrayList<String> mBookmarkNames;
	private Context mContext;
	private ImageView mLastIndicater = null;
	private DirListAdapter mDelegate;
	private String mDirListString;
	private String mBookmarkString;
	

	public interface OnChangeLocationListener {
		void onChangeLocation(String name);
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String storage = "/" + Environment.getExternalStorageDirectory().getName();
		mContext = getActivity();
		mDirList = new ArrayList<String>();
		mBookmarkNames = new ArrayList<String>();
		mDirListString = (PreferenceManager.getDefaultSharedPreferences(mContext))
										    .getString(SettingsActivity.PREF_LIST_KEY, "");
		
		mBookmarkString = (PreferenceManager.getDefaultSharedPreferences(mContext))
											.getString(SettingsActivity.PREF_BOOKNAME_KEY, "");
		
		if (mDirListString.length() > 0) {
			String[] l = mDirListString.split(":");
			
			for(String string : l)
				mDirList.add(string);
		
		} else {
			mDirList.add("/");
			mDirList.add(storage);
			mDirList.add(storage + "/" + "Download");
			mDirList.add(storage + "/" + "Music");
			mDirList.add(storage + "/" + "Movies");
			mDirList.add(storage + "/" + "Pictures");
			mDirList.add("Bookmarks");			
		}
		
		if (mBookmarkString.length() > 0) {
			String[] l = mBookmarkString.split(":");
			
			for(String string : l)
				mBookmarkNames.add(string);
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ListView lv = getListView();		
		lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		lv.setCacheColorHint(0x00000000);
		lv.setDrawSelectorOnTop(true);
		lv.setOnItemLongClickListener(this);
		lv.setBackgroundResource(R.drawable.listgradback);
		
		mDelegate = new DirListAdapter(mContext, R.layout.dir_list_layout, mDirList);
		registerForContextMenu(lv);
		setListAdapter(mDelegate);
		
		DirContentActivity.setOnBookMarkAddListener(this);
		
	}
	
	@Override
	public void onListItemClick(ListView list, View view, int pos, long id) {
		ImageView v;
		
		if(pos == BOOKMARK_POS)
			return;
		
		if(mLastIndicater != null)
			mLastIndicater.setVisibility(View.GONE);
			
		v = (ImageView)view.findViewById(R.id.list_arrow);
		v.setVisibility(View.VISIBLE);
		mLastIndicater = v;
		
		if(mChangeLocList != null)
			mChangeLocList.onChangeLocation(mDirList.get(pos));
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> list, View view, int pos, long id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View v = inflater.inflate(R.layout.input_dialog_layout, null);
		final EditText text = (EditText)v.findViewById(R.id.dialog_input);
		
		
		/* the first two items in our dir list is / and scdard.
		 * the user should not be able to change the location
		 * of these two entries. Everything else is fair game */
		if (pos > 1 && pos < BOOKMARK_POS) {
			final int position = pos;
						
			((TextView)v.findViewById(R.id.dialog_message))
							.setText("Change the location of this directory.");
			
			text.setText(mDirList.get(pos));
			builder.setTitle("Bookmark Location");
			builder.setView(v);
			
			switch(pos) {
			case 2:	builder.setIcon(R.drawable.download_md);break;
			case 3: builder.setIcon(R.drawable.music_md); 	break;
			case 4:	builder.setIcon(R.drawable.movie_md);	break;
			case 5:	builder.setIcon(R.drawable.photo_md); 	break;
			}
			
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			
			builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String location = text.getText().toString();
					File file = new File(location);
					
					if (!file.isDirectory()) {
						Toast.makeText(mContext, 
									   location + " is an invalid directory", 
									   Toast.LENGTH_LONG).show();
						dialog.dismiss();
					
					} else {
						mDirList.remove(position);
						mDirList.add(position, location);
						buildDirString();
					}
				}
			});
			
			builder.create().show();
			return true;
		
		/*manage the users bookmarks, delete or rename*/
		} else if (pos > BOOKMARK_POS) {
			final int p = pos;

			String bookmark = mBookmarkNames.get(p - (BOOKMARK_POS + 1));
			
			builder.setTitle("Manage bookmark: " + bookmark);
			builder.setIcon(R.drawable.folder_md);
			builder.setView(v);
			
			((TextView)v.findViewById(R.id.dialog_message))
						.setText("Would you like to delete or rename this bookmark?");
			
			text.setText(bookmark);
			builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mDirList.remove(p);
					mBookmarkNames.remove(p - (BOOKMARK_POS + 1));
					
					buildDirString();
					mDelegate.notifyDataSetChanged();
				}
			});
			builder.setNegativeButton("Rename", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mBookmarkNames.remove(p - (BOOKMARK_POS + 1));
					mBookmarkNames.add(p - (BOOKMARK_POS + 1), text.getText().toString());
					
					buildDirString();
					mDelegate.notifyDataSetChanged();
				}
			});
			
			builder.create().show();
			return true;
			
		}
		return false;
	}

	@Override
	public void onBookMarkAdd(String path) {
		mDirList.add(path);
		mBookmarkNames.add(path.substring(path.lastIndexOf("/") + 1));
		
		buildDirString();
		mDelegate.notifyDataSetChanged();
	}
	
	public static void setOnChangeLocationListener(OnChangeLocationListener l) {
		mChangeLocList = l;
	}
	
	public String getDirListString() {
		return mDirListString;
	}
	
	
	public String getBookMarkNameString() {
		return mBookmarkString;
	}
	
	/*
	 * Builds a string from mDirList to easily save and recall
	 * from preferences. 
	 */
	private void buildDirString() {
		int len = mDirList.size();
		
		if(mDirListString != null && mDirListString.length() > 0) {
			mDirListString = "";
			mBookmarkString = "";
		}
		
		for (int i = 0; i <len; i++) {
			mDirListString += mDirList.get(i) + ":";
			
			if (i > BOOKMARK_POS && mBookmarkNames.size() > 0)
				mBookmarkString += mBookmarkNames.get(i - (BOOKMARK_POS + 1)) + ":";
		}
	}
	
	
	
	/*
	 * 
	 */
	private class DirListAdapter extends ArrayAdapter<String> {
		private DataViewHolder mHolder;
		
		DirListAdapter(Context context, int layout, ArrayList<String> data) {
			super(context, layout, data);		
		}
		
		@Override
		public View getView(int position, View view, ViewGroup parent) {			
			if(view == null) {
				LayoutInflater in = (LayoutInflater)mContext.
									getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = in.inflate(R.layout.dir_list_layout, parent, false);
				mHolder = new DataViewHolder();
				
				mHolder.mIcon = (ImageView)view.findViewById(R.id.list_icon);
				mHolder.mMainText = (TextView)view.findViewById(R.id.list_name);
				mHolder.mIndicate = (ImageView)view.findViewById(R.id.list_arrow);
				
				view.setTag(mHolder);
				
			} else {
				mHolder = (DataViewHolder)view.getTag();
			}
			
			if(mLastIndicater == null) {
				if(position == 1) {
					mHolder.mIndicate.setVisibility(View.VISIBLE);
					mLastIndicater = mHolder.mIndicate;
				}
			}

			switch(position) {
			case 0:
				mHolder.mMainText.setText("/");
				mHolder.mIcon.setImageResource(R.drawable.drive);
				break;
			case 1:
				mHolder.mMainText.setText("sdcard");
				mHolder.mIcon.setImageResource(R.drawable.sdcard);
				break;
			case 2:
				mHolder.mMainText.setText("Downloads");
				mHolder.mIcon.setImageResource(R.drawable.download_md);
				break;
			case 3:
				mHolder.mMainText.setText("Music");
				mHolder.mIcon.setImageResource(R.drawable.music_md);
				break;
			case 4:
				mHolder.mMainText.setText("Movies");
				mHolder.mIcon.setImageResource(R.drawable.movie_md);
				break;
			case 5:
				mHolder.mMainText.setText("Photos");
				mHolder.mIcon.setImageResource(R.drawable.photo_md);
				break;
			case 6:
				mHolder.mMainText.setText("Bookmarks");
				mHolder.mIcon.setImageResource(R.drawable.favorites);
				view.setBackgroundColor(R.color.black);
				break;
				
			default:
				mHolder.mMainText.setText(mBookmarkNames.get(position - (BOOKMARK_POS + 1)));
				mHolder.mIcon.setImageResource(R.drawable.folder_md);
				break;
			}
			
			return view;
		}
	}	
}
