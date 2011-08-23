/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.Lobretimgap.NetworkClient.EventListeners;

import java.util.EventListener;
import com.Lobretimgap.NetworkClient.Events.NetworkEvent;

/**
 * Base class listener for all the network events
 * @date 2011/08/02
 * @author Lawrence Webley
 */
public interface NetworkEventListener extends EventListener
{
    public void EventOccured(NetworkEvent e);
}
