/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import networkserver.ServerVariables;

/**
 * @date 2011/09/16
 * @author Lawrence Webley
 */
public class LokemonPlayer {
    private int playerID;
    private String playerName;
    private int avatar;

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



}
