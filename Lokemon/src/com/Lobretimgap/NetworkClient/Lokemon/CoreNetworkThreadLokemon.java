package com.Lobretimgap.NetworkClient.Lokemon;

import networkTransferObjects.NetworkMessage;
import networkTransferObjects.PlayerRegistrationMessage;
import android.lokemon.*;
import android.util.Log;

import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;

public class CoreNetworkThreadLokemon extends CoreNetworkThread {

	@Override
	public PlayerRegistrationMessage getPlayerRegistrationInformation() {		
		return new PlayerRegistrationMessage(G.player.nickname);
	}

	@Override
	public void processInitialGameState(NetworkMessage message) {
		Log.d(NetworkVariables.TAG, "Received initial game state. Message: "+message.getMessage());
		//if (Integer.parseInt(message.getMessage()) > 1) G.game.initiateBattle(); 
	}

}
