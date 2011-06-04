package com.nexes.manager.tablet;

import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.util.Log;

import java.util.Set;
import java.util.ArrayList;
import java.util.UUID;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.IOException;


public class BluetoothActivity extends Activity implements OnClickListener,
														   OnItemClickListener{
	private static final int REQUEST_ENABLE = 	0;
	
	private static final int BUTTON_ENABLE =	0x01;
	private static final int BUTTON_FIND =		0x02;
	private static final int BUTTON_CLEAR =		0x03;
	
	private ArrayList<String> mDeviceData;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDeviceAdapter mDelegate;
	private boolean mUnRegister = false;
	private TextView mMessageView;
	private Button mButton;
	
	private String mFilePath;

	/*
	 * String name, is the variable that will contain the name and MAC address
	 * of the found bluetooth device. plus the device class in the format
	 * (name)\n(MAC):deviceClass. This is so we can easily parse the string
	 * to show to the user while having all the necessary information.
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			String name;
			
			if(action.equals(BluetoothDevice.ACTION_FOUND)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				BluetoothClass btClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
				
				name = device.getName() +"\n" + device.getAddress() + 
					   ":" + btClass.getDeviceClass();
				
				if(mDeviceData.get(0).equals("No Devices"))
					mDeviceData.clear();
				
				if(!mDeviceData.contains(name)) {
					mDeviceData.add(name);
					mMessageView.setText("Found " + mDeviceData.size() + " devices");
					mDelegate.notifyDataSetChanged();
				
				} else {
					mMessageView.setText("Found " + mDeviceData.size() + " devices");
				}
			}
		}
	};

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(android.R.style.Theme_Holo_Dialog_MinWidth);
		setContentView(R.layout.bluetooth_layout);
		
		mFilePath = getIntent().getExtras().getString("path");
		
		mDeviceData = new ArrayList<String>();
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if(mBluetoothAdapter == null) {
			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setTitle("Bluetooth error")
			 .setMessage("This device does not support bluetooth")
			 .setIcon(R.drawable.download)
			 .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			}).create().show();
		}
		
		mDelegate = new BluetoothDeviceAdapter(this, 
											   R.layout.grid_content_layout, 
											   mDeviceData);
		
		GridView gview = (GridView)findViewById(R.id.bt_gridview);
		gview.setAdapter(mDelegate);
		gview.setOnItemClickListener(this);
		
		mMessageView = (TextView)findViewById(R.id.bt_message);	
		mButton = (Button)findViewById(R.id.bt_button);
		mButton.setOnClickListener(this);
		
		if(!mBluetoothAdapter.isEnabled()) {
			mMessageView.setText("Bluetooth is turned off");
			mButton.setText("Enable Bluetooth");
			mButton.setId(BUTTON_ENABLE);
		
		} else {
			mMessageView.setText("Bluetooth is turned on");
			mButton.setText("Scan for Devices");
			mButton.setId(BUTTON_FIND);
			
			findPairedDevices();
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		
		switch(id) {
		case BUTTON_ENABLE:
			if(mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
				Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enable, REQUEST_ENABLE);
			}
			break;
			
		case BUTTON_FIND:
			mMessageView.setText("Finding devices, please wait...");
			mUnRegister = true;
			
			IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			registerReceiver(mReceiver, filter);
			mBluetoothAdapter.startDiscovery();
			
			mButton.setText("Clear list");
			mButton.setId(BUTTON_CLEAR);
			break;
		
		case BUTTON_CLEAR:
			mButton.setText("Scan for Devices");
			mButton.setId(BUTTON_FIND);
			
			mDeviceData.clear();
			mDeviceData.add("No Devices");
			mDelegate.notifyDataSetChanged();
			break;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
		final String name = mDeviceData.get(pos);
		String deviceName;
		
		if(name.equals("No Devices"))
			return;
		
		deviceName = name.substring(0, name.lastIndexOf('\n'));
		
		//don't discover devices when tyring to pair or connect.
		if(mBluetoothAdapter.isDiscovering())
			mBluetoothAdapter.cancelDiscovery();
		
		new AlertDialog.Builder(this)
			.setTitle("Bluetooth Transfer")
			.setMessage("Would you like to send this file to " + deviceName +"?")
			.setIcon(R.drawable.bluetooth)
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new ClientSocketThread(name).start();
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
				
			}).create().show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_ENABLE && resultCode == Activity.RESULT_OK) {
			mMessageView.setText("Bluetooth is turned on.");
			mButton.setText("Scan for Devices");
			mButton.setId(BUTTON_FIND);
			
			findPairedDevices();
		
		} else if(requestCode == REQUEST_ENABLE && resultCode == Activity.RESULT_CANCELED) {
			Toast.makeText(this, "Bluetooth was not be turned on.", Toast.LENGTH_LONG).show();
			finish();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(mUnRegister)
			unregisterReceiver(mReceiver);
	}
	
	private void findPairedDevices() {
		Set<BluetoothDevice> paired = mBluetoothAdapter.getBondedDevices();
		
		if(paired.size() > 0) {
			for(BluetoothDevice device : paired)
				mDeviceData.add(device.getName() +"\n"+ device.getAddress() +
								":" + device.getBluetoothClass().getDeviceClass());
			
			mMessageView.setText("Found " + paired.size() + " paired devices");
		
		} else {
			mDeviceData.add("No Devices");
			mMessageView.setText("Found no paired devices");
		}
		
		mDelegate.notifyDataSetChanged();
	}
	
	
	
	
	/**
	 * 
	 * @author Joe Berria
	 *
	 */
	private class ClientSocketThread extends Thread {
		private static final String DEV_UUID = "00001101-0000-1000-8000-00805F9B34FB";
		private BluetoothDevice mRemoteDevice;
		private BluetoothSocket mSocket;
		private String mMACAddres;
		
		
		public ClientSocketThread(String device) {
			mMACAddres = device.substring(device.lastIndexOf('\n') + 1,
										 device.lastIndexOf(":"));
			
			try {
				mRemoteDevice = mBluetoothAdapter.getRemoteDevice(mMACAddres);
				mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(DEV_UUID));
				
			} catch (IllegalArgumentException e) {
				Log.e("ClientSocketThread", "Remote address " + mMACAddres + ", " + e.getMessage());
				mSocket = null;
			
			} catch (IOException e) {
				Log.e("ClientSocketThread", "ioexception " + e.getMessage());
			}			
		}
		
		@Override
		public void run() {
			//make sure we are not discovering
			if(mBluetoothAdapter.isDiscovering())
				mBluetoothAdapter.cancelDiscovery();
			
			try {
				if(mSocket != null)
					mSocket.connect();
				else
					return;
				
				communicateData(mSocket);
				
			} catch (IOException e) {
				Log.e("ClientSocketThread", "catch mSocket.connect() = " + e.getMessage());
				
				try {
					mSocket.close();
				} catch (IOException ee) {}
				
				return;
			}
		}
		
		public void cancel() {
			try {
				if(mSocket != null)
					mSocket.close();
				
			} catch (IOException e) { }
		}
		
		private void communicateData(BluetoothSocket socket) {
			OutputStream writeStrm = null;
			FileInputStream readStrm = null;
			File file = new File(mFilePath);
			byte[] buffer = new byte[1024];
			
			Log.e("ClientSocketThread", "communicateData is called");

			
			try {
				writeStrm = socket.getOutputStream();
				readStrm = new FileInputStream(file);
				
				while((readStrm.read(buffer)) != -1)
					writeStrm.write(buffer);
				
				
			} catch (IOException e) {
				Log.e("ClientSocketThread", "communicateData IOException " + e.getMessage());
			}
			
			try {
				socket.close();
			} catch (IOException e) {
				Log.e("ClientSocketThread", "communicateData socket.close =  " + e.getMessage());
			}
			cancel();
		}
	}
	
	
	/**
	 * 
	 * @author Joe Berria
	 *
	 */
	private class ServerSocketThread extends Thread {
		
		public ServerSocketThread() {
			
		}
		
		@Override
		public void run() {
			
		}
	}
	
	/**
	 * 
	 * @author Joe Berria
	 *
	 */
	private class BluetoothDeviceAdapter extends ArrayAdapter<String> {
		DataViewHolder mHolder;
				
		public BluetoothDeviceAdapter(Context context, int res, ArrayList<String> data) {
			super(context, res, data);
		}
		
		@Override
		public View getView(int position, View view, ViewGroup parent) {
			String name;
			int classType;
			
			if(view == null) {
				LayoutInflater in = (LayoutInflater)BluetoothActivity.this
										.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				view = in.inflate(R.layout.grid_content_layout, parent, false);
				
				mHolder = new DataViewHolder();
				mHolder.mIcon = (ImageView)view.findViewById(R.id.content_icon);
				mHolder.mMainText = (TextView)view.findViewById(R.id.content_text);
				
				view.setTag(mHolder);
				
			} else {
				mHolder = (DataViewHolder)view.getTag();
			}
			
			name = mDeviceData.get(position);
			
			if(!name.equals("No Devices")) {
				classType = Integer.valueOf(name.substring(name.lastIndexOf(":") + 1,
											name.length()));

				if((classType & 0x200) > 0)
					mHolder.mIcon.setImageResource(R.drawable.cellphone);
				if((classType & 0x100) > 0)
					mHolder.mIcon.setImageResource(R.drawable.computer);
				
				mHolder.mMainText.setText(name.substring(0, name.lastIndexOf(":")));
			
			} else {
				mHolder.mIcon.setImageResource(R.drawable.computer);
				mHolder.mMainText.setText(name);
			}
			
			mHolder.mMainText.setMaxLines(5);
				
			return view;
		}
	}
}
