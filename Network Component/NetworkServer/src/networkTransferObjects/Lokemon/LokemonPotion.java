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
    private Integer id;
    public static enum PotionType
    {
        HEALTH,
        ATTACK,
        DEFENSE,
        SPECIAL,
        SPEED
    }
    private Location position;
    private PotionType type;
    


    public LokemonPotion(PotionType potType, int ident)
    {
        id = ident;
        type = potType;
        position = new Location();
    }

    public LokemonPotion()
    {
        //default constructor for serialization
//        identifier = -1;
        position = new Location();
    }

    public Location getPosition() {
        return position;
    }

    public void setPosition(Location position) {
        this.position = position;

    }

    public PotionType getType() {
        return type;
    }

    public void setType(PotionType potType) {
        type = potType;
    }

    public int getId()
    {
        return id;
    }
}
