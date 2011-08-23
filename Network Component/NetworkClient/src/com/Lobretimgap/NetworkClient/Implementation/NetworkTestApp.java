package com.Lobretimgap.NetworkClient.Implementation;

import com.Lobretimgap.NetworkClient.NetworkComBinder;
import com.Lobretimgap.NetworkClient.NetworkComService;
import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.EventListeners.ConnectionEstablishedListener;
import com.Lobretimgap.NetworkClient.EventListeners.ConnectionLostListener;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

public class NetworkTestApp extends Activity {
	
	private TextView tv;
	private boolean networkBound = false;
	private NetworkComBinder binder;

	public void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
		tv = new TextView(this);
		setContentView(tv);		
	}
	
	public void onStart()
	{
		super.onStart();
		tv.append("Starting networking component tests...\n");
		
		//Bind network component
		Intent intent = new Intent(this, NetworkComService.class);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}
	
	private ServiceConnection connection = new ServiceConnection() {
		
		public void onServiceDisconnected(ComponentName name) {
			networkBound = false;			
		}
		
		public void onServiceConnected(ComponentName name, IBinder service) {
			//Gets us an instance of the binder for the service.
			binder = (NetworkComBinder)service;
			networkBound = true;
			tv.append("Service connected! Starting connection...\n");
			
			bindHandlers();			
			
			if(binder.ConnectToServer())
			{
				tv.append("Connection to server successful!\n");
			}
			else
			{
				tv.append("Failed to connect to server....\n");
			}
			
		}
	};
	
	class eventHandler extends Handler{
		
		public void handleMessage(Message msg) 
		{
			tv.append("Event Received: ");
			switch (msg.what)
			{
				case CONNECTION_ESTABLISHED:
					tv.append("Connection established with host!\n");
					break;
					
				case CONNECTION_LOST:
					tv.append("Connect to host lost...\n");
					break;
					
				default:
					super.handleMessage(msg);
			}
		}
	}
	
	final Messenger eventMessenger = new Messenger(new eventHandler());
	
	final int CONNECTION_ESTABLISHED = 0;
	final int CONNECTION_LOST = 1;
	
	private void bindHandlers()
	{
		binder.addListener(ConnectionEstablishedListener.class, new ConnectionEstablishedListener() {			
			public void EventOccured(NetworkEvent e) {					
				try {
					eventMessenger.send(Message.obtain(null, CONNECTION_ESTABLISHED, e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}				 			
			}
		});
		
		binder.addListener(ConnectionLostListener.class, new ConnectionLostListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					eventMessenger.send(Message.obtain(null, CONNECTION_LOST, e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});
	}
	
	public void onStop()
	{
		super.onStop();
	}
	
	public void onDestroy()
	{
		super.onDestroy();
	}

}
