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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.io.File;

public class ThumbnailCreator {
	private int mWidth;
	private int mHeight;
	private SoftReference<Bitmap> mThumb;
	private static HashMap<String, Bitmap> cacheMap;

	public ThumbnailCreator(int width, int height) {
		mHeight = height;
		mWidth = mHeight * (4/3);
		
		cacheMap = new HashMap<String, Bitmap>();
	}
		
	public Bitmap isBitmapCached(String name) {
		return cacheMap.get(name);
	}

	public void createNewThumbnail(final String imageName, final Handler handler) {
		
		Thread thread = new Thread() {
			public void run() {
				File file = new File(imageName);
				
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 32;
				options.outWidth = mWidth;
				options.outHeight = mHeight;
									
				mThumb = (file.length() > 100000) ?
						 new SoftReference<Bitmap>(BitmapFactory.decodeFile(imageName, options)) : 
						 new SoftReference<Bitmap>(Bitmap.createScaledBitmap(
								 						  BitmapFactory.decodeFile(imageName),
								 						  mWidth,
								 						  mHeight,
								 						  false));
				cacheMap.put(imageName, mThumb.get());
				handler.post(new Runnable() {
					@Override
					public void run() {
						Message msg = handler.obtainMessage();
						msg.obj = cacheMap.get(imageName);
						msg.sendToTarget();
					}
				});
			}
		};
		thread.start();
	}

}



