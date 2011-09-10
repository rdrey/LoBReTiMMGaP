package com.Lobretimgap.NetworkClient.Implementation;

import networkTransferObjects.NetworkMessage;
import networkTransferObjects.PlayerRegistrationMessage;
import android.util.Log;

import com.Lobretimgap.NetworkClient.NetworkVariables;
import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;

public class CoreNetworkThreadImpl extends CoreNetworkThread {

	@Override
	public PlayerRegistrationMessage getPlayerRegistrationInformation() {	
		PlayerRegistrationMessage msg = new PlayerRegistrationMessage("Lawrence");		
		msg.strings.add("Client sent this");
		msg.strings.add("And he sent this");
		msg.strings.add("Also.. who can forget this?");
		msg.integers.add(42);
		return msg;
	}

	@Override
	public void processInitialGameState(NetworkMessage message) {
		Log.d(NetworkVariables.TAG, "Received initial game state. Message: "+message.getMessage());
	}

}
