package org.mobiloc.lobgasp;

import com.vividsolutions.jts.geom.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.mobiloc.lobgasp.util.HibernateUtil;
import org.hibernate.Transaction;
import org.mobiloc.lobgasp.model.SpatialDBEntity;
import org.mobiloc.lobgasp.osm.model.Ways.BuildingEntity;
import org.mobiloc.lobgasp.osm.model.POIs.LibraryEntity;
import org.mobiloc.lobgasp.osm.model.POIs.PubEntity;
import org.mobiloc.lobgasp.osm.model.Ways.FieldEntity;
import org.mobiloc.lobgasp.osm.model.Ways.ForestEntity;
import org.mobiloc.lobgasp.osm.model.Ways.ParkingEntity;
import org.mobiloc.lobgasp.osm.model.Ways.ReservoirEntity;
import org.mobiloc.lobgasp.osm.model.Ways.RoadEntity;
import org.mobiloc.lobgasp.osm.model.Ways.StepsEntity;
import org.mobiloc.lobgasp.osm.model.Ways.TunnelEntity;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) {
        System.out.println("Hello World!");

        SpatialProvider sp = new SpatialProvider();
        //TODO interface for custom objects
        sp.register(PubEntity.class, PubEntity.class);
        sp.register(LibraryEntity.class, LibraryEntity.class);

        sp.register(TunnelEntity.class, TunnelEntity.class);
        sp.register(StepsEntity.class, StepsEntity.class);
        sp.register(ParkingEntity.class, ParkingEntity.class);
        sp.register(ForestEntity.class, ForestEntity.class);
        sp.register(FieldEntity.class, FieldEntity.class);
        sp.register(ReservoirEntity.class, ReservoirEntity.class);
        sp.register(RoadEntity.class, RoadEntity.class);
        sp.register(BuildingEntity.class, BuildingEntity.class);
        
        sp.initFromFile("campus.osm");

        Session s = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = s.beginTransaction();

        List so = s.createQuery("from PubEntity where name like 'UCT Club'").list();
        PubEntity pub = (PubEntity) so.get(0);
        List cs = s.createQuery("from LibraryEntity where name like 'Rondebosch Public Library'").list();
        LibraryEntity lib = (LibraryEntity) cs.get(0);

        System.out.println("Distance: " + Math.toRadians(lib.getGeom().distance(pub.getGeom())) * Math.PI / 180.0 * 6378137.0);
        System.out.println("Distance: " + lib.getGeom().distance(pub.getGeom()));
        System.out.println("Distance: " + distance((Point)lib.getGeom(), (Point)pub.getGeom()));

        serializeResults(PubEntity.class, "pub.out", s);
//        serializeResults(Road.class, "roads.out", s);
//        serializeResults(Building.class, "buildings.out", s);

        tx.commit();
    }
    
    public static double distance(Point a, Point b)
    {
        return distFrom(a.getY(), a.getX(), b.getY(), b.getX());
    }

    public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371.009;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;

        return dist;
    }

    private static void serializeResults(Class object, String toFile, Session s) {
        FileOutputStream fos = null;
        Criteria query = s.createCriteria(object);
        //This is how to later filter by geometry:
        //        query.add(SpatialRestrictions.within("geom", filter));
        List results = query.list();
        List temp = new LinkedList();

        for (Object t: results) {
            temp.add(((SpatialDBEntity) t).toSimple());
        }

        try {
            fos = new FileOutputStream(toFile);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(temp);
            out.close();
        } catch (Exception ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
