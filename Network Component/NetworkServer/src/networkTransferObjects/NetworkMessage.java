
package networkTransferObjects;

import java.io.Serializable;
import java.util.HashMap;
import networkserver.ServerCustomisation;

/**
 * Used to pass information between the client and the server
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class NetworkMessage implements Serializable
{
    /**
     * Used to ensure that the server object and client object are treated as identical objects.
     */
    private static final long serialVersionUID = 4259455514140197693L;
        
    //Used internally for network message classification.
    public enum MessageType //Comments show where the type can be received
    {
        UPDATE_MESSAGE, //Client and Server
        REQUEST_MESSAGE,//Client and Server
        INITIAL_GAME_STATE_MESSAGE, //Client only
        PARTIAL_GAMESTATE_UPDATE_MESSAGE, //Client only
        GAMESTATE_UPDATE_MESSAGE, //Client Only
        GAMESTATE_REQUEST_MESSAGE, //Server only
        TERMINATION_REQUEST_MESSAGE, //Server Only
        PEER_LIST_MESSAGE, //Client only
        PEER_LIST_REQUEST_MESSAGE, //Server Only
        LATENCY_REQUEST_MESSAGE, //Server & Client.
        LATENCY_RESPONSE_MESSAGE //Server & Client

    }

    private HashMap<String, String> strings;
    private HashMap<String, Integer> ints;
    private HashMap<String, Object> objects;
    private MessageType messageType;

    private String primeMessage;

    public NetworkMessage(String message)
    {
        primeMessage = message;
        strings = new HashMap<String, String>(ServerCustomisation.initialNetworkMessageMapSize);
        ints = new HashMap<String, Integer>(ServerCustomisation.initialNetworkMessageMapSize);
        objects = new HashMap<String, Object>(ServerCustomisation.initialNetworkMessageMapSize);
    }
   
    public void addDataString(String key, String value)
    {
        strings.put(key, value);
    }

    public void addDataInt(String key, int value)
    {
        ints.put(key, new Integer(value));
    }

    /*
     * Adds an object to this network message. The object must implement serializable
     */
    public void addDataObject(String key, Object value) throws IllegalArgumentException
    {
        if(value instanceof java.io.Serializable)
        {
            objects.put(key, value);
        }
        else
        {
            throw new IllegalArgumentException("Object is not serializable!");
        }
    }

    public String getDataString(String key)
    {
        return strings.get(key);
    }

    public int getDataInt(String key)
    {
        return ints.get(key).intValue();
    }

    public Object getDataObject(String key)
    {
        return objects.get(key);
    }

    public String getMessage()
    {
        return primeMessage;
    }

    //Used internally for network message classification. Don't use this.
    public void setMessageType(MessageType mType)
    {
        messageType = mType;
    }

    public MessageType getMessageType()
    {
        return messageType;
    }
}
