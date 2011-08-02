package networkserver.Threads;

import java.net.Socket;
import java.util.Vector;
import networkserver.DataContainers.NetworkMessage;
import networkserver.Peer2Peer.ClientPeer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public abstract class ServerDaemonThread extends Thread{

    private Socket socket;
    private ServerDaemonWriteoutThread out;

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
    public void setSocket(Socket acceptedSocket)
    {
        socket = acceptedSocket;
        out = new ServerDaemonWriteoutThread(acceptedSocket);
    }

    /*
     * Called once joining information for the player has been received.
     */
    public abstract void RegisterPlayer(String playerName, int playerID);

    /*
     * Called after RegisterPlayer, meant to send game state initialization
     * information to the client.
     */
    public abstract void SendInitialState();

    /*
     * Method which must return a list of peers that the client can establish
     * peer to peer connections with (or an empty list to disable P2P connections)
     */
    public abstract Vector<ClientPeer> GetPeerList();


    /*
     * Sends an update message to the client. Can be anything the implementer
     * chooses. Used for general communication with the client.
     */
    public void SendGameUpdate(NetworkMessage message)
    {
        throw new NotImplementedException();
    }

    /*
     * This method should be used to request information from the client. The message
     * sent to the client should generally be one that requires a response, such as
     * what your highest offline score was
     */
    public void SendRequest(NetworkMessage message)
    {
        throw new NotImplementedException();
    }

    /*
     * Used to send custom game state information to the client. The contents of
     * this send are up to the implmenter, but the corresponding event will be
     * fired on the client side. The contents of this message are assumed to be
     * larger than that of a standard Game update.
     */
    public void SendPartialGameStateUpdate(NetworkMessage message)
    {
        throw new NotImplementedException();
    }

    /*
     * Sends a full copy of the current game state to the client. What the game
     * state contains depends on the implementer.
     */
     public void SendGameStateUpdate(NetworkMessage message)
    {
        throw new NotImplementedException();
    }


    @Override
    public void run()
    {
        //Do running stuff        
        while(true)
        {
            /*
            try
            {
                String data = in.readUTF(in);
                processNetworkString(data);

            }
            catch(IOException e)
            {
                System.err.println("Error occured while reading from thread "+id+": "+e);
                try
                {
                    this.closeConnections();
                }
                catch(IOException exp)
                {
                    System.err.println("Failed to close data channals in thread! : "+ e);
                }
                break;
            }
             */
             
        }
    }

    /*
     * Writes a given object to the outputstream
     */
    private void writeOut(Object object)
    {
        
    }

}
