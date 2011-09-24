/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkTransferObjects.Lokemon;

import networkTransferObjects.UtilityObjects.Location;

/**
 * @date 2011/09/16
 * @author Lawrence Webley
 */
public class LokemonPotion
{
    public static enum PotionType
    {
        HEALTH,
        ATTACK,
        DEFENSE,
        SPECIAL,
        SPEED;
    }

    private Location position;
    private PotionType type;    
    private int id;

    public Location getPosition() {
        return position;
    }

    public void setPosition(Location position) {
        this.position = position;
        
    }

    public PotionType getType() {
        return type;
    }

    public void setType(PotionType type) {
        this.type = type;
    }

    public LokemonPotion(PotionType type, int id)
    {
        this.type = type;
        this.id = id;
    }

    public LokemonPotion()
    {
        //default constructor for serialization
    }

    public int getId()
    {
        return id;
    }
}
