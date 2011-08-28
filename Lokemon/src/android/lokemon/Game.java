package android.lokemon;

import android.content.res.*;
import android.util.Log;
import android.app.Activity;
import java.io.*;

public class Game {
	
	public static Game game;
	
	private Game()
	{
		game = this;
	}
	
	public static void loadGameData(Activity current)
	{
		new Game();
		try
		{
			AssetManager assetManager = current.getAssets();
			BufferedReader input = new BufferedReader(new InputStreamReader(assetManager.open("pokemon.json")));
			String str = "";
			String in = input.readLine();
			while (in != null)
			{
				str += in;
				in = input.readLine();
			}
			Pokemon.loadPokemon(str);
			input.close();
			assetManager.close();
		}
		catch (Exception e) {Log.e("Data load", e.getMessage());}
	}
}
