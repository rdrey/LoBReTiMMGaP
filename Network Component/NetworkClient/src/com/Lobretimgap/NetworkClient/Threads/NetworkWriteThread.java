package com.Lobretimgap.NetworkClient.Threads;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import networkTransferObjects.KeepAliveMessage;
import networkTransferObjects.NetworkMessage;
import networkTransferObjects.NetworkMessageLarge;
import networkTransferObjects.NetworkMessageMedium;
import networkTransferObjects.PlayerRegistrationMessage;
import android.util.Log;

import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

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
    
    private ByteBuffer b = ByteBuffer.allocate(4);
    
    private boolean connectionInUse = false;
    private Timer keepAliveTimer;
    private KeepAliveTask keepAliveTask;
    private boolean keepAliveBreak = false;
    
    private static Schema<PlayerRegistrationMessage> playerRegSchema = RuntimeSchema.getSchema(PlayerRegistrationMessage.class);
    private static Schema<NetworkMessageMedium> mediumMsgSchema = RuntimeSchema.getSchema(NetworkMessageMedium.class);
    private static Schema<NetworkMessageLarge> largeMsgSchema = RuntimeSchema.getSchema(NetworkMessageLarge.class);
    private static Schema<NetworkMessage> networkMsgSchema = RuntimeSchema.getSchema(NetworkMessage.class);

    

    public NetworkWriteThread(Socket writeOutSocket) throws IOException
    {
        socket = writeOutSocket;
        os = socket.getOutputStream();
        messageQueue = new ArrayBlockingQueue<NetworkMessage>(NetworkVariables.writeThreadBufferSize);
        b.order(ByteOrder.BIG_ENDIAN);
        
        if(NetworkVariables.keepAliveEnabled)
        {
        	keepAliveTimer = new Timer();
        	keepAliveTask = new KeepAliveTask();
        	keepAliveTimer.scheduleAtFixedRate(keepAliveTask, 200, 200);
        }
    }
    
    class KeepAliveTask extends TimerTask {
    	public void run() {
    		//We don't need a keep alive message if the connection is currently in use
    		if(!connectionInUse)
    		{
    			//We also don't want to flood the message queue, so only send something if the queue is empty
    			if(messageQueue.isEmpty())
    			{
    				writeMessage(new KeepAliveMessage());
    			}
    		}
    	}
    }

    //Tries to add the message to the queue of messages waiting to be sent to
    //the client. If the message queue is full, it will return false, otherwise true.
    public void writeMessage(NetworkMessage message) throws BufferOverflowException
    {
    	if(message != null)
    	{
    		if(!messageQueue.offer(message))
    		{
    			if(!socket.isClosed())
    				throw new BufferOverflowException();
    		}
    	}    	
    }

   
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public void run()
    {
        while(!stopOperation)
        {
            try
            {   
            	//Get the message for processing
            	NetworkMessage msg = messageQueue.take();
            	//Boolean which tells us we are currently using the connection
            	connectionInUse = true;
            	Schema schema;
            	//Used to flag what type of class this is in the message 
            	byte classType;
            	
            	if(msg instanceof KeepAliveMessage)
            	{
            		//Special case message. We don't want to waste processing time by doing a serialisation,
            		//so we just send 5 bits on the wire. (because we expect at least 5 on the receiving end)
            		classType = -1; //Signifies keepAlive
            		byte[] message = {classType, 0, 0, 0, 0};
            		os.write(message);
            		//Now just break from the rest of the operation
            		keepAliveBreak = true;
            	}
            	//Determine the message type (added descendants of NetworkMessage must be defined here)
            	if(msg instanceof PlayerRegistrationMessage)
            	{
            		schema = playerRegSchema;
            		//Arbitrarily classType numbers. Doesn't matter what they are as long as they match the numbers on the server side!
            		classType = 1;
            	}
            	else if(msg instanceof NetworkMessageMedium)
            	{
            		schema = mediumMsgSchema;
            		classType = 2;
            	}
            	else if(msg instanceof NetworkMessageLarge)
            	{
            		schema = largeMsgSchema;
            		classType = 3;
            	}
            	else//If its none of the registered subclasses of networkMessage
            	{
            		schema = networkMsgSchema;
            		classType = 0;
            	}
            	
            	if(!keepAliveBreak)
            	{            		
	            	//Serialize the message
	                byte [] serializedObject = ProtostuffIOUtil.toByteArray(msg, schema, buffer);
	                                
	                //Calculate and create a leading length field (4 bytes of data, an integer)
	                b.clear();
	                b.putInt(serializedObject.length);                
	                byte [] lengthField = b.array();                
	                //Stitch them together into one message
	                byte [] message = new byte[serializedObject.length + lengthField.length + 1];
	                message[0] = classType;
	                System.arraycopy(lengthField, 0, message, 1, lengthField.length);
	                System.arraycopy(serializedObject, 0, message, lengthField.length + 1, serializedObject.length);
	                
	                Log.d(NetworkVariables.TAG, "Serialised Size: "+serializedObject.length);
	                //And then send it off.
	                os.write(message);
            	}
                
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
            	connectionInUse = false;
            	keepAliveBreak = false;
                buffer.clear();
                b.clear();
            }
            
        }
    }

    public void shutdownThread()
    {
    	if(keepAliveTask != null)
    		keepAliveTask.cancel();
        stopOperation = true;
        this.interrupt();
    }
}
