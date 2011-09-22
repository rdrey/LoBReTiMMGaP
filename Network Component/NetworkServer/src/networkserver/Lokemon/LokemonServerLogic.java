/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import com.vividsolutions.jts.geom.Coordinate;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import networkTransferObjects.Lokemon.LokemonPlayer;
import networkTransferObjects.Lokemon.LokemonPotion;
import networkTransferObjects.Lokemon.LokemonSpatialObject;
import networkTransferObjects.NetworkMessage;
import networkTransferObjects.NetworkMessageLarge;
import networkTransferObjects.UtilityObjects.Location;
import networkserver.ServerVariables;
import org.mobiloc.lobgasp.App;
import org.mobiloc.lobgasp.SpatialProvider;
import org.mobiloc.lobgasp.model.SpatialDBEntity;
import org.mobiloc.lobgasp.osm.model.Ways.*;

/**
 * @date 2011/09/16
 * @author Lawrence Webley
 */
public class LokemonServerLogic extends Thread{

    private boolean stopOperation = false;
    private int itemIDCounter = 0;

    private long itemSpawnTime = 0;

    private static ArrayList<ItemPickupRequest> requestAccumulationBuffer = new ArrayList<ItemPickupRequest>();

    //Send the map data in the specified area to the player
    static void sendMapDataToClient(int playerId, double lat, double lng, double radius)
    {
        //Send XML map data in the region to the player.
        SpatialProvider sp = new SpatialProvider();
        List<SpatialDBEntity> entities = sp.provide(new Coordinate(lat, lng),radius);
        
        ArrayList<LokemonSpatialObject> gameObjects = new ArrayList<LokemonSpatialObject>();
        
        for(SpatialDBEntity ent : entities)
        {
            LokemonSpatialObject so = null;
            if(ent instanceof FieldEntity)
            {
                so = new LokemonSpatialObject(ent.getId(), LokemonSpatialObject.SpatialObjectType.GRASSLAND);
            }
            else if(ent instanceof ForestEntity)
            {
                so = new LokemonSpatialObject(ent.getId(), LokemonSpatialObject.SpatialObjectType.FOREST);
            }
            else if(ent instanceof NatureReserveEntity)
            {
                so = new LokemonSpatialObject(ent.getId(), LokemonSpatialObject.SpatialObjectType.MOUNTAIN);
            }
            else if(ent instanceof ParkingEntity)
            {
                so = new LokemonSpatialObject(ent.getId(), LokemonSpatialObject.SpatialObjectType.URBAN);
            }
            else if(ent instanceof ReservoirEntity)
            {
                so = new LokemonSpatialObject(ent.getId(), LokemonSpatialObject.SpatialObjectType.WATER);
            }
            else if(ent instanceof StepsEntity)
            {
                so = new LokemonSpatialObject(ent.getId(), LokemonSpatialObject.SpatialObjectType.ROUGH);
            }
            else if(ent instanceof TunnelEntity)
            {
                so = new LokemonSpatialObject(ent.getId(), LokemonSpatialObject.SpatialObjectType.CAVE);
            }  
            
            if(so != null)
            {
                so.setGeomBytes(ent.getGeom());
                gameObjects.add(so);
            }
        }
        
        NetworkMessageLarge msg = new NetworkMessageLarge("MapDataResponse");
        msg.doubles.add(lat);
        msg.doubles.add(lng);
        msg.doubles.add(radius);        
        msg.objectDict.put("SpatialObjects", gameObjects);
        
        System.out.println("Server request for "+ gameObjects.size() + "spatial objects!");
        ServerVariables.playerThreadMap.get(playerId).sendGameStateUpdate(msg);
        
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
            for(LokemonPotion pot : LokemonServerVariables.itemMap.values())
            {
                //If the player has a valid location
                if(pot.getPosition() != null)
                {
                    if(App.distFrom(pot.getPosition().getX(), pot.getPosition().getY(),
                            player.getPosition().getX(), player.getPosition().getY()) < LokemonServerVariables.areaOfInterest)
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

    /***
     * Check if a given player can pick up the item. Upon receiving this request,
     * time stamps are checked to see how long ago it was sent. If it was recent,
     * a little time is waited to see if any additional requests for the item (from
     * different players) arrive. If they do, the request that has the earliest
     * time stamp is given the item (all time systems are synchronised to the server).
     * @param player
     */
    static void pickUpGameObject(LokemonPlayer player, int itemID, long timeStamp)
    {
        //If the item exists
        if(LokemonServerVariables.itemMap.containsKey(itemID))
        {
            /* First check to see how long ago this message was sent. If it was > pickupLatency,
             * we have no need to wait, so instantly reply with success.
             */
             System.out.println("Message sent: "+ (System.currentTimeMillis() - timeStamp) +"ms ago!"); //DEBUG
             if(System.currentTimeMillis() - timeStamp > LokemonServerVariables.pickupLatency)
             {
                 NetworkMessage msg = new NetworkMessage("Accept");
                 ServerVariables.playerThreadMap.get(player.getPlayerID()).sendRequest(msg);

                 //And remove the item
                 LokemonServerVariables.itemMap.remove(itemID);
             }
             else//Complicated case. Now we need to wait (without halting the calling thread)
                 //for additional requests for pickupLatency - (currentTime-timestamp)
             {
                LokemonServerLogic.requestAccumulationBuffer.add(new ItemPickupRequest(player.getPlayerID(), itemID, timeStamp));
                //Now we leave the run method to deal with the problem :-)
             }
        }
        else //item doesnt exist, so reply reject.
        {
            NetworkMessage msg = new NetworkMessage("Reject");
            ServerVariables.playerThreadMap.get(player.getPlayerID()).sendRequest(msg);
        }

    }

    //Main server logic includes generating interesting objects to pick up.
    @Override
    public void run()
    {
        while(!stopOperation)
        {
            try{
                /***************************** Pickup request handling ************************/

                if(requestAccumulationBuffer.size() != 0)
                {
                    for(ItemPickupRequest req : requestAccumulationBuffer)
                    {
                        if(req.shouldBeRemoved == false)
                        {
                            //If the item still exists,
                            if(LokemonServerVariables.itemMap.containsKey(req.itemId))
                            {
                                //If we have waited for another request to arrive for enough time
                                if(System.currentTimeMillis() - req.requestTimeStamp > LokemonServerVariables.pickupLatency)
                                {
                                    //Alright, so now we know we have waited long enough. But we dont know that
                                    //there isn't another request in the accumulation buffer that is very close
                                    //in time to this request, for the same item. So we need to look for one.
                                    boolean earliestRequest = true;
                                    for(int i = 0; i < requestAccumulationBuffer.size();i++)
                                    {
                                        if(requestAccumulationBuffer.get(i) != req)
                                        {
                                            if(requestAccumulationBuffer.get(i).itemId == req.itemId)
                                            {//Ok, so now we have another request for the same item
                                                if(req.requestTimeStamp > requestAccumulationBuffer.get(i).requestTimeStamp)
                                                {
                                                    /* Now we have determined that there is another request
                                                     * with a earlier timestamp than us, for the same item.
                                                     * There may be more, but at this stage we can give up,
                                                     * because in their turn in this iteration their wont be, and the item will
                                                     * be deleted.
                                                     */
                                                    earliestRequest = false;
                                                     break;
                                                }
                                            }
                                        }
                                    }

                                    //If we managed to go through the entire list of requests
                                    //without finding an earlier request, then send acceptance
                                    if(earliestRequest)
                                    {
                                        req.sendAccept();
                                        //and remove the item
                                        LokemonServerVariables.itemMap.remove(req.itemId);
                                    }
                                }
                            }
                            else
                            {
                                //Send reject
                                req.sendReject();
                            }
                        }
                    }
                }

                //Go through the list in reverse, removing elements marked for deletion
                for(int i = requestAccumulationBuffer.size() - 1; i >= 0 ; i --)
                {
                    if(requestAccumulationBuffer.get(i).shouldBeRemoved)
                    {
                        requestAccumulationBuffer.remove(i);
                    }
                }

                /***************************** Item Generation Code ***************************/
                if(System.currentTimeMillis() - itemSpawnTime > LokemonServerVariables.itemSpawnTimer)
                {
                    itemSpawnTime = System.currentTimeMillis();
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
                                        LokemonPotion.PotionType.values()[((int)Math.random()*LokemonPotion.PotionType.values().length)],
                                        itemIDCounter);
                                pot.setPosition(new Location(x, y));
                                itemIDCounter++;
                                LokemonServerVariables.itemMap.put(pot.getId(), pot);
                            }
                        }
                    }
                }
                //Loop the thread every 10ms.
                Thread.sleep(10);
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

    private static class ItemPickupRequest
    {
        public int playerId;
        public long requestTimeStamp;
        public int itemId;

        public boolean shouldBeRemoved = false;

        public ItemPickupRequest(int playerId, int itemId, long timeStamp)
        {
            this.playerId = playerId;
            this.itemId = itemId;
            this.requestTimeStamp = timeStamp;
        }

        public void sendAccept()
        {
            NetworkMessage msg = new NetworkMessage("Accept");
            ServerVariables.playerThreadMap.get(playerId).sendRequest(msg);
            shouldBeRemoved = true;
        }

        public void sendReject()
        {
            NetworkMessage msg = new NetworkMessage("Reject");
            ServerVariables.playerThreadMap.get(playerId).sendRequest(msg);
            shouldBeRemoved = true;
        }

    }


}
