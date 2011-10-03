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
	public int ordinal;
	public Types type;
	
	// modifiers
	
	public ElemType(String name, int colour_id, int ordinal)
	{
		this.name = name;
		this.colour_id = colour_id;
		this.ordinal = ordinal;
		this.type = Types.values()[ordinal]; 
	}
	
	public static void loadTypes(String typeJSON) throws JSONException
	{
		JSONArray array = (JSONArray)new JSONTokener(typeJSON).nextValue();
		G.types = new ElemType[array.length()];
		G.type_modifiers = new float[array.length()][array.length()];
		for (int i = 0; i < array.length(); i++)
		{
			JSONObject object = array.getJSONObject(i);
			JSONArray col = object.getJSONArray("colour");
			String n = object.getString("name");
			JSONArray eff = object.getJSONArray("effectiveness");
			int ordinal = Types.valueOf(n).ordinal();
			
			// read in type effectiveness modifiers
			for (int it = 0; it < array.length(); it++)
				G.type_modifiers[ordinal][it] = (float)eff.getDouble(it);
			
			G.types[ordinal] = new ElemType(n, Color.argb(192, col.getInt(0), col.getInt(1), col.getInt(2)),ordinal);
		}
	}
}
