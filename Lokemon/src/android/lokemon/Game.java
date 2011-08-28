package android.lokemon;

import android.content.*;
import android.content.res.*;
import android.os.*;
import android.util.Log;
import android.app.Activity;
import java.io.*;

//networking components
import com.Lobretimgap.NetworkClient.NetworkComBinder;
import com.Lobretimgap.NetworkClient.NetworkComService;
import com.Lobretimgap.NetworkClient.NetworkVariables;
import networkTransferObjects.NetworkMessage;

public class Game {
	
	private static Game game;
	
	// network variables
	private boolean connected;
	private boolean networkBound;
	private NetworkComBinder binder;
	private ServiceConnection ser;
	private final Messenger eventMessenger = new Messenger(new eventHandler());
	
	private Game()
	{
		game = this;
		connected = false;
		networkBound = false;
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
	
	public static void startGame(Activity current)
	{
		game.createConnection(current);
	}
	
	public static void endGame(Activity current)
	{
		if(game.binder != null)game.binder.sendTerminationRequest(new NetworkMessage("Bye bye!"));
		else Log.e(NetworkVariables.TAG, "Binder is null");
	}
	
	public static void pauseGame(Activity current)
	{
		
	}
	
	private void createConnection(Activity current)
	{
		// bind network component
		Intent intent = new Intent(current, NetworkComService.class);
		ser = new ServiceConnection() {
			
			public void onServiceDisconnected(ComponentName name) {
				networkBound = false;
				Log.i(NetworkVariables.TAG, "Service disconnected");
			}
			
			public void onServiceConnected(ComponentName name, IBinder service) {
				GameScreen.status.setText("Connecting to game server...");
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
					GameScreen.status.setText("Connected!");
					break;	
				case CONNECTION_LOST:
					connected = false;
					break;
				case CONNECTION_FAILED:
					connected = false;
					GameScreen.status.setText("Connection failed");
					break;
			}
		}
	}
}
