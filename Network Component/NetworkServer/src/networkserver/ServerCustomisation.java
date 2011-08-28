package networkserver;

import networkserver.Threads.ServerDaemonThread;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 *
 * This class forms a centralised point from where customisations to the network
 * component can be made. There are a set of required variables that must be set
 * and many more optional ones which can be set. The generic class it wants is
 * a implemented version of ServerDaemonThread.
 */
public class ServerCustomisation <T extends ServerDaemonThread>
{
    //Public variables
    public static final int threadWriteOutBufferSize = 16;
    public static final int initialNetworkMessageMapSize = 8;
    public static final int port = 10282;
    
    //Stores an the type of class, so we can instantiate them
    private Class<T> ServerDaemonThreadType;

    public ServerCustomisation(Class<T> classType)
    {
        ServerDaemonThreadType = classType;
    }

    public T buildInstance() throws InstantiationException, IllegalAccessException
    {
        return ServerDaemonThreadType.newInstance();
    }
}
