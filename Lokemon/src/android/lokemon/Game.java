package android.lokemon;

import android.content.res.*;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.lokemon.G.PlayerState;
import android.lokemon.G.Potions;
import android.lokemon.G.Regions;
import android.lokemon.game_objects.BasePokemon;
import android.lokemon.game_objects.ElemType;
import android.lokemon.game_objects.Move;
import android.lokemon.game_objects.NetworkPlayer;
import android.lokemon.game_objects.Region;
import android.util.Log;
import android.app.Activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.mapsforge.android.maps.GeoPoint;

//networking components
import com.Lobretimgap.NetworkClient.NetworkComBinder;
import com.Lobretimgap.NetworkClient.NetworkComService;
import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;

import networkTransferObjects.NetworkMessage;

public class Game {	
	
	// player list
	private List<NetworkPlayer> players;
	private ConcurrentLinkedQueue<NetworkPlayer> old_players;
	private ConcurrentLinkedQueue<NetworkPlayer> new_players;
	// item list
	private List<Potions> items;
	// region list
	private List<Region> regions;

	// testing threads for generating and removing players
	private Timer add_players_timer;
	private Timer remove_players_timer;
	
	private Game()
	{
		G.game = this;
		
		// create player list
		players = Collections.synchronizedList(new ArrayList<NetworkPlayer>());
		old_players = new ConcurrentLinkedQueue<NetworkPlayer>();
		new_players = new ConcurrentLinkedQueue<NetworkPlayer>();
		
		// create item list
		items = Collections.synchronizedList(new ArrayList<Potions>());
		
		// create region list
		regions = new LinkedList<Region>();
		// create testing region
		GeoPoint [] points = {new GeoPoint(-33.957411, 18.460988),
				new GeoPoint(-33.95792, 18.460888),
				new GeoPoint(-33.958005, 18.461639),
				new GeoPoint(-33.957511, 18.461701),
				new GeoPoint(-33.957411, 18.460988)};
		regions.add(new Region(points,Regions.ROUGH_TERRAIN));
		
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
	
	// addition method for players
	public void addPlayer(NetworkPlayer player)
	{
		players.add(player);
		new_players.add(player);
	}
	
	// removal method for players
	public void removePlayer(NetworkPlayer player)
	{
		players.remove(player);
		old_players.add(player);
	}
	
	// adds it to the main item list and adds it to a new item list
	public void addItem(Potions item)
	{
		items.add(item);
	}
	
	// removes it from the main item list and adds it to an old item list
	public void removeItem(Potions item)
	{
		items.remove(item);
	}
	
	// these methods are used by the map screen to update its overlays
	public List<NetworkPlayer> getAllPlayers() {return players;}
	public ConcurrentLinkedQueue<NetworkPlayer> getOldPlayers() {return old_players;}
	public ConcurrentLinkedQueue<NetworkPlayer> getNewPlayers() {return new_players;}
	public List<Potions> getAllItems() {return items;}
	public List<Region> getRegions() {return regions;}
	
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
			
			Resources res = current.getResources();
			
			// load map markers
			G.player_marker_available = res.getDrawable(R.drawable.marker_available);
			G.player_marker_busy = res.getDrawable(R.drawable.marker_busy);
			
			// load region colours and paint objects
			G.region_colours = new int[9];
			G.region_colours[0] = res.getColor(R.color.cave);
			G.region_colours[1] = res.getColor(R.color.forest);
			G.region_colours[2] = res.getColor(R.color.grassland);
			G.region_colours[3] = res.getColor(R.color.mountain);
			G.region_colours[4] = res.getColor(R.color.rough_terrain);
			G.region_colours[5] = res.getColor(R.color.urban);
			G.region_colours[6] = res.getColor(R.color.water_edge);
			G.region_colours[7] = res.getColor(R.color.pokemon_center);
			G.region_colours[8] = res.getColor(R.color.pokemart);
			
			G.region_fill = new Paint[9];
			G.region_outline = new Paint[9];
			for (int i = 0; i < 9; i++)
			{
				// the settings for the fill
				Paint fill = new Paint();
				fill.setColor(G.region_colours[i]);
				fill.setAlpha(128);
				fill.setStyle(Style.FILL);
				fill.setAntiAlias(true);
				G.region_fill[i] = fill;
				
				// the settings for the outline
				Paint outline = new Paint();
				outline.setColor(G.region_colours[i]);
				outline.setStrokeWidth(2);
				outline.setStrokeJoin(Join.ROUND);
				outline.setStyle(Style.STROKE);
				outline.setAntiAlias(true);
				G.region_outline[i] = outline;
			}
		}
		catch (Exception e) {Log.e("Data load", e.toString());}
		new Game();
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
				double lon = Math.random() * -0.005 - 33.955;
				double lat = Math.random() * 0.0008 + 18.4606;
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
			{
				if (Math.random() < 0.5)
					removePlayer(players.get((int)(Math.random()*players.size())));
				else
					players.get((int)(Math.random()*players.size())).setPlayerState(PlayerState.BUSY);
			}
			Log.i("Players", numToBeRemoved + " players removed (total: " + players.size() + ")");
			remove_players_timer.schedule(new PlayerRemoval(), (int)(Math.random()*1000));
		}
	}
}
