package android.lokemon.game_objects;

import android.graphics.Color;
import android.lokemon.G;
import android.lokemon.G.Types;
import android.util.Log;

import org.json.*;

public class ElemType {
	
	// basics
	public String name;
	public int colour_id;
	
	// modifiers
	
	public ElemType(String name, int colour_id)
	{
		this.name = name;
		this.colour_id = colour_id;
	}
	
	public static void loadTypes(String typeJSON) throws JSONException
	{
		JSONArray array = (JSONArray)new JSONTokener(typeJSON).nextValue();
		G.types = new ElemType[array.length()];
		for (int i = 0; i < array.length(); i++)
		{
			JSONObject object = array.getJSONObject(i);
			JSONArray col = object.getJSONArray("colour");
			String n = object.getString("name");
			G.types[Types.valueOf(n).ordinal()] = new ElemType(n, Color.argb(192, col.getInt(0), col.getInt(1), col.getInt(2)));
		}
	}
}
