package com.Lobretimgap.NetworkClient.Events;

@SuppressWarnings("serial")
public class DirectCommunicationEvent extends NetworkEvent {

	int srcPlayer;
	public DirectCommunicationEvent(Object source, Object message, int sourcePlayer) {
		super(source, message);
		srcPlayer = sourcePlayer;
	}
	
	/**
	 * Method to find out who sent the message to us.
	 * @return the playerID of the player who sent the message.
	 */
	public int getSourcePlayerID()
	{
		return srcPlayer;
	}

}
