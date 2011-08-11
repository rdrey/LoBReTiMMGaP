/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Peer2Peer;

import java.net.InetAddress;
import networkserver.ServerVariables;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class ClientPeer {
    public String playerName;
    public int playerId;
    public InetAddress networkAddress;

    public ClientPeer(int playerId, String playerName)
    {
        this.playerId = playerId;
        this.playerName = playerName;
        networkAddress = getClientAddress();
    }

    /**
     * Returns the network address of the associated player, for P2P connections.
     * @return the network address of the player associated with this object
     */
    private InetAddress getClientAddress()
    {
        return ServerVariables.playerNetworkAddressList.get(new Integer(playerId));
    }
    
}
