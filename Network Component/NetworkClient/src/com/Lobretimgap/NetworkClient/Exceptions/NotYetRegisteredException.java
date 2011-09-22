package com.Lobretimgap.NetworkClient.Exceptions;

@SuppressWarnings("serial")
public class NotYetRegisteredException extends IllegalStateException {
	
	public NotYetRegisteredException(String message)
	{
		super(message);
	}
}
