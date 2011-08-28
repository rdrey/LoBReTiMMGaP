/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * @date 2011/08/11
 * @author Lawrence Webley
 */
public class ServerVariables
{
    //Used to store a mapping from player ids to their respective network addresses.
    public static ArrayList<InetAddress> playerNetworkAddressList = new ArrayList<InetAddress>();
}
