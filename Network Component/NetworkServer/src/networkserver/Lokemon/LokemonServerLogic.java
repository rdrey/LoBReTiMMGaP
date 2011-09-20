/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import networkTransferObjects.Lokemon.LokemonPlayer;
import networkTransferObjects.Lokemon.LokemonPotion;
import networkTransferObjects.NetworkMessageLarge;
import networkTransferObjects.UtilityObjects.Location;
import networkserver.ServerVariables;

/**
 * @date 2011/09/16
 * @author Lawrence Webley
 */
public class LokemonServerLogic extends Thread{

    private boolean stopOperation = false;

    //Send the map data in the specified area to the player
    static void sendMapDataToClient(int playerId, Rectangle2D.Double area)
    {
        //Send XML map data in the region to the player.
    }

    /**
     * Send all the players, within the area of influence to the player specified
     * @param player The Lokemon player object representing the requesting player.
     */
    static void sendPlayersToClient(LokemonPlayer player)
    {
        NetworkMessageLarge msg = new NetworkMessageLarge("Response:GetPlayers");
        ArrayList<LokemonPlayer> players = new ArrayList<LokemonPlayer>();

        if(player.getPosition()!= null)
        {
            for(LokemonPlayer pl : LokemonServerVariables.playerList)
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
                            if(pl.getPosition().getDistanceFrom(player.getPosition()) < LokemonServerVariables.areaOfInterest)
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
            ServerVariables.playerThreadMap.get(player.getPlayerID()).sendGameStateUpdate(msg);
        }
    }

    static void sendGameObjectsToClient(LokemonPlayer player)
    {
        NetworkMessageLarge msg = new NetworkMessageLarge("Response:GetGameObjects");
        ArrayList<LokemonPotion> pots = new ArrayList<LokemonPotion>();

        if(player.getPosition()!= null)
        {
            for(LokemonPotion pot : LokemonServerVariables.itemList)
            {
                //If the player has a valid location
                if(pot.getPosition() != null)
                {
                    if(pot.getPosition().getDistanceFrom(player.getPosition()) < LokemonServerVariables.areaOfInterest)
                    {
                        pots.add(pot);
                    }
                }
            }

            msg.objectDict.put("ItemList", pots);
            ServerVariables.playerThreadMap.get(player.getPlayerID()).sendGameStateUpdate(msg);
        }
        else
        {
            msg.setMessage("Error: Requested surrounding items, but your location is unknown!");
            ServerVariables.playerThreadMap.get(player.getPlayerID()).sendGameStateUpdate(msg);
        }
    }

    //Main server logic includes generating interesting objects to pick up.
    @Override
    public void run()
    {
        while(!stopOperation)
        {
            try{
                //If we actually have some players ingame
                if(LokemonServerVariables.playerList.size() >= 1)
                {
                    //Now we would like to spawn a random item around each one of the players.
                    for(LokemonPlayer pl : LokemonServerVariables.playerList)
                    {
                        //Make sure the player has a set location
                        if(pl.getPosition() != null)
                        {
                            Location center = pl.getPosition();
                            //Creates a random distance between itemSpawnRangeMin and itemSpawnRangeMax
                            double randomDist = (Math.random()
                                    * (LokemonServerVariables.itemSpawnRangeMax - LokemonServerVariables.itemSpawnRangeMin))
                                    + LokemonServerVariables.itemSpawnRangeMin;
                            //Create a random angle away from the play at which distance the point of the item will be.
                            double randomAngle = Math.random() * 360;

                            //Now determine a point randomDist away from center, at angle randomAngle.
                            //Formula (x?,y?)=(x+dcosα,y+dsinα)
                            double x = center.getX() + randomDist*Math.cos(randomAngle);
                            double y = center.getY() + randomDist*Math.sin(randomAngle);

                            LokemonPotion pot = new LokemonPotion(
                                    LokemonPotion.PotionType.values()[((int)Math.random()*LokemonPotion.PotionType.values().length)]);
                            pot.setPosition(new Location(x, y));
                            LokemonServerVariables.itemList.add(pot);
                        }
                    }
                }

                Thread.sleep(LokemonServerVariables.itemSpawnTimer);
            }
            catch(InterruptedException e)
            {
                //Expected on shutdown.
            }
            catch(Exception e)
            {
                System.err.println("Unexpected error occuring in server logic! (Ignoring and continuing), Error:\n"+e);
            }
        }
    }

    public void stopOperation()
    {
        stopOperation = true;
        this.interrupt();
    }    


}
