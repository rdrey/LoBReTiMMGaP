package com.Lobretimgap.NetworkClient.Implementation;

import networkTransferObjects.NetworkMessage;
import networkTransferObjects.PlayerRegistrationMessage;
import android.util.Log;

import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;

public class CoreNetworkThreadImpl extends CoreNetworkThread {

	@Override
	public PlayerRegistrationMessage getPlayerRegistrationInformation() {		
		return new PlayerRegistrationMessage("Lawrence");
	}

	@Override
	public void processInitialGameState(NetworkMessage message) {
		Log.d(NetworkVariables.TAG, "Received initial game state. Message: "+message.getMessage());
	}

}
