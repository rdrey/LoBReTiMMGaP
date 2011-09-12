package com.Lobretimgap.NetworkClient.Implementation;

import networkTransferObjects.NetworkMessage;
import networkTransferObjects.PlayerRegistrationMessage;

import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;

public class NetworkThreadImplementation extends CoreNetworkThread {

	@Override
	public PlayerRegistrationMessage getPlayerRegistrationInformation() {
		return new PlayerRegistrationMessage("Lawrence");
	}

	@Override
	public void processInitialGameState(NetworkMessage message) {
		// TODO Auto-generated method stub

	}

}
