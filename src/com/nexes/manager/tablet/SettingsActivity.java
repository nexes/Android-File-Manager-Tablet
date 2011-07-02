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
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
	//keys used for preference file
	public static final String PREF_LIST_KEY =		"pref_dirlist";
	public static final String PREF_BOOKNAME_KEY = 	"pref_bookmarks";
	public static final String PREF_HIDDEN_KEY = 	"pref_hiddenFiles";
	public static final String PREF_THUMB_KEY	=	"pref_thumbnail";
	public static final String PREF_VIEW_KEY =		"pref_view";
	public static final String PREF_SORT_KEY = 		"pref_sorting";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}
}
