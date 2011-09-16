/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import networkTransferObjects.Lokemon.LokemonPlayer;
import java.awt.geom.Rectangle2D;
import java.util.Vector;
import networkTransferObjects.NetworkMessage;
import networkTransferObjects.NetworkMessageMedium;
import networkTransferObjects.PlayerRegistrationMessage;
import networkTransferObjects.UtilityObjects.Location;
import networkserver.EventListeners.ConnectionLostListener;
import networkserver.EventListeners.GameStateRequestReceivedListener;
import networkserver.EventListeners.RequestReceivedListener;
import networkserver.EventListeners.UpdateReceivedListener;
import networkserver.Events.NetworkEvent;
import networkserver.Peer2Peer.ClientPeer;
import networkserver.Threads.ServerDaemonThread;

/**
 * @date 2011/09/12
 * @author Lawrence Webley
 */
public class LokemonDaemonThread extends ServerDaemonThread{

    LokemonPlayer player;

    public LokemonDaemonThread()
    {
        super();

        //Register listeners
        this.addNetworkListener(RequestReceivedListener.class, rrListen);
        this.addNetworkListener(UpdateReceivedListener.class, urListen);
        this.addNetworkListener(ConnectionLostListener.class, clListen);
    }

    
    @Override
    protected void registerPlayer(PlayerRegistrationMessage initialMessage) {
        System.out.println("Player Registerd: ID:"+initialMessage.playerID+", name: "+initialMessage.playerName);
        player = new LokemonPlayer(initialMessage.playerID, initialMessage.playerName);
        LokemonServerVariables.playerList.add(player);
        player.setAvatar(initialMessage.integers.get(0));
    }

    @Override
    protected NetworkMessage getInitialState() {
        return new NetworkMessage("Initial state information not available.");
    }

    @Override
    protected Vector<ClientPeer> getPeerList(int playerId, String playerName) {
        return new Vector<ClientPeer>();
    }

    //============================================Listener processing==========================================================
    
    RequestReceivedListener rrListen = new RequestReceivedListener() {

        public void EventOccured(NetworkEvent e) {
            NetworkMessage msg = (NetworkMessage)e.getMessage();
            String sMsg = msg.getMessage();

            if(sMsg.equals("MapDataRequest"));
            {
                double topLeftX = ((NetworkMessageMedium)msg).doubles.get(0);
                double topLeftY = ((NetworkMessageMedium)msg).doubles.get(1);
                double width = ((NetworkMessageMedium)msg).doubles.get(2);
                double height = ((NetworkMessageMedium)msg).doubles.get(3);

                LokemonServerLogic.sendMapDataToClient(playerID, new Rectangle2D.Double(topLeftX, topLeftY, width, height));
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
            if(sMsg.equals("GetGameObjects"))
            {
                LokemonServerLogic.sendGameObjectsToClient(player);
            }
        }
    };

    ConnectionLostListener clListen = new ConnectionLostListener() {

        public void EventOccured(NetworkEvent e) {
            //Lost connection to client, so remove them from our game states
            LokemonServerVariables.playerList.remove(player);
        }
    };

}
