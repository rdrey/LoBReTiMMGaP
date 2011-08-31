package networkserver.Threads;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.ArrayBlockingQueue;
import networkTransferObjects.NetworkMessage;
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

    

    public ServerDaemonWriteoutThread(Socket writeOutSocket) throws IOException
    {
        socket = writeOutSocket;
        os = socket.getOutputStream();
        messageQueue = new ArrayBlockingQueue<NetworkMessage>(ServerCustomisation.threadWriteOutBufferSize);
    }

    //Tries to add the message to the queue of messages waiting to be sent to
    //the client. If the message queue is full, it will return false, otherwise true.
    public boolean writeMessage(NetworkMessage message)
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
                //Serialize the message
                NetworkMessage msg = messageQueue.take();
                Schema<NetworkMessage> schema = RuntimeSchema.getSchema(NetworkMessage.class);
                byte [] serializedObject = ProtostuffIOUtil.toByteArray(msg, schema, buffer);

                //Calculate and create a leading length field (6 bytes of data)
                NumberFormat nf = new DecimalFormat("000000");
                byte [] lengthField = nf.format(serializedObject.length).getBytes();

                //Stitch them together into one message
                byte [] message = new byte[serializedObject.length + lengthField.length];
                System.arraycopy(lengthField, 0, message, 0, lengthField.length);
                System.arraycopy(serializedObject, 0, message, lengthField.length, serializedObject.length);

                //And then send it off.
                os.write(message);
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
                System.err.println("Unexpected error occured in network write thread!\n "+ e);
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
