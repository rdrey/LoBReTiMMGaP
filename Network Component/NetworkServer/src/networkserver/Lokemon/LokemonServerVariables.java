/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.Lokemon;

import networkTransferObjects.Lokemon.LokemonPlayer;
import java.util.ArrayList;
import networkTransferObjects.Lokemon.LokemonPotion;

/**
 * @date 2011/09/16
 * @author Lawrence Webley
 */
public class LokemonServerVariables {

    public static ArrayList<LokemonPlayer> playerList = new ArrayList<LokemonPlayer>();
    public static ArrayList<LokemonPotion> itemList = new ArrayList<LokemonPotion>();
    public static final double areaOfInterest = 60.0;

    public static final double itemSpawnRangeMin = 50.0;
    public static final double itemSpawnRangeMax = 150.0;
    //Time in miliseconds before new items are spawned. Set at 30seconds by default
    public static final int itemSpawnTimer = 30000;


}
