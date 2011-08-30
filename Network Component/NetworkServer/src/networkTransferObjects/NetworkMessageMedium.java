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

    public List<Integer> ints;
    public List<String> strings;
    public NetworkMessageMedium(String message)
    {
        super(message);
        strings = new ArrayList<String>();
    	ints = new ArrayList<Integer>();
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
        return RuntimeSchema.getSchema(NetworkMessageMedium.class);
    }

}
