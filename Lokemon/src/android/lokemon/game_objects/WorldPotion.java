package android.lokemon.game_objects;

import org.mapsforge.android.maps.GeoPoint;
import android.lokemon.G.Potions;

public class WorldPotion extends MapItem{
	
	// public attributes
	public Potions potionType;
	
	public WorldPotion(Potions potionType, GeoPoint location, int id)
	{
		super(location, id);
		this.potionType = potionType;
	}
}
