package android.lokemon;

import android.content.*;
import android.content.res.*;
import android.os.*;
import android.util.Log;
import android.app.Activity;
import java.io.*;
import java.sql.Time;

//networking components
import com.Lobretimgap.NetworkClient.NetworkComBinder;
import com.Lobretimgap.NetworkClient.NetworkComService;
import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;

import networkTransferObjects.NetworkMessage;

public class Game {
	
	public static Game game;
	
	// network variables
	private boolean connected;
	private boolean networkBound;
	private NetworkComBinder binder;
	private final Messenger eventMessenger = new Messenger(new eventHandler());
	
	// battle variables
	private int battleSeed;
	private int opponentIndex;
	private String opponentNick;
	
	// display variables
	GameScreen gameScreen;
	
	private Game()
	{
		game = this;
		connected = false;
		networkBound = false;
		battleSeed = 0;
		gameScreen = null;
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
	
	public static void startGame(GameScreen current)
	{
		game.gameScreen = current;
		game.createConnection(current);
	}
	
	public static void endGame()
	{
		if(game.binder != null)game.binder.sendTerminationRequest(new NetworkMessage("Bye bye!"));
		else Log.e(NetworkVariables.TAG, "Binder is null");
	}
	
	public static void pauseGame()
	{
		
	}
	
	public void initiateBattle()
	{
		Log.i(NetworkVariables.TAG,""+binder.sendGameUpdate(new NetworkMessage("battle,"+Trainer.player.nickname + "," + Trainer.player.pokemon[0])));
	}
	
	public void acceptBattle()
	{
		battleSeed = (int)(Math.random()*100);
		binder.sendGameUpdate(new NetworkMessage("accept,"+Trainer.player.nickname + "," + Trainer.player.pokemon[0] + "," + battleSeed));
		gameScreen.setStatusText("Accepted battle...");
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
						acceptBattle();
						setBattleTurn(Pokemon.pokemon[opponentIndex].speed <= Pokemon.pokemon[Trainer.player.pokemon[0]].speed);
					}
					else if (args[0].equals("accept"))
					{
						opponentNick = args[1];
						opponentIndex = Integer.parseInt(args[2]);
						battleSeed = Integer.parseInt(args[3]);
						setBattleTurn(Pokemon.pokemon[opponentIndex].speed <= Pokemon.pokemon[Trainer.player.pokemon[0]].speed);
					}
					break;
			}
		}
	}
}
