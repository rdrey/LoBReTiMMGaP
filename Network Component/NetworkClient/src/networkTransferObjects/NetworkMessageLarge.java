/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkTransferObjects;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @date 2011/08/30
 * @author Lawrence Webley
 */
public class NetworkMessageLarge extends NetworkMessage {

    public ArrayList<Integer> integers;
    public ArrayList<Double> doubles;

    public HashMap<String, Object> objectDict;
	public HashMap<String, String> stringDict;
        
    public ArrayList<String> strings;
        
    public NetworkMessageLarge(String message)
    {
        super(message);        
        
        objectDict = new HashMap<String, Object>();
        stringDict = new HashMap<String, String>();
        integers = new ArrayList<Integer>();
        doubles = new ArrayList<Double>();
        strings = new ArrayList<String>();        
    }

    public NetworkMessageLarge()
    {
        super();
    }
}
