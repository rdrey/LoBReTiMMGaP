package android.lokemon;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.*;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.location.Location;
import android.lokemon.G.Action;
import android.lokemon.G.BattleMove;
import android.lokemon.G.BattleType;
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

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Coordinate;

import com.Lobretimgap.NetworkClient.*;
import com.Lobretimgap.NetworkClient.Events.DirectCommunicationEvent;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;
import networkTransferObjects.*;
import networkTransferObjects.Lokemon.*;

public class Game implements LBGLocationAdapter.LocationListener, Handler.Callback{	
	
	// player list
	private List<NetworkPlayer> players;
	// item list
	private List<WorldPotion> items;
	private LinkedList<Integer> ignored_item_ids;
	// region list
	private List<Region> regions;
	private Regions currentRegion;
	
	// a reference to the screen that displays the game world
	private MapScreen display;
	
	// interaction variables
	private NetworkPlayer selectedPlayer;
	private WorldPotion selectedItem;
	private boolean foundPokeInRegion;
	
	// battle variabes
	private int randomSeed;
	public int opponentID;
	private NetworkMessageMedium battleInitMessage;
	public Pokemon genPokemon; // easiest to have it public
	
	// network objects
	private boolean networkReqLock; // when true all battle requests from players are ignored
	private boolean waitingForAccept; // when true all messages other than battle acceptance are ignored
	private boolean waitingForItemAccept;
	private boolean busyConnecting;
	private boolean networkBound;
	private boolean busyBinding;
	
	// we don't want to overwhelm the client with responses if there is a backlog
	private boolean waitingForPlayers;
	private boolean waitingForItems;
	
	// periodically requests game state updates from server
	private Handler networkUpdater;
	private Runnable updater;
	private double elapsedTime;
	
	private NetworkComBinder networkBinder;
	private final Messenger networkEventMessenger;
	
	public Game(MapScreen display)
	{
		G.game = this;
		
		this.display = display;
		
		// check if the player has battle-ready Pokemon
		boolean hasPoke = false;
		for (Pokemon p:G.player.pokemon)
		{
			if (p.getHP() > 0)
			{
				hasPoke = true;
				break;
			}
		}
		if (!hasPoke)
		{
			G.player.playerState = PlayerState.BUSY;
			display.showNoPokemonAlert(true);
		}
		
		// create player list
		players = new LinkedList<NetworkPlayer>();
		
		// create item list
		items = new LinkedList<WorldPotion>();
		ignored_item_ids = new LinkedList<Integer>();
		
		// create region list
		regions = new LinkedList<Region>();
		
		// network setup
		networkReqLock = false;
		waitingForAccept = false;
		waitingForItemAccept = false;
		elapsedTime = 0;
		busyBinding = false;
		busyConnecting = false;
		networkEventMessenger = new Messenger(new Handler(this));
		networkUpdater = new Handler();
		updater = new Runnable(){
			public void run() {
				if (networkBound)
				{
					if (networkBinder.isConnectedToServer())
					{
						if (G.mode == Mode.MAP)
						{
							//if (!waitingForItems)
							//{
								networkBinder.sendGameStateRequest(new NetworkMessage("GetGameObjects"));
								waitingForItems = true;
							//}
							
							//if (!waitingForPlayers)
							//{
								networkBinder.sendGameStateRequest(new NetworkMessage("GetPlayers"));
								waitingForPlayers = true;
							//}
							
							// generate a Pokemon with probability based on catch rate if the player is in a special region
							if (G.player.playerState == PlayerState.AVAILABLE && !waitingForAccept)
							{
								if (elapsedTime >= 1000 && currentRegion != null && currentRegion.ordinal() < 7 && !foundPokeInRegion)
								{
									/*
									 * generate a pokemon that is within 5 levels of your party's
									 * strongest pokemon with 30% chance every 1 second
									 */
									if (G.random.nextDouble() < 0.3)
									{
										ArrayList<BasePokemon> pokes_in_region = G.pokemon_by_region[currentRegion.ordinal()];
										ArrayList<BasePokemon> options = new ArrayList<BasePokemon>();
										int levelSum = 0;
										for (Pokemon p:G.player.pokemon)
											levelSum += p.getLevel();
										int level = levelSum/G.player.pokemon.size() + G.random.nextInt(11) - 5;
										if (level <= 0)
											level = 1;
										for (BasePokemon p:pokes_in_region)
										{
											// if the level falls within the pokemon's normal range
											if (p.baseLevel <= level && (p.evolution == null || p.evolution[1] > level))
												options.add(p);
										}
										networkReqLock = true;
										genPokemon = new Pokemon(options.get(G.random.nextInt(options.size())).index, level);
										foundPokeInRegion = true;
										battleInitMessage = null;
										initiateBattle();
									}
									elapsedTime -= 1000;
								}
								try{elapsedTime += networkBinder.getGameClock().getLatency();}
								catch (RuntimeException e) {elapsedTime += 500;}
							} 
						}
					}
					else if (!busyConnecting)
					{
						Log.i(NetworkVariables.TAG, "Trying to connect to game server...");
						networkBinder.ConnectToServer();
						busyConnecting = true;
					}
				}
				else if (!busyBinding)
				{
					Log.i(NetworkVariables.TAG, "Trying to rebind network service");
					createConnection();
				}
				try
				{
					Log.i(NetworkVariables.TAG, "Requested update after " + networkBinder.getGameClock().getLatency() + "ms");
					networkUpdater.postDelayed(updater, networkBinder.getGameClock().getLatency());
				}
				catch (RuntimeException e)
				{
					Log.i(NetworkVariables.TAG, "Requested update after " + 500 + "ms");
					networkUpdater.postDelayed(updater, 500);
				}
			}
		};
		display.showProgressDialog("Starting up...");
	}
	
	/*
	 * Methods related to adding, removing and updating network players, items and regions
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
		if (selectedPlayer != null && player.getID() == selectedPlayer.getID())
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
		if (!ignored_item_ids.contains(item.getID()))
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
		if (!ignored_item_ids.contains(item.getID()))
			display.addItem(item);
	}
	
	// addition method for regions
	private void addRegion(Region region)
	{
		if (region.getRegion() != Regions.POKEMART)
		{
			regions.add(region);
			display.addRegion(region);
		}
	}
	
	// removal method for regions
	private void removeRegion(Region region)
	{
		regions.remove(region);
		display.removeRegion(region);
	}
	
	// insert regions to keep the list sorted according to id
	private void insertRegion(Region region, int index)
	{
		if (region.getRegion() != Regions.POKEMART)
		{
			regions.add(index, region);
			display.addRegion(region);
		}
	}
	
	public void requestItem(int itemID)
	{
		synchronized (items)
		{
			if (!waitingForItemAccept)
			{
				selectedItem = items.get(Collections.binarySearch(items, new MapItem(null,itemID)));
				if (G.player.getDistanceFrom(selectedItem.getAndroidLocation()) < 20)
				{
					BagItem item = G.player.items[selectedItem.potionType.ordinal()+1];
					if (item.atMax())
					{
						display.showToast("You have the max no. of " + item.getName() + "s");
						selectedItem = null;
					}
					else
					{
						// check if there are any other players nearby
						boolean all_clear = true;
						synchronized (players)
						{
							for (NetworkPlayer pl:players)
							{
								if (selectedItem.getAndroidLocation().distanceTo(pl.getAndroidLocation()) < 50)
								{
									all_clear = false;
									break;
								}
							}
						}
						
						// send item request
						NetworkMessageMedium msg = new NetworkMessageMedium("ItemPickupRequest");
						msg.integers.add(itemID);
						networkBinder.sendRequest(msg);
						removeItem(selectedItem);
						ignored_item_ids.addLast(selectedItem.getID());
						if (ignored_item_ids.size() > 3)
							ignored_item_ids.removeFirst();
						
						if (all_clear)
						{
							BagItem itm = G.player.items[selectedItem.potionType.ordinal()+1];
							itm.increment();
							display.showToast("You have picked up a " + itm.getName());
							selectedItem = null;
						}
						else
						{
							waitingForItemAccept = true;
							display.showToast("Getting permission to pick up item...");
						}
					}
				}
				else
				{
					display.showToast("Item is too far away");
					selectedItem = null;
				}
			}
			else
				display.showToast("Try again in a second");
		}
	}
	
	/*
	 * Methods related to battles
	 */
	
	public void requestPlayer(int playerID)
	{
		synchronized (players)
		{
			networkReqLock = true;
			selectedPlayer = players.get(Collections.binarySearch(players, new MapItem(null,playerID)));
			if (G.player.getDistanceFrom(selectedPlayer.getAndroidLocation()) < 20)
			{
				if (selectedPlayer.getPlayerState() == PlayerState.BUSY)
				{
					display.showToast("Player is currently busy");
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
				networkBinder.sendDirectCommunication(getBattleInitiationMessage(Action.REQUEST_BATTLE), selectedPlayer.getID());
				display.showProgressDialog("Waiting for player response...");
				waitingForAccept = true;
			}
		}
	}
	
	public void rejectBattle(boolean sendResponse)
	{
		if (sendResponse)
		{
			// send Action.REJECT_BATTLE
			networkBinder.sendDirectCommunication(new NetworkMessage(Action.REJECT_BATTLE.toString()), selectedPlayer.getID());
		}
		selectedPlayer = null;
		waitingForAccept = false;
		networkReqLock = false;
	}
	
	public void acceptBattle()
	{
		// send Action.ACCEPT_BATTLE
		opponentID = selectedPlayer.getID();
		randomSeed = G.random.nextInt();
		NetworkMessageMedium battleInit = getBattleInitiationMessage(Action.ACCEPT_BATTLE);
		battleInit.integers.add(randomSeed);
		networkBinder.sendDirectCommunication(battleInit, opponentID);
		initiateBattle();
	}
	
	private void initiateBattle()
	{
		// send busy status update
		networkBinder.sendGameUpdate(new NetworkMessage("EnteredBattle"));
		G.player.playerState = PlayerState.BUSY;
		// switch to a battle against a network opponent
		if (battleInitMessage != null)
			display.switchToBattle(battleInitMessage, randomSeed);
		// switch to a battle against a generated pokemon
		else 
			display.switchToBattle(null, G.random.nextInt());
		networkReqLock = false;
		waitingForAccept = false;
	}
	
	public void finalizeBattle()
	{
		// send available status update
		selectedPlayer = null;
		if (G.battle == null || G.battle.pokeCount > 0)
		{
			Log.i("Players", "Player set to available");
			networkBinder.sendGameUpdate(new NetworkMessage("ExitedBattle"));
			G.player.playerState = PlayerState.AVAILABLE;
			display.updateCoins();
		}
		else
			display.showNoPokemonAlert(true);
		G.battle = null;
		Trainer.saveTrainer(display);
	}
	
	public void sendSimpleBattleMessage(BattleMove move, int index)
	{
		NetworkMessageMedium nMsg = new NetworkMessageMedium(Action.BATTLE_MOVE.toString());
		nMsg.integers.add(move.ordinal());
		nMsg.integers.add(index);
		networkBinder.sendDirectCommunication(nMsg, opponentID);
	}
	
	public void sendSwitchBattleMessage(Pokemon new_poke)
	{
		NetworkMessageMedium nMsg = new NetworkMessageMedium(Action.BATTLE_MOVE.toString());
		nMsg.integers.add(BattleMove.SWITCH_POKEMON.ordinal());
		nMsg.integers.add(new_poke.index);
		nMsg.integers.add(new_poke.getLevel());
		nMsg.integers.add(new_poke.getHP());
		for (int stat:new_poke.getStats())
			nMsg.integers.add(stat);
		networkBinder.sendDirectCommunication(nMsg, opponentID);
	}
	
	public void sendCancelMessage()
	{
		NetworkMessageMedium nMsg = new NetworkMessageMedium(Action.CANCEL.toString());
		if (G.mode == Mode.BATTLE)
			networkBinder.sendDirectCommunication(nMsg, opponentID);
		else
			networkBinder.sendDirectCommunication(nMsg, selectedPlayer.getID());
	}
	
	private NetworkMessageMedium getBattleInitiationMessage(Action action)
	{
		// data in request: action, id, nick, gender, base, hp, attack, defense, speed, special, level
		NetworkMessageMedium msg = new NetworkMessageMedium(action.toString());
		
		// get the first pokemon that has hp left
		int i = 0;
		while (G.player.pokemon.get(i).getHP() <= 0)
			i++;
		Pokemon first = G.player.pokemon.get(i);
		
		msg.integers.add(action.ordinal());
		msg.integers.add(G.player.id);
		msg.integers.add(G.player.gender.ordinal());
		msg.integers.add(first.index);
		msg.integers.add(first.getLevel());
		msg.integers.add(first.getHP());
		for (int stat:first.getStats())
			msg.integers.add(stat);
		
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
				fill.setStyle(Style.FILL);
				fill.setAntiAlias(true);
				
				// the settings for the outline
				Paint outline = new Paint();
				if (i > 6)
				{
					outline.setColor(Color.BLACK);
					outline.setStrokeWidth(3);
					fill.setAlpha(192);
				}
				else
				{
					outline.setColor(G.region_colours[i]);
					outline.setStrokeWidth(2);
					fill.setAlpha(128);
				}
				outline.setStrokeJoin(Join.ROUND);
				outline.setStyle(Style.STROKE);
				outline.setAntiAlias(true);
				G.region_outline[i] = outline;
				G.region_fill[i] = fill;
			}
		}
		catch (Exception e) {Log.e("Data load", e.toString());}
	}
	
	/*
	 * Location listener methods
	 */
	
	public void onLocationChanged(Location location) 
	{
		display.updateLocation(location);
		
		// send location update to server
		NetworkMessageMedium msg = new NetworkMessageMedium("LocationUpdate");
		msg.doubles.add(location.getLatitude());
		msg.doubles.add(location.getLongitude());
		networkBinder.sendGameUpdate(msg);
		
		// check which region type the player is in
		Regions newRegion = checkRegion(location);
		// can only find one Pokemon in a region at a time
		if (currentRegion != newRegion)
		{
			foundPokeInRegion = false;
			if (newRegion == Regions.POKEMART)
			{
				// show pokemart view
			}
			else if (newRegion == Regions.POKEMON_CENTER)
			{
				// heal all pokemon
				for (Pokemon p:G.player.pokemon)
				{
					p.setHP(p.getTotalHP());
					p.restorePP();
				}
				display.showToast("All your Pokémon have been restored!");
				display.showNoPokemonAlert(false);
				
				// send available update
				G.player.playerState = PlayerState.AVAILABLE;
				networkBinder.sendGameUpdate(new NetworkMessage("ExitedBattle"));
			}
		}
		currentRegion = newRegion;
	}

	public void onLocationError(int errorCode) 
	{
		// do nothing for now
	}
	
	private Regions checkRegion(Location location)
	{
		Regions current = Regions.NONE;
		Point point = Region.geomFactory.createPoint(new Coordinate(location.getLongitude(), location.getLatitude()));
		synchronized (regions)
		{
			for (Region region:regions)
			{
				if(region.contains(point))
				{
					current = region.getRegion();
					break;
				}
			}
			return current;
		}
	}
	
	/*
	 * Network event handler and connection setup
	 */
	
	public void createConnection()
	{
		// bind network component
		busyBinding = true;
		Intent intent = new Intent(display, NetworkComService.class);
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
				busyConnecting = true;
				networkBound = true;
				busyBinding = false;
				display.showToast("Connected to service");
			}
		};
		display.bindService(intent, ser, Context.BIND_AUTO_CREATE);
	}
	
	public void endGame()
	{
		networkBinder.sendTerminationRequest(new NetworkMessage(""));
	}
	
	public boolean handleMessage(Message msg) 
	{
		switch (NetworkComBinder.EventType.values()[msg.what])
		{
			case CONNECTION_ESTABLISHED:
			{
				display.showToast("Connected to game server");
				busyConnecting = false;
				break;	
			}
			case PLAYER_REGISTERED:
			{
				// get player id
				G.player.id = networkBinder.getPlayerId();
				// send busy status update (if necessary)
				if (G.player.playerState == PlayerState.BUSY)
					networkBinder.sendGameUpdate(new NetworkMessage("EnteredBattle"));
				// only get map data after 5 seconds to give lawrence's latency estimation time to converge
				Log.i(NetworkVariables.TAG, "Requesting map data in 5 seconds...");
				networkUpdater.postDelayed(new Runnable(){
					public void run()
					{
						// gets regions in a 2000km radius
						if (regions.size() == 0)
						{
							NetworkMessageMedium msg1 = new NetworkMessageMedium("MapDataRequest");
							msg1.doubles.add(G.player.getLocation().getLatitude());
							msg1.doubles.add(G.player.getLocation().getLongitude());
							msg1.doubles.add(2000.0);
							networkBinder.sendRequest(msg1);
							Log.i("Regions", "Requesting regions");
						}
					}
				}, 5000);
				break;
			}
			case CONNECTION_LOST:
			{
				display.showToast("Connection to game server lost");
				if (G.mode == Mode.BATTLE)
					G.battle.handlePlayerDisconnected();
				break;
			}
			case CONNECTION_FAILED:
			{
				display.showToast("Could not connect to game server");
				busyConnecting = false;
				// if the connection fails without ever having been connected we need to start the updater
				networkUpdater.removeCallbacks(updater);
				networkUpdater.postDelayed(updater, 0);
				break;
			}
			case GAMESTATE_RECEIVED:
			{
				NetworkMessageLarge nMsg = (NetworkMessageLarge)((NetworkEvent)msg.obj).getMessage();
				String tag = nMsg.getMessage();
				if (tag.equals("Response:GetGameObjects"))
				{
					waitingForItems = false;
					ArrayList<LokemonPotion> ilist = (ArrayList<LokemonPotion>)nMsg.objectDict.get("ItemList");
					if (ilist != null)
					{
						synchronized (items)
						{
							// merging old and new player lists (trying to avoid unnecessary creation of objects)
							Iterator<WorldPotion> i = items.iterator();
							Iterator<LokemonPotion> it = ilist.iterator();
							List<Integer> newIndices = new LinkedList<Integer>();
							List<WorldPotion> newItems = new LinkedList<WorldPotion>();
							WorldPotion ni = i.hasNext()?i.next():null;
							LokemonPotion li = it.hasNext()?it.next():null;
							int index = 0;
							while (ni != null && li != null)
							{
								int id = li.getId();
								if (ni.getID() < id)
								{
									display.removeItem(ni);
									i.remove();
									if (i.hasNext())
										ni = i.next();
									else ni = null;
								}
								else if (ni.getID() > id)
								{
									networkTransferObjects.UtilityObjects.Location loc = li.getPosition();
									newIndices.add(index);
									newItems.add(new WorldPotion(Potions.values()[li.getType().ordinal()],new GeoPoint(loc.getX(),loc.getY()),li.getId()));
									if (it.hasNext())
									{
										li = it.next();
										index++;
									}
									else li = null;
								}
								else
								{
									if (it.hasNext())
									{
										li = it.next();
										index++;
									}
									else li = null;
									if (i.hasNext())
										ni = i.next();
									else ni = null;
								}
							}
							
							// remove or add tail of list
							if (ni != null)
							{	
								display.removeItem(ni);
								i.remove();
								while (i.hasNext())
								{
									ni = i.next();
									display.removeItem(ni);
									i.remove();
								}
							}
							else if (li != null)
							{
								networkTransferObjects.UtilityObjects.Location loc = li.getPosition();
								addItem(new WorldPotion(Potions.values()[li.getType().ordinal()],new GeoPoint(loc.getX(),loc.getY()),li.getId()));
								while (it.hasNext())
								{
									li = it.next();
									loc = li.getPosition();
									addItem(new WorldPotion(Potions.values()[li.getType().ordinal()],new GeoPoint(loc.getX(),loc.getY()),li.getId()));
								}
							}
							
							// insert new objects
							Iterator<Integer> count = newIndices.iterator();
							for (WorldPotion p:newItems)
								insertItem(p,count.next());
						}
					}
				}
				else if (tag.equals("Response:GetPlayers"))
				{
					waitingForPlayers = false;
					ArrayList<LokemonPlayer> plist = (ArrayList<LokemonPlayer>)nMsg.objectDict.get("PlayerList");
					if (plist != null)
					{
						synchronized (players)
						{
							// merging old and new player lists (trying to avoid unnecessary creation of objects)
							Iterator<NetworkPlayer> i = players.iterator();
							Iterator<LokemonPlayer> it = plist.iterator();
							List<Integer> newIndices = new LinkedList<Integer>();
							List<NetworkPlayer> newPlayers = new LinkedList<NetworkPlayer>();
							NetworkPlayer np = i.hasNext()?i.next():null;
							LokemonPlayer lp = it.hasNext()?it.next():null;
							int index = 0;
							while (np != null && lp != null)
							{
								int id = lp.getPlayerID();
								if (np.getID() == id)
								{
									np.updateLocation(Util.fromSerialLocation(lp.getPosition()));
									np.setPlayerState(lp.getBusy()?PlayerState.BUSY:PlayerState.AVAILABLE);
									if (i.hasNext())
										np = i.next();
									else np = null;
									if (it.hasNext())
									{
										lp = it.next();
										index++;
									}
									else lp = null;
								}
								else if (np.getID() < id)
								{
									display.removePlayer(np);
									i.remove();
									if (i.hasNext())
										np = i.next();
									else np = null;
								}
								else
								{
									networkTransferObjects.UtilityObjects.Location loc = lp.getPosition();
									newIndices.add(index);
									newPlayers.add(new NetworkPlayer(lp.getPlayerID(), lp.getPlayerName(), Gender.values()[lp.getAvatar()], new GeoPoint(loc.getX(),loc.getY())));
									if (it.hasNext())
									{
										lp = it.next();
										index++;
									}
									else lp = null;
								}
							}
							
							// remove or add tail of list
							if (np != null)
							{
								display.removePlayer(np);
								i.remove();
								while (i.hasNext())
								{
									np = i.next();
									display.removePlayer(np);
									i.remove();
								}
							}
							else if (lp != null)
							{
								networkTransferObjects.UtilityObjects.Location loc = lp.getPosition();
								addPlayer(new NetworkPlayer(lp.getPlayerID(), lp.getPlayerName(), Gender.values()[lp.getAvatar()], new GeoPoint(loc.getX(),loc.getY())));
								while (it.hasNext()) 
								{
									lp = it.next();
									loc = lp.getPosition();
									addPlayer(new NetworkPlayer(lp.getPlayerID(), lp.getPlayerName(), Gender.values()[lp.getAvatar()], new GeoPoint(loc.getX(),loc.getY())));
								}
							}
							
							// insert new objects
							Iterator<Integer> count = newIndices.iterator();
							for (NetworkPlayer p:newPlayers)
								insertPlayer(p,count.next());
						}
					}
				}
				else if (tag.equals("MapDataResponse"))
				{
					display.cancelProgressDialog();
					// tell the server where the player is
					onLocationChanged(G.player.getLocation());			
					// start requesting updates from server
					networkUpdater.postDelayed(updater, 0);
					
					// add regions
					ArrayList<LokemonSpatialObject> rlist = (ArrayList<LokemonSpatialObject>)nMsg.objectDict.get("SpatialObjects");
					if (rlist != null)
					{
						synchronized (regions)
						{
							// merging old and new region lists (trying to avoid unnecessary creation of objects)
							Iterator<Region> i = regions.iterator();
							Iterator<LokemonSpatialObject> it = rlist.iterator();
							List<Integer> newIndices = new LinkedList<Integer>();
							List<Region> newRegions = new LinkedList<Region>();
							Region nr = i.hasNext()?i.next():null;
							LokemonSpatialObject lr = it.hasNext()?it.next():null;
							int index = 0;
							while (nr != null && lr != null)
							{
								int id = lr.getObjectId();
								if (nr.getID() == id)
								{
									if (i.hasNext())
										nr = i.next();
									else nr = null;
									if (it.hasNext())
									{
										lr = it.next();
										index++;
									}
									else lr = null;
								}
								else if (nr.getID() < id)
								{
									Log.i("Regions", "Keeping id=" + nr.getID());
									display.removeRegion(nr);
									i.remove();
									if (i.hasNext())
										nr = i.next();
									else nr = null;
								}
								else
								{
									Log.i("Regions", "Inserting id=" + id);
									newIndices.add(index);
									newRegions.add(new Region(lr.getCoords(), Regions.values()[lr.getType().ordinal()], lr.getObjectId()));
									if (it.hasNext())
									{
										lr = it.next();
										index++;
									}
									else lr = null;
								}
							}
							
							// remove or add tail of list
							if (nr != null)
							{
								Log.i("Regions", "Removing id=" + nr.getID());
								display.removeRegion(nr);
								i.remove();
								while (i.hasNext())
								{
									nr = i.next();
									Log.i("Regions", "Removing id=" + nr.getID());
									display.removeRegion(nr);
									i.remove();
								}
							}
							else if (lr != null)
							{
								Log.i("Regions", "Adding id=" + lr.getObjectId());
								addRegion(new Region(lr.getCoords(), Regions.values()[lr.getType().ordinal()], lr.getObjectId()));
								while (it.hasNext()) 
								{
									lr = it.next();
									Log.i("Regions", "Adding id=" + lr.getObjectId());
									addRegion(new Region(lr.getCoords(), Regions.values()[lr.getType().ordinal()], lr.getObjectId()));
								}
							}
							
							// insert new objects
							Iterator<Integer> count = newIndices.iterator();
							for (Region p:newRegions)
								insertRegion(p,count.next());
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
			case REQUEST_RECEIVED:
			{
				NetworkMessage nMsg = (NetworkMessage)((NetworkEvent)msg.obj).getMessage();
				String tag = nMsg.getMessage();
				if (waitingForItemAccept)
				{
					if (tag.equals("Accept") && selectedItem != null)
					{
						BagItem item = G.player.items[selectedItem.potionType.ordinal()+1];
						item.increment();
						display.showToast("You have picked up a " + item.getName());
						selectedItem = null;
					}
					else
					{
						display.showToast("Someone picked up the item before you");
						selectedItem = null;
					}
					waitingForItemAccept = false;
				}
				break;
			}
			case UPDATE_RECEIVED:
			{
				break;
			}
			case DIRECT_MESSAGE_RECEIVED:
			{
				DirectCommunicationEvent dce = (DirectCommunicationEvent)msg.obj;
				NetworkMessage nMsg = (NetworkMessage)dce.getMessage();
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
						{
							battleInitMessage = (NetworkMessageMedium)nMsg;
							opponentID = selectedPlayer.getID();
							randomSeed = battleInitMessage.integers.get(11);
							initiateBattle();
						}
						break;
					case REQUEST_BATTLE:
						NetworkMessageMedium req = (NetworkMessageMedium)nMsg;
						if (G.mode == Mode.MAP)
						{
							if (!networkReqLock)
							{
								networkReqLock = true;
								battleInitMessage = (NetworkMessageMedium)nMsg;
								selectedPlayer = new NetworkPlayer(req.integers.get(1), req.strings.get(0), Gender.values()[req.integers.get(2)], null);
								display.showBattleIncomingDialog(req.strings.get(0));
							}
							else if (waitingForAccept && selectedPlayer.getID() == dce.getSourcePlayerID())
							{
								// arbitrate based on IDs because the result will be consistent
								if (G.player.id > selectedPlayer.getID())
								{
									battleInitMessage = (NetworkMessageMedium)nMsg;
									display.cancelProgressDialog();
									waitingForAccept = false;
									display.showBattleIncomingDialog(req.strings.get(0));
								}
							}
							else
								networkBinder.sendDirectCommunication(new NetworkMessage(Action.REJECT_ALL.toString()), req.integers.get(1));
						}
						break;
					case REJECT_ALL:
						if (waitingForAccept)
						{
							display.cancelProgressDialog();
							display.showToast(selectedPlayer.nickname + " is currently busy");
							rejectBattle(false);
						}
						break;
					case BATTLE_MOVE:
						if (G.mode == Mode.BATTLE)
						{
							NetworkMessageMedium mMsg = (NetworkMessageMedium)nMsg;
							BattleMove move = BattleMove.values()[mMsg.integers.get(0)];
							if (move == BattleMove.DISCONNECTED)
								G.battle.handleOpponentDisconnected();
							else if (move == BattleMove.GAME_OVER)
								G.battle.handleOpponentDefeated();
							else if (move != BattleMove.SWITCH_POKEMON)
								G.battle.handleSimpleBattleMove(move, mMsg.integers.get(1));
							else
							{
								int stats[] = new int[5];
								for (int i= 0; i < 5; i++)
									stats[i] = mMsg.integers.get(4+i);
								Pokemon new_poke = new Pokemon(mMsg.integers.get(1), mMsg.integers.get(3),0, mMsg.integers.get(2), null, null, stats, null);
								G.battle.handleSwitchBattleMove(new_poke);
							}
						}
						break;
					case CANCEL:
						if (G.mode == Mode.BATTLE)
						{
							if (G.battle != null && dce.getSourcePlayerID() == opponentID)
							{
								G.battle.handleOpponentDisconnected();
								Log.i("Players", "Opponent canceled the battle");
							}	
						}
						else if (selectedPlayer != null && selectedPlayer.getID() == dce.getSourcePlayerID())
						{
							display.cancelBattleAlert();
							display.cancelProgressDialog();
							Log.i("Players", "Opponent canceled the battle request");
							display.showToast("The request was canceled");
							rejectBattle(false);
						}
						break;
					}
				}
				catch (IllegalArgumentException e)
				{
					// either NOTIFICATION:PlayerDisconnected or ERROR: Direct communication target <targetPlayerId> is no longer connected!
					if ((waitingForAccept || networkReqLock) && dce.getSourcePlayerID() == selectedPlayer.getID())
					{
						display.cancelBattleAlert();
						selectedPlayer = null;
						waitingForAccept = false;
						networkReqLock = false;
						Log.i("Players", "Opponent disconnected during battle initiation");
					}
					else if (G.mode == Mode.BATTLE && G.battle != null && G.battle.battleType == BattleType.TRAINER && dce.getSourcePlayerID() == opponentID)
					{
						G.battle.handleOpponentDisconnected();
						Log.i("Players", "Opponent disconnected during battle");
					}
				}
				break;
			}
		}
		return true;
	}
}
