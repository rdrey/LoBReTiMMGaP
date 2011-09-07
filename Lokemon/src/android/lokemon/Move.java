package android.lokemon;

import org.json.*;

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
	
	public Move(JSONObject object) throws JSONException
	{
		index = object.getInt("index");
		category = Util.getIntFromJSON(object, "category");
		if (category == -1) category = 2;
		else
		{
			name = object.getString("name");
			type = G.types[G.Types.valueOf(object.getString("type")).ordinal()];
			pp = object.getInt("pp");
			description = object.getString("desc");
			if (category == 0)
			{
				power = object.getInt("power");
				accuracy = object.getInt("accuracy");
				critical = Util.getBoolFromJSON(object, "critical");
			}
			else
			{
				// load moves that apply status effects here
			}
		}
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
