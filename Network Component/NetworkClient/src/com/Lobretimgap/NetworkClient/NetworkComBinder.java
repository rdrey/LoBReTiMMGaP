package com.Lobretimgap.NetworkClient;

import java.nio.BufferOverflowException;
import networkTransferObjects.NetworkMessage;
import com.Lobretimgap.NetworkClient.EventListeners.*;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;
import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;
import android.os.Binder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

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
	 * Requests the latency from the network component. Result is 
	 * returned as a network event.
	 */
	public void requestLatency()
	{
		networkThread.requestNetworkLatency();
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
	
	/** 
	 * Enum describing the types of messages that might be sent to the messenger.
	 * Compare them using EventType.values()[i]
	 * @author Lawrence
	 *
	 */
	public enum EventType
	{
		CONNECTION_ESTABLISHED,
		CONNECTION_LOST,
		GAMESTATE_RECEIVED,
		LATENCY_UPDATE_RECEIVED,
		PARTIAL_GAMESTATE_RECEIVED,
		REQUEST_RECEIVED,
		UNKNOWN_MESSAGE_TYPE_RECEIVED,
		UPDATE_RECEIVED
		
	}
	
	/**
	 * An android utility function that registers a messenger for all the available network events.
	 * The event object will be passed to the Messenger whenever any event occurs, using the 
	 * message.what parameter to describe what type of event it is. The message.what parameter is
	 * and integer representation of the EventType enum in this class.
	 * @param eventMessenger
	 */
	public void registerMessenger(final Messenger eventMessenger)
	{
		addListener(ConnectionEstablishedListener.class, new ConnectionEstablishedListener() {			
			public void EventOccured(NetworkEvent e) {					
				try {
					eventMessenger.send(Message.obtain(null, EventType.CONNECTION_ESTABLISHED.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}				 			
			}
		});
		
		addListener(ConnectionLostListener.class, new ConnectionLostListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					eventMessenger.send(Message.obtain(null, EventType.CONNECTION_LOST.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});
		
		addListener(GamestateReceivedListener.class, new GamestateReceivedListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					eventMessenger.send(Message.obtain(null, EventType.GAMESTATE_RECEIVED.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});	
		
		addListener(LatencyUpdateListener.class, new LatencyUpdateListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					eventMessenger.send(Message.obtain(null, EventType.LATENCY_UPDATE_RECEIVED.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});	
		
		addListener(PartialGamestateReceivedListener.class, new PartialGamestateReceivedListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					eventMessenger.send(Message.obtain(null, EventType.PARTIAL_GAMESTATE_RECEIVED.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});		
		
		
		addListener(RequestReceivedListener.class, new RequestReceivedListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					eventMessenger.send(Message.obtain(null, EventType.REQUEST_RECEIVED.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});
		
		addListener(UnknownMessageTypeReceivedListener.class, new UnknownMessageTypeReceivedListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					eventMessenger.send(Message.obtain(null, EventType.UNKNOWN_MESSAGE_TYPE_RECEIVED.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});
		
		addListener(UpdateReceivedListener.class, new UpdateReceivedListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					eventMessenger.send(Message.obtain(null, EventType.UPDATE_RECEIVED.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});		
		
	}
	
}
