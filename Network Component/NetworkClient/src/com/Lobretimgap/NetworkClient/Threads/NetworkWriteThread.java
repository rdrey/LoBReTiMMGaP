package com.Lobretimgap.NetworkClient.Threads;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

import networkTransferObjects.NetworkMessage;

import android.util.Log;

import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;

/**
 * This thread allows the user to write an object to the client asynchronously.
 * Internally it adds the object to be written to a queue, and sends items over the
 * network in a FIFO fashion
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class NetworkWriteThread extends Thread
{
    private Socket socket;
    private OutputStream os;
    private ArrayBlockingQueue<NetworkMessage> messageQueue;
    private boolean stopOperation = false;
    private LinkedBuffer buffer = LinkedBuffer.allocate(512);

    

    public NetworkWriteThread(Socket writeOutSocket) throws IOException
    {
        socket = writeOutSocket;
        os = socket.getOutputStream();
        messageQueue = new ArrayBlockingQueue<NetworkMessage>(NetworkVariables.writeThreadBufferSize);
    }

    //Tries to add the message to the queue of messages waiting to be sent to
    //the client. If the message queue is full, it will return false, otherwise true.
    public boolean writeMessage(NetworkMessage message)
    {
    	if(message != null)
    	{
    		return messageQueue.offer(message);
    	}
    	else
    	{
    		return false;
    	}
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public void run()
    {
        while(!stopOperation)
        {
            try
            {
                NetworkMessage msg = messageQueue.take();
                Schema schema = msg.getSchema();

                ProtostuffIOUtil.writeTo(os, msg, schema, buffer);
            }
            catch(IOException e)
            {
                System.err.println("Failed to send object to client! \n"+e);
            }
            catch(InterruptedException e)
            {
                //We have been interrupted, so restart the loop.
                //This is used in shutdownThread, after setting stopOperation to true
                //To enforce an immediate thread shutdown.
            }
            catch(Exception e)
            {
            	Log.e(NetworkVariables.TAG, "Unexpected error in network write thread.", e);
            }
            finally
            {
                buffer.clear();
            }
            
        }
    }

    public void shutdownThread()
    {
        stopOperation = true;
        this.interrupt();
    }
}
