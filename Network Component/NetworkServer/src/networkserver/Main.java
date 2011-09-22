package networkserver;

import networkserver.Lokemon.LokemonDaemonThread;
import networkserver.Lokemon.LokemonServerLogic;
import networkserver.Threads.ServerCoreThread;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class Main
{
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        //Start the core server thread. In an implementation, the game engine could
        //start this thread, or it could be started by this thread.

       //Start networking component
       ServerCoreThread sct = new ServerCoreThread(new ServerCustomisation<LokemonDaemonThread>(LokemonDaemonThread.class));
       sct.start();

       //Start game logic component
       LokemonServerLogic lsl = new LokemonServerLogic();
       lsl.start();
    }

}
