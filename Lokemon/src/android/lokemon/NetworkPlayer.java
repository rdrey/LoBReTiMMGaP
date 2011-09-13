package android.lokemon;

import org.mapsforge.android.maps.*;

public class NetworkPlayer {
	public static enum Gender {FEMALE, MALE};
	
	// public attributes
	public Gender gender;
	public String nickname;
	public int id;
	public boolean busy;
	
	// private attributes
	private GeoPoint location;
	private OverlayCircle map_marker;
	private int index;
	
	public NetworkPlayer(int id, String nick, Gender gender, GeoPoint location)
	{
		this.id = id;
		this.nickname = nick;
		this.gender = gender;
		this.location = location;
		this.index = -1;
		this.busy = false;
		map_marker = new OverlayCircle(location,2,nick);
	}
	
	public NetworkPlayer(int id, String nick, Gender gender) {this(id,nick,gender,null);}
	
	public void updateLocation(GeoPoint location)
	{
		this.location = location;
		map_marker.setCircleData(location, 2);
	}
	
	public GeoPoint getLocation() {return location;}
	public int getIndex() {return index;}
}
