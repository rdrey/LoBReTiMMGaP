package com.Lobretimgap.NetworkClient.Implementation;

import com.Lobretimgap.NetworkClient.NetworkComBinder;
import com.Lobretimgap.NetworkClient.NetworkComService;
import com.Lobretimgap.NetworkClient.EventListeners.ConnectionEstablishedListener;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
			
			binder.addListener(ConnectionEstablishedListener.class, new ConnectionEstablishedListener() {				
				
				public void EventOccured(NetworkEvent e) {					
					tv.append("Event Received: Connection Established to server!\n");					
				}
			});
			
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
	
	public void onStop()
	{
		super.onStop();
	}
	
	public void onDestroy()
	{
		super.onDestroy();
	}

}
