package android.lokemon;

public class PokeBall extends Item {

	public PokeBall(int count) 
	{
		super("Poké Ball (empty)", count, 3, "Select an empty Poké Ball to capture a wild Pokémon.");
		this.spriteID = R.drawable.pokeball;
	}
	
	public PokeBall() {this(0);}
}
