package networkserver.Threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
            System.err.println("Error aquiring port: "+e);
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
                System.out.println("Waiting for a connection....");
                Socket con = socket.accept();                
                System.out.println("Client accepted: "+ con);                
                ServerDaemonThread daemonThread = customSettings.buildInstance();
                daemonThread.setSocket(con);
                daemonThread.start();
                
            }catch(IOException e)
            {
                System.err.println("Error while accepting socket: "+e);                
            }
            catch(InstantiationException e)
            {
                System.err.println("Failed to create an instance ServerDaemonThread!" +
                        " Have you extended it with a parameterless constructor?\n"+e);
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Failed to create an instance of ServerDaemonThread! \n"+ e);
            }
        }
        
        
    }
}
