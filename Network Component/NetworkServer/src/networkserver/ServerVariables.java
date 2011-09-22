/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import networkserver.Threads.ServerDaemonThread;

/**
 * @date 2011/08/11
 * @author Lawrence Webley
 */
public class ServerVariables
{
    //Used to store a mapping from player ids to their respective network addresses.
    public static ArrayList<InetAddress> playerNetworkAddressList = new ArrayList<InetAddress>();

    /**
     * ArrayList of threads servering various players. Some items in this list might be dead (disconnected players).
     * The position in the ArrayList corresponds to the players ID.
     */
    public static ArrayList<ServerDaemonThread> playerThreads = new ArrayList<ServerDaemonThread>();

    /**
     * A hashmap that links the server threads to players via the playerIDs
     */
    public static HashMap<Integer, ServerDaemonThread> playerThreadMap = new HashMap<Integer, ServerDaemonThread>();
}
