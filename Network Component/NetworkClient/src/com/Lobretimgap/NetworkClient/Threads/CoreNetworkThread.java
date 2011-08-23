package com.Lobretimgap.NetworkClient.Threads;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.util.Vector;

import networkTransferObjects.NetworkMessage;
import networkTransferObjects.PlayerRegistrationMessage;

import android.util.Log;

import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.Utility.EventListenerList;
import com.Lobretimgap.NetworkClient.EventListeners.*;
import com.Lobretimgap.NetworkClient.Events.*;
import com.Lobretimgap.NetworkClient.Peer2Peer.*;

public abstract class CoreNetworkThread extends Thread 
{
	private Socket socket;
	private NetworkWriteThread out;
	private ObjectInputStream in;
    private boolean stopOperation = false;
    public boolean isRunning = false;
    
    private EventListenerList listeners = new EventListenerList();
    
    public Vector<ClientPeer> peers;
	
	public CoreNetworkThread()
	{
		peers = new Vector<ClientPeer>();
	}
	
	public boolean connect()
	{
		try
		{
			Inet4Address hostAddress = (Inet4Address)InetAddress.getByName(NetworkVariables.hostname);
			socket = new Socket(hostAddress, NetworkVariables.port);
			in = new ObjectInputStream(socket.getInputStream());
			out = new NetworkWriteThread(socket);
			out.start();
		}
		catch(UnknownHostException e)
		{
			Log.e(NetworkVariables.TAG, "Failed to resolve host.", e);	
			return false;
		}
		catch(IOException e)
		{
			Log.e(NetworkVariables.TAG, "Error while initializing connection to server.", e);	
			return false;
		}
		Log.i(NetworkVariables.TAG, "Connection to server has been established.");
		return true;
	}
	
	
	
	/**
	 * This method must be implemented so that the network component knows what type of initialisation 
	 * information must be sent to the server. The only absolutely required fields in the PlayerRegistrationMessage
	 * are the playerName and playerId. These are used to uniquely identify the player, and are used for
	 * network component logic.
	 * 
	 * You should also use this message as a way of transferring any other initial information to the game server, 
	 * such as initial position, starting game/character states, etc.
	 * 
	 * @return A PlayerRegistrationMessage object which the server will use to instantiate the player on its side. 
	 */
	public abstract PlayerRegistrationMessage getPlayerRegistrationInformation();
	
	/**
	 * Will be called once the initial game state from the server is received.
	 * This message should process this initial game information and appropriatly
	 * configure the local game state.
	 * @param message A network message representing the initial game state received
	 * from the server.
	 */
	public abstract void processInitialGameState(NetworkMessage message);
	
	private void registerWithServer(PlayerRegistrationMessage message)
	{
		writeOut(message);
	}
	
	/**
     * Sends an update message to the server. Can be anything the implementer
     * chooses. Used for general communication with the server.
     */
    public void sendGameUpdate(NetworkMessage message) throws BufferOverflowException
    {
        message.setMessageType(NetworkMessage.MessageType.UPDATE_MESSAGE);
        writeOut(message);
    }

    /**
     * This method should be used to request information from the server. The message
     * sent to the client should generally be one that requires a response, such as
     * what your rank is.
     */
    public void sendRequest(NetworkMessage message)throws BufferOverflowException
    {
        message.setMessageType(NetworkMessage.MessageType.REQUEST_MESSAGE);
        writeOut(message);
    }
    
    /**
     * If the local gamestate should become compromised in some way, and the local game would like to get a 
     * fresh copy of the game state, this method should be used to request it from the server.
     */
    public void sendGameStateRequest(NetworkMessage message) throws BufferOverflowException
    {
    	message.setMessageType(NetworkMessage.MessageType.GAMESTATE_REQUEST_MESSAGE);
        writeOut(message);
    }
    
    /**
     * Before ending the game session, a termination message should be sent to the server. 
     * Things that need to be saved at the server (high scores, items, saved games) should
     * be transferred at this point. After calling this method, the network component will
     * begin to shut down, and will become unavailable.
     */
    public void sendTerminationRequest(NetworkMessage message) throws BufferOverflowException
    {
    	message.setMessageType(NetworkMessage.MessageType.TERMINATION_REQUEST_MESSAGE);
        writeOut(message);
        shutdownThread();
    }
	
    /**
     * By default, the network component will deal with the P2P connections invisibly, updating as it sees
     * fit. If the implementer would like to force the peer list to be updated for some reason (a player has
     * rapidly changed location, like a teleport or change of map or something) then this method can 
     * be called to force the peer list to be refreshed from the server.
     * 
     * Note that if the game state has recently radically changed, and this is why we are forcing a peer list 
     * update, you should make sure that the prerequisite game state information has already been sent to the 
     * server, otherwise the peer list received might still be inaccurate.
     */
    public void forcePeerListUpdate()
    {
    	requestPeerList();
    }
    
    private void requestPeerList()
    {
    	NetworkMessage message = new NetworkMessage("Requesting new peer list.");
    	message.setMessageType(NetworkMessage.MessageType.PEER_LIST_REQUEST_MESSAGE);
        writeOut(message);
    }
    
	@Override
    public void run()
	{		
		isRunning = true;
		registerWithServer(getPlayerRegistrationInformation());
        //Do running stuff        
        while(!stopOperation)
        {
            try
            {
                Object data = in.readObject();                
                processNetworkMessage(data);
            }
            catch(InterruptedIOException e)
            {
                //We expect that something wants the threads attention. This is
                //used to immediately end the thread in shutdownThread().
            }
            catch(IOException e)
            {
                System.err.println("Error occured while reading from thread : "+e);
                fireEvent(new NetworkEvent(this, "Connection to client lost!\n" + e),  ConnectionLostListener.class);
                this.shutdownThread();                
                break;
            }
            catch(ClassNotFoundException e)
            {
                System.err.println("Unrecognised class object received from client - ignoring");
            } 
        }
    }
	
	private void processNetworkMessage(Object message)
	{
		if(message instanceof NetworkMessage)
        {
            NetworkMessage msg = (NetworkMessage)message;
            switch(msg.getMessageType())
            {
                case UPDATE_MESSAGE:
                    fireEvent(new NetworkEvent(this, msg),  UpdateReceivedListener.class);
                    break;
                case REQUEST_MESSAGE:
                    fireEvent(new NetworkEvent(this, msg),  RequestReceivedListener.class);
                    break;
                case INITIAL_GAME_STATE_MESSAGE:
                	processInitialGameState(msg);
                	break;
                case PARTIAL_GAMESTATE_UPDATE_MESSAGE:
                	fireEvent(new NetworkEvent(this, msg),  PartialGamestateReceivedListener.class);
                    break;
                case GAMESTATE_UPDATE_MESSAGE:
                	fireEvent(new NetworkEvent(this, msg),  GamestateReceivedListener.class);
                    break;
                case GAMESTATE_REQUEST_MESSAGE:
                    //Used exclusively on the server side
                    break;
                case TERMINATION_REQUEST_MESSAGE:
                    //similarly, dealt with on the server side
                    break;
                case PEER_LIST_MESSAGE:                	
                	handlePeerListUpdate(msg);
                	break;
                case PEER_LIST_REQUEST_MESSAGE:
                    //Also dealt with on the server side.
                    break;
                default:
                    fireEvent(new NetworkEvent(this, msg),  UnknownMessageTypeReceivedListener.class);
                    //throw new UnsupportedOperationException("Message type has not been catered for. Please include handling code for it!");

            }
        }
	}
	
	@SuppressWarnings("unchecked")
	private void handlePeerListUpdate(NetworkMessage msg)
	{
		//Terminate communication with existing peers
		
		//And now replace them with a new set of peers. 
		peers = (Vector<ClientPeer>)msg.getDataObject("peerList");
		
		//Now try to connect to the new list of peers
	}
	
	/*
     * Writes a given object to the outputstream
     */
    private void writeOut(Object object) throws BufferOverflowException
    {
        //Later perhaps we can more gracefully deal with this. Perhaps add wait
        //a little while and then try again?
        if(!out.writeMessage(object))
        {
            throw new BufferOverflowException();
        }
    }

    public void shutdownThread()
    {
        try
        {
        	isRunning = false;
            out.shutdownThread();
            stopOperation = true;
            this.interrupt();
            socket.close();
        }
        catch(IOException e)
        {
            //We don't really care if the socket failed to close correctly.
            System.err.println("Socket failed to close correctly. \n"+e);
        }  
    }
    
    /************************************************* Event Handling *********************************************************/
    
    public <T extends NetworkEventListener> void addNetworkListener(Class<T> t, T listener)
    {
        listeners.add(t, listener);
    }
    
    public <T extends NetworkEventListener> void removeNetworkListener(Class<T> t, T listener)
    {
        listeners.remove(t, listener);
    }
    
    /**
     * Used to fire off a network event in a generalised manner. Takes the event to
     * be fired (all events are expected to be children of NetworkEvent) and the
     * class of the listener to be notified of the event.
     * @param <T> The type of listener that we wish to fire events on.
     * @param event The event we would like to propagate to the event listeners
     * @param t The class type of the listeners we wish to fire events on.
     */
    private <T extends NetworkEventListener> void fireEvent(NetworkEvent event, Class<T> t)
    {
        Object[] listenerArray = listeners.getListenerList();

        //Loop through the listeners, notifying those of the correct type.
        for(int i = 0; i < listenerArray.length; i+=2)
        {
            if(listenerArray[i] == t)
            {
                /*
                 * This might seem a little confusing at first, so here's an explanation.
                 * The listener list stores event listeners in pairs of
                 * {listener.class, instance of listener}. This means that
                 * i will be the class of the listener, which we compare with our
                 * given class type T. If it matches we cast the instance of the
                 * Listener as our given class type T (which we have checked, it actually
                 * is) and then run its EventOccured method.
                 */
                t.cast(listenerArray[i+1]).EventOccured(event);
            }
        }
    }
}
