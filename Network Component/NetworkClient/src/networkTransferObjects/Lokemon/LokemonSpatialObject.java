/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package networkTransferObjects.Lokemon;

import com.vividsolutions.jts.geom.Coordinate;
import networkTransferObjects.UtilityObjects.Location;

/**
 *
 * @author Lawrence Webley & Rainer Dreyer
 */
public class LokemonSpatialObject {
    
    private SpatialObjectType Objtype;
    
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

    private Location[] points;
    //private byte [] wellKnownBytes;
    private int objectId;   
       
    
    public LokemonSpatialObject(int id, SpatialObjectType objectType)
    {
        objectId = id;
        Objtype = objectType;
    }
    
    public LokemonSpatialObject()
    {
        
    }

    public void setCoords(Coordinate[] coordinates)
    {
        points = new Location[coordinates.length];
        for(int i = 0; i < coordinates.length; i++)
        {
            points[i] = new Location(coordinates[i].x, coordinates[i].y);
        }
    }

    public Location[] getCoords()
    {
        return points;
    }
    
//    public void setGeomBytes(Geometry geom)
//    {
//        WKBWriter writer = new WKBWriter();
//        wellKnownBytes = writer.write(geom);
//    }

    public int getObjectId() {
        return objectId;
    }

    
    public SpatialObjectType getType() {
        return Objtype;
    }
    
//    public Geometry getGeom()
//    {
//
//        WKBReader reader = new WKBReader();
//        try {
//            return reader.read(wellKnownBytes);
//        } catch (ParseException ex) {
//            Logger.getLogger(LokemonSpatialObject.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//        }
//    }

    
    
}
