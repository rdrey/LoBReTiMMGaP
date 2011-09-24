package com.Lobretimgap.NetworkClient.Implementation;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import networkTransferObjects.NetworkMessage;
import networkTransferObjects.NetworkMessageLarge;
import networkTransferObjects.NetworkMessageMedium;
import networkTransferObjects.Lokemon.LokemonPlayer;
import networkTransferObjects.Lokemon.LokemonPotion;

import com.Lobretimgap.NetworkClient.NetworkComBinder;
import com.Lobretimgap.NetworkClient.NetworkComService;
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
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class ExampleActivity extends Activity {
	private TextView tv;
	private boolean networkBound = false;
	private NetworkComBinder binder;
	
	private final Timer timer = new Timer();
	private final int recurranceDelay = 1; //in seconds
	
	private int pingsPerformed = 0;
	private int highest=0;
	private int lowest=1000;
	private int total=0;

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
		tv.setMovementMethod(new ScrollingMovementMethod());
		
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
			
			binder.ConnectToServer();			
			
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					if(pingsPerformed < 20)
					{
						if(networkBound)
							binder.requestLatency();
							pingsPerformed++;
						}
					else
					{
						this.cancel();
					}
				}
				
			}, recurranceDelay * 1000, recurranceDelay * 200);
			
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
					tv.append("Connection to host lost...\n");
					break;
					
				case CONNECTION_FAILED:
					tv.append("Failed to connect to host...\n");
					break;
					
				case LATENCY_UPDATE_RECEIVED:
					tv.append("Latency reported as: " + ((NetworkEvent)msg.obj).getMessage()+"ms\n");
					int latency = ((Long)((NetworkEvent)msg.obj).getMessage()).intValue();
					if(latency > highest)
						highest = latency;					
					if(latency < lowest)
						lowest = latency;
					total += latency;
					
					if(pingsPerformed == 20)
					{
						tv.append("Max ="+highest+", min = "+lowest+", average = "+(total/20)+"\n");
					}
					else if(pingsPerformed == 2)
					{
						NetworkMessageMedium medMessage = new NetworkMessageMedium("LocationUpdate");
						medMessage.doubles.add(18.5);
						medMessage.doubles.add(32.5);
						binder.sendGameUpdate(medMessage);
					}
					else if (pingsPerformed == 5)
					{
						//DEBUG
						binder.sendGameStateRequest(new NetworkMessage("GetGameObjects"));
						tv.append("Sent player request...\n");
						
					}
					break;		
					
				case GAMESTATE_RECEIVED:
					NetworkMessage mMsg = (NetworkMessage)(((NetworkEvent)msg.obj).getMessage()); 
					if(mMsg.getMessage().equals("Response:GetGameObjects"))
					{
						tv.append("Received item list from server!\n");
						if(((NetworkMessageLarge)mMsg).objectDict.containsKey("ItemList"))
						{
							Object pl = ((NetworkMessageLarge)mMsg).objectDict.get("ItemList");
							if(pl instanceof ArrayList<?>)
							{
								ArrayList<LokemonPotion> players = (ArrayList<LokemonPotion>)pl;
								tv.append("Item list successfully extracted! Size : "+players.size()+"\n");
								//tv.append("First Item: "+players.get(0).getId()+"\n");
							}
							else
							{
								tv.append("Item list is in object dict, but is not an array list!\n");
							}
							
						}
						else
						{
							tv.append("Item list not in object dict!\n");
						}
						
					}
					else
					{
						tv.append("Received this from server: "+mMsg.getMessage()+"\n");
					}
					break;
				default:
					tv.append("Unrecognised event of type "+ NetworkComBinder.EventType.values()[msg.what] + " received.\n");					
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
