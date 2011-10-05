package com.Lobretimgap.NetworkClient;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
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
	private Timer logSaver = new Timer();
	
	public void onCreate()
	{	
		try {						
			binder = new NetworkComBinder(getApplicationContext());
			
			logSaver = new Timer();
			logSaver.schedule(new TimerTask() {
				
				@Override
				public void run() {
					
					try
					{
						Log.w(NetworkVariables.TAG, "Writing logs to sd card...");
						
						Date current = new Date(System.currentTimeMillis());
				        SimpleDateFormat formatter = new SimpleDateFormat("E-dd-MMM");
				        
						String filePrefix = Environment.getExternalStorageDirectory()+"/Logs/"+formatter.format(current)+"/";
						File folder = new File(filePrefix);
						folder.mkdirs();
						
						String cmd = "logcat -d -v time -f "+filePrefix+"fullLog.log *:D";					
					    Process mainRequest = Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -f "+filePrefix+"networkclientLog.log NetworkClient:V *:S";					    
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -f "+filePrefix+"bandwidthLog.log bandwidth:V *:S";					    
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -f "+filePrefix+"compressionLog.log compression:V *:S";					    
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -f "+filePrefix+"latencyLog.log latency:V *:S";					    
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -f "+filePrefix+"networksLog.log networks:V *:S";			    
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -f "+filePrefix+"timesyncLog.log timesync:V *:S";					    
					    Runtime.getRuntime().exec(cmd);
					    
					    // Riz's stuff
					    cmd = "logcat -d -v time -f "+filePrefix+"Location.log Location:V *:S";					    
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -f "+filePrefix+"Network_update.log Network_update:V *:S";					    
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -f "+filePrefix+"Items.log Items:V *:S";					    
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -f "+filePrefix+"Players.log Players:V *:S";					    
					    Runtime.getRuntime().exec(cmd);
					    
					    cmd = "logcat -d -v time -f "+filePrefix+"Battle.log Battle:V *:S";					    
					    Runtime.getRuntime().exec(cmd);
					    
					    //Wait until the full log save has finished. We expect this to take the longest, so we dont need to 
					    //check if the others are finished
					    mainRequest.waitFor();
					    cmd = "logcat -c";
					    Runtime.getRuntime().exec(cmd);
					    
					    Log.w(NetworkVariables.TAG, "Writing of logs completed, system log flushed.");
				    
					}
					catch(IOException e)
					{
						Log.e(NetworkVariables.TAG, "Failed to save logs to sd card!: " + e);
					} catch (InterruptedException e) {						
						e.printStackTrace();
					}
				}
			}, 1000, NetworkVariables.logSaveInterval);
			
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
