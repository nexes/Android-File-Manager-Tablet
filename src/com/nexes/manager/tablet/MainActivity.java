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

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.util.Log;

public class MainActivity extends Activity {
	//keys used for preference file
	private static final String PREF_LIST_KEY =		"pref_dirlist";
	private static final String PREF_HIDDEN_KEY = 	"pref_hiddenFiles";
	private static final String PREF_THUMB_KEY	=	"pref_thumbnail";
	private static final String PREF_VIEW_KEY =		"pref_view";
	private static final String PREF_SORT_KEY = 	"pref_sorting";
	
	//menu IDs
	private static final int MENU_DIR = 		0;
	private static final int MENU_SEARCH = 		1;
	private static final int MENU_MULTI =		2;
	private static final int MENU_SETTINGS = 	3;
	private static final int PREF_CODE =		6;
	
	private static OnSetingsChangeListener mSettingsListener;
	private SharedPreferences mPreferences;
	private ActionMode mActionMode;
	private SearchView mSearchView;
	
	private ActionMode.Callback mMultiSelectAction = new ActionMode.Callback() {
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			
			DirContentActivity.isMultiSelectOn(false);
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.setTitle("Multi-select Options");
			
			menu.add(0, 12, 0, "Delete");
			menu.add(0, 13, 0, "Copy");
			menu.add(0, 14, 0, "Cut");
			menu.add(0, 15, 0, "Send");
			
			DirContentActivity.isMultiSelectOn(true);
			
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch(item.getItemId()) {
			case 12:
				Log.e("ACTION MODE", item.getTitle().toString());
				return true;
			
			case 13:
				Log.e("ACTION MODE", item.getTitle().toString());
				return true;
				
			case 14:
				Log.e("ACTION MODE", item.getTitle().toString());
				return true;
				
			case 15:
				Log.e("ACTION MODE", item.getTitle().toString());
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
        setContentView(R.layout.main);
        
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSearchView = new SearchView(this);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				Log.e("asdf", query);
				
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}
		});
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
    	case MENU_DIR:
    		DirContentActivity cont = ((DirContentActivity)getFragmentManager()
    								.findFragmentById(R.id.content_frag));
    		cont.newFolder();
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
    
    public static void setOnSetingsChangeListener(OnSetingsChangeListener e) {
    	mSettingsListener = e;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode == PREF_CODE) {
    		//write this better, check if you need to call these methods eg getAll
    		mSettingsListener.onHiddenFilesChanged(mPreferences.getBoolean(PREF_HIDDEN_KEY, false));
    		mSettingsListener.onThumbnailChanged(mPreferences.getBoolean(PREF_THUMB_KEY, false));
    		mSettingsListener.onViewChanged(mPreferences.getString(PREF_VIEW_KEY, "grid"));
    		mSettingsListener.onSortingChanged(mPreferences.getString(PREF_SORT_KEY, "alpha"));
    	}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	String list = ((DirListActivity)getFragmentManager()
    					.findFragmentById(R.id.list_frag)).getDirListString();
    	String saved = mPreferences.getString(PREF_LIST_KEY, "");
    	
    	if(!list.equals(saved)) {
    		SharedPreferences.Editor e = mPreferences.edit();
    		e.putString(PREF_LIST_KEY, list);
    		e.commit();    		
    	}
    }
}


