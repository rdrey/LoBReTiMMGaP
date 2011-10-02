package networkserver.Threads;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import networkTransferObjects.*;
import networkTransferObjects.UtilityObjects.QuickLZ;
import networkserver.LogMaker;
import networkserver.ServerCustomisation;

/**
 * This thread allows the user to write an object to the client asynchronously.
 * Internally it adds the object to be written to a queue, and sends items over the
 * network in a FIFO fashion
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class ServerDaemonWriteoutThread extends Thread
{
    private Socket socket;
    private OutputStream os;
    private ArrayBlockingQueue<NetworkMessage> messageQueue;
    private boolean stopOperation = false;
    private LinkedBuffer buffer = LinkedBuffer.allocate(512);
    private ByteBuffer b = ByteBuffer.allocate(4);

    private static Schema<PlayerRegistrationMessage> playerRegSchema = RuntimeSchema.getSchema(PlayerRegistrationMessage.class);
    private static Schema<NetworkMessageMedium> mediumMsgSchema = RuntimeSchema.getSchema(NetworkMessageMedium.class);
    private static Schema<NetworkMessageLarge> largeMsgSchema = RuntimeSchema.getSchema(NetworkMessageLarge.class);
    private static Schema<NetworkMessage> networkMsgSchema = RuntimeSchema.getSchema(NetworkMessage.class);

    

    public ServerDaemonWriteoutThread(Socket writeOutSocket) throws IOException
    {
        socket = writeOutSocket;
        os = socket.getOutputStream();        
        messageQueue = new ArrayBlockingQueue<NetworkMessage>(ServerCustomisation.threadWriteOutBufferSize);
        b.order(ByteOrder.BIG_ENDIAN);
        
    }

    //Tries to add the message to the queue of messages waiting to be sent to
    //the client. If the message queue is full, it will return false, otherwise true.
    public boolean writeMessage(NetworkMessage message)
    {
        if(messageQueue.size() > 0.8 * ServerCustomisation.threadWriteOutBufferSize)
        {
            LogMaker.println("WARNING: Output buffer is at "+messageQueue.size()+"/"+ServerCustomisation.threadWriteOutBufferSize);
        }
        return messageQueue.offer(message);
    }

    @Override
    public void run()
    {
        while(!stopOperation)
        {
            try
            {
                //Get the message for processing
            	NetworkMessage msg = messageQueue.take();
            	Schema schema;
            	//Used to flag what type of class this is in the message
            	byte classType;

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

            	//Serialize the message
                byte [] serializedObject = ProtostuffIOUtil.toByteArray(msg, schema, buffer);
                
                //Compress the serialized bytes
                //byte [] tempArray = QuickLZ.compress(serializedObject, 3);
                //serializedObject = tempArray;

                //Calculate and create a leading length field (4 bytes of data, an integer)                
                b.clear();
                b.putInt(serializedObject.length);
                byte [] lengthField = b.array();
                //Stitch them together into one message
                byte [] message = new byte[serializedObject.length + lengthField.length + 1];
                message[0] = classType;
                System.arraycopy(lengthField, 0, message, 1, lengthField.length);
                System.arraycopy(serializedObject, 0, message, lengthField.length + 1, serializedObject.length);

                //Log.d(NetworkVariables.TAG, "Serialized Size: "+serializedObject.length);
                //And then send it off.
                
                //If message is too big, split it into multiple pieces
                if(message.length > 8000)
                {
                    LogMaker.println("Need to send "+message.length+" bytes");
                    int numMsgs = message.length /4096;
                    int remainder = message.length % 4096;
                    byte [][] msgs = new byte [numMsgs][4096];
                    byte[] lastMsg = new byte [remainder];

                    for(int i = 0; i < numMsgs;i++)
                    {
                        msgs[i] = Arrays.copyOfRange(message, i*4096, (i*4096) + 4096);
                        os.write(msgs[i]);
                        //LogMaker.println("Sent "+(i*4096+4096)+"/"+message.length+" bytes");
                    }
                    
                    if(remainder != 0)
                    {
                        lastMsg = Arrays.copyOfRange(message, numMsgs*4096, message.length);
                        os.write(lastMsg);
                        LogMaker.println("Sent "+(lastMsg.length + numMsgs*4096)+"/"+message.length+" bytes");
                    }


                }
                else
                {
                    os.write(message);                    
                }
                os.flush();
            }
            catch(IOException e)
            {
                LogMaker.errorPrintln("IOEXCEPTION: Failed to send object to client! \n"+e);
            }
            catch(InterruptedException e)
            {
                //We have been interrupted, so restart the loop.
                //This is used in shutdownThread, after setting stopOperation to true
                //To enforce an immediate thread shutdown.
            }
            catch(Exception e)
            {
                LogMaker.errorPrintln("Unexpected error occured in network write thread!\n " );
                e.printStackTrace(System.err);
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
