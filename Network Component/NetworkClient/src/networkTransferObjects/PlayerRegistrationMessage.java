
package networkTransferObjects;

import java.util.ArrayList;
import java.util.List;

import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

/**
 * A once off message sent to the server which should contain any initialisation information
 * required to create an instance of the player in the servers game world. This should at least
 * include a player name, but could also include data such as starting locations, experience,
 * initial client states, etc.
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class PlayerRegistrationMessage extends NetworkMessage{
  
	public String playerName;   
	public int playerID;    
    public List<String> strings;    
    
    public PlayerRegistrationMessage(String playerName)
    {
    	super("playerRegistration");
    	this.playerName = playerName;
    	strings = new ArrayList<String>();    		
    }
    
    public PlayerRegistrationMessage(int playerId)
    {
        super("playerRegistration");
        playerID = playerId;
        strings = new ArrayList<String>();    	
    }
    
    public PlayerRegistrationMessage()
    {
    	super("playerRegistration");
    }
    
    /**
     * Gets the runtime schema of this class for serialization.
     * If you inherit from this class, you MUST OVERRIDE this method, 
     * otherwise it will be serialized as its parent, and you will lose data.
     * 
     * Additionally you will need to add a case for it in the network read and write 
     * methods, so that the receiving end knows what type of class to deserialize it as.
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
	public Schema getSchema()
    {
    	return RuntimeSchema.getSchema(PlayerRegistrationMessage.class);
    }
   }
