package org.mobiloc.lobgasp.osm.model.Ways;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import javax.persistence.*;
import org.mobiloc.lobgasp.model.SpatialDBEntity;
import org.mobiloc.lobgasp.osm.model.WayEntity;
import org.mobiloc.lobgasp.osm.parser.model.AbstractNode;
import org.mobiloc.lobgasp.osm.parser.model.Way;

/**
 *
 * @author rainerdreyer
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class FieldEntity extends WayEntity {

    @Override
    public SpatialDBEntity construct(AbstractNode in)
    {
        this.setName(in.tags.get("name"));
        this.setOSMid(Long.parseLong(in.id));
        LineString lineString = ((Way) in).getLineString();
        LinearRing ring = lineString.getFactory().createLinearRing(lineString.getCoordinateSequence());
        Geometry poly = lineString.getFactory().createPolygon(ring, null);
//        Logger.getLogger(ForestEntity.class.getName()).log(Level.INFO, "message {0}", poly.getNumPoints());
        this.setGeom(poly);
        return this;
    }

    @Override
    public boolean xmlRule(AbstractNode in) {
        if (in.tags.containsKey("leisure") && (
                in.tags.get("leisure").equals("pitch") || 
                in.tags.get("leisure").equals("sports_centre"))) {
            return true;
        }
        return false;
    }

}