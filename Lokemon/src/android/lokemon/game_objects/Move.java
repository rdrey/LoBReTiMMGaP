package android.lokemon.game_objects;

import org.json.*;

import android.lokemon.G;
import android.lokemon.Util;
import android.lokemon.G.Types;

public class Move {
	
	public int index;
	public String name;
	public ElemType type;
	public int pp;
	public int power;
	public int accuracy;
	public String description;
	public boolean critical;
	public int category; // 0: basic, 1: intermediate, 2:complex (only 0 is implemented for now)
	public int [] stages;
	public boolean special; // true if it is water/grass/fire/electric/psychic
	
	public Move(JSONObject object) throws JSONException
	{
		index = object.getInt("index");
		name = object.getString("name");
		try 
		{
			description = object.getString("desc");
			category = 0;
			type = G.types[G.Types.valueOf(object.getString("type")).ordinal()];
			if (type.type == Types.grass || type.type == Types.water || type.type == Types.fire || type.type == Types.electric ||type.type == Types.psychic)
				special = true;
			else 
				special = false;
			pp = object.getInt("pp");
			power = Util.getIntFromJSON(object,  "power");
			critical = Util.getBoolFromJSON(object, "critical");
			accuracy = object.getInt("accuracy");
			stages = new int[5];
			stages[0] = Util.getIntFromJSON(object, "stage_hp");
			stages[1] = Util.getIntFromJSON(object, "stage_attack");
			stages[2] = Util.getIntFromJSON(object, "stage_defense");
			stages[3] = Util.getIntFromJSON(object, "stage_special");
			stages[4] = Util.getIntFromJSON(object, "stage_speed");
			
			// we are not implementing health stages for now
			if (stages[0] != 0)
				category = 2;
		}
		catch (Exception e){category = 2;}
	}
	
	public static void loadMoves(String moveJSON) throws JSONException
	{
		JSONArray array = (JSONArray)new JSONTokener(moveJSON).nextValue();
		G.moves = new Move[array.length()];
		for (int i = 0; i < array.length(); i++)
		{
			JSONObject obj = array.getJSONObject(i);
			Move m = new Move(obj);
			G.moves[m.index] = m;
		}
	}
}
