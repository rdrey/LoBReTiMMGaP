package android.lokemon;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.*;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.location.Location;
import android.lokemon.G.Action;
import android.lokemon.G.PlayerState;
import android.lokemon.G.Potions;
import android.lokemon.G.Regions;
import android.lokemon.game_objects.*;
import android.lokemon.screens.MapScreen;
import android.util.Log;
import android.app.Activity;
import android.lbg.*;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import org.mapsforge.android.maps.GeoPoint;

import com.Lobretimgap.NetworkClient.*;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;
import networkTransferObjects.*;

public class Game implements LBGLocationAdapter.LocationListener, Handler.Callback{	
	
	// player list
	private List<NetworkPlayer> players;
	// item list
	private List<WorldPotion> items;
	// region list
	private List<Region> regions;

	// testing threads for generating and removing players
	private Timer add_players_timer;
	private Timer remove_players_timer;
	
	// a reference to the screen that displays the game world
	private MapScreen display;
	
	// interaction variables
	private NetworkPlayer selectedPlayer;
	private WorldPotion selectedItem;
	
	// network objects
	private boolean networkReqLock;
	private boolean networkConnected;
	private boolean networkBound;
	private NetworkComBinder networkBinder;
	private final Messenger networkEventMessenger;
	
	public Game(MapScreen display)
	{
		G.game = this;
		
		this.display = display;
		
		// create player list
		players = Collections.synchronizedList(new ArrayList<NetworkPlayer>());
		
		// create item list
		items = Collections.synchronizedList(new ArrayList<WorldPotion>());
		
		// create region list
		regions = new LinkedList<Region>();
		// create testing region
		GeoPoint [] points = {new GeoPoint(-33.957411, 18.460988),
				new GeoPoint(-33.95792, 18.460888),
				new GeoPoint(-33.958005, 18.461639),
				new GeoPoint(-33.957511, 18.461701),
				new GeoPoint(-33.957411, 18.460988)};
		regions.add(new Region(points,Regions.ROUGH_TERRAIN));
		
		display.addRegions(regions);
		
		// start testing threads 
		add_players_timer = new Timer();
		remove_players_timer = new Timer();
		add_players_timer.schedule(new PlayerGeneration(), (int)(Math.random()*1000));
		remove_players_timer.schedule(new PlayerRemoval(), (int)(Math.random()*1000));
		
		// network setup
		networkReqLock = false;
		networkEventMessenger = new Messenger(new Handler(this));
	}
	
	/*
	 * Methods related to adding, removing and updating network players and items
	 */
	
	// addition method for players
	private synchronized void addPlayer(NetworkPlayer player)
	{
		players.add(player);
		display.addPlayer(player);
	}
	
	// removal method for players
	private synchronized void removePlayer(NetworkPlayer player)
	{
		players.remove(player);
		display.removePlayer(player);
		if (selectedPlayer != null && player.id == selectedPlayer.id)
			selectedPlayer = null;
	}
	
	// adds it to the main item list and adds it to a new item list
	private synchronized void addItem(WorldPotion item)
	{
		items.add(item);
		display.addItem(item);
	}
	
	// removes it from the main item list and adds it to an old item list
	private synchronized void removeItem(WorldPotion item)
	{
		items.remove(item);
		display.removeItem(item);
	}
	
	/*
	 * Methods related to initiating battles
	 */
	
	public synchronized void requestItem(int itemIndex)
	{
		selectedItem = items.get(itemIndex);
		if (G.player.getDistanceFrom(selectedItem.getAndroidLocation()) < 20)
		{
			BagItem item = G.player.items[selectedItem.potionType.ordinal()+1];
			if (item.atMax())
				display.showToast("You have the max no. of " + item.getName() + "s");
			else
			{
				// !!!send item request!!!
				item.increment();
				display.showToast("You have picked up a " + item.getName());
				removeItem(selectedItem);
				selectedItem = null;
			}
		}
		else
			display.showToast("Item is too far away");
	}
	
	/*
	 * Methods related to initiating battles
	 */
	
	public synchronized void requestPlayer(int playerIndex)
	{
		selectedPlayer = players.get(playerIndex);	
		if (G.player.getDistanceFrom(selectedPlayer.getAndroidLocation()) < 20)
		{
			if (selectedPlayer.getPlayerState() == PlayerState.BUSY)
				display.showToast("Player is engaged in battle");
			else
			{
				networkReqLock = true;
				display.showBattleOutgoingDialog(selectedPlayer.nickname);
			}
		}
		else
			display.showToast("Player is too far away");
	}
	
	public synchronized void requestBattle()
	{
		if (selectedPlayer == null)
			display.showToast("Player is no longer online");
		else
		{
			display.showProgressDialog("Waiting for player response...");
			// !!!send player request!!!
			// getBattleInitiationMessage(Action.REQUEST_BATTLE)
			
		}
	}
	
	public synchronized void rejectBattle(boolean sendResponse)
	{
		networkReqLock = false;
		if (sendResponse)
		{
			// !!!send rejection message (Action.REJECT_BATTLE)!!!
		}
	}
	
	public synchronized void acceptBattle()
	{
		// !!!send player acceptance!!!
		// getBattleInitiationMessage(Action.ACCEPT_BATTLE)
		initiateBattle();
	}
	
	public void initiateBattle()
	{
		// !!!send busy status update!!!
		display.switchToBattle();
		networkReqLock = false;
	}
	
	private NetworkMessage getBattleInitiationMessage(Action action)
	{
		// data in request: action, id, nick, gender, base, hp, attack, defense, speed, special, level
		NetworkMessageMedium msg = new NetworkMessageMedium(action.toString());
		Pokemon first = G.player.pokemon.get(0);
		
		msg.integers.add(action.ordinal());
		msg.integers.add(G.player.id);
		msg.integers.add(G.player.gender.ordinal());
		msg.integers.add(first.index);
		msg.integers.add(first.getHP());
		msg.integers.add(first.getAttack());
		msg.integers.add(first.getDefense());
		msg.integers.add(first.getSpeed());
		msg.integers.add(first.getSpecial());
		msg.integers.add(first.getLevel());
		
		msg.strings.add(G.player.nickname);
		
		return msg;
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
	
	// adds a player & item to the game with a 50% chance
	private class PlayerGeneration extends TimerTask
	{
		public void run()
		{
			if (Math.random() < 0.5)
			{
				double lon = Math.random() * -0.005 - 33.955;
				double lat = Math.random() * 0.0008 + 18.4606;
				NetworkPlayer p = new NetworkPlayer(-1,"Test",G.Gender.FEMALE,new GeoPoint(lon,lat));
				addPlayer(p);
				Log.i("Players", "Player added (total: " + players.size() + ")");
			}
			if (Math.random() < 0.5)
			{
				double lon = Math.random() * -0.005 - 33.955;
				double lat = Math.random() * 0.0008 + 18.4606;
				addItem(new WorldPotion(Potions.values()[(int)(Math.random()*5)],new GeoPoint(lon,lat)));
				Log.i("Items", "Item added (total: " + items.size() + ")");
			}
			add_players_timer.schedule(new PlayerGeneration(), (int)(Math.random()*5000));
		}
	}
	
	// removes a random number of players & items from the game
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
				{
					players.get((int)(Math.random()*players.size())).setPlayerState(PlayerState.BUSY);
				}
			}
			numToBeRemoved = (int)(items.size() * Math.random());
			for (int i = 0; i < numToBeRemoved; i++)
				removeItem(items.get((int)(Math.random()*items.size())));
			Log.i("Players", numToBeRemoved + " players removed (total: " + players.size() + ")");
			Log.i("Items", numToBeRemoved + " items removed (total: " + items.size() + ")");
			remove_players_timer.schedule(new PlayerRemoval(), (int)(Math.random()*5000));
		}
	}
	
	/*
	 * Location listener methods
	 */
	
	public void onLocationChanged(Location location) 
	{
		display.updateLocation(location);
		// !!!send location update to server!!!
		NetworkMessageMedium msg = new NetworkMessageMedium("LocationUpdate");
		msg.doubles.add(location.getLatitude());
		msg.doubles.add(location.getLongitude());
		networkBinder.sendGameUpdate(msg);
		Log.i("Location", "New location received");
	}

	public void onLocationError(int errorCode) 
	{
		// do nothing for now
	}
	
	/*
	 * Network event handler and connection setup
	 */
	
	public void createConnection(Activity current)
	{
		// bind network component
		Intent intent = new Intent(current, NetworkComService.class);
		ServiceConnection ser = new ServiceConnection() {
			
			public void onServiceDisconnected(ComponentName name) 
			{
				networkBound = false;
				Log.i(NetworkVariables.TAG, "Service disconnected");
				display.showToast("Disconnected from service");
			}
			
			public void onServiceConnected(ComponentName name, IBinder service) 
			{
				// get an instance of the binder for the service
				networkBinder = (NetworkComBinder)service;
				networkBinder.registerMessenger(networkEventMessenger);
				networkBinder.ConnectToServer();
				networkBound = true;
				display.showToast("Connected to service");
			}
		};
		current.bindService(intent, ser, Context.BIND_AUTO_CREATE);
	}
	
	public boolean handleMessage(Message msg) 
	{
		switch (NetworkComBinder.EventType.values()[msg.what])
		{
			case CONNECTION_ESTABLISHED:
				display.showToast("Connected to game server");
				networkConnected = true;
				break;	
			case CONNECTION_LOST:
				display.showToast("Connection to game server lost");
				networkConnected = false;
				break;
			case CONNECTION_FAILED:
				display.showToast("Could not connect to game server");
				networkConnected = false;
				break;
			case UPDATE_RECEIVED:		
				break;
		}
		return true;
	}
}
