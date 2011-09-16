/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import java.awt.Rectangle;
import java.util.Vector;
import networkTransferObjects.NetworkMessage;
import networkTransferObjects.NetworkMessageMedium;
import networkTransferObjects.PlayerRegistrationMessage;
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
        this.addNetworkListener(RequestReceivedListener.class, rrList);
        this.addNetworkListener(UpdateReceivedListener.class, urList);
    }

    
    @Override
    protected void registerPlayer(PlayerRegistrationMessage initialMessage) {
        System.out.println("Player Registerd: ID:"+initialMessage.playerID+", name: "+initialMessage.playerName);
        player = new LokemonPlayer(initialMessage.playerID, initialMessage.playerName);
        LokemonSeverVariables.playerList.add(player);
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

    //Listener processing
    RequestReceivedListener rrList = new RequestReceivedListener() {

        public void EventOccured(NetworkEvent e) {
            NetworkMessage msg = (NetworkMessage)e.getMessage();
            String sMsg = msg.getMessage();

            if(sMsg.equals("MapDataRequest"));
            {
                double topLeftX = ((NetworkMessageMedium)msg).integers.get(0);
                double topLeftY = ((NetworkMessageMedium)msg).integers.get(1);
                double width = ((NetworkMessageMedium)msg).integers.get(2);
                double height = ((NetworkMessageMedium)msg).integers.get(3);
                LokemonServerLogic.sendMapDataToPlayer(playerID, topLeftX, topLeftY, width, height);
            }
        }
    };

    UpdateReceivedListener urList = new UpdateReceivedListener() {

        public void EventOccured(NetworkEvent e) {
            NetworkMessage msg = (NetworkMessage)e.getMessage();
            String sMsg = msg.getMessage();

            if(sMsg.equals("LocationUpdate"))
            {
                
            }
        }
    };

}
