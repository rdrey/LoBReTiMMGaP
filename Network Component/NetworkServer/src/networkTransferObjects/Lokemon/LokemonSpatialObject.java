/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package networkTransferObjects.Lokemon;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        objectId = id;
        this.type = type;
    }
    
    public LokemonSpatialObject()
    {
        
    }
    
    public void setGeomBytes(Geometry geom)
    {
        WKBWriter writer = new WKBWriter();
        wellKnownBytes = writer.write(geom);
    }

    public int getObjectId() {
        return objectId;
    }

    public SpatialObjectType getType() {
        return type;
    }
    
    public Geometry getGeom()
    {
        WKBReader reader = new WKBReader();
        try {
            return reader.read(wellKnownBytes);
        } catch (ParseException ex) {
            Logger.getLogger(LokemonSpatialObject.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    
}
