package com.Lobretimgap.NetworkClient.Threads;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import networkTransferObjects.NetworkMessage;
import networkTransferObjects.NetworkMessageLarge;
import networkTransferObjects.NetworkMessageMedium;
import networkTransferObjects.PlayerRegistrationMessage;
import android.util.Log;

import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.EventListeners.ConnectionEstablishedListener;
import com.Lobretimgap.NetworkClient.EventListeners.ConnectionFailedListener;
import com.Lobretimgap.NetworkClient.EventListeners.ConnectionLostListener;
import com.Lobretimgap.NetworkClient.EventListeners.DirectMessageListener;
import com.Lobretimgap.NetworkClient.EventListeners.GamestateReceivedListener;
import com.Lobretimgap.NetworkClient.EventListeners.LatencyUpdateListener;
import com.Lobretimgap.NetworkClient.EventListeners.NetworkEventListener;
import com.Lobretimgap.NetworkClient.EventListeners.PartialGamestateReceivedListener;
import com.Lobretimgap.NetworkClient.EventListeners.RequestReceivedListener;
import com.Lobretimgap.NetworkClient.EventListeners.UnknownMessageTypeReceivedListener;
import com.Lobretimgap.NetworkClient.EventListeners.UpdateReceivedListener;
import com.Lobretimgap.NetworkClient.Events.DirectCommunicationEvent;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;
import com.Lobretimgap.NetworkClient.Exceptions.NotYetRegisteredException;
import com.Lobretimgap.NetworkClient.Peer2Peer.ClientPeer;
import com.Lobretimgap.NetworkClient.Utility.EventListenerList;
import com.Lobretimgap.NetworkClient.Utility.GameClock;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

public abstract class CoreNetworkThread extends Thread 
{
	private Socket socket;
	private NetworkWriteThread out;
	private InputStream in;
    private boolean stopOperation = false;
    private boolean keepAliveBreak = false;
    public boolean isRunning = false;
    LinkedBuffer buffer = LinkedBuffer.allocate(512);
    ByteBuffer b = ByteBuffer.allocate(4);
    private boolean connected = false;
    private boolean awaitingLatencyResponse = false;
    private int playerId = -1;
    
    private static Schema<PlayerRegistrationMessage> playerRegSchema = RuntimeSchema.getSchema(PlayerRegistrationMessage.class);
    private static Schema<NetworkMessageMedium> mediumMsgSchema = RuntimeSchema.getSchema(NetworkMessageMedium.class);
    private static Schema<NetworkMessageLarge> largeMsgSchema = RuntimeSchema.getSchema(NetworkMessageLarge.class);
    private static Schema<NetworkMessage> networkMsgSchema = RuntimeSchema.getSchema(NetworkMessage.class);
    
    private long latencyStartTime, latencyEndTime;    
    private EventListenerList listeners = new EventListenerList();    
    public Vector<ClientPeer> peers;
    private GameClock gameClock;
	
	public CoreNetworkThread()
	{
		peers = new Vector<ClientPeer>();
		b.order(ByteOrder.BIG_ENDIAN);		
		gameClock = new GameClock();		
	}
	
	/**
	 * Asynchronously connects to the server
	 */
	public void connect()
	{
		if(!connected)
			this.start();
	}
	
	private void connectToServer()
	{
		boolean success = true;
		try
		{
			Inet4Address hostAddress = (Inet4Address)InetAddress.getByName(NetworkVariables.hostname);
			socket = new Socket(hostAddress, NetworkVariables.port);
			in = socket.getInputStream();
			out = new NetworkWriteThread(socket);		
			out.start();
			
			fireEvent(new NetworkEvent(this, "Connection successfully established!"), ConnectionEstablishedListener.class);
		}
		catch(UnknownHostException e)
		{
			Log.e(NetworkVariables.TAG, "Failed to resolve host.", e);	
			success = false;
		}
		catch(IOException e)
		{
			Log.e(NetworkVariables.TAG, "Unable to connect to server.");	
			success = false;
		}
		finally
		{
			if(!success)
			{
				fireEvent(new NetworkEvent(this, "Failed to connect to server... See log for details."), ConnectionFailedListener.class);				
			}
			else
			{
				connected = true;
			}
		}
		Log.i(NetworkVariables.TAG, "Connection to server has been established.");
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
	
	/**
	 * Will make a request to the server to check out what kind of latency there is between android device and server.
	 * Response will come as a latencyUpdateEvent.
	 */
	public void requestNetworkLatency()
	{
		if(!awaitingLatencyResponse)
		{
			awaitingLatencyResponse = true;
			NetworkMessage msg = new NetworkMessage("Requesting Latency check");
			msg.setMessageType(NetworkMessage.MessageType.LATENCY_REQUEST_MESSAGE);
			writeOut(msg);
			latencyStartTime = System.currentTimeMillis();
		}
	}
	
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
      * Sends a message directly to the player with the given player ID. Uses P2P communication
      * if possible, but if that isn't possible, it will route the message via the game server.
      * @param message The message to send to the peer. Must be one of the standard NetworkMessages
      * (NetworkMessage, NetworkMessageMedium or NetworkMessageLarge)
      * @param targetPlayerId The player ID of the player we wish to send this message too. If the
      * player ID does not exist, this message will be lost in transit, with no notification.
      */
    public void sendDirectCommunicationMessage(NetworkMessage message, int targetPlayerId)
    {
    	
    	if(playerId != -1)
    	{
	    	//We need to turn this message into something that can hold additional info, such as
	    	//the target player ID. So if it isn't large or medium, change it into a medium message.
	    	if(message instanceof NetworkMessageLarge)
	    	{
	    		((NetworkMessageLarge)message).integers.add(targetPlayerId);
	    		((NetworkMessageLarge)message).integers.add(playerId);
	    	}else if (message instanceof NetworkMessageMedium)
	    	{
	    		((NetworkMessageMedium)message).integers.add(targetPlayerId);
	    		((NetworkMessageLarge)message).integers.add(playerId);
	    	}else
	    	{
	    		NetworkMessageMedium msg = new NetworkMessageMedium();
	    		msg.setMessage(message.getMessage());
	    		msg.integers.add(targetPlayerId);
	    		msg.integers.add(playerId);
	    		message = msg;
	    	}
	    	message.setMessageType(NetworkMessage.MessageType.DIRECT_COMMUNICATION_MESSAGE);
	    	writeOut(message);
    	}
    	else
    	{
    		throw new NotYetRegisteredException("Must have a registered playerID from the game server before sending direct messages!");
    	}
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
    
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public void run()
	{
		if(!connected)
		{
			connectToServer();
		}
		if(connected)
		{
			isRunning = true;
			registerWithServer(getPlayerRegistrationInformation());
	        //Do running stuff        
	        while(!stopOperation)
	        {
	            try
	            {
	            	NetworkMessage msg = null;
	            	Schema schema = null;

	                //Expecting 6 bytes of length info
	                byte [] messageHeader = new byte [5];
	                int success = in.read(messageHeader);
	                if(success == 5)
	                {
	                    //Chop off message type modifier
	                    byte classType = messageHeader[0];	                    
	                  //set the message and schema to the correct type
	                    switch(classType)
	                    {
	                        case -1:
	                            //Keep alive message, ignore and wait for a new message
	                            keepAliveBreak = true;
	                            break;
	                        case 1:
	                            msg = new PlayerRegistrationMessage();
	                            schema = playerRegSchema;
	                            break;
	                        case 2:
	                            msg = new NetworkMessageMedium();
	                            schema = mediumMsgSchema;
	                            break;
	                        case 3:
	                            msg = new NetworkMessageLarge();
	                            schema = largeMsgSchema;
	                            break;
	                        default:
	                            msg = new NetworkMessage();
	                            schema = networkMsgSchema;
	                    }
	                    if(!keepAliveBreak)
	                    {
	                        //Determine message length
	                        b.clear();
	                        b.put(messageHeader, 1, 4);
	                        b.rewind();
	                        int mSize = b.getInt();

	                        //Read in the object bytes
	                        byte [] object = new byte [mSize];
	                        int bytesRead = 0;
	                        while(bytesRead != mSize)
	                        {
	                            bytesRead += in.read(object, bytesRead, object.length - bytesRead);
	                        }

	                        //System.out.println("Mid receive, byte buffer at "+bytesRead);
	                        ProtostuffIOUtil.mergeFrom(object, msg, schema);
	                        processNetworkMessage(msg);
	                    }  
	                }
	                else
	                {//Failed to read in length field properly
	                    if(success == -1)
	                    {//Stream closed
	                        System.err.println("End of stream!");
	                        shutdownThread();
	                    }
	                } 
	            }
	            catch(InterruptedIOException e)
	            {
	                //We expect that something wants the threads attention. This is
	                //used to immediately end the thread in shutdownThread().
	            }
	            catch(IOException e)
	            {
	                System.err.println("Error occured while reading from thread : "+e);
	                fireEvent(new NetworkEvent(this, "Connection to Server lost!\n" + e),  ConnectionLostListener.class);
	                this.shutdownThread();                
	                break;
	            }            
	            catch(NullPointerException e)
	            {
	            	Log.e(NetworkVariables.TAG, "Null Pointer Exception in run loop.", e);
	            }
	            catch(RuntimeException e)
	            {
	                System.err.println("Failed to deserialize object! Perhaps it had fields that could not be correctly serialized?");
	            }
				finally
	            {
	            	buffer.clear();
	            }
	        }
		}
		else
		{
			shutdownThread();
		}
    }
	
	/**
	 * Reimplementation of Arrays.copyOf, since android version 8 has not yet included it. 
	 * It appears in android 9 onwards.
	 * @param original
	 * @param newLength
	 * @return
	 */
	public byte[] copyOf(byte [] original, int newLength)
	{
		byte [] fresh = new byte[newLength];
		for(int i = 0; (i < fresh.length) && (i < original.length);i++)
		{
			fresh[i] = original[i];
		}
		
		if(fresh.length > original.length)
		{
			for(int i = original.length; i < fresh.length; i++)
			{
				fresh[i] = 0;
			}
		}		
		return fresh;
	}
	
	private void processNetworkMessage(NetworkMessage message)
	{
		if(message instanceof PlayerRegistrationMessage)
        {
        	playerId = ((PlayerRegistrationMessage)message).playerID;
        	Log.i(NetworkVariables.TAG, "Received player ID of "+playerId+" from the server.");
        }		
		else if(message instanceof NetworkMessage)
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
	            case LATENCY_REQUEST_MESSAGE:
	            	//Respond by sending a latency response message asap.
	            	NetworkMessage latMsg = new NetworkMessage("Latency Response");
	            	latMsg.setMessageType(NetworkMessage.MessageType.LATENCY_RESPONSE_MESSAGE);
	            	writeOut(latMsg);
	            	break;
	            case LATENCY_RESPONSE_MESSAGE:
	            	//WE have received a response to an earlier latency request.
	            	awaitingLatencyResponse = false;
	            	latencyEndTime = System.currentTimeMillis();
	            	if(latencyStartTime < latencyEndTime)
	            	{
	            		fireEvent(new NetworkEvent(this, (latencyEndTime - latencyStartTime)),  LatencyUpdateListener.class);
	            	}
	            	break;
	            case DIRECT_COMMUNICATION_MESSAGE:
	            	//We added a src and dest integer to message before sending it at the other client,
	            	//so we need to remove that info now, and add it to the network event. Additionally, we know
	            	//the message is either a large or medium network message (we changed it to medium if it was 
	            	//a normal network message.)
	            	
	            	if(msg instanceof NetworkMessageLarge)
	            	{
	            		int listSize = ((NetworkMessageLarge)msg).integers.size();
	            		int sourcePlayerId = ((NetworkMessageLarge)msg).integers.get(listSize - 1);
	            		//listSize -1 is the src, listSize -2 is the dest (IE us)
	            		((NetworkMessageLarge)msg).integers.remove(listSize - 1);
	            		((NetworkMessageLarge)msg).integers.remove(listSize - 2);
	            		
	            		fireEvent(new DirectCommunicationEvent(this, msg, sourcePlayerId), DirectMessageListener.class);
	            	}else if (msg instanceof NetworkMessageMedium)
	            	{
	            		int listSize = ((NetworkMessageMedium)msg).integers.size();
	            		int sourcePlayerId = ((NetworkMessageMedium)msg).integers.get(listSize - 1);
	            		//listSize -1 is the src, listSize -2 is the dest (IE us)
	            		((NetworkMessageMedium)msg).integers.remove(listSize - 1);
	            		((NetworkMessageMedium)msg).integers.remove(listSize - 2);
	            		
	            		fireEvent(new DirectCommunicationEvent(this, msg, sourcePlayerId), DirectMessageListener.class);
	            	}
	            	else
	            	{ //Should never be anything other than NetworkMessage medium or large.
	                  //If you want to add your own types of direct communication, add special cases here
	            		Log.w(NetworkVariables.TAG, "Attempt at direct peer communication with unrecognised object - Ignoring.");            		
	            	}
	            	
	            	break;
	            default:
	                fireEvent(new NetworkEvent(this, msg),  UnknownMessageTypeReceivedListener.class);
	                //throw new UnsupportedOperationException("Message type has not been catered for. Please include handling code for it!");
	                break;        
	        }
		}
	}
	
	@SuppressWarnings("unchecked")
	private void handlePeerListUpdate(NetworkMessage msg)
	{
		//Terminate communication with existing peers
		
		//And now replace them with a new set of peers. 
		//peers = (Vector<ClientPeer>)msg.getDataObject("peerList");
		Log.d(NetworkVariables.TAG, "New Peer list received.");
		Log.d(NetworkVariables.TAG, "New Peer list received.");
		
		//Now try to connect to the new list of peers
	}
	
	/*
     * Writes a given object to the outputstream
     */
    private void writeOut(NetworkMessage object) throws BufferOverflowException
    {
        //Later perhaps we can more gracefully deal with this. Perhaps add wait
        //a little while and then try again?
    	try
    	{
    		if(object != null)
    		{
    			out.writeMessage(object);
    		}
    		else
    		{
    			Log.w(NetworkVariables.TAG, "Attempted to write null object!");
    		}
    	}
    	catch(NullPointerException e)
    	{
    		//No idea why this sometimes happens
    	}
    }
    	

    public void shutdownThread()
    {
        try
        {
        	isRunning = false;
        	if(out != null)
        		out.shutdownThread();
            stopOperation = true;
            this.interrupt();
            if(socket != null)
            	socket.close();
            connected = false;
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
