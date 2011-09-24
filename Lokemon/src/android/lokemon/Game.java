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
import android.lokemon.G.Gender;
import android.lokemon.G.Mode;
import android.lokemon.G.PlayerState;
import android.lokemon.G.Potions;
import android.lokemon.G.Regions;
import android.lokemon.game_objects.*;
import android.lokemon.screens.MapScreen;
import android.util.Log;
import android.app.Activity;
import android.lbg.*;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.IBinder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

import org.mapsforge.android.maps.GeoPoint;

import com.Lobretimgap.NetworkClient.*;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;
import networkTransferObjects.*;
import networkTransferObjects.Lokemon.*;

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
	private boolean networkReqLock; // when true all battle requests from players are ignored
	private boolean waitingForAccept; // when true all messages other than battle acceptance are ignored
	private boolean networkConnected;
	private boolean networkBound;
	
	// periodically requests game state updates from server
	private Handler networkUpdater;
	private Runnable updater;
	
	private NetworkComBinder networkBinder;
	private final Messenger networkEventMessenger;
	
	public Game(MapScreen display)
	{
		G.game = this;
		
		this.display = display;
		
		// create player list
		players = new LinkedList<NetworkPlayer>();
		
		// create item list
		items = new LinkedList<WorldPotion>();
		
		// create region list
		regions = new LinkedList<Region>();
		// create testing region
		GeoPoint [] points = {new GeoPoint(-33.957411, 18.460988),
				new GeoPoint(-33.95792, 18.460888),
				new GeoPoint(-33.958005, 18.461639),
				new GeoPoint(-33.957511, 18.461701),
				new GeoPoint(-33.957411, 18.460988)};
		regions.add(new Region(points,Regions.ROUGH_TERRAIN,0));
		
		display.addRegions(regions);
		
		// start testing threads 
		/*add_players_timer = new Timer();
		remove_players_timer = new Timer();
		add_players_timer.schedule(new PlayerGeneration(), (int)(Math.random()*1000));
		remove_players_timer.schedule(new PlayerRemoval(), (int)(Math.random()*1000));*/
		
		// network setup
		networkReqLock = false;
		waitingForAccept = false;
		networkEventMessenger = new Messenger(new Handler(this));
		networkUpdater = new Handler();
		updater = new Runnable(){
			public void run() {
				networkBinder.sendGameStateRequest(new NetworkMessage("GetGameObjects"));
				networkBinder.sendGameStateRequest(new NetworkMessage("GetPlayers"));
				networkUpdater.postDelayed(updater, 1000);
				}
		};
	}
	
	/*
	 * Methods related to adding, removing and updating network players and items
	 */
	
	// addition method for players
	private void addPlayer(NetworkPlayer player)
	{
		players.add(player);
		display.addPlayer(player);
	}
	
	// removal method for players
	private void removePlayer(NetworkPlayer player)
	{
		players.remove(player);
		display.removePlayer(player);
		if (selectedPlayer != null && player.id == selectedPlayer.id)
			selectedPlayer = null;
	}
	
	// insert players to keep the list sorted according to id
	private void insertPlayer(NetworkPlayer player, int index)
	{
		players.add(index, player);
		display.addPlayer(player);
	}
	
	// adds it to the main item list and adds it to a new item list
	private void addItem(WorldPotion item)
	{
		items.add(item);
		display.addItem(item);
	}
	
	// removes it from the main item list and adds it to an old item list
	private void removeItem(WorldPotion item)
	{
		items.remove(item);
		display.removeItem(item);
	}
	
	// insert players to keep the list sorted according to id
	private void insertItem(WorldPotion item, int index)
	{
		items.add(index, item);
		display.addItem(item);
	}
	
	/*
	 * Methods related to initiating battles
	 */
	
	public void requestItem(int itemIndex)
	{
		synchronized (items)
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
	}
	
	/*
	 * Methods related to initiating battles
	 */
	
	public void requestPlayer(int playerIndex)
	{
		synchronized (players)
		{
			networkReqLock = true;
			selectedPlayer = players.get(playerIndex);	
			if (G.player.getDistanceFrom(selectedPlayer.getAndroidLocation()) < 20)
			{
				if (selectedPlayer.getPlayerState() == PlayerState.BUSY)
				{
					display.showToast("Player is engaged in battle");
					rejectBattle(false);
				}
				else
					display.showBattleOutgoingDialog(selectedPlayer.nickname);
			}
			else
			{
				display.showToast("Player is too far away");
				rejectBattle(false);
			}
		}
	}
	
	public void requestBattle()
	{
		synchronized(players)
		{
			if (selectedPlayer == null)
				display.showToast("Player is no longer online");
			else
			{
				// send Action.REQUEST_BATTLE
				networkBinder.sendDirectCommunication(getBattleInitiationMessage(Action.REQUEST_BATTLE), selectedPlayer.id);
				display.showProgressDialog("Waiting for player response...");
				waitingForAccept = true;
			}
		}
	}
	
	public void rejectBattle(boolean sendResponse)
	{
		selectedPlayer = null;
		waitingForAccept = false;
		networkReqLock = false;
		if (sendResponse)
		{
			// send Action.REJECT_BATTLE
			networkBinder.sendDirectCommunication(new NetworkMessage(Action.REJECT_BATTLE.toString()), selectedPlayer.id);
		}
	}
	
	public void acceptBattle()
	{
		// send Action.ACCEPT_BATTLE
		networkBinder.sendDirectCommunication(getBattleInitiationMessage(Action.ACCEPT_BATTLE), selectedPlayer.id);
		initiateBattle();
	}
	
	public void initiateBattle()
	{
		// send busy status update
		networkBinder.sendGameUpdate(new NetworkMessage("EnteredBattle"));
		display.switchToBattle();
		networkReqLock = false;
		waitingForAccept = false;
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
			ElemType.loadTypes(Util.readFile(assetManager.open("types.json")));
			Log.i("Data load", "Types loaded");
			Move.loadMoves(Util.readFile(assetManager.open("moves.json")));
			Log.i("Data load", "Moves loaded");
			BasePokemon.loadPokemon(Util.readFile(assetManager.open("base_pokemon.json")), current);
			Log.i("Data load", "Pokemon loaded");
			
			// save map on external storage if necessary/possible
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			{
				Log.i("Data load", "Checking map data");
				File file = new File(G.mapDir);
				file.mkdirs();
				if (!(new File(G.mapDir + G.mapFile)).exists())
					Util.copyFile(assetManager.open(G.mapFile), new FileOutputStream(G.mapDir + G.mapFile));
			}
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
				addItem(new WorldPotion(Potions.values()[(int)(Math.random()*5)],new GeoPoint(lon,lat),-1));
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
	
	public void endGame()
	{
		networkBinder.sendTerminationRequest(new NetworkMessage(""));
	}
	
	public boolean handleMessage(Message msg) 
	{
		Log.e(NetworkVariables.TAG, NetworkComBinder.EventType.values()[msg.what].toString());
		switch (NetworkComBinder.EventType.values()[msg.what])
		{
			case CONNECTION_ESTABLISHED:
			{
				display.showToast("Connected to game server");
				networkConnected = true;
				networkUpdater.postDelayed(updater, 1000);
				break;	
			}
			case CONNECTION_LOST:
			{
				display.showToast("Connection to game server lost");
				networkConnected = false;
				break;
			}
			case CONNECTION_FAILED:
			{
				display.showToast("Could not connect to game server");
				networkConnected = false;
				break;
			}
			case GAMESTATE_RECEIVED:
			{
				NetworkMessageLarge nMsg = (NetworkMessageLarge)((NetworkEvent)msg.obj).getMessage();
				String tag = nMsg.getMessage();
				Log.i(NetworkVariables.TAG, tag);
				if (tag.equals("Response:GetGameObjects"))
				{
					ArrayList<LokemonPotion> ilist = (ArrayList<LokemonPotion>)nMsg.objectDict.get("ItemList");
					if (ilist != null)
					{
						synchronized (items)
						{
							// merging old and new player lists (trying to avoid unnecessary creation of objects)
							Iterator<WorldPotion> i = items.iterator();
							Iterator<LokemonPotion> it = ilist.iterator();
							WorldPotion ni = i.hasNext()?i.next():null;
							LokemonPotion li = it.hasNext()?it.next():null;
							int index = 0;
							while (i.hasNext() && it.hasNext())
							{
								int id = li.getId();
								if (ni.getID() == id)
								{
									ni.updateLocation(Util.fromSerialLocation(li.getPosition()));
									ni = i.next();
									li = it.next();
									index++;
								}
								else if (ni.getID() < id)
								{
									display.removeItem(ni);
									i.remove();
									ni = i.next();
								}
								else
								{
									networkTransferObjects.UtilityObjects.Location loc = li.getPosition();
									insertItem(new WorldPotion(Potions.values()[li.getType().ordinal()],new GeoPoint(loc.getX(),loc.getY()),li.getId()),index);
									li = it.next();
									index++;
								}
							}
							if (ni != null)
							{
								for(;;){
									display.removeItem(ni);
									i.remove();
									if (i.hasNext())
										ni = i.next();
									else break;
								}
							}
							if (li != null)
							{
								for(;;){
									networkTransferObjects.UtilityObjects.Location loc = li.getPosition();
									addItem(new WorldPotion(Potions.values()[li.getType().ordinal()],new GeoPoint(loc.getX(),loc.getY()),li.getId()));
									if (it.hasNext())
										li = it.next();
									else break;
								}
							}
						}
					}
				}
				else if (tag.equals("Response:GetPlayers"))
				{
					ArrayList<LokemonPlayer> plist = (ArrayList<LokemonPlayer>)nMsg.objectDict.get("PlayerList");
					if (plist != null)
					{
						synchronized (players)
						{
							Log.i("Players", plist.size() + " players in area of interest");
							// merging old and new player lists (trying to avoid unnecessary creation of objects)
							Iterator<NetworkPlayer> i = players.iterator();
							Iterator<LokemonPlayer> it = plist.iterator();
							NetworkPlayer np = i.hasNext()?i.next():null;
							LokemonPlayer lp = it.hasNext()?it.next():null;
							int index = 0;
							while (i.hasNext() && it.hasNext())
							{
								int id = lp.getPlayerID();
								if (np.id == id)
								{
									Log.i("Players", "Updating id=" + id);
									np.updateLocation(Util.fromSerialLocation(lp.getPosition()));
									np = i.next();
									lp = it.next();
									index++;
								}
								else if (np.id < id)
								{
									Log.i("Players", "Removing id=" + np.getID());
									display.removePlayer(np);
									i.remove();
									np = i.next();
								}
								else
								{
									Log.i("Players", "Inserting id=" + id);
									networkTransferObjects.UtilityObjects.Location loc = lp.getPosition();
									insertPlayer(new NetworkPlayer(lp.getPlayerID(), lp.getPlayerName(), Gender.values()[lp.getAvatar()], new GeoPoint(loc.getX(),loc.getY())),index);
									lp = it.next();
									index++;
								}
							}
							if (np != null)
							{
								for (;;) {
									display.removePlayer(np);
									i.remove();
									if (i.hasNext())
										np = i.next();
									else break;
								}
							}
							if (lp != null)
							{
								for (;;) {
									Log.i("Players", "Adding id=" + lp.getPlayerID());
									networkTransferObjects.UtilityObjects.Location loc = lp.getPosition();
									addPlayer(new NetworkPlayer(lp.getPlayerID(), lp.getPlayerName(), Gender.values()[lp.getAvatar()], new GeoPoint(loc.getX(),loc.getY())));
									if (it.hasNext())
										lp = it.next();
									else break;
								}
							}
						}
					}
				}
				else
				{
					// some error state
					Log.i("Players", nMsg.getMessage());
				}
				break;
			}
			case UPDATE_RECEIVED:
			{
				break;
			}
			case DIRECT_MESSAGE_RECEIVED:
			{
				NetworkMessage nMsg = (NetworkMessage)((NetworkEvent)msg.obj).getMessage();
				try
				{
					Action action = Action.valueOf(nMsg.getMessage());
					switch(action)
					{
					case REJECT_BATTLE:
						if (waitingForAccept)
						{
							display.cancelProgressDialog();
							display.showToast(selectedPlayer.nickname + " rejected battle request");
							rejectBattle(false);
						}
						break;
					case ACCEPT_BATTLE:
						if (waitingForAccept)
							initiateBattle();						
						break;
					case REQUEST_BATTLE:
						NetworkMessageMedium req = (NetworkMessageMedium)nMsg;
						if (G.mode == Mode.MAP && !networkReqLock)
						{
							networkReqLock = true;
							selectedPlayer = new NetworkPlayer(req.integers.get(1), req.strings.get(0), Gender.values()[req.integers.get(2)], null);
							display.showBattleIncomingDialog(req.strings.get(0));
						}
						else
							networkBinder.sendDirectCommunication(new NetworkMessage(Action.REJECT_ALL.toString()), req.integers.get(1));
						break;
					case REJECT_ALL:
						if (waitingForAccept)
						{
							display.cancelProgressDialog();
							display.showBattleIncomingDialog(selectedPlayer.nickname + " is currently busy");
							rejectBattle(false);
						}
						break;
					}
				}
				catch (IllegalArgumentException e)
				{
					
				}
				break;
			}
		}
		return true;
	}
}
