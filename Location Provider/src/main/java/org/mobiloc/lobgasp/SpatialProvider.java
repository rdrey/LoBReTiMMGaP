/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobiloc.lobgasp;

import com.vividsolutions.jts.geom.Point;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mobiloc.lobgasp.model.SpatialDBEntity;
import org.mobiloc.lobgasp.osm.model.BuildingEntity;
import org.mobiloc.lobgasp.osm.model.POIEntity;
import org.mobiloc.lobgasp.osm.model.RoadEntity;
import org.mobiloc.lobgasp.osm.model.WayEntity;
import org.mobiloc.lobgasp.osm.parser.OSMParser;
import org.mobiloc.lobgasp.osm.parser.model.OSM;
import org.mobiloc.lobgasp.osm.parser.model.OSMNode;
import org.mobiloc.lobgasp.osm.parser.model.Way;
import org.mobiloc.lobgasp.util.HibernateUtil;

/**
 *
 * @author rainerdreyer
 */
public class SpatialProvider {

    HashMap<SpatialDBEntity, SpatialDBEntity> objects;

    public SpatialProvider() {
        objects = new HashMap<SpatialDBEntity, SpatialDBEntity>();
    }

    void init() {
        //TODO find a nice spot to put map.osm if lobgasp is imported as package
        initFromFile("map.osm");
    }

    void initFromFile(String mapOsm) {
        OSM osm = null;

        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = s.beginTransaction();

        try {
            osm = OSMParser.parse(mapOsm);
        } catch (Exception ex) {
            Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Nodes first
        for (OSMNode node : osm.getNodes()) {
            boolean found = false;
            for (SpatialDBEntity so : objects.keySet()) {
                if (so.xmlRule(node)) {
                    SpatialDBEntity temp = new POIEntity();
                    try {
                        temp = so.getClass().newInstance();
                    } catch (InstantiationException ex) {
                        Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.out.println("Found " + so.getClass());
                    Serializable save = s.save(temp.construct(node));
                    Logger.getLogger(SpatialProvider.class.getName()).log(Level.INFO, save.toString());
                    found = true;
                }
            }
//            if (!found) {
//                POIEntity poi = new POIEntity();
//                poi.construct(node);
//                s.save(poi);
//            }
        }

        //Now ways
//        BuildingEntity building = new BuildingEntity();
//        RoadEntity road = new RoadEntity();
//
//        for (Way way : osm.getWays()) {
//            if (building.xmlRule(way)) {
//                BuildingEntity tempBuilding = new BuildingEntity();
//                tempBuilding.construct(way);
//                s.save(tempBuilding);
//            } else if (road.xmlRule(way)) {
//                RoadEntity tempRoad = new RoadEntity();
//                tempRoad.construct(way);
//                s.save(tempRoad);
//            } else {
//                WayEntity dbWay = new WayEntity();
//                dbWay.construct(way);
//                s.save(dbWay);
//            }
//        }

        //Then relations

        tx.commit();

    }

    List<SpatialDBEntity> provide(Point p, float radius) {

        return null;
    }

    void register(Class<? extends SpatialDBEntity> source, Class<? extends SpatialDBEntity> result) {
        try {
            objects.put(source.newInstance(), result.newInstance());
        } catch (InstantiationException ex) {
            Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(source);
    }
}
