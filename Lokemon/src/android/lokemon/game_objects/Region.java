package android.lokemon.game_objects;

import org.mapsforge.android.maps.*;

import android.lokemon.G;
import android.lokemon.G.*;

public class Region {

	// protected attributes
	protected GeoPoint [] vertices;
	protected OverlayWay regionWay;
	protected Regions region;
	protected int id;
	
	public Region(GeoPoint[] vertices, Regions region, int id)
	{
		this.id = id;
		this.vertices = vertices;
		this.region = region;
		regionWay = new OverlayWay(new GeoPoint[][]{vertices}, G.region_fill[region.ordinal()], G.region_outline[region.ordinal()]);
	}
	
	public GeoPoint[] getVertices() {return vertices;}
	public OverlayWay getWay() {return regionWay;}
	public Regions getRegion() {return region;}
	public int getID() {return id;}
}
