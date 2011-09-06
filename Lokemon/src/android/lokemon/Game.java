package android.lokemon;

import android.content.*;
import android.content.res.*;
import android.os.*;
import android.util.Log;
import android.app.Activity;
import java.io.*;
import java.util.Random;

//networking components
import com.Lobretimgap.NetworkClient.NetworkComBinder;
import com.Lobretimgap.NetworkClient.NetworkComService;
import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;

import networkTransferObjects.NetworkMessage;

public class Game {
	
	// network variables
	private boolean connected;
	private boolean networkBound;
	private NetworkComBinder binder;
	private final Messenger eventMessenger = new Messenger(new eventHandler());
	
	// battle variables
	private int battleSeed;
	private int opponentIndex;
	private String opponentNick;
	private int opponentMove;
	private int yourMove;
	private int myHealth;
	private int oppHealth;
	private Random random;
	
	// display variables
	GameScreen gameScreen;
	
	private Game()
	{
		G.game = this;
		connected = false;
		networkBound = false;
		battleSeed = 0;
		gameScreen = null;
		opponentMove = -1;
		yourMove = -1;
		myHealth = 100;
		oppHealth = 100;
	}
	
	public static void loadGameData(Activity current)
	{
		new Game();
		try
		{
			AssetManager assetManager = current.getAssets();
			BufferedReader input = new BufferedReader(new InputStreamReader(assetManager.open("base_pokemon.json")));
			String str = "";
			String in = input.readLine();
			while (in != null)
			{
				str += in;
				in = input.readLine();
			}
			BasePokemon.loadPokemon(str, current);
			input.close();
			assetManager.close();
		}
		catch (Exception e) {Log.e("Data load", e.getMessage());}
	}
	
	public static void startGame(GameScreen current)
	{
		G.game.gameScreen = current;
		G.game.createConnection(current);
	}
	
	public static void endGame()
	{
		if(G.game.binder != null)G.game.binder.sendTerminationRequest(new NetworkMessage("Bye bye!"));
		else Log.e(NetworkVariables.TAG, "Binder is null");
	}
	
	public static void pauseGame()
	{
		
	}
	
	public void initiateBattle()
	{
		binder.sendGameUpdate(new NetworkMessage("battle,"+G.player.nickname + "," + G.player.pokemon.get(0).index));
	}
	
	public void acceptBattle()
	{
		battleSeed = (int)(Math.random()*100);
		random = new Random(battleSeed);
		binder.sendGameUpdate(new NetworkMessage("accept,"+G.player.nickname + "," + G.player.pokemon.get(0).index + "," + battleSeed));
		gameScreen.setStatusText("Accepted battle...");
	}
	
	public void runAway()
	{
		binder.sendGameUpdate(new NetworkMessage("run"));
	}
	
	public void attack(int moveIndex)
	{
		yourMove = moveIndex;
		Log.e(NetworkVariables.TAG, "move: " + opponentMove + " " + yourMove);
		binder.sendGameUpdate(new NetworkMessage("attack," + moveIndex));
		if (opponentMove > -1) playTurn();
	}
	
	public void playTurn()
	{
		myHealth -= random.nextInt()%10;
		oppHealth -= random.nextInt()%10;
		gameScreen.setMyHealth(myHealth);
		gameScreen.setOppHealth(oppHealth);
		yourMove = -1;
		opponentMove = -1;
		Log.e(NetworkVariables.TAG, "move played by " + G.player.nickname);
		setBattleTurn(G.basePokemon[opponentIndex].speed <= G.player.pokemon.get(0).getBase().speed);
	}
	
	public void setBattleTurn(boolean self)
	{
		if (self)
		{
			gameScreen.setStatusText("Your turn...");
		}
		else
		{
			gameScreen.setStatusText("Opponent's turn...");
		}
		gameScreen.enableAttackInterface(true);
	}
	
	private void createConnection(Activity current)
	{
		// bind network component
		Intent intent = new Intent(current, NetworkComService.class);
		ServiceConnection ser = new ServiceConnection() {
			
			public void onServiceDisconnected(ComponentName name) {
				networkBound = false;
				Log.i(NetworkVariables.TAG, "Service disconnected");
			}
			
			public void onServiceConnected(ComponentName name, IBinder service) {
				gameScreen.setStatusText("Connecting to game server...");
				// get an instance of the binder for the service
				binder = (NetworkComBinder)service;
				networkBound = true;
				binder.registerMessenger(eventMessenger);
				binder.ConnectToServer();		
			}
		};
		current.bindService(intent, ser, Context.BIND_AUTO_CREATE);
	}
	
	private class eventHandler extends Handler{
		
		public void handleMessage(Message msg) 
		{
			switch (NetworkComBinder.EventType.values()[msg.what])
			{
				case CONNECTION_ESTABLISHED:
					connected = true;
					gameScreen.setStatusText("Connected!");
					break;	
				case CONNECTION_LOST:
					connected = false;
					break;
				case CONNECTION_FAILED:
					connected = false;
					gameScreen.setStatusText("Connection failed");
					break;
				case UPDATE_RECEIVED:
					String str = ((NetworkMessage)(((NetworkEvent)msg.obj).getMessage())).getMessage();
					String [] args = str.split(",");
					if (args[0].equals("battle"))
					{
						opponentNick = args[1];
						opponentIndex = Integer.parseInt(args[2]);
						gameScreen.setOppPoke(opponentIndex);
						gameScreen.setOppNick(opponentNick);
						gameScreen.setOppHealth(100);
						acceptBattle();
						setBattleTurn(G.basePokemon[opponentIndex].speed <= G.player.pokemon.get(0).getBase().speed);
					}
					else if (args[0].equals("accept"))
					{
						opponentNick = args[1];
						opponentIndex = Integer.parseInt(args[2]);
						battleSeed = Integer.parseInt(args[3]);
						random = new Random(battleSeed);
						gameScreen.setOppPoke(opponentIndex);
						gameScreen.setOppNick(opponentNick);
						gameScreen.setOppHealth(100);
						setBattleTurn(G.basePokemon[opponentIndex].speed <= G.player.pokemon.get(0).getBase().speed);
					}
					else if (args[0].equals("run"))
					{
						gameScreen.removeOpp();
						gameScreen.setStatusText(opponentNick + " ran away...");
						gameScreen.enableAttackInterface(false);
					}
					else if (args[0].equals("attack"))
					{
						opponentMove = Integer.parseInt(args[1]);
						Log.e(NetworkVariables.TAG, "move: " + opponentMove + " " + yourMove);
						if (yourMove > -1) playTurn();
					}
					break;
			}
		}
	}
}
