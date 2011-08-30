package networkserver.Threads;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.nio.BufferOverflowException;
import java.util.Vector;
import javax.swing.event.EventListenerList;
import networkTransferObjects.NetworkMessage;
import networkTransferObjects.PlayerRegistrationMessage;
import networkserver.EventListeners.*;
import networkserver.Events.NetworkEvent;
import networkserver.Peer2Peer.ClientPeer;
import networkserver.ServerVariables;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public abstract class ServerDaemonThread extends Thread{

    private Socket socket;
    private ServerDaemonWriteoutThread out;
    private InputStream in;
    private boolean stopOperation = false;
    LinkedBuffer buffer = LinkedBuffer.allocate(512);

    public int playerID;
    public String playerName;

    private long latencyStartTime, latencyEndTime;

    private EventListenerList listeners = new EventListenerList();

    /*
     * Any class extending this should have a parameterless constructor, so that
     * the network component can instantiate a generic instance of it.
     */
    public ServerDaemonThread()
    {
        
    }

    /*
     * Will be called by the ServerCoreThread upon creation of this object.
     */
    public void setSocket(Socket acceptedSocket) throws IOException
    {
        socket = acceptedSocket;
        out = new ServerDaemonWriteoutThread(acceptedSocket);
        in = socket.getInputStream();        
        out.start();
    }

    /**
     * Called once joining information for the player has been received.
     * First method called after the client has connected. Use
     * this to add player information to the game engine/world.
     */
    protected abstract void registerPlayer(PlayerRegistrationMessage initialMessage);
    

    /**
     * Called after RegisterPlayer. This is used to obtain the initial game state
     * in the form of a networkMessage. This is then immediatly passed on to the
     * client.
     */
    protected  abstract NetworkMessage getInitialState();

    /**
     * Method which must return a list of peers that the client can establish
     * peer to peer connections with (or an empty list to disable P2P connections).
     * Guarenteed to only be called after registerPlayer.
     */
    protected abstract Vector<ClientPeer> getPeerList(int playerId, String playerName);

    /**
     * Will make a request to the server to check out what kind of latency there is between android device and server.
     * Response will come as a latencyUpdateEvent.
     */
    public void requestNetworkLatency()
    {
        NetworkMessage msg = new NetworkMessage("Requesting Latency check");
        msg.setMessageType(NetworkMessage.MessageType.LATENCY_REQUEST_MESSAGE);
        writeOut(msg);
        latencyStartTime = System.currentTimeMillis();
    }

    /**
     * Called after RegisterPlayer, meant to send game state initialization
     * information to the client.
     */
    private  void sendInitialState()
    {
        NetworkMessage message = getInitialState();
        message.setMessageType(NetworkMessage.MessageType.INITIAL_GAME_STATE_MESSAGE);
        writeOut(message);
    }

    private void sendPeerList()
    {
        Vector<ClientPeer> peers = getPeerList(playerID, playerName);
        //Set the network address on peers, in case implementer didnt.
        for(int i = 0; i < peers.size(); i++)
        {
            int playerId = peers.elementAt(i).playerId;
            peers.elementAt(i).networkAddress = ServerVariables.playerNetworkAddressList.get(new Integer(playerId));
        }
        //Now send these to the client
        NetworkMessage message = new NetworkMessage("Peer list transfer");
        message.setMessageType(NetworkMessage.MessageType.PEER_LIST_MESSAGE);
        //message.addDataObject("peerList", peers);
        writeOut(message);

    }


    /*
     * Sends an update message to the client. Can be anything the implementer
     * chooses. Used for general communication with the client.
     */
    public void sendGameUpdate(NetworkMessage message) throws BufferOverflowException
    {
        message.setMessageType(NetworkMessage.MessageType.UPDATE_MESSAGE);
        writeOut(message);
    }

    /*
     * This method should be used to request information from the client. The message
     * sent to the client should generally be one that requires a response, such as
     * what your highest offline score was
     */
    public void sendRequest(NetworkMessage message)throws BufferOverflowException
    {
        message.setMessageType(NetworkMessage.MessageType.REQUEST_MESSAGE);
        writeOut(message);
    }

    /*
     * Used to send custom game state information to the client. The contents of
     * this send are up to the implmenter, but the corresponding event will be
     * fired on the client side. The contents of this message are assumed to be
     * larger than that of a standard Game update.
     */
    public void sendPartialGameStateUpdate(NetworkMessage message) throws BufferOverflowException
    {
        message.setMessageType(NetworkMessage.MessageType.PARTIAL_GAMESTATE_UPDATE_MESSAGE);
        writeOut(message);
    }

    /*
     * Sends a full copy of the current game state to the client. What the game
     * state contains depends on the implementer.
     */
     public void sendGameStateUpdate(NetworkMessage message) throws BufferOverflowException
    {
        message.setMessageType(NetworkMessage.MessageType.GAMESTATE_UPDATE_MESSAGE);
        writeOut(message);
    }


    @Override
    public void run()
    {        
        //Do running stuff        
        while(!stopOperation)
        {
            try
            {
                
                NetworkMessage msg = null;
            	Schema schema = RuntimeSchema.getSchema(NetworkMessage.class);
            	ProtostuffIOUtil.mergeFrom(in, msg, schema, buffer);
                processNetworkMessage(msg);
            }
            catch(InterruptedIOException e)
            {
                //We expect that something wants the threads attention. This is
                //used to immediatly end the thread in shutdownThread().
            }
            catch(IOException e)
            {
                System.err.println("Error occured while reading from thread : "+e);
                fireEvent(new NetworkEvent(this, "Connection to client lost!\n" + e),  ConnectionLostListener.class);
                this.shutdownThread();                
                break;
            }
            finally
            {
                buffer.clear();
            }
        }
    }

    private void processNetworkMessage(NetworkMessage message)
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
                    //Dealt with on the client side.
                    break;
                case PARTIAL_GAMESTATE_UPDATE_MESSAGE:
                    //Not used on server side. This flag is used to notify the client of incoming partial game state
                    break;
                case GAMESTATE_UPDATE_MESSAGE:
                    //Similarly, this is used exclusivly on the client side.
                    break;
                case GAMESTATE_REQUEST_MESSAGE:
                    fireEvent(new NetworkEvent(this, msg),  GameStateRequestReceivedListener.class);
                    break;
                case TERMINATION_REQUEST_MESSAGE:
                    fireEvent(new NetworkEvent(this, msg),  TerminationRequestReceivedListener.class);
                    break;
                case PEER_LIST_MESSAGE:
                    //Client uses this to update its peer list.
                    break;
                case PEER_LIST_REQUEST_MESSAGE:
                    sendPeerList();
                    break;
                case LATENCY_REQUEST_MESSAGE:
                    //Respond by sending a latency response message asap.
                    NetworkMessage latMsg = new NetworkMessage("Latency Response");
                    latMsg.setMessageType(NetworkMessage.MessageType.LATENCY_RESPONSE_MESSAGE);
                    writeOut(latMsg);
                    break;
                case LATENCY_RESPONSE_MESSAGE:
                    //WE have received a response to an earlier latency request.
                    latencyEndTime = System.currentTimeMillis();
                    if(latencyStartTime < latencyEndTime)
                    {
                            fireEvent(new NetworkEvent(this, (latencyEndTime - latencyStartTime)),  LatencyUpdateListener.class);
                    }
                    break;
                default:
                    fireEvent(new NetworkEvent(this, msg),  UnknownMessageTypeReceivedListener.class);
                    //throw new UnsupportedOperationException("Message type has not been catered for. Please include handling code for it!");
                    break;

            }
        }
        else if(message instanceof PlayerRegistrationMessage)
        {
            PlayerRegistrationMessage regMessage = (PlayerRegistrationMessage)message;
            playerID = ServerVariables.playerNetworkAddressList.size();
            ServerVariables.playerNetworkAddressList.add(socket.getInetAddress());            
            playerName = regMessage.playerName;
            PlayerRegistrationMessage reply = new PlayerRegistrationMessage(playerID);
            writeOut(reply);
            registerPlayer(regMessage);

            
            sendInitialState();
            sendPeerList();
            fireEvent(new NetworkEvent(this, "Connection successfully established"),  ConnectionEstablishedListener.class);
        }
        else
        {
            System.err.println("Unrecognised object received from client: "+message.getClass());
        }
    }

    /*
     * Writes a given object to the outputstream
     */
    private void writeOut(NetworkMessage object) throws BufferOverflowException
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
            out.shutdownThread();
            stopOperation = true;
            this.interrupt();
            socket.close();
        }
        catch(IOException e)
        {
            //We dont really care if the socket failed to close correctly.
            System.err.println("Socket failed to close correctly. \n"+e);
        }
        
        
    }

    /*************************************** EVENTS HANDELING***************************/

    public <T extends NetworkEventListener> void addNetworkListener(Class<T> t, T listener)
    {
        listeners.add(t, listener);
    }
    
    public <T extends NetworkEventListener> void removeNetworkListener(Class<T> t, T listener)
    {
        listeners.remove(t, listener);
    }

    /* Convenience Listener methods below for those that dont know how to use the above
     * Generic methods.
     */


    public void addConnectionEstablishedListener(ConnectionEstablishedListener listener)
    {
        listeners.add(ConnectionEstablishedListener.class, listener);
    }

    public void removeConnectionEstablishedListener(ConnectionEstablishedListener listener)
    {
        listeners.remove(ConnectionEstablishedListener.class, listener);
    }

    public void addConnectionLostListener(ConnectionLostListener listener)
    {
        listeners.add(ConnectionLostListener.class, listener);
    }

    public void removeConnectionLostListener(ConnectionLostListener listener)
    {
        listeners.remove(ConnectionLostListener.class, listener);
    }

    public void addUpdateReceivedListener(UpdateReceivedListener listener)
    {
        listeners.add(UpdateReceivedListener.class, listener);
    }

    public void removeUpdateReceivedListener(UpdateReceivedListener listener)
    {
        listeners.remove(UpdateReceivedListener.class, listener);
    }

    public void addRequestReceivedListener(RequestReceivedListener listener)
    {
        listeners.add(RequestReceivedListener.class, listener);
    }

    public void removeRequestReceivedListener(RequestReceivedListener listener)
    {
        listeners.remove(RequestReceivedListener.class, listener);
    }

    public void addGameStateRequestReceivedListener(GameStateRequestReceivedListener listener)
    {
        listeners.add(GameStateRequestReceivedListener.class, listener);
    }

    public void removeGameStateRequestReceivedListener(GameStateRequestReceivedListener listener)
    {
        listeners.remove(GameStateRequestReceivedListener.class, listener);
    }

    public void addTerminationRequestReceivedListener(TerminationRequestReceivedListener listener)
    {
        listeners.add(TerminationRequestReceivedListener.class, listener);
    }

    public void removeTerminationRequestReceivedListener(TerminationRequestReceivedListener listener)
    {
        listeners.remove(TerminationRequestReceivedListener.class, listener);
    }

    public void addUnknownMessageTypeReceivedListener(UnknownMessageTypeReceivedListener listener)
    {
        listeners.add(UnknownMessageTypeReceivedListener.class, listener);
    }

    public void removeUnknownMessageTypeReceivedListener(UnknownMessageTypeReceivedListener listener)
    {
        listeners.remove(UnknownMessageTypeReceivedListener.class, listener);
    }

    /**
     * Used to fire off a network event in a generalised manner. Takes the event to
     * be fired (all events are expected to be children of NetworkEvent) and the
     * class of the listener to be notified of the event.
     * @param <T> The type of listener that we wish to fire events on.
     * @param event The event we would like to propagate to the event listeners
     * @param t The class type of the listerns we wish to fire events on.
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
                 * This might seem a little confusing at first, so here's an explaination.
                 * The listener list stores event listeners in pairs of
                 * {listener.class, instance of listener}. This means that
                 * i will be the class of the listener, which we compare with our
                 * given class type T. If it matches we cast the instance of the
                 * listner as our given class type T (which we have checked, it actually
                 * is) and then run its EventOccured method.
                 */
                t.cast(listenerArray[i+1]).EventOccured(event);
            }
        }
    }

}
