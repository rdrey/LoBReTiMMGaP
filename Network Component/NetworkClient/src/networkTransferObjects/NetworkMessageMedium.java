/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkTransferObjects;

import java.util.ArrayList;

/**
 * @date 2011/08/30
 * @author Lawrence Webley
 */
public class NetworkMessageMedium extends NetworkMessage{
    
	public ArrayList<Double> doubles;
	public ArrayList<Integer> integers;
	public ArrayList<String> strings;	
    
    public NetworkMessageMedium(String message)
    {
        super(message);
        strings = new ArrayList<String>();
        integers = new ArrayList<Integer>(); 
        doubles = new ArrayList<Double>();
    }

    public NetworkMessageMedium()
    {
        super();
    }

}
