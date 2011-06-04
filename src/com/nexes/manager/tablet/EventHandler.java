package com.nexes.manager.tablet;

import android.os.AsyncTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.FragmentTransaction;
import android.view.View;
import android.view.LayoutInflater;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.TextView;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.io.File;


public class EventHandler {
	private static final int SEARCH_TYPE =		0x00;
	private static final int COPY_TYPE =		0x01;
	private static final int UNZIP_TYPE =		0x02;
	private static final int UNZIPTO_TYPE =		0x03;
	private static final int ZIP_TYPE =			0x04;
	private static final int DELETE_TYPE = 		0x05;
	
	private OnWorkerThreadFinishedListener mThreadListener;
	private FileManager mFileMang;
	private Context mContext;
	
	public interface OnWorkerThreadFinishedListener {
		public void onWorkerThreadComplete(int type);
	}
	
	
	public EventHandler(Context context, FileManager filemanager) {
		mFileMang = filemanager;
		mContext = context;
	}
	
	public void setOnWorkerThreadFinishedListener(OnWorkerThreadFinishedListener e) {
		mThreadListener = e;
	}
	
	public void deleteFile(final String path) {
		String name = path.substring(path.lastIndexOf("/") + 1, path.length());
		AlertDialog.Builder b = new AlertDialog.Builder(mContext);
		
		b.setTitle("Deleting " + name)
		 .setMessage("Deleting " + name + " cannot be undone.\nAre you sure" +
					 " you want to continue?")
		 .setIcon(R.drawable.download)
		 .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new BackgroundWork(DELETE_TYPE).execute(path);
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
			
		}).create().show();
	}
	
	public void renameFile(final String path, boolean isFolder) {
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.input_dialog_layout, null);
		String name = path.substring(path.lastIndexOf("/") + 1, path.length());
		
		final EditText text = (EditText)view.findViewById(R.id.dialog_input);	
		TextView msg = (TextView)view.findViewById(R.id.dialog_message);
		msg.setText("Please type the new name you want to call this file.");
		
		if(!isFolder) {
			TextView type = (TextView)view.findViewById(R.id.dialog_ext);
			type.setVisibility(View.VISIBLE);
			type.setText(path.substring(path.lastIndexOf("."), path.length()));
		}
		
		new AlertDialog.Builder(mContext)
		.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String name = text.getText().toString();
				
				if(name.length() > 0) {
					mFileMang.renameTarget(path, name);
					mThreadListener.onWorkerThreadComplete(1);
				} else {
					dialog.dismiss();
				}
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.setView(view)
		.setTitle("Rename " + name)
		.setCancelable(false)
		.setIcon(R.drawable.download).create().show();
	}

	/**
	 * 
	 * @param directory directory path to create the new folder in.
	 */
	public void createNewFolder(final String directory) {
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.input_dialog_layout, null);
		
		final EditText text = (EditText)view.findViewById(R.id.dialog_input);
		TextView msg = (TextView)view.findViewById(R.id.dialog_message);
		
		msg.setText("Type the name of the folder you would like to create.");
		
		new AlertDialog.Builder(mContext)
		.setPositiveButton("Create", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String name = text.getText().toString();
				
				if(name.length() > 0) {
					mFileMang.createDir(directory, name);
					mThreadListener.onWorkerThreadComplete(1);
				} else {
					dialog.dismiss();
				}
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.setView(view)
		.setTitle("Create a new Folder")
		.setCancelable(false)
		.setIcon(R.drawable.download).create().show();
	}
	
	public void sendFile(String path) {
		String name = path.substring(path.lastIndexOf("/") + 1, path.length());
		final String filePath = path;
		final File file = new File(path);
		CharSequence[] list = {"Bluetooth", "Email"};
		
		AlertDialog.Builder b = new AlertDialog.Builder(mContext);
		b.setTitle("Sending " + name)
		 .setIcon(R.drawable.download)
		 .setItems(list, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
					case 0:
						Intent bt = new Intent(mContext, BluetoothActivity.class);
						bt.putExtra("path", filePath);
						mContext.startActivity(bt);
						break;
						
					case 1:
						Intent mail = new Intent();
						
						mail.setAction(android.content.Intent.ACTION_SEND);
						mail.setType("application/mail");
						mail.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
						mContext.startActivity(mail);
						break;
				}
				
			}
		}).create().show();
	}
	
	/**
	 * Do work on second thread class
	 * @author Joe Berria
	 */
	private class BackgroundWork extends AsyncTask<String, Void, ArrayList<String>> {
		private int mType;
		private ProgressDialog mPDialog;
		
		public BackgroundWork(int type) {
			mType = type;
		}
		
		@Override
		protected void onPreExecute() {
			switch(mType) {
			case DELETE_TYPE:
				mPDialog = ProgressDialog.show(mContext, "Delete", 
											   "Please Wait...");
				break;
				
			case SEARCH_TYPE:
				break;
				
			case COPY_TYPE:
				break;
				
			case UNZIP_TYPE:
				break;
				
			case UNZIPTO_TYPE:
				break;
				
			case ZIP_TYPE:
				break;
			}
		}
		
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			ArrayList<String> results = null;
			
			switch(mType) {
			case DELETE_TYPE:
				if(results == null)
					 results = new ArrayList<String>();
								
				int ret = mFileMang.deleteTarget(params[0]);
				results.add(ret + "");
				
				return results;
				
			case SEARCH_TYPE:
				return null;
				
			case COPY_TYPE:
				return null;
				
			case UNZIP_TYPE:
				return null;
				
			case UNZIPTO_TYPE:
				return null;
				
			case ZIP_TYPE:
				return null;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(ArrayList<String> result) {
			switch(mType) {
			case DELETE_TYPE:
				int ret = Integer.valueOf(result.get(0));
				
				mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType);
				
				if(ret == 0)
					Toast.makeText(mContext, "Delete was successful", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(mContext, "Sorry, delete was unsuccessful", Toast.LENGTH_SHORT).show();
					
				break;
				
			case SEARCH_TYPE:
				break;
				
			case COPY_TYPE:
				break;
				
			case UNZIP_TYPE:
				break;
				
			case UNZIPTO_TYPE:
				break;
				
			case ZIP_TYPE:
				break;
			}
		}
	}
}
