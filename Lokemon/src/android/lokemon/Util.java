package android.lokemon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
		catch (JSONException e) {return 0;}
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
	
	public static Location fromSerialLocation(networkTransferObjects.UtilityObjects.Location loc)
	{
		Location loc2 = new Location("");
		loc2.setLatitude(loc.getX());
		loc2.setLongitude(loc.getY());
		return loc2;
	}

	// read all from a file
	public static String readFile(InputStream file) throws IOException
	{
		BufferedReader input = new BufferedReader(new InputStreamReader(file));
		String str = "";
		String in = input.readLine();
		while (in != null)
		{
			str += in;
			in = input.readLine();
		}
		input.close();
		return str;
	}

	public static void copyFile(InputStream in, OutputStream out) throws IOException
	{
		byte [] buffer = new byte[1024];
		int size = 0;
		while ((size = in.read(buffer)) != -1)
			out.write(buffer, 0, size);
		out.flush();
		out.close();
		in.close();
	}
}
