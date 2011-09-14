/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobiloc.lobgasp.osm.parser.model;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTWriter;
import java.util.Map;

/**
 *
 * @author Willy Tiengo
 */
public class OSMNode extends AbstractNode {

    public String lat;
    public String lon;

    public OSMNode(String id, String visible, String timestamp, String version, String changeset, String user, String uid, String lat, String lon, Map<String, String> tags) {
        super(id, visible, timestamp, version, changeset, user, uid, tags);
        this.lat = lat;
        this.lon = lon;
        this.tags = tags;
    }

    public Geometry getGeom() {
        return new GeometryFactory().createPoint(new Coordinate(Double.valueOf(lon), Double.valueOf(lat)));
    }

    public String getLocation() {
        Point p = new GeometryFactory().createPoint(
                new Coordinate(Double.valueOf(lon), Double.valueOf(lat)));

        WKTWriter w = new WKTWriter(2);
        return w.writeFormatted(p);
        //return WKBWriter.bytesToHex(new WKBWriter().write(p));
    }
}
