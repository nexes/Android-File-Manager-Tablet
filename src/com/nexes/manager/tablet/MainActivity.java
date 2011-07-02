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

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity {	
	//menu IDs
	private static final int MENU_DIR = 		0x0;
	private static final int MENU_SEARCH = 		0x1;
	private static final int MENU_MULTI =		0x2;
	private static final int MENU_SETTINGS = 	0x3;
	private static final int PREF_CODE =		0x6;
	
	private static OnSetingsChangeListener mSettingsListener;
	private SharedPreferences mPreferences;
	private ActionMode mActionMode;
	private SearchView mSearchView;
	private ArrayList<String> mHeldFiles;
	private boolean mBackQuit = false;
	
	private EventHandler mEvHandler;
	private FileManager mFileManger;
	
	
	private ActionMode.Callback mMultiSelectAction = new ActionMode.Callback() {
		MultiSelectHandler handler;
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {			
			((DirContentActivity)getFragmentManager()
					.findFragmentById(R.id.content_frag))
						.changeMultiSelectState(false, handler);
			
			mActionMode = null;
			handler = null;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			handler = MultiSelectHandler.getInstance(MainActivity.this);
			mode.setTitle("Multi-select Options");
			
			menu.add(0, 12, 0, "Delete");
			menu.add(0, 13, 0, "Copy");
			menu.add(0, 14, 0, "Cut");
			menu.add(0, 15, 0, "Send");
			
			((DirContentActivity)getFragmentManager()
					.findFragmentById(R.id.content_frag))
						.changeMultiSelectState(true, handler);
			
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			ArrayList<String>files = handler.getSelectedFiles();
			
			//nothing was selected
			if(files.size() < 1) {
				mode.finish();
				return true;
			}
			
			if(mHeldFiles == null)
				mHeldFiles = new ArrayList<String>();
			
			mHeldFiles.clear();
			
			for(String s : files)
				mHeldFiles.add(s);
			
			switch(item.getItemId()) {
			case 12: /* delete */
				mEvHandler.deleteFile(mHeldFiles);
				mode.finish();
				return true;
			
			case 13: /* copy */
				getActionBar().setTitle("Holding " + files.size() + " File");
				((DirContentActivity)getFragmentManager()
						.findFragmentById(R.id.content_frag))
							.setCopiedFiles(mHeldFiles, false);
				
				Toast.makeText(MainActivity.this, 
							   "Tap the upper left corner to see your held files",
							   Toast.LENGTH_LONG).show();
				mode.finish();
				return true;
				
			case 14: /* cut */
				getActionBar().setTitle("Holding " + files.size() + " File");
				((DirContentActivity)getFragmentManager()
						.findFragmentById(R.id.content_frag))
							.setCopiedFiles(mHeldFiles, true);
				
				Toast.makeText(MainActivity.this, 
						   "Tap the upper left corner to see your held files",
						   Toast.LENGTH_LONG).show();
				mode.finish();
				return true;
				
			case 15: /* send */
				ArrayList<Uri> uris = new ArrayList<Uri>();
				Intent mail = new Intent();
				mail.setType("application/mail");
				
				if(mHeldFiles.size() == 1) {
					mail.setAction(android.content.Intent.ACTION_SEND);
					mail.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mHeldFiles.get(0))));
					startActivity(mail);
					
					mode.finish();
					return true;
				}
				
				for(int i = 0; i < mHeldFiles.size(); i++)
					uris.add(Uri.fromFile(new File(mHeldFiles.get(i))));
				
				mail.setAction(android.content.Intent.ACTION_SEND_MULTIPLE);
				mail.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
				startActivity(mail);

//				this is for bluetooth
//				mEvHandler.sendFile(mHeldFiles);
				mode.finish();
				return true;
			}
			
			return false;
		}
	};
	
	
	public interface OnSetingsChangeListener {
		
		public void onHiddenFilesChanged(boolean state);
		public void onThumbnailChanged(boolean state);
		public void onViewChanged(String state);
		public void onSortingChanged(String state);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_fragments);
                
        mEvHandler = ((DirContentActivity)getFragmentManager()
        					.findFragmentById(R.id.content_frag)).getEventHandlerInst();
        mFileManger = ((DirContentActivity)getFragmentManager()
							.findFragmentById(R.id.content_frag)).getFileManagerInst();
        
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSearchView = new SearchView(this);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        
			@Override
			public boolean onQueryTextSubmit(String query) {
				mSearchView.clearFocus();
				mEvHandler.searchFile(mFileManger.getCurrentDir(), query);
				
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}
		});
        
        /* read and display the users preferences */
        mSettingsListener.onHiddenFilesChanged(mPreferences.getBoolean(SettingsActivity.PREF_HIDDEN_KEY, false));
		mSettingsListener.onThumbnailChanged(mPreferences.getBoolean(SettingsActivity.PREF_THUMB_KEY, true));
		mSettingsListener.onViewChanged(mPreferences.getString(SettingsActivity.PREF_VIEW_KEY, "grid"));
		mSettingsListener.onSortingChanged(mPreferences.getString(SettingsActivity.PREF_SORT_KEY, "type"));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, MENU_SEARCH, 0, "Search").setIcon(R.drawable.search)
							.setActionView(mSearchView)
							.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    	
    	menu.add(0, MENU_DIR, 1, "New Folder").setIcon(R.drawable.newfolder)
    						.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    	
    	menu.add(0, MENU_MULTI, 2, "Multi-Select").setIcon(R.drawable.multiselect)
    						.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    	
    	menu.add(0, MENU_SETTINGS, 5, "Settings").setIcon(R.drawable.settings_actbar)
    						.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    	
    	return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch(item.getItemId()) {
    	case android.R.id.home:
    		if (mHeldFiles != null) {
    			DialogHandler dialog = DialogHandler.newDialog(DialogHandler.HOLDINGFILE_DIALOG, this);
    			dialog.setHoldingFileList(mHeldFiles);
    			
    			dialog.show(getFragmentManager(), "dialog");
    		}
    		return true;
    		
    	case MENU_DIR:
    		mEvHandler.createNewFolder(mFileManger.getCurrentDir());
    		return true;
    		
    	case MENU_MULTI:
    		if(mActionMode != null)
    			return false;
    		
    		mActionMode = startActionMode(mMultiSelectAction);
    		return true;
    		
    	case MENU_SETTINGS:
    		startActivityForResult(new Intent(this, SettingsActivity.class), PREF_CODE);
    		return true;
    		
    	case MENU_SEARCH:
    		return true;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (mBackQuit) {
    			return super.onKeyUp(keyCode, event);
    		} else {
    			Toast.makeText(this, "Press back again to quit", Toast.LENGTH_SHORT).show();
    			mBackQuit = true;
    			return true;
    		}    	
    	}
    	return super.onKeyUp(keyCode, event);
    }
    
    public static void setOnSetingsChangeListener(OnSetingsChangeListener e) {
    	mSettingsListener = e;
    }
    
    /*
     * used to inform the user when they are holding a file to copy, zip, et cetera
     * When the user does something with the held files (from copy or cut) this is 
     * called to reset the apps title. When that happens we will get rid of the cached
     * held files if there are any.  
     * @param title the title to be displayed
     */
    public void changeActionBarTitle(String title) {
    	if (title.equals("Open Manager") && mHeldFiles != null) {
	    	mHeldFiles.clear();
	    	mHeldFiles = null;
    	}
    	getActionBar().setTitle(title);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode == PREF_CODE) {
    		//this could be done better.
    		mSettingsListener.onHiddenFilesChanged(mPreferences.getBoolean(SettingsActivity.PREF_HIDDEN_KEY, false));
    		mSettingsListener.onThumbnailChanged(mPreferences.getBoolean(SettingsActivity.PREF_THUMB_KEY, false));
    		mSettingsListener.onViewChanged(mPreferences.getString(SettingsActivity.PREF_VIEW_KEY, "grid"));
    		mSettingsListener.onSortingChanged(mPreferences.getString(SettingsActivity.PREF_SORT_KEY, "alpha"));
    	}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	String list = ((DirListActivity)getFragmentManager()
    					.findFragmentById(R.id.list_frag)).getDirListString();
    	String bookmark = ((DirListActivity)getFragmentManager()
    					.findFragmentById(R.id.list_frag)).getBookMarkNameString();
    	
    	String saved = mPreferences.getString(SettingsActivity.PREF_LIST_KEY, "");
    	String saved_book = mPreferences.getString(SettingsActivity.PREF_BOOKNAME_KEY, "");
    	
    	if (!list.equals(saved)) {
    		SharedPreferences.Editor e = mPreferences.edit();
    		e.putString(SettingsActivity.PREF_LIST_KEY, list);
    		e.commit();
    	}
    	
    	if (!bookmark.equals(saved_book)) {
    		SharedPreferences.Editor e = mPreferences.edit();
    		e.putString(SettingsActivity.PREF_BOOKNAME_KEY, bookmark);
    		e.commit();
    	}
    }
}


