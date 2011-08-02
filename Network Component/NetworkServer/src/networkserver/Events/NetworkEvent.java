package networkserver.Events;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class NetworkEvent extends java.awt.AWTEvent {

    Object message;

    public NetworkEvent(Object source, int id, Object message)
    {
        super(source, id);
        this.message = message;
    }

    public Object getMessage()
    {
        return message;
    }

}
