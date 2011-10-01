/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobiloc.lobgasp.osm.model.POIs;

import javax.persistence.*;
import org.mobiloc.lobgasp.model.SpatialDBEntity;
import org.mobiloc.lobgasp.osm.model.POIEntity;
import org.mobiloc.lobgasp.osm.parser.model.AbstractNode;
import org.mobiloc.lobgasp.osm.parser.model.OSMNode;

/**
 *
 * @author rainerdreyer
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class NamedPOIEntity extends POIEntity {

    private String name;

    public NamedPOIEntity() {
    }

    @Override
    public SpatialDBEntity construct(AbstractNode in) {
        this.setName(in.tags.get("name"));
        this.setGeom(((OSMNode) in).getGeom().buffer(0.0001f));
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



}
