package android.lokemon.game_objects;

import java.io.*;
import java.util.ArrayList;
import org.json.*;

import android.content.*;
import android.lokemon.G;
import android.lokemon.G.Potions;
import android.util.Log;
import android.app.Activity;
import android.app.AlertDialog;

// class encapsulates player attributes - follows singleton pattern
public class Trainer {
	
	public ArrayList<Pokemon> pokemon;
	public String nickname;
	public BagItem [] items;
	
	
	public Trainer(String nick, int startPokemon)
	{
		this.nickname = nick;
		pokemon = new ArrayList<Pokemon>();
		pokemon.add(new Pokemon(startPokemon, 5));
		pokemon.add(new Pokemon(3, 5));
		pokemon.add(new Pokemon(6, 5));
		items = new BagItem[6];
		items[0] = new PokeBall();
		items[1] = new Potion(Potions.HP);
		items[2] = new Potion(Potions.ATTACK);
		items[3] = new Potion(Potions.DEFENSE);
		items[4] = new Potion(Potions.SPECIAL);
		items[5] = new Potion(Potions.SPEED);
		// give a new player a starting gift
		try
		{
			items[0].increment();
			items[0].increment();
			items[1].increment();
		}
		catch(BagItem.MaxItemCountException e) {Log.e("Player creation", e.getMessage());}
		G.player = this;
	}
	
	public Trainer(String nick, ArrayList<Pokemon> pokemon, int [] itemCount)
	{
		this.nickname = nick;
		this.pokemon = pokemon;
		items = new BagItem[6];
		items[0] = new PokeBall(itemCount[0]);
		items[1] = new Potion(Potions.HP,itemCount[1]);
		items[2] = new Potion(Potions.ATTACK,itemCount[2]);
		items[3] = new Potion(Potions.DEFENSE,itemCount[3]);
		items[4] = new Potion(Potions.SPECIAL,itemCount[4]);
		items[5] = new Potion(Potions.SPEED,itemCount[5]);
		G.player = this;
	}
	
	public static void saveTrainer(Activity current)
	{
		try
		{
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(current.openFileOutput("save_data", Context.MODE_PRIVATE)));
			String str_out = "{\"nick\":\""+ G.player.nickname + "\",\"items\":[";
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
			JSONObject object = (JSONObject)new JSONTokener(input.readLine()).nextValue();
			JSONArray array = object.getJSONArray("pokemon");
			ArrayList<Pokemon> pokes = new ArrayList<Pokemon>();
			for (int i = 0; i < array.length(); i++)
				pokes.add(new Pokemon(array.getJSONObject(i)));
			array = object.getJSONArray("items");
			int [] count = new int[array.length()];
			for (int i = 0; i < array.length(); i++)
				count[i] = array.getInt(i);
			new Trainer(object.getString("nick"),pokes,count);
			Log.i("Data load", "Trainer data loaded");
		}
		catch (Exception e) {Log.e("Data load", e.getMessage());}
	}
}
