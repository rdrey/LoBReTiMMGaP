package android.lokemon.game_objects;

import android.lokemon.R;

public class PokeBall extends BagItem {

	public PokeBall(int count) 
	{
		super("Pok� Ball (empty)", count, 6, "Select an empty Pok� Ball to capture a wild Pok�mon.");
		this.spriteID = R.drawable.pokeball;
	}
	
	public PokeBall() {this(0);}

	public int getIndex() {return 0;}
}
