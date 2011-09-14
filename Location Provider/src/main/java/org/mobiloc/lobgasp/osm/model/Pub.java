/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobiloc.lobgasp.osm.model;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.io.Serializable;
import org.mobiloc.lobgasp.model.SpatialObject;

/**
 *
 * @author rainerdreyer
 */
public class Pub extends SpatialObject implements Serializable {

    private String name;
    private Geometry geom;

    public Pub(String name, Geometry geom) {
        this.name = name;
        this.geom = GeometryFactory.createPointFromInternalCoord(((Point)geom).getCoordinate(),geom);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the geom
     */
    public Geometry getGeom() {
        return geom;
    }

    /**
     * @param geom the geom to set
     */
    public void setGeom(Geometry geom) {
        this.geom = geom;
    }


}
