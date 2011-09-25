package android.lokemon.game_objects;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.OverlayItem;

import android.location.Location;

public class MapItem implements Comparable<MapItem>{

	// protected attributes
	protected GeoPoint location;
	protected OverlayItem map_marker;
	protected OverlayItem map_shadow;
	protected Location location_android;
	protected Integer id;
	
	public MapItem(GeoPoint location, Integer id)
	{
		this.id = id;
		this.location = location;
		map_marker = new OverlayItem(location,"","");
		map_shadow = new OverlayItem(location,"","");
		if (location != null)
		{
			location_android = new Location("");
			location_android.setLatitude(location.getLatitude());
			location_android.setLongitude(location.getLongitude());
		}
	}
	
	public void updateLocation(Location location)
	{
		this.location_android = location;
		this.location = new GeoPoint(location.getLatitude(),location.getLongitude());
		map_marker.setPoint(this.location);
		map_shadow.setPoint(this.location);
	}
	
	public GeoPoint getLocation() {return location;}
	public Location getAndroidLocation() {return location_android;}
	public OverlayItem getMarker() {return map_marker;}
	public OverlayItem getShadow() {return map_shadow;}
	public Integer getID() {return id;}

	public int compareTo(MapItem obj) {
		return this.id - obj.id;
	}
}
