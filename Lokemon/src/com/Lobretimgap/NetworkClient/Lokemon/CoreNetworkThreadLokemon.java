package com.Lobretimgap.NetworkClient.Lokemon;

import networkTransferObjects.NetworkMessage;
import networkTransferObjects.PlayerRegistrationMessage;
import android.lokemon.*;
import com.Lobretimgap.NetworkClient.Threads.CoreNetworkThread;

public class CoreNetworkThreadLokemon extends CoreNetworkThread {

	@Override
	public PlayerRegistrationMessage getPlayerRegistrationInformation() {
		PlayerRegistrationMessage msg = new PlayerRegistrationMessage(G.player.nickname);
		msg.integers.add(G.player.gender.ordinal()); //
		return msg;

	}

	@Override
	public void processInitialGameState(NetworkMessage message) {
		// TODO Auto-generated method stub

	}

}
