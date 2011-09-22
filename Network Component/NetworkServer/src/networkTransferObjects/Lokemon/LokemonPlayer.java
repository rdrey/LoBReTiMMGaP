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
    private int playerID;
    private String playerName;
    private int avatar;

    private Location position;
    private boolean busy = false;
    
    public Location getPosition() {
        return position;
    }    

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public boolean isBusy()
    {
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

    public LokemonPlayer(int playerID, String playerName)
    {
        this.playerID = playerID;
        this.playerName = playerName;
    }    
    

    public void setPosition(Location loc)
    {
        position = loc;
    }

}
