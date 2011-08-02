package networkserver.Threads;

import java.awt.AWTEvent;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.BufferOverflowException;
import java.util.Vector;
import javax.swing.event.EventListenerList;
import networkserver.DataContainers.NetworkMessage;
import networkserver.DataContainers.PlayerRegistrationMessage;
import networkserver.EventListeners.*;
import networkserver.Events.NetworkEvent;
import networkserver.Peer2Peer.ClientPeer;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public abstract class ServerDaemonThread extends Thread{

    private Socket socket;
    private ServerDaemonWriteoutThread out;
    private ObjectInputStream in;
    private boolean stopOperation = false;

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
        in = new ObjectInputStream(socket.getInputStream());
        out = new ServerDaemonWriteoutThread(acceptedSocket);
    }

    /*
     * Called once joining information for the player has been received. Use
     * this to add player information to the game engine/world.
     */
    public abstract void registerPlayer(String playerName, int playerID, InetAddress clientAddress);

    /*
     * Called after RegisterPlayer, meant to send game state initialization
     * information to the client.
     */
    public abstract void sendInitialState();

    /*
     * Method which must return a list of peers that the client can establish
     * peer to peer connections with (or an empty list to disable P2P connections)
     */
    public abstract Vector<ClientPeer> getPeerList();

    private void sendInitialPeerList()
    {
        Vector<ClientPeer> peers = getPeerList();
        //Now send these to the client somehow
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
                Object data = in.readObject();                
                processNetworkMessage(data);
            }
            catch(InterruptedIOException e)
            {
                //We expect that something wants the threads attention. This is
                //used to immediatly end the thread in shutdownThread().
            }
            catch(IOException e)
            {
                System.err.println("Error occured while reading from thread : "+e);
                fireEvent(new NetworkEvent(this, AWTEvent.RESERVED_ID_MAX + 1, "Connection to client lost!\n" + e),  ConnectionLostListener.class);
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
                    fireEvent(new NetworkEvent(this, AWTEvent.RESERVED_ID_MAX + 1, message),  UpdateReceivedListener.class);
                    break;
                case REQUEST_MESSAGE:
                    fireEvent(new NetworkEvent(this, AWTEvent.RESERVED_ID_MAX + 1, message),  RequestReceivedListener.class);
                    break;
                case PARTIAL_GAMESTATE_UPDATE_MESSAGE:
                    //Not used on server side. This flag is used to notify the client of incoming partial game state
                    break;
                case GAMESTATE_UPDATE_MESSAGE:
                    //Similarly, this is used exclusivly on the client side.
                    break;
                case GAMESTATE_REQUEST_MESSAGE:
                    fireEvent(new NetworkEvent(this, AWTEvent.RESERVED_ID_MAX + 1, message),  GameStateRequestReceivedListener.class);
                    break;
                case TERMINATION_REQUEST_MESSAGE:
                    fireEvent(new NetworkEvent(this, AWTEvent.RESERVED_ID_MAX + 1, message),  TerminationRequestReceivedListener.class);
                    break;
                default:
                    throw new UnsupportedOperationException("Message type has not been catered for. Please include handling code for it!");

            }
        }
        else if(message instanceof PlayerRegistrationMessage)
        {
            PlayerRegistrationMessage regMessage = (PlayerRegistrationMessage)message;
            registerPlayer(regMessage.playerName, regMessage.playerID, socket.getInetAddress());
            sendInitialState();
            sendInitialPeerList();
            fireEvent(new NetworkEvent(this, AWTEvent.RESERVED_ID_MAX + 1, "Connection successfully established"),  ConnectionEstablishedListener.class);
        }
        else
        {
            System.err.println("Unrecognised object received from client: "+message.getClass());
        }
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
            out.shutdownThread();
            socket.close();
        }
        catch(IOException e)
        {
            //We dont really care if the socket failed to close correctly.
            System.err.println("Socket failed to close correctly. \n"+e);
        }
        stopOperation = true;
        this.interrupt();
        
    }

    /*************************************** EVENTS ***************************/
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

    /**
     * Used to fire off a network event in a generalised manner. Takes the event to
     * be fired (all events are expected to be children of NetworkEvent) and the
     * class of the listener to be notified of the event.
     * @param <T>
     * @param event
     * @param t
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
