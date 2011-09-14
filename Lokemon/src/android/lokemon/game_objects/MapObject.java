package android.lokemon.game_objects;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.OverlayItem;

import android.lokemon.G;
import android.lokemon.G.MapObjectState;

public class MapObject {

	// protected attributes
	protected GeoPoint location;
	protected OverlayItem map_marker;
	protected OverlayItem map_shadow;
	protected MapObjectState state;
	
	public MapObject(GeoPoint location)
	{
		this.location = location;
		map_marker = new OverlayItem(location,"","");
		map_shadow = new OverlayItem(location,"","");
		state = MapObjectState.NEW;
	}
	
	public void updateLocation(GeoPoint location)
	{
		this.location = location;
		map_marker.setPoint(location);
	}
	public GeoPoint getLocation() {return location;}
	public OverlayItem getMarker() {return map_marker;}
	public OverlayItem getShadow() {return map_shadow;}
	public MapObjectState getState() {return state;}
	public void setState(MapObjectState state) {this.state = state;} 
}
