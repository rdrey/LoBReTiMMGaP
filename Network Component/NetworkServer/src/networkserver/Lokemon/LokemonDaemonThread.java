/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import java.util.Vector;
import networkTransferObjects.NetworkMessage;
import networkTransferObjects.PlayerRegistrationMessage;
import networkserver.Peer2Peer.ClientPeer;
import networkserver.Threads.ServerDaemonThread;

/**
 * @date 2011/09/12
 * @author Lawrence Webley
 */
public class LokemonDaemonThread extends ServerDaemonThread{

    @Override
    protected void registerPlayer(PlayerRegistrationMessage initialMessage) {
        System.out.println("Player Registerd: ID:"+initialMessage.playerID+", name: "+initialMessage.playerName);
    }

    @Override
    protected NetworkMessage getInitialState() {
        return new NetworkMessage("Initial state information not available.");
    }

    @Override
    protected Vector<ClientPeer> getPeerList(int playerId, String playerName) {
        return new Vector<ClientPeer>();
    }

}
