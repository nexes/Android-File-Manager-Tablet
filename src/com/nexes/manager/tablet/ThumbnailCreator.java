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
import android.graphics.drawable.BitmapDrawable;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;

public class ThumbnailCreator extends Thread {
	private int mWidth;
	private int mHeight;
	private SoftReference<Bitmap> mThumb;
	private static HashMap<String, BitmapDrawable> mCacheMap = null;	
	private ArrayList<String> mFiles;
	
	private Context mContext;
	private String mDir;
	private Handler mHandler;
	private boolean mStop = false;

	public ThumbnailCreator(Context context, int width, int height) {
		mHeight = height;
		mWidth = width;
		mContext = context;
		
		if(mCacheMap == null)
			mCacheMap = new HashMap<String, BitmapDrawable>();
	}
	
	public BitmapDrawable isBitmapCached(String name) {
		return mCacheMap.get(name);
	}
	
	public void createNewThumbnail(ArrayList<String> files,  String dir,  Handler handler) {
		this.mFiles = files;
		this.mDir = dir;
		this.mHandler = handler;		
	}
	
	public void setCancelThumbnails(boolean stop) {
		mStop = stop;
	}

	@Override
	public void run() {
		int len = mFiles.size();
		
		for (int i = 0; i < len; i++) {	
			if (mStop) {
				mStop = false;
				mFiles = null;
				return;
			}
			
			final File file = new File(mDir + "/" + mFiles.get(i));
			
			//we already loaded this thumbnail, just return it.
			if (mCacheMap.containsKey(file.getPath())) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Message msg = mHandler.obtainMessage();
						msg.obj = mCacheMap.get(file.getPath());
						msg.sendToTarget();
					}
				});
			
			//we havn't loaded it yet, lets make it. 
			} else {
				if (isImageFile(file.getName())) {
					long len_kb = file.length() / 1024;
					
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.outWidth = mWidth;
					options.outHeight = mHeight;
											
					if (len_kb > 500 && len_kb < 2000) {
						options.inSampleSize = 16;
						options.inPurgeable = true;						
						mThumb = new SoftReference<Bitmap>(BitmapFactory.decodeFile(file.getPath(), options));
											
					} else if (len_kb >= 2000) {
						options.inSampleSize = 32;
						options.inPurgeable = true;
						mThumb = new SoftReference<Bitmap>(BitmapFactory.decodeFile(file.getPath(), options));
										
					} else if (len_kb <= 500) {
						options.inPurgeable = true;
						Bitmap b = BitmapFactory.decodeFile(file.getPath());
						
						if (b == null) 
							b = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.photo);
						
						mThumb = new SoftReference<Bitmap>(Bitmap.createScaledBitmap(b, mWidth, mHeight, false));
					}

					final BitmapDrawable d = new BitmapDrawable(mThumb.get());					
					d.setGravity(Gravity.CENTER);
					mCacheMap.put(file.getPath(), d);
					
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							Message msg = mHandler.obtainMessage();
							msg.obj = (BitmapDrawable)d;
							msg.sendToTarget();
						}
					});
				}
			}
		}
	}
	
	private boolean isImageFile(String file) {
		String ext = file.substring(file.lastIndexOf(".") + 1);
		
		if (ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("jpg") ||
			ext.equalsIgnoreCase("jpeg")|| ext.equalsIgnoreCase("gif") ||
			ext.equalsIgnoreCase("tiff")|| ext.equalsIgnoreCase("tif"))
			return true;
		
		return false;
	}
}
