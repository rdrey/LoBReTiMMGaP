package networkserver.Events;

/**
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public class NetworkEvent extends java.util.EventObject {

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
