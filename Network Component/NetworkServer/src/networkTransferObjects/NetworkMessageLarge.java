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
     * Gets the class serialization schema for network serialization.
     * You MUST override this method if you decide to extend this class.
     * @return The schema to seralize this class with
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Schema getSchema()
    {
        return RuntimeSchema.getSchema(NetworkMessageLarge.class);
    }

}
