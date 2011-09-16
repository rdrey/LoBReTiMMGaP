/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

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
     * @param playerId The id of the player to search around.
     */
    static void sendSurroundingPlayersToClient(int playerId)
    {
        
    }

}
