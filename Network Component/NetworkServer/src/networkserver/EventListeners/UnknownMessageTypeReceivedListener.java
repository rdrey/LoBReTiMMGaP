/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package networkserver.EventListeners;

/**
 * This event will be fired if a NetworkMessage with no type, or an unrecognised
 * type is received by the server. This could be used by an implementer to handle a
 * NetworkMessage with a custom MessageType.
 * @date 2011/08/03
 * @author Lawrence Webley
 */
public interface UnknownMessageTypeReceivedListener  extends NetworkEventListener{

}
