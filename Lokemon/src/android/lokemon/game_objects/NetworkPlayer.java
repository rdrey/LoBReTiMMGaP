package android.lokemon.game_objects;

import org.mapsforge.android.maps.*;

import android.graphics.drawable.BitmapDrawable;
import android.lokemon.G;
import android.lokemon.G.Gender;
import android.lokemon.G.PlayerState;

public class NetworkPlayer extends MapItem{
	
	// public attributes
	public Gender gender;
	public String nickname;
	public int id;
	
	// state changes the map icon so cannot be set publicly
	private PlayerState playerState;
	
	public NetworkPlayer(int id, String nick, Gender gender, GeoPoint location)
	{
		super(location);
		this.id = id;
		this.nickname = nick;
		this.gender = gender;
		map_marker.setTitle(nick);
		setPlayerState(PlayerState.AVAILABLE);
	}
	
	public NetworkPlayer(int id, String nick, G.Gender gender) {this(id,nick,gender,null);}
	
	public void setPlayerState(PlayerState state) 
	{
		this.playerState = state;
		if (state == PlayerState.BUSY)
			map_marker.setMarker(G.player_marker_busy);
		else if (state == PlayerState.AVAILABLE)
			map_marker.setMarker(G.player_marker_available);
	}
	public PlayerState getPlayerState() {return playerState;}
}
