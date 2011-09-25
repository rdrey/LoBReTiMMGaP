package com.Lobretimgap.NetworkClient;

import networkTransferObjects.NetworkMessage;

import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class NetworkComService <T extends CoreNetworkThread> extends Service{	

	
	private IBinder binder;
	
	public void onCreate()
	{	
		try {			
			binder = new NetworkComBinder();
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
		super.onDestroy();
	}

}
