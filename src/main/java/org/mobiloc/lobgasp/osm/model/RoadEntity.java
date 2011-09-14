/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobiloc.lobgasp.osm.model;

import javax.persistence.*;
import org.mobiloc.lobgasp.model.SpatialDBEntity;
import org.mobiloc.lobgasp.osm.parser.model.AbstractNode;
import org.mobiloc.lobgasp.osm.parser.model.Way;

/**
 *
 * @author rainerdreyer
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class RoadEntity extends SpatialDBEntity{
    private String name;

    private long OSMid;

    @Override
    public SpatialDBEntity construct(AbstractNode in)
    {
        this.setName(in.tags.get("name"));
        this.setOSMid(Long.parseLong(in.id));
        this.setGeom(((Way) in).getLineString());
        return this;
    }

    @Override
    public boolean xmlRule(AbstractNode in) {
        if (in.tags.containsKey("highway")) {
            return true;
        }
        return false;
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
     * @return the OSMid
     */
    public long getOSMid() {
        return OSMid;
    }

    /**
     * @param OSMid the OSMid to set
     */
    public void setOSMid(long OSMid) {
        this.OSMid = OSMid;
    }
}
