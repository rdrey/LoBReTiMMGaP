package networkserver.Threads;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.BufferOverflowException;
import java.util.Vector;
import networkserver.DataContainers.NetworkMessage;
import networkserver.DataContainers.PlayerRegistrationMessage;
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
    public abstract void registerPlayer(String playerName, int playerID);

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
                    break;
                case REQUEST_MESSAGE:
                    break;
                case PARTIAL_GAMESTATE_UPDATE_MESSAGE:
                    break;
                case GAMESTATE_UPDATE_MESSAGE:
                    break;

            }
        }
        else if(message instanceof PlayerRegistrationMessage)
        {
            
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

}
