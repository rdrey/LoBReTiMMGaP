package com.Lobretimgap.NetworkClient.Threads;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;

import android.util.Log;

import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.Utility.EventListenerList;
import com.Lobretimgap.NetworkClient.DataContainers.NetworkMessage;
import com.Lobretimgap.NetworkClient.EventListeners.*;
import com.Lobretimgap.NetworkClient.Events.*;

public class CoreNetworkThread extends Thread 
{
	private Socket socket;
	private NetworkWriteThread out;
	private ObjectInputStream in;
    private boolean stopOperation = false;
    
    private EventListenerList listeners = new EventListenerList();
	
	public CoreNetworkThread()
	{
		
	}
	
	public boolean connect()
	{
		try
		{
			Inet4Address hostAddress = (Inet4Address)InetAddress.getByName(NetworkVariables.hostname);
			socket = new Socket(hostAddress, NetworkVariables.port);
			in = new ObjectInputStream(socket.getInputStream());
			out = new NetworkWriteThread(socket);
			out.start();
		}
		catch(UnknownHostException e)
		{
			Log.e(NetworkVariables.TAG, "Failed to resolve host.", e);	
			return false;
		}
		catch(IOException e)
		{
			Log.e(NetworkVariables.TAG, "Error while initializing connection to server.", e);	
			return false;
		}
		
		return true;
	}
	
	/*
     * Sends an update message to the server. Can be anything the implementer
     * chooses. Used for general communication with the server.
     */
    public void sendGameUpdate(NetworkMessage message) throws BufferOverflowException
    {
        message.setMessageType(NetworkMessage.MessageType.UPDATE_MESSAGE);
        writeOut(message);
    }

    /*
     * This method should be used to request information from the server. The message
     * sent to the client should generally be one that requires a response, such as
     * what your rank is.
     */
    public void sendRequest(NetworkMessage message)throws BufferOverflowException
    {
        message.setMessageType(NetworkMessage.MessageType.REQUEST_MESSAGE);
        writeOut(message);
    }
	
	@Override
    public void run()
    {        
        //Do running stuff        
        while(!stopOperation)
        {
            try
            {
                Object data = in.readObject();                
                processNetworkMessage(data);
            }
            catch(InterruptedIOException e)
            {
                //We expect that something wants the threads attention. This is
                //used to immediately end the thread in shutdownThread().
            }
            catch(IOException e)
            {
                System.err.println("Error occured while reading from thread : "+e);
                fireEvent(new NetworkEvent(this, "Connection to client lost!\n" + e),  ConnectionLostListener.class);
                this.shutdownThread();                
                break;
            }
            catch(ClassNotFoundException e)
            {
                System.err.println("Unrecognised class object received from client - ignoring");
            } 
        }
    }
	
	private void processNetworkMessage(Object data)
	{
		
	}
	
	/*
     * Writes a given object to the outputstream
     */
    private void writeOut(Object object) throws BufferOverflowException
    {
        //Later perhaps we can more gracefully deal with this. Perhaps add wait
        //a little while and then try again?
        if(!out.writeMessage(object))
        {
            throw new BufferOverflowException();
        }
    }

    public void shutdownThread()
    {
        try
        {
            out.shutdownThread();
            stopOperation = true;
            this.interrupt();
            socket.close();
        }
        catch(IOException e)
        {
            //We don't really care if the socket failed to close correctly.
            System.err.println("Socket failed to close correctly. \n"+e);
        }  
    }
    
    /************************************************* Event Handling *********************************************************/
    
    public <T extends NetworkEventListener> void addNetworkListener(Class<T> t, T listener)
    {
        listeners.add(t, listener);
    }
    
    public <T extends NetworkEventListener> void removeNetworkListener(Class<T> t, T listener)
    {
        listeners.remove(t, listener);
    }
    
    /**
     * Used to fire off a network event in a generalised manner. Takes the event to
     * be fired (all events are expected to be children of NetworkEvent) and the
     * class of the listener to be notified of the event.
     * @param <T> The type of listener that we wish to fire events on.
     * @param event The event we would like to propagate to the event listeners
     * @param t The class type of the listeners we wish to fire events on.
     */
    private <T extends NetworkEventListener> void fireEvent(NetworkEvent event, Class<T> t)
    {
        Object[] listenerArray = listeners.getListenerList();

        //Loop through the listeners, notifying those of the correct type.
        for(int i = 0; i < listenerArray.length; i+=2)
        {
            if(listenerArray[i] == t)
            {
                /*
                 * This might seem a little confusing at first, so here's an explanation.
                 * The listener list stores event listeners in pairs of
                 * {listener.class, instance of listener}. This means that
                 * i will be the class of the listener, which we compare with our
                 * given class type T. If it matches we cast the instance of the
                 * Listener as our given class type T (which we have checked, it actually
                 * is) and then run its EventOccured method.
                 */
                t.cast(listenerArray[i+1]).EventOccured(event);
            }
        }
    }
}
