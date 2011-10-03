/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import java.util.ArrayList;
import networkTransferObjects.Lokemon.LokemonPlayer;
import networkTransferObjects.NetworkMessage;
import networkTransferObjects.NetworkMessageMedium;
import networkTransferObjects.PlayerRegistrationMessage;
import networkTransferObjects.UtilityObjects.Location;
import networkserver.EventListeners.ConnectionLostListener;
import networkserver.EventListeners.GameStateRequestReceivedListener;
import networkserver.EventListeners.RequestReceivedListener;
import networkserver.EventListeners.TerminationRequestReceivedListener;
import networkserver.EventListeners.UpdateReceivedListener;
import networkserver.Events.NetworkEvent;
import networkserver.LogMaker;
import networkserver.Peer2Peer.ClientPeer;
import networkserver.ServerVariables;
import networkserver.Threads.ServerDaemonThread;

/**
 * @date 2011/09/12
 * @author Lawrence Webley
 */
public class LokemonDaemonThread extends ServerDaemonThread{

    LokemonPlayer player;
    
    public static final double DEGREE_METER_HACK = 111111;
    
    public ArrayList<Integer> interestedParties = new ArrayList<Integer>();

    public LokemonDaemonThread()
    {
        super();

        //Register listeners
        this.addNetworkListener(RequestReceivedListener.class, rrListen);
        this.addNetworkListener(UpdateReceivedListener.class, urListen);
        this.addNetworkListener(ConnectionLostListener.class, clListen);
        this.addNetworkListener(GameStateRequestReceivedListener.class, gsrrListen);
        this.addNetworkListener(TerminationRequestReceivedListener.class, trrListen);
    }

    
    @Override
    protected void registerPlayer(PlayerRegistrationMessage initialMessage) {
        LogMaker.println("Player Registerd: ID:"+initialMessage.playerID+", name: "+initialMessage.playerName, playerID);
        player = new LokemonPlayer(initialMessage.playerID, initialMessage.playerName);
        LokemonServerVariables.playerList.add(player);
        player.setAvatar(initialMessage.integers.get(0));
    }

    @Override
    protected NetworkMessage getInitialState() {
        return new NetworkMessage("Initial state information not available.");
    }

    @Override
    protected ArrayList<ClientPeer> getPeerList(int playerId, String playerName) {
        return new ArrayList<ClientPeer>();
    }

    //============================================Listener processing==========================================================
    
    RequestReceivedListener rrListen = new RequestReceivedListener() {

        public void EventOccured(NetworkEvent e) {
            NetworkMessage msg = (NetworkMessage)e.getMessage();
            String sMsg = msg.getMessage();

            if(sMsg.equals("MapDataRequest"))
            {
                double lat = ((NetworkMessageMedium)msg).doubles.get(0);
                double lng = ((NetworkMessageMedium)msg).doubles.get(1);
                double radius = ((NetworkMessageMedium)msg).doubles.get(2);
                
                //Convert radius from meters to degrees.
                radius /= DEGREE_METER_HACK;              

                LokemonServerLogic.sendMapDataToClient(playerID, lat, lng, radius);
            }
            else if(sMsg.equals("ItemPickupRequest"))
            {
                int itemId = ((NetworkMessageMedium)msg).integers.get(0);
                
                LokemonServerLogic.pickUpGameObject(player, itemId, msg.getTimeStamp());
            }
        }
    };

    UpdateReceivedListener urListen = new UpdateReceivedListener() {

        public void EventOccured(NetworkEvent e) {
            NetworkMessage msg = (NetworkMessage)e.getMessage();
            String sMsg = msg.getMessage();
            if(sMsg.equals("LocationUpdate"))
            {
                double x = ((NetworkMessageMedium)msg).doubles.get(0);
                double y = ((NetworkMessageMedium)msg).doubles.get(1);
                player.setPosition(new Location(x, y));
                
                //LogMaker.println("Position update: Latitude = "+x+", Longitude = "+y);
            }
            else if (sMsg.equals("EnteredBattle"))
            {
                player.setBusy(true);
            }
            else if (sMsg.equals("ExitedBattle"))
            {
                player.setBusy(false);
            }
        }
    };

    GameStateRequestReceivedListener gsrrListen = new GameStateRequestReceivedListener() {

        public void EventOccured(NetworkEvent e) {
            NetworkMessage msg = (NetworkMessage)e.getMessage();
            String sMsg = msg.getMessage();
            
            
            if(sMsg.equals("GetPlayers"))
            {
                LokemonServerLogic.sendPlayersToClient(player);
            }
            else if(sMsg.equals("GetGameObjects"))
            {
                LokemonServerLogic.sendGameObjectsToClient(player);
            }
        }
    };

    ConnectionLostListener clListen = new ConnectionLostListener() {

        public void EventOccured(NetworkEvent e) {
            //Lost connection to client, so remove them from our game states
            LokemonServerVariables.playerList.remove(player);            
            for(Integer pl : interestedParties)
            {
                //If the interested party still exists
                if(ServerVariables.playerThreadMap.containsKey(pl.intValue()))
                {
                    NetworkMessageMedium failMsg = new NetworkMessageMedium("NOTIFICATION:PlayerDisconnected");                    
                    
                    failMsg.integers.add(pl.intValue());
                    failMsg.integers.add(playerID);
                    
                    ServerVariables.playerThreadMap.get(pl.intValue()).forwardDirectCommunication(failMsg);
                }
            }
        }
    };

    TerminationRequestReceivedListener trrListen = new TerminationRequestReceivedListener() {

        public void EventOccured(NetworkEvent e) {
            LogMaker.println("Termination requested: "+ ((NetworkMessage)e.getMessage()).getMessage(), playerID);
            ServerVariables.playerThreadMap.get(player.getPlayerID()).shutdownThread();
            LokemonServerVariables.playerList.remove(player);            
        }
    };

}
