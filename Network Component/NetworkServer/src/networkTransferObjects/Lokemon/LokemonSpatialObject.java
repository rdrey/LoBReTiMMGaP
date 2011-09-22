/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package networkTransferObjects.Lokemon;

/**
 *
 * @author Lawrence Webley & Rainer Dreyer
 */
public class LokemonSpatialObject {
    
    public enum SpatialObjectType
    {
        CAVE,
        FOREST,
        GRASSLAND,
        MOUNTAIN,
        ROUGH,
        URBAN,
        WATER,        
        CLINIC,
        SHOP
    };
    
    byte [] wellKnownBytes;
    int objectId;
    SpatialObjectType type;
    
    public LokemonSpatialObject(int id, SpatialObjectType type)
    {
        id = objectId;
    }
    
    public void setGeomBytes()
    {
        
    }
    
}
