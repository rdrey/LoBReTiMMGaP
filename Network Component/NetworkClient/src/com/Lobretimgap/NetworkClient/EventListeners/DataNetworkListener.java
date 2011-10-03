package com.Lobretimgap.NetworkClient.EventListeners;

import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class DataNetworkListener extends PhoneStateListener {
	CoreNetworkThread thread;
	
	public DataNetworkListener(CoreNetworkThread callingThread)
	{
		super();
		thread = callingThread;
	}

	public void onDataConnectionStateChanged(int state, int networkType)
	{
		String networkTypeString = getNetworkTypeString(networkType);
		Log.i("networks", "Data Network Changed to: "+networkTypeString);
		
		thread.requestNetworkTimeSync();
	}
	
	public static String getNetworkTypeString(int networkType)
	{
		switch(networkType)
		{
			case TelephonyManager.NETWORK_TYPE_GPRS:
				return "GPRS";
		case TelephonyManager.NETWORK_TYPE_EDGE:
				return "EDGE";
		case TelephonyManager.NETWORK_TYPE_UMTS:
				return "UMTS";
		case TelephonyManager.NETWORK_TYPE_CDMA:
				return "CDMA";
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
				return "EVDO_0";
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
				return "EVDO_A";
		case TelephonyManager.NETWORK_TYPE_1xRTT:
				return "1xRTT";
		case TelephonyManager.NETWORK_TYPE_HSDPA:
				return "HSDPA";
		case TelephonyManager.NETWORK_TYPE_HSUPA:
				return "HSUPA";
		case TelephonyManager.NETWORK_TYPE_HSPA:
				return "HSPA";
		case TelephonyManager.NETWORK_TYPE_IDEN:
				return "iDen";
		default:
				return "Unknown";
		}
	}
}
