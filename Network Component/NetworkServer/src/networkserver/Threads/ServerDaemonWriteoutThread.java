/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Threads;

import java.net.Socket;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class ServerDaemonWriteoutThread extends Thread
{
    Socket socket;
    public ServerDaemonWriteoutThread(Socket writeOutSocket)
    {
        socket = writeOutSocket;
    }
}
