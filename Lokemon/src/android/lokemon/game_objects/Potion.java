package android.lokemon.game_objects;

import android.lokemon.R;
import android.lokemon.G.Potions;

public class Potion extends BagItem {
	
	private Potions type;
	
	public Potion(Potions type, int count)
	{
		super(getTypeName(type), count, 6, getTypeDescription(type));
		this.type = type;
		this.spriteID = getTypeSprite(type);
	}
	
	public Potion(Potions type) {this(type,0);}
	
	public Potions getType() {return type;}
	
	private static String getTypeName(Potions type)
	{
		switch (type)
		{
		case HP:
			return "Health Potion";
		case ATTACK:
			return "Attack Potion";
		case DEFENSE:
			return "Defense Potion";
		case SPECIAL:
			return "Special Potion";
		case SPEED:
			return "Speed Potion";
		default:
			return "";
		}
	}
	
	private static String getTypeDescription(Potions type)
	{
		switch (type)
		{
		case HP:
			return "Fully restores your active Pokémon's HP.";
		case ATTACK:
			return "Increases your active Pokémon's Attack by 1 stage.";
		case DEFENSE:
			return "Increases your active Pokémon's Defense by 1 stage.";
		case SPECIAL:
			return "Increases your active Pokémon's Special by 1 stage.";
		case SPEED:
			return "Increases your active Pokémon's Speed by 1 stage.";
		default:
			return "";
		}
	}
	
	private static int getTypeSprite(Potions type)
	{
		switch (type)
		{
		case HP:
			return R.drawable.health;
		case ATTACK:
			return R.drawable.attack;
		case DEFENSE:
			return R.drawable.defense;
		case SPECIAL:
			return R.drawable.special;
		case SPEED:
			return R.drawable.speed;
		default:
			return -1;
		}
	}

	public int getIndex() {return type.ordinal()+1;}
}
