package android.lokemon.game_objects;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.OverlayItem;

public class MapItem {

	// protected attributes
	protected GeoPoint location;
	protected OverlayItem map_marker;
	protected OverlayItem map_shadow;
	
	public MapItem(GeoPoint location)
	{
		this.location = location;
		map_marker = new OverlayItem(location,"","");
		map_shadow = new OverlayItem(location,"","");
	}
	
	public void updateLocation(GeoPoint location)
	{
		this.location = location;
		map_marker.setPoint(location);
	}
	public GeoPoint getLocation() {return location;}
	public OverlayItem getMarker() {return map_marker;}
	public OverlayItem getShadow() {return map_shadow;}
}
