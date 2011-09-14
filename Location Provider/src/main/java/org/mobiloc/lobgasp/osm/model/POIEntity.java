/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobiloc.lobgasp.osm.model;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import javax.persistence.*;
import org.mobiloc.lobgasp.model.SpatialDBEntity;

/**
 *
 * @author rainerdreyer
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class POIEntity extends SpatialDBEntity{

    /**
     * @return the lat
     */
    @Transient
    public double getLat() {
        return ((Point) this.getGeom()).getY();
    }

    /**
     * @param lat the lat to set
     */
    public void setLat(double lat) {
        this.setGeom(new GeometryFactory().createPoint(new Coordinate(((Point) this.getGeom()).getX(), lat)));
    }

    /**
     * @return the lon
     */
    @Transient
    public double getLon() {
        return ((Point) this.getGeom()).getX();
    }

    /**
     * @param lon the lon to set
     */
    public void setLon(double lon) {
        this.setGeom(new GeometryFactory().createPoint(new Coordinate(lon, ((Point) this.getGeom()).getX())));
    }

}
