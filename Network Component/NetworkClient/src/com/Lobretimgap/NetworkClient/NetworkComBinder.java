package com.Lobretimgap.NetworkClient;

import java.nio.BufferOverflowException;

import networkTransferObjects.NetworkMessage;
import android.os.Binder;

import com.Lobretimgap.NetworkClient.EventListeners.NetworkEventListener;
import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;

public class NetworkComBinder extends Binder {
	
	private CoreNetworkThread networkThread;
	private boolean isConnected = false;
	
	public NetworkComBinder(CoreNetworkThread thread)
	{
		networkThread = thread;
	}
	
	/**
	 * Uses the connection information in NetworkVariables to try 
	 * and establish a connection with the server.
	 * @return True if succeded in establishing the connection, false otherwise.
	 */
	public boolean ConnectToServer()
	{
		if(networkThread.connect())
		{
			isConnected = true;
			networkThread.start();
			return true;
		}
		else
		{
			return false;
		}		
	}
	
	/**
	 * Attempts to send a game update message to the server.
	 * @param msg The message to send to the game server
	 * @return True if message send succeeded, false otherwise.
	 */
	public boolean sendGameUpdate(NetworkMessage msg)
	{
		if(isConnected)
		{
			try
			{
				networkThread.sendGameUpdate(msg);
				return true;
			}
			catch(BufferOverflowException e)
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Attempts to send a request message to the server.
	 * @param msg The request message to send to the game server
	 * @return True if message send succeeded, false otherwise.
	 */
	public boolean sendRequest(NetworkMessage msg)
	{
		if(isConnected)
		{
			try
			{
				networkThread.sendRequest(msg);
				return true;
			}
			catch(BufferOverflowException e)
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Attempts to send a game state update request message to the server.
	 * @param msg Message containing any additional information needed to retrieve
	 * a game state from the server.
	 * @return True if request send succeeded, false otherwise.
	 */
	public boolean sendGameStateRequest(NetworkMessage msg)
	{
		if(isConnected)
		{
			try
			{
				networkThread.sendGameStateRequest(msg);
				return true;
			}
			catch(BufferOverflowException e)
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Attempts to send game termination request to the server. After this message, 
	 * the network thread will shut down, and no more messages can be sent.
	 * @param msg Any final closing information that should be sent to the server (saved high scores etc).
	 * @return True if message send succeeded, false otherwise.
	 */
	public boolean sendTerminationRequest(NetworkMessage msg)
	{
		if(isConnected)
		{
			try
			{
				networkThread.sendTerminationRequest(msg);
				isConnected = false;
				return true;
			}
			catch(BufferOverflowException e)
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Forces the networking component to update its list of peers.
	 * @return	 True if the operation succeeded, false otherwise.
	 */
	public boolean forcePeerListUpdate()
	{
		if(isConnected)
		{
			networkThread.forcePeerListUpdate();
			return true;
		}else
		{
			return false;
		}
	}
	
	/**
	 * Use this method to sign up for various events from the network thread. This is the 
	 * networking component informs the client side game about messages received from the server.
	 * 
	 * @param <T> The type of listener to add. Each listener listens for different network events.
	 * @param t The class type of the listener. IE ConnectionLostListener.Class
	 * @param listener An instance of the actual listener, which will be called every time a network
	 * event of the given type occurs.
	 */
	public <T extends NetworkEventListener> void addListener(Class<T> t, T listener)
	{
		networkThread.addNetworkListener(t, listener);
	}	
	
	
	/**
	 * Removes a network event listener.
	 * @param <T> The type of listener to remove.
	 * @param t The class type of the listener. IE ConnectionLostListener.Class
	 * @param listener The actual instance of the listener to remove. Only this instance will be 
	 * removed, not all listeners of this type.
	 */
	public <T extends NetworkEventListener> void removeListener(Class<T> t, T listener)
	{
		networkThread.removeNetworkListener(t, listener);
	}
	
}
