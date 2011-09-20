package org.mobiloc.lobgasp;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mobiloc.lobgasp.model.SpatialDBEntity;
import org.mobiloc.lobgasp.model.SpatialObject;
import org.mobiloc.lobgasp.osm.model.POIEntity;
import org.mobiloc.lobgasp.osm.model.WayEntity;
import org.mobiloc.lobgasp.osm.model.Ways.ForestEntity;
import org.mobiloc.lobgasp.osm.parser.OSMParser;
import org.mobiloc.lobgasp.osm.parser.model.AbstractNode;
import org.mobiloc.lobgasp.osm.parser.model.OSM;
import org.mobiloc.lobgasp.util.HibernateUtil;

/**
 *
 * @author rainerdreyer
 */
public class SpatialProvider {

    HashMap<POIEntity, SpatialObject> pointsOfInterest;
    HashMap<WayEntity, SpatialObject> waysOfInterest;
    Geometry example;

    public SpatialProvider() {
        pointsOfInterest = new HashMap<POIEntity, SpatialObject>();
        waysOfInterest = new HashMap<WayEntity, SpatialObject>();
        example = new GeometryFactory().createPoint(new Coordinate(0.0, 0.0));
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

        //Save nodes that match pointsOfInterest
        saveCollections(s, pointsOfInterest, osm.getNodes());
        //Now ways
        saveCollections(s, waysOfInterest, osm.getWays());

        tx.commit();
    }

    private void saveCollections(Session s, HashMap mappings, Set nodes) {
        for (Object poiOrWay : nodes) {
            for (Object so : mappings.keySet()) {
                if (((SpatialDBEntity)so).xmlRule((AbstractNode) poiOrWay)) {
                    try {
                        SpatialDBEntity temp = ((SpatialDBEntity)so).getClass().newInstance();
//                        System.out.println("Found " + so.getClass());
                        Serializable save = s.save(temp.construct((AbstractNode) poiOrWay));
                    } catch (InstantiationException ex) {
                        Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    public void addCustomAreaAroundPoint(Class<? extends SpatialDBEntity> aClass, Coordinate coordinate) {
        addCustomAreaAroundPoint(aClass, coordinate, 0.0001f);
    }

    public void addCustomAreaAroundPoint(Class<? extends SpatialDBEntity> type, Coordinate coordinate, float radius) {
        try {
            Session s = HibernateUtil.getSessionFactory().getCurrentSession();
            Transaction tx = s.beginTransaction();

            SpatialDBEntity entity = type.newInstance();
            Point point = GeometryFactory.createPointFromInternalCoord(coordinate, example);
            entity.setGeom(point.buffer(radius));
            s.save(entity);

            tx.commit();
        } catch (InstantiationException ex) {
            Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    List<SpatialDBEntity> provide(Point p, float radius) {

        return null;
    }

    void register(Class<? extends SpatialDBEntity> source, Class<? extends SpatialObject> result) {
        try {

            if (source.newInstance() instanceof POIEntity) {
                Logger.getLogger(SpatialProvider.class.getName()).log(Level.INFO, "Registered in POIs: {0}", source.getSimpleName());
                pointsOfInterest.put((POIEntity) source.newInstance(),result.newInstance());
            }

            else if (source.newInstance() instanceof WayEntity) {
                Logger.getLogger(SpatialProvider.class.getName()).log(Level.INFO, "Registered in Ways: {0}", source.getSimpleName());
                waysOfInterest.put((WayEntity) source.newInstance(),result.newInstance());
            }

        } catch (InstantiationException ex) {
            Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SpatialProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
