/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package networkTransferObjects.Lokemon;

import networkTransferObjects.UtilityObjects.Location;

/**
 * @date 2011/09/16
 * @author Lawrence Webley
 */
public class LokemonPlayer {

    private Location position;
    private String playerName;
    private int playerID;    
    private int avatar;    
    private boolean busy;
    
    public LokemonPlayer(int playerID, String playerName) {
        this.playerID = playerID;
        this.playerName = playerName;
        busy = false;
        avatar = 0;
        position = new Location();

    }

    public LokemonPlayer() {
        //default constructor for serialization
        playerID = -1;
        playerName = "";
        busy = false;
        avatar = 0;
        position = new Location();
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }
    public boolean getBusy() {
        return busy;
    }

    public int getPlayerID() {
        return playerID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getAvatar() {
        return avatar;
    }

    public void setAvatar(int avatar) {
        this.avatar = avatar;
    }

    public void setPosition(Location loc) {
        position = loc;
    }

       public Location getPosition() {
        return position;
    }    
}
