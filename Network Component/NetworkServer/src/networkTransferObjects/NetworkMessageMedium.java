/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkTransferObjects;

import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import java.util.ArrayList;
import java.util.List;

/**
 * @date 2011/08/30
 * @author Lawrence Webley
 */
public class NetworkMessageMedium extends NetworkMessage{
    
    public List<String> strings;
    public NetworkMessageMedium(String message)
    {
        super(message);
        strings = new ArrayList<String>();    	
    }

    public NetworkMessageMedium()
    {
        super();
    }

}
