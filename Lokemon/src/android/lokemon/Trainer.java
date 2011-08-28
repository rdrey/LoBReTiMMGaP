package android.lokemon;

import android.content.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.app.Activity;

// class encapsulates player attributes - follows singleton pattern
public class Trainer {

	public static Trainer player = null;
	
	public int [] pokemon;
	public int pokemonCount;
	public String nickname;
	
	public Trainer(String nick, int startPokemon)
	{
		this.nickname = nick;
		pokemon = new int[6];
		pokemon[0] = startPokemon;
		pokemonCount = 1;
		player = this;
	}
	
	public static void saveTrainer(Activity current)
	{
		if (Trainer.player != null)
		{
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(current.getBaseContext()).edit();
			editor.putString("nickname", Trainer.player.nickname);
			editor.putInt("poke1", Trainer.player.pokemon[0]);
			editor.commit();
			Log.i("Save", "Trainer data saved");
		}
	}
	
	public static void loadTrainer(Activity current)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(current.getBaseContext());
		String nick = prefs.getString("nickname", null);
		int poke1 = prefs.getInt("poke1", -1);
		if (nick != null)
		{
			new Trainer(nick, poke1);
			Log.i("Load", "Trainer data loaded");
		}
	}
}
