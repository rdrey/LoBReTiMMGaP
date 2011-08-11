package com.Lobretimgap.NetworkClient.Threads;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

import com.Lobretimgap.NetworkClient.NetworkVariables;

public class NetworkWriteThread extends Thread 
{	
	Socket socket;
	private ObjectOutputStream oos;
	private ArrayBlockingQueue<Object> messageQueue;
	private boolean stopOperation = false;
	
	public NetworkWriteThread(Socket netSocket) throws IOException
	{
		socket = netSocket;
		oos = new ObjectOutputStream(socket.getOutputStream());
        messageQueue = new ArrayBlockingQueue<Object>(NetworkVariables.writeThreadBufferSize);
	}
	
	//Tries to add the message to the queue of messages waiting to be sent to
    //the client. If the message queue is full, it will return false, otherwise true.
    public boolean writeMessage(Object message)
    {
        return messageQueue.offer(message);
    }

    @Override
    public void run()
    {
        while(!stopOperation)
        {
            try
            {
                Object message = messageQueue.take();
                oos.writeObject(message);
                oos.flush();
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
            
        }
    }

    public void shutdownThread()
    {
        stopOperation = true;
        this.interrupt();
    }
}
