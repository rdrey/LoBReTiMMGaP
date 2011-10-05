package android.lokemon.game_objects;

import java.io.*;
import java.util.ArrayList;
import org.json.*;
import org.mapsforge.android.maps.*;

import android.content.*;
import android.location.Location;
import android.lokemon.G;
import android.lokemon.G.Gender;
import android.lokemon.G.PlayerState;
import android.lokemon.G.Potions;
import android.util.Log;
import android.app.Activity;

// class encapsulates player attributes - follows singleton pattern
public class Trainer {
	
	public ArrayList<Pokemon> pokemon;
	public String nickname;
	public BagItem [] items;
	public OverlayItem aura;
	public OverlayCircle circle;
	public int coins;
	public int id;
	public Gender gender;
	public PlayerState playerState;
	
	private Location location;
	
	public Trainer(String nick, int startPokemon, Gender gender)
	{
		this.nickname = nick;
		this.coins = 3;
		this.gender = gender;
		pokemon = new ArrayList<Pokemon>();
		pokemon.add(new Pokemon(startPokemon, 5));
		items = new BagItem[6];
		items[0] = new PokeBall();
		items[1] = new Potion(Potions.HP);
		items[2] = new Potion(Potions.ATTACK);
		items[3] = new Potion(Potions.DEFENSE);
		items[4] = new Potion(Potions.SPECIAL);
		items[5] = new Potion(Potions.SPEED);
		// give a new player a starting gift
		items[0].increment();
		items[0].increment();
		items[0].increment();
		items[0].increment();
		items[0].increment();
		items[0].increment();
		items[1].increment();
		
		this.aura = new OverlayItem();
		this.circle = new OverlayCircle();
		this.setDefaultLocation();
		this.playerState = PlayerState.AVAILABLE;
		
		G.player = this;
	}
	
	public Trainer(String nick, ArrayList<Pokemon> pokemon, int [] itemCount, int coins, int genderOrdinal)
	{
		this.nickname = nick;
		this.pokemon = pokemon;
		this.coins = coins;
		this.gender = Gender.values()[genderOrdinal];
		items = new BagItem[6];
		items[0] = new PokeBall(itemCount[0]);
		items[1] = new Potion(Potions.HP,itemCount[1]);
		items[2] = new Potion(Potions.ATTACK,itemCount[2]);
		items[3] = new Potion(Potions.DEFENSE,itemCount[3]);
		items[4] = new Potion(Potions.SPECIAL,itemCount[4]);
		items[5] = new Potion(Potions.SPEED,itemCount[5]);
		
		this.aura = new OverlayItem();
		this.circle = new OverlayCircle();
		this.setDefaultLocation();
		this.playerState = PlayerState.AVAILABLE;
		
		G.player = this;
	}
	
	public static void saveTrainer(Activity current)
	{
		try
		{
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(current.openFileOutput("save_data", Context.MODE_PRIVATE)));
			String str_out = "{\"nick\":\""+ G.player.nickname + "\",\"coins\":" + G.player.coins + ",\"gender\":" + G.player.gender.ordinal() + ",\"items\":[";
			for (BagItem i:G.player.items) str_out += i.getCount() + ",";
			str_out = str_out.substring(0, str_out.length()-1) + "],\"pokemon\":[";
			for (Pokemon p:G.player.pokemon) str_out += p.getJSON() + ",";
			str_out = str_out.substring(0, str_out.length()-1) + "]}\n";
			output.write(str_out);
			output.close();
			Log.i("Data save", "Trainer data saved");
		}
		catch (Exception e) {Log.e("Data save", e.getMessage());}
	}
	
	public static void loadTrainer(Activity current)
	{
		try
		{
			BufferedReader input = new BufferedReader(new InputStreamReader(current.openFileInput("save_data")));
			String text = input.readLine();			
			JSONObject object = (JSONObject)new JSONTokener(text).nextValue();
			JSONArray array = object.getJSONArray("pokemon");
			ArrayList<Pokemon> pokes = new ArrayList<Pokemon>();
			for (int i = 0; i < array.length(); i++)
				pokes.add(new Pokemon(array.getJSONObject(i)));
			array = object.getJSONArray("items");
			int [] count = new int[array.length()];
			for (int i = 0; i < array.length(); i++)
				count[i] = array.getInt(i);
			new Trainer(object.getString("nick"),pokes,count,object.getInt("coins"), object.getInt("gender"));
			Log.i("Data load", "Trainer data loaded");
		}
		catch (Exception e) {Log.e("Data load", e.getMessage());}
	}
	
	public float getDistanceFrom(Location loc) {return location.distanceTo(loc);}
	
	public Location getLocation(){return location;}
	public void setLocation(Location loc)
	{
		location = loc;
		GeoPoint mapPoint = new GeoPoint(loc.getLatitude(),loc.getLongitude());
		aura.setPoint(mapPoint);
		circle.setCircleData(mapPoint, 20);
	}
	
	private void setDefaultLocation()
	{
		Location loc = new Location("");
		loc.setLatitude(-33.957657);
		loc.setLongitude(18.46125);
		setLocation(loc);
	}
}
