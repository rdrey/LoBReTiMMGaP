/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import networkTransferObjects.Lokemon.LokemonPlayer;
import networkTransferObjects.NetworkMessageLarge;
import networkserver.ServerVariables;

/**
 * @date 2011/09/16
 * @author Lawrence Webley
 */
public class LokemonServerLogic {

    //Send the map data in the specified area to the player
    static void sendMapDataToClient(int playerId, Rectangle2D.Double area)
    {
        //Send XML map data in the region to the player.
    }

    /**
     * Send all the players, within the area of influence to the player specified
     * @param player The Lokemon player object representing the requesting player.
     */
    static void sendSurroundingPlayersToClient(LokemonPlayer player)
    {
        NetworkMessageLarge msg = new NetworkMessageLarge("Response:GetPlayers");
        ArrayList<LokemonPlayer> players = new ArrayList<LokemonPlayer>();

        if(player.getPosition()!= null)
        {
            for(LokemonPlayer pl : LokemonSeverVariables.playerList)
            {
                //We dont want to compare to ourselves
                if(pl.getPlayerID() != player.getPlayerID())
                {
                    //If this player is a valid, registered player in the networking component (just a consistency check)
                    if(ServerVariables.playerThreadMap.containsKey(pl.getPlayerID()))
                    {
                        //If the player has a valid location
                        if(pl.getPosition() != null)
                        {
                            if(pl.getPosition().getDistanceFrom(player.getPosition()) < LokemonSeverVariables.areaOfInterest)
                            {
                                players.add(pl);
                            }
                        }
                    }
                }
            }

            msg.objectDict.put("PlayerList", players);
            ServerVariables.playerThreadMap.get(player.getPlayerID()).sendGameStateUpdate(msg);
        }
        else
        {
            msg.setMessage("Error: Requested surrounding players, but your location is unknown!");
        }
    }

}
