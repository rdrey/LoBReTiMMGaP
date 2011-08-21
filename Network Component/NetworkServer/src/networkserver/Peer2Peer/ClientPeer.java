/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Peer2Peer;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class ClientPeer implements Serializable {
    /**
     *  Makes sure that this object has the same serial version as the remote one.
     */
    private static final long serialVersionUID = -4363673355596267473L;
    public String playerName;
    public int playerId;
    public InetAddress networkAddress;

    public ClientPeer(int playerId, String playerName)
    {
        this.playerId = playerId;
        this.playerName = playerName;
    }
}
