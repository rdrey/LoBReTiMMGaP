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

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Coordinate;

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
	private Regions currentRegion;
	
	// a reference to the screen that displays the game world
	private MapScreen display;
	
	// interaction variables
	private NetworkPlayer selectedPlayer;
	private WorldPotion selectedItem;
	private boolean foundPokeInRegion;
	
	// network objects
	private boolean networkReqLock; // when true all battle requests from players are ignored
	private boolean waitingForAccept; // when true all messages other than battle acceptance are ignored
	private boolean busyConnecting;
	private boolean networkBound;
	private boolean busyBinding;
	
	// we don't want to overwhelm the client with responses if there is a backlog
	private boolean waitingForRegions;
	private boolean waitingForPlayers;
	private boolean waitingForItems;
	
	// periodically requests game state updates from server
	private Handler networkUpdater;
	private Runnable updater;
	private Location lastUpdateLocation;
	private int numUpdates;
	
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
		
		addRegion(new Region(points,Regions.GRASSLAND,0));
		
		// network setup
		networkReqLock = false;
		waitingForAccept = false;
		numUpdates = 0;
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
						if (G.player.playerState == PlayerState.AVAILABLE)
						{
							if (numUpdates == 3 && !waitingForItems)
							{
								networkBinder.sendGameStateRequest(new NetworkMessage("GetGameObjects"));
								numUpdates = 0;
								waitingForItems = true;
							}
							
							if (!waitingForPlayers)
							{
								networkBinder.sendGameStateRequest(new NetworkMessage("GetPlayers"));
								waitingForPlayers = true;
							}
							numUpdates++;
							
							// generate a Pokemon with probability based on catch rate if the player is in a special region
							if (!waitingForAccept && currentRegion.ordinal() < 7 && !foundPokeInRegion)
							{
								double prob = Math.random();
								double total = 0;
								ArrayList<BasePokemon> pokes_in_region = G.pokemon_by_region[currentRegion.ordinal()];
								Pokemon newPoke = null;
								for (int i = 0; i < pokes_in_region.size(); i++)
								{
									BasePokemon base = pokes_in_region.get(i);
									total += base.catchrate;
									if (prob < total)
									{					
										networkReqLock = true;
										newPoke = new Pokemon(base.index, base.baseLevel+(int)(Math.random() * 4 + 1));
										foundPokeInRegion = true;
										initiateBattle();
										break;
									}
								}
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
				networkUpdater.postDelayed(updater, 1000);
			}
		};
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
	
	// addition method for regions
	private void addRegion(Region region)
	{
		regions.add(region);
		display.addRegion(region);
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
		regions.add(index, region);
		display.addRegion(region);
	}
	
	/*
	 * Methods related to initiating battles
	 */
	
	public void requestItem(int itemID)
	{
		synchronized (items)
		{
			if (selectedItem == null)
			{
				selectedItem = items.get(Collections.binarySearch(items, new MapItem(null,itemID)));
				if (G.player.getDistanceFrom(selectedItem.getAndroidLocation()) < 20)
				{
					BagItem item = G.player.items[selectedItem.potionType.ordinal()+1];
					if (item.atMax())
						display.showToast("You have the max no. of " + item.getName() + "s");
					else
					{
						// send item request
						NetworkMessageMedium msg = new NetworkMessageMedium("ItemPickupRequest");
						msg.integers.add(itemID);
						networkBinder.sendRequest(msg);
						removeItem(selectedItem);
						display.showToast("Trying to pick up item");
					}
				}
				else
					display.showToast("Item is too far away");
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
		networkBinder.sendDirectCommunication(getBattleInitiationMessage(Action.ACCEPT_BATTLE), selectedPlayer.getID());
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
	
	public void finalizeBattle(/*arguments describing result of battle*/)
	{
		// send available status update
		selectedPlayer = null;
		networkBinder.sendGameUpdate(new NetworkMessage("ExitedBattle"));
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
	 * Location listener methods
	 */
	
	public void onLocationChanged(Location location) 
	{
		display.updateLocation(location);
		
		// gets regions in a 100m radius if player has moved more than 50m
		if (!waitingForRegions && (lastUpdateLocation == null || location.distanceTo(lastUpdateLocation) > 5000))
		{
			// request regions
			waitingForRegions = true;
			NetworkMessageMedium msg = new NetworkMessageMedium("MapDataRequest");
			msg.doubles.add(location.getLatitude());
			msg.doubles.add(location.getLongitude());
			msg.doubles.add(10000.0);
			networkBinder.sendRequest(msg);
			lastUpdateLocation = location;
		}
		
		// send location update to server
		NetworkMessageMedium msg = new NetworkMessageMedium("LocationUpdate");
		msg.doubles.add(location.getLatitude());
		msg.doubles.add(location.getLongitude());
		networkBinder.sendGameUpdate(msg);
		
		// check which region type the player is in
		Regions newRegion = checkRegion(location);
		// can only find one Pokemon in a region at a time
		if (currentRegion != newRegion)
			foundPokeInRegion = false;
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
		Log.i(NetworkVariables.TAG, NetworkComBinder.EventType.values()[msg.what].toString());
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
				// tell the server where the player is
				onLocationChanged(G.player.getLocation());			
				// start requesting updates from server
				networkUpdater.postDelayed(updater, 1000);
				break;
			}
			case CONNECTION_LOST:
			{
				display.showToast("Connection to game server lost");
				break;
			}
			case CONNECTION_FAILED:
			{
				display.showToast("Could not connect to game server");
				busyConnecting = false;
				break;
			}
			case GAMESTATE_RECEIVED:
			{
				NetworkMessageLarge nMsg = (NetworkMessageLarge)((NetworkEvent)msg.obj).getMessage();
				String tag = nMsg.getMessage();
				Log.i(NetworkVariables.TAG, tag);
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
					waitingForRegions = false;
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
									Log.i("Regions", "Removing id=" + nr.getID());
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
									newRegions.add(new Region(lr.getGeom(), Regions.values()[lr.getType().ordinal()], lr.getObjectId()));
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
								addRegion(new Region(lr.getGeom(), Regions.values()[lr.getType().ordinal()], lr.getObjectId()));
								while (it.hasNext()) 
								{
									lr = it.next();
									Log.i("Regions", "Adding id=" + lr.getObjectId());
									addRegion(new Region(lr.getGeom(), Regions.values()[lr.getType().ordinal()], lr.getObjectId()));
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
				if (tag.equals("Accept") && selectedItem != null)
				{
					BagItem item = G.player.items[selectedItem.potionType.ordinal()+1];
					item.increment();
					display.showToast("You have picked up a " + item.getName());
					selectedItem = null;
				}
				else
				{
					display.showToast("Someone picked it up before you");
					selectedItem = null;
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
