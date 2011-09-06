package android.lokemon;

import java.io.*;
import java.util.*;
import org.json.*;

/*
 * This class contains all variables that need to be accessible throughout the game.
 */
public class G {
	public static BasePokemon [] basePokemon;
	public static Move [] moves;
	public static Trainer player;
	public static Game game;
	public static enum Mode {BATTLE, MAP};
	public static Mode mode;
	public static enum Types {bug, electric, fire, flying, grass, ground, normal, poison, psychic, rock, steel, water};
	public static ElemType [] types;
	public static Battle battle;
	
	// converts a JSONArray to an integer array
	public static int[] getIntArray(JSONArray array) throws JSONException
	{
		int [] copy = new int[array.length()];
		for (int i = 0; i < array.length(); i++)
			copy[i] = array.getInt(i);
		return copy;
	}
	
	// converts an integer array to a JSONArray
	public static JSONArray getIntJSONArray(int [] array)
	{
		Collection<Integer> c = new LinkedList<Integer>();
		for (int i = 0; i < array.length; i++)
			c.add(new Integer(array[i]));
		return new JSONArray(c);
	}
	
	public static String capitalize(String str) {return Character.toUpperCase(str.charAt(0)) + str.substring(1);}
}
