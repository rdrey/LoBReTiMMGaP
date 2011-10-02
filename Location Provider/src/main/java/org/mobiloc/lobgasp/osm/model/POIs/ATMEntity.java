/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobiloc.lobgasp.osm.model.POIs;

import javax.persistence.*;
import org.mobiloc.lobgasp.model.SpatialObject;
import org.mobiloc.lobgasp.osm.model.Pub;
import org.mobiloc.lobgasp.osm.parser.model.AbstractNode;

/**
 *
 * @author rainerdreyer
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ATMEntity extends NamedPOIEntity {

    @Override
    public SpatialObject toSimple() {
        return new Pub(this.getName(), getGeom());
    }

    @Override
    public boolean xmlRule(AbstractNode in) {
        if (in.tags.containsKey("amenity") && in.tags.get("amenity").equalsIgnoreCase("atm")) {
//            System.out.println("Found atm");
            return true;
        }
        return false;
    }
}
