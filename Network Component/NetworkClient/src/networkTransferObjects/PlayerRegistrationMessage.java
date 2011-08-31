
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
    

    public PlayerRegistrationMessage(String playerName)
    {
    	super("playerRegistration");
    	this.playerName = playerName;
        playerID = -1;    
    }

    public PlayerRegistrationMessage(int playerId)
    {
        super("playerRegistration");
        playerID = playerId;
        playerName = "Player";    
    }

    public PlayerRegistrationMessage()
    {
    	super("playerRegistration");
        playerID = -1;
        playerName = "Player";    
    }
}
