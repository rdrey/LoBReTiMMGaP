package android.lokemon.game_objects;

import android.lokemon.R;

public class PokeBall extends BagItem {

	public PokeBall(int count) 
	{
		super("Poké Ball (empty)", count, 3, "Select an empty Poké Ball to capture a wild Pokémon.");
		this.spriteID = R.drawable.pokeball;
	}
	
	public PokeBall() {this(0);}

	public int getIndex() {return 0;}
}
