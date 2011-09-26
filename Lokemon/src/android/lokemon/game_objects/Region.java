package android.lokemon.game_objects;

import org.mapsforge.android.maps.*;

import android.lokemon.G;
import android.lokemon.G.*;
import com.vividsolutions.jts.geom.*;

public class Region {

	// protected attributes
	protected GeoPoint [] vertices;
	protected OverlayWay regionWay;
	protected Regions region;
	protected int id;
	protected Geometry polygon;
	
	// public static attributes
	public static GeometryFactory geomFactory = new GeometryFactory();
	public static CoordinateSequenceFactory coordFactory = geomFactory.getCoordinateSequenceFactory();
	
	public Region(GeoPoint[] vertices, Regions region, int id)
	{
		this.id = id;
		this.vertices = vertices;
		this.region = region;
		regionWay = new OverlayWay(new GeoPoint[][]{vertices}, G.region_fill[region.ordinal()], G.region_outline[region.ordinal()]);
		
		// create geometric representation
		Coordinate[] coords = new Coordinate[vertices.length];
		for (int i = 0; i < vertices.length;i++)
		{
			GeoPoint p = vertices[i];
			coords[i] = new Coordinate(p.getLatitude(), p.getLongitude());
		}
		LinearRing ring = new LinearRing(coordFactory.create(coords), geomFactory);
		polygon = geomFactory.createPolygon(ring, null);
	}
	
	public Region(Geometry geom, Regions region, int id)
	{
		this.id = id;
		this.region = region;
		this.polygon = geom;
		
		// create GeoPoint vertices for map view
		this.vertices = new GeoPoint[geom.getNumPoints()];
		int index = 0;
		for (Coordinate c:geom.getCoordinates())
		{
			this.vertices[index] = new GeoPoint(c.x,c.y);
			index++;
		}
		regionWay = new OverlayWay(new GeoPoint[][]{vertices}, G.region_fill[region.ordinal()], G.region_outline[region.ordinal()]);
	}
	
	public GeoPoint[] getVertices() {return vertices;}
	public OverlayWay getWay() {return regionWay;}
	public Regions getRegion() {return region;}
	public int getID() {return id;}
	
	public boolean contains(Point point) {return polygon.contains(point);}
}
