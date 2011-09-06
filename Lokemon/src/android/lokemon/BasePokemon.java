package android.lokemon;

import org.json.*;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;

public class BasePokemon {
	// Pokemon instance attributes
	public int index;
	public String name;
	public float catchrate;
	public String [] type;
	public int hp;
	public int attack;
	public int defense;
	public int speed;
	public int special;
	public int [] evolution; // 0: index of next evolution, 1: level required for evolution
	public int [] moves;
	public int spriteID;
	public int spriteID_attack;
	
	private BasePokemon(JSONObject object, Activity current) throws JSONException
	{
		index = object.getInt("index");
		name = object.getString("name");
		catchrate = (float)object.getDouble("catchrate");
		JSONArray array = object.getJSONArray("type");
		type = new String[array.length()];
		for (int i = 0; i < type.length; i++) type[i] = array.getString(i);
		hp = object.getInt("hp");
		attack = object.getInt("attack");
		defense = object.getInt("defense");
		speed = object.getInt("speed");
		special = object.getInt("special");
		array = object.getJSONArray("evolution");
		if (array.length() == 0) evolution = null;
		else
		{
			evolution = new int[2];
			evolution[0] = array.getInt(0);
			evolution[1] = array.getInt(1);
		}
		array = object.getJSONArray("moves");
		moves = new int[array.length()];
		for (int i = 0; i < moves.length; i++) moves[i] = array.getInt(i);
		String spriteName = Character.toLowerCase(name.charAt(0)) + name.substring(1);
		try
		{
			spriteID = current.getResources().getIdentifier(spriteName, "drawable", "android.lokemon");
			spriteID_attack = current.getResources().getIdentifier(spriteName + "_attack", "drawable", "android.lokemon");
		}
		catch (Resources.NotFoundException e){ Log.e("loading", e.getMessage()); }
	}
	
	public String getDescription()
	{
		String desc = name + " is a " + (type.length==2?"dual ":"") + Character.toUpperCase(type[0].charAt(0)) + type[0].substring(1);
		if (type.length == 2) desc += "/" + Character.toUpperCase(type[1].charAt(0)) + type[1].substring(1);
		desc += " type Pokémon.";
		if (evolution != null)
		{
			BasePokemon next = G.basePokemon[evolution[0]];
			desc += " It evolves into " + next.name + " at level " + evolution[1];
			if (next.evolution != null) desc += " and " + G.basePokemon[next.evolution[0]].name + " at level " + next.evolution[1];
			desc += ".";
		}
		return desc;
	}
	
	public String getBaseStats()
	{
		String stats = "<b>HP</b>:\t\t" + hp + "<br/>";
		stats += "<b>Attack</b>:\t\t" + attack + "<br/>";
		stats += "<b>Defense</b>:\t\t" + defense + "<br/>";
		stats += "<b>Speed</b>:\t\t" + speed + "<br/>";
		stats += "<b>Special</b>:\t\t" + special;
		return stats;
	}
	
	public static void loadPokemon(String pokeJSON, Activity current) throws JSONException
	{
		JSONArray array = (JSONArray)(new JSONTokener(pokeJSON)).nextValue();
		G.basePokemon = new BasePokemon[array.length()];
		for (int i = 0; i < array.length(); i++)
		{
			JSONObject object = (JSONObject)array.get(i);
			BasePokemon poke = new BasePokemon(object, current); 
			G.basePokemon[poke.index] = poke;
		}
	}
}
