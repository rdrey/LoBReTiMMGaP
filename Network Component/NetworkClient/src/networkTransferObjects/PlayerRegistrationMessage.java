
package networkTransferObjects;


import java.util.ArrayList;

/**
 * A once off message sent to the server which should contain any initialisation information
 * required to create an instance of the player in the servers game world. This should at least
 * include a player name, but could also include data such as starting locations, experience,
 * initial client states, etc.
 * 
 * Always add new fields to the TOP of the file. This will facilitate correct backward compatibility with serialisation.
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class PlayerRegistrationMessage extends NetworkMessage{

	public ArrayList<Integer> integers;
	public ArrayList<String> strings;
    public String playerName;
    public int playerID;
    


    public PlayerRegistrationMessage(String playerName)
    {
    	super("playerRegistration");
    	this.playerName = playerName;    	
        playerID = -1;        
        strings = new ArrayList<String>();
        integers = new ArrayList<Integer>();
    }

    public PlayerRegistrationMessage(int playerId)
    {
        super("playerRegistration");
        playerID = playerId;
        playerName = "Player";
        strings = new ArrayList<String>();
        integers = new ArrayList<Integer>();
    }

    public PlayerRegistrationMessage()
    {
    	super("playerRegistration");               
    }
}
