package android.lokemon;

import android.content.*;
import android.content.res.*;
import android.lokemon.G.Gender;
import android.lokemon.G.Potions;
import android.lokemon.game_objects.BasePokemon;
import android.lokemon.game_objects.ElemType;
import android.lokemon.game_objects.Move;
import android.lokemon.game_objects.NetworkPlayer;
import android.os.*;
import android.util.Log;
import android.app.Activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.OverlayItem;

//networking components
import com.Lobretimgap.NetworkClient.NetworkComBinder;
import com.Lobretimgap.NetworkClient.NetworkComService;
import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;

import networkTransferObjects.NetworkMessage;

public class Game {	
	
	// player lists
	private ArrayList<NetworkPlayer> players;
	private LinkedList<NetworkPlayer> new_players;
	private LinkedList<NetworkPlayer> old_players;
	
	// item lists
	private ArrayList<Potions> items;
	private LinkedList<Potions> new_items;
	private LinkedList<Potions> old_items;
	
	// testing threads for generating and removing players
	private Timer add_players_timer;
	private Timer remove_players_timer;
	
	private Game()
	{
		G.game = this;
		
		// create player lists
		players = new ArrayList<NetworkPlayer>();
		new_players = new LinkedList<NetworkPlayer>();
		old_players = new LinkedList<NetworkPlayer>();
		
		// create item lists
		items = new ArrayList<Potions>();
		new_items = new LinkedList<Potions>();
		old_items = new LinkedList<Potions>();
		
		// some test players
		addPlayer(new NetworkPlayer(121,"Test1", G.Gender.FEMALE,new GeoPoint(52.52709,13.416012)));
		addPlayer(new NetworkPlayer(435,"Test2", G.Gender.MALE,new GeoPoint(52.527312,13.415609)));     
		addPlayer(new NetworkPlayer(234,"Test3", G.Gender.FEMALE,new GeoPoint(52.527407,13.415114)));
		addPlayer(new NetworkPlayer(287,"Test4", G.Gender.MALE,new GeoPoint(52.327198,13.415969)));
		
		// start testing threads 
		add_players_timer = new Timer();
		remove_players_timer = new Timer();
		add_players_timer.schedule(new PlayerGeneration(), (int)(Math.random()*1000));
		remove_players_timer.schedule(new PlayerRemoval(), (int)(Math.random()*1000));
	}
	
	/*
	 * Methods related to adding, removing and updating network players and items
	 */
	
	// adds it to the main player list and adds it to a new player list
	public void addPlayer(NetworkPlayer player)
	{
		players.add(player);
		new_players.add(player);
	}
	
	// removes it from the main player list and adds it to an old player list
	public void removePlayer(NetworkPlayer player)
	{
		old_players.add(player);
		players.remove(player);
	}
	
	// adds it to the main item list and adds it to a new item list
	public void addItem(Potions item)
	{
		items.add(item);
		new_items.add(item);
	}
	
	// removes it from the main item list and adds it to an old item list
	public void removeItem(Potions item)
	{
		old_items.add(item);
		items.remove(item);
	}
	
	// this method is used by the map screen to update its overlays
	public ArrayList<NetworkPlayer> getAllPlayers() {return players;}
	// this method is used by the map screen to update its overlays
	public LinkedList<NetworkPlayer> getNewPlayers() {return new_players;}
	// this method is used by the map screen to update its overlays
	public LinkedList<NetworkPlayer> getOldPlayers() {return old_players;}
	// this method is used by the map screen to update its overlays
	public ArrayList<Potions> getAllItems() {return items;}
	// this method is used by the map screen to update its overlays
	public LinkedList<Potions> getNewItems() {return new_items;}
	// this method is used by the map screen to update its overlays
	public LinkedList<Potions> getOldItems() {return old_items;}
	
	/*
	 * Methods related to initiating battles
	 */
	
	public void requestBattle(int playerIndex)
	{
		
	}
	
	/*
	 * Methods related to saving and loading game state
	 */
	
	public static void loadGameData(Activity current)
	{
		new Game();
		try
		{
			AssetManager assetManager = current.getAssets();
			ElemType.loadTypes(Game.readFile(assetManager.open("types.json")));
			Log.i("Data load", "Types loaded");
			Move.loadMoves(Game.readFile(assetManager.open("moves.json")));
			Log.i("Data load", "Moves loaded");
			BasePokemon.loadPokemon(Game.readFile(assetManager.open("base_pokemon.json")), current);
			Log.i("Data load", "Pokemon loaded");
			//assetManager.close(); // started causing a RuntimeException for no apparent reason
			
			// load map markers
			G.player_marker_available = current.getResources().getDrawable(R.drawable.marker_available);
			G.player_marker_busy = current.getResources().getDrawable(R.drawable.marker_busy);
		}
		catch (Exception e) {Log.e("Data load", e.getMessage());}
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
	
	/*
	 * TimerTask classes to generate/remove players (only used for testing)
	 */
	
	// adds a player to the game with a 50% chance
	private class PlayerGeneration extends TimerTask
	{
		public void run()
		{
			if (Math.random() < 0.5)
			{
				double lon = Math.random() * 0.0004 + 52.527;
				double lat = Math.random() * 0.001 + 13.415;
				addPlayer(new NetworkPlayer(-1,"Test",G.Gender.FEMALE,new GeoPoint(lon,lat)));
				Log.i("Players", "Player added (total: " + players.size() + ")");
			}
			add_players_timer.schedule(new PlayerGeneration(), (int)(Math.random()*1000));
		}
	}
	
	// removes a random number of players from the game, proportional to the total number of players
	private class PlayerRemoval extends TimerTask
	{
		public void run()
		{
			int numToBeRemoved = (int)(players.size() * Math.random());
			for (int i = 0; i < numToBeRemoved; i++)
				removePlayer(players.get((int)(Math.random()*players.size())));
			Log.i("Players", numToBeRemoved + " players removed (total: " + players.size() + ")");
			remove_players_timer.schedule(new PlayerRemoval(), (int)(Math.random()*1000));
		}
	}
}
