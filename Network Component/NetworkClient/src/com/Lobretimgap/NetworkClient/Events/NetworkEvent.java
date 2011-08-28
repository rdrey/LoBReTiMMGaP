package com.Lobretimgap.NetworkClient.Events;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class NetworkEvent extends java.util.EventObject {

    /**
	 * 
	 */
	private static final long serialVersionUID = -2044906726180328059L;
	
	Object message;

    public NetworkEvent(Object source, Object message)
    {
        super(source);
        this.message = message;
    }

    public Object getMessage()
    {
        return message;
    }

}
