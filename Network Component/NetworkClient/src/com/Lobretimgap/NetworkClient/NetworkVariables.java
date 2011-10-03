package com.Lobretimgap.NetworkClient;


import com.Lobretimgap.NetworkClient.Implementation.NetworkThreadImplementation;
import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;



public class NetworkVariables {
	public static final String TAG = "NetworkClient";
	public static final int port = 10282;
	public static final String hostname = "46.137.162.239";//"blue.cs.uct.ac.za";
	//public static final String hostname = "192.168.42.101";//"blue.cs.uct.ac.za";
	//public static final String hostname = "137.158.60.206";//"blue.cs.uct.ac.za";
	//public static final String hostname = "paymentportal.co.za";//"blue.cs.uct.ac.za";
	public static final int writeThreadBufferSize = 256;
	public static final int initialNetworkMessageMapSize = 8;
	
	/* Uses approximately 1.5MB of additional data per hour in order to make sure that the
	 * connection to the server is always completely active and available. This can reduce
	 * the latency of periodic calls to the game server by as much as 140ms. Its also 
	 * possible this uses more battery power. Has no effect on wifi
	 */
	public static final boolean keepAliveEnabled = false;
	
	//Replace CoreNetworkThread.class with a concrete implementation of that class.
	public static final Class<NetworkThreadImplementation> coreNetworkThreadClass = NetworkThreadImplementation.class;
	
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
