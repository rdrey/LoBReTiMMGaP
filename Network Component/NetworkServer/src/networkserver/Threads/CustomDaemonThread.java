/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Threads;

import java.util.Vector;
import networkserver.Peer2Peer.ClientPeer;

/**
 * @date 2011/08/11
 * @author Lawrence Webley
 */
public class CustomDaemonThread extends ServerDaemonThread
{
    public void registerPlayer(String playerName, int playerId)
    {
        
    }

    public void sendInitialState()
    {

    }

    protected Vector<ClientPeer> getPeerList()
    {
        return null;
    }

}
