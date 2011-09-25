/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import networkTransferObjects.Lokemon.LokemonPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import networkTransferObjects.Lokemon.LokemonPotion;

/**
 * @date 2011/09/16
 * @author Lawrence Webley
 */
public class LokemonServerVariables {

    public static ArrayList<LokemonPlayer> playerList = new ArrayList<LokemonPlayer>();
    public static HashMap<Integer, LokemonPotion> itemMap = new HashMap<Integer, LokemonPotion>();
    //Meters
    public static final double areaOfInterest = 60.0;

    //Meters
    public static final double itemSpawnRangeMin = 50.0;
    public static final double itemSpawnRangeMax = 150.0;

    //Time in miliseconds before new items are spawned. Set at 30seconds by default
    public static final int itemSpawnTimer = 30000;
    public static final int maxItemsIngame = 500;

    //Miliseconds to wait for additional item pickup requests before sending an ack to the player.
    public static final long pickupLatency = 200;


}
