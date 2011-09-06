package android.lokemon;

import java.io.*;
import java.util.ArrayList;
import org.json.*;

import android.content.*;
import android.util.Log;
import android.app.Activity;

// class encapsulates player attributes - follows singleton pattern
public class Trainer {
	
	ArrayList<Pokemon> pokemon;
	public String nickname;
	
	
	public Trainer(String nick, int startPokemon)
	{
		this.nickname = nick;
		pokemon = new ArrayList<Pokemon>();
		pokemon.add(new Pokemon(startPokemon, 5));
		pokemon.add(new Pokemon(3, 5));
		pokemon.add(new Pokemon(6, 5));
		G.player = this;
	}
	
	public Trainer(String nick, ArrayList<Pokemon> pokemon)
	{
		this.nickname = nick;
		this.pokemon = pokemon;
		G.player = this;
	}
	
	public static void saveTrainer(Activity current)
	{
		try
		{
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(current.openFileOutput("save_data", Context.MODE_PRIVATE)));
			String str_out = "{\"nick\":\""+ G.player.nickname + "\",\"pokemon\":[";
			for (Pokemon p:G.player.pokemon) str_out += p.getJSON() + ",";
			str_out = str_out.substring(0, str_out.length()-1);
			str_out += "]}\n";
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
			new Trainer(object.getString("nick"),pokes);
			Log.i("Data load", "Trainer data loaded");
		}
		catch (Exception e) {Log.e("Data load", e.getMessage());}
	}
}
