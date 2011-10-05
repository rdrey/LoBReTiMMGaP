package com.Lobretimgap.NetworkClient;

import java.nio.BufferOverflowException;
import java.nio.channels.AlreadyConnectedException;
import java.util.ArrayList;

import networkTransferObjects.NetworkMessage;
import android.content.Context;
import android.os.Binder;
import com.Lobretimgap.NetworkClient.EventListeners.*;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;
import com.Lobretimgap.NetworkClient.Exceptions.NotYetRegisteredException;
import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;
import com.Lobretimgap.NetworkClient.Utility.GameClock;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
public class NetworkComBinder extends Binder {	
	
	private CoreNetworkThread networkThread;
	private boolean isConnected = false;
	private ArrayList<Messenger> linkedMessengers = new ArrayList<Messenger>();
	private Context context;
	
	public NetworkComBinder(Context appContext) throws IllegalAccessException, InstantiationException
	{
		context = appContext;
		networkThread = NetworkVariables.getInstance();
		networkThread.setContext(context);
		addListener(ConnectionEstablishedListener.class, new ConnectionEstablishedListener() {			
			public void EventOccured(NetworkEvent e) {
				isConnected = true;	
			}
		});
		
		addListener(ConnectionLostListener.class, new ConnectionLostListener() {			
			public void EventOccured(NetworkEvent e) {			
				isConnected = false;									
			}
		});
	}
	
	public boolean isConnectedToServer()
	{
		return isConnected;
	}
	
	/**
	 * Uses the connection information in NetworkVariables to try 
	 * and establish a connection with the server.	
	 */
	public void ConnectToServer()
	{
		if(!isConnected)
		{
			if(networkThread.hasCompletedOperation)
			{
				boolean success = true;
				try {
					networkThread = NetworkVariables.getInstance();
					networkThread.setContext(context);
					addListener(ConnectionEstablishedListener.class, new ConnectionEstablishedListener() {			
						public void EventOccured(NetworkEvent e) {
							isConnected = true;							 			
						}
					});
					
					addListener(ConnectionLostListener.class, new ConnectionLostListener() {			
						public void EventOccured(NetworkEvent e) {			
							isConnected = false;									
						}
					});
					
					networkThread.connectToServerAsync();
				} catch (IllegalAccessException e) {					
					Log.e(NetworkVariables.TAG, "Failed to recreate network thread!\n"+e);
					success = false;
				} catch (InstantiationException e) {
					Log.e(NetworkVariables.TAG, "Failed to recreate network thread!\n"+e);
					success = false;
				}				
				finally
				{
					if(!success)
					{
						for(Messenger m : linkedMessengers)
						{
							try {
								m.send(Message.obtain(null, EventType.CONNECTION_FAILED.ordinal(), 
										new NetworkEvent(this, "Failed to recreate network thread!")));
							} catch (RemoteException e1) {					
								Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
							}	
						}
					}
					else
					{
						try
						{
						for(Messenger m : linkedMessengers)
						{
							registerMessenger(m);
						}
						}
						catch(Exception e)
						{
							Log.w(NetworkVariables.TAG, "Failed to re-register old messengers with new network thread!");
						}
					}
				}
			}
			else
			{	
				networkThread.connectToServerAsync();
			}
		}
		else
		{
			throw new AlreadyConnectedException();
		}
	}
	
	/**
	 * Requests the latency from the network component. Result is 
	 * returned as a network event.
	 */
	public void requestLatency()
	{
		if(isConnected)
		{
			networkThread.requestNetworkLatency();
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
	
	public void sendDirectCommunication(NetworkMessage msg, int destPlayerID)
	{
		networkThread.sendDirectCommunicationMessage(msg, destPlayerID);
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
	
	/***
	 * Returns a clock that is in sync with the server time, allowing for calculations using the network message
	 * time stamps. Adjusting the clock delta is not advised, as it will cause the network component to get 
	 * out of sync with the server. Running forceTimeSync will remedy the problem if it occurs.
	 * @return
	 */
	public GameClock getGameClock()
	{
		return networkThread.gameClock;
	}
	
	/***
	 * Method to get what this players ID is. The player ID is used by the networking component to uniquely 
	 * identify a client.
	 * @return
	 */
	public int getPlayerId()
	{
		if(networkThread.playerId != -1)
		{
			return networkThread.playerId;
		}
		else
		{
			throw new NotYetRegisteredException("PlayerID is not yet available! Need to register with the server first.");
		}
	}
	
	/***
	 * Restarts the time synchronisation protocol with the server, in case our clocks have
	 * gone out of sync. Possible causes of this would be changing from Wifi to EDGE.
	 * If the protocol is already in progress this call is ignored.
	 */
	public void forceTimeSync()
	{
		if(isConnected)
		{
			networkThread.requestNetworkTimeSync();
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
		CONNECTION_FAILED,
		DIRECT_MESSAGE_RECEIVED,
		GAMESTATE_RECEIVED,
		LATENCY_UPDATE_RECEIVED,
		PARTIAL_GAMESTATE_RECEIVED,
		REQUEST_RECEIVED,
		UNKNOWN_MESSAGE_TYPE_RECEIVED,
		UPDATE_RECEIVED,
		PLAYER_REGISTERED
		
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
		//Keep track of who's listening, in case we need to restart the network thread.
		linkedMessengers.add(eventMessenger);
		
		addListener(ConnectionEstablishedListener.class, new ConnectionEstablishedListener() {			
			public void EventOccured(NetworkEvent e) {					
				try {
					isConnected = true;
					eventMessenger.send(Message.obtain(null, EventType.CONNECTION_ESTABLISHED.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}				 			
			}
		});
		
		addListener(ConnectionLostListener.class, new ConnectionLostListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					isConnected = false;
					eventMessenger.send(Message.obtain(null, EventType.CONNECTION_LOST.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});
		
		addListener(ConnectionFailedListener.class, new ConnectionFailedListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					isConnected = false;
					eventMessenger.send(Message.obtain(null, EventType.CONNECTION_FAILED.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});
		
		addListener(DirectMessageListener.class, new DirectMessageListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					eventMessenger.send(Message.obtain(null, EventType.DIRECT_MESSAGE_RECEIVED.ordinal(), e));
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
		
		addListener(PlayerRegisteredListener.class, new PlayerRegisteredListener() {			
			public void EventOccured(NetworkEvent e) {
				try {
					eventMessenger.send(Message.obtain(null, EventType.PLAYER_REGISTERED.ordinal(), e));
				} catch (RemoteException e1) {					
					Log.e(NetworkVariables.TAG, "Failed to send message...", e1);
				}					
			}
		});
		
	}
	
}
