package android.lokemon;

import android.lokemon.G.Potions;

public class Potion extends Item {
	
	private Potions type;
	
	public Potion(int count, Potions type)
	{
		super(getTypeName(type), count, 6, getTypeDescription(type));
		this.type = type;
	}
	
	public Potion(Potions type) {this(0,type);}
	
	public Potions getType() {return type;}
	
	private static String getTypeDescription(Potions type)
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
	
	private static String getTypeName(Potions type)
	{
		switch (type)
		{
		case HP:
			return "Fully restores your active Pokémon's HP.";
		case ATTACK:
			return "Increases your active Pokémon's Attack by 1 stage (can have a cumulative effect).";
		case DEFENSE:
			return "Increases your active Pokémon's Defense by 1 stage (can have a cumulative effect).";
		case SPECIAL:
			return "Increases your active Pokémon's Special by 1 stage (can have a cumulative effect).";
		case SPEED:
			return "Increases your active Pokémon's Speed by 1 stage (can have a cumulative effect).";
		default:
			return "";
		}
	}
}
