package org.mobiloc.lobgasp.osm.parser.model;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKBWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Willy Tiengo
 */
public class Relation extends AbstractNode {

    private OSM osm;
    public List<Member> members;

    public Relation(OSM osm, String id, String visible, String timestamp,
            String version, String changeset, String user,
            String uid, List<Member> members, Map<String, String> tags) {

        super(id, visible, timestamp, version, changeset, user, uid, tags);
        this.osm = osm;
        this.members = members;
    }

    /**
     * @return The MultiLineString of all ways members of this relation. If any
     *         way members can not be found in the datase, returns
     *         <code>null</code>.
     */
    public Polygon getPolygon() {
        Way way;
        List<Coordinate> lines = new ArrayList<Coordinate>();

        for (Member member : members) {
            if (isWay(member)) {
                way = osm.getWay(member.ref);

                if (way == null) {
                    return null;
                }

                List<Coordinate> coord = Arrays.asList(way.getLineString().getCoordinates());

                if (!lines.isEmpty()) {
                    Coordinate c = lines.get(lines.size() - 1);

                    if (!c.equals(coord.get(0))) {

                        if (c.equals(coord.get(coord.size() - 1))) {

                            Collections.reverse(coord);

                        } else {

                            Collections.reverse(lines);
                            c = lines.get(lines.size() - 1);
                            
                            if (!c.equals(coord.get(0))) {
                                Collections.reverse(coord);
                            }

                        }

                    }
                }

                lines.addAll(coord);
            }
        }

        GeometryFactory fac = new GeometryFactory();
        return fac.createPolygon(fac.createLinearRing(lines.toArray(
                new Coordinate[0])), null);
    }

    public boolean isBoundary() {
        return tags.get("boundary") != null;
    }

    public int getAdminLevel() {
        return Integer.parseInt(tags.get("admin_level"));
    }

    public String getName() {
        return tags.get("name");
    }

    public String getShape() {
        Polygon pol = getPolygon();
        return (pol != null) ? WKBWriter.bytesToHex(new WKBWriter().write(pol)) : null;
    }

    private boolean isWay(Member m) {
        return m.type.equals("way");
    }
}
