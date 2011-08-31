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
public class NetworkMessageLarge extends NetworkMessage {
    
    public List<Integer> ints;
    public List<String> strings;
    public List<Long> longs;
    public List<Boolean> bools;

    public NetworkMessageLarge(String message)
    {
        super(message);
        strings = new ArrayList<String>();
    	ints = new ArrayList<Integer>();
        longs = new ArrayList<Long>();
        bools = new ArrayList<Boolean>();
    }
    
    /**
     * Gets the runtime schema of this class for serialization.
     * If you inherit from this class, you MUST OVERRIDE this method, 
     * otherwise it will be serialized as its parent, and you will lose data.
     * 
     * Additionally you will need to add a case for it in the network read and write 
     * methods, so that the receiving end knows what type of class to deserialize it as.
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
	public Schema getSchema()
    {
    	return RuntimeSchema.getSchema(NetworkMessageLarge.class);
    }
    

}
