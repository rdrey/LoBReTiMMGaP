package android.lokemon;

import java.util.Collection;
import java.util.LinkedList;
import org.json.*;
import org.mapsforge.android.maps.GeoPoint;

import android.location.Location;

public class Util {

	public static String getStringFromJSON(JSONObject obj, String name)
	{
		try {return obj.getString(name);}
		catch (JSONException e) {return null;}
	}

	public static String capitalize(String str) {return Character.toUpperCase(str.charAt(0)) + str.substring(1);}

	public static int getIntFromJSON(JSONObject obj, String name)
	{
		try {return obj.getInt(name);}
		catch (JSONException e) {return -1;}
	}
	
	public static boolean getBoolFromJSON(JSONObject obj, String name)
	{
		try {return obj.getBoolean(name);}
		catch (JSONException e) {return false;}
	}

	public static int[] getIntArrayFromJSON(JSONObject obj, String name)
	{
		try {return Util.getIntArray(obj.getJSONArray(name));}
		catch (JSONException e) {return null;}
	}

	// converts an integer array to a JSONArray
	public static JSONArray getIntJSONArray(int [] array)
	{
		Collection<Integer> c = new LinkedList<Integer>();
		for (int i = 0; i < array.length; i++)
			c.add(new Integer(array[i]));
		return new JSONArray(c);
	}

	// converts a JSONArray to an integer array
	public static int[] getIntArray(JSONArray array) throws JSONException
	{
		int [] copy = new int[array.length()];
		for (int i = 0; i < array.length(); i++)
			copy[i] = array.getInt(i);
		return copy;
	}

	public static Location fromGeoPoint(GeoPoint point)
	{
		Location loc = new Location("");
		loc.setLatitude(point.getLatitude());
		loc.setLongitude(point.getLongitude());
		return loc;
	}
	
	public static GeoPoint fromLocation(Location loc)
	{
		return new GeoPoint(loc.getLatitude(),loc.getLongitude());
	}
}
