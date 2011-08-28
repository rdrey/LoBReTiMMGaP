package com.Lobretimgap.NetworkClient.Implementation;

import java.util.Timer;
import java.util.TimerTask;

import networkTransferObjects.NetworkMessage;
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
import android.widget.TextView;

import com.Lobretimgap.NetworkClient.NetworkComBinder;
import com.Lobretimgap.NetworkClient.NetworkComService;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;

public class NetworkTestApp extends Activity {
	
	private TextView tv;
	@SuppressWarnings("unused")
	private boolean networkBound = false;
	private NetworkComBinder binder;
	
	private final Timer timer = new Timer();
	private final int recurranceDelay = 5; //in seconds

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
			
			binder.registerMessenger(eventMessenger);
			
			if(binder.ConnectToServer())
			{
				tv.append("Connection to server successful!\n");
			}
			else
			{
				tv.append("Failed to connect to server....\n");
			}	
			
			timer.schedule(new TimerTask() {
				@Override
				public void run() {				
					if(networkBound)
						binder.requestLatency();
				}
				
			}, recurranceDelay * 1000, recurranceDelay * 1000);
			
		}
	};	
	
	
	class eventHandler extends Handler{
		
		public void handleMessage(Message msg) 
		{
			tv.append("Event Received: ");
			
			switch (NetworkComBinder.EventType.values()[msg.what])
			{
				case CONNECTION_ESTABLISHED:
					tv.append("Connection established with host!\n");
					break;
					
				case CONNECTION_LOST:
					tv.append("Connect to host lost...\n");
					break;
					
				case LATENCY_UPDATE_RECEIVED:
					tv.append("Latency reported as: " + ((NetworkEvent)msg.obj).getMessage()+"ms\n");
					break;					
				default:
					tv.append("Unrecognised event of type "+ NetworkComBinder.EventType.values()[msg.what] + " received.");
			}
		}
	}
	
	final Messenger eventMessenger = new Messenger(new eventHandler());
		
	
	public void onStop()
	{
		super.onStop();
	}
	
	public void onDestroy()
	{
		super.onDestroy();
	}

}
