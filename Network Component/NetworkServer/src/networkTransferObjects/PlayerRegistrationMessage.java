
package networkTransferObjects;

import java.io.Serializable;
import java.util.HashMap;
import networkserver.ServerCustomisation;



/**
 * A once off message sent to the server which should contain any initialisation information
 * required to create an instance of the player in the servers game world. This should at least
 * include a player name and id, but could also include data such as starting locations, experience,
 * initial client states, etc.
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class PlayerRegistrationMessage implements Serializable{
    /**
	 * Used to ensure conformity across the network connection.
	 */
	private static final long serialVersionUID = 6520835168631802917L;
	public String playerName;
        public int playerID;

    private HashMap<String, String> strings;
    private HashMap<String, Integer> ints;
    private HashMap<String, Object> objects;

    public PlayerRegistrationMessage(String playerName)
    {    	
    	this.playerName = playerName;

    	strings = new HashMap<String, String>(ServerCustomisation.initialNetworkMessageMapSize);
        ints = new HashMap<String, Integer>(ServerCustomisation.initialNetworkMessageMapSize);
        objects = new HashMap<String, Object>(ServerCustomisation.initialNetworkMessageMapSize);
    }
    
    public PlayerRegistrationMessage(int playerId)
    {
        playerID = playerId;
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
}
