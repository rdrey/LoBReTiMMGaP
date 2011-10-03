package networkserver.Threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import networkserver.LogMaker;
import networkserver.ServerCustomisation;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 *
 *  This class is the starting point for the network server. It is designed around
 *  a thread, so that it might easily be integrated into existing game servers.
 */
public class ServerCoreThread  extends Thread {

    
    private ServerSocket socket;
    ServerCustomisation customSettings;

    public ServerCoreThread(ServerCustomisation customisation)
    {
        try
        {
            customSettings = customisation;
            socket = new ServerSocket(ServerCustomisation.port);
        }catch(IOException e)
        {
            LogMaker.errorPrintln("Error aquiring port: "+e, -1);
            System.exit(1);
        }
    }

    @Override
    public void run ()
    {
        //If the game engine is yet to be initialized, you should do it here.
        //(if it is to persist between all players)
        //GameEngine.start();

        while(true)
        {
            try
            {
                LogMaker.println("Waiting for a connection....", -1);
                Socket con = socket.accept();                
                LogMaker.println("Client accepted: "+ con, -1);                
                ServerDaemonThread daemonThread = customSettings.buildInstance();
                daemonThread.setSocket(con);
                daemonThread.start();
                
            }catch(IOException e)
            {
                LogMaker.errorPrintln("Error while accepting socket: "+e, -1);                
            }
            catch(InstantiationException e)
            {
                LogMaker.errorPrintln("Failed to create an instance ServerDaemonThread!" +
                        " Have you extended it with a parameterless constructor?\n"+e, -1);
            }
            catch(IllegalAccessException e)
            {
                LogMaker.errorPrintln("Failed to create an instance of ServerDaemonThread! \n"+ e, -1);
            }
        }
        
        
    }
}
