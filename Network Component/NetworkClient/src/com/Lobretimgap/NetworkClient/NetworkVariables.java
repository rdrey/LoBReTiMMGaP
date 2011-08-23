package com.Lobretimgap.NetworkClient;

import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;

public class NetworkVariables {
	public static final String TAG = "NetworkClient";
	public static final int port = 10282;
	public static final String hostname = "blue.cs.uct.ac.za";
	public static final int writeThreadBufferSize = 16;
	public static final int initialNetworkMessageMapSize = 8;
	
	//Replace CoreNetworkThread.class with a concrete implementation of that class.
	public static final Class<CoreNetworkThread> coreNetworkThreadClass = CoreNetworkThread.class;
	
	/**
	 * Uses the class type above to create a concrete instance of the CoreNetworkThread 
	 * class.
	 * @return Returns an instance of the CoreNetworkThread implementation
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static CoreNetworkThread getInstance() throws IllegalAccessException, InstantiationException
	{
		return (CoreNetworkThread) coreNetworkThreadClass.newInstance();
	}
	
}
