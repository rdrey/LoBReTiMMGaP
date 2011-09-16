/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import java.awt.Point;
import java.awt.geom.Point2D;
import networkserver.ServerVariables;

/**
 * @date 2011/09/16
 * @author Lawrence Webley
 */
public class LokemonPlayer {
    private int playerID;
    private String playerName;
    private int avatar;

    private Point2D.Double position;
    private boolean busy = false;

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
    
    /***
     * Retrieves the networkThread associated with this player.
     * @return the LokemonDaemonThread associated with this player.
     */
    public LokemonDaemonThread getPlayerThread()
    {
        return (LokemonDaemonThread)ServerVariables.playerThreadMap.get(playerID);
    }

    public void setPosition(Point2D.Double location)
    {
        position = location;
    }

}
