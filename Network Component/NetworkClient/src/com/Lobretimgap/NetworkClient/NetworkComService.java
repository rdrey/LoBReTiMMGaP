package com.Lobretimgap.NetworkClient;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import networkTransferObjects.NetworkMessage;

import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class NetworkComService <T extends CoreNetworkThread> extends Service{	

	
	private IBinder binder;
	private Timer logSaver;
	
	public void onCreate()
	{	
		try {						
			binder = new NetworkComBinder(getApplicationContext());
			
			logSaver.schedule(new TimerTask() {
				
				@Override
				public void run() {
					
					try
					{
						
						String filePrefix = Environment.getExternalStorageDirectory()+"/";
						
						String cmd = "logcat -d -v time >> "+filePrefix+"fullLog.log";					
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -s NetworkClient >> "+filePrefix+"networkclientLog.log";
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -s bandwidth >> "+filePrefix+"bandwidthLog.log";
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -s compression >> "+filePrefix+"compressionLog.log";
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -s latency >> "+filePrefix+"latencyLog.log";
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -s networks >> "+filePrefix+"networksLog.log";
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -s timesync >> "+filePrefix+"timesyncLog.log";
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -c";
					    Runtime.getRuntime().exec(cmd);
				    
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
				}
			}, 1000, 60000);
			
		} catch (IllegalAccessException e) {
			Log.e(NetworkVariables.TAG, "Error thrown in Network thread instantiation: " + e.getMessage(), e);			
		} catch (InstantiationException e) {
			Log.e(NetworkVariables.TAG, "Error thrown in Network thread instantiation: " + e.getMessage(), e);
		}		
	}
		
	@Override
	public IBinder onBind(Intent intent) {
		if(binder != null)
		{
			return binder;
		}
		else
		{
			return null;
		}
	}
	
	public void onDestroy()
	{
		if(binder.isBinderAlive())
		{
			if(((NetworkComBinder)binder).isConnectedToServer())
			{
				((NetworkComBinder)binder).sendTerminationRequest(new NetworkMessage("Automatic Termination"));
			}
			
		}
		logSaver.cancel();
		super.onDestroy();
	}

}
