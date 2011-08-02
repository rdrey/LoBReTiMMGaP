
package networkserver.DataContainers;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Used to pass information between the client and the server
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class NetworkMessage implements Serializable
{
    private HashMap<String, String> strings;
    private HashMap<String, Integer> ints;
    private HashMap<String, Object> objects;

    private String primeMessage;

    public NetworkMessage(String message)
    {
        primeMessage = message;
    }
   
    public void addDataString(String key, String value)
    {
        strings.put(key, value);
    }

    public void addDataInt(String key, int value)
    {
        ints.put(key, new Integer(value));
    }

    /*
     * Adds an object to this network message. The object must implement serializable
     */
    public void addDataObject(String key, Object value) throws IllegalArgumentException
    {
        if(value instanceof java.io.Serializable)
        {
            objects.put(key, value);
        }
        else
        {
            throw new IllegalArgumentException("Object is not serializable!");
        }
    }

    public String getDataString(String key)
    {
        return strings.get(key);
    }

    public int getDataInt(String key)
    {
        return ints.get(key).intValue();
    }

    public Object getDataObject(String key)
    {
        return objects.get(key);
    }

    public String getMessage()
    {
        return primeMessage;
    }
}
